#ifndef __config_h__
#define __config_h__

// ================================ 功能开关 ================================ //
// 需要时取消注释对应的宏; 默认关闭保持仿真干净
// 注意: CONFIG_ISA_riscv 等由 Makefile 的 -D 定义, 不在此处

// 日志写文件: 配合 log_init 的 -l <file> 参数
// #define CONFIG_LOG 1

// 差分测试: 与参考实现对比(需要 -d <ref.so> / -p <port> 参数)
//  #define CONFIG_DIFFTEST 1

// trace 总开关: 配合 log_enable() 按指令数范围写日志(见 csrc/sdb/log.c)
#define CONFIG_TRACE 1
#define CONFIG_TRACE_START 0          // trace 起始指令数
#define CONFIG_TRACE_END   0          // trace 结束指令数(0 = 不限制, 一直记到程序结束)

#ifdef CONFIG_TRACE
// 指令 trace: 每条指令打印到 log + ringbuf(程序结束时显示最后16条)
// 也是 cachesim 的 PC 序列来源(见 cachesim/README.md)
#define CONFIG_ITRACE 1

// 访存 trace: 记录每次数据访存(load/store)的 pc/地址/数据
// #define CONFIG_MTRACE 1
#ifdef CONFIG_MTRACE
// 在 mtrace 记录中打印访存指令的 pc(默认开, 关闭可减小日志量)
#define CONFIG_MTRACE_PC 1
#endif

// 函数 trace: 记录函数调用(需要 -f <elf> 参数, 见 monitor.c elf_get_func)
//  #define CONFIG_FTRACE 1

#ifdef CONFIG_FTRACE
// 过滤 libgcc 内部函数(__ 开头的符号, 如 __udivsi3/__umodsi3), 只显示用户/klib 函数
// #define CONFIG_FTRACE_FILTER_INTERNAL 1
#endif
#endif
// 监视点: 启用 sdb 的 w/d/info w 命令(见 watchpoint.c)
// #define CONFIG_WATCHPOINT 1

#endif
