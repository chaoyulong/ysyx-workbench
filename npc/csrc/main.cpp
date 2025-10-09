#define STR_HELPER(x) #x
#define STR(x) STR_HELPER(x)

// 让 #include 也能用宏替换生成
#include STR( TOP_NAME.h )

#include <verilated.h>
#include <verilated_fst_c.h>
#ifdef __USE_NVBOARD__
#include <nvboard.h>
#endif

#include "pmem.h"

VerilatedContext* const contextp = new VerilatedContext;
TOP_NAME* const top = new TOP_NAME{contextp};
VerilatedFstC* tfp = new VerilatedFstC;

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

static void reset(int n) {
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

extern "C" void my_ebreak(void){
  printf("in ebreak\n");
}

int main(int argc, char** argv) 
{
  pmem_init();
  sim_init(argc, argv);
  reset(50);
  n_cycle(50);

 

  // 若使用nvboard,运行就不会结束
#ifdef __USE_NVBOARD__
  while(1) single_cycle();
#endif
  sim_exit();

  return 0;
}
