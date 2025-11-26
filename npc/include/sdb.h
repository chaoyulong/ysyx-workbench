#ifndef __sdb_h__
#define __sdb_h__

#include "common.h"

void disasm_init();
word_t expr(char *e, bool *success);

void create_watchpoint(char* arg);  // 创建一个监视点
void delete_watchpoint(int no);   // 删除一个监视点
void display_all_watchpoints(void);    // 查看所有的监视点
bool watchpoint_update(void);

void sdb_set_batch_mode();
void sdb_mainloop();
void init_sdb();

enum { DIFFTEST_TO_DUT, DIFFTEST_TO_REF };
// void init_difftest(char *ref_so_file, long img_size, int port);

#endif
