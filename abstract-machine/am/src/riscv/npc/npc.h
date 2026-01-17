#ifndef __npc_h__
#define __npc_h__

#include <klib-macros.h>
#include <stdint.h>
#include <riscv/riscv.h>

#define npc_trap(code) asm volatile("mv a0, %0; ebreak" : :"r"(code))

#define SERIAL_PORT     0x10000000
#define KBD_ADDR        0x10011000
#define RTC_ADDR        0x02000000

#endif
