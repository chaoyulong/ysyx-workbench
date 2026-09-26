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

#include <memory/host.h>
#include <memory/paddr.h>
#include <device/mmio.h>
#include <isa.h>

#if   defined(CONFIG_PMEM_MALLOC)
static uint8_t *pmem = NULL;
#else // CONFIG_PMEM_GARRAY
static uint8_t pmem[CONFIG_MSIZE] PG_ALIGN = {};
#endif

// DUT 的另外三块内存: 尺寸一律取自 abstract-machine/scripts/linker_ysyxsoc.ld 的 MEMORY
// (psram 0x8000_0000 4M 落在 MBASE 区间内, 由 pmem 覆盖, 不需要单独建)
static uint8_t flash_mem[FLASH_SIZE] PG_ALIGN = {};   // 0x3000_0000, 256M
static uint8_t sram_mem [SRAM_SIZE ] PG_ALIGN = {};   // 0x0f00_0000, 8K
static uint8_t sdram_mem[SDRAM_SIZE] PG_ALIGN = {};   // 0xa000_0000, 128M

#ifdef CONFIG_MTRACE
static void m_trace(paddr_t addr, int len, word_t data, char* wr)
{
  char trace_log_buf[64];
  char *p = trace_log_buf;
  p += sprintf(p, "%s    0x%x    %d    ", wr, addr, len);
  uint8_t *num = (uint8_t *)&data;
  for (int i = len - 1; i >= 0; i --) {
    p += snprintf(p, 4, " %02x", num[i]);
  }
  
#ifdef CONFIG_MTRACE_COND
  if (MTRACE_COND) { log_write("%s\n", trace_log_buf); }
#endif
  printf("%s\n", trace_log_buf);
}
#endif

uint8_t* guest_to_host(paddr_t paddr) { 
  if (in_pmem (paddr)) return pmem + paddr - CONFIG_MBASE;
  if (in_flash(paddr)) return flash_mem + paddr - FLASH_BASE;
  if (in_sram (paddr)) return sram_mem  + paddr - SRAM_BASE;
  if (in_sdram(paddr)) return sdram_mem + paddr - SDRAM_BASE;
  return NULL;
}
paddr_t host_to_guest(uint8_t *haddr) {
  uintptr_t h = (uintptr_t)haddr;
  if (h >= (uintptr_t)pmem      && h < (uintptr_t)pmem      + CONFIG_MSIZE) return (paddr_t)(h - (uintptr_t)pmem     ) + CONFIG_MBASE;
  if (h >= (uintptr_t)flash_mem && h < (uintptr_t)flash_mem + FLASH_SIZE ) return (paddr_t)(h - (uintptr_t)flash_mem) + FLASH_BASE;
  if (h >= (uintptr_t)sram_mem  && h < (uintptr_t)sram_mem  + SRAM_SIZE  ) return (paddr_t)(h - (uintptr_t)sram_mem ) + SRAM_BASE;
  if (h >= (uintptr_t)sdram_mem && h < (uintptr_t)sdram_mem + SDRAM_SIZE ) return (paddr_t)(h - (uintptr_t)sdram_mem) + SDRAM_BASE;
  return 0;
}

static word_t pmem_read(paddr_t addr, int len) {
  word_t ret = host_read(guest_to_host(addr), len);
  return ret;
}

static void pmem_write(paddr_t addr, int len, word_t data) {
  host_write(guest_to_host(addr), len, data);
}

static void out_of_bound(paddr_t addr) {
  panic("address = " FMT_PADDR " is out of bound of pmem [" FMT_PADDR ", " FMT_PADDR "] at pc = " FMT_WORD,
      addr, PMEM_LEFT, PMEM_RIGHT, cpu.pc);
}

void init_mem() {
#if   defined(CONFIG_PMEM_MALLOC)
  pmem = malloc(CONFIG_MSIZE);
  assert(pmem);
#endif
  IFDEF(CONFIG_MEM_RANDOM, memset(pmem, rand(), CONFIG_MSIZE));
  // 三块新区域保持 0 初值(与 DUT 的内存模型一致); 启动时由 difftest_memcpy 把镜像灌进来
  Log("physical memory area [" FMT_PADDR ", " FMT_PADDR "]", PMEM_LEFT, PMEM_RIGHT);
  Log("flash  [" FMT_PADDR ", " FMT_PADDR "]", FLASH_LEFT, FLASH_RIGHT);
  Log("sram   [" FMT_PADDR ", " FMT_PADDR "]", SRAM_LEFT,  SRAM_RIGHT);
  Log("sdram  [" FMT_PADDR ", " FMT_PADDR "]", SDRAM_LEFT, SDRAM_RIGHT);
}

word_t paddr_read(paddr_t addr, int len) {
  if (likely(in_mem_region(addr))) {          // ★ 含 pmem/flash/sram/sdram
    word_t mdata = pmem_read(addr, len);
    
    IFDEF(CONFIG_MTRACE, m_trace(addr, len, mdata, "rd"));
    return mdata;
  }
  // IFDEF(CONFIG_DEVICE, word_t mdata = mmio_read(addr, len); IFDEF(CONFIG_MTRACE, m_trace(addr, len, mdata, "rd")); return mdata);
  IFDEF(CONFIG_DEVICE, return mmio_read(addr, len););
  IFDEF(CONFIG_MTRACE, m_trace(addr, len, -1, "rd"));
  out_of_bound(addr);
  return 0;
}

void paddr_write(paddr_t addr, int len, word_t data) {
  if (likely(in_mem_region(addr))) {          // ★ 含 pmem/flash/sram/sdram

    pmem_write(addr, len, data); 
    // if(addr == 0x820AC0B0)
    //   printf("at pc = %x, write mem at 0x820AC0B0, wdata = %x\n",cpu.pc, data);
    IFDEF(CONFIG_MTRACE, m_trace(addr, len, data, "wr"));
    return; 
  }
  // IFDEF(CONFIG_DEVICE, mmio_write(addr, len, data); IFDEF(CONFIG_MTRACE, m_trace(addr, len, data, "wr")); return);
  IFDEF(CONFIG_DEVICE, mmio_write(addr, len, data); return);
  IFDEF(CONFIG_MTRACE, m_trace(addr, len, data, "wr"));
  out_of_bound(addr);
}
