#include <klib.h>
#include <klib-macros.h>
//#include <stdint.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

size_t strlen(const char *s) 
{
  size_t len = 0;
  while(*s != '\0')
  {
    len++;
    s++;
  }
  return len;
}

char *strcpy(char *dst, const char *src) 
{
  char* p = NULL;
  if(dst == NULL || src == NULL)
    return NULL;

  p = dst;
  while(*src != '\0')
  {
    *dst = *src;
    dst++;
    src++;
  }
  *dst = '\0';
  return p;
}

char *strncpy(char *dst, const char *src, size_t n) 
{
  if(dst == NULL || src == NULL || n == 0) 
    return NULL;
	char *res = dst;

	while (n > 0)
	{
    if(*src == '\0') break; // 若到达字符串尾则跳出
		*dst = *src;
    dst++; src++;
    n--;
	}
  while(n > 0)  // 若有剩余则使用 \0 填充
  {
    *dst = '\0';
    dst++;
    n--;
  }

	return res;
}

char *strcat(char *dst, const char *src) 
{
  char* p = NULL;
  if(dst == NULL || src == NULL)
    return NULL;

  p = dst;
  while(*dst != '\0')
  {
    dst++;
  }
  while(*src != '\0')
  {
    *dst = *src;
    dst++;
    src++;
  }
  *dst = '\0';
  return p;
}

int strcmp(const char *s1, const char *s2) 
{
  if(s1 == NULL || s2 == NULL)
    return 0;

  while((*s1 != '\0') && (*s1 == *s2))
  {
    s1++; s2++;
  }
  return (*s1 - *s2);
}

int strncmp(const char *s1, const char *s2, size_t n) 
{
  if(s1 == NULL || s2 == NULL || n == 0)
    return 0;

  while((*(s1) != '\0') && (*(s2) != '\0') && (*s1 == *s2) && n > 1)
  {
    s1++;s2++;
    n--;
  }
  return (*s1 - *s2);
}

void *memset(void *s, int c, size_t n) {
  char* p = NULL;
  if(s == NULL)
    return s;
  
  p = (char *)s;
  while(n > 0)
  {
    *(p++) = c;
    n--;
  }
  return s;
}

void *memmove(void *dst, const void *src, size_t n) 
{
  char *p_dst;
  char *p_src;

  if(dst == NULL || src == NULL || n == 0)
  {
  	return NULL;
  }

  if(src < dst) 
  {
    p_dst = (char*)dst + n - 1;
    p_src = (char*)src + n - 1;
    while(n > 0)
    {
      *p_dst = *p_src;
      p_dst--; p_src--;
      n--;
    }
  }
  else if(src > dst) 
  { 
    p_dst = (char*)dst;
    p_src = (char*)src;
    while(n > 0)
    {
      *p_dst = *p_src;
      p_dst++; p_src++;
      n--;
    }
  }
  return dst;
}

void *memcpy(void *out, const void *in, size_t n) 
{
  char *p_out;
  char *p_in;

  if(out == NULL || in == NULL || n == 0)
  {
  	return NULL;
  }

  if((in < out) && (in + n > out)) // 若发生内存重叠并且输入在输出之前，从后往前复制
  {
    p_out = (char*)out + n - 1;
    p_in = (char*)in + n - 1;
    while(n > 0)
    {
      *p_out = *p_in;
      p_out--; p_in--;
      n--;
    }
  }
  else
 { 
    p_out = (char*)out;
    p_in = (char*)in;
    while(n > 0)
    {
      *p_out = *p_in;
      p_out++; p_in++;
      n--;
    }
  }
  return out;
}

int memcmp(const void *s1, const void *s2, size_t n) 
{
  char *p1 = NULL, *p2 = NULL;
  if(s1 == NULL || s2 == NULL || n == 0)
    return 0;

  p1 = (char *)s1;
  p2 = (char *)s2;
  while(n > 1 && *p1 == *p2)
  {
    p1++;p2++;
    n--;
  }
  return (*p1 - *p2);
}

#endif
