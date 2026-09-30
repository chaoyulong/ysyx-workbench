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

void _trm_init() {
  ysyxsoc_dis_id();
  int ret = main(mainargs);
  halt(ret);
}
