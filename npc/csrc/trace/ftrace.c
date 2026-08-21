#include <elf.h>
#include "common.h"
#include "trace.h"
#include "cpu-exec.h"
#include "log.h"

// ============================== 函数符号表 ============================== //
elf_fun fun_buf[FUN_BUF_MAX];
int fun_buf_count = 0;

// 解析 ELF 文件, 收集函数符号(地址/大小/名字)到 fun_buf
// 供 ftrace 查询当前 pc 属于哪个函数
// 仅在 CONFIG_FTRACE 开启时解析(避免不需要时加载 ELF 浪费启动时间)
void elf_get_func(const char *filename) {
#ifdef CONFIG_FTRACE
  if (filename == NULL) return;
  FILE *fp = fopen(filename, "rb");
  if (fp == NULL) return;

  Elf32_Ehdr ehdr;
  Elf32_Shdr *shdr = NULL;
  char *shstrtab = NULL;
  Elf32_Sym *sym = NULL;
  char *strtab = NULL;
  const Elf32_Shdr *shstr = NULL, *symtab = NULL, *strtab_hdr = NULL;
  int sym_count = 0;

  if (fread(&ehdr, 1, sizeof(ehdr), fp) != sizeof(ehdr)) goto cleanup;
  // 检查 ELF 魔数与位数(32位)
  if (memcmp(ehdr.e_ident, ELFMAG, SELFMAG) != 0 || ehdr.e_ident[EI_CLASS] != ELFCLASS32) goto cleanup;

  // 节头表: e_shnum 边界检查后 malloc(替代固定数组)
  if (ehdr.e_shnum == 0 || ehdr.e_shoff == 0) goto cleanup;
  shdr = (Elf32_Shdr *)malloc(ehdr.e_shnum * sizeof(Elf32_Shdr));
  if (!shdr) goto cleanup;
  if (fseek(fp, ehdr.e_shoff, SEEK_SET) != 0 ||
      fread(shdr, sizeof(Elf32_Shdr), ehdr.e_shnum, fp) != ehdr.e_shnum) goto cleanup;

  // 节名字符串表(替代 VLA)
  if (ehdr.e_shstrndx >= ehdr.e_shnum) goto cleanup;
  shstr = &shdr[ehdr.e_shstrndx];
  if (shstr->sh_size == 0) goto cleanup;
  shstrtab = (char *)malloc(shstr->sh_size);
  if (!shstrtab) goto cleanup;
  if (fseek(fp, shstr->sh_offset, SEEK_SET) != 0 ||
      fread(shstrtab, 1, shstr->sh_size, fp) != shstr->sh_size) goto cleanup;

  // 分离出 .symtab 和 .strtab
  for (int i = 0; i < ehdr.e_shnum; i++) {
    if (shdr[i].sh_name >= shstr->sh_size) continue;
    const char *name = &shstrtab[shdr[i].sh_name];
    if (strcmp(name, ".symtab") == 0)      symtab = &shdr[i];
    else if (strcmp(name, ".strtab") == 0) strtab_hdr = &shdr[i];
  }
  if (!symtab || !strtab_hdr || symtab->sh_entsize == 0 || symtab->sh_size == 0) goto cleanup;

  // 符号表 + 字符串表(替代 VLA)
  sym = (Elf32_Sym *)malloc(symtab->sh_size);
  strtab = (char *)malloc(strtab_hdr->sh_size);
  if (!sym || !strtab) goto cleanup;
  if (fseek(fp, symtab->sh_offset, SEEK_SET) != 0 ||
      fread(sym, symtab->sh_entsize, symtab->sh_size / symtab->sh_entsize, fp) != symtab->sh_size / symtab->sh_entsize) goto cleanup;
  if (fseek(fp, strtab_hdr->sh_offset, SEEK_SET) != 0 ||
      fread(strtab, 1, strtab_hdr->sh_size, fp) != strtab_hdr->sh_size) goto cleanup;

  // 收集函数符号(带边界检查)
  sym_count = symtab->sh_size / symtab->sh_entsize;
  for (int j = 0; j < sym_count && fun_buf_count < FUN_BUF_MAX; j++) {
    if (ELF32_ST_TYPE(sym[j].st_info) == STT_FUNC && sym[j].st_size != 0) {
      if (sym[j].st_name >= strtab_hdr->sh_size) continue;   // 防字符串表越界
      fun_buf[fun_buf_count].addr = sym[j].st_value;
      fun_buf[fun_buf_count].size = sym[j].st_size;
      snprintf(fun_buf[fun_buf_count].name, sizeof(fun_buf[fun_buf_count].name), "%s", &strtab[sym[j].st_name]);
      fun_buf_count++;
    }
  }

cleanup:
  free(shdr);
  free(shstrtab);
  free(sym);
  free(strtab);
  fclose(fp);
#endif
}

// ============================== 调用栈 ============================== //
// 原理: 用 pc 查所属函数; 跨函数时新函数不在栈中 => call(入栈),
//       新函数已在栈中(返回到调用者) => ret(弹出到 pos+1, 保留调用者)
#ifdef CONFIG_FTRACE
#define TRACE_STACK_SIZE 128
static elf_fun *trace_stack[TRACE_STACK_SIZE];
static int trace_top = -1;
static elf_fun *prev_func = NULL;

// 调用顺序记录(按执行顺序的 call 序列, 供 print_func 打印)
#define TRACE_SEQ_MAX 4096
static elf_fun *trace_seq[TRACE_SEQ_MAX];
static int trace_seq_cnt = 0;

static elf_fun *find_func(uint32_t pc) {
  for (int i = 0; i < fun_buf_count; i++)
    if (pc >= fun_buf[i].addr && pc < fun_buf[i].addr + fun_buf[i].size)
      return &fun_buf[i];
  return NULL;
}

void func_trace(void) {
  if (fun_buf_count == 0) return;
  elf_fun *cur = find_func(cpu.pc);
  if (cur == prev_func) return;            // 同函数内, 忽略

  int pos = -1;
  for (int i = 0; i <= trace_top; i++) if (trace_stack[i] == cur) { pos = i; break; }

  if (pos == -1 && cur) {                  // 不在栈中 => 调用
    if (trace_top + 1 < TRACE_STACK_SIZE) {
      trace_stack[++trace_top] = cur;
      cur->call_count++;
      log_write("call  %s\n", cur->name);
      if (trace_seq_cnt < TRACE_SEQ_MAX) trace_seq[trace_seq_cnt++] = cur;   // 记录调用顺序
    }
  } else if (pos >= 0) {                   // 在栈中 => 返回(弹出到 pos+1, 保留调用者)
    while (trace_top > pos) {
      log_write("ret   %s\n", trace_stack[trace_top]->name);
      trace_top--;
    }
  }
  prev_func = cur;
}

void print_func(void) {
  printf("FUNC TRACE:\n");
  for (int i = 0; i < fun_buf_count; i++)
    if (fun_buf[i].call_count > 0)
      printf("  %-30s %u calls\n", fun_buf[i].name, fun_buf[i].call_count);
  printf("CALL SEQUENCE (按执行顺序):\n");
  for (int i = 0; i < trace_seq_cnt; i++)
    printf("  %s\n", trace_seq[i]->name);
}
#endif