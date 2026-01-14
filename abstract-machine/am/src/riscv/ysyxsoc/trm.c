#include <am.h>
#include <klib-macros.h>
#include <riscv/riscv.h>
#include <klib.h>
#include "ysyxsoc.h"

extern char _heap_start;
int main(const char *args);

extern char _pmem_start;
#define PMEM_SIZE (4 * 1024 * 1024)
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
  char real[5];

  uint32_t id0, id1; 
  asm volatile("csrr t0, mvendorid");
  asm volatile ("mv %0, t0" : "=r" (id0));
  asm volatile("csrr t0, marchid");
  asm volatile ("mv %0, t0" : "=r" (id1));

  *(uint32_t *)temp = id0;
  real[0] = temp[3];
  real[1] = temp[2];
  real[2] = temp[1];
  real[3] = temp[0];
  real[4] = '\0';

  printf("ID = %s_%d\n", real, id1);
}

void putch(char ch) {
  uart_tx(ch);
}

void halt(int code) {
  ysyxsoc_trap(code);
  
  // should not reach here
  while (1);  
}

void _trm_init() {
  uart_init();
  // ysyxsoc_dis_id();
  int ret = main(mainargs);
  halt(ret);
}
