#include "common.h"
#include "simulation.h"
#include "cpu-exec.h"
#include "log.h"
#include "trace.h"

// perf: 性能计数器统计(读各模块的 PerfReg 黑盒, 程序结束时打印)
// 黑盒字段: perfCyc/dlySum0-3/dlyCnt0-3/evtCnt0-7 (64位)
// 平台路径前缀: npc 与 ysyxsoc 的 CPU 内模块路径不同
#ifdef __npc__
#include "VNPC_TOP___024root.h"
#define PERF_F(module, field)  top->rootp->NPC_TOP__DOT__cpu__DOT__##module##__DOT__perfReg_1__DOT__##field
#elif defined(__ysyxsoc__)
#include "VysyxSoCFull___024root.h"
#define PERF_F(module, field)  top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__##module##__DOT__perfReg_1__DOT__##field
#endif
static uint64_t idu_evt[8];
static uint64_t lsu_sum[4], lsu_cnt[4];

// 程序结束时打印性能统计
void perf_stat(void) {
  // 读各模块 PerfReg(两平台, PERF_F 宏自动选路径)
  idu_evt[0] = PERF_F(idu, evtCnt0);
  idu_evt[1] = PERF_F(idu, evtCnt1);
  idu_evt[2] = PERF_F(idu, evtCnt2);
  idu_evt[3] = PERF_F(idu, evtCnt3);
  idu_evt[4] = PERF_F(idu, evtCnt4);
  idu_evt[5] = PERF_F(idu, evtCnt5);
  idu_evt[6] = PERF_F(idu, evtCnt6);
  idu_evt[7] = PERF_F(idu, evtCnt7);
  uint64_t ifuDlySum  = PERF_F(ifu, dlySum0);
  uint64_t ifuDlyCnt  = PERF_F(ifu, dlyCnt0);
  uint64_t exuCyc     = PERF_F(exu, evtCnt0);
  uint64_t exuCalcCnt = PERF_F(exu, evtCnt1);
  uint64_t lsuPerfCyc = PERF_F(lsu, perfCyc);
  lsu_sum[0] = PERF_F(lsu, dlySum0);
  lsu_sum[1] = PERF_F(lsu, dlySum1);
  lsu_sum[2] = PERF_F(lsu, dlySum2);
  lsu_sum[3] = PERF_F(lsu, dlySum3);
  lsu_cnt[0] = PERF_F(lsu, dlyCnt0);
  lsu_cnt[1] = PERF_F(lsu, dlyCnt1);
  lsu_cnt[2] = PERF_F(lsu, dlyCnt2);
  lsu_cnt[3] = PERF_F(lsu, dlyCnt3);

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
}
