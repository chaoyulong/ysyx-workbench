# 交接提示词：iverilog 四值仿真 / 网表仿真（B5 流片准备）

> 用法：下面每段都是**自包含**的，按需整段复制给 AI 助手。工作区：`ysyx-workbench`（`npc` 仓库）。
> 相关背景另见 `npc/AI_PROMPTS.md`（工程总览/代码地图）与 `HANDOVER-CI.md`（CI 合规线）。
>
> **2026-10-01 更新**：网表 X 卡点**已定位并修复**（commit `6a6d85d`）。原因不是状态机/valid 漏复位，
> 而是 **CSR 侧控制信号没按 `io.input.valid` 门控**（复位释放那一拍 CSR 被 X 写入）。详见 `npc/README.md`
> 的 2026-10-01 节与 `npc/PERF.md` 的 `6a6d85d` 行。提示词 3 保留作为方法记录。
>
> **2026-10-03 更新**：定位并修复了**另一个独立的**"网表完全不工作"根因（commit `e3d72f4`）：
> `ifu.rdataReg` 无复位 ⇒ 上电即 X ⇒ `isJump` 译码出 X ⇒ `stopFetch` 锁成 X ⇒ `tryFetch` 恒 X ⇒
> **IFU 永不取指**（现象：cyc=50 起 `arvalid` 恒 0、端口全 X、看门狗超时，而同一份 RTL 用 iverilog
> 跑完全正常；这也是"同一 RTL 换个位宽/写法有时能过、有时不能"的真因）。修法：`rdataReg … init(0)`。
> 另外：CI 三变量写法 `NETLIST=`/`CELLS=` 已实现（给了就跳过 SpinalToVerilog 与 yosys）；
> dcache guard 位宽重扫取 W=21，当前 **1005.4 MHz / 面积 24752.36**。详见 `npc/README.md` 的 2026-10-03 节。

---

## 提示词 1：任务总览 + 当前进度（接手先读这段）

```
我在做 ysyx（一生一芯）的 NPC（SpinalHDL 写的 RV32E 五级流水 CPU），现在这条线的任务是
B5「流片准备」里的【四值仿真】与【网表仿真】，用 iverilog 做，目的是把"Verilator 二值仿真
掩盖掉的漏复位触发器（X 传播问题）"和"综合后才暴露的行为差异"找出来。

规范：https://ysyx.oscc.cc/docs/2306/basic/1.11.html 的「四值仿真 / 网表仿真 / 申请代码调试考核」三节。
CI 的目标名约定：make -C npc sim-iverilog IMG=xxx.bin  以及
                  make -C npc sim-iverilog-netlist IMG=xxx.bin NETLIST=yyy CELLS=zzz

【已完成】✓
- npc/Makefile 已有 sim-iverilog / sim-iverilog-netlist 两个目标（别用别的名字）
  · sim-iverilog 内部用 SPINAL_SIM_DEBUG=0 **PARTFORM=npc** 生成到 build/iverilog/（无 DPI 黑盒，
    复位值 0x80000000）★ 注意与 sta 不同：sta 为对齐 CI 用 PARTFORM=ysyxsoc（复位 0x30000000）
  · 网表默认由【同一份 npc RTL】综合到 build/iverilog/sta/ysyx_23060082-500MHz/ysyx_23060082.netlist.v.sim
    （★ 必须是 .sim 那份：yosys.tcl 里 splitnets -ports 会拆端口，.netlist.v 是位级端口，TB 接不上）
  · 也支持 CI 的三变量写法（给了 NETLIST 就跳过 SpinalToVerilog 与 yosys）：
      make -C npc sim-iverilog-netlist IMG=xxx.bin NETLIST=yyy.netlist.v.sim CELLS=zzz/cells.v
- TB 在 npc/sim-iverilog/testbench/tb_npc.v；标准单元模型在 npc/sim-iverilog/testbench/cells.v
- .bin→$readmemh 的转换脚本 npc/sim-iverilog/tools/bin2mem.sh
- RTL 四值仿真：microbench 全量 PASS ✓（10/10 项 Passed + MicroBench PASS，1,049,605 周期）
  RT-Thread 能启动到 msh shell ✓（之后停在 AM 侧 panic：npc/ioe.c 的 lut 没实现 rtt 要的设备）

【已解决 2026-10-01（`6a6d85d`）】✓ 网表 X 根因 = 【CSR 侧控制信号没按 valid 门控】
- 网表在复位释放那一拍(cyc 50)把 lsu.csr 的 mtvec/mstatus/mcause/mcycle/minstret 写成 X；
  RTL 的 if(reset) 是程序分支(if(X) 当假)，综合成门后复位只是 D 端 mux 的一项 ⇒ X 直接写进寄存器
- X 经前递/比较 → exu.redirect.valid → IFU 的 pcFetch(cyc 42253，第一个变 X 的控制寄存器)
  → 顶层 arvalid/araddr X(cyc 42260)；Xbar 的 readState/arbiterState 是【最后】被污染的，不是源头
- 修法(LSU.scala，零新增触发器)：csrCmd = Mux(trapEnter || !io.input.valid, 0, ...)；
  trapEnter/trapExit 都与 io.input.valid 相与
- 验收：网表 X 消失(端口含 X 周期 787→47，只剩启动瞬态)；网表全量 microbench 10/10 PASS、UART 539B、
  AXI ar/aw/rbeat/wbeat = 104590/48668/252778/48668(与 RTL 逐位一致)；rtthread(新 bin) 网表跑到 RTT
  自动 microbench PASS 后收尾；make perf 11908252/555445 及全部计数器逐位不变；
  make sta 面积 24990.97(<25000)、0 VIOLATED、slack +0.838ns
- 残留(协议内正常，刻意不动)：mepc / regFile.rf_3,rf_4 / clint.wStrbFullReg 仍是 X —— 它们只在
  "先写后读"的协议保证下被选中，不逃逸到控制路径；加复位要 32/512 个触发器，远超 9 µm² 余量
- ★ 2026-10-03 更正：上面"数据寄存器不用复位"的结论**不成立** —— `ifu.rdataReg`(无复位) 的 X 经
  `isJump` 把 `stopFetch` 锁成 X(`tryFetch` 恒 X)，会让【整条网表完全不工作】(arvalid 恒 0、看门狗超时)，
  而 RTL 因 `valid && …` 的语义把这条 X 掩盖掉、看不出问题。已在 `e3d72f4` 用 `init(0)` 修掉

【下一步】CI 的两个 bin 现在都能在网表模式自动收尾(E1 取指 0 / E3 UART "AM Panic:")，不必再手设 IV_MAXCYC
```

