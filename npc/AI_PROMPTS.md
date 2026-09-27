# 给 AI 助手的提示词（ysyx-workbench / NPC）

这个工作区有几个容易让 AI 走弯路的特点：**三个仓库混在一个目录**、**ysyxSoC 的本地改动推不上去**、以及一批"看起来能跑通、其实是侥幸"的隐含约束。

下面几段提示词都是**自包含**的，可以直接整段复制给任何 AI 助手。按用途分段，**按需选用，不必全贴**。

---

## 提示词 1：工程总览与代码地图（入门必看）

```
我在做 ysyx（一生一芯）的处理器设计，工作区在 /home/cyl/Desktop/ysyx-workbench。
请读这段背景，然后回答我的问题，不要臆测工程结构。

【工作区顶层】
- npc/            ← 我自己写的 CPU，【我自己的 git 仓库】(远端 chaoyulong/npc)
- ysyxSoC/        ← 官方 SoC 框架，【远端是官方仓库 OSCPU/ysyxSoC，本地改动推不上去】
- abstract-machine/  ← AM 运行时(裸机库)
- am-kernels/     ← 测试程序(cpu-tests / microbench / 各种 kernel)
- nemu/ nvboard/ fceux-am/   ← 参考模拟器 / 板级外设 / FC 模拟器

【npc 的架构】
- 用 SpinalHDL 1.12.3 + Scala 2.13.14 写，构建工具 mill
- 多周期 RV32E，5 级流水 IFU → IDU → EXU → LSU → WBU，
  级间握手用自定义的 pipelineConnect / pipelineConnectLast
- 复位 PC = 0x30000000(flash XIP)
- 源码在 npc/playground/src/*.scala：
  23060082.scala(顶层接线/流水线) IFU IDU EXU LSU WBU RegFile Decoder CSR
  CLINT icache Xbar NPC_TOP DPI-C
- npc/playground/Config.scala：顶层名由环境变量 SPINAL_TOPNAME 选(默认 NPC_TOP)；
  SPINAL_SIM_DEBUG=0 时关掉 itrace/mtrace 黑盒(跑 STA 用)
- npc/csrc/ 是仿真的 C 侧：pmem.c(flash/sram/psram/sdram 的存储，走 DPI-C)、
  regfile.c、device/*(串口/键盘/VGA/定时器)、sdb/*(调试器)、
  trace/*(itrace/mtrace/ftrace/perf 计数器)

【地址映射】mrom 0x20000000(4K) / sram 0x0f000000(8K) / flash 0x30000000(XIP)
             psram 0x80000000 / sdram 0xa0000000

【重要事实】flash 挂在 APB 上(APBSPI)，【不支持突发】，
发往 flash 的 AXI 突发会被 AXI4Fragmenter 自动拆成单拍 APB 事务；
SDRAM 走 AXI(sdramUseAXI=true)。
```

## 提示词 2：构建 / 仿真 / 性能测试流程

```
ysyx 的构建与测试流程（npc = /home/cyl/Desktop/ysyx-workbench/npc）。

【在 npc/ 下】
make sim        # 编译并用 Verilator 跑(需要镜像参数，一般由 AM 流程调用)
make run        # 带 nvboard 跑 —— 【会卡住不退出，测 ysyxSoC 时不要用】
make test       # SpinalSim 单元测试
make verilog    # 只生成 Verilog(产物 build/ysyx_23060082.v)
make perf       # 跑 microbench(test 规模)并打印周期数/指令数/IPC + PERF 计数器
make sta        # 综合(yosys-sta)，默认 nangate45，产物 build/sta/ysyx_23060082-500MHz/
make sta STA_PDK=icsprout55    # 换工艺

【跑单个测试(AM 流程)】测 ysyxSoC 时【必须用 sim + sdb=n】：
cd am-kernels/tests/cpu-tests && make ARCH=riscv32e-ysyxsoc ALL=shift sim sdb=n
cd am-kernels/benchmarks/microbench && make ARCH=riscv32e-ysyxsoc sim mainargs=test sdb=n

【已知环境坑】
1. 编译常报 "ccache: error: Read-only file system" → 命令前加 CCACHE_DISABLE=1
2. 不要在用户自己的构建/仿真正在跑时并行跑另一个仿真 —— 会互相破坏
   build/obj_dir，报 VysyxSoCFull___024unit.h 找不到之类
3. npc 的 itrace 要真正写进 -l 指定的文件，必须打开 include/config.h 里的
   CONFIG_LOG 【和】 CONFIG_TRACE、CONFIG_ITRACE —— 只开 TRACE 的话文件是空的
   (log_write 被 IFDEF(CONFIG_LOG,...) 包着)
4. itrace 里除指令行外还有日志行和结尾回显的 ringbuf(重复指令)，解析要严格
   匹配"行首就是 0x........:" 才能拿到正确条数
```

