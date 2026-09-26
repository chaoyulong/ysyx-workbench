# NPC RISC-V32E CPU

## 2026-09-16 五级流水线（数据前递 + 重定向与冲刷）

### 结构

- `IFU | IDU | EXU | LSU | WBU` 五级；**IDU 是组合级**（译码 + 读寄存器堆 + 前递选择），各级之间的寄存器统一由顶层 `pipelineConnect` / `pipelineConnectLast` 生成
- `pipelineConnect` 带两个语义：
  - `flush`：本级里的指令一定比重定向源年轻 → 直接清掉（优先级最高，且不管它能不能往下走）
  - `block`：本级里可能就留着**重定向源自己** → 只挡上游、不清。否则 `lw x1,0(x2); jal ra,f` 里分支被 LSU 顶住时 `ra` 会丢
- 顶层按"谁比谁年轻"派生：`ifu→idu` 用 `flush=redirectAny`；`idu→exu` 用 `flush=lsuRedir, block=redirectAny`；`exu→lsu` 用 `block=lsuRedir`；`lsu→wbu` 不动（里面永远更老）

### 数据前递

- `forwardData`（`FwdState` 四态）由 EXU/LSU/WBU 组合给出：
  `NoWriter`（本级没有写这个 rd 的指令）/ `DataReady`（数据就在本级这一拍）/ `DataPendingHere`（本级产生但还没好，只有 LSU 的 load 等 AXI）/ `DataPendingLater`（要等它走到后面某级，只有 EXU 的 load/csr）
- IDU 用 `stageStatus(f, rs, useRf)` 查询三级并取**最年轻的写者**，得到 `FwdOutcome` 三态：`Miss`（用寄存器堆）/ `Hit`（前递给 EXU）/ `Wait`（停一拍，下一拍重判）
- 好处：`valid`/`notReady` 那种"两个 bool 描述三种状态"的写法被换掉，状态间互斥由类型保证；级空时不会再有"幽灵写者"

### 重定向与冲刷

| 源 | 触发条件 | 目标 |
|---|---|---|
| EXU | 分支/jal/jalr 成立（`banchCond.io.pcAsrc`） | 直通 `pcNext`（不加逻辑；非分支指令它天然是 `pc+4`） |
| LSU | `trapEnter` / `trapExit` / `fence.i` | `Mux(trapEnter, mtvec, Mux(trapExit, mepc, pcNext))` |

- `fence.i` 放在 **LSU** 产生：LSU 顺序处理访存，fence 进到 LSU 时它前面的 store 一定已完成（`b` 已回），之后的取指必然看得到新指令；`RedirectReq` 带 `fenceI` 字段，由 IFU 自己驱动 `icache.io.fenceI`（一次重定向 = 重新取指 + 可选失效 icache）
- IFU：`pcFetch`（重定向直接写、`reqIn.fire` 时 +4）+ `pcOfReq`（请求 pc 与响应配对）；`when(io.redirect.valid){state := Idle}` 对**所有**状态生效（否则重定向和一次 `reqIn.fire` 同拍时，错路径指令之后会被当正常指令发出去）
- icache：fence 到达时若正在 miss，置 `discardMiss` 丢弃这次回写（否则失效前的旧行会被重新标 valid）

### 顺带修掉的 bug

- `Decoder`：csr 从 `typeI` 移出后 `io.imm` 漏了 CSR 地址 → `csrAddr` 恒 0（所有 CSR 读写打空）；`useRf1` 补上 csr 的 rs1
- `Decoder`：改为显式列出 load/store/regWr，**非法指令不再产生假访存、假写回**
- `IFU`：icache 缺失完成时交付的 pc 偏 4（Mux 条件该用 `reqIn.fire`，不是 `rspOut.valid`）
- `icache`：删掉重复的 `validReg` 赋值块（靠 Verilog 后写覆盖先写才对）
- `regfile.c`：IFU 不再有 `pc` 寄存器，pc 改从 itrace 黑盒读

### 性能（microbench test, ysyxsoc）

| 指标 | 多周期 0690d2c | 流水线 519cf4f | 变化 |
|---|---|---|---|
| 总周期 | 15228461 | **14354926** | −5.7% |
| IPC | 0.0382 | **0.0405** | +6.0% |
| icache 命中率 | 91.83% | 90.96% | −0.9 pt |
| icache 缺失次数 | ~47.5K（按命中率推算） | 70.9K | +49% |
| 平均缺失代价 | 60.32 cyc | 59.37 cyc | −1.6% |
| LSU mem rd avg / total | 102.72 / 7.78M | 103.25 / 7.82M | ~ |
| LSU mem wr avg / total | 17.13 / 0.99M | 18.95 / 1.10M | +10.6% |
| 综合面积(nangate45) | 21735.39 µm² | **22307.29 µm²** | +2.6% |
| 综合频率(500MHz 目标) | 623.2 MHz | **601.9 MHz** | −3.4% |
| microbench Scored time | — | 6428.06 ms | — |

**瓶颈**：LSU 访存（rd 7.82M + wr 1.10M）+ icache 缺失（4.21M）≈ **13.13M / 14.35M = 91.5% 的周期在等访存**。所以流水线级数不是瓶颈，这也是本版只比多周期快 5.7% 的原因。另外**取指 784650 条 vs 退休 581592 条：26% 的取指被冲刷丢弃**，把 icache 缺失次数顶高了 49%——错路径取指会真金白银地吃 miss 代价。

**下一步（按收益/面积排序）**：

1. **0 面积**：IFU 命中同拍交付（现在 `output.valid` 没算上 Idle+hit 那一拍，等于取指上限 0.5 条/拍）；Xbar 的 arbiter+crossbar 两级串联每次事务白等 1-2 拍；LSU/icache 的 `ar` 打拍（每访存 1 拍）
2. **减少错路径取指**：把分支解析提前（EXU 3 泡 → 1 泡），既省冲刷也直接减少 icache 缺失
3. **小面积（~300-600 µm²）**：1-entry 写缓冲，store 不再等 `b`（写 avg 18.95 cyc）
4. **D-Cache 的账**：按 icache 的 DSE 反推面积单价约 **50-58 µm²/Byte**（nangate45、寄存器堆实现），所以"能装下 microbench 工作集"的 D-Cache 远超 2000 µm²；但 **1~4 行（16~64B）的顺序预取缓冲**只要约 800~3700 µm²，对数组遍历型负载可能吃掉相当一部分读延迟（读 avg 103 cyc）。值不值先用 `tools/cachesim` 跑访存 trace 估命中率再定（方法同 icache 的 DSE）

### 追加：0 面积三件（Xbar 提前放行 / ar 组合发出 / 计数器语义）

| 改动 | 效果 |
|---|---|
| `Xbar`：`ar` 通道在 Idle 拍就组合给出 grant（`grantIfu/grantLsu`）、并组合解码地址（`routeClint/routeExternal`），不再等 `arbiterState`/`readState` 各注册一拍 | 每次读事务省 2 拍 |
| `LSU`/`icache` 的 AXI 控制器：`ar.valid/addr` 不再 `setAsReg`，改为 `readReq \|\| arValidReg` 组合发出，`arValidReg` 只负责把"已发出但未握手"的请求举住（AXI 要求 valid 保持到 ready） | 每次读事务再省 1 拍 |
| IDU 类别计数器：`io.input.valid` → `io.output.fire` | 恢复"指令条数占比"语义 |

