#include "common.h"
#include "cpu-exec.h"
#include "log.h"
#include "trace.h"

// 单步打印开关: cpu_exec(n) 入口通过 itrace_set_print(n) 设置,
// n <= MAX_INST_TO_PRINT(显式单步少量指令) 时打印每条指令反汇编到屏幕
#define MAX_INST_TO_PRINT 10
static bool g_print_step = false;

// g_nr_guest_inst 定义于 cpu-exec.c: 已执行指令数(ringbuf 条数判断用)
extern uint64_t g_nr_guest_inst;

// 根据本次 cpu_exec 的步数 n 设置是否打印指令反汇编(仅单步时打印)
void itrace_set_print(uint64_t n) {
  g_print_step = (n <= MAX_INST_TO_PRINT);
}

// 是否单步打印模式(供 mtrace 等单步终端显示用)
bool itrace_print_step(void) {
  return g_print_step;
}

// 生成当前指令的 trace: 格式化 log_buf + 存入 ringbuf + 输出(log/屏幕)
// 由 trace_and_difftest 每条指令调用(cpu.pc/cpu.instr 已是退休指令)
void itrace_trace(void) {
#ifdef CONFIG_ITRACE
  char *p = cpu.decode.log_buf;
  p += snprintf(p, sizeof(cpu.decode.log_buf), "0x%08x:", cpu.pc);
  uint8_t *inst = (uint8_t *)&cpu.instr;
  for (int i = 3; i >= 0; i --) {
    p += snprintf(p, 4, " %02x", inst[i]);
  }
  memset(p, ' ', 4);
  p += 4;

  void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);
  disassemble(p, cpu.decode.log_buf + sizeof(cpu.decode.log_buf) - p, cpu.pc, (uint8_t *)&cpu.instr, 4);

  strcpy(cpu.decode.iringbuf[cpu.decode.iringbuf_end], cpu.decode.log_buf);
  cpu.decode.iringbuf_end++;
  if (cpu.decode.iringbuf_end >= 16) cpu.decode.iringbuf_end = 0;

  if (g_print_step) puts(cpu.decode.log_buf);
  log_write("%s\n", cpu.decode.log_buf);
#endif
}

// 程序结束时打印最后 16 条指令(从最旧到最新, 最新标 -->)
void iringbuf_printf(void) {
#ifdef CONFIG_ITRACE
  int total = g_nr_guest_inst < 16 ? (int)g_nr_guest_inst : 16;
  int start = g_nr_guest_inst < 16 ? 0 : cpu.decode.iringbuf_end;
  puts("-- ring buf:");
  for (int i = 0; i < total; i++) {
    int idx = (start + i) % 16;
    const char *mark = (i == total - 1) ? "-->" : "   ";
    printf("%s%s\n", mark, cpu.decode.iringbuf[idx]);
  }
#endif
}
