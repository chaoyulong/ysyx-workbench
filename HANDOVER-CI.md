# 交接说明：B5 流片准备 / CI 合规线

> **给接手这条线的 AI**：先读本文件（它就在工作区根目录），再读 `npc/AI_PROMPTS.md`（493 行：工程总览 / 代码地图 / 地址映射 / 常规踩坑，**本文件不重复它的内容**）。
> **本文件没有写死任何绝对路径**：下文所有路径都相对"工作区根目录"（= 本文件所在目录），因此复制到任意目录名/任意位置都成立。
> **基线状态**：`npc` 仓库停在 **`a98086e`**（已 push，`origin/main` 同步）。性能优化那条线**已经结束**。

---

## 0. 任务来源（规范原文）

<https://ysyx.oscc.cc/docs/2306/basic/1.11.html> —— 唯一规范来源。**动手前完整读一遍**，尤其：`四值仿真`、`网表仿真`、`申请代码调试考核` 三节（后者给出了 CI 的目录/命名/make 目标约定）。下面第 2 节只摘录与代码直接相关的硬性部分。

---

## 1. 接手须知：这条线的目标与边界

- 本工作区是从**性能主线**复制出来的副本。性能线已完成（周期 / 面积 / 频率都有实测记录）⇒ **不要再做性能优化，不要改流水线/cache/位宽**。
- 本线**唯一目标**：满足 B5 的**四值仿真**（iverilog，用 `X` 暴露"该复位却没复位"的触发器）与**网表仿真**（yosys 网表 + nangate45 标准单元），并把仿真环境整理成 **CI 认可的形状**（目录 / make 目标 / 补丁）。
- ⚠ 两个工作区（性能主线、本副本）是**独立副本**：`npc` 的提交不会互相自动同步。别假设某个提交在另一侧也有。
- ⚠ 本副本的**外层仓库 index 很脏**（见 §7.1），提交前务必看清。

---

## 2. 硬性约定摘录（照这个做，CI 才会认）

### 2.1 四个 make 目标的确切契约

| CI 执行 | 契约要点 |
|---|---|
| `make -C npc verilog` | 在 `npc/build/` 生成 **`ysyx_23060082.v`**；**PC 复位值 = `0x30000000`**；含模块 `ysyx_23060082`；**`module ysyx_23060082` 前一字符必须是空白**（另起一行即可）；CLINT 地址与 ysyxSoC 一致。CI 把该文件作为仿真与综合的输入 |
| **`make -C npc sim-iverilog IMG=xxx`** | 用 iverilog 仿真上面那个 `.v` 并运行 `xxx`；**程序第一条指令位于 `0x80000000`**；`xxx` 是 **`.bin`**（⚠ `$readmemh` 读不了 `.bin` ⇒ **该目标内部必须自己转换**）|
| **`make -C npc sim-iverilog-netlist IMG=xxx NETLIST=yyy CELLS=zzz`** | 用 iverilog 仿真网表 `yyy` 并运行 `xxx`；`zzz` = 标准单元行为文件，**加进源文件列表**；`xxx` 同为 `.bin` |
| `make ARCH=riscv32e-npc`（在 am-kernels 代码目录）| 生成"未接 SoC 的 NPC 程序"，**第一条指令 `0x80000000`** |

### 2.2 其他要求（逐条）

- **去掉 DiffTest 与存储器相关的 DPI-C 调用**：iverilog 不支持 DPI-C，也不能用 C++ 驱动仿真。
- **存储器用 Verilog（或 Chisel）行为级重写**（不必可综合），用 `$readmemh()` 初始化。
- 需要一个**简单仿真顶层**：产生时钟/复位并驱动 NPC。
- 编译用 **`-g2012`**；iverilog 自动定义 **`__ICARUS__`** ⇒ 可用条件编译让一份代码同时支持 verilator 与 iverilog。
- 有一条**可忽略**的报错：`sorry: constant selects in always_* processes are not currently supported`。
- 网表仿真的标准单元实现：`yosys-sta/nangate45/sim/cells.v`（真实路径见 §3）。
- 目录结构：仓库需含 `abstract-machine` / `nemu` / `npc` / `Makefile` / `patch/{rt-thread-am,ysyxSoC}/`；`Makefile` 里 `STUID`/`STUNAME` 必须正确（当前 ✓）。
- CI 会**从上游重新拉取** `am-kernels` / `nvboard` / `yosys-sta` ⇒ **不能依赖对这三者的修改**；`rt-thread-am` 与 `ysyxSoC` 通过 `patch/` 下的补丁（`git format-patch`，总量 < 1MB）获取。
- 建议**关闭 difftest、关闭波形**（CI 不返回波形，波形只拖慢仿真）。
- 还要推送一个 `tracer-ysyx` 分支（git 追踪系统自动维护）。

---

## 3. 现状体检（都已实测，别重复劳动）

