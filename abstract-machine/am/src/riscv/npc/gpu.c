#include <am.h>
#include <klib.h>
#include "npc.h"

void __am_gpu_init() {
}

void __am_gpu_config(AM_GPU_CONFIG_T *cfg) {
  uint32_t wh = inl(VGACTL_ADDR);
  *cfg = (AM_GPU_CONFIG_T) {
    .present = true, .has_accel = false,
    .width = wh >> 16, .height = wh & 0xffff,
    .vmemsz = 0
  };
}

void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *ctl) {
  uint32_t x = ctl->x;
  uint32_t y = ctl->y;
  uint32_t w = ctl->w;
  uint32_t h = ctl->h;
  uint32_t *pixels = (uint32_t *)(uintptr_t)ctl->pixels;

  uint32_t w_len = (w < VGA_W - x) ? w : VGA_W - x;   // 裁剪到屏幕范围内
  uint32_t h_len = (h < VGA_H - y) ? h : VGA_H - y;

  for (int row = 0; row < h_len; row++) {
    volatile uint32_t* dst = &VGA_BUF32[((y + row) * VGA_W) + x];   // {v_addr, h_addr}
    memcpy((void *)dst, pixels + (row * w), w_len * sizeof(uint32_t));
    // memcpy((void *)&VGA_BUF32[(y + i) * VGA_W + x], pixels + i * w, wlen * sizeof(uint32_t));
  }

  if (ctl->sync) outl(SYNC_ADDR, 1);   // 请求刷新屏幕
}

void __am_gpu_status(AM_GPU_STATUS_T *status) {
  status->ready = true;
}
