/***************************************************************************************
* Copyright (c) 2014-2024 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#ifndef __MEMORY_PADDR_H__
#define __MEMORY_PADDR_H__

#include <common.h>

#define PMEM_LEFT  ((paddr_t)CONFIG_MBASE)
#define PMEM_RIGHT ((paddr_t)CONFIG_MBASE + CONFIG_MSIZE - 1)
#define RESET_VECTOR (PMEM_LEFT + CONFIG_PC_RESET_OFFSET)

/* ---- DUT 的另外三块内存, 尺寸按 abstract-machine/scripts/linker_ysyxsoc.ld 的 MEMORY 规定 ---- */
/*   sram  0x0f000000 8K   |   flash 0x30000000 256M   |   sdram 0xa0000000 128M       */
/*   (psram 0x80000000 4M 落在 NEMU 的 MBASE(128M) 区间内, 已被 pmem 覆盖)                */
#define FLASH_BASE  0x30000000u
#define FLASH_SIZE  0x10000000u          /* 256M */
#define SRAM_BASE   0x0f000000u
#define SRAM_SIZE   0x00002000u          /* 8K   */
#define SDRAM_BASE  0xa0000000u
#define SDRAM_SIZE  0x08000000u          /* 128M */

#define FLASH_LEFT  ((paddr_t)FLASH_BASE)
#define FLASH_RIGHT ((paddr_t)FLASH_BASE + FLASH_SIZE - 1)
#define SRAM_LEFT   ((paddr_t)SRAM_BASE)
#define SRAM_RIGHT  ((paddr_t)SRAM_BASE + SRAM_SIZE - 1)
#define SDRAM_LEFT  ((paddr_t)SDRAM_BASE)
#define SDRAM_RIGHT ((paddr_t)SDRAM_BASE + SDRAM_SIZE - 1)

/* convert the guest physical address in the guest program to host virtual address in NEMU */
uint8_t* guest_to_host(paddr_t paddr);
/* convert the host virtual address in NEMU to guest physical address in the guest program */
paddr_t host_to_guest(uint8_t *haddr);

static inline bool in_pmem(paddr_t addr) {
  return addr - CONFIG_MBASE < CONFIG_MSIZE;
}

/* 和 in_pmem 一个套路: 无符号回绕, 一条减法判范围(地址小于 BASE 时会变成极大值, 自然为假) */
static inline bool in_flash(paddr_t addr) {
  return addr - FLASH_BASE < FLASH_SIZE;
}

static inline bool in_sram(paddr_t addr) {
  return addr - SRAM_BASE < SRAM_SIZE;
}

static inline bool in_sdram(paddr_t addr) {
  return addr - SDRAM_BASE < SDRAM_SIZE;
}

/* 是否落在任意一块被模拟的内存里(设备区不在其中: 设备访问会被 difftest skip) */
static inline bool in_mem_region(paddr_t addr) {
  return in_pmem(addr) || in_flash(addr) || in_sram(addr) || in_sdram(addr);
}

word_t paddr_read(paddr_t addr, int len);
void paddr_write(paddr_t addr, int len, word_t data);

#endif
