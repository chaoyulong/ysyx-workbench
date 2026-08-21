/* We use the POSIX regex functions to process regular expressions.
 * Type 'man regex' for more information about POSIX regex functions.
 */
#include <regex.h>
#include "pmem.h"
#include "macro.h"
#include "regfile.h"
/*
  表达式求值
*/
enum {
  TK_NOTYPE = 256,  // 空
  TK_EQ,      // 判断相等
  TK_NEQ,     // 判断不等
  TK_AND,     // 逻辑与

  TK_HEX,     // 十六进制数T
  TK_DEC,     // 十进制数
  TK_NEG,     // 十进制负数
  TK_POI,     // 指针
  TK_REG,     // 寄存器
};

#define err_num 555555    // 自定义的一个错误值

static struct rule {
  const char *regex;
  int token_type;
} rules[] = {
  {"0[xX]([0-9a-fA-F])+[Uu]?", TK_HEX},      // decimal number 十六进制识别要放在十进制识别前面，不然会识别到前面的0
  {"[0-9]+[Uu]?", TK_DEC},                   // decimal number \d+不能用，查着应该是正则表达式的版本不支持

  {" +", TK_NOTYPE},    // spaces
  {"\\+", '+'},         // plus
  {"\\-", '-'},         // sub
  {"\\*", '*'},         // mul
  {"\\/", '/'},         // div
  {"\\(", '('},         // 左括号
  {"\\)", ')'},         // 右括号
  {"==", TK_EQ},        // 相等
  {"!=", TK_NEQ},       // 不相等
  {"&&", TK_AND},       // 逻辑与
  {"\\$([a-z0-9])+", TK_REG},        // 读取寄存器值
};

#define NR_REGEX ARRLEN(rules)

static regex_t re[NR_REGEX] = {};

/* Rules are used for many times.
 * Therefore we compile them only once before any usage.
 */
void init_regex() {
  char error_msg[128];
  int ret;

  for(int i = 0; i < NR_REGEX; i ++) {
    ret = regcomp(&re[i], rules[i].regex, REG_EXTENDED);
    if(ret != 0) {
      regerror(ret, &re[i], error_msg, 128);
      printf("regex compilation failed: %s\n%s", error_msg, rules[i].regex);
      assert(0);
    }
  }
}

typedef struct token {
  int type;
  char str[32];
} Token;

static Token tokens[128] __attribute__((used)) = {};
static int nr_token __attribute__((used))  = 0;

