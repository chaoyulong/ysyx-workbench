#ifndef __npc_h__
#define __npc_h__

#include <klib-macros.h>
#include <stdint.h>
#include <riscv/riscv.h>

#define npc_trap(code) asm volatile("mv a0, %0; ebreak" : :"r"(code))

#define SERIAL_PORT     0x10000000
#define KBD_ADDR        0x10011000
#define RTC_ADDR        0x02000000

#define VGA_W           640
#define VGA_H           480
#define VGA_BUF_BASE    0x21000000
#define VGA_BUF32       ((volatile uint32_t *)VGA_BUF_BASE)
#define VGACTL_ADDR     0x21200000     // [0]=wh(高16位宽低16位高), [4]=sync
#define SYNC_ADDR       (VGACTL_ADDR + 4)

#endif
