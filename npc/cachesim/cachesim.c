// ============================================================================
// cachesim.c —— icache 功能模拟器 (cache 设计空间探索 / DSE 用)
//
// 【原理】
//   对于给定的访存地址序列, cache 的缺失次数与访存内容无关, 只与地址有关。
//   因此模拟器只需要维护 cache 的【元数据】(valid / tag / 替换信息),
//   不需要模拟数据内容, 也不需要执行指令 —— 直接回放取指 PC 序列即可。
//
//   这与 RTL 仿真相比快几千倍, 可以用来快速扫描 cache 参数组合。
//
// 【输入】PC 序列(文本, 每行一条退休指令的取指 PC), 支持两种格式:
//   (1) NPC itrace:  "0x00000000: <disasm>"   -> 取冒号前的 PC, 并从 disasm 识别 fence.i
//   (2) 纯 PC 文本:  "0x00000000" 或 "305419896"
//   "-" 表示从标准输入读, 便于配合压缩:
//       bzcat trace.bz2 | ./cachesim - --lines 16
//
//   PC 序列的来源可以是任何一种, 本程序不关心也不需要它是什么:
//     - NPC 仿真的 itrace: 仿真命令加 -l <file> 即输出到该文件
//       (前提是在 include/config.h 里打开 CONFIG_TRACE)
//     - NEMU 生成的 itrace (文档推荐, 比 NPC 快得多)
//     - 任何你自己造出来的 PC 序列(比如脚本生成的)
//
// 【独立性】
//   本程序是**完全独立的 C 程序**, 只依赖 libc。
//   与 NPC / AM / ysyxsoc.mk / Verilator 都没有任何耦合 —— 上面提到它们,
//   仅仅是为了说明"PC 序列可以从哪里得到", 而不是运行本程序的前提。
//
// 【输出】
//   一行 RESULT: 便于脚本扫描参数组合, 后面是可读的明细。
//
// 【编译】
//   gcc -O2 -o build/cachesim tools/cachesim.c
//
// 【用法示例】
//   ./build/cachesim trace.log --lines 16                # 直接映射 16 块 x 4B(与当前 RTL 一致)
//   ./build/cachesim trace.log --lines 16 --block 16     # 16 块 x 16B
//   ./build/cachesim trace.log --lines 32 --ways 2       # 32 块, 2 路组相联
//   ./build/cachesim trace.log --lines 16 --misscost 31.79   # 顺便算 TMT
//   ./build/cachesim trace.log --from 0xa0000000         # 只统计 SDRAM 阶段(排除 boot)
// ============================================================================

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

// ------------------------------- 参数 ------------------------------- //
static uint32_t cfg_lines     = 16;        // cache 总块数(与 RTL 的 IcacheParams.lines 对应)
static uint32_t cfg_block     = 4;         // 块大小(字节, 与 lineBytes 对应)
static uint32_t cfg_ways      = 1;         // 相联度(1 = 直接映射)
static const char *cfg_repl   = "lru";     // 替换算法: lru / fifo / rand (仅 ways>1 有意义)
static double   cfg_misscost  = -1;        // 平均缺失代价(cyc), 给了就算 TMT
static uint32_t cfg_from      = 0;         // 只统计 pc >= cfg_from 的访问(排除 boot 阶段)
static uint64_t cfg_skip      = 0;         // 跳过前 N 条(调试用)
static uint64_t cfg_max       = (uint64_t)-1; // 最多处理多少条

// ------------------------------- 统计 ------------------------------- //
static uint64_t n_access = 0;   // 计入统计的访问次数
static uint64_t n_miss   = 0;   // 缺失次数
static uint64_t n_fence  = 0;   // fence.i 次数
static uint64_t n_line   = 0;   // 读入的总行数(含被过滤的)

// ------------------------------- cache 元数据 ------------------------------- //
static uint32_t *tag;      // 每个块的 tag
static uint8_t  *valid;    // 每个块的 valid
static uint64_t *stamp;    // LRU/FIFO 用的时间戳
static uint64_t  g_clk = 0;

enum { REPL_LRU, REPL_FIFO, REPL_RAND };

static void die(const char *msg) {
  fprintf(stderr, "cachesim: %s\n", msg);
  exit(1);
}

static void usage(const char *prog) {
  printf("用法: %s <trace文件|-> [选项]\n\n", prog);
  printf("  --lines  N      cache 总块数 (默认 16)\n");
  printf("  --block  B      块大小(字节) (默认 4)\n");
  printf("  --ways   W      相联度, 1=直接映射 (默认 1)\n");
  printf("  --repl   P      替换算法 lru|fifo|rand (默认 lru)\n");
  printf("  --misscost C    平均缺失代价(cyc), 给出则计算 TMT\n");
  printf("  --from   ADDR   只统计 pc >= ADDR 的访问(排除 boot 阶段, 支持 0x 前缀)\n");
  printf("  --skip   N      跳过前 N 行\n");
  printf("  --max    N      最多处理 N 行\n");
  printf("  -h, --help      显示本帮助\n");
}

