ISA ?= riscv32e
PARTFORM ?= npc

# NPC环境下，spinalhdl编译的顶层与verilator仿真的顶层一样为NPC_TOP
# NPC环境下，spinalhdl编译的顶层ysyx_23060082,verilator仿真ysyxSoCFull
ifeq ($(PARTFORM),ysyxsoc)
	SPINAL_TOPNAME = ysyx_23060082
	TOPNAME = ysyxSoCFull
	VSRCS += $(abspath ../ysyxSoC/build/ysyxSoCFull.v)
	VSRCS += $(shell find $(abspath ../ysyxSoC/perip) -name "*.v") 
else ifeq ($(PARTFORM),npc)
	SPINAL_TOPNAME = NPC_TOP
	TOPNAME = $(SPINAL_TOPNAME)
else
	$(error Unknown PARTFORM $(PARTFORM))
endif

INC_PATH += $(abspath include)
BUILD_DIR = ./build
OBJ_DIR = $(BUILD_DIR)/obj_dir
BIN = $(BUILD_DIR)/V$(TOPNAME)
SIM_WAVE = $(BUILD_DIR)/waveform.fst

# spinal生成verilog相关
SPINAL_PRJ = playground
SPINAL_VFILE = $(abspath $(BUILD_DIR)/$(SPINAL_TOPNAME).v)
SPINAL_SIM_DIR = $(BUILD_DIR)/simulations

# yosys综合相关
STA_DESIGN=ysyx_23060082
STA_SDC_FILE=$(abspath npc.sdc)
STA_CLK_FREQ_MHZ=100
STA_CLK_PORT_NAME=clock
STA_OUT=$(abspath $(BUILD_DIR)/sta)
# sta 目标内自行生成到 $(BUILD_DIR)/sta/ysyx_23060082.v (SPINAL_SIM_DEBUG=0)

ifneq ($(words $(CURDIR)),1)
  $(error Unsupported: GNU Make cannot build in directories containing spaces, build elsewhere: '$(CURDIR)')
endif

ifeq ($(VERILATOR_ROOT),)
  VERILATOR = verilator
else
  export VERILATOR_ROOT
  VERILATOR = $(VERILATOR_ROOT)/bin/verilator
endif

VERILATOR_CFLAGS += -MMD --build -cc  \
				-O3 --x-assign fast --x-initial fast --noassert \
				--trace-fst --relative-includes	\
				--no-timing --timescale "1ns/1ns"	\
				--autoflush

default: $(BIN)

$(shell mkdir -p $(BUILD_DIR))

# rules for NVBoard	
NXDC_FILES = constr/pin.nxdc
SRC_AUTO_BIND = $(abspath $(BUILD_DIR)/auto_bind.cpp)
$(SRC_AUTO_BIND): $(NXDC_FILES)
	@(sed -i '1s/.*/top=$(TOPNAME)/' $(NXDC_FILES))
	@python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@

include $(NVBOARD_HOME)/scripts/nvboard.mk

# verilator相关
VSRCS += $(SPINAL_VFILE)
VSRCS += $(shell find $(abspath playground/vsrc) -name "*.v")
CSRCS = $(shell find $(abspath csrc) -name "*.c" -or -name "*.cc" -or -name "*.cpp")
INCFLAGS = $(addprefix -I, $(INC_PATH))
CXXFLAGS += $(INCFLAGS) -DTOP_NAME="\"V$(TOPNAME)\"" -D__$(ISA)__ -D__$(PARTFORM)__
CXXFLAGS += $(shell pkg-config --cflags sdl2)
LDFLAGS += -lreadline $(shell pkg-config --libs sdl2)
override ARGS ?=
IMG ?=

#rules for disassemble
LIBCAPSTONE = tools/capstone/repo/libcapstone.so.5
INC_PATH +=  $(abspath tools/capstone/repo/include)
CXXFLAGS += -DCONFIG_ISA_riscv
$(abspath csrc/trace/disasm.c): $(LIBCAPSTONE)
$(LIBCAPSTONE):
	$(MAKE) -C tools/capstone

# 只操作spinal的两条命令:verilog与test
SPINAL_GEN_ENV := SPINAL_TOPNAME=$(SPINAL_TOPNAME) BUILD_DIR=$(BUILD_DIR)
SPINAL_SIM_ENV := SPINAL_SIM_DIR=$(BUILD_DIR)/simulations

