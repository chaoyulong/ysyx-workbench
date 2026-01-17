#ifndef __devices_h__
#define __devices_h__

#include <common.h>

#define SERIAL_PORT     0x10000000
#define KBD_ADDR        0x10011000
#define RTC_ADDR        0x02000000


uint32_t rtc_io_handler(uint32_t offset);
uint32_t keyboard_data_io_handler(void);

#endif