**踩到的坑**：`arValidReg` 的保持分支一开始写成 `otherwise { arValidReg := False }`（应为 `True`）——于是"请求已发出、但总线被 icache 占着没握上手"时，`ar.valid` 下一拍就掉了，那笔事务永远不完成 → LSU 卡在 `WaitMem`，表现为 `ERROR: 一条指令超过10000周期未完成 (pc=0x30000048)`。**LSU 和 icache 两处都是这个写法**，改的时候要一起改。

| 指标 | 519cf4f | bbd3432 | 变化 |
|---|---|---|---|
| 总周期 | 14354926 | **13708413** | −4.5% |
| IPC | 0.0405 | **0.0424** | +4.7% |
| microbench Scored time | 6428.06 ms | **5964.00 ms** | −7.2% |
| icache 缺失 avg | 59.37 cyc | **54.31 cyc** | −8.5% |
| 取指次数 | 784650 | 817065 | +4.1% |
| 综合面积(nangate45) | 22307.29 µm² | **22520.62 µm²** | +0.96% |
| 综合频率(500MHz 目标) | 601.9 MHz | **587.8 MHz** | −2.3%（关键路径仍是 `ifu.icache.axi4Ctrler.io_readAddr`，slack 0.299ns） |

（`LSU mem wr avg` 18.95 → 20.84 是争用抖动：写路径这次没动。）

### 追加：EXU 的 pcNext 把比较器移出 32 位路径（纯时序优化，0 面积）

**问题**：`pcNext = Mux(pcAsrc, imm, 4) + Mux(pcBsrc, rs1, pc)`，而 `pcAsrc` 在条件分支时就是 ALU 的 `zero/less`——**晚到信号串在 32 位加法器前面**，等于两条 32 位进位链串联。STA 报告里 `pcNext` 的锥里出现 `rfReadData2` 就是证据：`pcNext` 的**值**本来不该依赖它（它只通过"是否成立"这 1 bit 影响"要不要跳"，而跳的目标恒为 `imm + (jalr ? rs1 : pc)`）。

**观察**：`(pcAsrc, pcBsrc)` 只有 3 种可用组合——`(1,0)`=jal/条件成立 → `imm+pc`；`(1,1)`=jalr → `(imm+rs1)` 清 bit0；`(0,0)`=不重定向 → **pcNext 是 don't care**（顺序取指由 IFU 自己 `+4`）。需要 pcNext 的两行 A 输入都是 `imm` → **A 直接接 imm，比较器只驱动 1 bit 的 `redirect.valid`**。

**改动**：

- `EXU`：`pcDataTmp = imm + Mux(pcBsrc, rs1, pc)`；清零合并进 bit0（`!pcBsrc && sum(0)`，连 32 位 mux 都省了）；删除 `Exu2Lsu_data.pcNext`
- `LSU`：fence.i 的目标改成本地算 `io.input.pc + 4`——**fence.i 是唯一 `pcAsrc=0` 但仍要重定向的指令，不补这处会重定向到 pc 自己 → 死循环**；删除 `Lsu2Wbu_data.pcNext`

| 指标 | bbd3432 | b06409e | 变化 |
|---|---|---|---|
| 总周期 / IPC | 13708413 / 0.0424 | 13708413 / 0.0424 | **逐项不变**（纯时序改动） |
| 综合面积(nangate45) | 22520.62 µm² | **22342.40 µm²** | −178 µm² |
| 最差 slack / 频率 | 0.299ns / 587.8 MHz | **0.648ns / 739.6 MHz** | **+25.8%** |

→ 余量从 2479 µm² 变成 **2657.6 µm²**（25000 − 22342.40），而且频率余量大幅拉开，后面加 dcache 时不必再担心时序。

### 追加：ALU 判零改走"操作数比较"（跳开 33 位减法链，频率 +21.5%）

`zeroFlag = (resultAdder === U"32'h0")` 要等 33 位进位链才出结果 ✗，而 `io.zero` 的**唯一消费者是 `BranchCond`**，它只在条件分支（`branch=100..111`）时被看，而那些指令的 `aluCtr` 全是减法（`0010/1010`）✓ —— 减法下 `a - b == 0 ⟺ a == b`，所以可以并行算：

```scala
val zeroFlag = (io.aluIn1 === io.aluIn2)      // 32 位 XOR 归约, 与减法链并行
```

| 指标 | b06409e | d1cdfe4 | 变化 |
|---|---|---|---|
| 总周期 / IPC | 13708413 / 0.0424 | 13708413 / 0.0424 | **逐项不变** |
| 综合面积(nangate45) | 22342.40 µm² | **22448.80 µm²** | +106 µm² |
| 频率 | 739.6 MHz | **898.8 MHz** | **+21.5%** |

**新的关键路径已经不在 CPU 核里了**：最差的是 `clint.io_clintAxi4_r_payload_data_*__reg_p:D`（1.082ns / slack 0.887ns）——CLINT 的读数据寄存器 `readData`，它的 D 锥是 `ar.addr`（Xbar 从 CPU 控制器**组合透传**过来），整条是：

```
CPU 地址生成 → Xbar 地址 mux → CLINT 地址解码/mux → readData 寄存器
```

这条路径只在读 `mtime` 时经过，不在 CPU 的性能环路上，而且 500MHz 目标早已满足（0.887ns 余量）→ **不值得为它花时间**（它下面的几条也都是 CLINT 的时钟门控 enable 路径）。余量现在是 **2551.2 µm²**（25000 − 22448.80）。

### 追加：CLINT 读通道寄存器化 + 计数器拆分，icache AR 恢复打拍（切开 Xbar/CLINT 长锥）

**CLINT**

- 读通道寄存器化（`addrReg`/`dataReg`/`dataFinish`），响应从 AR+1 变成 **AR+2** —— 设备读只有 662 次，多 1 拍无所谓
- 64 位计数器拆成两个 32 位：`timeCountLow` 每拍 +1，`timeCountHigh` 只在低位全 1 时 +1 → 砍掉原来那条 **1.088ns 的 64 位进位链**；语义与单计数器完全一致（回绕相位、先读低再读高的快照都对）
- **功能验证**：与拆分前的运行结果**逐位相同**（Scored/Total/cycle/inst 全同）✓；另外 `am-tests` 的 **rtc** 用例 uptime 每秒 +1 ✓

**icache**：突发控制器的 `ar.valid/addr` 恢复 `setAsReg` 打拍（回退之前的"组合发 ar"）

这样交给 Xbar 的是**寄存器信号** ✓，一次切断这一族长组合锥：

```
icache: tagMem 读 mux + tag 比较 → hit → enterMiss → ar.valid
   → Xbar 的地址 mux / isClint 解码 → CLINT 的 ar.fire → readCnt/readActive/dataFinish
```

其中 CLINT 那半截其实是**不可敏化的假路径** ✗（IFU 的取指地址永远不会落在 CLINT 的 `0x0200_0000` 段，只是 STA 做静态图分析时看不出这个相关性），所以只靠"在 Xbar 或 CLINT 里加寄存器"救不了 ✗——必须在**源头**（icache 的 AR）切掉 ✓。