| 项 | 状态 |
|---|---|
| **iverilog** | ✓ **14.0**，位于 `/home/cyl/tools/oss-cad-suite/bin/iverilog`（已在 PATH）|
| iverilog 能否吃下我们的 RTL | ✓ **实测 `iverilog -g2012 <STA 生成的 ysyx_23060082.v>` 退出码 0**（SpinalHDL 输出兼容）|
| **cells.v** | ✓ `/home/cyl/tools/yosys-sta/pdk/nangate45/sim/cells.v`（⚠ 文档少写了 `pdk/`）；**101 个 cell、0 个 `specify`/时序块 ⇒ 纯功能模型** ⇒ 网表仿真结果**应与 RTL 逐位一致**；iverilog 编译通过 ✓ |
| 综合网表 | `npc/build/sta/ysyx_23060082-500MHz/`：`…netlist.v`（**端口被拆成位级** ✗ 别用）/ **`…netlist.v.sim`（拆端口之前，向量端口 ✓ 用它）**；单元统计：`DFF_X1` 3226 / `MUX2_X1` 938 / `AOI22_X1` 906 / `NAND2_X1` 666 / `OAI21_X1` 471 / `INV_X1` 426 |
| 镜像 | ✓ `am-kernels/benchmarks/microbench/build/microbench-riscv32e-npc.{elf,bin,txt}` 已存在；另有小的 `am-kernels/kernels/hello/build/hello-riscv32e-npc.bin`（**拿它做第一个里程碑**）|
| **rt-thread-am** | ✗ **尚未克隆** ⇒ rtt 里程碑的前置条件 |
| `patch/` 目录 | ✗ **尚未创建**（素材：`npc/ysyxSoC-local.patch` + `npc/tools/make-ysyxsoc-patch.sh`）|
| DPI 的位置 | **只在 `npc/playground/vsrc/dpi-c.v`**（模块 `NpcMemRW`/`ItraceReg`/`MtraceReg`/`PerfReg`）；`SPINAL_SIM_DEBUG=0` 生成的核心 RTL **一条 DPI 都没有** ✓ |
| 关键地址（`abstract-machine/am/src/riscv/npc/npc.h`）| `SERIAL_PORT = 0x10000000`（`putch` 是**字节写**）；`RTC_ADDR = 0x02000000`（CLINT mtime）；程序结束 = **`ebreak`**（`npc_trap(code)` = `mv a0,code; ebreak`）|
| 复位值（`npc/playground/Config.scala`）| `ysyxsoc → 0x30000000`、`npc → 0x80000000`；`Config` 按环境变量 **`PARTFORM`** 选（**缺省是 `npc`** ⚠ 见 §9 的坑）|
| 功能基准（`a98086e`，逻辑等价于上一版）| `make perf`（microbench，`test` 规模）= **周期 `11908252` / 指令 `555445` / IPC 0.0466** |
| STA 基准（`a98086e` 实测）| 面积 **24,986.178 µm²**（上限 25,000 ⇒ **余量仅 13.8** ⚠）；setup slack **1.004 ns ⇒ 1004.0 MHz**；最差端点 `_zz_io_output_ready_1_reg_p:D`；hold 0.077 ns；VIOLATED 0 |

---

## 4. 已定的设计决策（**除非有更好理由并写进 PR 说明，否则不要推翻**）

1. **目标名**用 CI 约定的 `sim-iverilog` / `sim-iverilog-netlist`（**不是** `iverilog`）。
2. **仿真用的 RTL 自己生成**：在 `sim-iverilog` 里用 `SPINAL_SIM_DEBUG=0` 生成到**独立目录**（如 `build/iverilog/`），照抄 `npc/Makefile` 里 `sta` 目标那三行。
   - 理由：① 天然无 DPI（满足"去掉 DPI 调用"）；② 不被 `make sta` 覆盖（`build/sta/…v` 每次综合都会重写）；③ 幂等可复现。
3. **网表用 `…netlist.v.sim`**：`~/tools/yosys-sta/scripts/yosys.tcl` 第 246 行做 `splitnets -format __v -ports`（**连端口都拆**）⇒ `…netlist.v` 里只有 `io_master_rdata_0_` 这类位级端口（几百个，TB 接不上）；它上一行写出的 `.sim` 是**拆端口之前**的版本，端口是向量 ⇒ 同一个 TB 可直接复用。
4. **PC 复位值不能改**（CI 要求 `0x30000000`），而程序要放在 `0x80000000` ⇒
   **让 TB 在 `0x30000000` 处返回一条 trampoline**（`lui t0,0x80000` + `jr t0`，共 8 字节）⇒ RTL 一个字都不用动，两个约定同时满足。（另一条路是给复位值加 `ifdef __ICARUS__` 条件编译，但 SpinalHDL 里不好写。）
5. **存储器用字节粒度数组**（`reg [7:0] mem [0:N]`）+ `$readmemh` 从下标 0 开始。
   - 理由：`objcopy -O verilog` 的输出是**每行一个字节**；且镜像链接在 `0x80000000` 而 `$readmemh` 从 0 开始 ⇒ 让**下标 = `addr - 0x80000000`** 最省事（不依赖 objcopy 的地址语义）。
6. **TB 的 AXI slave 必须支持突发**：icache 会发 4 拍突发（`ARLEN=3`）；只做单拍必挂。写通道按 **WSTRB** 逐字节落内存；`RID/BID` 回传 `ARID/AWID`。
7. **结束判定**（无 DPI，且 debug 信号已关，顶层看不到退休指令）：
   - **RTL 侧**：TB 用**层级引用**看内部退休信号，检测 `instr == 32'h00100073`（`ebreak`）⇒ 打印 `a0`（halt code）并 `$finish`
   - **网表侧**（flatten 后层级消失）：退化成"**看 UART 输出 + watchdog 超时 `$finish`**"
8. **`cells.v` 复制进仓库**（如 `npc/vsrc/nangate45/cells.v`），`CELLS=` 默认指向仓库内副本（CI 会传 `CELLS=zzz`，但我们自己的 `make` 调用要有默认值）。
9. 需要改 RTL 时优先用 `ifdef __ICARUS__` 隔离差异（iverilog 自动定义该宏），让 `make verilog` 的输出对 verilator/SoC 保持原样。

---

## 5. TB 设计清单（`npc/vsrc/tb_npc.v`）

