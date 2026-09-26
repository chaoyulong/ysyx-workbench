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
NPCFLAGS += -l $(shell dirname $(IMAGE).elf)/npc-log.txt
NPCFLAGS += -f $(IMAGE).elf

SDB_VAL := $(SDB) $(sdb)
ifneq ($(filter N n, $(SDB_VAL)),)
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
