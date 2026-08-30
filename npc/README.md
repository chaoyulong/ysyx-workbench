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
