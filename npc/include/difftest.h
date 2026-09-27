#ifndef __difftest_h__
#define __difftest_h__

// ============================== 差分测试(DiffTest)对外接口 ============================== //
// 移植自 NEMU 的 include/cpu/difftest.h。为适配本工程(NPC)做了 4 处裁剪/补充，每一处都是
// "直接照抄会出问题"的地方：
//
// 1) **不搬** NEMU 的 difftest-def.h
//    它在 nemu/include/ 下，而且依赖 NEMU 自己的 generated/autoconf.h(本工程没有这个文件)。
//    本工程真正要从它那里拿的只有"搬运方向"这一个枚举，就地定义即可。
//    ★ DIFFTEST_TO_DUT / DIFFTEST_TO_REF 的**数值**必须与 NEMU 保持一致(0 / 1)，
//      改了会让 TO_REF/TO_DUT 走反方向(ref 被写坏)，而且不会有任何编译错误。
//
// 2) 补一个 FMT_WORD
//    NEMU 的 include/macro.h 里有 FMT_WORD，本工程的 macro.h 只有 MUXDEF/IFDEF。
//    common.h 里 word_t 恒为 uint32_t，所以固定按 8 位十六进制打印。
//
// 3) 关闭开关时的空实现**不放在头文件里**
//    NEMU 那份用 #ifdef CONFIG_DIFFTEST 在头里生成 static inline 空实现，本工程不能这么做：
//      difftest.c 先 #include "difftest.h"(此时宏还没定义 → 头里生成了空实现)，
//      下面再写真正的函数定义 → 触发 "redefinition" 编译错误。
//    所以本工程的做法是：头文件**无条件**声明真函数，空实现放在 difftest.c 的 #else 分支里。
//    开关统一放在 include/config.h 的 CONFIG_DIFFTEST。
//
// 4) 删掉本工程用不到的函数
//    · difftest_set_patch / difftest_detach / difftest_attach：NEMU 给 QEMU ref 用的，本工程不需要；
//    · difftest_skip_dut：它的语义是"让 ref 先跑 nr_ref 条，等 DUT 在 nr_dut 条内追平"，
//      专治 QEMU 一次执行多条指令(instruction packing)。本工程的 ref 是 NEMU 解释器，
//      一次 ref_difftest_exec(1) 恒等于一条指令，那套追赶逻辑永远走不到；
//      留着还会引入本工程没有的 panic()(见 log.h：panic 是被注释掉的)。
//    · difftest_check_reg：NEMU 里给它的 isa difftest 用的打印工具，本工程的差异报告
//      在 difftest.c 里做成了"dut/ref 并排表格"，比单个寄存器逐条打印更好用。

#include "common.h"

// 搬运方向：数值必须与 NEMU 的 include/difftest-def.h 一致
enum { DIFFTEST_TO_DUT, DIFFTEST_TO_REF };

#ifndef FMT_WORD
#define FMT_WORD "0x%08x"          // 本工程 word_t 恒为 32 位(见 common.h)
#endif

// ------------------------------- dut(NPC)侧接口 ------------------------------- //
// 三者都可以无条件调用：没开 CONFIG_DIFFTEST、或没传 -d 时，内部会静默返回。
void init_difftest(char *ref_so_file, long img_size, int port);  // 初始化：连 ref + 灌镜像 + 同步复位状态
void difftest_skip_ref(void);                                   // 标记"下一条 ref 不要执行，直接回灌 dut 状态"
void difftest_step(vaddr_t pc, vaddr_t pcNext);                 // 每条退休指令调用一次

// ------------------------------- ref(.so)侧符号 ------------------------------- //
// 由 init_difftest 用 dlsym 填好，实现在 difftest.c
extern void (*ref_difftest_memcpy)(paddr_t addr, void *buf, size_t n, bool direction);
extern void (*ref_difftest_regcpy)(void *dut, bool direction);
extern void (*ref_difftest_exec)(uint64_t n);
extern void (*ref_difftest_raise_intr)(uint64_t NO);

#endif
