AM_SRCS := riscv/npc/start.S \
           riscv/npc/trm.c \
           riscv/npc/ioe.c \
           riscv/npc/timer.c \
           riscv/npc/keyboard.c \
           riscv/npc/gpu.c \
           riscv/npc/cte.c \
           riscv/npc/trap.S \
           platform/dummy/vme.c \
           platform/dummy/mpe.c

CFLAGS    += -fdata-sections -ffunction-sections
LDSCRIPTS += $(AM_HOME)/scripts/linker.ld
LDFLAGS   += --defsym=_pmem_start=0x80000000 --defsym=_entry_offset=0x0
LDFLAGS   += --gc-sections -e _start
NPCFLAGS  += -l $(shell dirname $(IMAGE).elf)/npc-log.txt
NPCFLAGS  += -f $(IMAGE).elf
# difftest 参考 so 是本地产物(不在仓库里): 没有它时不传 -d, 由 npc 侧自动关闭 difftest
DIFFTEST_SO = $(NPC_HOME)/tools/difftest/riscv32-nemu-interpreter-so
NPCFLAGS += $(if $(wildcard $(DIFFTEST_SO)),-d $(DIFFTEST_SO),)

# 默认不进入 sdb 交互模式(CI/管道没有终端, 会卡在等 stdin);
# 想交互调试: 命令行传 sdb=y(命令行变量优先级最高, 会覆盖这里)
sdb = n
SDB_VAL := $(SDB) $(sdb)
# 默认批处理(-b): 没有终端时(CI / 管道)不会卡在 sdb 交互模式等 stdin;
# 需要交互调试时显式传 sdb=y(或 make ... SDB=y)
ifeq ($(filter Y y, $(SDB_VAL)),)
  NPCFLAGS += -b
endif

MAINARGS_MAX_LEN = 64
MAINARGS_PLACEHOLDER = the_insert-arg_rule_in_Makefile_will_insert_mainargs_here
CFLAGS += -DMAINARGS_MAX_LEN=$(MAINARGS_MAX_LEN) -DMAINARGS_PLACEHOLDER=$(MAINARGS_PLACEHOLDER)

insert-arg: image
	@python $(AM_HOME)/tools/insert-arg.py $(IMAGE).bin $(MAINARGS_MAX_LEN) $(MAINARGS_PLACEHOLDER) "$(mainargs)"

image: image-dep
	@$(OBJDUMP) -d $(IMAGE).elf > $(IMAGE).txt
	@echo + OBJCOPY "->" $(IMAGE_REL).bin
	@$(OBJCOPY) -S --set-section-flags .bss=alloc,contents -O binary $(IMAGE).elf $(IMAGE).bin

# npc没办法使用nvboard,所以run和sim一样
run: insert-arg
	$(MAKE) -C $(NPC_HOME) ISA=$(ISA) sim ARGS="$(NPCFLAGS)" IMG=$(IMAGE).bin PARTFORM=npc

sim: insert-arg
	$(MAKE) -C $(NPC_HOME) ISA=$(ISA) sim ARGS="$(NPCFLAGS)" IMG=$(IMAGE).bin PARTFORM=npc

wave: insert-arg
	$(MAKE) -C $(NPC_HOME) ISA=$(ISA) wave ARGS="$(NPCFLAGS)" IMG=$(IMAGE).bin PARTFORM=npc

.PHONY: insert-arg
