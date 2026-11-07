#include "pmem.h"
#include "monitor.h"
#include "cpu-exec.h"
#include "sdb.h"

int main(int argc, char** argv) 
{
  monitor_init(argc, argv);
  cpu_reset(50);
  monitor_mainloop();
  // 原先这里有 `#ifdef __USE_NVBOARD__ while (1) nvboard_wait_quit();`
  // (为 nvboard 板子演示让进程不退出)。但 sdb_mainloop() 只会在
  //   ① 批处理模式(-b): cmd_c() 跑完就 return, 状态是 NPC_END/NPC_ABORT
  //   ② 交互模式: 用户 q/EOF 才 return, 状态是 NPC_QUIT
  // 两条路径都不会返回 NPC_STOP, 返回后也没有"还需要刷新板子"的状态,
  // 所以这个循环只会让 CI/无终端环境永远卡住(Kill the process due to timeout)。
  monitor_exit();
  return is_exit_status_bad();
}

  // sim_init(argc, argv);
  // pmem_init();
  // cpu_reset(50);
  // n_cycle(500);
  // monitor_exit();
  // return 0;