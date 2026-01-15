#include <klib.h>
#include <riscv/riscv.h>
#include "ysyxsoc.h"

void __am_gpu_init() {
  // uint32_t *col_addr_base;   
  // for(uint32_t i = 0; i  < 640; i++)
  // {
  //   col_addr_base = &fb[(i)*480]; 
  //   for(uint32_t j = 0; j < 480; j++)
  //   {
  //     *(col_addr_base + j) = 0xff0000;
  //     for (volatile int i = 0; i < 100; i++) ;
  //   }
  // }
}

void __am_gpu_config(AM_GPU_CONFIG_T *cfg) {
  *cfg = (AM_GPU_CONFIG_T) {
    .present = true, .has_accel = false,
    .width = 640, .height = 480,
    .vmemsz = 0
  };
}

void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *ctl) {
  uint32_t x = ctl->x;
  uint32_t y = ctl->y;
  uint32_t h = ctl->h;
  uint32_t w = ctl->w;

  uint32_t *pixels = (uint32_t *)(uintptr_t)ctl->pixels;
  uint32_t h_len = (h < 480 - y) ? h : 480 - y;       
  uint32_t w_len = (w < 640 - x) ? w : 640 - x;

  for(uint32_t i = 0; i < w_len; i++)
  {
    // col_addr_base = (fb + (x+i)*480 + y); 
    volatile uint32_t *col_addr_base = &VGA_BUF32[(x+i)*512 + y]; 
    for(uint32_t j = 0; j < h_len; j++)
    {
      *(col_addr_base + j) = *(pixels + j);
      // *(col_addr_base + j) = 0xff0000;
      // for (volatile int i = 0; i < 100; i++) ;
    }
    pixels += h;
  }
}

void __am_gpu_status(AM_GPU_STATUS_T *status) {
  status->ready = true;
}
