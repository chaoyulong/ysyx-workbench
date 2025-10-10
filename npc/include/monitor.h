#ifndef __monitor_h__
#define __monitor_h__

#include <elf.h>
#include "common.h"
// ftrace
typedef struct elffun
{
  uint32_t addr;  // 地址
  char name[64];  // 函数名
  uint32_t size;
}elf_fun;
typedef struct _fun_list
{
  elf_fun *old_func;
  elf_fun *new_func;
  paddr_t addr;
  int type;
  struct _fun_list *next;
}fun_list;

uint64_t get_time(void);

void monitor_init(int argc, char *argv[]);
void monitor_mainloop(void);
void monitor_exit(void);

#define FUNC_CALL 0
#define FUNC_RET  1

#endif

