#include <am.h>
#include <riscv/riscv.h>
#include "ysyxsoc.h"

void __am_uart_init() {
  UART->FCR     = 0xc6;
  UART->LCR     = 0x80;     // 允许访问除数锁存器
  UART->DIV_MSB = 0;
  UART->DIV_LSB = 1;
  UART->LCR     = 0x03;     // 禁止访问除数锁存器
  // outb(UART_FCR, 0xc6);
  // outb(UART_LCR, 0x80);     // 允许访问除数锁存器
  // outb(UART_DIV_MSB, 0);
  // outb(UART_DIV_LSB, 1);
  // outb(UART_LCR, 0x03);     // 禁止访问除数锁存器
}

void __am_uart_config(AM_UART_CONFIG_T *cfg) {
  cfg->present = true;
}

void __am_uart_rx(AM_UART_RX_T *rx){
  if(UART->LSR & 0x01){
    rx->data = UART->DR;
  } else{
    rx->data = 0xff;
  }
  // if(inb(UART_LSR) & 0x01){
  //   rx->data = inb(UART_DR);
  // } else{
  //   rx->data = 0xff;
  // }
}

void __am_uart_tx(AM_UART_TX_T *tx) {
  while(!(UART->LSR & 0x20));    // 等待发送fifo为空
  outb(UART->DR, tx->data);
  // while(!(inb(UART_LSR) & 0x20));    // 等待发送fifo为空
  // outb(SERIAL_PORT, tx->data);
}
