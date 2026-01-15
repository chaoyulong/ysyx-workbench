#ifndef __ysyxsoc_h__
#define __ysyxsoc_h__

#include <klib-macros.h>
#include <stdint.h>
#include <riscv/riscv.h>

#define ysyxsoc_trap(code) asm volatile("mv a0, %0; ebreak" : :"r"(code))

typedef struct {
  union {
    volatile uint8_t DR;        // 0x00 数据寄存器
    volatile uint8_t DIV_LSB;   // 0x00 除数低字节
  };
  union {
    volatile uint8_t IER;       // 0x01 中断使能
    volatile uint8_t DIV_MSB;   // 0x01 除数高字节
  };
  volatile uint8_t FCR;         // 0x02 
  volatile uint8_t LCR;         // 0x03 通信格式设置寄存器
  volatile uint8_t _RES0;       // 0x04 保留
  volatile uint8_t LSR;         // 0x05 通信状态寄存器
} UART_TypeDef;

#define UART_BASE   0x10000000UL
#define UART        ((UART_TypeDef *)UART_BASE)

typedef struct {
  volatile uint32_t LSB;   // 0x00
  volatile uint32_t MSB;   // 0x04
} RTC_TypeDef;

#define RTC_BASE   0x02000000UL
#define RTC        ((RTC_TypeDef *)RTC_BASE)

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
