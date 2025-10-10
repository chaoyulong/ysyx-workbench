// #include <cstdlib>
#include <getopt.h>
#include <sys/time.h>
#include "common.h"
#include "log.h"
#include "simulation.h"
// #include <unistd.h> 
// #include "monitor.h"
// #include "memory.h"
// #include "utils.h"
// #include "macro.h"
// #include "v_sim.h"
// #include "sdb.h"
// #include "cpu-exec.h"

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
  printf("Welcome to %s-npc!\n", ANSI_FMT(str(riscv32e), ANSI_FG_YELLOW ANSI_BG_RED));
  printf("For help, type \"help\"\n");
}

// 加载程序
extern uint8_t pmem[];
static long load_img() 
{
  if (img_file == NULL) {
    Log("No image is given. Use the default build-in image.");
    return 4096; // built-in image size
  }

  FILE *fp = fopen(img_file, "rb");
  if(fp == NULL)  
  {
    Log("Image %s not found.", img_file);
    return 4096;
  }

  fseek(fp, 0, SEEK_END);
  long size = ftell(fp);

  Log("The image is %s, size = %ld", img_file, size);

  fseek(fp, 0, SEEK_SET);
  int ret = fread(pmem, size, 1, fp);
  assert(ret == 1);

  fclose(fp);
  return size;
}

// void sdb_set_batch_mode();

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
      // case 'b': sdb_set_batch_mode(); break;
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
// static void init_mem(void) 
// {
//   flash_init();
// }
void pmem_init();

void init_monitor(int argc, char *argv[]) 
{
  parse_args(argc, argv);
  // init_log(log_file);
  // init_cpu_state();
  // init_mem();
  pmem_init();
  long img_size = load_img();
  // init_difftest(diff_so_file, img_size, difftest_port);
  // init_sdb();
  init_sim(argc, argv);
  welcome();
}



static void elf_get_func(char *filename) 
{  
#ifdef CONFIG_FTRACE
  FILE *fp;
  size_t rs;
  int ostype = 0;

  char headtable[5];
  Elf32_Ehdr ehdr;    
  Elf32_Shdr shdr[512], _symtab, _strtab;  // 

  uint32_t shdr_count;  // 节头表数据的数量
  uint32_t sym_count;   // 符号表数据的数量

  if(filename == NULL) return;
  fp = fopen(filename, "r");
  if(fp == NULL) return;
  rs = fread(headtable, 1, 5, fp); if(rs == 0) return;
  if(headtable[0] != 0x7f || headtable[1] != 'E' || headtable[2] != 'L' || headtable[3] != 'F') return;
  ostype = headtable[4] == 1 ? 32 : 64;// 判断elf文件为32位还是64位,
  if(ostype != 32) return;

  fseek(fp, 0, SEEK_SET);
  rs = fread(&ehdr, sizeof(Elf32_Ehdr), 1, fp); if(rs == 0) return;// 获取ELF头
  shdr_count = ehdr.e_shnum;    

  fseek(fp, ehdr.e_shoff, SEEK_SET);
  rs = fread(shdr, sizeof(Elf32_Shdr), shdr_count, fp); if(rs == 0) return;

  char shdr_strtable[shdr[ehdr.e_shstrndx].sh_size];  // 字符串数据暂存
  fseek(fp, shdr[ehdr.e_shstrndx].sh_offset, SEEK_SET); 
  rs = fread(shdr_strtable, 1, shdr[ehdr.e_shstrndx].sh_size, fp); if(rs == 0) return; // 获取结头表的字符串数据

  for(int i = 0; i < shdr_count; i++) // 从结头表分离出符号表和字符串表
  {
    if(strcmp(".symtab", &shdr_strtable[shdr[i].sh_name]) == 0)    // 获取SYMTAB
    {
      _symtab = shdr[i];
    }
    else if(strcmp(".strtab", &shdr_strtable[shdr[i].sh_name]) == 0)    // 获取STRTAB
    {
      _strtab = shdr[i];
    }
  }

  Elf32_Sym symbuf[_symtab.sh_size];
  sym_count = _symtab.sh_size/_symtab.sh_entsize;
  fseek(fp, _symtab.sh_offset, SEEK_SET);  
  rs = fread(symbuf, _symtab.sh_entsize, _symtab.sh_size, fp); if(rs == 0) return;// 获取符号表中的数据

  char strtable[_strtab.sh_size];  // 字符串数据暂存
  fseek(fp, _strtab.sh_offset, SEEK_SET);   
  rs = fread(strtable, 1, _strtab.sh_size, fp); if(rs == 0) return; // 获取字符串表中的数据

  for(int j = 0; j < sym_count; j++)
  {
    unsigned char sym_type = ELF32_ST_TYPE(symbuf[j].st_info);

    if(sym_type == STT_FUNC && symbuf[j].st_size != 0 )
    {
      fun_buf[fun_buf_count].addr = symbuf[j].st_value;
      fun_buf[fun_buf_count].size = symbuf[j].st_size;
      strcpy(fun_buf[fun_buf_count].name, &strtable[symbuf[j].st_name]);
      fun_buf_count++;
    }
  }
  // // 打印所有函数名
  // for (int i = 0; i < fun_buf_count; i++)
  // {
  //   printf("addr = 0x%08x    name = %s\n", fun_buf[i].addr, fun_buf[i].name);
  // }
  #endif
}


