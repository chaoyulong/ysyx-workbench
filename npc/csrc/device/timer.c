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

void rtc_io_handler(uint32_t offset) {
  assert(offset == 0 || offset == 4);
  if (offset == 4) {
    uint64_t us = get_time();
    rtc_port_base[0] = (uint32_t)us;
    rtc_port_base[1] = us >> 32;
    return rtc_port_base[1];
  }
  return rtc_port_base[0];
}
//uptime->us = ((uint64_t)inl(RTC_ADDR + 4) << 32) + (uint64_t)inl(RTC_ADDR);