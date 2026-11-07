#include "pmem.h"
#include "monitor.h"
#include "cpu-exec.h"
#include "sdb.h"

int main(int argc, char** argv) 
{
  monitor_init(argc, argv);
  cpu_reset(50);
  monitor_mainloop();
#ifdef __USE_NVBOARD__
// 为nvboard板子演示让进程不退出; 但留着会让CI测试卡住
  // void nvboard_update(void);
  // while(1) nvboard_update(void);
#endif
  monitor_exit();
  return is_exit_status_bad();
}

  // sim_init(argc, argv);
  // pmem_init();
  // cpu_reset(50);
  // n_cycle(500);
  // monitor_exit();
  // return 0;