## 提示词 3：面积与时序（STA）与预算

```
ysyx 的电路质量约束与测量方法。

【硬约束】面积 ≤ 23000 µm²（工艺 nangate45）、时序要满足 500 MHz。
面积限制的判定工艺是 nangate45；icsprout55 是流片工艺，同一份 RTL 两者差约 30%，
【面积数字不可跨工艺比较】。

【怎么看结果】
build/sta/ysyx_23060082-500MHz/synth_stat.txt  → "Chip area for module"
build/sta/ysyx_23060082-500MHz/ysyx_23060082.rpt → 最差路径表(Endpoint/Slack/Freq)
  grep 'core_clock' *.rpt | head -3       # 最差路径 + slack + 可达频率
  grep -c 'VIOLATED' *.rpt                # 应为 0

【关键经验】
- yosys 会【自动裁掉"只写不读"的寄存器/位】——所以"声明了但用不到"确实不花面积，
  但这是脆弱的：哪天那几位被读了，面积会悄悄涨回来。要省就直接别声明。
- 实测过：ABC 换面积导向(SYNTH_STRATEGY="AREA 0")能省约 1056 µm²，
  可以用富余的时序换紧缺的面积(不用改脚本，命令行覆盖即可)
- 面积模型(已用实测校准到 ~1%)：面积 ≈ 14200 + 5.6 × 触发器数(nangate45)，
  其中 icache 每行 = 数据(lineBytes×8 位) + tag(32−log2(lines)−log2(lineBytes) 位) + 1 位 valid
- 该流程有 ±100~250 µm² 的 ABC 映射抖动，小于 ~100 µm² 的改动无法验证
- STA 日志里 "Warning: val outside table ranges: val = 0" 是【无害的】：
  独立综合 NPC 时 io_master_* 这些 AXI 从机输入端口悬空，传播时 slew/负载算成 0，
  超出 liberty 表范围被钳到边界。它只影响从这些端口出发的路径，
  最差路径是内部 reg2reg(pcNext)，所以面积/频率结论有效。
  想消掉就在 npc.sdc 里用【显式端口名】写 set_input_transition
  (iEDA 的 [all_inputs] 这类集合对象可能不生效)
```

## 提示词 4：icache 与 cachesim（DSE 方法论）

```
ysyx 的性能优化方法论(对应官方文档 B3 的 cache 设计空间探索)。

【核心公式】TMT(总缺失时间) = 缺失次数 × 缺失代价。
优化分两半：降"缺失次数"(增大 cache / 增大块) 和 降"缺失代价"(突发传输 / 校准延迟)。

【当前 icache】npc/playground/src/icache.scala
- IcacheParams(lineBytes=16, lines=8)：8 行 × 16 字节 = 128B 容量，直接映射
- 一行 128 位(4 个字)，取指缺失时发【4 拍 AXI 突发】(ar.len=3, size=BYTE_4, burst=INCR)
- 突发与逐拍拼装在 ysyx_23060082_Axi4_Ctrler_ReadOnly_Burst 里完成，
  对 icache 只暴露"一次读回一整行"；命中按 pc 的块内字偏移选字，
  【缺失时必须按"请求时锁存的字偏移"选字】(不是最后读到的那拍)
- 调试输出 io.miss / io.missDone 供 IFU 的 PerfReg 统计命中率与缺失代价

【cachesim】npc/tools/cachesim/ —— 独立的 C 程序(只依赖 libc，与工程零耦合)
- trace-driven：回放取指 PC 序列，只维护 valid/tag/替换元数据，不执行指令
- 用法：./cachesim <trace文件> [--lines N] [--block B] [--ways W] [--from ADDR]
        [--misscost C]，输出单行 RESULT: 便于脚本扫参数
- trace 来自 NPC 仿真日志(AM 平台 mk 会传 -l build/ysyxsoc-log.txt)
- 【对拍是验收标准】：同一份 trace 下，cachesim 的缺失次数必须和 RTL 的
  make perf 输出里 "Icache access: ... miss=..." 一致(实测误差 0.07%~0.19%)

【实测过的 DSE 结论】等容量下【加大块远优于加行数】——
同一程序、同为 128B 容量：
  16行×8B  = 缺失 70685, 命中率 87.85%, 面积 22276.70µm², 599.1MHz
  8行×16B  = 缺失 47522, 命中率 91.83%, 面积 21735.39µm², 623.2MHz   ← 更优
原因：tag 按【行数】存，行数减半 tag 从 400 降到 200，所以"行数少+块大"
      在等容量下反而更省面积
代价：块越大缺失代价越高(4 拍 60.32 cyc vs 2 拍 42.55 cyc)，
      所以 TMT 的优势(−4.7%)远小于缺失次数的优势(−32.8%)
```