---

## 提示词 2：TB 的结构与已排除的坑（要改 TB 前必读）

```
npc/sim-iverilog/testbench/tb_npc.v 是替代 verilator C++/DPI 环境的仿真顶层，结构：
- 时钟/复位：period 2ns，复位保持 RESET_CYCLES=50 拍（项目是同步复位）
- pmem：0x80000000 起 0x800_0000(128MB)，字节粒度，下标 = 地址 - 0x80000000
- 地址译码只有三块：pmem / UART(0x10000000 写→$write("%c") stdout) / trampoline(0x30000000, 16B)
  ★ CLINT 在 ysyx_23060082 内部（Xbar 把 0x02000000 路由给内部 CLINT），TB 不需要实现
  ★ trampoline 只是为了让 CI 要求的"复位值 0x30000000"能跳到程序所在的 0x80000000
- AXI4 从机：读/写都支持突发（icache 会发 4 拍 ARLEN=3）；写按 WSTRB 逐字节落内存
- 结束判定（只用顶层可见量，RTL/网表通用）：
  · E1 从地址 0 取指（microbench：ebreak + mtvec=0）
  · E3 UART 出现 "AM Panic:"（rtthread 跑完自动 microbench 后 panic）
  · RTL 另有层级引用看 LSU 的 ebreak 提交(dut.lsu.io_input_valid && ...trapEnter && excCause==3)；
    网表 flatten 后没有 lsu 实例 ⇒ 该信号恒 0，自动只用 E1/E3（+no_ebreak_stop 可关掉它）
- 诊断开关（默认关）：+xcheck（端口 X 统计/首次 X/首次有效事务上的 X）、+traffic（每次 AR/AW）、
  +memcheck（存储器自检）、+img_size=（镜像大小，Makefile 会传）、+quiet、+zero=（默认 16MB）、+max_cycles=

【已经在 TB 里踩过并修掉的坑，别再踩】
1. $readmemh 会覆盖数组其余部分 ⇒ 必须【先 $readmemh、再清零"镜像之外"的区间】，
   清零区间用 +img_size= 区分（否则 .bss/stack 读到 X）
2. X 检查必须用 negedge 采样：门级组合逻辑在 posedge 还在收敛，posedge 采样有假阳性
3. AXI 窄传输(sb/sh)：AWADDR 是字节地址、数据在 WSTRB 对应的字节道 ⇒
   字节 k 的落点 = (AWADDR & ~3) + k，不是 AWADDR + k（写错会把栈上的 char 写歪）
4. 含 X 的写数据要按 0 落内存（verilator 流程内存是二值的），否则无害的"存未初始化值"
   会被放大成大面积 X 传播
5. 网表里【能】引用扁平转义网名，写法必须是"实例点 + 转义名"(如 dut.\lsu.io_input_valid)；
   写成 dut.lsu.xxx(当 scope)才报 Unable to bind。但 yosys 的改名不稳定(lsu.io_input_valid 会变成
   _zz_io_input_valid 等) ⇒ 不要依赖它，网表收尾用 E1/E3 这种顶层行为判据
6. 网表很慢（≈9.5 分钟/百万周期）⇒ 现在有 E1/E3 自动收尾，一般不用再手设 IV_MAXCYC
```