| 指标 | d1cdfe4 | 92a825d | 变化 |
|---|---|---|---|
| 总周期 | 13708413 | **13822167** | +0.78% ✗ |
| IPC | 0.0424 | **0.0421** | ✗ |
| Scored time | 5967.06 ms | **6058.02 ms** | +1.5% ✗ |
| icache 缺失 avg | 54.31 cyc | **59.69 cyc** | +5.4 ✗（AR 晚一拍更容易在 Xbar 输给 LSU）|
| 面积 | 22448.80 µm² | **22364.75 µm²** | −84 ✓ |
| 最差路径 | `clint.readCnt` 1.085ns | **`ifu.state_0` 0.964ns** | 假路径族消失 ✓ |
| 频率 | 898.8 MHz | **993.6 MHz** | **+12%** ✓ |

**代价比预估大**（预期 +0.52%，实测 +0.78%，多出来的部分就是 `缺失avg` 涨的那几拍），但常被流水线停顿吸收 ✓（总增幅 < 缺失次数 × 5.4）。

**真实瓶颈第一次暴露出来** —— 不再是 cache/CLINT，而是核内：

- `exu.io_input_payload_rfReadData{1,2}_*`（0.951~0.957ns）：**IDU 的 RF 读 + 三级前递 mux** → IDU→EXU 寄存器
- `ifu.state_0`（0.964ns）：IFU 的次态逻辑（重定向 / 响应 / Done 的 mux）

评分口径是周期 + 面积、频率只要求"500MHz 通过" ✓ → 这 0.78% 周期在评分上是净亏 ✗；保留它的理由是频率余量（994MHz ≈ 2 倍）和"把假路径清出报告、让真瓶颈可见" ✓。只在乎评分的话，可以**只回退 icache 那一处**，保留 CLINT 的寄存器化与计数器拆分。

### 追加：同步复位（项目要求）+ mcycle/minstret 拆两个 32 位 + 去掉可省的复位

**改法只有一行**（`playground/Config.scala`）：

```scala
defaultConfigForClockDomains = ClockDomainConfig(
  resetKind = SYNC,            // SpinalHDL 默认是 ASYNC
  resetActiveLevel = HIGH)
```

**两个层面的验证**：生成 RTL 里 `or posedge reset` 从有到 **0 处** ✓；网表里 `DFFR_X1`(579) + `DFFS_X1`(9) **全部消失** ✓，只剩 `DFF_X1`(3036) ✓。
**功能验证**：`make perf` 的周期/指令数/全部 PERF 计数器与 `92a825d` **逐位相同** ✓（复位只在上电生效 ✓）。

**对照实验**（只把 `resetKind` 改回 `ASYNC`，其他一字不动）：

| 配置 | 面积 | 最差路径 | 频率 | 500MHz slack |
|---|---|---|---|---|
| `92a825d`（ASYNC，未清理）| 22,364.75 | 0.964ns | 993.6 MHz | +0.994ns |
| **本次（SYNC + 清理）** | **22,478.33** | **1.387ns** | **698.6 MHz** | **+0.569ns ✓ 达标** |
| 对照（ASYNC + 同样清理）| 22,625.43 | 1.029ns | 929.2 MHz | +0.924ns |

**两条结论**：

1. **同步复位的代价是"关键路径 +0.36ns"，而面积反而更小（−147 µm²）** ✓ —— 复位变成"参与数据路径的高扇出信号"后，每个带复位触发器 D 侧多一层 mux、且要像普通信号一样满足建立时间；异步复位走专用复位脚、无 D 侧逻辑 ✓。**面积与关键路径是两本独立的账，减少线不保证缩短路径** ✓。
2. 同步复位**把一条本来不是最长的路径顶成了最长** ✓：ASYNC 版榜首是 `ifu.rdataReg_31`（1.029ns）✓，SYNC 版榜首变成 EXU 的 imm/branch/pcNext 锥（1.387ns）✗。

**去掉可省的复位**（规则："上电后可能被读、而此前从未被写"的才必须保留）：

| 去掉 | icache: `lineReg`(128b)/`wordCnt`/`wordSelReg`/`pcReg`/`indexReg`/`tagReg`；IFU: `pcOfReq`；LSU: `rdataReg`；CLINT: `readLen`/`readCnt`/`timeCountHighSnap` |
|---|---|
| **必须保留** | 各状态机（LSU/IFU/icache/Xbar）、流水线 valid、AXI 握手 valid、icache 行有效位、CSR 的 `mstatus/mtvec/mcause`（规范要求复位值）、CLINT 的 `timeCountLow/High`（项目要求初始为 0）；`mepc` 例外——它只被 `mret` 读，而 `mret` 之前必有 trap 写入 ✓ |

这一轮清理把同步复位的面积代价从 **+502 µm² 压到 +114 µm²**（相对 `92a825d`）✓。

**注意**：报告里的"可达频率"会随"复位种类 + 再映射"大幅跳动（这一路走过 587→739→898→993→799→698 MHz，而期间周期数几乎没变 ✗），**判断标准应看 500MHz 目标下的 slack 是否为正** ✓，不要只看这个数 ✓。

### 追加：IFU 遇无条件跳转立即停取指（周期 −6.1%，面积/时序还略好）

**思路** ✓：`jal`/`jalr` **必然重定向** → 它们之后顺序取的指令 **100% 会被丢弃** ✗；而这些错路径取指**既自己缺失、又污染 cache** ✗。所以在 IFU 里对"刚取到的指令"预译码（只看 opcode：jal=`1101111` ✓ / jalr=`1100111` ✓），一发现就关掉取指闸门，直到重定向重启前端 ✓。

```scala
val instrOut  = Mux(icache.io.rspOut.valid, icache.io.rspOut.rdata, rdataReg)   // = io.output.instr
val isJump    = (instrOut === M"-------------------------1101111") ||           // jal
                (instrOut === M"-----------------000-----1100111")              // jalr
val stopFetch = RegInit(False)
when(io.redirect.valid)              { stopFetch := False }
.elsewhen(io.output.valid && isJump) { stopFetch := True  }                     // ★ 必须用 io.output.valid 限定
icache.io.reqIn.valid := (state === IfuState.Idle) && rstEnd && !stopFetch
```

**实测** ✓：周期 13,822,167 → **12,975,343（−6.13%）**、IPC 0.0421 → **0.0448**、Scored time 6058 → **5203 ms**；IFU 取指 797,102 → **769,871**、icache 缺失 70,893 → **52,810（−25.5%）**、命中率 91.11% → **93.14%**；LSU 读写**完全不变** ✓；面积 22,478.33 → **22,460.24 µm²**、500MHz slack +0.569 → **+0.711ns** —— **三个指标全赢** ✓✓。

**为什么缺失降得比取指降得多** ✓：减少的 27,231 次取指里有 **66%（18,083）会 miss** ✗ —— 错路径取指既自己缺失，又挤掉有用的行 ✓，所以一个闸门吃到两份收益 ✓（原先估"只有 8% 会 miss"是错的 ✗）。

**★ 死锁坑（第一版）** ✗✗：置位条件**必须**用 `io.output.valid` 限定 ✓。`icache.io.rspOut.valid = (reqIn.fire && hit) || missDone` ✓，其中 `missDone` **会迟到** ✗：若重定向发生在缺失填充期间，填充完成时 IFU 已回到 Idle、等的是新 pc，这条"已作废"的响应仍会拉高 `rspOut.valid` ✗。只看 `rspOut.valid && isJump` 时，一条**废弃路径上的 jalr** 就会把 `stopFetch` **永久置起** ✗（它只被 redirect 清 ✓）→ 前端从此不再取指 → **死锁** ✓（表现为 `一条指令超过10000周期未完成 (pc=0xa0005668)` ✓ —— 反汇编出来那条正是 `ret` = `jalr x0,0(x1)` ✓✓）。`io.output.valid` 里 missDone 那一项要求 `state===WaitMem` ✓，正好排除这种迟到响应 ✓✓。