## 提示词 5：ysyxSoC 外设与访存延迟校准

```
ysyxSoC 这边和性能强相关的几件事。

【访存延迟校准】官方要求把"模拟中的设备访存延迟"校准到和真实设备一致，
否则 TMT 的统计没有意义。做法是在设备前面插延迟模块，按
  (t_k − t0) × R = t_k' − t0
把响应延迟交付给 CPU，其中 R 是"处理器频率 / 设备频率"的比例。
- perip/amba/apb_delayer.v  —— APB 设备(flash/串口等)，R = 3.76
- perip/amba/axi4_delayer.v —— AXI 设备(SDRAM 等)，读/写通道
  · 计时起点 t0 是 arvalid 有效那一拍(含等 arready 的握手时间)
  · 计数器 += MUL(=R×S 定点)表示小数倍率，设备完成时 counter >>= S 得到延迟量，
    倒数到 0 再交 rvalid
  · 写通道不用 FIFO：AXI 写事务无论几拍只有一个 B 响应
  · AR/W 通道必须直通(延迟了会把延迟算两次)

【flash 与 SDRAM 的突发能力】
- flash 挂在 APB 上(APBSPI)，【物理上不支持突发】；AXI4Fragmenter 会把突发
  拆成单拍 APB 事务，所以对 flash 发突发【不会变快】
- SDRAM 的突发是【在 sdram_axi_pmem.v 层拆开】的：
  core 里的 inport_len_i 【只声明、从未使用】，一粒一拍。
  所以一次 4 拍突发 = 4 次独立的 SDRAM 访问(地址连续 → 同一行，不用重新激活)
- perip/sdram/sdram.v 是【SDRAM 芯片模型】(不是控制器)：
  MODE_REG 是 localparam(BL=1, CAS=2)，在上电初始化序列里用 LOAD_MODE 发给颗粒，
  之后不再改；4 片芯片实例在 ysyxSoC/src/SoC.scala 里生成
```

## 提示词 6：已知的坑（能省你几小时）

```
我在这个工程里踩过的坑，请在设计/调试时优先怀疑这些。

【AXI 突发相关】
1. 自研 Xbar(npc/playground/src/Xbar.scala) 的仲裁器和读状态机原本用 r.fire
   结束读事务，【没看 r.last】。单拍访问看不出问题，一旦发突发，
   第 1 拍通过后状态机就回 Idle，把被门控的 r.valid 掐掉，
   第 2 拍的 rvalid 就消失了；而 r.payload 是无条件透传的，
   所以波形上表现为"rlast 高而 rvalid 低"——看到这个现象先查这里。
2. SDRAM 芯片模型 sdram.v 里，各 always 块的判断链必须统一遵守
   【命令优先，突发结束放最后】。曾经把 data_count >= BURST_LENGTH 排在
   cmd == READ/WRITE 前面，BL=1 时 data_count 会在读出数据后停在 1，
   于是【背靠背的第 2 个读命令被当成"突发结束"吞掉】——
   rw_state 回 idle、col_address 被冲成 0、read_data 被清零、dq 变 z，
   控制器采到 0。现象是"flash 取指正常，一进 SDRAM 就奇数 32 位字全 0"。
3. 同一个 SDC/判断链的优先级，如果在多个 always 块里各写一遍，
   【很容易写成不一致】——这是上面两个 bug 的共同根源。

【SpinalHDL 相关】
4. 寄存器在 when 分支里不赋值就是保持，【不要写 .otherwise { x := x }】：
   对寄存器是多余的，而且如果别的分支是【分片赋值】，
   整体赋值会触发 ASSIGNMENT OVERLAP(精化期报错)。它只在分片恰好等于
   全宽时"侥幸"不报错，所以这种坑会等到改参数时才爆发。
5. when(...) 链【不能】被 Scala 的 if 块打断，.elsewhen 必须语法上连续；
   要按参数生成分支，用链式变量 + for 循环：
     val chain = when(cond0){...}
     for (i <- 1 until n) chain.elsewhen(condI){...}
     chain.otherwise{...}
6. import spinal.lib._ 会引入 spinal.lib.math 包，【遮蔽 scala.math】，
   于是 math.max(...) 报 "object max is not a member of package spinal.lib.math"。
   用 (a max b) 或 scala.math.max 即可。
7. AXI 字段类型不一致：ar.addr/id/len/size 是 UInt，而 ar.burst/lock/cache/resp
   是 Bits。SpinalHDL 的常量 Axi4.size.BYTE_4 是 Bits，赋给 UInt 的 ar.size
   必须 .asUInt；Axi4.burst.INCR 可以直接用。
   (arlen 没有预定义常量；arsize 是"每拍字节数的 log2"，与块大小无关，
    块大小靠 arlen 表达)
8. log2Up(1) = 0(log2Up 的实现是 (x-1).bitLength())。所以当块大小/行数为 1 时，
   基于它的寄存器会变成 0 位而报错 —— 加参数化时要么给保底 max(x,1)，
   要么用 require 把边界钉死。

【仿真与验证】
9. "功能测试全过"【不足以】说明改动正确：上面坑 1 和坑 2 都是
   microbench 能跑通、但取指数据已经错了才暴露的。
   对 cache 类改动，一定要用 cachesim 对拍缺失次数；
   对时序类改动，用"读命令数 vs dq 有效拍数"这类【统计不变式】。
```