---

## 提示词 3：卡点排查方案（**已完成**，保留作方法记录）

**结论（2026-10-01，`6a6d85d`）**：源头不是状态机/valid 漏复位，而是 `LSU.scala` 的 CSR 侧控制信号
未按 `io.input.valid` 门控 —— 网表在复位释放那拍把 `lsu.csr.{mtvec,mstatus,mcause,mcycle,minstret}`
写成 X，再经前递/比较 → `exu.redirect.valid` → `pcFetch`(cyc 42253) → 顶层 `arvalid/araddr`(cyc 42260)；
Xbar 的 `readState/arbiterState` 是**最后**被污染的。修法见 `LSU.scala`（3 行门控，零新增触发器）。
下面这段 VCD 方法就是本次定位用的手段（VCD 生成代码事后已从 TB 移除，需要时按这段临时加回）。

```
NPC 的 iverilog 网表仿真在 cyc=42261 出现 X：顶层 io_master_arvalid 先变 X，随后 araddr、
rvalid/bvalid/wvalid 全变 X；同期 RTL（同 TB、同镜像）完全没有 X。
排查面已经收敛到【IFU/LSU 的读请求 valid】+【xbar 的仲裁/路由状态】这一小块：
- Xbar.scala 里 arbiterState 有 init（Reg(ArbiterState()) init(Idle)），
  但 readState / writeState（CrossState 路由状态机）的同族写法需要核对是否都带 init
- 网表里对应的寄存器名（grep 过）：\xbar.arbiterState_1_0__reg_p_ \xbar.readState_0__reg_p_
  \xbar.writeState_0__reg_p_ 等
★ 要求：先【不要改 RTL】，先查清是哪个信号先变 X。

【可靠做法：VCD 窗口 + 文本解析】
1. tb_npc.v 里加 +vcdwin：初始块只记开关 vcdwin=1；真正的 dump 放到窗口开始那一拍：
     always @(posedge clock) if (vcdwin) begin
       if (cycles == vcd_from) begin $dumpfile("tb_npc_win.vcd"); $dumpvars(0, dut); end
       if (cycles == vcd_to)   $dumpoff;
     end
   （vcd_from=42000, vcd_to=42400；★ 不要用"t=0 先 $dumpvars 再 $dumpoff"的写法，
     iverilog 下那样只会留下 t=0 的快照，抓不到窗口）
2. 编译网表并跑：
     iverilog -g2012 -DNETLIST -o build/iverilog/net-iv.vvp -s tb_npc \
       build/iverilog/sta/ysyx_23060082-500MHz/ysyx_23060082.netlist.v.sim \
       sim-iverilog/testbench/cells.v sim-iverilog/testbench/tb_npc.v
     vvp build/iverilog/net-iv.vvp +img=build/iverilog/img/img.hex +img_size=30620 \
         +max_cycles=42400 +vcdwin +quiet
3. 解析 VCD：先用 $var 行建 code→name 表，再找【第一个含 x 的值变化行】⇒ 那个名字就是源头：
     awk '/^\$var/{nm[$4]=$5;next} /^#/{t=$1;next}
          /^[bB]?[01xXzZ]+[ \t]/{n=split($0,a,/[ \t]+/); v=a[1]; c=a[n];
            if (v ~ /[xXzZ]/ && (c in nm)) { print t, nm[c], substr(v,1,16); if(++k>=8) exit }}' tb_npc_win.vcd
4. 拿到名字后判断：
   - 若是 xbar 的 readState/writeState（或类似状态机寄存器）没复位 ⇒ 结论就是"状态机漏复位"
   - 若是 IFU/LSU 控制器的 ar.valid/aw.valid/w.valid ⇒ 结论是"AXI 握手 valid 漏复位"
   （文档把"各状态机、流水线 valid、AXI 握手 valid、icache 行有效位"列为【必须复位】）
★ 只有在查清并复现之后，才动 RTL 补 init。
  ★ 2026-10-03 更正：当初"不要给 rdataReg 这类数据寄存器加复位"的说法已被推翻 —— `ifu.rdataReg`
  正是"网表完全不工作"的根因(见顶部 2026-10-03 更新)，一个 `init(0)` 就修好了；`init(0)` 只是给
  已有触发器加复位端，很便宜(本例面积 24752.36 < 25000，余量 247.6 µm²)
```

