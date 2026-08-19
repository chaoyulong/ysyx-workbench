#include <SDL2/SDL.h>
#include "device.h"
#include "cpu-exec.h"

// ---------------------------------------- 显存与显示 ---------------------------------------- //
static uint32_t vgactl_port_base[2];   // [0] = (height << 16) | width, [1] = sync
static uint32_t *vga_fb = NULL;        // 宿主端显存指针, 640*480 个 32 位像素(RGBA8888)
static bool vga_dirty = false;         // 显存是否被修改, 用于触发屏幕刷新
static bool sdl_quit = false;          // 窗口被关闭

static SDL_Window *sdl_window = NULL;
static SDL_Renderer *sdl_renderer = NULL;
static SDL_Texture *sdl_texture = NULL;

void vga_set_dirty(void) { vga_dirty = true; }

uint32_t *vga_fb_addr(void) { return vga_fb; }

// 显存读取: offset 为相对 VGA_BUF_BASE 的偏移
uint32_t vga_fb_read(uint32_t offset) {
  return vga_fb[offset >> 2];
}

// 显存写入: 按字节掩码写入
void vga_fb_write(uint32_t offset, uint32_t wdata, uint8_t wmask) {
  uint32_t mask32 = 0;
  for (int i = 0; i < 4; i++) {
    if (wmask & (1 << i)) mask32 |= (0xFF << (8 * i));
  }
  uint32_t *p = &vga_fb[offset >> 2];
  *p = (*p & ~mask32) | (wdata & mask32);
  vga_dirty = true;
}

// 控制寄存器读写: offset 相对 VGACTL_ADDR
uint32_t gpu_io_handler(uint32_t offset, bool is_write, uint32_t wdata) {
  if (is_write) {
    if (offset == 4) vga_set_dirty();   // 写入 sync, 请求刷新屏幕
    return 0;
  }
  return vgactl_port_base[offset >> 2];
}

// 将显存内容渲染到 SDL 窗口
static void vga_update_screen(void) {
  if (sdl_texture == NULL) return;
  SDL_UpdateTexture(sdl_texture, NULL, vga_fb, VGA_W * 4);
  SDL_RenderClear(sdl_renderer);
  SDL_RenderCopy(sdl_renderer, sdl_texture, NULL, NULL);
  SDL_RenderPresent(sdl_renderer);
  vga_dirty = false;
}

// 轮询 SDL 事件: 键盘事件由 keyboard.c 处理, 这里处理窗口关闭
void sdl_poll_events(void);

void device_update(void) {
  sdl_poll_events();
  if (sdl_quit) npc_state.state = NPC_QUIT;   // 窗口被关闭, 结束仿真
  if (vga_dirty) vga_update_screen();
}

// 初始化 SDL 与显存
void gpu_init(void) {
  vgactl_port_base[0] = (VGA_H << 16) | VGA_W;
  vgactl_port_base[1] = 0;
  vga_fb = (uint32_t *)calloc(VGA_W * VGA_H, sizeof(uint32_t));
  assert(vga_fb);

  if (SDL_Init(SDL_INIT_VIDEO) != 0) {
    printf("[gpu] SDL_Init failed: %s\n", SDL_GetError());
    return;
  }
  sdl_window = SDL_CreateWindow("NPC-VGA", SDL_WINDOWPOS_UNDEFINED, SDL_WINDOWPOS_UNDEFINED,
                                VGA_W, VGA_H, SDL_WINDOW_SHOWN);
  if (sdl_window == NULL) {
    printf("[gpu] SDL_CreateWindow failed: %s\n", SDL_GetError());
    return;
  }
  sdl_renderer = SDL_CreateRenderer(sdl_window, -1, SDL_RENDERER_ACCELERATED);
  if (sdl_renderer == NULL) {
    printf("[gpu] SDL_CreateRenderer failed: %s\n", SDL_GetError());
    return;
  }
  sdl_texture = SDL_CreateTexture(sdl_renderer, SDL_PIXELFORMAT_ARGB8888,
                                  SDL_TEXTUREACCESS_STREAMING, VGA_W, VGA_H);
  if (sdl_texture == NULL) {
    printf("[gpu] SDL_CreateTexture failed: %s\n", SDL_GetError());
    return;
  }
  printf("[gpu] VGA window created: %dx%d\n", VGA_W, VGA_H);
}

void sdl_quit_request(void) { sdl_quit = true; }