```
tb_npc
 ├─ clock / reset 生成          ★ 项目是【同步复位】（Config.scala: resetKind = SYNC）⇒ 复位保持若干拍再释放
 ├─ dut: ysyx_23060082          （向量端口 ⇒ 用 SPINAL_SIM_DEBUG=0 生成的那份）
 ├─ AXI4 slave（行为级）        ★ 必须支持突发；写按 WSTRB 落字节；读 32 位
 ├─ 地址解码
 │    · 0x30000000        ⇒ trampoline: lui t0,0x80000; jr t0
 │    · 0x10000000        ⇒ UART: $write("%c", data)   ← 判定"跑对了"的唯一输出（CI 也看 stdout）
 │    · 0x02000000/0x02000004 ⇒ CLINT mtime 计数器（低/高）
 │    · 其余              ⇒ 平坦内存（用到的段：0x8000_0000 起）
 ├─ 结束：见 §4.7
 ├─ 四值检查：复位后报告 pc/state/valid/写使能 是否含 X
 └─ 波形：$dumpfile/$dumpvars —— ★ 只在调试时开（会再慢一个数量级）
```

**四值仿真的判据**（本线的核心目的）：

- **预期就该是 X**（刻意省了复位的寄存器，别当 bug）：`regFile`、`icache.lineReg`/`wordCnt`/`wordSelReg`/`pcReg`、`dcache` 的 `wTempL`/`wid`/`rid`/`wStrbFull`、CLINT `timeCountHighSnap` 等；
- **真 bug 的判据**：**X 有没有进入控制路径** —— X 只待在"被 `valid` 挡住的数据"里是设计意图；一旦 `pc`/`state`/`valid`/写使能变成 X ⇒ 就是漏复位。

---

## 6. make 目标骨架（放 `npc/Makefile`）

```makefile
IVERILOG ?= iverilog
IV_DIR    = $(BUILD_DIR)/iverilog
IV_RTL    = $(IV_DIR)/$(STA_DESIGN).v                 # SPINAL_SIM_DEBUG=0, 与 verilator 的 build/ 分开
IV_TB     = $(abspath vsrc/tb_npc.v)
CELLS    ?= $(abspath vsrc/nangate45/cells.v)

$(IV_RTL):
	@mkdir -p $(IV_DIR)
	@SPINAL_TOPNAME=$(STA_DESIGN) BUILD_DIR=$(IV_DIR) SPINAL_SIM_DEBUG=0 PARTFORM=$(PARTFORM) \
		./mill -i $(SPINAL_PRJ).runMain $(SPINAL_PRJ).SpinalToVerilog

sim-iverilog: $(IV_RTL) $(IV_TB)
	@mkdir -p $(IV_DIR)
	python3 tools/bin2mem.py $(IMG) $(IV_DIR)/img.hex
	$(IVERILOG) -g2012 -o $(IV_DIR)/sim.vvp -s tb_npc $(IV_RTL) $(IV_TB)
	vvp $(IV_DIR)/sim.vvp +img=$(IV_DIR)/img.hex        # plusarg ⇒ 编译一次可跑多个镜像

sim-iverilog-netlist: $(IV_TB) $(CELLS)
	@mkdir -p $(IV_DIR)
	python3 tools/bin2mem.py $(IMG) $(IV_DIR)/img.hex
	$(IVERILOG) -g2012 -o $(IV_DIR)/net.vvp -s tb_npc $(NETLIST) $(CELLS) $(IV_TB)
	vvp $(IV_DIR)/net.vvp +img=$(IV_DIR)/img.hex
```

⚠ 注意 `sta` 目标**没有把 `PARTFORM` 传给 mill** ⇒ 它生成的是**默认（`npc`）平台**的 RTL（`resetPc = 0x80000000`）。`sim-iverilog` 请**显式**传 `PARTFORM`（否则复位值不是 `0x30000000`，与 CI 要求不符）。

---

## 7. 仓库拓扑与提交约定（★ 极易搞错）

- **根仓库**（工作区根目录的 `.git`）：`.gitignore` 只白名单部分路径 —— 追踪 `abstract-machine`(170 文件) / `nemu`(152) / `npc`(50)；`am-kernels` / `nvboard` / `fceux-am` **被 gitignore**（CI 会重新拉取，与文档一致）。
- **`npc/` 同时是独立仓库**（远端 `chaoyulong/npc`，`main` 与 `origin` 同步）。提交信息沿用它的风格：`perf(xxx): …` / `chore: …` / `docs: 记录 <hash> …`。
- **`abstract-machine` / `nemu` 在副本里没有 `.git`** ⇒ 它们的改动只能提交到**根仓库**；推送方式**先问用户**。
- **`ysyxSoC`** 远端是官方仓库（本地改动推不上去）⇒ 只能以补丁交付：CI 要 `patch/ysyxSoC/*.patch`（`git format-patch`）。**已有素材**：`npc/ysyxSoC-local.patch` + `npc/tools/make-ysyxsoc-patch.sh`。
- **`am-kernels`** / `nvboard` 的改动 CI **不会采纳** ⇒ 需要的镜像/脚本要自己保存进 `npc/`。
- ⚠ `npc/Makefile` 里有 `$(call git_commit, ...)`（注释标了 `DO NOT REMOVE THIS LINE`）⇒ **跑 `make verilog` / `make sim`（含 `make perf`）会自动往所在仓库提交一次**，`git log` 里出现 `sim RTL ...` / `generate verilog ...` 这类提交属正常现象。`make sta` / `make lint` **不会**触发它。

### 7.1 ⚠ 根仓库里已经有一堆**遗留**未提交改动（接手时必须小心）

