# NPC RISC-V32E CPU

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

### cachesim（icache 功能模拟器，`cachesim/`）
- **完全独立的 C 程序**（只依赖 libc，不参与 NPC 构建流程）：回放取指 PC 序列，只维护元数据（valid/tag/替换信息）统计缺失次数——不模拟数据、不执行指令，因此比 RTL 仿真相差几千倍。
- 支持直接映射 / 组相联（`--ways`）、`lru|fifo|rand` 替换、`fence.i` 清空、`--from` 排除 boot 阶段、`--misscost` 直接算 TMT。
- **输出单行 `RESULT:`**，便于脚本并行扫描参数组合（`npc/cachesim/README.md`）。
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
