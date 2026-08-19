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
