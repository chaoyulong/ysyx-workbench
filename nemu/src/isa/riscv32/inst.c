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

#include "local-include/reg.h"
#include <cpu/cpu.h>
#include <cpu/ifetch.h>
#include <cpu/decode.h>

#define R(i) gpr(i)
#define Mr vaddr_read
#define Mw vaddr_write
enum {
  TYPE_I, TYPE_U, TYPE_S, TYPE_J, TYPE_R, TYPE_B,
  TYPE_N, // none
};

#define src1R() do { *src1 = R(rs1); } while (0)
#define src2R() do { *src2 = R(rs2); } while (0)
#define immI() do { *imm = SEXT(BITS(i, 31, 20), 12); } while(0)
#define immU() do { *imm = SEXT(BITS(i, 31, 12), 20) << 12; } while(0)
#define immS() do { *imm = (SEXT(BITS(i, 31, 25), 7) << 5) | BITS(i, 11, 7); } while(0)
#define immJ() do { *imm = (SEXT(BITS(i, 31, 31), 1) << 20) | (BITS(i, 19, 12) << 12)| (BITS(i, 20, 20) << 11) | (BITS(i, 30, 21) << 1); } while(0)
#define immB() do { *imm = (SEXT(BITS(i, 31, 31), 1) << 12) | (BITS(i, 7, 7) << 11)| (BITS(i, 30, 25) << 5) | (BITS(i, 11, 8) << 1); } while(0)

#define csr_uimm BITS((s)->isa.inst, 19, 15)

static void decode_operand(Decode *s, int *rd, word_t *src1, word_t *src2, word_t *imm, int type) {
  uint32_t i = s->isa.inst;
  int rs1 = BITS(i, 19, 15);
  int rs2 = BITS(i, 24, 20);
  *rd     = BITS(i, 11, 7);
  switch (type) {
    case TYPE_I: src1R();          immI(); break;
    case TYPE_U:                   immU(); break;
    case TYPE_S: src1R(); src2R(); immS(); break;
    case TYPE_J:                   immJ(); break;
    case TYPE_R: src1R(); src2R();         break;
    case TYPE_B: src1R(); src2R(); immB(); break;
  }
}

#ifdef CONFIG_FTRACE
static void func_trace(Decode *s, word_t next_addr);
#endif
#ifdef CONFIG_ETRACE
static void e_trace(word_t NO, vaddr_t epc);
#endif

