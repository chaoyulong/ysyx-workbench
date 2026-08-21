#include <SDL2/SDL.h>
#include "device.h"

// SDL 外设模拟仅用于 npc 平台; ysyxsoc 平台使用真实 RTL 外设
#ifdef __npc__

// -------------------------------------------------------------------------- //
// 键盘模拟: 输出 PS/2 Set 2 扫描码字节流(与 ysyxsoc 的 ps2_top_apb 一致)        //
//   - 通码(按下):   [0xE0] make_code                                         //
//   - 断码(抬起):   [0xE0] 0xF0 make_code                                    //
//   - KBD_ADDR 每读一次弹出一个字节, FIFO 为空时返回 0                          //
// -------------------------------------------------------------------------- //

#define KBD_FIFO_SIZE 64

void sdl_quit_request(void);   // 定义于 gpu.c

static uint8_t fifo[KBD_FIFO_SIZE];
static int fifo_head = 0, fifo_tail = 0;   // head=读指针, tail=写指针

static bool fifo_empty(void) { return fifo_head == fifo_tail; }

static void fifo_push(uint8_t byte) {
  int next = (fifo_tail + 1) % KBD_FIFO_SIZE;
  if (next == fifo_head) return;           // FIFO 满, 丢弃
  fifo[fifo_tail] = byte;
  fifo_tail = next;
}

static uint8_t fifo_pop(void) {
  if (fifo_empty()) return 0;
  uint8_t byte = fifo[fifo_head];
  fifo_head = (fifo_head + 1) % KBD_FIFO_SIZE;
  return byte;
}

// SDL scancode -> (PS/2 Set 2 make code, 是否扩展键)
typedef struct { uint8_t make; bool ext; } ps2_key_t;

