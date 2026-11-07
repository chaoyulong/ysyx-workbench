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
  void nvboard_wait_quit(void);
  // 为 nvboard 板子演示让进程不退出; 但 sdb_mainloop() 返回时状态只可能是
  // NPC_END / NPC_ABORT / NPC_QUIT, 没有"还需要刷新板子"的场景,
  // 留着会让 CI / 无终端环境永远卡住(Kill the process due to timeout) ⇒ 注释掉
  // while(1) nvboard_wait_quit();
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