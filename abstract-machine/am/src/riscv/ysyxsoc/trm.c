#include <am.h>
#include <klib-macros.h>
#include <riscv/riscv.h>
#include <klib.h>
#include "ysyxsoc.h"

int main(const char *args);

extern char _heap_start;
extern char _heap_end;
Area heap = RANGE(&_heap_start, &_heap_end);

static const char mainargs[MAINARGS_MAX_LEN] = TOSTRING(MAINARGS_PLACEHOLDER); // defined in CFLAGS


// 二级bootlader的首尾
extern char _ssbl_start;
extern char _ssbl_end;
extern char _ssbl_load_start;
// 各个段的首尾
extern char _text_start;
extern char _text_end;
extern char _rodata_start;
extern char _rodata_end;
extern char _data_start;
extern char _data_end;
extern char _bss_start;
extern char _bss_end;
extern char _text_load_start;
extern char _rodata_load_start;
extern char _data_load_start;

// 一级bootloader，将二级bootloader装载进sram
void _first_stage_bootloader (void) __attribute__ ((section ("fsbl")));
void _first_stage_bootloader() 
{
  uintptr_t i, n;

  n = (uintptr_t)(&_ssbl_end - &_ssbl_start);
  for(i = 0; i < n; i += 4){
    *(uint32_t *)((uintptr_t)&_ssbl_start + i) = *(uint32_t *)((uintptr_t)&_ssbl_load_start + i);
  }
}

// 二级bootloader， 装载程序
void _second_stage_bootloader (void) __attribute__ ((section ("ssbl")));
void _second_stage_bootloader() 
{
  uintptr_t i, n;
  
  n = (uintptr_t)(&_text_end - &_text_start);
  for(i = 0; i < n; i+=4){
    *(uint32_t *)((uintptr_t)&_text_start + i) = *(uint32_t *)((uintptr_t)&_text_load_start + i);
  }
  
  n = (uintptr_t)(&_rodata_end - &_rodata_start);
  for(i = 0; i < n; i+=4){
    *(uint32_t *)((uintptr_t)&_rodata_start + i) = *(uint32_t *)((uintptr_t)&_rodata_load_start + i);
  }

  n = (uintptr_t)(&_data_end - &_data_start);
  for(i = 0; i < n; i+=4){
    *(uint32_t *)((uintptr_t)&_data_start + i) = *(uint32_t *)((uintptr_t)&_data_load_start + i);
  }

  n = (uintptr_t)(&_bss_end - &_bss_start);
  for(i = 0; i < n; i+=4){
    *(uint32_t *)((uintptr_t)&_bss_start + i) = 0;
  }


// #define __INSERT_EXTRA__

#ifdef __INSERT_EXTRA__
  extern char _data_extra_start;
  extern char _data_extra_end;
  extern char _data_extra_load_start;
  extern char _bss_extra_start;
  extern char _bss_extra_end;

  n = (uintptr_t)(&_data_extra_end - &_data_extra_start);
  for(i = 0; i < n; i++){
    *(char *)((uintptr_t)&_data_extra_start + i) = *(char *)((uintptr_t)&_data_extra_load_start + i);
  }
  n = (uintptr_t)(&_bss_extra_end - &_bss_extra_start);
  for(i = 0; i < n; i++){
    *(char *)((uintptr_t)&_bss_extra_start + i) = 0;
  }
#endif
}

uint32_t flash_read(uint32_t raddr)
{
  uint8_t real[4];
  SPI->DIVIDER  = 0;
  SPI->Tx[0]    = 0;  
  SPI->Tx[1]    = 0x03000000 | (raddr & 0x00ffffff); 
  SPI->SS       = (1 << 0);  
  SPI->CTRL   = 0x140;    // bit9 :Rx_NEG,可能会用到
  while(SPI->CTRL & 0x100);
  SPI->SS = 0;
  real[0] = SPI->Rx[3];
  real[1] = SPI->Rx[2];
  real[2] = SPI->Rx[1];
  real[3] = SPI->Rx[0];
  return *(uint32_t *)real;
}

uint32_t get_ysyxid(){
  uint32_t num; 
  asm volatile("csrr t0, marchid");
  asm volatile ("mv %0, t0" : "=r" (num));
  return num;
}

void ysyxsoc_dis_id(){
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
  extern void __am_uart_tx(AM_UART_TX_T *tx);
  __am_uart_tx((AM_UART_TX_T *)(&ch));
}

void halt(int code) {
  ysyxsoc_trap(code);
  // should not reach here
  while (1);  
}

void _trm_init() {
  extern void __am_uart_init();
  __am_uart_init();
  ysyxsoc_dis_id();
  int ret = main(mainargs);
  halt(ret);
}