## 提示词 7：当前进度与下一步（接手工作时贴这段）

```
ysyx NPC 项目的当前状态(截至 2026-09-12)。

【已确定的设计】
- icache: IcacheParams(lineBytes=16, lines=8)，直接映射 8 行 × 16B = 128B，
  取指缺失发 4 拍 AXI 突发(arlen=3)
- STA(nangate45): 面积 21735.39 µm²(预算 23000，余 1264.6)，
  最差路径 pcNext_31，可达 623.2 MHz(目标 500，余 123.2)，全部 MET
- 性能(microbench test): 命中率 91.83%，缺失 47522，缺失代价 60.32 cyc/miss，
  TMT 2866587，总周期 15228461，Scored time 6662.01 ms

【性能记录】npc/PERF.md 是按 commit 的记录表，每行含周期数/指令数/IPC/
综合频率/综合面积/取指总周期/命中率/缺失avg 等 24 列。
npc/README.md 按日期记录每次做了什么、为什么、实测数据。

【仓库边界(重要)】
- npc/ 是我自己的仓库(远端 chaoyulong/npc)，正常提交推送
- ysyxSoC/ 远端是官方 OSCPU/ysyxSoC，【本地改动推不上去、重新 clone 会丢】，
  所以固化成了 npc/ysyxSoC-local.patch(基线 commit 记录在文件头)，
  用 npc/tools/make-ysyxsoc-patch.sh 重新生成，改完 ysyxSoC 记得刷新

【下一步方向(按性价比)】
1. 用富余的面积/时序换性能：目前 icache 已经调到预算内最优
   (再往上 16行×16B≈28.5k、8行×32B≈27.4k 都超预算)
2. 继续压缺失代价：8行×16B 的 4 拍缺失代价 60.32 cyc，
   而 2 拍只有 42.55 cyc —— 要么改 SDRAM 的 MODE_REG 让颗粒进 BL 模式，
   要么让 sdram_axi_core 真正用起 inport_len_i，把 4 拍的开销压下来
3. dcache 暂不考虑：逻辑比 icache 复杂得多(store/字节使能/写通道/写回策略)，
   以当前预算基本不可能塞进去，官方文档也说那是最后一步
4. 延迟校准的准确性：TMT 里"代价"那一项取决于 apb_delayer/axi4_delayer
   的 R 值是否与真实设备一致
```

---

## 附：一句话版本的提示词（占位最小，适合塞进系统提示）

```
这是一个 ysyx(一生一芯)的 RISC-V 处理器项目，工作区 /home/cyl/Desktop/ysyx-workbench。
npc/ 是用 SpinalHDL 写的多周期 RV32E CPU(5 级流水，我的仓库)；
ysyxSoC/ 是官方 SoC 框架(改动推不上去，本地补丁固化在 npc/ysyxSoC-local.patch)；
abstract-machine/ 与 am-kernels/ 提供裸机运行时和测试程序。
约束：面积 ≤23000 µm²(nangate45)、500 MHz。用 make perf 测性能、make sta 测面积；
测 ysyxSoC 必须用 make ... sim sdb=n(不要用 run，会长住)。
性能方法论见官方文档 B3：TMT = 缺失次数 × 缺失代价，用 npc/tools/cachesim 扫 cache 参数，
并与 RTL 的 Icache access 缺失次数对拍验证。
详细背景见 npc/README.md 与 npc/PERF.md。
```

---

## 提示词 8：工程全貌 + 当前交接状态（**接手请先整段读完**）

