#define STR_HELPER(x) #x
#define STR(x) STR_HELPER(x)

// 让 #include 也能用宏替换生成
#include STR( TOP_NAME.h )

#include <verilated.h>
#include <verilated_fst_c.h>
// #include "VCPU.h"
#include <iostream>
#include <iomanip>
#include <cassert>
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
  top->reset = 1;
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

    
uint32_t test_operation(uint32_t rs1, uint32_t rs2, uint8_t alu_ctr, 
                        uint32_t expected, const char* description) {
    top->io_alu_in1 = rs1;
    top->io_alu_in2 = rs2;
    top->io_alu_ctr = alu_ctr;
    step_and_dump_wave();
    
    uint32_t result = top->io_alu_result;
    bool less = top->io_less;
    bool zero = top->io_zero;
    
    bool success = (result == expected);
    
    std::cout << (success ? "✓ " : "✗ ") << description << std::endl;
    std::cout << "  输入: rs1=0x" << std::hex << std::setw(8) << std::setfill('0') << rs1
              << ", rs2=0x" << std::setw(8) << std::setfill('0') << rs2
              << ", ctr=0x" << std::setw(1) << (int)alu_ctr << std::endl;
    std::cout << "  输出: result=0x" << std::setw(8) << std::setfill('0') << result
              << ", less=" << less << ", zero=" << zero << std::endl;
              
    if (!success) {
        std::cout << "  期望: 0x" << std::setw(8) << std::setfill('0') << expected << std::endl;
    }
    std::cout << std::endl;
    
    return success ? 1 : 0;
}

int main(int argc, char** argv) 
{
  sim_init(argc, argv);
  reset(50);
  n_cycle(50);

  std::cout << "=== ALU Verilator 测试 ===" << std::endl;
      int passed = 0;
    int total = 0;
    
    // 算术运算测试
    std::cout << "1. 算术运算测试" << std::endl;
    passed += test_operation(10, 20, 0x0, 30, "ADD: 10 + 20");
    passed += test_operation(50, 30, 0x8, 20, "SUB: 50 - 30");
    passed += test_operation(25, 25, 0x8, 0, "SUB: 25 - 25 = 0");
    passed += test_operation(10, 20, 0x8, 0xFFFFFFF6, "SUB: 10 - 20 = -10");
    total += 4;
    
    // 移位运算测试
    std::cout << "2. 移位运算测试" << std::endl;
    passed += test_operation(0x0000000F, 4, 0x1, 0x000000F0, "SLL: 0xF << 4");
    passed += test_operation(0x000000F0, 4, 0x5, 0x0000000F, "SRL: 0xF0 >> 4");
    passed += test_operation(0xFFFFFF80, 4, 0xD, 0xFFFFFFF8, "SRA: 0xFFFFFF80 >> 4");
    passed += test_operation(0x80000000, 4, 0xD, 0xF8000000, "SRA: 0x80000000 >> 4");
    total += 4;
    
    // 比较运算测试
    std::cout << "3. 比较运算测试" << std::endl;
    passed += test_operation(10, 20, 0x2, 1, "SLT: 10 < 20");
    passed += test_operation(20, 10, 0x2, 0, "SLT: 20 > 10");
    passed += test_operation(0x80000000, 1, 0x2, 1, "SLT: -2147483648 < 1");
    passed += test_operation(0x80000000, 0x7FFFFFFF, 0xA, 0, "SLTU: 0x80000000 > 0x7FFFFFFF");
    total += 4;
    
    // 逻辑运算测试
    std::cout << "4. 逻辑运算测试" << std::endl;
    passed += test_operation(0x12345678, 0x87654321, 0x4, 0x95511559, "XOR");
    passed += test_operation(0x12345678, 0x87654321, 0x6, 0x97755779, "OR");
    passed += test_operation(0x12345678, 0x87654321, 0x7, 0x02244220, "AND");
    total += 3;
    
    // LUI 测试
    std::cout << "5. LUI 测试" << std::endl;
    passed += test_operation(0x12345678, 0x0000ABCD, 0x3, 0x0000ABCD, "LUI");
    total += 1;
    
    std::cout << "测试完成: " << passed << "/" << total << " 通过" << std::endl;
    
    if (passed == total) {
        std::cout << "🎉 所有测试通过! ALU 功能正常" << std::endl;
    } else {
        std::cout << "❌ 有 " << (total - passed) << " 个测试失败" << std::endl;
    }

  // 若使用nvboard,运行就不会结束
#ifdef __USE_NVBOARD__
  while(1) single_cycle();
#endif
  sim_exit();

  return 0;
}