`git status --porcelain` 有 **92 条**（`MM` 49 / `D` 20 / `??` 17 / `RD` 3 / `AD` 2 / `M` 1）。其中：
- 20 条 `D` 是**索引里记着"删除"、但磁盘上文件还在**（如 `npc/PERF.md`、`npc/csrc/trace/*.c`、`npc/include/difftest.h`、`abstract-machine/.../npc/gpu.c`）⇒ **陈旧/不一致的索引**，不是真缺文件；
- 3 条 `RD` 是一次改了名的（索引里是拼错的 `keybord.c`，磁盘上其实是正确的 `keyboard.c`）；
- 2 条 `AD` = 索引里"已 add"但磁盘上已删（如 `abstract-machine/am/src/riscv/npc/input.c`）。

**两条铁律**：
1. ✗ **绝对不要** `git add -A` / `git commit -a`（会把这一堆无关甚至自相矛盾的改动一起提交）；
2. ✓ 提交时**只写你要动的具体路径**（`git add <path> …`）；要清理旧状态**先问用户**，不要自己 `git reset --hard` / `git checkout -- .`（`MM` 那 49 条里可能有用户想留的改动）。

### 7.2 ⚠ 外层仓库的 `origin` 是**官方仓库**

根仓库的 `origin` = `git@github.com:OSCPU/ysyx-workbench.git`（**官方，推不上去**）。CI 要求把 workbench 推到"一个公开仓库" ⇒ 需要**另建 fork / 换远端**，**先与用户确认**（别擅自改 remote）。

---

## 8. 项目既有铁律（别违反，否则性能线会塌）

- **评分口径 = 周期 + 面积**；面积上限 **25,000 µm²**（nangate45）。当前面积 **24,986.178 ⇒ 余量仅 13.8 µm²** ⚠ ⇒ **任何会改变结构（增删信号）的 RTL 改动之后，必须重新 `make sta` 确认面积**。
- 频率不计分，但要求 **> 500 MHz**（当前 1004.0 MHz，2 倍余量）。
- **功能基准**：`make perf` 必须得到 **周期 `11908252` / 指令 `555445`**；数字变了先怀疑**程序**（AM 侧/镜像），而不是 RTL（RTL 不会改变仿真执行了多少条指令）。
- **不要改** `~/tools/yosys-sta/scripts/yosys.tcl`（工具脚本，课程不允许）。
- `CCACHE_DISABLE=1`；**绝不并行跑两个仿真/综合**（`build/` 会互相覆盖）；本机 `/tmp` 不可写 ⇒ 日志写 `npc/build/`。
- verilator 用的是 `--x-assign fast --x-initial fast --noassert` ⇒ **它把"漏复位"全遮住了** —— 这正是本线要做的四值仿真的意义。
- **AXI 的 `addr`/`data`/`strb` 只在 `valid` 那一拍有意义** ⇒ 任何"立即生效"的逻辑（写寄存器/改状态/发响应）**必须用握手门控**（见 §9）。

### 8.1 ★ 实测补充：**纯外观改动不会重掷映射**（这条纠正了旧文档的说法）

旧文档写的是"换源码就重新抽一次签"。实测（`a98086e`）表明要**加限定**：

| 改动类型 | 生成 RTL | ABC 映射（面积/频率）|
|---|---|---|
| **注释 / 空白 / 缩进 / 改名** | 只有 `// @ file lNNN` 注释与 `when_Xxx_lNN` 这类**行号派生名字**变；归一化后逐字节相同 | **完全不重掷**（面积 24,986.178、slack 1.004 ns 与改动前**逐位相同**）|
| **增删任意信号**（哪怕没人用） | 结构/名字变 | **重掷**（面积可抖动 ±100~500 µm²、最差路径换族）|

⇒ 格式化/删注释**可以放心提交**（不必重跑 STA）；**增删信号才必须重跑**。

---

## 9. 已经踩过的坑（血泪清单）

| 坑 | 现象 / 教训 |
|---|---|
| **CLINT 写逻辑缺握手门控** | `when(writeHigh)` 里直接用 `aw.addr`（**只在 valid 那一拍有效**）⇒ 空闲时被每个时钟执行一次 ⇒ `mtime` 被乱写 ⇒ microbench 打印 `Scored time: 1894080620.0837 ms`、指令数 +4723。**用总线信号做动作必须 `&& w.fire`** |
| **锁存值 vs 组合值用反** | 写动作要用**握手拍的组合值**（`w.strb === 1111`），`b.resp` 要用**锁存值**（下一拍才发响应）⇒ 用反就差一拍 |
| **iverilog 不支持 DPI-C** | `import "DPI-C"` 只在 `npc/playground/vsrc/dpi-c.v` ⇒ 用 `SPINAL_SIM_DEBUG=0` 的 RTL，整条 DPI 链都不存在 |
| **`make sta`/`make lint` 没传 `PARTFORM`** | 它们生成的是**默认平台（`npc`，`resetPc=0x80000000`）**的 RTL ⚠ ⇒ 想让 iverilog 跑的 RTL 复位值是 CI 要求的 `0x30000000`，`sim-iverilog` 里必须**显式** `PARTFORM=ysyxsoc` |
| **"行号派生名"会让对比出现假差异** | SpinalHDL 把行号写进(a)行尾注释 `// @ X.scala l131`、(b)匿名 `when` 的信号名 `when_Xbar_l94` ⇒ 删空行就会"改名" ⇒ 直接 md5 会误判。**正确做法**：`sed 's\|//.*\|\|'` 去注释 + `sed -E 's/_l[0-9]+/_lN/g'` 归一化后再比 |
| **iEDA 的 SDC 不支持 `[all_inputs]`** | 写了会被**静默忽略**（等于没约束）；改成逐引脚后，**写一个网表里不存在的端口名会让 iEDA 直接 SIGABRT（错误 134）** ⇒ `npc/constr/npc.sdc` 已恢复原状，不要再乱加 |
| **ABC 映射是全局抽签** | 单次 STA 数字**不能当因果证据**；只有"触发器数量 / 周期数"这类确定性指标可靠（另见 §8.1 的限定）|
| **指令数变化 = 程序变了** | RTL 改动不可能改变仿真执行了多少条指令；数字变了先查 AM 侧 |
| **difftest 跳过设备访问** | CLINT/mtime 数值错误、CSR 计数值错误 ⇒ **difftest 抓不到**，只能靠定向测试 |
| 复位类型 | 项目要求**同步复位**（`Config.scala`）；`Reg(X()) init(Y)` 与 `RegInit(Y)` 等价；"确定会在被读之前被写"的寄存器**可以不复位**（刻意为之，不是 bug）|

