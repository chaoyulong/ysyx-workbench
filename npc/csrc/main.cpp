#include "pmem.h"
#include "monitor.h"
#include "cpu-exec.h"
#include "sdb.h"
#include "log.h"

int main(int argc, char** argv) 
{
  monitor_init(argc, argv);
  cpu_reset(50);
  monitor_mainloop();

// 使用nvboard，并且不处于CI测试时，会进入死循环便于查看现象
#if defined(__USE_NVBOARD__) && !defined(__CI_MODE__)
  Log(ANSI_FMT("nvboard sim end", ANSI_FG_GREEN));
  void nvboard_update(void);
  while(1) nvboard_update();
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