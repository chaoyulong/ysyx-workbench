#ifndef __common_h__
#define __common_h__

#include <stdint.h>
#include <inttypes.h>
#include <stdbool.h>
#include <string.h>

#include <assert.h>
#include <stdlib.h>
#include <stdio.h>

#include "macro.h"

typedef uint32_t word_t;
typedef int32_t  sword_t;

typedef word_t vaddr_t;
typedef uint32_t paddr_t;
typedef uint16_t ioaddr_t;

#define __RV32_E__ 1

#endif