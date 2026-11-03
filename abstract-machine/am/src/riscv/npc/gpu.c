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
    .vmemsz = VGA_W * VGA_H * sizeof(uint32_t)
  };
}

void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *ctl) {
  int x = ctl->x, y = ctl->y, h = ctl->h, w = ctl->w;

  // 参数合法时才可以搬运
  if (VGA_W > 0 && VGA_H > 0 && x >= 0 && y >= 0 && x < VGA_W && y < VGA_H && w > 0 && h > 0) {
    int pw = (w < VGA_W - x) ? w : VGA_W - x;     // 横向裁剪到屏幕内
    int ph = (h < VGA_H - y) ? h : VGA_H - y;     // 纵向裁剪到屏幕内
    size_t len = (size_t)pw * sizeof(uint32_t);
    int *pixels = (int *)(uintptr_t)ctl->pixels;

    for (int row = 0; row < ph; row++) {
      volatile uint32_t* dst = &VGA_BUF32[((y + row) * VGA_W) + x];
      memcpy((void *)dst, pixels + (row * w), len);
    }
  }

  if (ctl->sync) outl(SYNC_ADDR, 1);   // 请求刷新屏幕
}

void __am_gpu_status(AM_GPU_STATUS_T *status) {
  status->ready = true;
}
