#include <am.h>
#include <nemu.h>
#include <klib.h>

#define SYNC_ADDR (VGACTL_ADDR + 4)

static uint32_t *fb = (uint32_t *)(uintptr_t)FB_ADDR;

void __am_gpu_init() {
}

void __am_gpu_config(AM_GPU_CONFIG_T *cfg) {
  uint32_t wh = inl(VGACTL_ADDR);
  int width = wh >> 16;
  int height = wh & 0xffff;
  // int vmemsz = width * height * sizeof(uint32_t);

  *cfg = (AM_GPU_CONFIG_T) {
    .present = true, .has_accel = false,
    .width = width, .height = height,
    .vmemsz = 0
  };
}

void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *ctl) {
  int x = ctl->x, y = ctl->y, h = ctl->h, w = ctl->w;

  uint32_t *pixels = (uint32_t *)(uintptr_t)ctl->pixels;
  int len = (w < 400 - x) ? w : 400-x;
  len = len * sizeof(uint32_t);

  for(int i = 0; i < h && i + y < 300; i++)
  {
    // memcpy(&fb[(y + i) * 400 + x], pixels, len);
    memcpy(fb +(y + i) * 400 + x, pixels, len);
    pixels += w;
  }
  // printf("sync = %d  ", ctl->sync);
  if (ctl->sync) 
  {
    outl(SYNC_ADDR, 1);
  }
}

void __am_gpu_status(AM_GPU_STATUS_T *status) {
  status->ready = true;
}