---

## 10. 里程碑（建议的执行顺序）

| # | 做什么 | 验收标准 |
|---|---|---|
| **M0** | 工具链 | ✓ 已完成：iverilog 14.0 在位、能编译核心 RTL、`cells.v` 能编译 |
| **M1** | `npc/vsrc/tb_npc.v` + `npc/tools/bin2mem.py` + Makefile 两个目标；跑 `hello-riscv32e-npc.bin` | 打印出 AM 开头的 `ID = ysyx_23060082`（每个 AM 程序开头都会打，最干净的第一个目标）|
| **M2** | microbench | `MicroBench PASS`；若失败 ⇒ 用波形定位 X 传播 |
| **M3** | RT-Thread | 前置：克隆 `rt-thread-am` + 建 `patch/rt-thread-am/`；用 `ARCH=riscv32e-npc` 构建 |
| **M4** | 网表仿真（同一 TB + `NETLIST=…netlist.v.sim` + `CELLS=cells.v`）| 输出与 RTL 仿真**逐位一致**（`cells.v` 无时序）|
| **M5** | CI 形状对齐 | 目标名/参数 ✓、`make verilog` 复位值 `0x30000000` 与模块名 ✓、`patch/{rt-thread-am,ysyxSoC}/` ✓、`STUID/STUNAME` ✓、关 difftest/波形 ✓ |

**第一个 PR 建议只做 M1**：TB + 转换脚本 + 两个 make 目标 + `hello` 跑通。TB 一旦跑通，后面基本只是换镜像。

---

## 11. 待确认 / 风险（动手前先问用户）

1. **`rt-thread-am` 尚未克隆** ⇒ rtt 里程碑的前置条件；且它要以 `patch/` 形式交付（< 1MB）。
2. **`abstract-machine` / `nemu` 没有 `.git`** ⇒ 改动提交进根仓库；但 **CI 到底从哪个远端/分支拉？** 先确认。
3. **外层仓库远端是官方的** ⇒ CI 需要新的公开远端/fork（§7.2）。
4. `sim-iverilog` 内部**重新生成** RTL（而不是直接用 `make verilog` 的产物）**偏离了文档字面**，但符合"去掉 DPI 调用"的意图。若担心 CI 检查，可额外提供 `verilog-nodbg` 目标并在 PR 说明里写清理由。
5. **CI 判定依据是 stdout**（文档提到 nvboard 通过标准输入输出交互）⇒ TB 必须把 UART 字符打到 stdout，不要依赖波形。
6. 网表仿真**可能暴露不可综合代码**（这正是它的目的）⇒ 若发现，改动要小，并回归 `make perf` 验证周期逐位不变。

---

## 12. 常用命令速查

```bash
cd npc
# ---- 功能/性能基准（结构改动后都跑一遍；会触发外层仓库自动提交）----
CCACHE_DISABLE=1 make perf        # 必须得到 周期 11908252 / 指令 555445
# ---- 综合（不触发自动提交）----
make sta                          # 面积 24986.178（余量仅 13.8！）slack 1.004ns
make lint                         # verilator 静态检查（已带 waiver）

# ---- 生成 RTL（两条线，别互相覆盖）----
make verilog                      # ⇒ build/ysyx_23060082.v（PARTFORM=ysyxsoc ⇒ 复位 0x30000000，含 DPI 黑盒）
# sim-iverilog 内部应生成：SPINAL_SIM_DEBUG=0 PARTFORM=ysyxsoc ⇒ build/iverilog/ysyx_23060082.v（无 DPI）

# ---- 镜像 ----
cd ../am-kernels/benchmarks/microbench && make ARCH=riscv32e-npc
#   ⇒ build/microbench-riscv32e-npc.bin（★ 立刻拷走：ysyxsoc 构建会覆盖同目录）

# ---- iverilog ----
iverilog -g2012 -o build/iverilog/xx.vvp -s tb_npc <RTL> <TB>
vvp build/iverilog/xx.vvp +img=xxx.hex

# ---- 验证"外观改动是否中性"（本项目专用配方）----
norm() { sed 's|//.*||' "$1" | sed -E 's/_l[0-9]+/_lN/g' | sed 's/[[:space:]]*$//' | grep -v '^$' | md5sum; }
norm build/sta/.../ysyx_23060082.v      # 改动前
norm build/ivcheck/ysyx_23060082.v      # 改动后 —— 两个 md5 相同即"只改了命名/行号"
```

---

## 13. 更新记录

| 日期 | 基线 | 变化 |
|---|---|---|
| 2026-09-30 | `npc 771b2d2` | 初版（写在旧的 CI 副本里）|
| **2026-09-30** | **`npc a98086e`** | **重写为本版**：路径改为通用相对路径、更新基线、新增 §7.2（外层远端是官方）、§8.1（纯外观改动不重掷映射，实测）、§9 新增两条坑（`PARTFORM` 默认值、行号派生名的假差异）|