**剩余空间** ✓：现在 52,810 次缺失 vs 按退休 PC 算的理想值 47,610 → **多余的只剩约 5,200 次（10%）** ✗，所以"把闸门扩展到条件分支"收益已不大（约 2% ✓）；下一刀应转向**顺序预取**（fetch 侧 ✓）或 sectored dcache（load 侧 ✓）。

## 2026-08-18 更新

- `ebreak` 不再通过 DPI-C 直接结束仿真，改为可综合的异常处理：`mcause` 置为 3。
- 修正 `csrrw/csrrs` 写回：CSR 指令现在可以正常写回 `rd`。
- RegFile 接口整理为 `readBus` / `writeBus` 两组总线，IDU 和 WBU 使用同一组 `IMasterSlave` 定义。
- WBU 调整为流水线最后一级：输入改为 `Flow`，级间寄存逻辑由顶层 `pipelineConnectLast` 统一完成。
- 新增 `CpuConfig` 平台配置：
  - `CpuConfig.npc`：复位 PC 为 `0x80000000`；
  - `CpuConfig.ysyxSoc`：复位 PC 为 `0x30000000`；
  - 预留乘法、除法、中断等后续选项开关。
- IFU 改为只接收 `resetPc`，避免直接依赖完整 `CpuConfig`。
- CLINT 使用 64 位计时器，读取高位时保存低位快照，保证高低位读取一致。

## 2026-08-19 更新

- 新增模拟外设 **键盘(Keyboard)** 与 **GPU(VGA)**，与 ysyxsoc 平台对齐：
  - 硬件模拟位于 `csrc/device/`，通过 `pmem.c` 按地址分发，SDL2 实现输入与显示；
  - 驱动位于 `abstract-machine/am/src/riscv/npc/`（`input.c` / `gpu.c`）。
- 地址映射：
  - `0x10000000` 串口输出；
  - `0x10011000` 键盘状态（bit15=keydown，低 15 位为 AM keycode）；
  - `0x02000000` RTC 时间（64 位 us）；
  - `0x21000000` VGA 显存（640x480，32bpp）；
  - `0x21200000` VGACTL（读 [0]=wh，写 [4]=sync 触发屏幕刷新）。
- 窗口关闭（SDL_QUIT）会以 `NPC_QUIT` 状态正常结束仿真。

## 2026-08-19 总线互连与 CLINT 重构

- **Xbar 重构（方案 A）**：`crossState` 拆分为 `readState` / `writeState` 两个独立状态机，读事务（ar/r）与写事务（aw/w/b）可并行，互不阻塞。
- **地址映射宏定义**：`Xbar.scala` 顶部新增 `AddressMap`（`CLINT_BASE` / `CLINT_SIZE` / `CLINT_MTIME` / `CLINT_MTIMEH` / `isClint`），clint 地址集中配置，修改只动一处。
- **CLINT 独立成文件**：`CLINT.scala`，寄存器地址（mtime 高低位）由 `AddressMap` 派生，可配置。
- **CLINT 双向快照**：读任一侧都同时锁存高低位快照，任意读取顺序下高低位保持一致。
- **CLINT / Axi4MemSlave 支持突发读**：按 `len` 计数，`r.last` 在最后一拍拉高（不再假设 len=0）；`readCnt`/`readActive` 采用单一赋值源（when-elsewhen）写法。
- **Axi4MemSlave 支持 INCR 突发读**：地址按 `base + cnt<<size` 递增，len=0 时不发多余越界读。
- **NPC_TOP 封装 AXI 从机模块**：`ysyx_23060082_Axi4MemSlave`（NpcMemRW 黑盒 + 握手/响应），响应信号显式赋值（`r.resp`/`b.resp` = `Axi4.resp.OKAY`、`r.last`、`r.id`/`b.id` 回传）。
- **IFU/LSU 控制器**：新增 resp 错误检查（非 OKAY 时仿真 `report`）；`readEnd` 规范为 `r.fire && r.last`。
- **resp 统一使用库枚举** `Axi4.resp.OKAY`，不再写裸 `B"00"`。


## 2026-08-20 更新

- **mcycle / minstret 64 位计数器**：`mcycle` 每周期 +1、`minstret` 每条退休指令 +1；支持读写（用于调试/性能统计）。
- **64 位计数器读取协议统一为"先读低再读高"**（mcycle 与 CLINT mtime 一致）：
  - 读低位时硬件锁存当时的高位，读高位返回锁存值，保证组合的 64 位一致；
  - CLINT 由双向快照改为单向"先低后高"（逻辑更简单，与 mcycle 统一）；
  - AM 驱动（npc / ysyxsoc 的 `timer.c`）同步改为先读低再读高。
- **CSR 写回旧值用实时寄存器值**（`rdataWb`），避免读高时把锁存值写回计数器导致回退。
- **命名统一驼峰**：`csr_addr/csr_wdata/csr_rdata/pc_in/cause_in` → `csrAddr/csrWdata/...`；`pc_next` → `pcNext`；`rf_ctrl/mem_ctrl/csr_ctrl` → `rfCtrl/memCtrl/csrCtrl`；`type_U` → `typeU`；`pc_asrc/pc_bsrc` → `pcAsrc/pcBsrc`。
- **itrace 指令退休追踪**：
  - 新增 `ItraceReg` 黑盒（`vsrc/dpi-c.v`）：记录 WBU 退休指令的 `pc/instr/valid`，C++ 通过 Verilator rootp 层次指针直接读取；
  - `enableSimDebug` 条件生成（`CpuConfig` 开关，npc/ysyxSoc 均开启，STA 时关闭），不加顶层端口，综合零开销；
  - `exec_once` 改为"执行一条指令"粒度：`do-while` 循环周期直到 `itraceRetireValid`，`ITRACE_TIMEOUT_CYCLE`（默认 5000，可配置）上限防卡死；
  - 状态更新改用退休指令的 PC；新增 `g_nr_guest_inst` 计数与 CPI 统计；
  - `iringbuf` 修复：按时间顺序（从最旧到最新）打印环形缓冲，最新一条标 `-->`；
  - `trace_and_difftest` 承担 trace 生成与输出，`exec_once` 保持纯执行（职责分离）。
- **mcycle 测试程序**：`am-kernels/tests/cpu-tests/tests/mcycle.c`（验证递增、快照一致性、低 32 位回绕进位）。
- **Makefile 清理**：移除无用的 `RESET_PC`（复位地址由 `CpuConfig.npc/ysyxSoc` 平台配置统一管理）。

## 2026-08-30 平台统一与调试基础设施完善

### 平台统一（npc / ysyxsoc）
- **复位地址统一**：npc 与 ysyxsoc 复位地址统一为 `0x30000000`（flash 基址）；`CONFIG_MBASE` 同步统一。
- **CpuConfig 合并**：不再区分 `CpuConfig.npc/ysyxSoc`，单一 `CpuConfig`（`resetPc` 默认 `0x30000000`、`enableSimDebug` 默认 true；综合/STA 显式关闭）。
- **RTC 统一走 CLINT**：`RTC_ADDR` 改为 `0x02000000`（CLINT mtime），与 ysyxsoc 一致。

