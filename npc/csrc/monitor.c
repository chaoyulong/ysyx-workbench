// #include <cstdlib>
#include <getopt.h>
#include <sys/time.h>
#include "common.h"
#include "log.h"
#include "simulation.h"
#include "pmem.h"
#include "monitor.h"
#include "sdb.h"

static char *log_file = NULL;
static char *img_file = NULL;
static char *diff_so_file = NULL;
static int difftest_port = 1234;


// *********************************************** monitor使用的计时器 *********************************************** //
static uint64_t boot_time = 0;

static uint64_t get_time_internal() {
  struct timeval now;
  gettimeofday(&now, NULL);
  uint64_t us = now.tv_sec * 1000000 + now.tv_usec;
  return us;
}

uint64_t get_time() {
  if (boot_time == 0) boot_time = get_time_internal();
  uint64_t now = get_time_internal();
  return now - boot_time;
}

// *********************************************** 欢迎页面 *********************************************** //
static void welcome() 
{
  Log("Trace: %s", MUXDEF(CONFIG_TRACE, ANSI_FMT("ON", ANSI_FG_GREEN), ANSI_FMT("OFF", ANSI_FG_RED)));
  IFDEF(CONFIG_TRACE, Log("If trace is enabled, a log file will be generated "
        "to record the trace. This may lead to a large log file. "
        "If it is not necessary, you can disable it in csrc/config.h"));
  Log("Build time: %s, %s", __TIME__, __DATE__);
  Log("Welcome to %s!", ANSI_FMT(str(riscv32e) "-npc", ANSI_FG_YELLOW ANSI_BG_RED));
  Log("For help, type \"help\"");
}

// 加载程序
static long load_img() {
  extern uint8_t flash[];
  if (img_file == NULL) {
    Log("No image is given. Use the default build-in image.");
    return 4096; // built-in image size
  }

  FILE *fp = fopen(img_file, "rb");
  if(fp == NULL) {
    Log("Image %s not found.", img_file);
    return 4096;
  }

  fseek(fp, 0, SEEK_END);
  long size = ftell(fp);
  Assert(size <= FLASH_SIZE, "Image %s size %ld exceeds flash size %u", img_file, size, (unsigned)FLASH_SIZE);

  Log("The image is %s, size = %ld", img_file, size);

  fseek(fp, 0, SEEK_SET);
  int ret = fread(flash, size, 1, fp);
  assert(ret == 1);

  fclose(fp);
  return size;
}

void sdb_set_batch_mode();

static void elf_get_func(char *filename);

static int parse_args(int argc, char *argv[]) 
{
  const struct option table[] = {
    {"batch"    , no_argument      , NULL, 'b'},
    {"log"      , required_argument, NULL, 'l'},
    {"diff"     , required_argument, NULL, 'd'},
    {"port"     , required_argument, NULL, 'p'},
    {"help"     , no_argument      , NULL, 'h'},
    {"ftrace"   , required_argument, NULL, 'e'},
    {0          , 0                , NULL,  0 },
  };
  int o;
  while ( (o = getopt_long(argc, argv, "-bhl:p:d:e:", table, NULL)) != -1) 
  {
    switch (o) 
    {
      case 'b': sdb_set_batch_mode(); break;
      case 'p': sscanf(optarg, "%d", &difftest_port); break;
      case 'l': log_file = optarg; break;
      case 'd': diff_so_file = optarg; break;
      case 'e': elf_get_func(optarg); break;
      case 1: img_file = optarg; return 0;
      default:
        printf("Usage: %s [OPTION...] IMAGE [args]\n\n", argv[0]);
        printf("\t-b,--batch              run with batch mode\n");
        printf("\t-l,--log=FILE           output log to FILE\n");
        printf("\t-d,--diff=REF_SO        run DiffTest with reference REF_SO\n");
        printf("\t-p,--port=PORT          run DiffTest with port PORT\n");
        printf("\t-f,--ftrace=FILE.elf    open function trace\n");
        printf("\n");
        exit(0);
    }
  }
  return 0;
}

// 如果加载bin文件则会覆盖
// static void init_mem(void) {
//   flash_init();
// }
void pmem_init();
void cpu_state_init();
void disasm_init();

