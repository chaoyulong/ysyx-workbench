#include <am.h>
#include <riscv/riscv.h>
#include "ysyxsoc.h"

void __am_gpio_config(AM_GPIO_CONFIG_T *cfg) {
  cfg->present = true;
}

void __am_gpio_out(AM_GPIO_OUT_T *out) {
  GPIO->ODR = out->data;
}

void __am_gpio_in(AM_GPIO_IN_T *in) {
  in->data = GPIO->IDR;
}

void __am_gpio_7seg(AM_GPIO_7SEG_T *seg) {
  GPIO->ODR_SEG = seg->data;
}