verilog:
	$(call git_commit, "generate verilog")
	@mkdir -p $(BUILD_DIR)
	@$(SPINAL_GEN_ENV) ./mill -i $(SPINAL_PRJ).runMain $(SPINAL_PRJ).SpinalToVerilog 

test:
	@$(SPINAL_SIM_ENV) ./mill -i $(SPINAL_PRJ).runMain $(SPINAL_PRJ).SpinalSim 
	@gtkwave $(SPINAL_SIM_DIR)/$(SPINAL_TOPNAME)/test/wave.fst

# 应该每次都重新生成一次verilog文件
$(SPINAL_VFILE): verilog

# 此函数用于verilator编译，nvboard和直接仿真所用文件不同，例如：
# 		$(call build_target, $^, -D__USE_NVBOARD__),第一个变量为所需的文件，第二个变量为加入的宏定义
define build_target
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	@echo [verilator] compiling...; \
	_t=$$(date +%s.%N); \
	rm -rf $(OBJ_DIR);	\
	MAKEFLAGS=-s $(VERILATOR) $(VERILATOR_CFLAGS) \
		$(addprefix -CFLAGS , $(CXXFLAGS) $2) $(addprefix -LDFLAGS , $(LDFLAGS)) \
		--top-module $(TOPNAME) $1 \
		--Mdir $(OBJ_DIR) --exe -o $(abspath $(BIN)); \
	echo "[verilator] done ($$(printf "%.1f" $$(echo "$$(date +%s.%N) - $$_t" | bc))s)"
	@echo verilator[]: completed
	@echo [sim] $(BIN) running...; \
	_t=$$(date +%s.%N); \
	$(BIN) $(ARGS) $(IMG); \
	echo "[sim] done ($$(printf "%.1f" $$(echo "$$(date +%s.%N) - $$_t" | bc))s)"
endef


# 直接在nvboard上运行，直接查看现象
run: $(CSRCS) $(VSRCS) $(NVBOARD_ARCHIVE) $(SRC_AUTO_BIND)
	@$(call build_target, $^, -D__USE_NVBOARD__)

# 用于单步执行
sim: $(CSRCS) $(VSRCS)
	@$(call build_target, $^, )

# 运行并记录波形
wave: $(CSRCS) $(VSRCS)
	@$(call build_target, $^, -D__GET_WAVE__)
	@gtkwave $(SIM_WAVE)

# 终端图形配置界面: 切换 include/config.h 的功能开关(需终端运行)
menuconfig:
	@python3 tools/menuconfig.py

# sta: 自动用 ysyx_23060082 顶层 + 关闭仿真调试(SPINAL_SIM_DEBUG=0)生成综合用 verilog
# 输出到 build/sta/ 独立目录, 避免覆盖仿真用的 debug=true 版本
sta:
	@mkdir -p $(BUILD_DIR)/sta
	@SPINAL_TOPNAME=$(STA_DESIGN) BUILD_DIR=$(BUILD_DIR)/sta SPINAL_SIM_DEBUG=0 \
		./mill -i $(SPINAL_PRJ).runMain $(SPINAL_PRJ).SpinalToVerilog
	make -C /home/cyl/tools/yosys-sta sta \
			DESIGN=$(STA_DESIGN) SDC_FILE=$(STA_SDC_FILE) \
			CLK_FREQ_MHZ=$(STA_CLK_FREQ_MHZ) CLK_PORT_NAME=$(STA_CLK_PORT_NAME) O=$(STA_OUT) \
			RTL_FILES="$(abspath $(BUILD_DIR))/sta/$(STA_DESIGN).v"

# help:
# 	./mill -i $(SPINAL_PRJ).runMain Elborate --help

# reformat:
# 	./mill -i __.reformat

# checkformat:
# 	./mill -i __.checkFormat

# bsp:
# 	./mill -i mill.bsp.BSP/install

# idea:
# 	./mill -i mill.idea.GenIdea/idea

clean:
	-rm -rf $(BUILD_DIR) ./simulations ./*.v

.PHONY: $(SRC_AUTO_BIND) $(SPINAL_VFILE) test verilog run sim wave help reformat checkformat clean sta


-include ../Makefile
