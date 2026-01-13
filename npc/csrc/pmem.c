#include "pmem.h"
#include "device.h"

uint8_t pmem[CONFIG_MSIZE];

static void out_of_bound(int addr, int rw) 
{
  const char *a[2]={"read", "write"};
  printf("%s address = 0x%08x is out of bound of pmem [0x%08x, 0x%08x]\n", 
          a[rw], addr, PMEM_LEFT, PMEM_RIGHT);
  assert(0);
}

uint8_t* guest_to_host(paddr_t paddr) { return pmem + paddr - CONFIG_MBASE; }
paddr_t host_to_guest(uint8_t *haddr) { return haddr - pmem + CONFIG_MBASE; }

extern "C" uint32_t pmem_read(uint32_t raddr) {
  // 总是读取地址为`raddr & ~0x3u`的4字节返回
  if(in_pmem(raddr)){
    paddr_t real_addr = ((paddr_t)raddr & (paddr_t)(~0x3u));
    return host_read(guest_to_host(real_addr));
  }
  switch(raddr){
    case RTC_ADDR: 
    case RTC_ADDR + 4: return rtc_io_handler(raddr - RTC_ADDR); 
    // case KBD_ADDR: return keyboard_data_io_handler();
    default: out_of_bound(raddr, 0); return 0;
  }
}  

// 总是往地址为`waddr & ~0x3u`的4字节按写掩码`wmask`写入`wdata`
// `wmask`中每比特表示`wdata`中1个字节的掩码,
// 如`wmask = 0x3`代表只写入最低2个字节, 内存中的其它字节保持不变
extern "C" void pmem_write(uint32_t waddr, uint32_t wdata, uint8_t wmask) {
  if(in_pmem(waddr)){
    
    paddr_t real_addr = ((paddr_t)waddr & (paddr_t)(~0x3u));  // 地址对齐
    word_t wmask32 = 0;
    for (int i = 0; i < 4; i++) {
        if (wmask & (1 << i)) wmask32 |= (0xFF << (8 * i));
    }
    word_t old_data = host_read(guest_to_host(real_addr)); 
    word_t real_wdata = (old_data & ~wmask32) | (wdata & wmask32);
    host_write(guest_to_host(real_addr), real_wdata);
    return;
  }
  
  switch(waddr){
    case SERIAL_PORT: putc((uint8_t)wdata, stderr); break;
    default: out_of_bound(waddr, 1); break;
  }

}

extern "C" void flash_read(int32_t addr, int32_t *data) { printf("flash read\n"); assert(0); }
extern "C" void mrom_read(int32_t addr, int32_t *data) { assert(0); }

void pmem_init(){
  *(word_t *)(pmem + sizeof(word_t) * 0) = 0x00000297;  // auipc t0,0
  *(word_t *)(pmem + sizeof(word_t) * 1) = 0x00028823;  // sb  zero,16(t0)
  *(word_t *)(pmem + sizeof(word_t) * 2) = 0x0102c503;  // lbu a0,16(t0)
  *(word_t *)(pmem + sizeof(word_t) * 3) = 0x00100073;  // ebreak (used as nemu_trap)
  *(word_t *)(pmem + sizeof(word_t) * 4) = 0x00000297;  // some data
}