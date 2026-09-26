#ifndef __pmem_h__
#define __pmem_h__

#include "common.h"

// *********************************** mem *********************************** //
#define FLASH_SIZE   (paddr_t)128*1024*1024
#define PSRAM_SIZE   (paddr_t)4*1024*1024

#define CONFIG_MSIZE FLASH_SIZE

#define CONFIG_MBASE (paddr_t)0x80000000

#define PMEM_LEFT  ((paddr_t)CONFIG_MBASE)
#define PMEM_RIGHT ((paddr_t)CONFIG_MBASE + CONFIG_MSIZE - 1)
#define CONFIG_PC_RESET_OFFSET 0x0
#ifdef __ysyxsoc__
#define RESET_VECTOR 0x30000000
#else
#define RESET_VECTOR (PMEM_LEFT + CONFIG_PC_RESET_OFFSET)
#endif
uint8_t* guest_to_host(paddr_t paddr);
paddr_t host_to_guest(uint8_t *haddr);

static inline word_t host_read(void *addr) {
  return *(uint32_t *)addr;
}

static inline void host_write(void *addr, word_t data) {
  *(uint32_t *)addr = data; 
}

static inline bool in_pmem(paddr_t addr) {
  return (addr >= PMEM_LEFT) && (addr <= PMEM_RIGHT);
}

void pmem_init(void);

#endif