```
我在做 ysyx（一生一芯）的 NPC 处理器。下面这段是【自包含】的工程说明与当前进度，
读完请先复述你的理解与下一步计划，再动手改代码；改完必须按第 6 节的流程自验。

════════════════════════════════════
一、仓库与目录（有两个嵌套的 git 仓库，别搞混）
════════════════════════════════════
工作区: /home/cyl/Desktop/ysyx-workbench/
  npc/                ★ 主战场。独立 git 仓库, 远端 github.com/chaoyulong/npc, 分支 main
  nemu/               NEMU 参考模拟器(difftest 的 ref 侧就在这里改)
  abstract-machine/   AM 裸机运行时; 各平台设备驱动在 am/src/riscv/{npc,ysyxsoc}/
  am-kernels/         测试程序(cpu-tests / benchmarks/microbench)
  ysyxSoC/            SoC(外设/内存/AXI 互连), npc 平台不用它
  nvboard/            板级仿真(FPGA)
★ 外层 ysyx-workbench/ 也是一个 git 仓库(包含 AM/NEMU/ysyxSoC 等)。
  npc/ 的代码改动要在 npc/ 里 commit + push origin main;
  改 AM/NEMU 则在外层仓库(注意: ysyxSoC 的本地改动推不上去, 远端被课程锁定)。

npc/ 内部结构:
  playground/src/*.scala     处理器 RTL(IFU/IDU/EXU/LSU/WBU/icache/dcache/CSR/Decoder/...)
  playground/Config.scala    CpuConfig(按平台选) + SpinalToVerilog 入口
  playground/vsrc/dpi-c.v    DPI-C 黑盒(PerfReg/ItraceReg/MtraceReg 的 Verilog 侧)
  csrc/                      仿真主程序: cpu-exec.c(退休/主循环) monitor.c(命令行/SDB)
                             difftest.c(新, 进行中) pmem.c regfile.c trace/*.c
  include/                   h 头: cpu-exec.h(CPU_state) trace.h(Verilator 层次路径宏) pmem.h
  constr/npc.sdc             时序约束(500MHz)
  PERF.md / README.md        ★ 性能数据与设计原因的正式记录, 改完必须更新

════════════════════════════════════
二、处理器结构（都是 SpinalHDL 1.12.3 / Scala 2.13）
════════════════════════════════════
五级顺序流水:  IFU | IDU | EXU | LSU | WBU
  - 级间用 pipelineConnect(prevOut, thisIn, thisOut, flush, block); Flow 没有 ready
  - IDU 是组合逻辑(译码 + RF 读 + 前递), 其它级有寄存器
  - 退休(提交)点 = WBU; itrace 黑盒挂在 WBU, 用 itraceRetireValid/Pc/Instr/PcNext 暴露给 C 侧
  - icache: 8 行 × 16B, 4 拍突发, 独占一条 AXI 读通道
  - dcache: 4 项 × 1 字(4B), 直接映射, 写穿 + 写不分配, 独占 LSU 的 AXI 口
  - CSR: mepc/mcause/mtvec/mstatus(+ mcycle/minstret, 32 位拆分)
  - 异常: 统一通道 CsrCtrl.trapEnter(任何阶段的异常) + CsrCtrl.excCause(4 位异常号)
  - 两个顶层: NPC_TOP(npc 平台, 只有 CPU) / ysyx_23060082(ysyxsoc 平台, 接 SoC)
  - ISA: RV32E(16 个通用寄存器) + Zicsr + fence.i; 无 M/A/F/D(enableMul/enableDiv 都是 false)

平台差异(Config.scala):
  CpuConfig.forPartform(PARTFORM) → npc: resetPc=0x80000000(规范值) / ysyxsoc: resetPc=0x30000000(SPI flash 启动)
  Makefile 已经把 PARTFORM 传进 SpinalHDL 生成环境(SPINAL_GEN_ENV / SPINAL_SIM_ENV);
  生成时会打印一行 [Info] SpinalToVerilog: top=... partform=... resetPc=0x... , 用来确认平台选对了。

════════════════════════════════════
三、常用命令（★ 是最常用的）
════════════════════════════════════
cd npc
  ./mill playground.compile        ★ 只做 Scala 编译/elaborate 检查(最快, 改 RTL 后先跑这个)
  make verilog                     ★ 生成 RTL(会重新 elaborate); 失败时先看 build/v.log
  make perf                        ★ ysyxsoc 平台跑 microbench 并打印全部性能计数器
  make sta                         ★ yosys-sta(nangate45, 500MHz): 面积 + slack, 结果在 build/sta/
  make sim / make wave             npc 平台仿真(/波形); make wave 会开 gtkwave(make wave GTKWAVE=surfer 可换)
  make test                        SpinalSim 自测

跑 AM 测试(在 am-kernels 里):
  cd am-kernels/tests/cpu-tests && make ARCH=riscv32e-npc     ALL=shuixianhua    # npc 平台
  cd am-kernels/tests/cpu-tests && make ARCH=riscv32e-ysyxsoc ALL=shuixianhua    # ysyxsoc 平台
  结果看同目录的 .result 文件(日志 *-log.txt 常常是空的, 不要被骗)

✗ 两个高频坑:
  1) make wave 与 make perf 之间必须 `rm -rf build/obj_dir`
     (wave 用 -D__GET_WAVE__ 编译, 产物与 perf 不兼容; 否则报 "打开依赖文件 xxx.d 失败")
  2) 在受限沙箱里构建时 ccache 会写不了 ~/.ccache → 用 CCACHE_DISABLE=1 make ...
     (正常终端里不需要)

════════════════════════════════════
四、当前性能(已记录在 PERF.md, 以那里为准)
════════════════════════════════════
最新提交 f1b8c26(异常处理) / 复核提交 f292f79:
  周期 12,309,885 | 指令 582,020 | IPC 0.0473 | 面积 24,701.03 um2 | slack +0.989ns(约 989MHz)
  microbench PASS; shuixianhua 在 riscv32e-npc 与 riscv32e-ysyxsoc 双平台均 PASS
关键计数器: Icache miss 52,810(avg 60.83) | LSU mem rd 64,258(avg 115.34) | LSU mem wr 57,963(avg 20.58)
            Dcache access 75,728 / miss 64,258 / 命中率 15.14%

★ 硬约束(不要越界):
  - 面积上限 25,000 um2(nangate45), 当前 24,701 → 只剩约 299 um2!
    (同一份 RTL 的 STA 结果是确定的, 不会因为重跑而变; 但只要改了 RTL 就必须重跑 make sta)
  - AXI 突发长度 ≤ 4 拍(8 拍会让 icache 缺失代价暴涨, 已实测否决)
  - 评分 = 周期 + 面积; 频率只要 slack > 0 即可, 不要用"可达频率"的数字做判断(它随重映射抖动 ±10%)
  - 周期预算几乎可加: LSU 读等待 7.4M(60%) + icache 缺失 3.2M(26%) + LSU 写等待 1.2M(10%)
    → 机器是【访存延迟受限】, 任何优化都要先问"它减少的是哪一块"

════════════════════════════════════
五、已完成的工作(按提交)
════════════════════════════════════
519cf4f  五级流水线
0690d2c  icache 8 行 × 16B(4 拍突发)
...
f9d00c5  IFU 遇到 jal/jalr 立即停取指, 消除错路径取指            → 周期 -6.1%
1d85319  D-Cache(4 项 × 1 字, 写穿/写不分配, 独占 AXI)           → 周期 -2.8%, 命中率 15.14%
21e3319  fix(npc平台): NpcMemRW 拆分读写地址(同一拍可同时读+写)   → 修复 npc 平台随机挂死
f1b8c26  异常处理(统一通道 trapEnter+excCause, 各级产生, LSU 生效) + mstatus  → 零周期成本
c252c6c  同步复位 + mcycle/minstret 32 位拆分

★ 三个必须记住的设计结论:
  1) 在 LSU 生效异常是【精确】的, 因为本设计没有分支预测、且重定向由 EXU/LSU 产生,
     错路径指令在到达 LSU 之前一定被 flush。
     ⚠ 这是靠"距离"维持的不变量: 一旦加了分支预测或流水线变深, 必须把生效点搬到 WBU。
  2) ecall 的 mcause 走 a5(AM 约定), 不能改成规范的 11:
     AM 的 __am_irq_handle 把 mcause 当"事件号"(yield() = li a5,-1; ecall),
     而且它对 0~19 一律 ev.event = EVENT_SYSCALL 且 c->mepc += 4。
     所以 causeIn := Mux(csrCtrl.ecall, rfReadData /*完整32位*/, excCause.resize(32))
  3) dcache 的两个握手坑(症状都是"错数据"而不是挂死):
     - 不能用 rspOut.valid 判"读命中"(它含上一笔 store 的 b 响应/上一笔缺失的完成) → 必须用 readHit
     - Idle → WaitMem 必须等 dcache.io.reqIn.fire, 否则请求被丢掉

════════════════════════════════════
六、当前主战场: difftest 接入（进行中, 接手重点）
════════════════════════════════════
目标: 每条指令退休时与 NEMU 比对 {gpr[32], pc}, 立刻定位错误指令。
原理/契约(NEMU 的 src/cpu/difftest/dut.c 就是标准模板):
  DUT 侧                              ref(.so) 侧
  init_difftest(so, img_size, port) → dlopen + dlsym 5 个函数
                                    → ref_difftest_init(port)
                                    → ref_difftest_memcpy(地址, 镜像, 大小, TO_REF)
                                    → ref_difftest_regcpy(&状态, TO_REF)   ★ 含 pc → 复位值由此对齐
  每条指令退休:  difftest_step(pc, npc)
                                    → 若 skip: regcpy(TO_REF) 重新同步后 return
                                    → 否则 ref_difftest_exec(1); regcpy(TO_DUT); 比对

── NEMU 侧(己方已改好, 位置在 nemu/) ──
  ✓ include/memory/paddr.h : 加了 in_flash/in_sram/in_sdram/in_mem_region 与 FLASH/SRAM/SDRAM 宏
  ✓ src/memory/paddr.c     : 三块内存数组(flash 256M / sram 8K / sdram 128M, 尺寸取自
                             abstract-machine/scripts/linker_ysyxsoc.ld 的 MEMORY)、
                             guest_to_host/host_to_guest 区域化、paddr_read/write 用 in_mem_region
  ✓ src/cpu/difftest/ref.c : difftest_memcpy / difftest_regcpy 已实现
  ✓ 已用 TARGET_SHARE=y 编出 build/riscv32-nemu-interpreter-so, 5 个符号已导出
     (menuconfig: ISA=riscv32, MODE_SYSTEM, TARGET_SHARE=y; 设备菜单会自动消失
      —— 因为 src/device/Kconfig 有 `depends on !TARGET_SHARE`, 正好躲开 NEMU 设备落在
      0xa000_0000 与 sdram 冲突的问题)
  ✗ 待做 1: ref.c 的 difftest_exec 要加 `nemu_state.state = NEMU_RUNNING;` 再 cpu_exec(n)
            (否则 ref 一旦到 END/ABORT, 后续 exec 不前进, 会报一堆假 diff)
  ✗ 待做 2(可选): 想用 difftest 验异常, 两边同步给状态结构体加 mepc/mcause/mtvec/mstatus 字段

── NPC 侧 ──
  ✓ csrc/difftest.c 已从 NEMU 的 dut.c 抄了一份(骨架在)
  ✗ 但它【直接照抄, 还没适配本工程】, 至少要处理这几处:
     1) ref_difftest_regcpy(&cpu, TO_REF) → 必须改成 &cpu.base
        本工程的 CPU_state 是包装过的(见下), difftest 只允许传 base
     2) checkregs 里的 isa_difftest_checkregs 是 NEMU 的函数 → 要写成自己的比对:
          比 ref->gpr[i] 与 cpu.base.gpr[i](i<32) 以及 ref->pc 与【DUT 执行后的 pc】
     3) nemu_state → 本工程叫 npc_state(NPCState, NPC_ABORT/NPC_END/NPC_STOP)
     4) 打开 CONFIG_DIFFTEST(include/config.h 或 Makefile 里定义)
     5) init_difftest 里那句 memcpy 用的 RESET_VECTOR / guest_to_host 要用【本工程的】
        (npc/include/pmem.h 里有, npc 平台 RESET_VECTOR 就是 0x80000000)
  ✗ monitor.c:111 那行 `// init_difftest(diff_so_file, img_size, difftest_port);` 要取消注释
     (线索已就绪: diff_so_file 变量 + 命令行 'd' 选项都在)
  ✗ csrc/cpu-exec.c 的 trace_and_difftest() 里要插入调用, 建议:
        difftest_step(cpu.base.pc, itraceRetirePcNext);
     (那一行注释就是预留位置; 注意第一个参数是【刚退休那条】的 pc, 第二个是【下一条】的 pc)
  ✗ 设备访问要 skip: 建议在 LSU 里用现成的判据(内存之外即设备)算一个 devAccess 位,
     随 payload 传到退休点, 胶水里 `if (devAccess) difftest_skip_ref();`
     (否则 ysyxsoc 的 UART/CLINT 轮询会与 NEMU 行为不同, 必报 diff)

