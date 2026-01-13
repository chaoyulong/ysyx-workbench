AM_SRCS := riscv/ysyxsoc/start.S \
           riscv/ysyxsoc/trm.c \
           riscv/ysyxsoc/ioe.c \
           riscv/ysyxsoc/uart.c \
           riscv/ysyxsoc/timer.c \
           riscv/ysyxsoc/keybord.c \
           riscv/ysyxsoc/cte.c \
           riscv/ysyxsoc/trap.S \
           platform/dummy/vme.c \
           platform/dummy/mpe.c

CFLAGS    += -fdata-sections -ffunction-sections
LDSCRIPTS += $(AM_HOME)/scripts/linker.ld
LDFLAGS   += --defsym=_pmem_start=0x80000000 --defsym=_entry_offset=0x0
LDFLAGS   += --gc-sections -e _start
YSYXSOCFLAGS += -l $(shell dirname $(IMAGE).elf)/ysyxsoc-log.txt
YSYXSOCFLAGS += -e $(IMAGE).elf
ifneq ($(BATCH), )
YSYXSOCFLAGS += -b
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

run: insert-arg
	$(MAKE) -C $(NPC_HOME) ISA=$(ISA) run ARGS="$(YSYXSOCFLAGS)" IMG=$(IMAGE).bin PARTFORM=YSYXSOC

sim: insert-arg
	$(MAKE) -C $(NPC_HOME) ISA=$(ISA) sim ARGS="$(YSYXSOCFLAGS)" IMG=$(IMAGE).bin PARTFORM=YSYXSOC

wave: insert-arg
	$(MAKE) -C $(NPC_HOME) ISA=$(ISA) wave ARGS="$(YSYXSOCFLAGS)" IMG=$(IMAGE).bin PARTFORM=YSYXSOC

.PHONY: insert-arg