int main(int argc, char **argv) {
  const char *path = NULL;

  // ------------------------------ 解析参数 ------------------------------ //
  for (int i = 1; i < argc; i++) {
    const char *a = argv[i];
    if (!strcmp(a, "-h") || !strcmp(a, "--help")) { usage(argv[0]); return 0; }
    else if (!strcmp(a, "--lines")    && i + 1 < argc) cfg_lines    = (uint32_t)strtoul(argv[++i], NULL, 0);
    else if (!strcmp(a, "--block")    && i + 1 < argc) cfg_block    = (uint32_t)strtoul(argv[++i], NULL, 0);
    else if (!strcmp(a, "--ways")     && i + 1 < argc) cfg_ways     = (uint32_t)strtoul(argv[++i], NULL, 0);
    else if (!strcmp(a, "--repl")     && i + 1 < argc) cfg_repl     = argv[++i];
    else if (!strcmp(a, "--misscost") && i + 1 < argc) cfg_misscost = strtod(argv[++i], NULL);
    else if (!strcmp(a, "--from")     && i + 1 < argc) cfg_from     = (uint32_t)strtoul(argv[++i], NULL, 0);
    else if (!strcmp(a, "--skip")     && i + 1 < argc) cfg_skip     = strtoull(argv[++i], NULL, 0);
    else if (!strcmp(a, "--max")      && i + 1 < argc) cfg_max      = strtoull(argv[++i], NULL, 0);
    else if (a[0] == '-' && a[1] != '\0' && strcmp(a, "-")) die("未知选项(见 --help)");
    else if (path == NULL) path = a;
    else die("只能指定一个 trace 文件");
  }
  if (path == NULL) { usage(argv[0]); return 1; }

  // ------------------------------ 参数检查与推导 ------------------------------ //
  if (cfg_lines == 0 || (cfg_lines & (cfg_lines - 1))) die("--lines 必须是 2 的幂");
  if (cfg_block == 0 || (cfg_block & (cfg_block - 1))) die("--block 必须是 2 的幂");
  if (cfg_ways == 0 || (cfg_ways & (cfg_ways - 1)))    die("--ways 必须是 2 的幂");
  if (cfg_ways > cfg_lines)                            die("--ways 不能大于 --lines");

  uint32_t sets = cfg_lines / cfg_ways;
  // 位宽推导必须与 RTL 一致: indexBits = log2Up(lines/ways), blockBits = log2Up(blockBytes)
  int blockBits = 0; while ((1u << blockBits) < cfg_block) blockBits++;
  int indexBits = 0; while ((1u << indexBits) < sets)      indexBits++;

  int repl;
  if      (!strcmp(cfg_repl, "lru"))  repl = REPL_LRU;
  else if (!strcmp(cfg_repl, "fifo")) repl = REPL_FIFO;
  else if (!strcmp(cfg_repl, "rand")) repl = REPL_RAND;
  else die("--repl 只能是 lru|fifo|rand");
  if (repl == REPL_RAND) srand(12345);   // 固定种子, 保证可复现

  tag   = calloc(cfg_lines, sizeof(uint32_t));
  valid = calloc(cfg_lines, 1);
  stamp = calloc(cfg_lines, sizeof(uint64_t));
  if (!tag || !valid || !stamp) die("内存分配失败");

  // ------------------------------ 打开输入 ------------------------------ //
  FILE *fp;
  if (!strcmp(path, "-")) fp = stdin;
  else { fp = fopen(path, "r"); if (!fp) { perror(path); return 1; } }

  printf("cachesim: lines=%u sets=%u ways=%u block=%uB repl=%s"
         " (indexBits=%d blockBits=%d tagBits=%d)\n",
         cfg_lines, sets, cfg_ways, cfg_block, cfg_repl,
         indexBits, blockBits, 32 - indexBits - blockBits);
  if (cfg_from) printf("          只统计 pc >= 0x%08x\n", cfg_from);

  // ------------------------------ 回放 PC 序列 ------------------------------ //
  // 支持两种输入格式, 自动识别:
  //   (a) NPC itrace: 行首必须是 "0x<hex>:"。要求"行首", 是为了排除普通日志行,
  //       以及程序结束时回显的 ringbuf(它以空格或 "-->" 开头, 内容是重复指令)。
  //   (b) 纯 PC 文本: 整行只有一个数字(十六进制或十进制)。
  // 其余行一律跳过。
  char line[512];
  while (fgets(line, sizeof(line), fp)) {
    uint32_t pc = 0;
    int is_itrace = 0;

    if (line[0] == '0' && (line[1] == 'x' || line[1] == 'X')) {
      // (a) itrace: "0x00000000: <bytes> <disasm>"
      char *end;
      unsigned long v = strtoul(line, &end, 16);
      if (end <= line + 2) continue;
      char *t = end;
      while (*t == ' ' || *t == '\t') t++;
      pc = (uint32_t)v;
      is_itrace = (*t == ':');
    } else {
      // (b) 纯 PC 文本: 允许前导空白, 但数字之后不能有别的内容
      char *s = line;
      while (*s == ' ' || *s == '\t') s++;
      if (*s < '0' || *s > '9') continue;              // 日志行/空行 -> 跳过
      char *end;
      unsigned long v = strtoul(s, &end, 0);
      if (end == s) continue;
      char *t = end;
      while (*t == ' ' || *t == '\t' || *t == '\n' || *t == '\r') t++;
      if (*t != '\0') continue;                        // ringbuf 行(数字后还有内容) -> 跳过
      pc = (uint32_t)v;
    }
    n_line++;

    if (n_line <= cfg_skip) continue;
    if (n_access >= cfg_max) break;

    // fence.i: 清空全部有效位(与 RTL 的 when(io.fenceI){ validReg := 0 } 一致)
    // 只有 itrace 行带反汇编才能识别; 纯 PC 文本无此信息
    if (is_itrace && strstr(line, "fence.i")) {
      memset(valid, 0, cfg_lines);
      n_fence++;
    }

    if (pc < cfg_from) continue;   // 排除 boot 阶段的地址区间

    // ------------------------------ 访问 cache ------------------------------ //
    n_access++;
    uint32_t block = pc >> blockBits;
    uint32_t set   = block & (sets - 1);
    uint32_t t     = block >> indexBits;
    uint32_t base  = set * cfg_ways;

    int hitWay = -1;
    for (uint32_t w = 0; w < cfg_ways; w++)
      if (valid[base + w] && tag[base + w] == t) { hitWay = (int)w; break; }

    if (hitWay >= 0) {
      if (repl == REPL_LRU) stamp[base + hitWay] = ++g_clk;   // LRU: 命中也要更新时间
    } else {
      n_miss++;
      int victim = -1;
      for (uint32_t w = 0; w < cfg_ways; w++)                 // 优先填无效块
        if (!valid[base + w]) { victim = (int)(base + w); break; }
      if (victim < 0) {                                       // 全有效 -> 按替换算法
        if (repl == REPL_RAND) victim = (int)(base + rand() % cfg_ways);
        else {
          victim = (int)base;
          for (uint32_t w = 1; w < cfg_ways; w++)
            if (stamp[base + w] < stamp[victim]) victim = (int)(base + w);
        }
      }
      valid[victim] = 1;
      tag[victim]   = t;
      stamp[victim] = ++g_clk;                                // FIFO 同样只在这里更新
    }
  }
  if (fp != stdin) fclose(fp);

  // ------------------------------ 输出 ------------------------------ //
  double missRate = n_access ? 100.0 * (double)n_miss / (double)n_access : 0.0;
  double hitRate  = 100.0 - missRate;
  double tmt      = (cfg_misscost >= 0) ? (double)n_miss * cfg_misscost : -1;

  // 单行结果, 便于脚本扫描参数组合
  printf("RESULT: lines=%-6u ways=%-2u block=%-4u repl=%-4s "
         "access=%-10llu miss=%-10llu hit=%6.2f%%%s\n",
         cfg_lines, cfg_ways, cfg_block, cfg_repl,
         (unsigned long long)n_access, (unsigned long long)n_miss, hitRate,
         cfg_misscost >= 0 ? "" : "");
  printf("  ========================================================================\n");
  printf("  %-17s %llu\n",   "访问次数:",   (unsigned long long)n_access);
  printf("  %-17s %llu\n",   "缺失次数:",   (unsigned long long)n_miss);
  printf("  %-17s %.2f%%\n", "命中率:",     hitRate);
  printf("  %-17s %llu  (读入 %llu 行)\n", "fence.i 次数:", (unsigned long long)n_fence,
         (unsigned long long)n_line);
  if (cfg_misscost >= 0) {
    printf("  %-17s %.2f cyc/次 (由 --misscost 给定)\n", "平均缺失代价:", cfg_misscost);
    printf("  %-17s %.0f cyc\n", "TMT(总缺失时间):", tmt);
  }
  printf("  ========================================================================\n");
  return 0;
}