---

## 提示词 4：修完之后的完整验收链（必须全过）

```
改 RTL 之后依次跑，全部要过（数值为 2026-10-03 实测，W=21 + 跳转译码字段化 + rdataReg 复位）：
1. 网表 +xcheck：不再出现"有效事务上的 X"（只剩启动瞬态；★ 已知无害：`ysyxsoc_dis_id` 序言
   `sw s1,28(sp)` 存了从未写过的 s1 ⇒ 1 拍 X 写，不落 UART）、且 AXI 事务数
   (ar/aw/rbeat/wbeat = 104590/48668/252778/48668) 与 RTL 一致
2. RTL 四值仿真全量：make -C npc sim-iverilog IMG=sim-iverilog/bin/microbench-riscv32e-npc.bin
   10/10 项 Passed + MicroBench PASS ✓（1,049,605 周期）
3. 功能基准：CCACHE_DISABLE=1 make -C npc perf
   ★ 周期 11908252 / 指令 555445，全部 PERF 计数器逐位不变 ✓
4. 时序/面积：make -C npc sta
   ★ 面积上限 25000（nangate45）：当前 24752.364 ⇒ 余量 247.6 µm²
   ★ 频率 >500MHz 通过（当前 slack +1.005ns ≈ 1005.4 MHz）
5. rtthread（新 bin）：RTL E3 收尾 2,398,028 拍；网表 10/10 PASS + E3 收尾 2,398,028 拍 ✓
6. 别并行跑两个仿真/综合（build/ 会互相覆盖，尤其 `build/iverilog/img/img.hex` 是固定路径）；日志写到 npc/build/ 下

【镜像怎么准备】
cd am-kernels/benchmarks/microbench && make ARCH=riscv32e-npc
⇒ build/microbench-riscv32e-npc.bin（★ 立刻拷到 npc/sim-iverilog/bin/，否则会被
  ARCH=riscv32e-ysyxsoc 的构建覆盖同目录）；rtthread 同理（需要 rt-thread-am 打了 patch）
```

---

## 提示词 5：一句话版本（把这段贴在提问最前面）

```
我的 NPC（ysyx 一生一芯，SpinalHDL RV32E）在用 iverilog 做四值仿真与网表仿真。
npc/Makefile 里已有 sim-iverilog / sim-iverilog-netlist 两个目标，TB 在
npc/sim-iverilog/testbench/tb_npc.v（带突发 AXI 从机、128MB pmem、只有 UART 外设、复位 50 拍）。
网表 X 卡点修过两次：① 2026-10-01 `6a6d85d`：LSU 的 CSR 侧控制信号没按已复位的 io.input.valid
门控(复位释放那拍 CSR 被 X 写入)；② 2026-10-03 `e3d72f4`：`ifu.rdataReg` 无复位 ⇒ X ⇒ `isJump`
译码出 X ⇒ `stopFetch` 锁成 X ⇒ 前端永不取指(整条网表不工作，RTL 却正常)，修法 `init(0)`。
TB 结束判据只用顶层可见量(RTL/网表通用)：E1 从地址 0 取指(microbench)、
E3 UART 出现 "AM Panic:"(rtthread)；CI 三变量 `NETLIST=`/`CELLS=` 已支持(给了就跳过综合)。
验收全过：网表 10/10 PASS 且 AXI 计数与 RTL 一致、make perf 周期/指令 11908252/555445 逐位不变、
make sta 面积 24752.36(<25000) / 1005.4 MHz。
```
