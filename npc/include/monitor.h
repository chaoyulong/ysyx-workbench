#ifndef __monitor_h__
#define __monitor_h__

#include "common.h"

uint64_t get_time(void);

void monitor_init(int argc, char *argv[]);
void monitor_mainloop(void);
void monitor_exit(void);

#endif
