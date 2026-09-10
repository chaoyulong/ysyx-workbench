#include "common.h"
#include "log.h"

// 打印Log
extern uint64_t g_nr_guest_inst;
FILE *log_fp = NULL;

void log_init(const char *log_file) {
  log_fp = stdout;
  if (log_file != NULL) {
    FILE *fp = fopen(log_file, "w");
    Assert(fp, "Can not open '%s'", log_file);
    log_fp = fp;
  }
  Log("Log is written to %s", log_file ? log_file : "stdout");
}

bool log_enable() {
  // CONFIG_TRACE_END == 0 表示不限制上限(一直记到程序结束)
  return MUXDEF(CONFIG_TRACE, (g_nr_guest_inst >= CONFIG_TRACE_START) &&
         (CONFIG_TRACE_END == 0 || g_nr_guest_inst <= CONFIG_TRACE_END), false);
}