static int decode_exec(Decode *s) {
  int rd = 0;
  word_t src1 = 0, src2 = 0, imm = 0;
  s->dnpc = s->snpc;

#define INSTPAT_INST(s) ((s)->isa.inst)
#define INSTPAT_MATCH(s, name, type, ... /* execute body */ ) { \
  decode_operand(s, &rd, &src1, &src2, &imm, concat(TYPE_, type)); \
  __VA_ARGS__ ; \
}

  INSTPAT_START();
  INSTPAT("??????? ????? ????? ??? ????? 00101 11", auipc  , U, R(rd) = s->pc + imm);
  INSTPAT("??????? ????? ????? ??? ????? 01101 11", lui    , U, R(rd) = imm);

  INSTPAT("??????? ????? ????? ??? ????? 11011 11", jal    , J, IFDEF(CONFIG_FTRACE, func_trace(s, s->pc + imm)); R(rd) = s->pc + 4; s->dnpc = s->pc + imm);

  INSTPAT("??????? ????? ????? 000 ????? 00000 11", lb     , I, R(rd) = SEXT(Mr(src1 + imm, 1), 8));
  INSTPAT("??????? ????? ????? 001 ????? 00000 11", lh     , I, R(rd) = SEXT(Mr(src1 + imm, 2), 16));
  INSTPAT("??????? ????? ????? 010 ????? 00000 11", lw     , I, R(rd) = Mr(src1 + imm, 4)); 
  INSTPAT("??????? ????? ????? 100 ????? 00000 11", lbu    , I, R(rd) = Mr(src1 + imm, 1));  
  INSTPAT("??????? ????? ????? 101 ????? 00000 11", lhu    , I, R(rd) = Mr(src1 + imm, 2));  
  INSTPAT("??????? ????? ????? 000 ????? 00100 11", addi   , I, R(rd) = src1 + imm);  
  INSTPAT("??????? ????? ????? 010 ????? 00100 11", slti   , I, R(rd) = ((int32_t)src1 < (int32_t)imm) ? 1 : 0);
  INSTPAT("??????? ????? ????? 011 ????? 00100 11", sltiu  , I, R(rd) = (src1 < imm) ? 1 : 0);
  INSTPAT("??????? ????? ????? 100 ????? 00100 11", xori   , I, R(rd) = src1 ^ imm);
  INSTPAT("??????? ????? ????? 110 ????? 00100 11", ori    , I, R(rd) = src1 | imm);
  INSTPAT("??????? ????? ????? 111 ????? 00100 11", andi   , I, R(rd) = src1 & imm); 
  INSTPAT("0000000 ????? ????? 001 ????? 00100 11", slli   , I, R(rd) = src1 << imm);  
  INSTPAT("0000000 ????? ????? 101 ????? 00100 11", srli   , I, R(rd) = src1 >> imm);
  INSTPAT("0100000 ????? ????? 101 ????? 00100 11", srai   , I, R(rd) = (word_t)(((int32_t)src1) >> (imm & 0x1f)));
  INSTPAT("??????? ????? ????? 000 ????? 11001 11", jalr   , I, IFDEF(CONFIG_FTRACE, func_trace(s, src1 + imm)); R(rd) = s->pc + 4; s->dnpc = (src1 + imm));  

  INSTPAT("??????? ????? ????? 000 ????? 01000 11", sb     , S, Mw(src1 + imm, 1, src2 & 0x000000ff));
  INSTPAT("??????? ????? ????? 001 ????? 01000 11", sh     , S, Mw(src1 + imm, 2, src2 & 0x0000ffff));
  INSTPAT("??????? ????? ????? 010 ????? 01000 11", sw     , S, Mw(src1 + imm, 4, src2));

  INSTPAT("0000000 ????? ????? 000 ????? 01100 11", add    , R, R(rd) = src1 + src2);   
  INSTPAT("0100000 ????? ????? 000 ????? 01100 11", sub    , R, R(rd) = src1 - src2); 
  INSTPAT("0000000 ????? ????? 100 ????? 01100 11", xor    , R, R(rd) = src1 ^ src2);
  INSTPAT("0000000 ????? ????? 110 ????? 01100 11", or     , R, R(rd) = src1 | src2);
  INSTPAT("0000000 ????? ????? 111 ????? 01100 11", and    , R, R(rd) = src1 & src2);  
  INSTPAT("0000000 ????? ????? 001 ????? 01100 11", sll    , R, R(rd) = src1 << src2);
  INSTPAT("0000000 ????? ????? 101 ????? 01100 11", srl    , R, R(rd) = src1 >> src2);
  INSTPAT("0100000 ????? ????? 101 ????? 01100 11", sra    , R, R(rd) = (word_t)(((int32_t)src1) >> (src2 & 0x1f)));
  INSTPAT("0000000 ????? ????? 010 ????? 01100 11", slt    , R, R(rd) = ((int32_t)src1 < (int32_t)src2) ? 1 : 0);
  INSTPAT("0000000 ????? ????? 011 ????? 01100 11", sltu   , R, R(rd) = (src1 < src2) ? 1 : 0);
   
  INSTPAT("0000001 ????? ????? 000 ????? 01100 11", mul    , R, R(rd) = (word_t)(src1 * src2));
  INSTPAT("0000001 ????? ????? 001 ????? 01100 11", mulh   , R, R(rd) = (word_t)((SEXT(src1, 32) * SEXT(src2, 32)) >> 32)); 
  INSTPAT("0000001 ????? ????? 010 ????? 01100 11", mulhsu , R, R(rd) = (word_t)((SEXT(src1, 32) * (uint64_t)src2) >> 32)); 
  INSTPAT("0000001 ????? ????? 011 ????? 01100 11", mulhu  , R, R(rd) = (word_t)(((uint64_t)src1 * (uint64_t)src2) >> 32)); 
  INSTPAT("0000001 ????? ????? 100 ????? 01100 11", div    , R, R(rd) = (word_t)((int32_t)src1 / (int32_t)src2));
  INSTPAT("0000001 ????? ????? 101 ????? 01100 11", divu   , R, R(rd) = src1 / src2);
  INSTPAT("0000001 ????? ????? 110 ????? 01100 11", rem    , R, R(rd) = (word_t)((int32_t)src1 % (int32_t)src2));
  INSTPAT("0000001 ????? ????? 111 ????? 01100 11", remu   , R, R(rd) = src1 % src2);

  INSTPAT("??????? ????? ????? 000 ????? 11000 11", beq    , B, if(src1 == src2) s->dnpc = s->pc + imm);
  INSTPAT("??????? ????? ????? 001 ????? 11000 11", bne    , B, if(src1 != src2) s->dnpc = s->pc + imm);
  INSTPAT("??????? ????? ????? 100 ????? 11000 11", blt    , B, if((int32_t)src1 < (int32_t)src2) s->dnpc = s->pc + imm);
  INSTPAT("??????? ????? ????? 101 ????? 11000 11", bge    , B, if((int32_t)src1 >= (int32_t)src2) s->dnpc = s->pc + imm);
  INSTPAT("??????? ????? ????? 110 ????? 11000 11", bltu   , B, if(src1 < src2) s->dnpc = s->pc + imm);
  INSTPAT("??????? ????? ????? 111 ????? 11000 11", bgeu   , B, if(src1 >= src2) s->dnpc = s->pc + imm);

  INSTPAT("??????? ????? ????? 001 ????? 11100 11", csrrw  , I, R(rd) = *CSR_rw(imm); *CSR_rw(imm) = src1; /*printf("pc = %x, csr = %s, wdata = %x\n",s->pc, get_csr_name(imm), src1);*/);
  INSTPAT("??????? ????? ????? 010 ????? 11100 11", csrrs  , I, R(rd) = *CSR_rw(imm); *CSR_rw(imm) |= src1; IFDEF(CONFIG_DIFFTEST,difftest_skip_ref());
                                                                                              /*printf("pc = %x, csr = %s, rdata = %x\n",s->pc, get_csr_name(imm), R(rd));*/);
  // INSTPAT("??????? ????? ????? 011 ????? 11100 11", csrrc  , I, puts("csrrc"));
  // INSTPAT("??????? ????? ????? 101 ????? 11100 11", csrrwi , I, puts("csrrwi"));
  // INSTPAT("??????? ????? ????? 110 ????? 11100 11", csrrsi , I, puts("csrrsi"));
  // INSTPAT("??????? ????? ????? 111 ????? 11100 11", csrrci , I, puts("csrrci"));

  INSTPAT("0000000 00000 00000 000 00000 11100 11", ecall  , N, IFDEF(CONFIG_ETRACE, e_trace(R(17), s->pc)); s->dnpc = isa_raise_intr(R(17), s->pc)); // R(17) is $a7
  INSTPAT("0011000 00010 00000 000 00000 11100 11", mret   , N, s->dnpc = riscv32_CSR.mepc;); 
  INSTPAT("0000000 00001 00000 000 00000 11100 11", ebreak , N, NEMUTRAP(s->pc, R(10))); // R(10) is $a0

  INSTPAT("??????? ????? ????? ??? ????? ????? ??", inv    , N, INV(s->pc));
  INSTPAT_END();

  R(0) = 0; // reset $zero to 0

  return 0;
}

