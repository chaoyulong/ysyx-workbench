#ifndef __ysyxsoc_h__
#define __ysyxsoc_h__

#include <klib-macros.h>
#include <stdint.h>
#include <riscv/riscv.h>

#define ysyxsoc_trap(code) asm volatile("mv a0, %0; ebreak" : :"r"(code))

#define SERIAL_PORT     0x10000000
  #define UART_DR       (SERIAL_PORT)
  #define UART_IER      (SERIAL_PORT + 1)
  #define UART_FCR      (SERIAL_PORT + 2) 
  #define UART_LCR      (SERIAL_PORT + 3)     // 通信格式设置寄存器
  #define UART_LSR      (SERIAL_PORT + 5)     // 通信状态寄存器
  #define UART_DIV_LSB  (SERIAL_PORT)
  #define UART_DIV_MSB  (SERIAL_PORT + 1)

#define RTC_BASE        0x02000000
  #define RTC_LSB           (RTC_BASE + 0x00)
  #define RTC_MSB           (RTC_BASE + 0x04)

#define KBD_BASE        0x10011000
  #define KBD_CODE          (KBD_BASE + 0x00)

#define SPI_BASE        0x10001000
  #define SPI_Rx0           (SPI_BASE + 0x00)
  #define SPI_Rx1           (SPI_BASE + 0x04)
  #define SPI_Rx2           (SPI_BASE + 0x08)
  #define SPI_Rx3           (SPI_BASE + 0x0c)
  #define SPI_Tx0           (SPI_BASE + 0x00)
  #define SPI_Tx1           (SPI_BASE + 0x04)
  #define SPI_Tx2           (SPI_BASE + 0x08)
  #define SPI_Tx3           (SPI_BASE + 0x0c)
  #define SPI_CTRL          (SPI_BASE + 0x10)
  #define SPI_DIVIDER       (SPI_BASE + 0x14)
  #define SPI_SS            (SPI_BASE + 0x18)

#define VGA_BUF_BASE    0x21000000

#define GPIO_BASE       0x10002000
  #define GPIO_ODR          (GPIO_BASE + 0x00)
  #define GPIO_IDR          (GPIO_BASE + 0x04)
  #define GPIO_ODR_SEG      (GPIO_BASE + 0x08)
#endif

void uart_init();
uint8_t uart_rx();
void uart_tx(uint8_t ch);