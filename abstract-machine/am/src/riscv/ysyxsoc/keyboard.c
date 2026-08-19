#include <am.h>
#include "ysyxsoc.h"

static const uint8_t keyboard_to_am_keys[]={
     //  0   1   2   3   4   5   6   7   8   9   a   b   c   d   e   f
/* 0 */  0, 10,  0,  6,  4,  2,  3, 13,  0, 11,  9,  7,  5, 28, 14,  0,
/* 1 */  0, 69, 55,  0, 67, 29, 15,  0,  0,  0, 56, 44, 43, 30, 16,  0,
/* 2 */  0, 58, 57, 45, 31, 18, 17,  0,  0, 70, 59, 46, 33, 32, 19,  0,
/* 3 */  0, 61, 60, 48, 47, 34, 20,  0,  0,  0, 62, 49, 35, 21, 22,  0,
/* 4 */  0, 63, 50, 36, 37, 24, 23,  0,  0, 64, 65, 51, 52, 38, 25,  0,
/* 5 */  0,  0, 53,  0, 39, 26,  0,  0, 42, 66, 54, 40,  0, 41,  0,  0,
/* 6 */  0,  0,  0,  0,  0,  0, 27,  0,  0, 15,  0, 18, 21,  0,  0,  0,
/* 7 */ 24,  0, 16, 19, 20, 22,  1,  0, 12,  0, 17,  0,  0, 23,  0,  0,
/* 8 */  0,  0,  0,  8,  0,  0, 28,  0,  0,  0,  0,  0,  0,  0,  0,  0
};
static const uint8_t keyboard_to_am_keys_extend[]={
     //  0   1   2   3   4   5   6   7   8   9   a   b   c   d   e   f
/* 0 */  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
/* 0 */  0, 71,  0,  0, 72,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
/* 0 */  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
/* 0 */  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
/* 0 */  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
/* 0 */  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
/* 0 */  0,  0,  0,  0,  0,  0,  0,  0,  0, 80,  0, 75, 79,  0,  0,  0, 
/* 0 */ 77, 78, 74,  0, 76, 73,  0,  0,  0,  0, 82,  0,  0, 81,  0,  0, 
};
void __am_input_config(AM_INPUT_CONFIG_T *cfg) { 
  cfg->present = true;  
}

void __am_input_keybrd(AM_INPUT_KEYBRD_T *kbd) {
  uint32_t code = KBD->CODE;  

  if(code == 0){                                    // FIFO 空: 无事件, 返回干净 NONE
    kbd->keydown = 0;
    kbd->keycode = AM_KEY_NONE;
    return;
  }

  if(code == 0xe0){                                 // 如果是扩展码部分，需要再读一位并从扩展码数组获取键值          
    do{code = KBD->CODE;}while(code == 0);      // nvboard的键盘发码较慢，有时会出现读到前面的标志位的码但下一位读出0的情况，此时则需要轮询等待接收      
    if(code == 0xf0){                               // 如果是断码（抬起按键）            
      kbd->keydown = 0;
      do{code = KBD->CODE;}while(code == 0);    // 需要获取真正的键值        
    }
    else{    
      kbd->keydown =1;
    }
    kbd->keycode = keyboard_to_am_keys_extend[code];
  }
  else{
    if(code == 0xf0){                               // 如果是断码（抬起按键）     
      kbd->keydown = 0;
      do{code = KBD->CODE;}while(code == 0);    // 需要获取真正的键值
    }
    else{  
      kbd->keydown =1;
    }
    kbd->keycode = keyboard_to_am_keys[code];
  }
}
