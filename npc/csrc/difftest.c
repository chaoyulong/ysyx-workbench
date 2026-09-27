
// ============================== 差分测试(DiffTest) dut 侧胶水 ============================== //
// 移植自 NEMU 的 src/cpu/difftest/dut.c，按本工程(NPC)适配。整体流程与 NEMU 一致：
//   init：    dlopen(ref.so) → 取 5 个符号 → ref_difftest_init → 灌镜像 → 同步复位状态
//   每条指令：ref_difftest_exec(1) → regcpy(TO_DUT) → 与 dut 比对 {gpr, pc}
//   不一致：  置 NPC_ABORT + halt_pc，打印差异(之后 cpu_exec 的 statistic() 会打出 ringbuf)
//
// 与 NEMU 的差异集中在下面 5 点，每一点都是"照抄必错"的坑：
//
// ★(1) pc 的语义 —— 最容易踩，也最致命
//   NEMU 的 difftest_step 被调用时，它的 cpu.pc **已经是执行后的 pc**
//   (NEMU 的 exec_once 里有 cpu.pc = s->dnpc)，所以 NEMU 的 checkregs 直接比 cpu.pc。
//   本工程不一样(见 csrc/cpu-exec.c)：
//       cpu.base.pc = itraceRetirePc       // 【本条】指令的 pc，用于报错/统计
//       cpu.pcNext  = itraceRetirePcNext   // 【下一条】要执行的 pc —— 这才是 ref 意义上的 pc
//   因此有两条铁律：
//     · 比对必须比 pcNext(difftest_step 的第二个参数)；比 cpu.base.pc 会"每条都 diff"；
//     · skip 时回灌给 ref 的 pc 也必须是 pcNext，否则 ref 会退回本条重跑 → 之后全线 diff。
//
// ★(2) 接口结构体只有 {gpr, pc} —— 绝不能用 sizeof(CPU_state)
//   本工程的 CPU_state 是包装过的：{ cpu_base_state_t base; word_t instr; word_t pcNext; Decode decode; }
//   而 difftest 的契约结构体只有第一个成员 cpu_base_state_t(见 include/cpu-exec.h)。
//   NEMU 的 ref.c 里是 memcpy(&cpu, dut, sizeof(cpu))，那个 sizeof 取的是 NEMU 的
//   riscv32_CPU_state = {gpr[32], pc} = 32*4 + 4 = 132 字节(CONFIG_RVE 关闭时)，
//   与本工程 cpu_base_state_t 完全一致。所以：一律只传 &cpu.base。
//   这个契约由两道防线守着：编译期的 static_assert，以及启动时的 difftest_abi_check()。
//
// ★(3) 镜像首址 —— 不能用 guest_to_host(RESET_VECTOR)
//   pmem.h 里 CONFIG_MBASE 恒为 0x80000000(guest_to_host 的基准)，而 ysyxsoc 平台
//   RESET_VECTOR = 0x30000000 → guest_to_host 会算出 flash - 0x50000000(指针下溢)。
//   镜像在 pmem.c 里就是 uint8_t flash[] 这个数组，直接用数组首址，两个平台都对：
//     npc 平台：    flash[0] ↔ 宾客 0x80000000 (NpcMemRW → pmem_read → guest_to_host)
//     ysyxsoc 平台：flash[0] ↔ 宾客 0x30000000 的 0 偏移(SPI flash 模型 flash_read 用 0 基偏移)
//   而两个平台的镜像宾客首址都正好是各自的 RESET_VECTOR，与 NEMU 侧的区域也对得上：
//     npc 0x80000000 → NEMU 的 pmem(MBASE=0x80000000)；ysyxsoc 0x30000000 → NEMU 的 flash_mem。
//
// ★(4) 没传 -d 时容错
//   NEMU 直接 assert(ref_so_file != NULL) 失败。本工程把"没传 -d"当作"关闭 difftest"(打一条警告)，
//   这样 CONFIG_DIFFTEST 可以常开，而 make perf / 普通仿真不会被带崩；
//   difftest_step / difftest_skip_ref 内部自己判断开关，调用方可以无脑调用。
//
// ★(5) 删掉 QEMU"指令打包"那一套(skip_dut_nr_inst / difftest_skip_dut)
//   理由见 include/difftest.h 第 4 条：本工程的 ref 是 NEMU 解释器，一次 exec(1) 恒等于一条指令。
//
// 两个提醒(C 侧看不到，但会影响结果)：
//   · 本工程的 .c 是用 g++ 编译的(verilator 生成的 Makefile 里是 $(CXX) -c difftest.c)，
//     所以 dlsym 返回的 void* 必须显式转成函数指针，否则 C++ 报 invalid conversion；
//   · NEMU 的 init_mem() 在 CONFIG_MEM_RANDOM=y 时会用 rand() 填满 pmem，而本工程 flash[] 是
//     零初始化。镜像之外的未初始化内存两边初值不同 → 程序若读了未初始化内存会报假 diff。
//     建议在 NEMU 的 menuconfig 里关掉 MEM_RANDOM，与 dut 的零初值内存模型对齐。

