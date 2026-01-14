#include <am.h>
#include <riscv/riscv.h>
#include "ysyxsoc.h"

void __am_gpio_config(AM_GPIO_CONFIG_T *cfg) {
  cfg->present = true;
}

void __am_gpio_out(AM_GPIO_OUT_T *dat_out) {
  outl(GPIO_ODR, dat_out->data);
}

void __am_gpio_in(AM_GPIO_IN_T *dat_in) {
  dat_in->data = inl(GPIO_IDR);
}

void __am_gpio_7seg(AM_GPIO_7SEG_T *dat_seg) {
  outl(GPIO_ODR_SEG, dat_seg->data);
}
