SPINAL_TOPNAME = CPU

# verilator相关
TOPNAME = $(SPINAL_TOPNAME)
YXYXSOC_VFILE =

INC_PATH += $(abspath include)
BUILD_DIR = ./build
OBJ_DIR = $(BUILD_DIR)/obj_dir
BIN = $(BUILD_DIR)/V$(TOPNAME)
SIM_WAVE = $(BUILD_DIR)/waveform.fst

# spinal生成verilog相关
SPINAL_PRJ = playground
SPINAL_VFILE = $(abspath $(BUILD_DIR)/$(TOPNAME).v)
SPINAL_SIM_DIR = $(BUILD_DIR)/simulations

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
VSRCS += $(shell find $(abspath playground/verilog) -name "*.v")
CSRCS = $(shell find $(abspath csrc) -name "*.c" -or -name "*.cc" -or -name "*.cpp")
INCFLAGS = $(addprefix -I, $(INC_PATH))
CXXFLAGS += $(INCFLAGS) -DTOP_NAME="\"V$(TOPNAME)\""
LDFLAGS +=
override ARGS ?=
IMG ?=

# 只操作spinal的两条命令:
verilog:
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_DIR)
	SPINAL_TOPNAME=$(SPINAL_PRJ).$(SPINAL_TOPNAME) BUILD_DIR=$(BUILD_DIR)  mill -i $(SPINAL_PRJ).runMain $(SPINAL_PRJ).SpinalToVerilog 

$(SPINAL_VFILE): verilog

test:
	SPINAL_SIM_DIR=$(SPINAL_SIM_DIR) mill -i $(SPINAL_PRJ).runMain $(SPINAL_PRJ).SpinalSim 
	@gtkwave $(SPINAL_SIM_DIR)/$(SPINAL_TOPNAME)/test/wave.fst


# 此函数用于verilator编译，nvboard和直接仿真所用文件不同，例如：
# 		$(call build_target, $^, -D__USE_NVBOARD__),第一个变量为所需的文件，第二个变量为加入的宏定义
define build_target
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	@echo verilator[]: compiling...
	@rm -rf $(OBJ_DIR)
	@$(VERILATOR) $(VERILATOR_CFLAGS) \
		$(addprefix -CFLAGS , $(CXXFLAGS) $2) $(addprefix -LDFLAGS , $(LDFLAGS)) \
		--top-module $(TOPNAME) $1 \
		--Mdir $(OBJ_DIR) --exe -o $(abspath $(BIN)) \
		2>&1 | grep -v -E '^g++|^echo|^rm|^python3'
	@echo verilator[]: completed
	@echo $(BIN) start running...
	@$(BIN) $(ARGS) $(IMG)
	@echo $(BIN) run end
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

# help:
# 	mill -i $(SPINAL_PRJ).runMain Elborate --help

# reformat:
# 	mill -i __.reformat

# checkformat:
# 	mill -i __.checkFormat

# bsp:
# 	mill -i mill.bsp.BSP/install

# idea:
# 	mill -i mill.idea.GenIdea/idea

clean:
	-rm -rf $(BUILD_DIR)

.PHONY: test verilog run sim wave help reformat checkformat clean


-include ../Makefile