static ps2_key_t sdl_to_ps2(SDL_Scancode sc) {
  switch (sc) {
    case SDL_SCANCODE_ESCAPE:    return ps2_key_t{0x08, false};
    case SDL_SCANCODE_F1:        return ps2_key_t{0x05, false};
    case SDL_SCANCODE_F2:        return ps2_key_t{0x06, false};
    case SDL_SCANCODE_F3:        return ps2_key_t{0x04, false};
    case SDL_SCANCODE_F4:        return ps2_key_t{0x0C, false};
    case SDL_SCANCODE_F5:        return ps2_key_t{0x03, false};
    case SDL_SCANCODE_F6:        return ps2_key_t{0x0B, false};
    case SDL_SCANCODE_F7:        return ps2_key_t{0x83, false};
    case SDL_SCANCODE_F8:        return ps2_key_t{0x0A, false};
    case SDL_SCANCODE_F9:        return ps2_key_t{0x01, false};
    case SDL_SCANCODE_F10:       return ps2_key_t{0x09, false};
    case SDL_SCANCODE_F11:       return ps2_key_t{0x78, false};
    case SDL_SCANCODE_F12:       return ps2_key_t{0x07, false};
    case SDL_SCANCODE_GRAVE:     return ps2_key_t{0x0E, false};
    case SDL_SCANCODE_1:         return ps2_key_t{0x16, false};
    case SDL_SCANCODE_2:         return ps2_key_t{0x1E, false};
    case SDL_SCANCODE_3:         return ps2_key_t{0x26, false};
    case SDL_SCANCODE_4:         return ps2_key_t{0x25, false};
    case SDL_SCANCODE_5:         return ps2_key_t{0x2E, false};
    case SDL_SCANCODE_6:         return ps2_key_t{0x36, false};
    case SDL_SCANCODE_7:         return ps2_key_t{0x3D, false};
    case SDL_SCANCODE_8:         return ps2_key_t{0x3E, false};
    case SDL_SCANCODE_9:         return ps2_key_t{0x46, false};
    case SDL_SCANCODE_0:         return ps2_key_t{0x45, false};
    case SDL_SCANCODE_MINUS:     return ps2_key_t{0x4E, false};
    case SDL_SCANCODE_EQUALS:    return ps2_key_t{0x55, false};
    case SDL_SCANCODE_BACKSPACE: return ps2_key_t{0x66, false};
    case SDL_SCANCODE_TAB:       return ps2_key_t{0x0D, false};
    case SDL_SCANCODE_Q:         return ps2_key_t{0x15, false};
    case SDL_SCANCODE_W:         return ps2_key_t{0x1D, false};
    case SDL_SCANCODE_E:         return ps2_key_t{0x24, false};
    case SDL_SCANCODE_R:         return ps2_key_t{0x2D, false};
    case SDL_SCANCODE_T:         return ps2_key_t{0x2C, false};
    case SDL_SCANCODE_Y:         return ps2_key_t{0x35, false};
    case SDL_SCANCODE_U:         return ps2_key_t{0x3C, false};
    case SDL_SCANCODE_I:         return ps2_key_t{0x43, false};
    case SDL_SCANCODE_O:         return ps2_key_t{0x44, false};
    case SDL_SCANCODE_P:         return ps2_key_t{0x4D, false};
    case SDL_SCANCODE_LEFTBRACKET:  return ps2_key_t{0x54, false};
    case SDL_SCANCODE_RIGHTBRACKET: return ps2_key_t{0x5B, false};
    case SDL_SCANCODE_BACKSLASH:    return ps2_key_t{0x5D, false};
    case SDL_SCANCODE_CAPSLOCK:     return ps2_key_t{0x58, false};
    case SDL_SCANCODE_A:         return ps2_key_t{0x1C, false};
    case SDL_SCANCODE_S:         return ps2_key_t{0x1B, false};
    case SDL_SCANCODE_D:         return ps2_key_t{0x23, false};
    case SDL_SCANCODE_F:         return ps2_key_t{0x2B, false};
    case SDL_SCANCODE_G:         return ps2_key_t{0x34, false};
    case SDL_SCANCODE_H:         return ps2_key_t{0x33, false};
    case SDL_SCANCODE_J:         return ps2_key_t{0x3B, false};
    case SDL_SCANCODE_K:         return ps2_key_t{0x42, false};
    case SDL_SCANCODE_L:         return ps2_key_t{0x4B, false};
    case SDL_SCANCODE_SEMICOLON: return ps2_key_t{0x4C, false};
    case SDL_SCANCODE_APOSTROPHE:return ps2_key_t{0x52, false};
    case SDL_SCANCODE_RETURN:    return ps2_key_t{0x5A, false};
    case SDL_SCANCODE_LSHIFT:    return ps2_key_t{0x12, false};
    case SDL_SCANCODE_Z:         return ps2_key_t{0x1A, false};
    case SDL_SCANCODE_X:         return ps2_key_t{0x22, false};
    case SDL_SCANCODE_C:         return ps2_key_t{0x21, false};
    case SDL_SCANCODE_V:         return ps2_key_t{0x2A, false};
    case SDL_SCANCODE_B:         return ps2_key_t{0x32, false};
    case SDL_SCANCODE_N:         return ps2_key_t{0x31, false};
    case SDL_SCANCODE_M:         return ps2_key_t{0x3A, false};
    case SDL_SCANCODE_COMMA:     return ps2_key_t{0x41, false};
    case SDL_SCANCODE_PERIOD:    return ps2_key_t{0x49, false};
    case SDL_SCANCODE_SLASH:     return ps2_key_t{0x4A, false};
    case SDL_SCANCODE_RSHIFT:    return ps2_key_t{0x59, false};
    case SDL_SCANCODE_LCTRL:     return ps2_key_t{0x14, false};
    case SDL_SCANCODE_LALT:      return ps2_key_t{0x11, false};
    case SDL_SCANCODE_SPACE:     return ps2_key_t{0x29, false};
    // ------------------------- 扩展键(E0 前缀) ------------------------- //
    case SDL_SCANCODE_RCTRL:     return ps2_key_t{0x14, true};
    case SDL_SCANCODE_RALT:      return ps2_key_t{0x11, true};
    case SDL_SCANCODE_APPLICATION: return ps2_key_t{0x2F, true};
    case SDL_SCANCODE_HOME:      return ps2_key_t{0x6C, true};
    case SDL_SCANCODE_END:       return ps2_key_t{0x69, true};
    case SDL_SCANCODE_PAGEUP:    return ps2_key_t{0x7D, true};
    case SDL_SCANCODE_PAGEDOWN:  return ps2_key_t{0x7A, true};
    case SDL_SCANCODE_INSERT:    return ps2_key_t{0x70, true};
    case SDL_SCANCODE_DELETE:    return ps2_key_t{0x71, true};
    case SDL_SCANCODE_UP:        return ps2_key_t{0x75, true};
    case SDL_SCANCODE_DOWN:      return ps2_key_t{0x72, true};
    case SDL_SCANCODE_LEFT:      return ps2_key_t{0x6B, true};
    case SDL_SCANCODE_RIGHT:     return ps2_key_t{0x74, true};
    default:                     return ps2_key_t{0x00, false};
  }
}

static void send_key(uint8_t scancode, bool is_keydown) {
  ps2_key_t key = sdl_to_ps2((SDL_Scancode)scancode);
  if (key.make == 0) return;               // 无映射的按键忽略
  if (key.ext) fifo_push(0xE0);            // 扩展码前缀
  if (!is_keydown) fifo_push(0xF0);        // 断码前缀
  fifo_push(key.make);
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

// 读键盘: 弹出一个扫描码字节, FIFO 为空返回 0
uint32_t keyboard_data_io_handler(void) {
  sdl_poll_events();
  // 调试: 自动注入 TAB 按键序列 (模拟 SDL keydown+keyup), 绕过 SDL 直接测驱动
  static uint64_t dbg_read_cnt = 0;
  if (dbg_read_cnt == 100) {
    fprintf(stderr, "[kbd-inject] TAB down+up\n");
    fifo_push(0x0D);    // TAB make
    fifo_push(0xF0);    // break 前缀
    fifo_push(0x0D);    // TAB break
  }
  dbg_read_cnt++;
  uint8_t byte = fifo_pop();
  static uint64_t dbg_cnt = 0;
  if (dbg_cnt++ < 60) fprintf(stderr, "[kbd-read] pop=0x%02x (%u)\n", byte, byte);   // 调试
  return byte;
}

#else   // __ysyxsoc__ 等平台: 真实 RTL 外设, 空实现

uint32_t keyboard_data_io_handler(void) { return 0; }

#endif
