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
