#include <am.h>
#include <klib-macros.h>
#include <riscv/riscv.h>
#include <klib.h>
#include "npc.h"

extern char _heap_start;
int main(const char *args);

extern char _pmem_start;
#define PMEM_SIZE (128 * 1024 * 1024)
#define PMEM_END  ((uintptr_t)&_pmem_start + PMEM_SIZE)

Area heap = RANGE(&_heap_start, PMEM_END);
static const char mainargs[MAINARGS_MAX_LEN] = TOSTRING(MAINARGS_PLACEHOLDER); // defined in CFLAGS

uint32_t get_ysyxid()
{
  uint32_t num; 
  asm volatile("csrr t0, marchid");
  asm volatile ("mv %0, t0" : "=r" (num));
  return num;
}

void ysyxsoc_dis_id()
{
  char temp[4];

  uint32_t id0, id1; 
  asm volatile("csrr t0, mvendorid");
  asm volatile ("mv %0, t0" : "=r" (id0));
  asm volatile("csrr t0, marchid");
  asm volatile ("mv %0, t0" : "=r" (id1));

  // 打印 "ID = ysyx_<id1>\n" (直接用 putch, 不依赖 printf/klib)
  *(uint32_t *)temp = id0;
  const char *prefix = "ID = ";
  for (int i = 0; i < 5; i++) putch(prefix[i]);
  putch(temp[3]);
  putch(temp[2]);
  putch(temp[1]);
  putch(temp[0]);
  putch('_');

  char buf[16];
  int len = 0;
  uint32_t n = id1;
  if (n == 0) buf[len++] = '0';
  while (n > 0) {
    buf[len++] = '0' + (n % 10);
    n /= 10;
  }
  while (len > 0) putch(buf[--len]);
  putch('\n');
}

// 64 位计数器标准读取: 读高 -> 读低 -> 再读高, 两次高相同才采信
// (硬件 mcycleh/minstreth 是直读当前值; 两条 csrr 在两个时钟沿采样,
//  "低->高"背靠背会留 2^-32 的窗口: 回绕正好落在两次读之间时组合值偏大 2^32,
//  用三重读即可彻底消掉, 且不需要任何硬件快照)
uint64_t read_mcycle64(void) {
  uint32_t hi, lo, hi2;
  do {
    asm volatile("csrr %0, mcycleh\n\t"
                 "csrr %1, mcycle\n\t"
                 "csrr %2, mcycleh"
                 : "=r"(hi), "=r"(lo), "=r"(hi2));
  } while (hi != hi2);
  return ((uint64_t)hi << 32) | lo;
}

uint64_t read_minstret64(void) {
  uint32_t hi, lo, hi2;
  do {
    asm volatile("csrr %0, minstreth\n\t"
                 "csrr %1, minstret\n\t"
                 "csrr %2, minstreth"
                 : "=r"(hi), "=r"(lo), "=r"(hi2));
  } while (hi != hi2);
  return ((uint64_t)hi << 32) | lo;
}

void putch(char ch) {
  outb(SERIAL_PORT, ch);
}

void halt(int code) {
  npc_trap(code);
  
  // should not reach here
  while (1);  
}

// 默认中断函数(与 riscv/ysyxsoc/trm.c 保持一致)
// ★ 最后那行 `prev->mepc = 0` 是【必须】的, 不是可选优化:
//   cte_init 会把 mtvec 从复位值 0 改成 __am_asm_trap, 而 iverilog testbench 的收尾判据
//   "E1: 从地址 0 取指"(tb_npc.v 里 araddr == 0)依赖 trap 跳到 0。
//   若不加这一行, halt() 的 ebreak 会被 __am_irq_handle 做 mepc += 4 后返回 ebreak+4,
//   也就是 halt() 里的 while(1) ⇒ 永远取不到地址 0 ⇒ sim-iverilog-netlist(网表 flatten 后
//   lsu_commit_trap 恒 0, 只能靠 E1)收不了尾, 只能等看门狗超时。
static Context *user_handler_default(Event ev, Context *prev) {
  (void)ev;
  static const char msg[] = "in user_handler_default, mcause is: ";
  for (unsigned i = 0; i < sizeof(msg) - 1; i++) {
    putch(msg[i]);
  }
  if(prev->mcause == -1) {
    putch('-'); putch('1');
  } else {
    putch('0' + prev->mcause / 10);
    putch('0' + prev->mcause % 10);
  }
  putch('\n');

  if (prev->mcause == 3) prev->mepc = 0;
  return prev;
}

void _trm_init() {
  ysyxsoc_dis_id();
  cte_init(user_handler_default);
  int ret = main(mainargs);
  halt(ret);
}