── 三个关键不变量(搞错就一定会 diff) ──
  1) 接口结构体只有 { word_t gpr[32]; paddr_t pc; } = 132 字节(NEMU 的 riscv32_CPU_state,
     CPU_state 就是它的 typedef)。本工程把它包在 CPU_state 里当第一个成员 base:
        typedef struct { word_t gpr[32]; paddr_t pc; } cpu_base_state_t;
        typedef struct cpu_state { cpu_base_state_t base; word_t instr; Decode decode; } CPU_state;
     → difftest 一律只碰 &cpu.base; 绝不能用 sizeof(CPU_state)(那是 280+ 字节, 会越界)
     → 建议加 _Static_assert(sizeof(cpu_base_state_t) == 132, ...)
     → gpr[16..31] 永远保持 0(NPC 的 REG_NUM=16, 与 NEMU 的 RV32E 行为一致)
  2) pc 的比对方式: 两边都比【执行后】的 pc ——
       ref 侧 regcpy 回来的 ref_r->pc 就是"执行后";
       DUT 侧要用 itraceRetirePcNext(= 刚退休那条的"下一条 pc", 已在 WBU 黑盒里接好:
       EXU 算顺序/跳转目标, LSU 覆盖 异常→mtvec、mret→mepc)。
     不要拿 cpu.base.pc 去比 pc(那是【本条】的 pc, 与 ref 天然差一条)
  3) 计时类 CSR(mcycle/minstret)与 mtime 不要纳入比对; 而 ecall 那拍的 mcause 两边
     约定不同(a5 vs 11), 要比 CSR 就得在那一步 skip

