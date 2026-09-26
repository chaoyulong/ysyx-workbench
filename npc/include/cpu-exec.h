#ifndef __cpu_exec_h__
#define __cpu_exec_h__

#include "common.h"
#include "regfile.h"

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
  int iringbuf_end;
}Decode;
#endif

typedef struct {
  word_t  gpr[32];      // 寄存器,恒定32,为了与nemu做difftest
  paddr_t pc;           // pc
} cpu_base_state_t; 

typedef struct cpu_state{
  cpu_base_state_t base;// 基础的pc与寄存器        
  word_t instr;         // 指令
#ifdef CONFIG_ITRACE    
  Decode decode;        // 指令译码
#endif
} CPU_state;
extern CPU_state cpu;

extern uint64_t g_nr_guest_inst; // 已执行指令数
extern uint64_t g_nr_guest_cycle;// 已执行周期数

void cpu_state_init(void);
int is_exit_status_bad(void);
void cpu_reset(int n);
void cpu_exec(uint64_t n);

#endif
