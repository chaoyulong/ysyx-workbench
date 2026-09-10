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
  uint64_t ifuAcc     = PERF_F(ifu, evtCnt0);   // 取指访问次数(命中率分母)
  uint64_t ifuMiss    = PERF_F(ifu, evtCnt2);   // icache 缺失次数(命中率分子)
  uint64_t ifuMissSum = PERF_F(ifu, dlySum1);   // 缺失总周期
  uint64_t ifuMissCnt = PERF_F(ifu, dlyCnt1);   // 缺失次数(与 ifuMiss 一致, 两份独立计数可互校)
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

  // 总周期/总指令已由 statistic() 打印, 此处只打细分统计; 所有行统一 cnt/total/avg 格式(等宽)
  const char *perf_sep = "  ========================================================================";  // 74 宽(2空格+72个=, 与 cnt/total 10位数据行对齐)
  Log_nohead("%s", perf_sep);
  Log_nohead("  %-17s cnt=%-10lu total=%-10lu avg=%7.2f cyc/access", "IFU fetch:",
         (unsigned long)ifuDlyCnt, (unsigned long)ifuDlySum,
         ifuDlyCnt ? (double)ifuDlySum / ifuDlyCnt : 0);
  // icache 命中率 + 缺失代价(缺失代价就是 TMT 里"每次缺失要等多少周期"那一项)
  Log_nohead("  %-17s cnt=%-10lu miss=%-10lu hit=%7.2f%%", "Icache access:",
         (unsigned long)ifuAcc, (unsigned long)ifuMiss,
         ifuAcc ? 100.0 * (ifuAcc - ifuMiss) / ifuAcc : 0);
  Log_nohead("  %-17s cnt=%-10lu total=%-10lu avg=%7.2f cyc/miss", "Icache miss:",
         (unsigned long)ifuMissCnt, (unsigned long)ifuMissSum,
         ifuMissCnt ? (double)ifuMissSum / ifuMissCnt : 0);
  const char *lsu_name[4] = {"LSU mem rd:", "LSU mem wr:", "LSU dev rd:", "LSU dev wr:"};
  for (int i = 0; i < 4; i++)
    Log_nohead("  %-17s cnt=%-10lu total=%-10lu avg=%7.2f cyc/access", lsu_name[i],
           (unsigned long)lsu_cnt[i], (unsigned long)lsu_sum[i],
           lsu_cnt[i] ? (double)lsu_sum[i] / lsu_cnt[i] : 0);
  Log_nohead("  %-17s cnt=%-10lu total=%-10lu avg=%7.2f cyc/inst", "EXU calc:",
         (unsigned long)exuCyc, (unsigned long)exuCalcCnt,
         exuCalcCnt ? (double)exuCyc / exuCalcCnt : 0);
  const char *cls_name[7] = {"calc", "mem", "branch", "jump", "csr", "sys", "other"};
  Log_nohead("  %-17s", "inst category:");
  for (int i = 0; i < 7; i++)
    Log_nohead("    %-7s %6lu  %5.1f%%", cls_name[i],
           (unsigned long)idu_evt[i],
           idu_evt[7] ? 100.0 * idu_evt[i] / idu_evt[7] : 0);
  Log_nohead("%s", perf_sep);
}
