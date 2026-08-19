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


