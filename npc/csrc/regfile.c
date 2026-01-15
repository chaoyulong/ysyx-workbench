#include <stdio.h>
#include "simulation.h"
#include "regfile.h"
#include STR(TOP_NAME.h)    // 自动生成

#ifdef __ysyxsoc__
#include "VysyxSoCFull___024root.h"
#define cpu_pc top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__ifu__DOT__pc
#define cpu_rf top->rootp->ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__regFile__DOT__rf_0
#else
#include "VNPC_TOP___024root.h"
#define cpu_pc top->rootp->NPC_TOP__DOT__cpu__DOT__ifu__DOT__pc
#define cpu_rf top->rootp->NPC_TOP__DOT__cpu__DOT__regFile__DOT__rf_0
#endif
extern TOP_NAME* top;
// VCPU___024root* rootp;

const char *regs[] = {
  "$0", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
  "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
#ifndef __riscv32e__ 
  "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
  "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
#endif
};

const char unfind[] = "xxx";

uint32_t Rpc(void)
{
  return cpu_pc;
}

uint32_t gpr(int n)
{  
  static uint32_t *rf_base_addr = &(cpu_rf);
  if(n >= REG_NUM)
  {
    printf("register only [0 - 15]");
    assert(0);
  }
  return rf_base_addr[n];
}

void isa_reg_display()  // 共有32个寄存器
{   
  int i = 0;
  printf("         reg     hex            dec\n");
  for(i = 0; i < REG_NUM; i++)
  {
    printf("-- %-2d -- %-3s     0x%08x     %-u\n", i, regs[i], gpr(i), gpr(i));
  }
  printf("-- %-2d -- pc      0x%08x\n", i, Rpc());    // 最后打印PC的值,cpu的寄存器组包括gpr和pc
}

word_t isa_reg_str2val(const char *s, bool *success)
{
  *success = false; 
  word_t result = 0;
  if(s[0] != '$')     // 判断符号是否正确
    return result;

  if(strcmp(&s[1], "pc") == 0)
  {
    result = Rpc();
    *success = true;
  }
  else
  {
    for(int i = 0; i < REG_NUM; i++)
    {
      if(strcmp(&s[1], regs[i]) == 0)   // 若查找到该名称的寄存器
      {
        result = gpr(i);
        *success = true;
        break;
      }
    }
  }
  return result;
};