#include <dlfcn.h>
#include <stddef.h>

#include "common.h"
#include "config.h"
#include "cpu-exec.h"
#include "difftest.h"
#include "log.h"
#include "pmem.h"
#include "regfile.h"

// ------------------------------- ref(.so)侧符号(声明见 include/difftest.h) ------------------------------- //
void (*ref_difftest_memcpy)(paddr_t addr, void *buf, size_t n, bool direction) = NULL;
void (*ref_difftest_regcpy)(void *dut, bool direction) = NULL;
void (*ref_difftest_exec)(uint64_t n) = NULL;
void (*ref_difftest_raise_intr)(uint64_t NO) = NULL;

// 镜像本体(定义在 pmem.c)。不能用 guest_to_host() 取，原因见文件头 ★(3)
extern uint8_t flash[];

// 寄存器名字表(定义在 regfile.c)，差异报告里打印寄存器名用
extern const char *regs[];

// ------------------------------- 接口契约(编译期) ------------------------------- //
// 本工程的 cpu_base_state_t 必须与 NEMU 的 riscv32_CPU_state(CONFIG_RVE 关闭)等大：
//   32 个通用寄存器(128 字节) + pc(4 字节) = 132 字节，pc 在偏移 128。
// 若有人打开 NEMU 的 CONFIG_RVE(会变成 16 个寄存器 / 68 字节 / pc 在偏移 64)，而这里没跟着改，
// 编译期就会报错；若只重编了 .so 没动这里，则由启动时的 difftest_abi_check() 兜住。
static_assert(sizeof(cpu_base_state_t) == 132,
              "cpu_base_state_t 必须与 NEMU 的 riscv32_CPU_state(32 个寄存器)等大");
static_assert(offsetof(cpu_base_state_t, pc) == 128,
              "pc 必须紧跟在 32 个通用寄存器之后");

#ifdef CONFIG_DIFFTEST

static bool difftest_enabled = false;   // init_difftest 成功后才置 true
static bool is_skip_ref = false;        // 下一条 ref 不执行，直接把 dut 的状态抄过去

// ------------------------------------------------------------------------------------------------ //
// 接口自检：量一下 ref 的 {gpr, pc} 到底是多少字节
//   给 ref 一个 256 字节的缓冲区让它往里写；sizeof(cpu_base_state_t) 之后的部分应该原封不动
//   (还是 0xCC 哨兵)。这样"68 字节的 .so 配 132 字节的 DUT"(或反过来)会在启动时立刻报错，
//   而不是跑几百条指令之后给出一个"看起来像 CPU 跳飞了"的假 diff。
//   注意：TO_DUT 方向是**只读** ref 的状态，不会污染 ref，所以这个自检是安全的。
// ------------------------------------------------------------------------------------------------ //
static void difftest_abi_check(void) {
  uint8_t probe[256];
  memset(probe, 0xCC, sizeof(probe));
  ref_difftest_regcpy(probe, DIFFTEST_TO_DUT);
  for (int i = (int)sizeof(cpu_base_state_t); i < (int)sizeof(probe); i++) {
    Assert(probe[i] == 0xCC,
           "difftest 接口不匹配：ref 写入了超过 %d 字节(第 %d 字节被改写)。"
           "请检查 NEMU 的 CONFIG_RVE 是否被打开(打开会变成 16 个寄存器/68 字节)",
           (int)sizeof(cpu_base_state_t), i + 1);
  }
  Log("difftest: 接口自检通过(%d 字节 = %d 个通用寄存器 + pc)",
      (int)sizeof(cpu_base_state_t), REG_NUM);
}

