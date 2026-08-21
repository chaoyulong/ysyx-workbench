#include <am.h>
#include "npc.h"

// NEMU 风格: 每次读 KBD->CODE 拿到一个完整按键事件
//   事件 = (keydown ? KEYDOWN_MASK : 0) | keycode
#define KEYDOWN_MASK 0x8000

void __am_input_config(AM_INPUT_CONFIG_T *cfg) {
  cfg->present = true;
}

void __am_input_keybrd(AM_INPUT_KEYBRD_T *kbd) {
  uint32_t key = KBD->CODE;                 // 读一个按键事件(无事件时返回 0)
  kbd->keydown = (key & KEYDOWN_MASK) ? true : false;
  kbd->keycode = key & ~KEYDOWN_MASK;
}
