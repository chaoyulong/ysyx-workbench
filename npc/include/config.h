#ifndef __config_h__
#define __config_h__

// ================================ 功能开关 ================================ //
// 需要时取消注释对应的宏; 默认关闭保持仿真干净
// 注意: CONFIG_ISA_riscv 等由 Makefile 的 -D 定义, 不在此处

// trace 总开关: 配合 log_enable() 按指令数范围写日志(见 csrc/sdb/log.c)
#define CONFIG_TRACE 1
#define CONFIG_TRACE_START 0          // trace 起始指令数
#define CONFIG_TRACE_END   1000000    // trace 结束指令数

#ifdef CONFIG_TRACE
// 指令 trace: 每条指令打印到 log + ringbuf(程序结束时显示最后16条)
// #define CONFIG_ITRACE 1

// 函数 trace: 记录函数调用(需要 -f <elf> 参数, 见 monitor.c elf_get_func)
// #define CONFIG_FTRACE 1
#endif
// 监视点: 启用 sdb 的 w/d/info w 命令(见 watchpoint.c)
// #define CONFIG_WATCHPOINT 1

// 差分测试: 与参考实现对比(需要 -d <ref.so> / -p <port> 参数)
// #define CONFIG_DIFFTEST 1

// 日志写文件: 配合 init_log 的 -l <file> 参数
// #define CONFIG_LOG 1

#endif
