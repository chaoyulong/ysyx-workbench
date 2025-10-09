#define STR_HELPER(x) #x
#define STR(x) STR_HELPER(x)

// #include <verilated.h>
// #include <verilated_fst_c.h>
// #include STR(TOP_NAME.h)    // 自动生成
// #ifdef __USE_NVBOARD__
// #include <nvboard.h>
// #endif

// VerilatedContext* contextp = new VerilatedContext;  // 环境
// TOP_NAME* top = new TOP_NAME{contextp};             // 设计
// VerilatedFstC* tfp = new VerilatedFstC;             // 波形

// static void step_and_dump_wave()
// {  
//   top->eval();
// #ifdef __GET_WAVE__
//   // static uint64_t sim_time = 0;
//   // sim_time++;
//   // tfp->dump(sim_time);
//   tfp->dump(contextp->time());
//   contextp->timeInc(1);
// #endif
// }

// VerilatedContext* contextp = NULL;  // 环境
// VerilatedVcdC* tfp = NULL;          // 波形
// VysyxSoCFull* top;                  // 设计
// VysyxSoCFull___024root* rootp;

// void single_cycle() 
// {
//   top->clock = 0; step_and_dump_wave();
//   top->clock = 1; step_and_dump_wave();
// #ifdef USE_NVBOARD
//   nvboard_update();
// #endif
// }

// void init_sim(int argc, char *argv[])
// {
//   contextp = new VerilatedContext;
//   tfp = new VerilatedVcdC;
//   top = new VysyxSoCFull;
//   contextp ->commandArgs(argc, argv);

// #ifdef __GET_WAVE__
//   init_disasm();    // 初始化反汇编
//   contextp->traceEverOn(true);  // 环境里打开波形开关
//   top->trace(tfp, 0);           // 深度为0
//   tfp->open("./build/sim.vcd");        // 打开要存数据的vcd文件
// #else
//   tfp->close();
// #endif

// #ifdef USE_NVBOARD
//   void nvboard_bind_all_pins(TOP_NAME* top);
//   nvboard_bind_all_pins(top);
//   nvboard_init();
// #endif
// }

// void sim_exit()
// {
//   step_and_dump_wave();
//   tfp->close();
// #ifdef USE_NVBOARD
//   nvboard_quit();
// #endif
// }