### trace 模块化与完善（`csrc/trace/`）
- **itrace**：指令退休追踪 + ringbuf，单步（`n <= MAX_INST_TO_PRINT`）打印反汇编；模块独立。
- **ftrace**：`-f <elf>` 解析符号，函数调用统计 + CALL/RET 缩进序列（`-> 调用 / ret 返回`）；修复返回弹出 bug（调用者误弹导致 main 计数虚高）；`CONFIG_FTRACE_FILTER_INTERNAL` 过滤 `__` 内部函数。
- **mtrace**：截取 `lsuAxi4`（只记数据访存，不含取指），按统一地址映射分类（flash/psram/sdram/sram + uart/spi/gpio/ps2/vga/clint 等）；`CONFIG_MTRACE_PC` 记录访存指令 pc；单步时灰色缩进显示；**支持 ysyxsoc**（读 `ysyxSoCFull` 的 mtraceReg）。

### SDB / 表达式 / 监视点
- **expr 修复**：`make_token` 缺 `break`（表达式一直不可用的根因）、负数/指针越界、TK_NEG 取负 UB、sscanf 返回值检查、tokens 数组缩小、风格统一。
- **watchpoint 接线**：`w <expr>` / `d <no>` / `info w` 全可用（修复 cmd_d 的 strtok 崩溃、create 的 strcpy 越界）。
- **menuconfig**：`make menuconfig` 终端图形配置界面（空格切换、依赖灰显、显示宽度对齐）。

### 仿真 / 综合
- **sdram DPI-C 化**（ysyxSoC 项目）：4 片 sdram（`chip` 端口区分 0-3）存储移入 C 侧（`sdram_mem_read/write`），sdram.v 保留命令时序逻辑。
- **SPINAL_SIM_DEBUG**：`make sta` 自动以 `ysyx_23060082` + `SPINAL_SIM_DEBUG=0` 生成综合版（不含 itrace/mtrace 黑盒）到 `build/sta/`。
- **EXU 输出改名**：`Exu2Lsu_data.imm` → `csrAddr`（仅用于 CSR 寻址，避免与真正立即数混淆）。

### 性能计数器（PerfReg，统一 enableSimDebug 控制）
- **分散到各模块**（信号就近，STA 零成本排除）：
  - **IDU/decoder**：指令类别统计（calc/mem/branch/jump/csr/sys/other + 总数）——decoder 类别端口**条件生成**（`val isCalc = if (enableSimDebug) out Bool() else null`，STA 时 null 无端口）；
  - **EXU**：运算周期（`isCalc && willValid`，单周期 1:1，isCalc 由 IDU 经 `Idu2Exu_data` 条件字段传入）；
  - **IFU**：取指延迟（ar→r 配对）；
  - **LSU**：访存延迟 4 组（mem/dev × rd/wr），`pendingIsDev` 寄存器在请求拍锁存类别，响应拍正确配对（修复 r/b 无法区分请求来源的 bug）。
- **PerfReg 黑盒**（dpi-c.v）：4 组延迟测量 + 8 个事件计数 + 全局周期，各模块 `enableSimDebug` 条件实例化。
- **C 侧 `csrc/trace/perf.c`**：双平台（PERF_F 宏）读取，输出 IFU/LSU 延迟与平均、EXU 计算周期、指令类别占比（npc：flash 快速 1 cyc；ysyxsoc：SDRAM 延迟真实可见）。
- **statistic() 统一统计**：CPI/仿真频率（Log）+ PERF 表格（Log_nohead，名称 17 列、cnt/total 10 位、avg 7 位、边框等宽对齐）；mtrace/ftrace 统计也并入。

### Makefile 修复
- **build_target 编译/运行改 `&&`**：verilator 编译失败或 VNPC_TOP 运行失败立即退出，不再执行后续（修复 `;` 分隔不检查退出码、echo 成功误判的问题）。
- **make sta 的 RTL_FILES 改绝对路径**：`make -C yosys-sta` 切换目录后相对路径失效的 bug。

## 2026-08-31 性能评估体系与 icache

### 性能评估
- **`make perf`**：运行 microbench（test 规模），输出仿真周期/指令数/IPC + PERF 计数器表格——配合 `git checkout <commit>` 可复现历史性能；注意用 `sim` 而非 `run`（run 会触发 nvboard 永不退出）。
- **`PERF.md`**：性能记录表（commit/说明/周期/指令/IPC/综合频率/面积/各计数器），首次记录 `ce442d4`（IPC 0.0186、最高频率 428.8MHz @500MHz目标违例、面积 19949µm²）。
- **校准访存延迟分析**（文档 B3）：`r = CPU频率/设备频率`（如 yzh 例 1.2GHz/100MHz → r=12），延迟模块计算 `c = k*r`（k 为设备侧周期）；`apb_delayer.v` 的 `R×S` 定点实现（`MUL=3.76×128`、`>>7` 还原）与文档方案一致。

### SDRAM 走 AXI（支持突发）
- `ysyxSoC/src/Top.scala` 的 `sdramUseAXI = true`——SDRAM 改用 AXI 接口（支持突发），为 icache 突发读取做准备。
- `axi4_delayer.v` 数据宽度 64→32 修正（与 32 位 xbar 匹配，消除 WIDTHEXPAND）。
- **突发支持检查**：`sdram.v`（芯片模型）支持突发（BURST_LENGTH 可配、连续读写）；`sdram_axi_core`（控制器）**不支持**（MODE_REG 配 `burst=1`、`inport_len` 未实现、读状态机单次）——真正突发需改 core。
- 模块分层：`sdram_axi`（AXI4 顶层）→ `sdram_axi_pmem`（AXI 事务引擎/突发地址计算/FIFO）→ `sdram_axi_core`（SDRAM 命令时序）。

### icache（简易指令缓存，`playground/src/icache.scala`）
- **直接映射、寄存器实现、参数化**（`IcacheParams`：块大小/块数）。
- **接口**：`reqIn`（Stream 取指请求 pc）+ `rspOut`（Flow 指令返回 rdata/valid 完成信号）+ `axi4`（Axi4ReadOnly，缺失访存）。
- **1 拍命中**（组合判断 valid+tag）；缺失进入 Miss 状态，**复用 ReadOnly AXI 控制器**读回并写回（valid/tag/data）；请求锁存防 Miss 期间变化。
- **已接入 IFU**：IFU 发取指请求（`reqIn` Stream）、等指令返回（`rspOut` Flow）；只读 AXI 控制器内嵌 icache；`fenceI` 链路（decoder→WBU→IFU→icache 清有效位）完成。

## 2026-09-10 综合工艺切换、面积优化探索、icache 性能计数器与 AXI 延迟校准

### 综合流程：引入 `STA_PDK`
- **`make sta` 支持切换工艺**：Makefile 新增 `STA_PDK`（默认 **nangate45**，即教程基准工艺、面积限制的判定工艺），sta 目标把 `PDK` 传给 yosys-sta；`make sta STA_PDK=icsprout55` 可临时切到流片工艺。
- **两工艺面积对比（同一份 RTL，16 块×4B icache）**：nangate45 **19421.72 µm²** / icsprout55 23590.28 µm²——**nangate45 小约 30%**。面积数字**不可跨工艺比较**，PERF 表已注明各行工艺。
- perf 目标不再显式传 `AM_HOME/NPC_HOME`（已在 shell 环境中）。