static bool make_token(char *e) {
  int position = 0;
  int i;
  regmatch_t pmatch;

  nr_token = 0;

  while(e[position] != '\0') {
    /* Try all rules one by one. */
    for(i = 0; i < NR_REGEX; i ++) {
      if(regexec(&re[i], e + position, 1, &pmatch, 0) == 0 && pmatch.rm_so == 0) {   //  这句话的意思应该就是匹配到规则 
        char *substr_start = e + position;
        int substr_len = pmatch.rm_eo;

        // Log("match rules[%d] = \"%s\" at position %d with len %d: %.*s",
        //     i, rules[i].regex, position, substr_len, substr_len, substr_start);

        position += substr_len;

        substr_len = substr_len > 31 ? 31 : substr_len;   // 防止数据溢出
        // Assert(substr_len < 32, "your num is too long");

        switch(rules[i].token_type) {
          case '+':
          case '-':
          case '*':
          case '/':
          case '(':
          case ')':  
          case TK_EQ:     // 逻辑相等
          case TK_NEQ:
          case TK_AND:
            tokens[nr_token].type = rules[i].token_type;  // 这些符号不需要赋值，只注明类型即可
            nr_token++;
            break;  
          case TK_DEC: 
          case TK_HEX:   
            tokens[nr_token].type = rules[i].token_type;  
            strncpy(tokens[nr_token].str, substr_start, substr_len); 
            tokens[nr_token].str[substr_len] = '\0';    // 用于数据更替时上一次数据比这次长导致字符串错误       
            nr_token++; 
            break;
          case TK_REG:       // 读取寄存器
            tokens[nr_token].type = rules[i].token_type;  // 寄存器形式变量
            strncpy(tokens[nr_token].str, substr_start, substr_len);  // 保留 $ 前缀(isa_reg_str2val 依赖)
            tokens[nr_token].str[substr_len] = '\0';
            nr_token++;
            break;
          default: break;
        }
        break;    // 匹配成功, 跳出规则循环(否则 i==NR_REGEX 会误判为无匹配)
      }
    }
    if(i == NR_REGEX) {
      printf("no match at position %d\n%s\n%*.s^\n", position, e, position, "");
      return false;
    }
  }
  // 负数处理部分
  for(i = 0; i < nr_token; i++) {   // 除去开头负号以外的负数识别，识别完token之后进行负数识别
    // -后面是十进制数字，并且是开头或者前面不是数字类型                                                                 // 
    if(i + 1 < nr_token && tokens[i].type == '-' && tokens[i+1].type == TK_DEC && (i == 0 || (tokens[i-1].type != TK_DEC && tokens[i-1].type != TK_HEX && tokens[i-1].type != ')'))) {
      tokens[i+1].type = TK_NEG;    // 变为负数类型，同时删除前面的-
      for(int j = i; j < nr_token; j++) {
        tokens[j].type = tokens[j+1].type;
        strcpy(tokens[j].str, tokens[j+1].str);
      }
      nr_token--;
    }
  }
  // 指针解引用部分
  for(i = 0; i < nr_token; i++) {   // 除去开头负号以外的负数识别，识别完token之后进行负数识别
    // *后面是十六进制，并且是开头或者前面不是数字类型
    if(i + 1 < nr_token && tokens[i].type == '*' && tokens[i+1].type == TK_HEX && (i == 0 || (tokens[i-1].type != TK_DEC && tokens[i-1].type != TK_HEX && tokens[i-1].type != ')'))) {
      tokens[i+1].type = TK_POI;    // 变为解指针类型，同时删除前面的*
      for(int j = i; j < nr_token; j++) {
        tokens[j].type = tokens[j+1].type;
        strcpy(tokens[j].str, tokens[j+1].str);
      }
      nr_token--;
    }
  }

  return true;
}

static bool check_parentheses(int p, int q) {
  if(tokens[p].type != '(' || tokens[q].type != ')')  // 若整个表达式没有被括号包围
    return false;

  int start = p;    // 开始和结束的指针,p和q一定是括号，要求就是检测出这两个括号是否是匹配的
  int end = q;  
  int count = 0;  // 括号计数，左括号加，右括号减

  while(start < end) {
    if(tokens[start].type == '(') {   //  如果识别到了左括号 
      count++;
    }
    else if(tokens[start].type == ')') {
      count--;
      if(count <= 0)
        return false;
    }
    start++;
  }
  if(count == 1)
    return true;
  else
    return false;
}

/*    C语言运算符优先级
      1   []  ()  .  ->
      2   -(负)  ~  ++  --  *  &  !  (强制转化)  sizeof
      3   /  *  %
      4   +  -
      5   <<  >>
      6   >  >=  <  <=  
      7   ==  !=
      8   &
      9   ^
      10  |
      11  &&
      12  ||
      13  ?:
      14  =  /=  *=  %=  +=  -=  <<=  >>=  &=  ^=  |=
      15  ,
*/

