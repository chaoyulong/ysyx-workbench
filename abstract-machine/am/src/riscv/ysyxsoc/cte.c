#include <am.h>
#include <riscv/riscv.h>
#include <klib.h>

static Context* (*user_handler)(Event, Context*) = NULL;

Context* __am_irq_handle(Context *c) {
  if (user_handler) {
    Event ev = {0};
    switch (c->mcause) {
      case 8:                     // ecall(U mode)，当前未用到
      case 9:                     // ecall(S mode)，当前未用到
      case 11: {                  // ecall(M mode)
        ev.event = (c->GPR1 == (uintptr_t)-1) ? EVENT_YIELD : EVENT_SYSCALL;  // yield()函数会进入EVENT_YIELD，其余进入EVENT_SYSCALL
        c->mepc += 4;
        break;
      }
      case 3: {
        ev.event = EVENT_SYSCALL; c->mepc += 4; break;
      }
      
      default: ev.event = EVENT_ERROR; break;
    }

    c = user_handler(ev, c);
    assert(c != NULL);
  }

  return c;
}

extern void __am_asm_trap(void);

bool cte_init(Context*(*handler)(Event, Context*)) {
  // initialize exception entry
  asm volatile("csrw mtvec, %0" : : "r"(__am_asm_trap));

  // register event handler
  user_handler = handler;

  return true;
}

Context *kcontext(Area kstack, void (*entry)(void *), void *arg) {
  Context *tmp = (Context *)(kstack.end - sizeof(Context));
  tmp->mstatus = 0x1800;
  tmp->mepc = (uintptr_t)entry;
  *(uintptr_t *)(tmp->gpr + 10) = (uintptr_t )arg;
  return tmp;
}

void yield() {
#ifdef __riscv_e
  asm volatile("li a5, -1; ecall");
#else
  asm volatile("li a7, -1; ecall");
#endif
}

bool ienabled() {
  return false;
}

void iset(bool enable) {
}
