#ifndef __devices_h__
#define __devices_h__

#include <common.h>

#define SERIAL_PORT     0x10000000
#define KBD_ADDR        0x10011000
#define RTC_ADDR        0x02000000   // CLINT mtime

// ---------------------------------------- VGA ---------------------------------------- //
#define VGA_W           640
#define VGA_H           480
#define VGA_BUF_BASE    0x21000000              // 显存起始地址
#define VGA_BUF_SIZE    (VGA_W * VGA_H * 4)     // 显存大小 640*480*4 = 0x12C000
#define VGA_BUF_END     (VGA_BUF_BASE + VGA_BUF_SIZE)
#define VGACTL_ADDR     0x21200000              // 控制寄存器基址: [0]=wh(高16位宽低16位高), [4]=sync

uint32_t rtc_io_handler(uint32_t offset);
uint32_t keyboard_data_io_handler(void);
uint32_t gpu_io_handler(uint32_t offset, bool is_write, uint32_t wdata);
uint32_t vga_fb_read(uint32_t offset);
void     vga_fb_write(uint32_t offset, uint32_t wdata, uint8_t wmask);
uint32_t *vga_fb_addr(void);

void gpu_init(void);
void keyboard_init(void);
void device_update(void);

uint64_t get_time(void);   // 宿主时间(us, 相对启动)


#endif