#ifndef __trace_h__
#define __trace_h__

#include "common.h"

// ============================== ftrace ============================== //
// 函数符号(由 elf_get_func 从 ELF 解析得到, 供 ftrace 查询 pc 所属函数)
typedef struct {
  uint32_t addr;       // 函数入口地址
  uint32_t size;       // 函数大小
  char name[64];       // 函数名
  uint32_t call_count; // 被调用次数(运行时统计)
} elf_fun;

// itrace 黑盒总是存在(enableSimDebug=true), 宏总是可用
#ifdef __ysyxsoc__
#include "VysyxSoCFull___024root.h"

#define itraceRetireValid   top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__wbu__DOT__itraceReg_1__DOT__itraceRetireValid
#define itraceRetirePc      top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__wbu__DOT__itraceReg_1__DOT__itraceRetirePc
#define itraceRetireInstr   top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__wbu__DOT__itraceReg_1__DOT__itraceRetireInstr
#define ebreak              top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__wbu__DOT__itraceReg_1__DOT__ebreak

#define mtraceCnt           top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__lsu__DOT__mtraceReg_1__DOT__mtraceCnt
#define mtraceWen           top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__lsu__DOT__mtraceReg_1__DOT__mtraceWen
#define mtraceAddr          top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__lsu__DOT__mtraceReg_1__DOT__mtraceAddr
#define mtraceWdata         top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__lsu__DOT__mtraceReg_1__DOT__mtraceWdata
#else
#include "VNPC_TOP___024root.h"

#define itraceRetireValid   top->rootp->NPC_TOP__DOT__cpu__DOT__wbu__DOT__itraceReg_1__DOT__itraceRetireValid
#define itraceRetirePc      top->rootp->NPC_TOP__DOT__cpu__DOT__wbu__DOT__itraceReg_1__DOT__itraceRetirePc
#define itraceRetireInstr   top->rootp->NPC_TOP__DOT__cpu__DOT__wbu__DOT__itraceReg_1__DOT__itraceRetireInstr
#define ebreak              top->rootp->NPC_TOP__DOT__cpu__DOT__wbu__DOT__itraceReg_1__DOT__ebreak

#define mtraceCnt           top->rootp->NPC_TOP__DOT__cpu__DOT__lsu__DOT__mtraceReg_1__DOT__mtraceCnt
#define mtraceWen           top->rootp->NPC_TOP__DOT__cpu__DOT__lsu__DOT__mtraceReg_1__DOT__mtraceWen
#define mtraceAddr          top->rootp->NPC_TOP__DOT__cpu__DOT__lsu__DOT__mtraceReg_1__DOT__mtraceAddr
#define mtraceWdata         top->rootp->NPC_TOP__DOT__cpu__DOT__lsu__DOT__mtraceReg_1__DOT__mtraceWdata
#endif

#define FUN_BUF_MAX 1024
extern elf_fun fun_buf[FUN_BUF_MAX];
extern int fun_buf_count;

void elf_get_func(const char *filename);  // 解析 ELF, 收集函数符号
void func_trace(void);                    // 每条指令调用, 记录函数调用/返回
void print_func(void);                    // 程序结束打印调用统计

// ============================== mtrace ============================== //
void mtrace_trace(void);                  // 记录访存踪迹(由 trace_and_difftest 调用)
void mtrace_stat(void);                   // 程序结束时打印内存/设备访问统计
void perf_stat(void);                        // 程序结束时打印性能计数器统计

// ============================== itrace ============================== //
void itrace_set_print(uint64_t n);        // 设置单步打印开关(由 cpu_exec 入口调用)
bool itrace_print_step(void);                 // 是否单步打印模式(供 mtrace 终端显示用)
void itrace_trace(void);                  // 生成指令 trace(格式化/ringbuf/输出)
void iringbuf_printf(void);               // 程序结束打印最后16条指令

#endif
