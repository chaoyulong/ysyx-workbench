# 交接提示词：iverilog 四值仿真 / 网表仿真（B5 流片准备）

> 用法：下面每段都是**自包含**的，按需整段复制给 AI 助手。工作区：`ysyx-workbench`（`npc` 仓库）。
> 相关背景另见 `npc/AI_PROMPTS.md`（工程总览/代码地图）与 `HANDOVER-CI.md`（CI 合规线）。

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
  · sim-iverilog 内部用 SPINAL_SIM_DEBUG=0 PARTFORM=ysyxsoc 生成到 build/iverilog/（无 DPI 黑盒）
  · 网表默认用 build/sta/ysyx_23060082-500MHz/ysyx_23060082.netlist.v.sim
    （★ 必须是 .sim 那份：yosys.tcl 里 splitnets -ports 会拆端口，.netlist.v 是位级端口，TB 接不上）
- TB 在 npc/sim-iverilog/testbench/tb_npc.v；标准单元模型在 npc/sim-iverilog/testbench/cells.v
- .bin→$readmemh 的转换脚本 npc/sim-iverilog/tools/bin2mem.sh
- RTL 四值仿真：microbench 全量 PASS ✓（10/10 项 Passed + MicroBench PASS，1049614 周期，约 46 秒）
  RT-Thread 能启动到 msh shell ✓（之后停在 AM 侧 panic：npc/ioe.c 的 lut 没实现 rtt 要的设备）

【当前卡点】✗ 网表仿真能跑但很慢、且有 X
- 网表与 RTL 的 AXI 访问序列【前 5068 条完全一致】✓（只差开头 2 条 trampoline 取指）
- 但 50k 周期时网表只发出 3337 个 AR（RTL 是 4225），UART 98 字节 vs 124 字节 ⇒ 卡住
- 用 TB 的 +xcheck 定位到：cyc=42261 顶层 io_master_arvalid 先变 X → 42263 araddr 全 X
  → 42264 awv/wv 也 X（先读请求、再污染整个 AXI 接口）
- 同期 RTL 完全没有 X ⇒ 是"综合后才暴露"的问题（Verilog 的 if(X) 被当假分支吃掉了）

【下一步要做的】见提示词 3（先用 VCD 窗口找出"第一个变 X 的信号名"，再决定改不改 RTL）
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
- 结束判定：RTL 模式用层级引用看 LSU 的 ebreak 提交（dut.lsu.io_input_valid &&
  io_input_payload_csrCtrl_trapEnter && excCause==3）；网表模式扁平化后只能靠 watchdog
- 诊断开关（默认关）：+xcheck（端口 X 统计/首次 X/首次有效事务上的 X）、+traffic（每次 AR/AW）、
  +memcheck（存储器自检）、+img_size=（镜像大小，Makefile 会传）、+vcdwin（只 dump 一段窗口）、
  +quiet、+zero=（启动清零字节数，默认 16MB）、+max_cycles=

【已经在 TB 里踩过并修掉的坑，别再踩】
1. $readmemh 会覆盖数组其余部分 ⇒ 必须【先 $readmemh、再清零"镜像之外"的区间】，
   清零区间用 +img_size= 区分（否则 .bss/stack 读到 X）
2. X 检查必须用 negedge 采样：门级组合逻辑在 posedge 还在收敛，posedge 采样有假阳性
3. AXI 窄传输(sb/sh)：AWADDR 是字节地址、数据在 WSTRB 对应的字节道 ⇒
   字节 k 的落点 = (AWADDR & ~3) + k，不是 AWADDR + k（写错会把栈上的 char 写歪）
4. 含 X 的写数据要按 0 落内存（verilator 流程内存是二值的），否则无害的"存未初始化值"
   会被放大成大面积 X 传播
5. 网表里【无法】从 TB 引用内部信号：iverilog 绑不上带点的扁平名（dut.\xbar.xxx 会报
   Unable to bind）⇒ 想查内部只能靠 VCD
6. watchdog 默认 4,000,000 拍（实测 microbench 约 105 万拍）；网表很慢（≈9.5 分钟/百万周期），
   跑网表一定显式给 IV_MAXCYC=
```

---

## 提示词 3：当前卡点的排查方案（把这段整段给 AI）

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
     iverilog -g2012 -DIV_NETLIST -o build/iverilog/net-iv.vvp -s tb_npc \
       build/sta/ysyx_23060082-500MHz/ysyx_23060082.netlist.v.sim \
       sim-iverilog/testbench/cells.v sim-iverilog/testbench/tb_npc.v
     vvp build/iverilog/net-iv.vvp +img=build/iverilog/img.hex +img_size=30620 \
         +max_cycles=42400 +vcdwin +quiet
3. 解析 VCD：先用 $var 行建 code→name 表，再找【第一个含 x 的值变化行】⇒ 那个名字就是源头：
     awk '/^\$var/{nm[$4]=$5;next} /^#/{t=$1;next}
          /^[bB]?[01xXzZ]+[ \t]/{n=split($0,a,/[ \t]+/); v=a[1]; c=a[n];
            if (v ~ /[xXzZ]/ && (c in nm)) { print t, nm[c], substr(v,1,16); if(++k>=8) exit }}' tb_npc_win.vcd
4. 拿到名字后判断：
   - 若是 xbar 的 readState/writeState（或类似状态机寄存器）没复位 ⇒ 结论就是"状态机漏复位"
   - 若是 IFU/LSU 控制器的 ar.valid/aw.valid/w.valid ⇒ 结论是"AXI 握手 valid 漏复位"
   （文档把"各状态机、流水线 valid、AXI 握手 valid、icache 行有效位"列为【必须复位】）
★ 只有在查清并复现之后，才动 RTL 补 init；补的是状态机/valid，不要顺手给
  lineReg/rdataReg/wTempL 这类数据寄存器加复位（面积余量只剩 13.8 µm²）
```

---

## 提示词 4：修完之后的完整验收链（必须全过）

```
改 RTL 之后依次跑，全部要过：
1. 网表 +xcheck 跑到 ≥50 万拍：不再出现 X、且 AXI 事务数(ar/aw/rbeat/wbeat)与 RTL 一致
2. RTL 四值仿真全量：make -C npc sim-iverilog IMG=sim-iverilog/bin/microbench-riscv32e-npc.bin
   必须 10/10 项 Passed + MicroBench PASS
3. 功能基准：CCACHE_DISABLE=1 make -C npc perf
   ★ 必须仍然是 周期 11908252 / 指令 555445（逐位不变）——补复位不该改行为
4. 时序/面积：make -C npc sta
   ★ 面积上限 25000（nangate45），当前 24986.178 ⇒ 余量只有 13.8 µm²，务必盯住
   ★ 频率不是评分项，但要 >500MHz 通过（当前 1004.0 MHz / slack 1.004ns）
5. 别并行跑两个仿真/综合（build/ 会互相覆盖）；日志写到 npc/build/ 下

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
RTL 四值仿真 microbench 全量 PASS ✓；网表仿真能跑但 cyc=42261 起顶层 arvalid/araddr 变 X，
导致它比 RTL 慢几十倍。已排除 TB 侧原因（存储器/清零/采样/窄传输落点/X 数据落内存）。
现在【不要改 RTL】，先用 VCD 窗口（+vcdwin，只在 42000~42400 拍 dump）找出第一个变 X 的信号名，
再决定补哪个寄存器的复位。改完的验收：网表无 X + make perf 周期仍是 11908252/555445 +
make sta 面积不超 25000（当前余量仅 13.8 µm²）。
```