### 面积优化探索（附实测结论，均已回退或按需保留）
- **`pcNext` → `pcOrNext` 合并**（EXU/LSU，**保留**）：trapEnter 类指令（ecall/ebreak/illegal）时 `pcNext` 必然无用，该字段此时改传 `pc` 供 CSR 写 `mepc`——省 32 位级间寄存器（顺序逻辑 −197 µm²）。
- **`pc` 改为"仅仿真字段"**（`Exu2Lsu_data` / `Lsu2Wbu_data`，**保留**）：`if (config.enableSimDebug) UInt(32 bits) else null`，STA 时不生成该字段，仿真下 itrace 仍可读。
- **ALU 桶形移位器合并 → 已回退**：手写单移位器（位反转复用）经受控实验实测**反而更大**（独立综合 1072.96 → 1118.60，+46 µm²）。原因：ABC 早已共享了三个移位表达式的 mux 树（三者合计仅 717.6 µm²）。结论——**综合器已做的"共享"类优化，RTL 再合并无收益**。
- **icache 寄存器写法实验 → 已回退**：`indexReg/tagReg` 改成 `pcReg` 位切片后触发器数量完全相同（30 位 vs 30 位），面积差异纯属 ABC 全局映射抖动。
- **重要经验**：该流程存在 **~±50 µm²/模块、~±100 µm² 总量**的映射抖动（RTL 未改动的模块面积也会变），因此 **< 100 µm² 的优化无法验证**；能真正省面积的只有"删触发器/减存储条目"这类综合器动不了的改动。

### icache 命中率 / 缺失代价计数器
- **icache 新增两个调试输出**：`io.miss`（缺失脉冲，即 `enterMiss`）、`io.missDone`（缺失完成脉冲）。STA 时无人使用被完全剪掉，**综合面积零增量**（实测 19421.724，与改动前逐位一致）。
- **复用 IFU 现有 PerfReg 的空闲槽位**（未新增黑盒实例）：`evt(2)` = 缺失次数，延迟组 1（`req(1)`=miss / `rsp(1)`=missDone）= 缺失起止配对。
- **`perf.c` 新增两行**：`Icache access`（访问次数 / 缺失次数 / 命中率）、`Icache miss`（平均缺失代价，即文档 TMT 中的那一项）；第三列用 `%-11` 补齐以与其他行对齐。
- **自检**：`evtCnt2`（来自 `miss`）与 `dlyCnt1`（来自 `missDone`）是两条独立路径，计数一致即可证明配对无误。
- **microbench(test) 实测**：命中率 **59.22%**、平均缺失代价 **31.79 cyc**；icache 缺失总周期 7548226，占 20073896 总周期的 **37.6%**。

### AXI 访存延迟校准（ysyxSoC `perip/amba/axi4_delayer.v`）
- **设计依据**：延迟模块测的是 **arvalid(计时起点 t0) → rvalid** 的整段时间（**含等待 arready 握手的时间**），乘倍率后把 rvalid 延迟交付给 CPU，即 `(t_k − t0) × R = t_k' − t0`。
- **读通道**：状态机 `wait_t/cnt_t/delay_t/out_t`；`cnt_t` 每拍 `counter += MUL`（`MUL = R×S` 定点实现小数倍率），`out_rvalid` 到达那拍 `counter >>= S_LENGTH` 得到延迟量，倒数到 0 后把 rvalid 交给 CPU。
- **写通道**：AXI 写事务无论多少拍**只有一个 B 响应**，因此不需要 FIFO——`b_pending` 标记 + `w_now >= b_tgt` 到点交付即可；`t0 = awvalid || wvalid`。
- **AR/W 必须直通**（若延迟会把延迟算两次）；`R` 的物理含义是**处理器与设备的频率比**（教程例：flash 在 SPI master 花 150 周期 × r=12 → 等 1800 周期）。
- **待办**：突发读需按"每拍各自的绝对交付时刻"扩展为 FIFO（当前单拍结构会让设备被 CPU 反压，破坏节拍时序）；突发写（含 W 节拍间隔）留待 dcache 阶段。
- **相关事实**：**flash 挂在 APB 上（APBSPI，不走 AXI）**，因此**不支持突发**；`AXI4Fragmenter` 会把发往 flash 的突发自动拆成单拍 APB 事务。flash/sram 的 SDRAM 之外路径由已有的 `apb_delayer.v`（R=3.76）校准，与本 AXI 延迟模块互不影响。

### cachesim（icache 功能模拟器，`tools/cachesim/`）
- **完全独立的 C 程序**（只依赖 libc，不参与 NPC 构建流程）：回放取指 PC 序列，只维护元数据（valid/tag/替换信息）统计缺失次数——不模拟数据、不执行指令，因此比 RTL 仿真相差几千倍。
- 支持直接映射 / 组相联（`--ways`）、`lru|fifo|rand` 替换、`fence.i` 清空、`--from` 排除 boot 阶段、`--misscost` 直接算 TMT。
- **输出单行 `RESULT:`**，便于脚本并行扫描参数组合（`npc/tools/cachesim/README.md`）。
- **与 RTL 对拍（性能 DiffTest）完全通过**：

  | 程序 | RTL（`make perf` 的 `Icache access`） | cachesim |
  |---|---|---|
  | `shift` | 4820 访问 / 829 缺失 / 82.80% | 完全一致 |
  | microbench(test) | 582164 访问 / 237431 缺失 / 59.22% | 完全一致 |

- **trace 来源修正**（`include/config.h` / `csrc/sdb/log.c`）：
  - `-l <file>` 要真正写出文件，必须打开 **`CONFIG_LOG`**——`log_write()` 被 `IFDEF(CONFIG_LOG, ...)` 包住，否则 `-l` 指定的文件会**一直为空**；
  - `CONFIG_TRACE_END = 0` 现在表示**不限制上限**（一直记到程序结束）。
- **DSE 结论（microbench test 规模，缺失次数）**：**等容量下加大块远优于加行数**——
  - 64B：`16行×4B` = 237431 vs **`8行×8B` = 131178（−45%）**
  - 128B：`32行×4B` = 126292 vs **`8行×16B` = 47610（−62%）**
  - `8行×16B`（128B）的 47610 **少于** `64行×4B`（256B）的 68813——**容量翻倍也补不回块大小的差距**
  - 而且大块**更省面积**（tag 数量随行数下降）：64B 容量下 `16行×4B` = 944 触发器 vs `8行×8B` = 728 触发器
  - **前提**：大块必须配合**突发传输**，否则缺失代价会按块大小线性上涨（文档「优化缺失代价」一节）

## 2026-09-12 icache 8 字节块与突发传输、Xbar 与 SDRAM 模型的连环修复

### icache：`16行×4B` → `8行×8B`（带突发）

**动机**：cachesim 的 DSE 结论——等容量下加大块远优于加行数（64B 容量：`16行×4B` 237431 缺失 vs `8行×8B` 131178 缺失，触发器还少 216 个）

**改动**：
- `IcacheParams` 增加派生常量（`lineBits`/`indexBits`/`tagBits`/`words`/`wordBits`/`dataBits`），供 icache 与 AXI 控制器共用
- `dataMem` 按 `lineBytes` 变宽（每行 `lineBytes*8` 位）
- 新增 **`ysyx_23060082_Axi4_Ctrler_ReadOnly_Burst`**：控制器内完成**突发读 + 逐拍拼装**（`ar.len = words-1`、`ar.size = BYTE_4`、`ar.burst = INCR`），对 icache 仍只暴露"一次读回一整行"
- 命中时按 `pc` 的块内字偏移选字；**缺失时按"请求时锁存的字偏移"选字**（不是最后读到的那拍 ✗ 这是最容易错的地方）
- 缺失地址按**块**对齐（`pc(31 downto lineBits)`）

