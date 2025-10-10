#include "simulation.h"
#include "regfile.h"
#include "monitor.h"
#include "cpu-exec.h"
#include "log.h"
#include "pmem.h"

#define MAX_INST_TO_PRINT 0    // 最大单步执行多少时打印反汇编

uint64_t g_timer = 0;
bool g_print_step = false;
uint64_t g_nr_guest_inst = 0;     // 运行了多少条指令
uint64_t g_nr_guest_cycle = 0;    // 运行了多少周期

NPCState npc_state = { .state = NPC_STOP };

void cpu_reset(int n)
{
  reset(n);
}

void init_cpu_state()
{
  // for(int i = 0; i < REG_NUM; i++)
  // {
  //   cpu.gpr[i] = 0;
  // }
  cpu.pc = RESET_VECTOR;
  cpu.pc_next = RESET_VECTOR;
}

static void cpu_state_update()
{
  // cpu.pc_next = Rpc();
  // for(int i = 0; i < REG_NUM; i++)
  // {
  //   cpu.gpr[i] = R(i);
  // }
}


static void exec_once() 
{
  single_cycle();
  cpu_state_update();

#ifdef CONFIG_ITRACE
  char *p = cpu_code.log_buf;
  p += snprintf(p, sizeof(cpu_code.log_buf), "0x%08x:", cpu.pc);
  int i;
  uint8_t *inst = (uint8_t *)&cpu_code.instr;
  for (i = 3; i >= 0; i --) {
    p += snprintf(p, 4, " %02x", inst[i]);
  }
  memset(p, ' ', 4);
  p += 4;
  void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);
  if(cpu_code.instr != 0)
    disassemble(p, 128 - (p - cpu_code.log_buf), cpu.pc, (uint8_t *)&cpu_code.instr, 4);
  // if(reg_updated) 
  {
    strcpy(cpu_code.iringbuf[cpu_code.iringbuf_end], cpu_code.log_buf);
    cpu_code.iringbuf_end++;
    if(cpu_code.iringbuf_end > 15)  cpu_code.iringbuf_end = 0;
  }
#endif
}

static void execute(uint64_t n) 
{
  for (;n > 0; n --) 
  {
    exec_once();
    g_nr_guest_cycle++;

    // trace_and_difftest();
    if (npc_state.state != NPC_RUNNING) 
      break;
  }
}

/* Simulate how the CPU works. */
void cpu_exec(uint64_t n) 
{
  g_print_step = (n < MAX_INST_TO_PRINT);
  switch (npc_state.state) 
  {
    case NPC_END: case NPC_ABORT:
      printf("Program execution has ended. To restart the program, exit NPC and run again.\n");
      return;
    default: npc_state.state = NPC_RUNNING;
  }

  uint64_t start_time = get_time();

  execute(n);

  uint64_t end_time = get_time();
  g_timer += end_time - start_time;

  npc_state.halt_ret = gpr(10);

  switch (npc_state.state) 
  {
    case NPC_RUNNING: npc_state.state = NPC_STOP; break;

    case NPC_END: case NPC_ABORT:
      IFDEF(CONFIG_ITRACE, void iringbuf_printf(); iringbuf_printf());
      IFDEF(CONFIG_FTRACE, void print_func(); print_func());
      Log("npc: %s at pc = 0x%08x", \
      (npc_state.state == NPC_ABORT ? ANSI_FMT("ABORT", ANSI_FG_RED) : \
      npc_state.halt_ret == 0 ? ANSI_FMT("HIT GOOD TRAP", ANSI_FG_GREEN) : ANSI_FMT("HIT BAD TRAP", ANSI_FG_RED)), \
      cpu.pc); // 打印正确还是错误的信息
      // fall through
    case NPC_QUIT: static void statistic(); statistic();
  }
}

int is_exit_status_bad() 
{
  int good = (npc_state.state == NPC_END && npc_state.halt_ret == 0) ||
    (npc_state.state == NPC_QUIT);
  return !good;
}

static void statistic() 
{
  // Log("host time spent = %lu us", g_timer);
  Log("total execution cycle = %lu", g_nr_guest_cycle);
  // Log("total execution inst  = %lu", g_nr_guest_inst);  
  
  // Log_nohead("-----------------------------------------");   
  // Log("IFU:"); 
  // Log_nohead("-----------------------------------------"); 
  // Log_nohead("    total get inst  = %lu", g_ifu_get_inst_cnt);  
  // Log_nohead("    get inst time   = %lu", g_ifu_get_inst_time_cnt);
  // Log_nohead("    average if cycle= %.4f cycle/mem_inst", (float)g_ifu_get_inst_time_cnt/(float)g_ifu_get_inst_cnt);
  // Log_nohead("    branch no hit   = %lu", g_ifu_branch_no_hit_cnt);
  // Log_nohead("    branch hit rate = %.4f%%", 100 - (float)g_ifu_branch_no_hit_cnt/(float)g_idu_i_branch * 100);
  // Log_nohead("-----------------------------------------"); 
  // Log("IDU:"); 
  // Log_nohead("-----------------------------------------"); 
  // Log_nohead("    total inst = %lu", g_idu_i_total  ); 
  // Log_nohead("    nop inst   = %lu, %2.4f%%", g_idu_i_nop  , (float)g_idu_i_nop/(float)g_idu_i_total * 100);  
  // Log_nohead("    mem inst   = %lu, %2.4f%%", g_idu_i_mem  , (float)g_idu_i_mem/(float)g_idu_i_total * 100);  
  // Log_nohead("    math inst  = %lu, %2.4f%%", g_idu_i_math , (float)g_idu_i_math/(float)g_idu_i_total * 100);  
  // Log_nohead("    csr inst   = %lu, %2.4f%%", g_idu_i_csr  , (float)g_idu_i_csr/(float)g_idu_i_total * 100);  
  // Log_nohead("    branch inst  = %lu, %2.4f%%", g_idu_i_branch , (float)g_idu_i_branch/(float)g_idu_i_total * 100);  
  // Log_nohead("    other inst = %lu, %2.4f%%", g_idu_i_other, (float)g_idu_i_other/(float)g_idu_i_total * 100);    
  // Log_nohead("-----------------------------------------"); 
  // Log("LSU:"); 
  // Log_nohead("-----------------------------------------"); 
  // Log_nohead("    total Rd/Wr mem     = %lu, %2.4f%%", g_lsu_rw_mem_cnt, (float)g_lsu_rw_mem_cnt/(float)(g_lsu_rw_mem_cnt + g_lsu_rw_device_cnt) * 100); 
  // Log_nohead("    total Rd/Wr device  = %lu, %2.4f%%", g_lsu_rw_device_cnt, (float)g_lsu_rw_device_cnt/(float)(g_lsu_rw_mem_cnt + g_lsu_rw_device_cnt) * 100); 
  // Log_nohead("    total Rd/Wr cycle = %lu", g_lsu_rw_total_time);
  // Log_nohead("    average access cycle = %.4f cycle/mem_inst", (float)g_lsu_rw_total_time/(float)(g_lsu_rw_mem_cnt + g_lsu_rw_device_cnt));
  // Log_nohead("-----------------------------------------"); 
  if (g_timer > 0) Log("simulation frequency = %lu cycle/s", g_nr_guest_cycle * 1000000 / g_timer); // 仿真频率
  else Log("Finish running in less than 1 us and can not calculate the simulation frequency");  
  // 性能计数器打印
  // Log("Instructions per cycle = %1.4f inst/cycle", (float)g_nr_guest_inst/(float)g_nr_guest_cycle);
  
}