#include <am.h>
#include "npc.h"

void __am_timer_init() {
}

void __am_timer_config(AM_TIMER_CONFIG_T *cfg) {
  cfg->present = cfg->has_rtc = true;
}

void __am_timer_uptime(AM_TIMER_UPTIME_T *uptime) {
  // uptime->us = ((uint64_t)inl(RTC_ADDR + 4) << 32) + (uint64_t)inl(RTC_ADDR);
  uptime->us = ((uint64_t)inl(RTC_ADDR + 4) << 32) + (uint64_t)inl(RTC_ADDR) / 100;
}

void __am_timer_rtc(AM_TIMER_RTC_T *rtc) {
  rtc->second = 0;
  rtc->minute = 0;
  rtc->hour   = 0;
  rtc->day    = 0;
  rtc->month  = 0;
  rtc->year   = 1900;
}
