#include <verilated.h>
#include <verilated_fst_c.h>
#include "VTopLevel.h"
#ifdef __USE_NVBOARD__
#include <nvboard.h>
#endif

VerilatedContext* const contextp = new VerilatedContext;
TOP_NAME* const top = new TOP_NAME{contextp};
VerilatedFstC* tfp = new VerilatedFstC;

static void step_and_dump_wave()
{  
  top->eval();
#ifdef __GET_WAVE__
  static uint64_t sim_time = 0;
  sim_time++;
  tfp->dump(sim_time);
#endif
}

void single_cycle() 
{
  top->clk = 0; step_and_dump_wave();
  top->clk = 1; step_and_dump_wave();
#ifdef __USE_NVBOARD__
  nvboard_update();
#endif
}

static void reset(int n) {
  top->reset = 1;
  while (n -- > 0) step_and_dump_wave();
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

int main(int argc, char** argv) 
{
  sim_init(argc, argv);

  reset(10);

  top->io_num = 0x1234567;

  n_cycle(100);

#ifdef __USE_NVBOARD__
  while(1) single_cycle();
#endif
  sim_exit();

  return 0;
}