void monitor_init(int argc, char *argv[]) {
  parse_args(argc, argv);

  IFDEF(CONFIG_ITRACE, disasm_init());
  init_log(log_file);
  cpu_state_init();
  pmem_init();
  long img_size = load_img();
  // init_difftest(diff_so_file, img_size, difftest_port);
  init_sdb();
  sim_init(argc, argv);
  welcome();
}

void monitor_mainloop() {
  sdb_mainloop();
}

void monitor_exit() {
  sim_exit();
}

// ----------------------------------- ftrace ----------------------------------- //
#define FUN_BUF_MAX 1024
static elf_fun fun_buf[FUN_BUF_MAX];
static int fun_buf_count = 0;

// 解析 ELF 文件, 收集函数符号(地址/大小/名字)到 fun_buf
// 供 ftrace 查询当前 pc 属于哪个函数
static void elf_get_func(char *filename) {
#ifdef CONFIG_FTRACE
  if (filename == NULL) return;
  FILE *fp = fopen(filename, "rb");
  if (fp == NULL) return;

  Elf32_Ehdr ehdr;
  Elf32_Shdr *shdr = NULL;
  char *shstrtab = NULL;
  Elf32_Sym *sym = NULL;
  char *strtab = NULL;

  if (fread(&ehdr, 1, sizeof(ehdr), fp) != sizeof(ehdr)) goto cleanup;
  // 检查 ELF 魔数与位数(32位)
  if (memcmp(ehdr.e_ident, ELFMAG, SELFMAG) != 0 || ehdr.e_ident[EI_CLASS] != ELFCLASS32) goto cleanup;

  // 节头表: e_shnum 边界检查后 malloc(替代固定数组)
  if (ehdr.e_shnum == 0 || ehdr.e_shoff == 0) goto cleanup;
  shdr = malloc(ehdr.e_shnum * sizeof(Elf32_Shdr));
  if (!shdr) goto cleanup;
  if (fseek(fp, ehdr.e_shoff, SEEK_SET) != 0 ||
      fread(shdr, sizeof(Elf32_Shdr), ehdr.e_shnum, fp) != ehdr.e_shnum) goto cleanup;

  // 节名字符串表(替代 VLA)
  if (ehdr.e_shstrndx >= ehdr.e_shnum) goto cleanup;
  const Elf32_Shdr *shstr = &shdr[ehdr.e_shstrndx];
  if (shstr->sh_size == 0) goto cleanup;
  shstrtab = malloc(shstr->sh_size);
  if (!shstrtab) goto cleanup;
  if (fseek(fp, shstr->sh_offset, SEEK_SET) != 0 ||
      fread(shstrtab, 1, shstr->sh_size, fp) != shstr->sh_size) goto cleanup;

  // 分离出 .symtab 和 .strtab
  const Elf32_Shdr *symtab = NULL, *strtab_hdr = NULL;
  for (int i = 0; i < ehdr.e_shnum; i++) {
    if (shdr[i].sh_name >= shstr->sh_size) continue;
    const char *name = &shstrtab[shdr[i].sh_name];
    if (strcmp(name, ".symtab") == 0)        symtab = &shdr[i];
    else if (strcmp(name, ".strtab") == 0)   strtab_hdr = &shdr[i];
  }
  if (!symtab || !strtab_hdr || symtab->sh_entsize == 0 || symtab->sh_size == 0) goto cleanup;

  // 符号表 + 字符串表(替代 VLA)
  sym = malloc(symtab->sh_size);
  strtab = malloc(strtab_hdr->sh_size);
  if (!sym || !strtab) goto cleanup;
  if (fseek(fp, symtab->sh_offset, SEEK_SET) != 0 ||
      fread(sym, symtab->sh_entsize, symtab->sh_size / symtab->sh_entsize, fp) != symtab->sh_size / symtab->sh_entsize) goto cleanup;
  if (fseek(fp, strtab_hdr->sh_offset, SEEK_SET) != 0 ||
      fread(strtab, 1, strtab_hdr->sh_size, fp) != strtab_hdr->sh_size) goto cleanup;

  // 收集函数符号(带边界检查)
  int sym_count = symtab->sh_size / symtab->sh_entsize;
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


