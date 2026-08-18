#include <verilated.h>
#include <verilated_fst_c.h>
#include "simulation.h"
#include "cpu-exec.h"
#ifdef __USE_NVBOARD__
#include <nvboard.h>
#endif

VerilatedContext* contextp = new VerilatedContext;  // 环境
TOP_NAME* top = new TOP_NAME{contextp};             // 设计
VerilatedFstC* tfp = new VerilatedFstC;             // 波形
static void step_and_dump_wave()
{  
  top->eval();
#ifdef __GET_WAVE__
  // static uint64_t sim_time = 0;
  // sim_time++;
  // tfp->dump(sim_time);
  tfp->dump(contextp->time());
  contextp->timeInc(1);
#endif
}

void single_cycle() 
{
  top->clock = 0; step_and_dump_wave();
  top->clock = 1; step_and_dump_wave();
#ifdef __USE_NVBOARD__
  nvboard_update();
#endif
}

void reset(int n) {
  top->reset = 1; top->eval();
  while (n -- > 0) single_cycle();
  top->reset = 0;
}

void n_cycle(int n)
{
  while (n -- > 0) single_cycle();
}

void sim_init(int argc, char *argv[])
{
  contextp ->commandArgs(argc, argv);

#ifdef __GET_WAVE__
  contextp->traceEverOn(true);      // 环境里打开波形开关
  top->trace(tfp, 99);              // 深度为99
  tfp->open("build/waveform.fst");  // 打开要存数据的vcd文件
#else
  tfp->close();
#endif

#ifdef __USE_NVBOARD__
  void nvboard_bind_all_pins(TOP_NAME* top);
  nvboard_bind_all_pins(top);
  nvboard_init();
  nvboard_update();
#endif
}

void sim_exit()
{
  step_and_dump_wave();
  // Final model cleanup
  top->final();
  delete top;
  tfp->close();
#ifdef __USE_NVBOARD__
  nvboard_quit();
#endif
}

// extern "C" void my_ebreak(void){
//   npc_state.state = NPC_END;
// }
