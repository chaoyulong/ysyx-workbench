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

#include "sdb.h"
#include "memory/paddr.h"

#define NR_WP 32

typedef struct watchpoint {
  int NO;
  struct watchpoint *next;
  /* TODO: Add more members if necessary */
  int seted;        // 是否已被设置的标志
  char exec[100];   // 记录表达式
  word_t new_value;
  word_t old_value;
  
} WP;

static WP wp_pool[NR_WP] = {};
static WP *head = NULL, *free_ = NULL;

void init_wp_pool() {
  int i;
  for (i = 0; i < NR_WP; i ++) {
    wp_pool[i].NO = i;
    wp_pool[i].next = (i == NR_WP - 1 ? NULL : &wp_pool[i + 1]);
  }

  head = NULL;
  free_ = wp_pool;
}

// static void wp_list_sorting(WP *list, WP* wp);
static WP * wp_list_sorting(WP *list, WP *wp)
{
  WP *temp_wp;
  if(list == NULL)   // 如果链表为空
  {
    wp->next = NULL;
    list = wp;
  }
  else if(wp->NO < list->NO)    // 否则，如果是开头就小于
  {
    wp->next = list;
    list = wp;
  }
  else
  {
    for(temp_wp = list; temp_wp->next != NULL; temp_wp = temp_wp->next)
    {
      if(wp->NO < temp_wp->next->NO)  // 如果wp的值仅次于temp->next的值
      {
        wp->next = temp_wp->next;
        temp_wp->next = wp;
        break;
      }
    }
    if(temp_wp->next == NULL)         // 找到最后一个都没有比wp还大的，就将其补在末尾
    {
      wp->next = NULL;
      temp_wp->next = wp;  
    }
  } 
  return list;
}

/* TODO: Implement the functionality of watchpoint */
WP* new_wp(void)
{
  WP *p;
  if(free_ == NULL)
  {
    // assert(0);
    return NULL;          // 没有空闲的内存了
  }
  p = free_;
  free_ = free_->next;
  // head = wp_list_sorting(head, p);
  return p;
}

void free_wp(WP *wp)
{
  if(wp == NULL)
  {
    // assert(0);
    printf("Wrong wp\n");
    return;
  }
  wp->old_value = wp->new_value = 0;
  wp->exec[0] = '\0';
  free_ = wp_list_sorting(free_, wp);
}

void create_watchpoint(char* arg)   // 创建一个监视点
{
  WP* temp_wp;
  bool success = false;
  paddr_t outcome = 0; 
  
  outcome = expr(arg, &success);
  if(success == false)            // 判断表达式是否正确
  {
    printf("ERROR:Wrong expression\n");
    return;
  }

  temp_wp = new_wp();
  if(temp_wp == NULL)             // 没有申请成功，说明已没有空闲内存
  {
    // assert(0);
    printf("ERROR:The number of watchpoints is full\n");
    return;
  }
  head = wp_list_sorting(head, temp_wp);  // 申请成功就将其加入head

  temp_wp->new_value = temp_wp->old_value = outcome;   // 赋初始值
  strcpy(temp_wp->exec, arg);
  temp_wp->exec[99] = '\0';         // 防止字符串过长导致复制不到结束符
  printf("New watchpoint NO%d  \"%s\"\n", temp_wp->NO, temp_wp->exec);
}

void delete_watchpoint(int no)   // 删除一个监视点
{
  WP* temp_wp, *p;
  if(head == NULL)  return;
  if(head->NO == no)       // 如果开头就是
  {
    p = head;
    head = head->next;
    free_wp(p);
    printf("Delete watchpoint NO%d\n", no);
  }
  else
  {
    for(temp_wp = head; temp_wp->next != NULL; temp_wp = temp_wp->next)
    {
      if(temp_wp->next->NO == no)
      {
        p = temp_wp->next;
        temp_wp->next = temp_wp->next->next;    // 如果找到了值相等的，链表就直接将其跳过
        free_wp(p);
        printf("Delete watchpoint NO%d\n", no);
        break;
      }
    }
  }
}

void display_all_watchpoints(void)    // 查看所有的监视点
{
  WP* temp_wp;
  for(temp_wp = head; temp_wp != NULL; temp_wp = temp_wp->next)
  {
    printf("NO%d expr = %s  old_value = %-10u  new_value = %-10u\n", 
            temp_wp->NO, temp_wp->exec, temp_wp->old_value, temp_wp->new_value);
  }
}

bool watchpoint_update(void)
{
  WP* temp_wp;
  // WP* changed_wp = NULL;    // 发生变化的监视点
  bool success = false;
  paddr_t outcome = 0; 
  bool changed_flag = false;    // 是否有发生变化的
  for(temp_wp = head; temp_wp != NULL; temp_wp = temp_wp->next)
  {  
    outcome = expr(temp_wp->exec, &success);
    if(success == false)            // 判断表达式是否正确
    {
      printf("ERROR:Wrong expression: %s\n", temp_wp->exec);
      return true;
    }

    temp_wp->old_value = temp_wp->new_value;
    temp_wp->new_value =  outcome;   // 进行数据更替
    if(temp_wp->old_value != temp_wp->new_value)
    {
      printf("\n%d: %s=0x%x\n    old=%u    hex=0x%x\n    new=%u    hex=0x%x\n", 
              temp_wp->NO, temp_wp->exec, outcome, temp_wp->old_value, temp_wp->old_value, temp_wp->new_value, temp_wp->new_value);
      changed_flag = true;
    }
  }
  return changed_flag;
}

