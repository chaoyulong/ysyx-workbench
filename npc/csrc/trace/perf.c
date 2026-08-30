#include "common.h"
#include "simulation.h"
#include "cpu-exec.h"
#include "log.h"
#include "trace.h"

// perf: 性能计数器统计(读各模块的 PerfReg 黑盒, 程序结束时打印)
// 黑盒字段: perfCyc/dlySum0-3/dlyCnt0-3/evtCnt0-7 (64位)
#ifdef __npc__
#include "VNPC_TOP___024root.h"
#endif

// 程序结束时打印性能统计
void perf_stat(void) {
#ifdef __npc__
  // 读各模块 PerfReg
  idu_evt[0] = top->rootp->NPC_TOP__DOT__cpu__DOT__idu__DOT__perfReg_1__DOT__evtCnt0;
  idu_evt[1] = top->rootp->NPC_TOP__DOT__cpu__DOT__idu__DOT__perfReg_1__DOT__evtCnt1;
  idu_evt[2] = top->rootp->NPC_TOP__DOT__cpu__DOT__idu__DOT__perfReg_1__DOT__evtCnt2;
  idu_evt[3] = top->rootp->NPC_TOP__DOT__cpu__DOT__idu__DOT__perfReg_1__DOT__evtCnt3;
  idu_evt[4] = top->rootp->NPC_TOP__DOT__cpu__DOT__idu__DOT__perfReg_1__DOT__evtCnt4;
  idu_evt[5] = top->rootp->NPC_TOP__DOT__cpu__DOT__idu__DOT__perfReg_1__DOT__evtCnt5;
  idu_evt[6] = top->rootp->NPC_TOP__DOT__cpu__DOT__idu__DOT__perfReg_1__DOT__evtCnt6;
  idu_evt[7] = top->rootp->NPC_TOP__DOT__cpu__DOT__idu__DOT__perfReg_1__DOT__evtCnt7;
  uint64_t ifuDlySum  = top->rootp->NPC_TOP__DOT__cpu__DOT__ifu__DOT__perfReg_1__DOT__dlySum0;
  uint64_t ifuDlyCnt  = top->rootp->NPC_TOP__DOT__cpu__DOT__ifu__DOT__perfReg_1__DOT__dlyCnt0;
  uint64_t exuCyc     = top->rootp->NPC_TOP__DOT__cpu__DOT__exu__DOT__perfReg_1__DOT__evtCnt0;
  uint64_t exuCalcCnt = top->rootp->NPC_TOP__DOT__cpu__DOT__exu__DOT__perfReg_1__DOT__evtCnt1;
  uint64_t lsuPerfCyc = top->rootp->NPC_TOP__DOT__cpu__DOT__lsu__DOT__perfReg_1__DOT__perfCyc;
  lsu_sum[0] = top->rootp->NPC_TOP__DOT__cpu__DOT__lsu__DOT__perfReg_1__DOT__dlySum0;
  lsu_sum[1] = top->rootp->NPC_TOP__DOT__cpu__DOT__lsu__DOT__perfReg_1__DOT__dlySum1;
  lsu_sum[2] = top->rootp->NPC_TOP__DOT__cpu__DOT__lsu__DOT__perfReg_1__DOT__dlySum2;
  lsu_sum[3] = top->rootp->NPC_TOP__DOT__cpu__DOT__lsu__DOT__perfReg_1__DOT__dlySum3;
  lsu_cnt[0] = top->rootp->NPC_TOP__DOT__cpu__DOT__lsu__DOT__perfReg_1__DOT__dlyCnt0;
  lsu_cnt[1] = top->rootp->NPC_TOP__DOT__cpu__DOT__lsu__DOT__perfReg_1__DOT__dlyCnt1;
  lsu_cnt[2] = top->rootp->NPC_TOP__DOT__cpu__DOT__lsu__DOT__perfReg_1__DOT__dlyCnt2;
  lsu_cnt[3] = top->rootp->NPC_TOP__DOT__cpu__DOT__lsu__DOT__perfReg_1__DOT__dlyCnt3;

  uint64_t total = idu_evt[7] ? idu_evt[7] : 1;   // 指令总数
  printf("\n========== PERF ==========\n");
  printf("total cycles      = %lu\n", (unsigned long)lsuPerfCyc);
  printf("total inst        = %lu\n", (unsigned long)total);
  printf("IFU fetch:        cnt=%lu total=%lu cyc avg=%.2f cyc/access\n",
         (unsigned long)ifuDlyCnt, (unsigned long)ifuDlySum,
         ifuDlyCnt ? (double)ifuDlySum / ifuDlyCnt : 0);
  const char *lsu_name[4] = {"mem rd", "mem wr", "dev rd", "dev wr"};
  for (int i = 0; i < 4; i++)
    printf("LSU %-6s:       cnt=%lu total=%lu cyc avg=%.2f cyc/access\n", lsu_name[i],
           (unsigned long)lsu_cnt[i], (unsigned long)lsu_sum[i],
           lsu_cnt[i] ? (double)lsu_sum[i] / lsu_cnt[i] : 0);
  printf("EXU calc:         cycles=%lu calc_inst=%lu (单周期 avg=1.00)\n",
         (unsigned long)exuCyc, (unsigned long)exuCalcCnt);
  const char *cls_name[7] = {"calc", "mem", "branch", "jump", "csr", "sys", "other"};
  printf("inst category:\n");
  for (int i = 0; i < 7; i++)
    printf("  %-7s %6lu  %5.1f%%\n", cls_name[i],
           (unsigned long)idu_evt[i], 100.0 * idu_evt[i] / total);
  printf("==========================\n");
#endif
}
