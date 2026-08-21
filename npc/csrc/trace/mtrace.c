#include "common.h"
#include "cpu-exec.h"
#include "log.h"
#include "trace.h"

// mtrace: 访存踪迹(仅 npc 平台, LSU 的 MtraceReg 黑盒; ysyxsoc 是 Rocket Chip 无此黑盒)
#ifdef __npc__
extern TOP_NAME* top;
#include "VNPC_TOP___024root.h"
#define mtraceCnt     top->rootp->NPC_TOP__DOT__cpu__DOT__lsu__DOT__mtraceReg_1__DOT__mtraceCnt
#define mtraceWen     top->rootp->NPC_TOP__DOT__cpu__DOT__lsu__DOT__mtraceReg_1__DOT__mtraceWen
#define mtracePc      top->rootp->NPC_TOP__DOT__cpu__DOT__lsu__DOT__mtraceReg_1__DOT__mtracePc
#define mtraceAddr    top->rootp->NPC_TOP__DOT__cpu__DOT__lsu__DOT__mtraceReg_1__DOT__mtraceAddr
#define mtraceWdata   top->rootp->NPC_TOP__DOT__cpu__DOT__lsu__DOT__mtraceReg_1__DOT__mtraceWdata

static uint64_t mtrace_last_cnt = 0;

// 每条指令后调用: 若黑盒计数器增长(有新的访存), 记录到日志
void mtrace_trace(void) {
  uint64_t cnt = mtraceCnt;
  if (cnt != mtrace_last_cnt) {
    mtrace_last_cnt = cnt;
    log_write("mtrace: %s pc=0x%08x addr=0x%08x data=0x%08x\n",
              mtraceWen ? "store" : "load ", mtracePc, mtraceAddr, mtraceWdata);
  }
}
#else
void mtrace_trace(void) {}
#endif
