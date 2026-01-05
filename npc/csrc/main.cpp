#include "pmem.h"
#include "monitor.h"
#include "cpu-exec.h"
#include "sdb.h"

int main(int argc, char** argv) 
{
  monitor_init(argc, argv);
  
  monitor_mainloop();
#ifdef __USE_NVBOARD__
  while(1) single_cycle();  // 若使用nvboard,运行就不会结束
#endif
  monitor_exit();
  return is_exit_status_bad();

  // sim_init(argc, argv);
  // pmem_init();
  // cpu_reset(50);
  // n_cycle(500);
  // monitor_exit();
  // return 0;
}
