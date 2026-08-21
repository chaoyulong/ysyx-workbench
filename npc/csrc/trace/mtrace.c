#include "common.h"
#include "simulation.h"   // 定义 TOP_NAME 类型
#include "cpu-exec.h"
#include "device.h"       // 设备地址
#include "log.h"
#include "trace.h"

// mtrace: 访存踪迹(仅 npc 平台, 截取 lsuAxi4; ysyxsoc 是 Rocket Chip 无此黑盒)
// 只记录数据访存(不含取指), isDev 区分内存/设备, 设备按地址细分
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

// 设备地址细分(与 device.h 一致)
static const char *dev_name(uint32_t addr) {
  if (addr == SERIAL_PORT)                      return "serial";
  if (addr == KBD_ADDR)                         return "kbd";
  if (addr >= RTC_ADDR && addr < RTC_ADDR + 8)  return "rtc";
  if (addr >= VGA_BUF_BASE && addr < VGA_BUF_END) return "vga";
  if (addr == VGACTL_ADDR || addr == VGACTL_ADDR + 4) return "vgactl";
  return "dev";
}

// 每条指令后调用: 若黑盒计数器增长(有新的访存), 分类记录
// pc 取 cpu.pc(exec_once 后 = 退休指令 pc, 访存指令在 LSU->WBU 退休时即当前指令)
// 单步模式(g_print_step)时终端灰色缩进显示(与 itrace 区分), 否则只写日志文件
void mtrace_trace(void) {
  uint64_t cnt = mtraceCnt;
  bool step = itrace_print_step();
  while (cnt != mtrace_last_cnt) {     // 可能一次多条(防丢)
    mtrace_last_cnt++;
    const char *kind = mtraceIsDev ? dev_name(mtraceAddr) : "mem";
    if (mtraceIsDev) dev_access_cnt++;
    else             mem_access_cnt++;
#ifdef CONFIG_MTRACE_PC
    log_write("mtrace: [%-6s] %s pc=0x%08x addr=0x%08x data=0x%08x\n",
              kind, mtraceWen ? "store" : "load ", cpu.pc, mtraceAddr, mtraceWdata);
    if (step) printf(ANSI_FMT("  mtrace: [%-6s] %s pc=0x%08x addr=0x%08x data=0x%08x", ANSI_FG_BLACK) "\n",
              kind, mtraceWen ? "store" : "load ", cpu.pc, mtraceAddr, mtraceWdata);
#else
    log_write("mtrace: [%-6s] %s addr=0x%08x data=0x%08x\n",
              kind, mtraceWen ? "store" : "load ", mtraceAddr, mtraceWdata);
    if (step) printf(ANSI_FMT("  mtrace: [%-6s] %s addr=0x%08x data=0x%08x", ANSI_FG_BLACK) "\n",
              kind, mtraceWen ? "store" : "load ", mtraceAddr, mtraceWdata);
#endif
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
