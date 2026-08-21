// #include <cstdlib>
#include <getopt.h>
#include "common.h"
#include "log.h"
#include "simulation.h"
#include "pmem.h"
#include "device.h"
#include "monitor.h"
#include "sdb.h"
#include "trace.h"

static char *log_file = NULL;
static char *img_file = NULL;
static char *diff_so_file = NULL;
static int difftest_port = 1234;

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



static int parse_args(int argc, char *argv[]) {
  const struct option table[] = {
    {"batch"    , no_argument      , NULL, 'b'},
    {"log"      , required_argument, NULL, 'l'},
    {"diff"     , required_argument, NULL, 'd'},
    {"port"     , required_argument, NULL, 'p'},
    {"help"     , no_argument      , NULL, 'h'},
    {"ftrace"   , required_argument, NULL, 'f'},
    {0          , 0                , NULL,  0 },
  };
  int o;
  while ( (o = getopt_long(argc, argv, "-bhl:p:d:f:", table, NULL)) != -1) {
    switch (o) {
      case 'b': sdb_set_batch_mode(); break;
      case 'p': sscanf(optarg, "%d", &difftest_port); break;
      case 'l': log_file = optarg; break;
      case 'd': diff_so_file = optarg; break;
      case 'f': elf_get_func(optarg); break;
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