── 验证顺序建议 ──
  ① 先只比 {gpr, pc} 在 npc 平台上跑 cpu-tests(内存映射天然一致: npc 的
     CONFIG_MBASE=0x80000000 + 128M 正好等于 NEMU 的 MBASE+MSIZE)
  ② 再接 devAccess → skip_ref, 上 ysyxsoc 平台
  ③ 最后加 CSR 字段, 用它验异常(0/2/3/4/6 等)
  ⚠ 加 difftest 后仿真会慢很多(每条都跑一遍 NEMU), 只当调试开关用

════════════════════════════════════
七、血泪坑清单(SpinalHDL / 流程类, 能省几小时)
════════════════════════════════════
  1) Scala 的 val 是"先用后声明 = 拿到 null": 任何 `val x = 某组件.io` 必须写在那个组件
     实例化【之后】。编译能过, 运行时 NullPointerException(本工程已踩过 3 次:
     IFU 的 tryFetch/stopFetch、EXU、LSU 的 dcache)。
  2) LATCH DETECTED: 新加的 Bundle 字段若没人赋值(或只在仿真分支里赋值), elaborate 会报错。
  3) ASSIGNMENT OVERLAP: 同一信号【整体赋值 + 分片赋值】会冲突。要改某个字段, 就
     逐字段赋值(Decoder 里给 csrCtrl 各字段赋值、EXU 里拆开 csrCtrl、LSU 里拆开 rfCtrl,
     都是这个原因); 条件赋值放在 when 的互斥分支里则没问题。
  4) 位宽/字面量: UInt 赋值时脊髓HDL 会自动扩位, 但 `B(0,32 bits)` 赋给 UInt 会报类型错
     (要写 U(0,32 bits))。
  5) 回归验证: 改完 RTL 至少跑 `make perf`(周期不许退化) + `make sta`(面积/slack) +
     shuixianhua 双平台(make ARCH=riscv32e-npc / -ysyxsoc)。
  6) 备份/中间文件不要放 build/(AM 的 make 会清掉), 放在工作区里或直接提交。

