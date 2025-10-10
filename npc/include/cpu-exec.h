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

#ifdef CONFIG_ITRACE
typedef struct decode{
  char log_buf[128];
  char iringbuf[16][128]; 
  int iringbuf_end = 0;
}Decode;
#endif

typedef struct cpu_state{
#ifdef __RISCV32_E__
  word_t gpr[16];       // 寄存器
#else
  word_t gpr[32];  
#endif
  paddr_t pc;           // pc
  word_t instr;         // 指令
#ifdef CONFIG_ITRACE    
  Decode decode;
#endif
}CPU_state;
extern CPU_state cpu;

void cpu_state_init();
int is_exit_status_bad();
void cpu_reset(int n);
void cpu_exec(uint64_t n);

#endif
