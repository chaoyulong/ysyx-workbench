#include "pmem.h"
#include "simulation.h"
#include "monitor.h"
#include "cpu-exec.h"
#include "sdb.h"

int main(int argc, char** argv) 
{
//   monitor_init(argc, argv);
//   cpu_reset(50);
//   monitor_mainloop();
// #ifdef __USE_NVBOARD__
//   while(1) single_cycle();  // 若使用nvboard,运行就不会结束
// #endif
//   monitor_exit();
//   return is_exit_status_bad();

  sim_init(argc, argv);
  cpu_reset(50);
  n_cycle(50);
  monitor_exit();
  return 0;
}