*本文件的"现状体检"（§3）与"实测补充"（§8.1）都是**真实跑出来的结果**，可直接信任；§4 的决策若要推翻，请在 PR 说明里给出理由。*

---

# 交接:当前工程状态与后续任务(AI 续作必读)

> 本段由上一轮 AI 追加。**读这一节就够了**,不必重读全文;冲突时以本节为准。

## 0. 一句话

"一生一芯" B 阶段工作区。**目标已从"做性能优化"转为"通过 1.11 的 CI(申请代码调试考核)"**:
RTL 功能已经正确(跑通 hello/microbench/rt-thread 并 `HIT GOOD TRAP`),现在卡在
**CI 的最后两关**:① 字符重复导致关键字匹配失败;② 面积超预算(16,500 µm²)。

## 1. 仓库与分支(CI 要求两项都必须推)

| 仓库 | 地址 | 分支 | 备注 |
|---|---|---|---|
| 主仓库 | https://github.com/chaoyulong/ysyx-workbench.git | **`master`** | CI 拉这个 |
| npc | https://github.com/chaoyulong/npc.git | **`main`** | 根仓库也跟踪 `npc/` 下的同名文件 ⇒ **改动要两处都提交** |
| tracer | 同上(主仓库内) | **`tracer-ysyx`** | 1.11 §5 硬性要求,**必须推送**;已推 ✓(由根 `Makefile` 的 `git_commit` 自动维护) |

⚠️ `navy-apps`、`nanos-lite` **不要提交**(已写入 `.gitignore`);`.ci-harness/`、`.yosys-sta-ci/`、
`.nvboard-ci/`、`.nvboard-nogui/` 是本地分析用的克隆,也已忽略。

## 2. CI(1.11)对我们代码的契约(实测自 harness 源码)

| 契约 | 我们的实现 |
|---|---|
| `make -C npc verilog` ⇒ `npc/build/ysyx_23060082.v`,PC 复位 `0x30000000`,**不含 DPI**,`module ysyx_23060082` 独占一行 | ✓ 已满足(`SPINAL_GEN_ENV` 固定 `SPINAL_SIM_DEBUG=0`) |
| `make -C npc sim-iverilog IMG=xxx`(xxx 为 `.bin`,内部自己转 hex) | ✓ |
| `make -C npc sim-iverilog-netlist IMG=xxx NETLIST=yyy CELLS=zzz` | ✓ |
| `make ARCH=riscv32e-npc`(首指令 0x80000000)/ `make ARCH=riscv32e-ysyxsoc run`(0x30000000) | ✓ |
| `patch/rt-thread-am/*.patch`、`patch/ysyxSoC/*.patch`(<1MB,`git format-patch`) | ✓ `rt-thread-am` 补丁已与本地树一致(可 `patch -R --dry-run` 自检) |
| NEMU 可编译(`nemu/tools/{kconfig,fixdep}` 需存在) | ✓ |

**CI 用上游代码**(不能依赖对这些的修改):`am-kernels`、`nvboard`(**`no-gui` 分支**)、`yosys-sta`(**`OSCPU/yosys-sta` 的 `ci` 分支**)。

## 3. CI 的 job 清单(`.ci-harness/.github/workflows/autotest.yml`)

`parse` / `setup` / **`hello`** / **`cpu-tests`** / **`riscv-tests`** / **`riscv-arch-test`** /
**`microbench`** / **`rt-thread`** / **`iverilog-microbench`** / **`yosys-sta`** /
`iverilog-netlist-microbench` / `more-tests` / `push-code`

## 4. 当前状态(最近一轮 CI 的结果)

| job | 状态 | 说明 |
|---|---|---|
| `iverilog-microbench` | ✅ PASS | 一直绿 |
| `hello` / `microbench` / `rt-thread` | 🟡 **跑通了但判定失败** | 已 `HIT GOOD TRAP`、统计表打印、`[sim] done` 正常退出;但 `monitor.py: Keyword not detected` ✗ |
| `cpu-tests` / `riscv-tests` / `riscv-arch-test` | 🟡 待复测 | 上一轮是 `difftest: dlopen(...) 失败 ⇒ Error 134`(已通过关闭 `CONFIG_DIFFTEST` 修掉,尚未复测) |
| `yosys-sta` | ❌ 面积超预算 | CI 口径 **27,546.96 µm²** vs 预算 **16,500** ✗ |

## 5. 已做的关键修复(按时间,均为已推送提交)

| 提交 | 内容 |
|---|---|
| `748021c` | **两份 RTL 分开**:`verilog`→`build/ysyx_23060082.v`(`SIM_DEBUG=0`,给 CI 综合);`sim/run`→`build/sim/…`(`SIM_DEBUG=1`,给 Verilator)。解决"谁最后跑谁赢"导致面积虚高 |
| `ca784cd` | `LDFLAGS += -Wl,--no-as-needed -lSDL2 -lSDL2_image -lSDL2_ttf` —— verilator 的链接规则是 `$(LINK) $(LDFLAGS) $^`,库排在 `nvboard.a` 前面会被 as-needed 丢掉 ⇒ `SDL_* undefined reference` |
| `0645eae` | 没有 `npc/tools/difftest/*.so` 时**不传 `-d`**(`$(if $(wildcard …))`),但 `.so` 仍在库里 |
| `40a8ea2` | **关闭 `CONFIG_DIFFTEST`**(`npc/include/config.h`)—— 参考 `.so` 是本机 glibc 2.44 编的,CI 的 glibc 2.35 `dlopen` 报 `version GLIBC_2.38 not found` |
| `f89c3be` | `run` 默认 `-b` 批处理(`npc.mk`/`ysyxsoc.mk` 里 `sdb = n`);`sdb=y` 才进 sdb。否则 CI 无终端 ⇒ 卡在 sdb 等 stdin |
| `b779601`→`290c02e` | `main.cpp` 里 `#ifdef __USE_NVBOARD__ while(1) nvboard_wait_quit();` 这行**注释掉**(保留 `#ifdef` 与声明),程序结束后不再死循环 |
| 681fa0f / ab0e266 | Revert 掉"`pmem.c` 里 `#ifndef __USE_NVBOARD__` 不打印"的改动(见 §6.1,已确认无关) |

