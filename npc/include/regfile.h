#ifndef __riscv_reg_h__
#define __riscv_reg_h__

#include "common.h"

#define REG_NUM MUXDEF(__riscv32e__, 16, 32)

uint32_t Rpc(void);
uint32_t gpr(int n);

uint32_t Rmcause(void);

static inline int check_reg_idx(int idx) {
  assert(idx >= 0 && idx < REG_NUM);
  return idx;
}

static inline const char* reg_name(int idx) 
{
  extern const char* regs[];
  return regs[check_reg_idx(idx)];
}

void isa_reg_display();
word_t isa_reg_str2val(const char *s, bool *success);

#endif

