#include <SDL2/SDL.h>
#include "device.h"

// SDL 外设模拟仅用于 npc 平台; ysyxsoc 平台使用真实 RTL 外设
#ifdef __npc__

// -------------------------------------------------------------------------- //
// 键盘模拟: NEMU 风格——每个按键事件 = (keydown ? KEYDOWN_MASK : 0) | keycode  //
//   - keymap: SDL scancode -> AM keycode (与 amdev.h 的 AM_KEYS 对齐)         //
//   - 事件队列: 每个元素是一次完整按键事件(按下/抬起 + 键值)                     //
//   - KBD_ADDR 每读一次弹出一个事件, 队列为空时返回 0                           //
// -------------------------------------------------------------------------- //

#define KEYDOWN_MASK 0x8000

#define NEMU_KEYS(f) \
  f(ESCAPE) f(F1) f(F2) f(F3) f(F4) f(F5) f(F6) f(F7) f(F8) f(F9) f(F10) f(F11) f(F12) \
  f(GRAVE) f(1) f(2) f(3) f(4) f(5) f(6) f(7) f(8) f(9) f(0) f(MINUS) f(EQUALS) f(BACKSPACE) \
  f(TAB) f(Q) f(W) f(E) f(R) f(T) f(Y) f(U) f(I) f(O) f(P) f(LEFTBRACKET) f(RIGHTBRACKET) f(BACKSLASH) \
  f(CAPSLOCK) f(A) f(S) f(D) f(F) f(G) f(H) f(J) f(K) f(L) f(SEMICOLON) f(APOSTROPHE) f(RETURN) \
  f(LSHIFT) f(Z) f(X) f(C) f(V) f(B) f(N) f(M) f(COMMA) f(PERIOD) f(SLASH) f(RSHIFT) \
  f(LCTRL) f(APPLICATION) f(LALT) f(SPACE) f(RALT) f(RCTRL) \
  f(UP) f(DOWN) f(LEFT) f(RIGHT) f(INSERT) f(DELETE) f(HOME) f(END) f(PAGEUP) f(PAGEDOWN)

#define KEY_NAME(k) KEY_##k,
enum {
  KEY_NONE = 0,
  NEMU_KEYS(KEY_NAME)
};

#define SDL_KEYMAP(k) keymap[SDL_SCANCODE_##k] = KEY_##k;
static uint32_t keymap[256] = {};

static void init_keymap(void) {
  NEMU_KEYS(SDL_KEYMAP)
}

void sdl_quit_request(void);   // 定义于 gpu.c

// 按键事件队列: 每个元素 = (keydown ? KEYDOWN_MASK : 0) | keycode
#define KEY_QUEUE_LEN 256
static uint32_t key_queue[KEY_QUEUE_LEN] = {};
static int key_f = 0, key_r = 0;   // f=读指针, r=写指针

static void key_enqueue(uint32_t ev) {
  key_queue[key_r] = ev;
  key_r = (key_r + 1) % KEY_QUEUE_LEN;
  if (key_r == key_f) key_f = (key_f + 1) % KEY_QUEUE_LEN;   // 满则丢最旧
}

static uint32_t key_dequeue(void) {
  if (key_f == key_r) return KEY_NONE;   // 队列空
  uint32_t ev = key_queue[key_f];
  key_f = (key_f + 1) % KEY_QUEUE_LEN;
  return ev;
}

static void send_key(uint8_t scancode, bool is_keydown) {
  if (keymap[scancode] != KEY_NONE) {
    key_enqueue(keymap[scancode] | (is_keydown ? KEYDOWN_MASK : 0));
  }
}

// 轮询 SDL 事件, 由 gpu.c 的 device_update 与键盘读取时调用
void sdl_poll_events(void) {
  SDL_Event ev;
  while (SDL_PollEvent(&ev)) {
    switch (ev.type) {
      case SDL_QUIT: sdl_quit_request(); break;
      case SDL_KEYDOWN: send_key(ev.key.keysym.scancode, true);  break;
      case SDL_KEYUP:   send_key(ev.key.keysym.scancode, false); break;
      default: break;
    }
  }
}

// 读键盘: 弹出一个按键事件, 队列为空返回 0
uint32_t keyboard_data_io_handler(void) {
  sdl_poll_events();
  // 调试: 自动注入 TAB 按下+抬起事件, 绕过 SDL 验证驱动链路
  static uint64_t dbg_cnt = 0;
  if (dbg_cnt == 50) key_enqueue(KEY_TAB | KEYDOWN_MASK);   // TAB down
  if (dbg_cnt == 60) key_enqueue(KEY_TAB);                  // TAB up
  dbg_cnt++;
  return key_dequeue();
}

void keyboard_init(void) { init_keymap(); }

#else   // __ysyxsoc__ 等平台: 真实 RTL 外设, 空实现

uint32_t keyboard_data_io_handler(void) { return 0; }

#endif
