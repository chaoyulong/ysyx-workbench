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
  // 只有程序被【暂停】(交互模式下按 ctrl-c / sdb 停住)时才继续刷新板子;
  // 程序正常结束(NPC_END)或出错(NPC_ABORT)时直接退出 ——
  // 否则 CI/无终端环境下会永远卡在这个循环里(表现为 "Kill the process due to timeout")
  if (npc_state.state == NPC_STOP) {
    while(1) nvboard_wait_quit();
  }
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