## 6. 未解决的问题(下一任 AI 从这里开始)

### 6.1 ⭐ CI 上字符重复 ⇒ `Keyword not detected`

**现象**:CI 的 hello 输出每个字符**打印两次**:
```
IIDD  ==  yyssyyxx__2233006600008822
mmaaiinnaarrggss  ==  ''EETTPPUUXXBBZZBB''.
```
CI 用 `monitor.py --good=$KEY`(**KEY 由 mainargs 传入,如 `ETPUXBZB`**)且是**整行包含匹配**
⇒ 重复后 KEY 变成 `EETTPPUUXXBBZZBB` ⇒ 匹配失败。
(1.11 文档原话:"CI 使用的 nvboard 是通过标准输入输出交互的,你可能会发现**一个字符输出多次**,**需要你自行解决**")

**已排除**:
- **nvboard 本身**:`no-gui` 分支里唯一打印点是 `src/term.cpp:111`(`putchar+fflush(stdout)`),
  调用链 `uart.cpp:55 term->feed_ch(tx_data)` 只在**停止位**触发一次 ⇒ 每字符**只打一次** ✓
- **`pmem.c` 的 `putc(…, stderr)`**:ysyxsoc 的 UART 是 **SoC 里的 16550(RTL)**,走 AXI,
  **根本不经过 `pmem.c`** ⇒ 与 CI 无关(此结论已被用户确认,A/B 实验也支持)

**仍存的怀疑**:`monitor.py` 用 **`pty.openpty()`** 接子进程 stdout(`stdout=slave`)⇒ pty 的
ECHO/行规程可能**把输出回显一次** ⇒ 本地(真终端/文件)看不到,CI 才出现。

**下一步(最快的验证)**:
```bash
cd ~/Desktop/ysyx-workbench
NVBOARD_HOME=$PWD/.nvboard-nogui make -C am-kernels/kernels/hello ARCH=riscv32e-ysyxsoc run | cat
```
- **能复现重复** ⇒ 问题在 `no-gui` nvboard 的初始化/`term` ⇒ 继续查它
- **不复现** ⇒ 问题在 `pty`/`monitor.py` 层 ⇒ 需要另想"让 KEY 只出现一次"的办法
  (例如让程序把 KEY 单独打印成一行,或换掉输出路径)

### 6.2 ⭐ 面积超预算(CI 口径 27,547 / 预算 16,500)

- **CI 口径** = `OSCPU/yosys-sta@ci`(`syn` → **`fix-fanout`(iEDA)** → `sta`),
  命令见 `.github/workflows/autotest.yml`:`make -C yosys-sta sta DESIGN=ysyx_$STUID CLK_FREQ_MHZ=500 CLK_PORT_NAME=clock RTL_FILES=$VFILE`
- **实测**:同一份**干净 RTL**(`simDebug=false`,`PerfReg/ItraceReg/MtraceReg` 均为 0 次)⇒ **27,546.96 µm²,DFF 3198**
  ⇒ **debug 模块确实没进综合**,"面积大"不是它导致的 ✓
- **本地老流程**(`~/tools/yosys-sta`,`npc/Makefile` 的 `sta`)⇒ **24,208.93 µm²,DFF 3198**
  ⇒ 差的 ~3,300 µm² 来自**流程差异**(`fix-fanout` 插 buffer + ABC 映射到 3 输入门为主)
- **结论**:优化要以 **CI 口径(27,547)** 为基线,要减 **~11,000 µm²(40%)** ✗
- **用户的优化计划**(尚未开始):去掉 dcache、缩小 icache,再做其他优化 —— ⚠️ 但按数据估算,
  这两项只能省 ~1,500-2,500;大头是 **RegFile(32×32≈1024 DFF≈4,600 µm²)**、流水线级寄存器、CSR
- **本地用 CI 口径量面积**(已备好 `npc/build/sta-ci/`,工具在 `.yosys-sta-ci/`):
```bash
make -C npc verilog          # 必做:生成干净 RTL,否则量的是调试版(+3,000)
cd .yosys-sta-ci && make sta DESIGN=ysyx_23060082 CLK_FREQ_MHZ=500 CLK_PORT_NAME=clock \
    RTL_FILES=$PWD/../npc/build/ysyx_23060082.v O=$PWD/../npc/build/sta-ci
grep "Chip area" ../npc/build/sta-ci/ysyx_23060082-500MHz/yosys-fixed.log
```

## 7. 工具/环境的既定事实(省得重新踩)

