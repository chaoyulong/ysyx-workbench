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

riscv32_CSR_state riscv32_CSR = {.mstatus = 0x1800};
riscv32_CSR_state riscv32_CSR;

char *csr_name[]={"mepc", "mcause", "mtvec", "mstatus"};

char *get_csr_name(paddr_t csr)
{
  switch(csr)
  {
    case 0x341: return csr_name[0];
    case 0x342: return csr_name[1];
    case 0x305: return csr_name[2];
    case 0x300: return csr_name[3];
    default: panic("err csr address");
  }
}

// csr：要操作的CSR寄存器地址，src1：写入CSR寄存器的数据，ret_dat：csr寄存器的数据
paddr_t *CSR_rw(paddr_t csr)
{
  switch(csr)
  {
    case 0x341: return &riscv32_CSR.mepc;
    case 0x342: return &riscv32_CSR.mcause;
    case 0x305: return &riscv32_CSR.mtvec;
    case 0x300: return &riscv32_CSR.mstatus;
    default: panic("err csr address");
  }
}

word_t isa_raise_intr(word_t NO, vaddr_t epc) 
{
  /* TODO: Trigger an interrupt/exception with ``NO''.
   * Then return the address of the interrupt/exception vector.
   */
  riscv32_CSR.mcause = NO;
  riscv32_CSR.mepc = epc;
  // printf("ecall mepc = %x\n", riscv32_CSR.mepc);
  return riscv32_CSR.mtvec;
}

word_t isa_query_intr() {
  return INTR_EMPTY;
}
