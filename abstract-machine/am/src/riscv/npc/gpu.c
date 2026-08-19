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
  int x = ctl->x, y = ctl->y, w = ctl->w, h = ctl->h;
  uint32_t *pixels = (uint32_t *)(uintptr_t)ctl->pixels;

  int wlen = (w < VGA_W - x) ? w : VGA_W - x;   // 裁剪到屏幕范围内
  int hlen = (h < VGA_H - y) ? h : VGA_H - y;

  for (int i = 0; i < hlen; i++) {
    memcpy((void *)&VGA_BUF32[(y + i) * VGA_W + x], pixels + i * w, wlen * sizeof(uint32_t));
  }

  if (ctl->sync) outl(SYNC_ADDR, 1);   // 请求刷新屏幕
}

void __am_gpu_status(AM_GPU_STATUS_T *status) {
  status->ready = true;
}