- 本地 `~/tools/yosys-sta`(老流程)`make sta` ⇒ 面积/频率写 `npc/build/sta/…/synth_stat.txt`、`*.rpt`
- `.yosys-sta-ci/` = CI 用的 `yosys-sta@ci`(已 `make init` 装好 `bin/iEDA`)
- `.nvboard-nogui/` = CI 用的 nvboard `no-gui` 分支;`.nvboard-ci/` = 主分支
- `.ci-harness/` = CI 服务端代码(`autotest.yml` / `monitor.py`,判断逻辑都在里面)
- 跑 Verilator 模型前常要 `export CCACHE_DISABLE=1`(沙箱里 ccache 只读)
- 四值/网表:`make -C npc sim-iverilog IMG=…`、`… sim-iverilog-netlist … IV_ARGS="+no_ebreak_stop"`
  - 已知遗留:**1 拍写数据含 X**(`cyc=96 addr=80008ff0`,没打到设备,不影响结果),网表同样复现

## 8. ⚠️ 陷阱清单

1. **`npc/build/ysyx_23060082.v` 会被覆盖**:每次综合前先 `make -C npc verilog`,
   并确认 `grep -c PerfReg npc/build/ysyx_23060082.v` = **0**(否则量到的面积大 ~3,000)
2. **AM 编译带 `-Wall -Werror`**(`abstract-machine/Makefile:80`)⇒ 未使用的变量/函数声明就是编译错误
3. **`run` 默认 `-b`(批处理)**;要交互调试必须 `make … run sdb=y`
4. **`CONFIG_DIFFTEST` 现在是关的**;打开前必须保证 `.so` 是**CI 的 glibc 能加载的**(本机编的 2.38 不行)
5. **`.so` 已入库但 CI 不用它**(在 `npc/tools/difftest/`,glibc 原因);AM 侧"没有就不传 `-d`"的兜底仍保留
6. **`rt-thread-am` / `ysyxSoC` 不进 git**,只通过 `patch/` 记录 ⇒ 改完要重新生成补丁并用
   `cd rt-thread-am && patch -p1 -R --dry-run -f < ../patch/rt-thread-am/0001-*.patch` 自检
7. **根 `Makefile` 的 tracer 会自动 `git add -A`**(扫 `tracer-ysyx` 分支)⇒ 新增的临时目录
   一定要先写进 `.gitignore`,否则会被夹带(历史上 `navy-apps` 就被夹带过)
8. **改动要在两个仓库各提交一次**(根仓库也跟踪 `npc/` 下的文件)

## 9. 下一步 TODO(建议顺序)

1. [ ] **定位 "字符重复"**:按 §6.1 的命令本地复现 ⇒ 若是 nvboard 则查 `term/uart` 初始化,
       若是 pty/monitor 则想"让 KEY 只出现一次"的方案 ⇒ 改完再交 CI
2. [ ] **复测其余 job**:`cpu-tests` / `riscv-tests` / `riscv-arch-test`(关闭 difftest 后应能跑完)
       与 `iverilog-netlist-microbench`
3. [ ] **面积优化**(§6.2):以 CI 口径 27,547 为基线,目标 ≤ 16,500;优先看 RegFile、
       流水线级寄存器、CSR,其次才是 dcache/icache;每轮都用 §6.2 的命令量
4. [ ] 收尾:四值/网表的"1 拍 X"问题(§7),以及 §8 的检查清单过一遍后再提交 CI

## 10. 2026-11-07 CI 42336277202 排查更新（优先于前面的旧状态）

- UART 重复输出已修复并推送：根仓库 `7210bb6`、NPC `7b741eb`。
  NVBoard 的 `run` 同时向 Verilog 和 C++ 定义 `__USE_NVBOARD__`，
  SoC UART FIFO 在该宏下关闭 `$write`；最新 CI 的 hello、microbench 已通过。
- cpu-tests、riscv-tests、riscv-arch-test 的生成文件丢失来自并发构建：CI 设置
  `MAKEFLAGS=-j4`，多个测试同时进入 NPC，共用 `build/obj_dir`，每次又删除这个目录。
  `npc/Makefile` 的 run/sim/wave 现通过同一个 `flock` 递归进入实际目标，
  锁从 RTL 生成前一直保持到仿真结束。仅锁 Verilator 配方不足以保护生成阶段。
- RT-Thread 的零地址异常发生在 bootloader，尚未进入串口初始化。
  已用 GCC 11.4 复现：遗漏的三字节 `.srodata.CSWTCH.13` 成为孤立段，
  使 Flash 中 `_data_load_start` 未按四字节对齐；SSBL 的 `lw` 触发异常，
  此时 mtvec 为零，随后在地址零取到非法指令。链接脚本改为收集 `*(.srodata*)`，
  同时保证这些常量落在 bootloader 的 rodata 拷贝范围内。
- RT-Thread 追加补丁 `patch/rt-thread-am/0002-fix-ci-uart-input-and-optional-fceux.patch`：
  缺少可选 fceux-am 时跳过集成，移除串口预置命令，让 CI monitor 驱动 shell；
  把 AM 的 `0xff` 无数据标记转换为 RT-Thread 期望的 `-1`（RISC-V 的 char 默认无符号）。
- 验证日志与 GCC 11 复现工程在 `/tmp/ysyx-ci-repro/`；最新 CI 原始日志为
  `/tmp/ysyx-ci-run-42336277202.log`。不要把临时复现工程加入提交。
- 并发锁验证覆盖生成、编译、运行全过程以及错误返回；实际 `cpu-tests` 以 `-j2`
  同时发起 add、if-else，两项均 PASS（复用现有仿真 RTL，`MILL=true git_commit=`）。
  其余测试集尚未全量重跑，不能据此声称全部指令测试已通过。
- RT-Thread 已使用 GCC 11.4、am-kernels 的 ci 分支、no-gui NVBoard 和同一份
  CI monitor 验证：help、am_hello、am_microbench、echo 关键字完整通过，
  monitor 输出 `Keyword detected, OK!`，进程返回零；日志为
  `/tmp/ysyx-ci-repro/ci-monitor.log`。
