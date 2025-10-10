#include "pmem.h"
#include "simulation.h"
#include "monitor.h"
#include "cpu-exec.h"

extern "C" void my_ebreak(void){
  npc_state.state = NPC_END;
}

int main(int argc, char** argv) 
{
  init_monitor(argc, argv);
  cpu_reset(50);
  sdb_mainloop();
#ifdef __USE_NVBOARD__
  while(1) single_cycle();  // 若使用nvboard,运行就不会结束
#endif
  sim_exit();
  return is_exit_status_bad();
}