════════════════════════════════════
八、当前未提交的改动(交接时必须知道)
════════════════════════════════════
npc/ 仓库 git status(在写这段时):
  M  Makefile, playground/Config.scala            ← 平台配置拆分(PARTFORM 传递)
  M  include/pmem.h                               ← npc 平台复位/内存改到 0x80000000
  M  csrc/{cpu-exec.c,trace/{itrace,mtrace,ftrace}.c} include/cpu-exec.h
                                                  ← CPU_state 包装成 base + 全部 cpu.pc→cpu.base.pc
  M  include/trace.h, playground/src/{DPI-C,EXU,LSU,WBU}.scala, playground/vsrc/dpi-c.v
                                                  ← 新增 itraceRetirePcNext(退休指令的下一条 pc)
  ?? csrc/difftest.c                              ← difftest 胶水(照抄 NEMU 的 dut.c, 待适配)
  (?? .metals/ 是编辑器产物, 与工程无关)
★ 这一批还没提交, 建议分 2~3 个提交整理掉(平台拆分 / npc 复位+内存 / cpu.base+pcNext+difftest)。

外层 ysyx-workbench/ 仓库里还有 AM 的改动(am/src/riscv/npc/* 与 ysyxsoc/* 的设备驱动增删改),
其中包括一次 ysyxsoc/keyboard.c → keybord.c 的改名, 接手时确认是不是笔误。

════════════════════════════════════
九、下一步优先级
════════════════════════════════════
  P0 把 difftest 跑通(第六节的 ✗ 逐条清掉): npc 平台 cpu-tests 能比对通过
  P1 接 devAccess → skip_ref, 上 ysyxsoc; 再用 CSR 比对验异常(0/2/3/4/6)
  P2 零面积优化(还有空间): IDU 提前解析分支(约 -1.5~2%)、请求早一拍发出(约 -0.5~1%)
  P3 想再吃 store 的那 ~7%: 需要 dcache 读写解耦 + 低位地址比较(约 250~400 um2),
     但面积只剩 299 → 必须先腾面积(例如 dcache 4 项减到 3 项, 代价约 +0.8% 周期)
  P4 整理提交 + 更新 PERF.md/README.md(每完成一项都要记)
```
