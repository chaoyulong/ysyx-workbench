#include <am.h>
#include "npc.h"

#define KEYDOWN_MASK 0x8000

void __am_input_keybrd(AM_INPUT_KEYBRD_T *kbd) {
  uint32_t code = inl(KBD_ADDR);
  kbd->keydown = code >> 15;
  kbd->keycode = code & (~KEYDOWN_MASK);
}