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

#define UART_BASE     0x10000000UL
#define UART          ((UART_TypeDef *)UART_BASE)


typedef struct {
  volatile uint32_t LSB;   // 0x00
  volatile uint32_t MSB;   // 0x04
} RTC_TypeDef;

#define RTC_BASE      0x02000000UL
#define RTC           ((RTC_TypeDef *)RTC_BASE)


typedef struct {
  volatile uint32_t CODE;   // 0x00
} KBD_TypeDef;

#define KBD_BASE      0x10011000UL
#define KBD           ((KBD_TypeDef *)KBD_BASE)

typedef struct {
  union {
    volatile uint32_t Rx[4];    // 0x00~0x0f  接收寄存器0~3,每个占4Byte
    volatile uint32_t Tx[4];    //            发送寄存器0~3
  };
  volatile uint32_t CTRL;     // 0x10 控制寄存器
  volatile uint32_t DIVIDER;  // 0x14 分频寄存器
  volatile uint32_t SS;       // 0x18 片选寄存器
} SPI_TypeDef;

#define SPI_BASE      0x10001000UL
#define SPI           ((SPI_TypeDef *)SPI_BASE)

#define VGA_BUF_BASE  0x21000000UL
#define VGA_BUF32     ((volatile uint32_t *)VGA_BUF_BASE)

typedef struct {
  volatile uint32_t ODR;      // 0x00 输出数据寄存器
  volatile uint32_t IDR;      // 0x04 输入数据寄存器
  volatile uint32_t ODR_SEG;  // 0x08 数码管输出
} GPIO_TypeDef;

#define GPIO_BASE     0x10002000UL
#define GPIO          ((GPIO_TypeDef *)GPIO_BASE)

#endif