#include "common.h"
#include "simulation.h"   // 定义 TOP_NAME 类型
#include "cpu-exec.h"
#include "log.h"
#include "trace.h"

// mtrace: 访存踪迹(仅 npc 平台, LSU 的 MtraceReg 黑盒; ysyxsoc 是 Rocket Chip 无此黑盒)
// 截取 lsuAxi4: 只记录数据访存(不含取指), isDev 区分内存/设备访问
#ifdef __npc__
#include "VNPC_TOP___024root.h"
#define mtraceCnt     top->rootp->NPC_TOP__DOT__cpu__DOT__mtraceReg_1__DOT__mtraceCnt
#define mtraceWen     top->rootp->NPC_TOP__DOT__cpu__DOT__mtraceReg_1__DOT__mtraceWen
#define mtraceIsDev   top->rootp->NPC_TOP__DOT__cpu__DOT__mtraceReg_1__DOT__mtraceIsDev
#define mtraceAddr    top->rootp->NPC_TOP__DOT__cpu__DOT__mtraceReg_1__DOT__mtraceAddr
#define mtraceWdata   top->rootp->NPC_TOP__DOT__cpu__DOT__mtraceReg_1__DOT__mtraceWdata

static uint64_t mtrace_last_cnt = 0;
static uint64_t mem_access_cnt = 0;    // 内存访存计数
static uint64_t dev_access_cnt = 0;    // 设备访问计数

// 每条指令后调用: 若黑盒计数器增长(有新的访存), 按内存/设备分类记录
void mtrace_trace(void) {
  uint64_t cnt = mtraceCnt;
  while (cnt != mtrace_last_cnt) {     // 可能一次多条(防丢)
    mtrace_last_cnt++;
    if (mtraceIsDev) { dev_access_cnt++; }
    else             { mem_access_cnt++; }
    log_write("mtrace: [%s] %s addr=0x%08x data=0x%08x\n",
              mtraceIsDev ? "dev " : "mem ",
              mtraceWen ? "store" : "load ", mtraceAddr, mtraceWdata);
  }
}

// 程序结束时打印内存/设备访问统计
void mtrace_stat(void) {
  log_write("mtrace: mem accesses = %lu, dev accesses = %lu\n",
            (unsigned long)mem_access_cnt, (unsigned long)dev_access_cnt);
}
#else
void mtrace_trace(void) {}
void mtrace_stat(void) {}
#endif
