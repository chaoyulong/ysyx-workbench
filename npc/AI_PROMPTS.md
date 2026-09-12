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