int isa_exec_once(Decode *s) {
  s->isa.inst = inst_fetch(&s->snpc, 4);
  return decode_exec(s);
}

#ifdef CONFIG_ETRACE
static void e_trace(word_t NO, vaddr_t epc)
{
  char trace_log_buf[128];
  char *p = trace_log_buf;
  p += sprintf(p, "E mcause= %d, mepc = 0x%08x, mtvec = 0x%08x, mstatus = 0x%08x", NO, epc, riscv32_CSR.mtvec, riscv32_CSR.mstatus);
  
#ifdef CONFIG_ETRACE_COND
  if (ETRACE_COND) { log_write("%s\n", trace_log_buf); }
#endif
  printf("%s\n", trace_log_buf);
}
#endif

#ifdef CONFIG_FTRACE
extern elf_fun fun_buf[1024];
extern uint32_t fun_buf_count;
fun_list *func_list_head = NULL;
fun_list *func_list_last = NULL;
//#ifdef CONFIG_FTRACE
static elf_fun *find_func_by_pc(uint32_t pc) 
{
  for (int i = 0; i < fun_buf_count; i++) 
  {
    if (pc >= fun_buf[i].addr && pc < fun_buf[i].addr + fun_buf[i].size) 
    {
      return &fun_buf[i];
    }
  }
  return NULL;
}

