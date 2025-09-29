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

#ifndef __ISA_H__
#define __ISA_H__

// Located at src/isa/$(GUEST_ISA)/include/isa-def.h
#include <isa-def.h>
#include <elf.h>

// The macro `__GUEST_ISA__` is defined in $(CFLAGS).
// It will be expanded as "x86" or "mips32" ...
typedef concat(__GUEST_ISA__, _CPU_state) CPU_state;
typedef concat(__GUEST_ISA__, _ISADecodeInfo) ISADecodeInfo;

// monitor
extern unsigned char isa_logo[];
void init_isa();

// reg
extern CPU_state cpu;
void isa_reg_display();
void isa_reg_display_ref(CPU_state *ref);
word_t isa_reg_str2val(const char *name, bool *success);

// exec
struct Decode;
int isa_exec_once(struct Decode *s);

// memory
enum { MMU_DIRECT, MMU_TRANSLATE, MMU_FAIL };
enum { MEM_TYPE_IFETCH, MEM_TYPE_READ, MEM_TYPE_WRITE };
enum { MEM_RET_OK, MEM_RET_FAIL, MEM_RET_CROSS_PAGE };
#ifndef isa_mmu_check
int isa_mmu_check(vaddr_t vaddr, int len, int type);
#endif
paddr_t isa_mmu_translate(vaddr_t vaddr, int len, int type);

// interrupt/exception
vaddr_t isa_raise_intr(word_t NO, vaddr_t epc);
#define INTR_EMPTY ((word_t)-1)
word_t isa_query_intr();

// difftest
bool isa_difftest_checkregs(CPU_state *ref_r, vaddr_t pc);
void isa_difftest_attach();


//  CSR寄存器
typedef struct {
  paddr_t mepc;      // 异常pc    0x341
  paddr_t mcause;    // 原因      0x342
  paddr_t mtvec;     // 异常入口地址 0x305
  paddr_t mstatus;   // 状态      0x300
} riscv32_CSR_state;
extern riscv32_CSR_state riscv32_CSR;

char *get_csr_name(paddr_t csr);
paddr_t *CSR_rw(paddr_t csr);

// ftrace
typedef struct elffun
{
  uint32_t addr;  // 地址
  char name[64];  // 函数名
  uint32_t size;
}elf_fun;
typedef struct _fun_list
{
  elf_fun *old_func;
  elf_fun *new_func;
  paddr_t addr;
  int type;
  struct _fun_list *next;
}fun_list;
#define FUNC_CALL 0
#define FUNC_RET  1
void print_func();

#endif
