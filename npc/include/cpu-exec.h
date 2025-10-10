#ifndef __cpu_exec_h__
#define __cpu_exec_h__

#include "common.h"

typedef enum { NPC_RUNNING, NPC_STOP, NPC_END, NPC_ABORT, NPC_QUIT }npc_state_enum;

typedef struct {
  npc_state_enum state;
  vaddr_t halt_pc;
  uint32_t halt_ret;
} NPCState;

extern NPCState npc_state;

typedef struct cpu_state{
#ifdef RISCV32_E
  word_t gpr[16];  
#else
  word_t gpr[32];  
#endif
  paddr_t pc_next;  // 前两个变量的顺序不要修改
  paddr_t pc;
}CPU_state;
extern CPU_state cpu;

typedef struct decode{
  word_t instr;
#ifdef CONFIG_ITRACE
  char log_buf[128];
  char iringbuf[16][128]; 
  int iringbuf_end = 0;
#endif
}Decode;

// void init_cpu_state();
int is_exit_status_bad();
void cpu_reset(int n);
void cpu_exec(uint64_t n);

#endif
