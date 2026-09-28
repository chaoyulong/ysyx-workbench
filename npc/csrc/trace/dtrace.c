// ============================== dtrace: LSU 数据访存 trace(dcache 请求侧) ============================== //
// 由 RTL 里的 LsuTrace 黑盒(playground/vsrc/dpi-c.v)在【CPU 发出的每一次数据访存】时 DPI-C 调用。
//
// 为什么需要它: 现有的 mtrace 截的是 dcache【之后】的 AXI 事务 —— 只有 86% 的缺失流,
// 看不到命中的那 14%, 所以**不能**用来评估"换一个 dcache 组织(行大小/项数/相联度)会怎样"。
// 做 dcache 的 DSE 需要的是 CPU 侧的完整访问流(命中+缺失都记) —— 这就是本文件记录的序列。
//
// 开关: 环境变量 NPC_DTRACE=<文件路径>; 不设置时本函数立即返回(对仿真零影响)。
// 用法: NPC_DTRACE=build/dse-backup/dtrace.log make perf
// 格式: 每行 "<addr> <wen> <size>"  (地址十六进制; wen=1 为写; size 为 memOp 低 2 位: 0=B/1=H/2=W)
//
// 实现说明: 本工程的 .c 是用 g++ 编译的(verilator 生成的 Makefile 里是 $(CXX)),
//           而 DPI-C 导入的是 C 符号, 所以这里要显式 extern "C"。

#include "common.h"
#include "log.h"

static FILE *dtrace_fp = NULL;
static bool dtrace_checked = false;

// 打开 trace 文件(只在第一次访存时做)。
// ⚠ 必须放在 lsu_trace【外面】: lsu_trace 是 extern "C", 若在它内部写 Log/Assert,
//   宏里声明的 assert_fail_msg/log_enable 会被当成 C 链接, 而本工程里它们是 C++ 链接的
//   (csrc 全部由 g++ 编译) -> 链接期 undefined reference。
static void dtrace_open_once(void) {
  dtrace_checked = true;
  const char *path = getenv("NPC_DTRACE");
  if (path != NULL && path[0] != '\0') {
    dtrace_fp = fopen(path, "w");
    Assert(dtrace_fp != NULL, "dtrace: 打不开 %s", path);
    Log("dtrace: 数据访存 trace(dcache 请求侧) 正在写入 %s", path);
  }
}

extern "C" void lsu_trace(int addr, int wen, int size) {
  if (!dtrace_checked) dtrace_open_once();   // 避免每次访存都查环境变量
  if (dtrace_fp == NULL) return;
  fprintf(dtrace_fp, "%08x %d %d\n", (uint32_t)addr, wen, size);
  // 不在这里 fflush/关文件: 进程正常退出时 stdio 会统一刷盘(仿真结束走的是 main 的正常返回)
}
