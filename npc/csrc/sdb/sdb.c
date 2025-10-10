#include <cstdio>
#include <readline/readline.h>
#include <readline/history.h>
#include "macro.h"
#include "cpu-exec.h"
#include "regfile.h"
#include "pmem.h"
#include "sdb.h"

static int is_batch_mode = false;

/* We use the `readline' library to provide more flexibility to read from stdin. */
static char* rl_gets() 
{
  static char *line_read = NULL;

  if (line_read) {
    free(line_read);
    line_read = NULL;
  }

  line_read = readline("(npc) ");

  if (line_read && *line_read) {
    add_history(line_read);
  }

  return line_read;
}

static int cmd_help(char *args);
static int cmd_c(char *args); 
static int cmd_q(char *args);
static int cmd_si(char *args);
static int cmd_info(char *args);
static int cmd_x(char *args);
static int cmd_p(char *args);
static int cmd_w(char *args);
static int cmd_d(char *args);

static struct {
  const char *name;
  const char *description;
  int (*handler) (char *);
} cmd_table [] = {
  { "help", "Display information about all supported commands", cmd_help },
  { "c", "Continue the execution of the program", cmd_c },
  { "q", "Exit NEMU", cmd_q },
  { "si", "Execute next program line", cmd_si },
  { "info", "print the reg(r) or watchpoint(w) value", cmd_info },
  { "x", "Print memory data", cmd_x },
  { "p", "Expression evaluation", cmd_p },
  { "w", "Set up a watchpoint", cmd_w },
  { "d", "Delete a watchpoint", cmd_d },
};

#define NR_CMD ARRLEN(cmd_table)

static int cmd_help(char *args) 
{
  /* extract the first argument */
  char *arg = strtok(NULL, " ");
  int i;

  if (arg == NULL) {
    /* no argument given */
    for (i = 0; i < NR_CMD; i ++) {
      printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
    }
  }
  else {
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(arg, cmd_table[i].name) == 0) {
        printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
        return 0;
      }
    }
    printf("Unknown command '%s'\n", arg);
  }
  return 0;
}

static int cmd_c(char *args) 
{
  cpu_exec(-1);
  return 0;
}

static int cmd_q(char *args) 
{
  npc_state.state = NPC_QUIT;
  return -1;
}

static int cmd_si(char *args) 
{
  int steps = 0;
  if (args == NULL)  // 没有参数默认执行一步
  {
    cpu_exec(1);  // 模拟CPU执行一条命令
  }
  else 
  {
    if(strspn(args, "0123456789") == strlen(args))  // 如果si之后的字符为纯数字
    {
      steps = atoi(args);
      cpu_exec(steps); 
      printf("-- The program executes %d step forward\n", steps);
    }
    else
    {
      printf("Please enter the right number after <si>\n");
    }
  }
  return 0;
}

static int cmd_info(char *args) 
{
  char *arg = strtok(NULL, " ");  // extract the first argument
  bool success = false;
  word_t result = 0;
  if(arg == NULL)                 //
  {
    printf("Please enter the right cmd after <info>\n");
  }
  else if(strcmp(arg, "r") == 0)   // 如果info之后的字符为r
  {
    arg = strtok(NULL, " ");  // 获取下一个字符
    if(arg == NULL)
      isa_reg_display();        // 打印所有寄存器值
    else
    {
      result = isa_reg_str2val(arg, &success);
      if(success)
      {
        printf("         reg     hex            dec\n");
        printf("-- -- -- %-3s     0x%08x     %-u\n", arg, result, result);
      }
      else        // 若没有找到对应寄存器，说明参数错误
      {
        printf("Unknown reg '%s'\n", arg);
      }
    }
  }
  else if(strcmp(arg, "w") == 0)   // 如果info之后的字符为w
  {
#ifndef CONFIG_WATCHPOINT
  printf("The monitoring point is not enabled, please go to menuconfig to enable it\n");
  return 0;
#endif
    arg = strtok(NULL, " ");  // 获取下一个字符
    if(arg == NULL)
    {
      // display_all_watchpoints();        // 打印所有监视点的值
    }
    else
    {
    }
  }
  else
  {
    printf("Unknown command '%s'\n", arg);
  }
  return 0;
}