static uint32_t eval(int p, int q) {
  uint32_t val1 = 0, val2 = 0;
  int op = 0;       // 主运算符位置
  int op_type = 0;  // 主运算符类型
  int op_level = 0; // 主运算符的等级，等级越大说明越晚运算
  int count = 0;

  if(p > q) {
    /* Bad expression */
    return err_num;
  }
  else if(p == q) {   //  p=q说明只有一个运算符，特殊符号不可能会单独被拎出来，所以这里只能是数字，只需要区分十六进制或者十进制就行了 
    /* Single token.
     * For now this token should be a number.
     * Return the value of the number.
     */

    switch(tokens[p].type) {
      case TK_HEX:  sscanf(tokens[p].str, "%x", &val1); break;  // 如果数字是十六进制
      case TK_DEC:  sscanf(tokens[p].str, "%u", &val1); break;  // 如果是普通十进制
      case TK_NEG:                                              // 如果是负数十进制
          sscanf(tokens[p].str, "%u", &val1); 
          val1 = 0 - val1;   // 无符号取负, 避免有符号溢出 UB
          break;  
      case TK_POI:                                              // 如果是需要解指针
          paddr_t addr;
          sscanf(tokens[p].str, "%x", &addr);
          if(in_pmem(addr)) {
            val1 = host_read(guest_to_host(addr));
          }
          else {
            printf("error at the address < 0x%x >\n", addr);
            return err_num;
          }
          break;
      case TK_REG:                                              // 如果是需要取寄存器
          bool success;
          val1 = isa_reg_str2val(tokens[p].str, &success);      // 获取寄存器的值
          if(success == false) {
            return err_num;
          }
          break;
      default:  return err_num;
    }
    return val1;
  }
  else if(check_parentheses(p, q) == true) {   //  在括号中包裹的表达式，不需要拆分，只需去掉括号 
    /* The expression is surrounded by a matched pair of parentheses.
     * If that is the case, just throw away the parentheses.
     */
    return eval(p + 1, q - 1);
  }
  else {
    op = -1;
    for(int i = p; i <= q; i++) {   //  op = the position of 主运算符 in the token expression; 
      if(tokens[i].type == ')')         // 如果未找到左括号的时候就找到了右括号，说明表达式有问题，直接返回错误
        return err_num;               
      if(tokens[i].type == '(') {
        count++;
        while(count > 0) {
          i++;
          if(tokens[i].type == '(')
            count++;
          else if(tokens[i].type == ')') {
            count--;
          }
          if(i > q)
            return err_num;             // 如果到最后都没找到右括号，说明括号不匹配，直接返回错误
        }
      }

      if(op_level <= 1 && (tokens[i].type == '*' || tokens[i].type == '/')) {   //  等级小于2，说明目前未出现加减符号，暂时将乘除当作主运算符 
        op_level = 1;
        op = i;
      }
      else if(op_level <= 2 && (tokens[i].type == '+' || tokens[i].type == '-')) {   //  出现加减号之后就把等级提高，不再判断更低级的符号 
        op_level = 2;
        op = i;
      }
      else if(op_level <= 3 && (tokens[i].type == TK_EQ || tokens[i].type == TK_NEQ )) {   //  等于或不等于 
        op_level = 3;
        op = i;
      }
      else if(op_level <= 4 && (tokens[i].type == TK_AND)) {   //  等于或不等于 
        op_level = 4;
        op = i;
      }
    }

    if(op_level == 0)
      return err_num; 
    op_type = tokens[op].type;          // 确定主运算符符号

    // printf("op in %d, is %c\n", op, op_type);

    val1 = eval(p, op - 1);             // 递归计算主运算符两边的表达式
    val2 = eval(op + 1, q);
    if(val1 == err_num || val2 == err_num)  // 如果结果为错误值，则说明有不合法的表达式，在此层也直接返回err传递到上一层
      return err_num;

    switch(op_type) {
      case TK_EQ:   return (val1 == val2);
      case TK_NEQ:  return (val1 != val2);
      case TK_AND:  return (val1 && val2);
      case '+':     return val1 + val2;
      case '-':     return val1 - val2;
      case '*':     return val1 * val2;
      case '/': 
        if(val2 == 0)       // 除数不能为0
          return err_num;
        else
          return (uint64_t)val1 / val2;
      default:      return err_num;          // 如果不是基本运算，直接返回错误
    }
  }
}

word_t expr(char *e, bool *success) {
  if(!make_token(e)) {   //  如果表达式包含未定义字符 
    *success = false;
    return 0;
  }

  uint32_t outcome = eval(0, nr_token - 1);
  if(outcome == err_num) {   //  如果返回的是错误结果 
    *success = false;
    return 0;
  }
  else {
    *success = true;
    return outcome;
  }
}

void expr_test() {
  char result_buff[20], expr_buff[128];
  uint32_t expect_num, got_num;
  bool success = false;
  FILE *fp = fopen("tools/gen-expr/input", "r");
  assert(fp != NULL);

  while(fscanf(fp, "%[^ ]%*c%[^\n]%*c", result_buff, expr_buff) != EOF) {
    expect_num = atoi(result_buff);
    got_num = expr(expr_buff, &success);
    printf("%u, %u, %d\n", expect_num, got_num, success);

  }
}