void init_difftest(char *ref_so_file, long img_size, int port) {
  // ★(4) 没传 -d：当作关闭 difftest(NEMU 在这里是 assert 失败)
  if (ref_so_file == NULL) {
    Log("difftest: 未指定参考实现(-d <ref.so>)，差分测试关闭");
    return;
  }

  void *handle = dlopen(ref_so_file, RTLD_LAZY);
  Assert(handle != NULL, "difftest: dlopen(%s) 失败: %s", ref_so_file, dlerror());

  // 本工程的 .c 由 g++ 编译，dlsym 的 void* 必须显式转型
  ref_difftest_memcpy      = (void (*)(paddr_t, void *, size_t, bool)) dlsym(handle, "difftest_memcpy");
  ref_difftest_regcpy      = (void (*)(void *, bool))                  dlsym(handle, "difftest_regcpy");
  ref_difftest_exec        = (void (*)(uint64_t))                      dlsym(handle, "difftest_exec");
  // 本工程不注入中断(ref 侧 difftest_raise_intr 是 assert(0))，但要求符号存在，顺便校验 .so 完整
  ref_difftest_raise_intr  = (void (*)(uint64_t))                      dlsym(handle, "difftest_raise_intr");
  void (*ref_difftest_init)(int) = (void (*)(int))                     dlsym(handle, "difftest_init");

  Assert(ref_difftest_memcpy && ref_difftest_regcpy && ref_difftest_exec &&
         ref_difftest_raise_intr && ref_difftest_init,
         "difftest: %s 中缺少 difftest_* 符号(是否用 make SHARE=1 编译了 NEMU?)", ref_so_file);

  Log("Differential testing: %s", ANSI_FMT("ON", ANSI_FG_GREEN));
  Log("The result of every instruction will be compared with %s. ", ref_so_file);
  Log("This will help you a lot for debugging, but also significantly reduce the performance.");

  ref_difftest_init(port);   // ref 侧：init_mem() + init_isa()

  difftest_abi_check();      // 先确认接口一致，再灌镜像/同步状态

  // 灌镜像：宾客首址 = 本工程的 RESET_VECTOR(npc 0x80000000 / ysyxsoc 0x30000000)，
  // host 指针直接用 flash[] 数组首址(见文件头 ★(3))
  ref_difftest_memcpy(RESET_VECTOR, flash, img_size, DIFFTEST_TO_REF);

  // 同步复位状态：{gpr 全 0, pc = RESET_VECTOR}
  // 这是全流程里唯一一次直接把 pc 取成 RESET_VECTOR 的地方 —— 因为复位后"第一条要执行的 pc"
  // 就是它；其余所有回灌/比对用的都是 difftest_step 的 pcNext 参数(见文件头 ★(1))。
  cpu_base_state_t reset_state;
  memset(&reset_state, 0, sizeof(reset_state));
  reset_state.pc = RESET_VECTOR;
  ref_difftest_regcpy(&reset_state, DIFFTEST_TO_REF);

  difftest_enabled = true;
}

// 标记"这一条 ref 不要执行"：ref 只被回灌寄存器，不执行这条指令。
// 本工程的用途：设备访存(串口/键盘/CLINT/VGA)、CSR 指令、ecall/ebreak/mret 等
//   —— 这些指令在 ref(NEMU) 那边要么会 panic(设备地址 out_of_bound、未知 CSR 地址)，
//      要么两边约定不同(ecall 的 mcause：dut 走 a5，NEMU 走 a7)。
// ⚠ 只能用于"没有内存副作用"的指令：回灌只同步 {gpr, pc}，**不同步内存**。
//   所以"设备访问"的判据必须是"ref 那边不当作内存的地址"，不能把真的内存(如 SRAM)算进去，
//   否则被跳过的那次写不会反映到 ref 的内存里，之后 ref 一读就是假 diff。
void difftest_skip_ref(void) {
  if (!difftest_enabled) return;
  is_skip_ref = true;
}

