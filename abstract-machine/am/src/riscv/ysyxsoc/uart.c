#include <am.h>
#include <riscv/riscv.h>
#include "ysyxsoc.h"

void __am_uart_config(AM_UART_CONFIG_T *cfg) {

  cfg->present = true;
}

void __am_uart_rx(AM_UART_RX_T *rx){
  rx->data = uart_rx();
}

void __am_uart_tx(AM_UART_TX_T *tx) 
{
  uart_tx(tx->data);
}

void uart_init(){
  outb(UART_FCR, 0xc6);
  outb(UART_LCR, 0x80);     // 允许访问除数锁存器
  outb(UART_DIV_MSB, 0);
  outb(UART_DIV_LSB, 1);
  outb(UART_LCR, 0x03);     // 禁止访问除数锁存器
}

uint8_t uart_rx(){
  if(inb(UART_LSR) & 0x01){
    return inb(UART_DR);
  } else{
    return 0xff;
  }
}

void uart_tx(uint8_t ch){
  while(!(inb(UART_LSR) & 0x20));    // 等待发送fifo为空
  outb(SERIAL_PORT, ch);
}