static void func_trace(Decode *s, word_t next_addr)
{
  uint32_t i = s->isa.inst;
  elf_fun *old_func = NULL;    // 跳转前地址
  elf_fun *new_func = NULL;    // 跳转后地址
  int type;

  int rs1 = BITS(i, 19, 15);
  int rd  = BITS(i, 11, 7);

  if(rd == 1)// 使用ra(x1)寄存器，函数调用
  {
    for(int i = 0; i < fun_buf_count; i++)
    {
      old_func = find_func_by_pc(s->pc);   
      new_func = find_func_by_pc(next_addr); 
      type = FUNC_CALL;
      if (/*old_func == NULL ||*/ new_func == NULL) 
      {
        return;
      }
    }
  }
  else if(rd == 0 && rs1 == 1)  // rd = $0, rs1 = ra, 返回
  {
    old_func = find_func_by_pc(s->pc);     
    new_func = find_func_by_pc(next_addr);  
    type = FUNC_RET; 
    if (/*old_func == NULL ||*/ new_func == NULL) 
    {
      return;
    }
  }
  else
  {
    return;
  }

  fun_list *cur = (fun_list *) malloc (sizeof(fun_list));
  cur->old_func = old_func;
  cur->new_func = new_func;
  cur->type = type;
  cur->addr = s->pc;
  cur->next = NULL;
  if(func_list_head == NULL)
  {
    func_list_head = func_list_last = cur;
  }
  else
  { 
    func_list_last->next = cur;
    func_list_last = func_list_last->next;
  }
}

//#endif
void print_func()
{
  char type_str[5];
  fun_list *cur;
  int str_tab = 0;
  int type_last , type_now = FUNC_RET;

  for(cur = func_list_head; cur != NULL; cur = cur->next)
  {
    type_last = type_now;
    type_now = cur->type;
    printf("0x%8x:  ", cur->addr);
 
    if(type_now == FUNC_CALL)  
    {
      if(type_now == type_last) str_tab += 2; 
      strcpy(type_str, "call");
    }
    else 
    {
      if(type_now == type_last) str_tab -= 2; 
      strcpy(type_str, "ret ");
    }
    for(int i = 0; i < str_tab; i++)  putchar(' ');
    printf("%s [%s@%08x]  %s --> %s\n", type_str, cur->new_func->name, cur->new_func->addr, cur->old_func != NULL ? cur->old_func->name : "???", cur->new_func->name);
  }
}
#endif