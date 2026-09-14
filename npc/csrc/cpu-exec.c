#include "simulation.h"
#include "regfile.h"
#include "monitor.h"
#include "cpu-exec.h"
#include "log.h"
#include "pmem.h"
#include "device.h"
#include "trace.h"
#include "sdb.h"

#define DEVICE_UPDATE_CYCLE 20000   // 每多少个周期更新一次外设(SDL事件/屏幕刷新)    
#define ITRACE_TIMEOUT_CYCLE 5000   // 单条指令周期上限(防卡死)

uint64_t g_timer = 0;
uint64_t g_nr_guest_inst = 0;     // 运行了多少条指令
uint64_t g_nr_guest_cycle = 0;    // 运行了多少周期

NPCState npc_state = { .state = NPC_STOP };
CPU_state cpu;

static void trace_and_difftest() {
  IFDEF(CONFIG_ITRACE, itrace_trace());
  IFDEF(CONFIG_MTRACE, mtrace_trace());
  IFDEF(CONFIG_FTRACE, func_trace());
  // IFDEF(CONFIG_DIFFTEST, void difftest_step(vaddr_t pc, vaddr_t npc); if(reg_updated && npc_state.state != NPC_END) {difftest_step(cpu.pc, cpu.pc_next); });

#ifdef CONFIG_WATCHPOINT
  if(watchpoint_update()) {
    npc_state.state = NPC_STOP;
    puts("\nhas changed");
  }
#endif
}

void cpu_reset(int n) {
  reset(n);
}

void cpu_state_init() {
  for(int i = 0; i < REG_NUM; i++) {
    cpu.gpr[i] = 0;
  }
  cpu.pc = RESET_VECTOR;
}

static void exec_once() 
{
  // 执行一条指令: 循环周期直到 WBU 退休(itraceRetireValid), 上限防卡死
  // 用 do-while: 至少先跑1拍, 清掉上一条退休的残留脉冲, 再等新指令退休
  uint64_t cycle_cnt = 0;
  do {
    single_cycle();
    g_nr_guest_cycle++;
    if (++cycle_cnt > ITRACE_TIMEOUT_CYCLE) {
      Log(ANSI_FMT("ERROR: 一条指令超过%u周期未完成 (pc=0x%08x), CPU可能卡死!", ANSI_FG_RED),
          ITRACE_TIMEOUT_CYCLE, (uint32_t)itraceRetirePc);
      npc_state.state = NPC_ABORT;
      break;
    }
  } while (!itraceRetireValid);

  if (npc_state.state == NPC_ABORT) return;
  g_nr_guest_inst++;   // 完成一条指令

  // 状态更新: 用退休指令的 PC (ifu.pc 可能已指向流水线后续)
  cpu.pc    = itraceRetirePc;
  cpu.instr = itraceRetireInstr;
  for (int i = 0; i < REG_NUM; i++) cpu.gpr[i] = gpr(i);

  // 检测程序结束
  if (ebreak) {
    npc_state.state = NPC_END;
  }
  if (Rmcause() == 2) { // 非法指令
    Log(ANSI_FMT("ERROR: 非法指令%08x (pc=0x%08x), CPU可能卡死!", ANSI_FG_RED),
        cpu.instr, (uint32_t)cpu.pc);
    npc_state.state = NPC_ABORT;
  }
}

static void execute(uint64_t n) {
  for (;n > 0; n --) {
    exec_once();
    if (g_nr_guest_cycle % DEVICE_UPDATE_CYCLE == 0) device_update();  // 周期更新外设
    trace_and_difftest();
    if (npc_state.state != NPC_RUNNING) 
      break;
  }
}

static void statistic();
void assert_fail_msg() 
{
  isa_reg_display();
  statistic();
}

/* Simulate how the CPU works. */
void cpu_exec(uint64_t n) 
{
  IFDEF(CONFIG_ITRACE, itrace_set_print(n));  // 单步(<=MAX)时打印指令反汇编
  switch (npc_state.state) {
    case NPC_END: case NPC_ABORT:
      Log("Program execution has ended. To restart the program, exit NPC and run again.");
      return;
    default: npc_state.state = NPC_RUNNING;
  }

  uint64_t start_time = get_time();
  execute(n);
  uint64_t end_time = get_time();
  g_timer += end_time - start_time;

  npc_state.halt_ret = gpr(10);

  switch (npc_state.state) {
    case NPC_RUNNING: npc_state.state = NPC_STOP; break;

    case NPC_END: case NPC_ABORT:
      Log(MUXDEF(__ysyxsoc__, "ysyxsoc", "npc") ": %s at pc = 0x%08x", \
      (npc_state.state == NPC_ABORT ? ANSI_FMT("ABORT", ANSI_FG_RED) : \
      npc_state.halt_ret == 0 ? ANSI_FMT("HIT GOOD TRAP", ANSI_FG_GREEN) : ANSI_FMT("HIT BAD TRAP", ANSI_FG_RED)), \
      cpu.pc); // 打印正确还是错误的信息
      // fall through
    case NPC_QUIT: statistic();
  }
}

int is_exit_status_bad() {
  int good = (npc_state.state == NPC_END && npc_state.halt_ret == 0) || (npc_state.state == NPC_QUIT);
  return !good;
}

static void statistic() {
  Log("host time spent = %lu us", g_timer);
  Log("total execution cycle = %lu", g_nr_guest_cycle);
  Log("total execution inst  = %lu", g_nr_guest_inst);
  if (g_nr_guest_inst > 0) Log("instructions per cycle = %1.4f inst/cycle", (float)g_nr_guest_inst / (float)g_nr_guest_cycle);
  if (g_timer > 0) Log("simulation frequency = %lu cycle/s", g_nr_guest_cycle * 1000000 / g_timer); // 仿真频率
  else Log("Finish running in less than 1 us and can not calculate the simulation frequency");

  // 各 trace/性能统计(统一入口, assert_fail 也会走这里)
  IFDEF(CONFIG_ITRACE, iringbuf_printf());
  IFDEF(CONFIG_MTRACE, mtrace_stat());
  perf_stat();   // 性能计数器统计(读各模块 PerfReg)
  IFDEF(CONFIG_FTRACE, print_func());
}

