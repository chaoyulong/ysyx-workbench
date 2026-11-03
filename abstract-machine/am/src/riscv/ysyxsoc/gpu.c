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
    .vmemsz = 640 * 480 * sizeof(uint32_t)
  };
}

void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *ctl) {
  int x = ctl->x, y = ctl->y, h = ctl->h, w = ctl->w;
  // 参数合法时才可以搬运
  if (x >= 0 && y >= 0 && x < 640 && y < 480 && w > 0 && h > 0) {
    int pw = (w < 640 - x) ? w : 640 - x;     // 横向裁剪到屏幕内
    int ph = (h < 480 - y) ? h : 480 - y;     // 纵向裁剪到屏幕内
    size_t len = (size_t)pw * sizeof(uint32_t);
    int *pixels = (int *)(uintptr_t)ctl->pixels;

    for (int row = 0; row < ph; row++) {
      volatile uint32_t* dst = &VGA_BUF32[((y + row) << 10) + x];   // {v_addr, h_addr}
      memcpy((void *)dst, pixels + (row * w), len);
    }
  }
}

void __am_gpu_status(AM_GPU_STATUS_T *status) {
  status->ready = true;
}
