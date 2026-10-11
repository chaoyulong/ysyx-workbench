/***************************************************************************************
* Copyright (c) 2014-2024 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include <isa.h>

riscv32_CSR_state riscv32_CSR = {
  .mstatus = 0x1800,
  .mtvec = 0,
  .mcause = 0,
  .mvendorid = 0x79737978, 
  .marchid = 0x15fde72,
  .mcycle = 0,
  .mcycleh = 0,
  .minstret = 0,
  .minstreth = 0
};

typedef struct {
  const char *name;
  paddr_t     addr;
  paddr_t    *reg;
} csr_entry;

static const csr_entry csr_table[] = {
  { "mstatus",   0x300, &riscv32_CSR.mstatus   },
  { "mtvec",     0x305, &riscv32_CSR.mtvec     },
  { "mepc",      0x341, &riscv32_CSR.mepc      },
  { "mcause",    0x342, &riscv32_CSR.mcause    },
  { "mvendorid", 0xf11, &riscv32_CSR.mvendorid },   // 只读
  { "marchid",   0xf12, &riscv32_CSR.marchid   },   // 只读
  { "mcycle",    0xb00, &riscv32_CSR.mcycle    },
  { "mcycleh",   0xb80, &riscv32_CSR.mcycleh   },
  { "minstret",  0xb02, &riscv32_CSR.minstret  },
  { "minstreth", 0xb82, &riscv32_CSR.minstreth },
};

#define CSR_TABLE_NR (sizeof(csr_table) / sizeof(csr_table[0]))

// 查找有没有实现这个csr寄存器
static const csr_entry *csr_find(paddr_t csr) {
  for (size_t i = 0; i < CSR_TABLE_NR; i++) {
    if (csr_table[i].addr == csr) return &csr_table[i];
  }
  return NULL;
}

char *get_csr_name(paddr_t csr) {
  const csr_entry *e = csr_find(csr);
  return e ? (char *)e->name : "unknown";
}

// csr：要操作的CSR寄存器地址，src1：写入CSR寄存器的数据，ret_dat：csr寄存器的数据
paddr_t *CSR_rw(paddr_t csr)
{
  const csr_entry *e = csr_find(csr);
  if (e == NULL) { panic("err csr address = 0x%x", csr); }
  return e->reg;
}

// NO是异常号，nemu中始终是M模式，ecall的异常号是11
word_t isa_raise_intr(word_t NO, vaddr_t epc) 
{
  riscv32_CSR.mcause = NO;
  riscv32_CSR.mepc = epc;
  return riscv32_CSR.mtvec;
}

void csr_update(void) {
  static uint64_t mcycle = 0, minstret = 0;
  mcycle ++;
  riscv32_CSR.mcycle = (uint32_t)mcycle;
  riscv32_CSR.mcycleh = (uint32_t)(mcycle >> 32);

  minstret ++;
  riscv32_CSR.minstret = (uint32_t)minstret;
  riscv32_CSR.minstreth = (uint32_t)(minstret >> 32);
}

word_t isa_query_intr() {
  return INTR_EMPTY;
}