static int cmd_x(char *args) 
{
  char *arg = strtok(NULL, " ");  // extract the first argument
  int len = 0;          // 要读取的长度
  paddr_t addr = 0;     // 地址
  char *temp_arg;       // 用来储存截断后的后一段字符串

  if(arg == NULL)                 //
  {
    printf("Please enter the right cmd after <x>\n");
  }
  else
  {
    if(strspn(arg, "0123456789") == strlen(arg))  // 如果x之后的字符为纯数字
    {
      len = atoi(arg);        // 获取第二个参数：读取的长度
      temp_arg = arg + strlen(arg) + 1; // 获取截断后的另一半字符串
      if(strspn(temp_arg + 2, "0123456789abcdefABCDEF") == strlen(temp_arg)-2 && temp_arg[0] == '0' && (temp_arg[1] == 'x' || temp_arg[1] == 'X'))
      {
        sscanf(temp_arg, "%x", &addr);    // 获取首地址
        printf("   addr              hex               dec\n");
        for(int i = 0; i < len; i++)
        {
          word_t dat = host_read(guest_to_host(addr));
          printf("-- 0x%08x        0x%08x        %u\n", addr, dat, dat);
          addr += 4;
        }        
      }
      else
      {
        printf("Please enter the right addr after <x N>\n");
      }
    }
    else
    {
      printf("Please enter the right num after <x>\n");
    }
  }
  return 0;
}

static int cmd_p(char *args) 
{
  bool success = false;
  word_t outcome = 0;
  outcome = expr(args,&success);
  if(success)
    printf("%s = %u\n", args, outcome);
  else
    printf("There is an error in the expression\n");
  return 0;
}

static int cmd_w(char *args) 
{
#ifndef CONFIG_WATCHPOINT
  printf("The monitoring point is not enabled, please go to menuconfig to enable it\n");
  return 0;
#endif
  if(args == NULL)  return 0;
  // create_watchpoint(args);
  return 0;
}

static int cmd_d(char *args) 
{
#ifndef CONFIG_WATCHPOINT
  printf("The monitoring point is not enabled, please go to menuconfig to enable it\n");
  return 0;
#endif
  int no = 555555;
  if(args == NULL)  return 0;
  char *arg = strtok(NULL, " ");  // extract the first argument
  if(strspn(arg, "0123456789") == strlen(arg))  // 如果x之后的字符为纯数字
  {
    no = atoi(arg);
    // delete_watchpoint(no);
  }
  return 0;
}

void sdb_set_batch_mode() 
{
  is_batch_mode = true;
}

void sdb_mainloop() 
{
  if (is_batch_mode) 
  {
    printf("in there\n");
    cmd_c(NULL);
    return;
  }

  for (char *str; (str = rl_gets()) != NULL; ) 
  {
    char *str_end = str + strlen(str);

    /* extract the first token as the command */
    char *cmd = strtok(str, " ");
    if (cmd == NULL) { continue; }

    /* treat the remaining string as the arguments,
     * which may need further parsing
     */
    char *args = cmd + strlen(cmd) + 1;
    if (args >= str_end) {
      args = NULL;
    }

    int i;
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(cmd, cmd_table[i].name) == 0) {
        if (cmd_table[i].handler(args) < 0) { return; }
        break;
      }
    }

    if (i == NR_CMD) { printf("Unknown command '%s'\n", cmd); }
  }
}

void init_regex();
void init_wp_pool();

void init_sdb() 
{
  /* Compile the regular expressions. */
  init_regex();

  /* Initialize the watchpoint pool. */
  init_wp_pool();
}