**性能**（microbench test）：

| 指标 | 16行×4B | 8行×8B | 变化 |
|---|---|---|---|
| 缺失次数 | 237431 | 131149 | **−44.8%** |
| 命中率 | 59.22% | 77.46% | +18.2 pt |
| 平均缺失代价 | 31.79 cyc | 40.64 cyc | +27.8%（一行 2 拍）|
| TMT | 7548226 | **5329394** | **−29.4%** |
| 总周期 | 20073896 | **17797665** | **−11.3%** |
| microbench Scored time | 10860.01 ms | **8985.01 ms** | **−17.3%** |

**与 cachesim 对拍**：访问次数完全一致（581747）；缺失 RTL 131149 vs cachesim 131147，**差 2**——因为 cachesim 在 `fence.i` **被取到那一拍**就清空，而 RTL 里 `fenceI` 是 **WBU 退休时**才发给 icache（中间 IFU 多取了几条）。每条 `fence.i` 多算 1 次，正好 2 次 ✓

### 连环 bug 1：Xbar 用 `r.fire` 结束读事务 → 突发第二拍被吞

`Xbar.scala` 的**仲裁器**和**读状态机**都在第一个响应拍就回 Idle：

```scala
when(io.ifuAxi4.r.fire) { arbiterState := ArbiterState.Idle }   // ✗ 没看 r.last
when(busAxi4.r.fire)    { readState    := CrossState.Idle }     // ✗ 没看 r.last
```

而 `r.valid` **被这两个状态门控**（`r.payload` 却无条件透传 ✗）——于是第 1 拍正常、状态机回 Idle、**第 2 拍的 `rvalid` 被门控掉** ✗，但 `r.last` 仍然透传 → 波形上表现为"**rlast 高而 rvalid 低**"

**修法**：3 处改成 `r.fire && r.last`（仲裁器 2 处 + 读状态机 1 处）；写通道 `b.fire` 不用改（AXI 写事务只有一个 B 响应 ✓）

### 连环 bug 2：SDRAM 芯片模型优先级反了 → 突发第二拍全 0

`perip/sdram/sdram.v` 的判断链把"**突发结束**"排在了"**新命令**"前面：

```verilog
else if(data_count >= BURST_LENGTH) rw_state <= 2'b00;   // ★ 抢在新命令之前 ✗
...
else if(cmd == 3'b101)              rw_state <= 2'b10;
```

BL=1 时 `data_count` 会在读出数据后停在 1，于是**背靠背的第 2 个 READ 被当成"突发结束"吞掉** ✗——`rw_state` 回 idle、`col_address` 被冲成 0、`read_data` 被清零 → `dq` 变 `z` → 控制器采到 **0** ✗

**现象**：flash 取指正常，一进 SDRAM 就"**奇数 32 位字全 0**"（`0xa0000004: 00 00 00 00  c.unimp`）

**修法**：**统一规则——命令优先，突发结束放最后**（`rw_state` / `col_address` / `data_count` 三处保持一致）

### SDRAM 模型的整理（行为不变，perf 逐位一致）

- `cmd` 具名化：`CMD_LOAD_MODE`/`CMD_REFRESH`/`CMD_PRECHARGE`/`CMD_ACTIVE`/`CMD_WRITE`/`CMD_READ`/`CMD_BURST_TERM`/`CMD_NOP`（3 位；与 `sdram_axi_core.v` 的 4 位 `CMD_*` 一一对应）
- Mode 字段具名化：`MODE_CAS_2/3`、`MODE_BL_1/2/4/8`——注意同一个 `3'b010` 在 CAS 与 BL 两个 case 里含义不同 ✗，具名后不再混淆
- `rw_state` 改用文件里已有的 `state_t` 枚举（顺带修正 `[2:0]` → `[1:0]` 的位宽不一致）
- `cs` 守卫在 9 个 `always` 块里统一补回
- `data_count`：命令拍直接置 1（原来用 `+1` 会累积 ✗）、读命令显式清 0
- `delay_count`：互斥分支重写，读命令显式起拍
- `read_data`：第一个条件补上 `rw_state == read_delay_t` 状态守卫

### 遗留

- ⚠️ **`sdram.v` 有一个隐含约束**：两次读命令间隔必须 ≥ `CAS_Latency` 拍（这个模型**不流水化**读命令，间隔不足时前一次读会**拿到错列的数据**，而不是报错 ✗）。现在靠 core 的 2 拍间隔 + CAS=2 侥幸错开——**建议加一条断言**把它变成显式报错
- **SDRAM 的突发不靠颗粒突发模式**：`MODE_REG` 配的是 **BL=1、CAS=2**，而 `sdram_axi_core` 里 `inport_len_i` **只声明未使用**——突发是在 `sdram_axi_pmem.v` 层被拆成"一拍一个 core 命令"实现的。所以 8B 块是 **2 次独立的 SDRAM 访问**（缺失代价 +27.8% 的来源）；想再降代价，要么改 `MODE_REG` 让颗粒进 BL=2，要么让 core 真正支持 `inport_len`

### 块大小/行数 DSE：cachesim 预测 → RTL 实测

先用 cachesim（`tools/cachesim/`）扫参数，再把选定配置落到 RTL 实测：

| 配置 | 容量 | cachesim 缺失 | **RTL 缺失** | **命中率** | **面积(µm²)** | **频率(MHz)** | **TMT** | **总周期** | **Scored(ms)** |
|---|---|---|---|---|---|---|---|---|---|
| `16行×4B` | 64B | 237431 | 237431 | 59.22% | 19421.72 | 519.0 | 7548226 | 20073896 | 10860.01 |
| `8行×8B` | 64B | 131178 | 131149 | 77.46% | 18563.87 | 573.3 | 5329394 | 17797665 | 8985.01 |
| **`8行×16B`** | **128B** | 47610 | **47522** | **91.83%** | **21735.39** | **623.2** | **2866587** | **15228461** | **6662.01** |
| `16行×8B` | 128B | 70737 | 70685 | 87.85% | **22276.70** | **599.1** | 3007449 | 15389954 | 6761.04 |

**等容量（128B）两组正面对比**：

| | `16行×8B` | **`8行×16B`** |
|---|---|---|
| 命中率 | 87.85% | **91.83%** ✓ |
| 缺失次数 | 70685 | **47522**（−32.8%）✓ |
| 平均缺失代价 | 42.55 cyc | 60.32 cyc（一行 4 拍 vs 2 拍）|
| TMT | 3007449 | **2866587**（−4.7%）✓ |
| 总周期 | 15389954 | **15228461**（−1.0%）✓ |
| 面积 | 22276.70（实测 ✓）| **21735.39**（实测 ✓）|
| Scored time | 6761.04 ms | **6662.01 ms** ✓ |
| 可达频率 | 599.1 MHz | **623.2 MHz** ✓ |
| 突发拍数 | 2 拍 | 4 拍（**已验证可行** ✓）|

