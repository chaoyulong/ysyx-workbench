#include <SDL2/SDL.h>
#include "device.h"

#define KEYDOWN_MASK 0x8000

static uint32_t kbd_port_base[1];   // 键盘状态: bit15=keydown, 低15位=AM keycode

void sdl_quit_request(void);

// SDL scancode -> AM keycode 映射 (AM keycode 定义于 abstract-machine/am/include/amdev.h 的 AM_KEYS 宏)
static uint8_t sdl_to_am(SDL_Scancode sc) {
  switch (sc) {
    case SDL_SCANCODE_ESCAPE: return 1;        // AM_KEY_ESCAPE
    case SDL_SCANCODE_F1: return 2;  case SDL_SCANCODE_F2: return 3;
    case SDL_SCANCODE_F3: return 4;  case SDL_SCANCODE_F4: return 5;
    case SDL_SCANCODE_F5: return 6;  case SDL_SCANCODE_F6: return 7;
    case SDL_SCANCODE_F7: return 8;  case SDL_SCANCODE_F8: return 9;
    case SDL_SCANCODE_F9: return 10; case SDL_SCANCODE_F10: return 11;
    case SDL_SCANCODE_F11: return 12; case SDL_SCANCODE_F12: return 13;
    case SDL_SCANCODE_GRAVE: return 14;        // AM_KEY_GRAVE
    case SDL_SCANCODE_1: return 15; case SDL_SCANCODE_2: return 16;
    case SDL_SCANCODE_3: return 17; case SDL_SCANCODE_4: return 18;
    case SDL_SCANCODE_5: return 19; case SDL_SCANCODE_6: return 20;
    case SDL_SCANCODE_7: return 21; case SDL_SCANCODE_8: return 22;
    case SDL_SCANCODE_9: return 23; case SDL_SCANCODE_0: return 24;
    case SDL_SCANCODE_MINUS: return 25; case SDL_SCANCODE_EQUALS: return 26;
    case SDL_SCANCODE_BACKSPACE: return 27;
    case SDL_SCANCODE_TAB: return 28;          // AM_KEY_TAB
    case SDL_SCANCODE_Q: return 29; case SDL_SCANCODE_W: return 30;
    case SDL_SCANCODE_E: return 31; case SDL_SCANCODE_R: return 32;
    case SDL_SCANCODE_T: return 33; case SDL_SCANCODE_Y: return 34;
    case SDL_SCANCODE_U: return 35; case SDL_SCANCODE_I: return 36;
    case SDL_SCANCODE_O: return 37; case SDL_SCANCODE_P: return 38;
    case SDL_SCANCODE_LEFTBRACKET: return 39; case SDL_SCANCODE_RIGHTBRACKET: return 40;
    case SDL_SCANCODE_BACKSLASH: return 41;
    case SDL_SCANCODE_CAPSLOCK: return 42;     // AM_KEY_CAPSLOCK
    case SDL_SCANCODE_A: return 43; case SDL_SCANCODE_S: return 44;
    case SDL_SCANCODE_D: return 45; case SDL_SCANCODE_F: return 46;
    case SDL_SCANCODE_G: return 47; case SDL_SCANCODE_H: return 48;
    case SDL_SCANCODE_J: return 49; case SDL_SCANCODE_K: return 50;
    case SDL_SCANCODE_L: return 51;
    case SDL_SCANCODE_SEMICOLON: return 52; case SDL_SCANCODE_APOSTROPHE: return 53;
    case SDL_SCANCODE_RETURN: return 54;       // AM_KEY_RETURN
    case SDL_SCANCODE_LSHIFT: return 55;       // AM_KEY_LSHIFT
    case SDL_SCANCODE_Z: return 56; case SDL_SCANCODE_X: return 57;
    case SDL_SCANCODE_C: return 58; case SDL_SCANCODE_V: return 59;
    case SDL_SCANCODE_B: return 60; case SDL_SCANCODE_N: return 61;
    case SDL_SCANCODE_M: return 62;
    case SDL_SCANCODE_COMMA: return 63; case SDL_SCANCODE_PERIOD: return 64;
    case SDL_SCANCODE_SLASH: return 65;
    case SDL_SCANCODE_RSHIFT: return 66;       // AM_KEY_RSHIFT
    case SDL_SCANCODE_LCTRL: return 67; case SDL_SCANCODE_APPLICATION: return 68;
    case SDL_SCANCODE_LALT: return 69; case SDL_SCANCODE_SPACE: return 70;
    case SDL_SCANCODE_RALT: return 71; case SDL_SCANCODE_RCTRL: return 72;
    case SDL_SCANCODE_UP: return 73; case SDL_SCANCODE_DOWN: return 74;
    case SDL_SCANCODE_LEFT: return 75; case SDL_SCANCODE_RIGHT: return 76;
    case SDL_SCANCODE_INSERT: return 77; case SDL_SCANCODE_DELETE: return 78;
    case SDL_SCANCODE_HOME: return 79; case SDL_SCANCODE_END: return 80;
    case SDL_SCANCODE_PAGEUP: return 81; case SDL_SCANCODE_PAGEDOWN: return 82;
    default: return 0;                          // AM_KEY_NONE
  }
}

static void send_key(uint8_t scancode, bool is_keydown) {
  uint8_t am_key = sdl_to_am((SDL_Scancode)scancode);
  if (am_key == 0) return;   // 无映射的按键忽略
  kbd_port_base[0] = (is_keydown ? KEYDOWN_MASK : 0) | am_key;
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

uint32_t keyboard_data_io_handler(void) {
  sdl_poll_events();
  return kbd_port_base[0];
}
