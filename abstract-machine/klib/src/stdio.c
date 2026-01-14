#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

#define PRINT_BUF_SIZE 256

const char hex_ascii[]="0123456789abcdef";
// char tmp[PRINT_BUF_SIZE];

// 数据， 储存字符串的地址， 有无符号（0无 1有）， 进制
static uint32_t number_to_ascii(uint32_t dat, char *p, int flag, int base, char blank, int blank_num)
{
  int neg_flag = 0;
  int32_t sign_dat = 0; 
  uint32_t usign_dat = 0;
  uint32_t len = 0;    
  char str[32];   // 缓冲区

  if(dat == 0)
  {
    str[len ++] = '0';
  }
  else
  {  
    if(flag == 0)   // 无符号
    {
      usign_dat = (uint32_t)dat;
      
      while(usign_dat != 0)
      {
        str[len ++] = hex_ascii[usign_dat % base];
        usign_dat = usign_dat / base;
      }
    }
    else
    {
      sign_dat = (int32_t)dat;
      if(sign_dat < 0)
      {
        neg_flag = 1;
        sign_dat = -sign_dat;
      }

      while(sign_dat != 0)
      {
        str[len ++] = hex_ascii[sign_dat % 10];
        sign_dat = sign_dat / 10;
      }    
      if(neg_flag)
        str[len ++] = '-';
    }
  }
    
// 改回正序输出
  if(len > blank_num)  // 不需要扩充的情况，包括不限制位宽(blank_num = 0)和限制位宽数字长度更长(len > blank_num)两种情况
  {
    for(int i = len; i > 0; i--)
    {
      *p = str[i - 1];
      p++;
    } 
    *p = '\0';
    return len;
  }
  else
  {
    int full_offset = 0;  // 用0填充并且是负数时，需要加偏移跳过真实负号

    if(blank == '0' && neg_flag) 
    {
        *p++ = '-';
        full_offset = 1;
    }

    for(int i = blank_num; i > len; i--)
    {
      *p = blank;
      p++;
    } 
    for(int i = len - full_offset; i > 0; i--)
    {
      *p = str[i - 1];
      p++;
    } 
    *p = '\0';
    return blank_num;
  }
  return len;
}

// 指针转换为十六进制
static uint32_t addr_to_ascii(uint64_t dat, char *p)
{
  uint32_t len = 0;
  char str[64];   // 缓冲区

  char temp_data;

  while(dat != 0)
  {
    temp_data = hex_ascii[dat % 16];
    str[len ++] = temp_data;
    dat = dat / 16;
  }

  *p = '0'; p++;
  *p = 'x'; p++; 
  
  for(int i = len; i > 0; i--)
  {
    *p = str[i - 1];
    p++;
  } 
  *p = '\0';
  return len + 2;
}

int printf(const char *fmt, ...) 
{
  char tmp[PRINT_BUF_SIZE];
  va_list arg;
  va_start(arg, fmt);
  uint32_t len = vsnprintf(tmp, PRINT_BUF_SIZE, fmt, arg);
  va_end(arg);

  tmp[PRINT_BUF_SIZE - 1] = '\0';
  for(int i = 0; tmp[i] != '\0'; i++)
  {
    putch(tmp[i]);
  }
  return len;
}

int vsprintf(char *out, const char *fmt, va_list ap) 
{
  uint32_t len = vsnprintf(out, -1, fmt, ap);
  return len;
}

int sprintf(char *out, const char *fmt, ...) 
{
  int buff_len = 0;
  va_list arg;

  va_start(arg, fmt);
  buff_len = vsnprintf(out, -1, fmt, arg);
  va_end(arg);

  return buff_len;
}

int snprintf(char *out, size_t n, const char *fmt, ...) 
{
  int buff_len = 0;
  va_list arg;

  va_start(arg, fmt);
  buff_len = vsnprintf(out, n, fmt, arg);
  va_end(arg);

  return buff_len;
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) 
{
  size_t buff_len = 0;
  int blank_num = 0;
  char blank = ' ';
  while(*fmt != '\0' && buff_len < n)
  {
    if(*fmt == '%' || blank_num != 0)  // 百分号代表开始转换, blank_num有值说明有格式要求的转换
    {
      int tmp_blank_num = blank_num;
      blank_num = 0;
      if(tmp_blank_num == 0)
        fmt++;
      else
        fmt--;

      switch(*fmt)
      {
        case 'f':
        {

        }break;
        case 'c':   // 字符
        {
          char ch = (char)va_arg(ap, int);
          *out = ch;
          out++;
          buff_len++;
        }break;
        case 'o':   // 无符号十六进制数
        case 'O':
        {
          uint32_t inum = (uint32_t)va_arg(ap, uint32_t);
          uint8_t ilen = number_to_ascii(inum, out, 0, 8, blank, tmp_blank_num);
          out += ilen;
          buff_len += ilen;
        }break;
        case 'd':
        case 'i':   // 有符号十进制数
        {
          int32_t inum = (int32_t)va_arg(ap, int32_t);
          uint8_t ilen = number_to_ascii(inum, out, 1, 10, blank, tmp_blank_num);
          out += ilen;
          buff_len += ilen;
        }break;
        case 'u':   // 无符号十进制数
        {
          uint32_t inum = (uint32_t)va_arg(ap, uint32_t);
          uint8_t ilen = number_to_ascii(inum, out, 0, 10, blank, tmp_blank_num);
          out += ilen;
          buff_len += ilen;
        }break;
        case 'x':   // 无符号十六进制数
        case 'X':
        {
          uint32_t inum = (uint32_t)va_arg(ap, uint32_t);
          uint8_t ilen = number_to_ascii(inum, out, 0, 16, blank, tmp_blank_num);
          out += ilen;
          buff_len += ilen;
        }break;
        case 'p':   // 地址
        {
          uint64_t inum = 0;
          if(sizeof(int *) == 8)
            inum = (uint64_t)va_arg(ap, uint64_t);
          else
            inum = (uint32_t)va_arg(ap, uint32_t);

          uint8_t ilen = addr_to_ascii(inum, out);
          out += ilen;
          buff_len += ilen;
        }break;
        case 's':   // 字符串
        {
          char *istr = (char *)va_arg(ap, char *);
          uint32_t ilen = strlen(strcpy(out, istr));
          out += ilen;
          buff_len += ilen;
        }break;
        case '%':
        {
          *out = '%';
          out++;
          buff_len++;
        }       
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
        {
          if(*fmt == '0') blank = '0';  // 用0填充
          else blank = ' '; 

          for(; *fmt <= '9' && *fmt >= '0'; fmt++)
          {
            blank_num = blank_num * 10 + (*fmt - '0');
          }       
        }break;
        default: break;
      }
    }
    else
    {
      *out = *fmt;
      out++;
      buff_len++;
    } 
    fmt++;
  }

  *out = '\0';
  buff_len++;
  return buff_len;
}


#endif