**要点**：
- **等容量下加大块远优于加行数** ✓ —— `8行×16B` 的缺失比 `16行×8B` 少 **32.8%**，而且**面积更小** ✓（tag 是按**行数**存的，行数减半 tag 从 400 降到 200 ✓）
- **大块也把缺失代价推高了** ✓ —— 一行 4 拍使缺失代价从 42.55 升到 60.32 cyc（+42%），所以 **TMT 的优势（−4.7%）远小于缺失次数的优势（−32.8%）** ✓ —— 这正是文档强调"大块必须配合突发"的原因
- **cachesim 预测精度极高** ✓ —— 四次预测的缺失次数与 RTL 实测只差 0.07%~0.19%；"每多一拍 ≈ +8.85 cyc"的代价外推误差 3.4%
- **4 拍突发可行** ✓ —— 之前担心 SDRAM 模型在 4 连读下会吞数据，实测 `MicroBench PASS`，没有发生
- **再往上都超预算** —— `16行×16B` ≈ 28545、`8行×32B` ≈ 27425
- 两组 128B 配置的**面积与频率都是 STA 实测**（`16行×8B` 22276.70/599.1MHz、`8行×16B` 21735.39/623.2MHz），不是估计值 ✓
- **最终选择 `8行×16B`** ✓ —— 命中率、缺失次数、TMT、总周期、面积**全部最优**（面积余 1264.6 µm²，频率余 123.2 MHz）

## ysyxSoC 本地补丁（重要，换环境必看）

**背景**：`ysyxSoC` 的远端是**官方仓库** `OSCPU/ysyxSoC`——本地所有改动**推不上去** ✗，重新 clone 就会全部丢失 ✗，其中包括上面**两个连环修复**。丢了之后"突发第二拍全 0"会**神秘复现** ✗

**做法**：把改动固化成 patch，放在**本项目**（`npc` 的远端是自己的仓库 ✓ 推得上去 ✓）：

```
npc/ysyxSoC-local.patch            # 补丁本体（自解释，头部记录基线 commit）
npc/tools/make-ysyxsoc-patch.sh    # 重新生成（改完 ysyxSoC 后再跑一次即可）
```

**应用**（在纯净的 `ysyxSoC` 克隆里）：

```bash
cd ysyxSoC
git checkout <patch 头部记录的基线 commit>       # 当前为 df38a4d9 (origin/ysyx6)
git apply ../npc/ysyxSoC-local.patch
```

**重新生成**：

```bash
npc/tools/make-ysyxsoc-patch.sh                  # 或 BASE=<其它基线> ... 
```

**有意排除**：
- `rocket-chip/` —— 子模块，只是指针脏标记（`-dirty`），无实际改动
- `patch/firtool/` —— `firtool`(28MB) + `om-linker`(8.5MB) 二进制 ✗。`git diff` 对二进制只能输出 `Binary files differ`，带上反而**无法应用**；需要时另行获取

**验证**：在 `origin/ysyx6` 的纯净 worktree 上 `git apply` ✓，20 个改动文件**逐字节一致** ✓

### 追加：D-Cache（周期 −2.82%）与一个平台侧真 bug（NpcMemRW 不支持同时读写）

#### ① D-Cache：4 项 × 1 个字，写穿 + 独占 AXI

结构**照抄 icache 的风格**：`Stream` 请求（read/write/addr/wdata/wmask/size）+ `Flow` 响应；dcache 内部复用 `ysyxx_23060082_Axi4_Ctrler`（单拍读写）并**独占 CPU 的 AXI 口** → **不需要第二个控制器、不需要任何 AXI 通道 mux**（早前一版正是栽在"两个控制器共用 r 通道"上）。

- 每项只有 1 个字（4B）→ **不需要突发，也不需要多字选择**
- 只缓存 **flash(0x3000_0000) 与 PSRAM/SDRAM(0x8000_0000~0xbfff_ffff)**；SRAM/MROM/设备不介入（设备读也不占 cache）
- 读命中当拍组合返回；读缺失进 `ReadMiss`，等控制器读完再填回并返回
- 写一律进 `Write`（**写穿**：命中也要写内存），命中时**同步更新 cache 那一个字**；写不分配
  → 于是 **cache 永远与内存一致**，不存在"读到旧值"的窗口（这正是不需要 `fence.i` 的原因）

**实测**：周期 12,975,343 → **12,609,428（−2.82%）**、IPC 0.0448 → **0.0462**、LSU 读事务 75,728 → **64,258**（命中率 **15.15%**）、icache 缺失 avg **不变**（无争用）、面积 22,460.24 → **24,830.83**、500MHz slack **+0.799ns**。

**两个必须记住的握手坑**（症状都是"错数据"，而不是明显挂死）：

| 坑 | 现象 | 正确做法 |
|---|---|---|
| 用 `rspOut.valid` 判"读命中" | 它还包含**上一笔 store 的 `b` 响应**与**上一笔缺失的完成** → 一次 load 拿别人的响应"当拍完成"，拿到旧数据 → ALU/分支错乱 → **死循环** | dcache 导出真正的 `readHit`（`Idle && reqRead && hit`），LSU 用它判命中 |
| `Idle → WaitMem` 在 `needMem` 上跳 | dcache 还在 `Write` 时 `reqIn.ready=0`，请求被丢掉，之后只会等到**属于别人的响应** | 等 `dcache.io.reqIn.fire` 再离开 Idle |

**注意**：面积余量只剩 **169 µm²**（抖动 ±250）→ 再往上加东西必须先腾面积。

#### ② NpcMemRW：一个平台侧的真 bug（不是核的问题）

- **现象**：跑 `am-kernels/tests/cpu-tests` 时随机失败，报"非法指令"（例如 `pc=0x3000017c 报非法 feb71ae3` —— 而该地址的指令其实完全合法），或输出错乱（`ID = __`）、或死循环。
- **定位**：`0690d2c`（更早）通过、`519cf4f`（五级流水线）起失败 → 逐版本二分锁定到流水线。关键推理：**同一条 `bne` 刚刚才在打印循环里执行过 5 次** → 不是译码器不认识 B 型 → 只能是"**送进 IDU 的指令与 pc 对不上**"。
- **根因**：五级流水线之后，**icache 的取指（读）与 LSU 的数据访问（读/写）可以在同一拍**通过 Xbar 打到内存；而旧的 `NpcMemRW` 只有**一个 `addr` 端口** → 两边抢用同一个地址 → 取指可能读到写地址、数据访问读到取指地址。
- **修复**：拆分读写地址（3 个文件同步改）
  - `playground/src/DPI-C.scala`：黑盒端口 `addr` → `waddr` + `raddr`
  - `playground/vsrc/dpi-c.v`：端口同步拆分，`pmem_write(waddr,…)` / `pmem_read(raddr)`
  - `playground/src/NPC_TOP.scala`：`waddr := io.axi4.aw.addr`；`raddr := Mux(arFire, ar.addr, readBase + 突发递增地址)`
- **验证**：`shuixianhua` 在 **npc 与 ysyxsoc 两个平台都 PASS**，项目全部回归通过。
- **附带说明**：这也是此前"dcache 好/坏"结论反复的根源 —— 该 bug 与 dcache 无关，纯属时序巧合。

#### ③ 两个无害的环境告警（供参考）

- `WARNING: Glycin running without sandbox.`（GTKWave，来自 OSS CAD Suite）—— glycin 是它用来加载图片/图标的库，只是"图片解码没跑在沙箱里"，**与仿真无关**。
- `xkbcommon: ERROR: … unrecognized keysym "dead_hamza"`（surfer 等现代 GUI）—— `libx11 1.8.13` 的 X11 Compose（组合键）表用了 `libxkbcommon 1.13.2` 不认识的旧 keysym，**与设计无关**；`XCOMPOSEFILE=/dev/null surfer xxx.fst` 即可消除。
