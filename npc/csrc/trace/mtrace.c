#include "common.h"
#include "simulation.h"   // 定义 TOP_NAME 类型
#include "cpu-exec.h"
#include "log.h"
#include "trace.h"

static uint64_t mtrace_last_cnt = 0;
static uint64_t mem_access_cnt = 0;    // 内存访存计数
static uint64_t dev_access_cnt = 0;    // 设备访问计数

// 统一地址映射分类(两平台布局一致): 返回内存类型或具体设备名
static const char *mtrace_class(uint32_t addr) {
  if (addr >= 0x0f000000 && addr < 0x10000000) return "sram";      // SRAM
  if (addr >= 0x02000000 && addr < 0x02010000) return "clint";     // CLINT(时钟)
  if (addr >= 0x10000000 && addr < 0x10001000) return "uart";      // UART16550/串口
  if (addr >= 0x10001000 && addr < 0x10002000) return "spi";       // SPI master
  if (addr >= 0x10002000 && addr < 0x10003000) return "gpio";      // GPIO
  if (addr >= 0x10011000 && addr < 0x10012000) return "ps2";       // PS2/键盘
  if (addr >= 0x20000000 && addr < 0x20001000) return "mrom";      // MROM
  if (addr >= 0x21000000 && addr < 0x21200000) return "vga";       // VGA 显存
  if (addr == 0x21200000 || addr == 0x21200004) return "vgactl";   // VGA 控制
  if (addr >= 0x30000000 && addr < 0x40000000) return "flash";     // Flash(内存)
  if (addr >= 0x40000000 && addr < 0x80000000) return "chiplink";  // ChipLink MMIO
  if (addr >= 0x80000000 && addr < 0xa0000000) return "psram";     // PSRAM(内存)
  if (addr >= 0xa0000000 && addr < 0xc0000000) return "sdram";     // SDRAM(内存)
  if (addr >= 0xc0000000)                      return "chiplink";  // ChipLink MEM
  return "dev";
}

// 内存类型(flash/psram/sdram/sram)计入内存访问
static bool mtrace_is_mem(const char *cls) {
  return cls[0] == 'f' || cls[0] == 'p' || cls[0] == 's';   // flash/psram/sdram/sram
}

// 每条指令后调用: 若黑盒计数器增长(有新的访存), 分类记录
// pc 取 cpu.base.pc(exec_once 后 = 退休指令 pc, 访存指令在 LSU->WBU 退休时即当前指令)
void mtrace_trace(void) {
  uint64_t cnt = mtraceCnt;
  bool step = itrace_print_step();
  while (cnt != mtrace_last_cnt) {     // 可能一次多条(防丢)
    mtrace_last_cnt++;
    const char *kind = mtrace_class(mtraceAddr);
    if (mtrace_is_mem(kind)) mem_access_cnt++;
    else                    dev_access_cnt++;
#ifdef CONFIG_MTRACE_PC
    log_write("mtrace: [%-6s] %s pc=0x%08x addr=0x%08x data=0x%08x\n",
              kind, mtraceWen ? "store" : "load ", cpu.base.pc, mtraceAddr, mtraceWdata);
    if (step) printf(ANSI_FMT("  mtrace: [%-6s] %s pc=0x%08x addr=0x%08x data=0x%08x", ANSI_FG_BLACK) "\n",
              kind, mtraceWen ? "store" : "load ", cpu.base.pc, mtraceAddr, mtraceWdata);
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
  Log_nohead("mtrace: mem accesses = %lu, dev accesses = %lu",
            (unsigned long)mem_access_cnt, (unsigned long)dev_access_cnt);
}