// 差异报告：把 dut 与 ref 逐寄存器并排打出来
// (比 NEMU 只打 dut 一侧的 isa_reg_display 直观得多)
// 用 Log_nohead 是为了表格行对齐 —— Log 每行都会带 [文件:行 函数] 前缀
static void difftest_reg_display(cpu_base_state_t *ref, vaddr_t pc, vaddr_t pcNext) {
  Log(ANSI_FMT("difftest: 寄存器不一致", ANSI_FG_RED));
  Log_nohead("  出错指令: pc = " FMT_WORD ", instr = " FMT_WORD, pc, cpu.instr);
  Log_nohead("           reg            dut          ref");
  for (int i = 0; i < REG_NUM; i++) {
    bool differ = (cpu.base.gpr[i] != ref->gpr[i]);
    Log_nohead("-- %-2d -- %-3s    0x%08x   0x%08x  %s",
               i, regs[i], cpu.base.gpr[i], ref->gpr[i], differ ? "<-- diff" : "");
  }
  Log_nohead("-- %-2d -- %-3s    0x%08x   0x%08x  %s",
             REG_NUM, "pc", pcNext, ref->pc, (pcNext != ref->pc) ? "<-- diff" : "");
  Log_nohead("(pc 列比的是[执行后]的 pc: dut 用 itraceRetirePcNext, ref 用执行完一条后的 pc)");
}

// 比对：返回是否一致。pcNext 是【执行后】的 pc(见文件头 ★(1))
static bool difftest_checkregs(cpu_base_state_t *ref, vaddr_t pc, vaddr_t pcNext) {
  bool ok = true;
  // 只比 16 个：本工程 REG_NUM=16(RV32E)，gpr[16..31] 两边恒为 0，比了也没有意义
  for (int i = 0; i < REG_NUM; i++) {
    if (cpu.base.gpr[i] != ref->gpr[i]) ok = false;
  }
  if (pcNext != ref->pc) ok = false;
  if (!ok) difftest_reg_display(ref, pc, pcNext);
  return ok;
}

// 每条退休指令调用一次。
//   pc     = 本条指令的 pc(出错时报这个)
//   pcNext = 下一条要执行的 pc(本工程 = itraceRetirePcNext，对应 NEMU 里的第二个参数 npc)
// 调用点见 csrc/cpu-exec.c 的 trace_and_difftest：
//   difftest_step(cpu.base.pc, cpu.pcNext);
void difftest_step(vaddr_t pc, vaddr_t pcNext) {
  if (!difftest_enabled) return;   // 没开 difftest 时静默返回，调用方不必额外判断

  if (is_skip_ref) {
    // 这条 ref 不执行：把 dut 的状态整体抄给 ref，两边从"下一条"重新对齐。
    // ★ pc 必须写成 pcNext：若直接传 &cpu.base，ref 的 pc 会退回本条，
    //   下一轮 ref 就会重跑同一条指令 → 之后每条都 diff(现象是"从某条开始全错")。
    cpu_base_state_t sync = cpu.base;
    sync.pc = pcNext;
    ref_difftest_regcpy(&sync, DIFFTEST_TO_REF);
    is_skip_ref = false;
    return;
  }

  ref_difftest_exec(1);            // ref 执行一条

  cpu_base_state_t ref_r;          // ★ 是 cpu_base_state_t，不是 CPU_state(后者是包装过的)
  ref_difftest_regcpy(&ref_r, DIFFTEST_TO_DUT);

  if (!difftest_checkregs(&ref_r, pc, pcNext)) {
    npc_state.state   = NPC_ABORT; // 走本工程的中止流程：打统计 + ringbuf，退出码非 0
    npc_state.halt_pc = pc;
  }
}

#else  // ============================ 关闭 CONFIG_DIFFTEST：空实现 ============================ //
// 头文件统一声明真函数，空实现放在这里(不放头里的 static inline，原因见 include/difftest.h)
void init_difftest(char *ref_so_file, long img_size, int port) { }
void difftest_skip_ref(void) { }
void difftest_step(vaddr_t pc, vaddr_t pcNext) { }

#endif
