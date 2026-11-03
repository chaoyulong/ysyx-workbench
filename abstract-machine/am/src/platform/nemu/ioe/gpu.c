#include <am.h>
#include <nemu.h>
#include <klib.h>

#define SYNC_ADDR (VGACTL_ADDR + 4)

static uint32_t *fb = (uint32_t *)(uintptr_t)FB_ADDR;
static int screen_w = 0, screen_h = 0; 

void __am_gpu_init() {  
  uint32_t wh = inl(VGACTL_ADDR); // 初始化时查询一次长宽
  screen_w = wh >> 16;
  screen_h = wh & 0xffff;
}

void __am_gpu_config(AM_GPU_CONFIG_T *cfg) {
  uint32_t wh = inl(VGACTL_ADDR);
  int width = wh >> 16;
  int height = wh & 0xffff;

  *cfg = (AM_GPU_CONFIG_T) {
    .present = true, .has_accel = false,
    .width = width, .height = height,
    .vmemsz = width * height * sizeof(uint32_t),
  };
}

void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *ctl) {
  int x = ctl->x, y = ctl->y, h = ctl->h, w = ctl->w;
  int pw = (w < screen_w - x) ? w : screen_w - x;     // 横向裁剪到屏幕内
  int ph = (h < screen_h - y) ? h : screen_h - y;     // 纵向裁剪到屏幕内
  size_t len = (size_t)pw * sizeof(uint32_t);

  uint32_t *pixels = (uint32_t *)(uintptr_t)ctl->pixels;
  for(int i = 0; i < ph; i++) {
    memcpy(fb +(y + i) * screen_w + x, pixels, len);  // nemu显存一行要跨screen_w的长度
    pixels += w;                                      // pixels数据一行跨w的长度
  }
  if (ctl->sync) {
    outl(SYNC_ADDR, 1);
  }
}

void __am_gpu_status(AM_GPU_STATUS_T *status) {
  status->ready = true;
}
