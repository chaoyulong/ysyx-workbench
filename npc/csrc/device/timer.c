#include <common.h>
#include <sys/time.h>
#include "device.h"

static uint64_t boot_time = 0;

static uint32_t rtc_port_base[2];

static uint64_t get_time_internal(){
  struct timeval now;
  gettimeofday(&now, NULL);
  uint64_t us = now.tv_sec * 1000000 + now.tv_usec;

  return us;
}

static uint64_t get_time() {
  if (boot_time == 0) boot_time = get_time_internal();
  uint64_t now = get_time_internal();
  return now - boot_time;
}

// 读取协议: 先读低位(RTC_ADDR), 硬件锁存完整64位; 再读高位(RTC_ADDR+4)返回锁存值
uint32_t rtc_io_handler(uint32_t offset) {
  assert(offset == 0 || offset == 4);
  if (offset == 0) {
    uint64_t us = get_time();
    rtc_port_base[0] = (uint32_t)us;
    rtc_port_base[1] = us >> 32;
  }
  return (offset == 4) ? rtc_port_base[1] : rtc_port_base[0];
}
