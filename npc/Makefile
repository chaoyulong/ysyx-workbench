ISA ?= riscv32e
PARTFORM ?= ysyxsoc

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
BUILD_DIR = $(abspath ./build)
OBJ_DIR = $(BUILD_DIR)/obj_dir
BIN = $(BUILD_DIR)/V$(TOPNAME)
SIM_WAVE = $(BUILD_DIR)/waveform.fst

# ======================================== spinal生成verilog相关 ======================================== #
MILL ?= mill
SPINAL_PRJ = playground
SPINAL_VFILE = $(abspath $(BUILD_DIR)/$(SPINAL_TOPNAME).v)
SPINAL_SIM_DIR = $(BUILD_DIR)/simulations
SPINAL_SRCS = $(shell find $(abspath playground) -name '*.scala') \
              $(wildcard $(abspath build.sc) $(abspath .mill-version) $(abspath build.mill))
# ======================================== yosys综合相关 ======================================== #
# sta目标内自行生成到$(BUILD_DIR)/sta/ysyx_23060082.v(SPINAL_SIM_DEBUG=0)
YOSYS_STA_PATH ?= /home/cyl/tools/yosys-sta
STA_DESIGN = ysyx_23060082
STA_SDC_FILE ?= $(abspath constr/npc.sdc)
STA_CLK_FREQ_MHZ ?= 500
STA_CLK_PORT_NAME ?= clock
STA_BUILD_DIR = $(abspath $(BUILD_DIR)/sta)
STA_VFILE = $(STA_BUILD_DIR)/$(STA_DESIGN).v
# nangate45，icsprout55
STA_PDK ?= nangate45

# ======================================== iverilog四值仿真相关 ======================================== #
IVERILOG ?= iverilog
VVP ?= vvp
# bin文件转hex文件
IV_BIN2MEM = $(abspath sim-iverilog/tools/bin2mem.sh)

IV_DESIGN = ysyx_23060082
IV_BUILD_DIR = $(abspath $(BUILD_DIR)/iverilog)
IV_TB_TOP = tb_npc
IV_TB ?= $(abspath sim-iverilog/testbench/tb_npc.v)
IV_VFILE = $(IV_BUILD_DIR)/$(IV_DESIGN).v
IV_STA_BUILD_DIR = $(IV_BUILD_DIR)/sta
NETLIST ?= $(IV_STA_BUILD_DIR)/$(IV_DESIGN)-$(STA_CLK_FREQ_MHZ)MHz/$(IV_DESIGN).netlist.v.sim
CELLS ?= $(abspath sim-iverilog/testbench/cells.v)

# 最大仿真周期
IV_MAXCYC ?= 20000000
IV_ARGS ?=
IV_IMG_DIR = $(IV_BUILD_DIR)/img
IV_IMG = $(IV_IMG_DIR)/img.hex

# ======================================== ======================================== #
ifneq ($(words $(CURDIR)),1)
  $(error Unsupported: GNU Make cannot build in directories containing spaces, build elsewhere: '$(CURDIR)')
endif

default: $(BIN)

$(shell mkdir -p $(BUILD_DIR))

# ======================================== NVBoard相关 ======================================== #
NXDC_FILES = constr/pin.nxdc
SRC_AUTO_BIND = $(abspath $(BUILD_DIR)/auto_bind.cpp)
$(SRC_AUTO_BIND): $(NXDC_FILES)
	@(sed -i '1s/.*/top=$(TOPNAME)/' $(NXDC_FILES))
	@python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@

include $(NVBOARD_HOME)/scripts/nvboard.mk

# ======================================== verilator相关 ======================================== #
VERILATOR ?= verilator
GTKWAVE ?= gtkwave

VERILATOR_CFLAGS += -MMD --build -cc \
				-O3 --x-assign fast --x-initial fast --noassert \
				--trace-fst --relative-includes	\
				--no-timing --timescale "1ns/1ns"	\
				--autoflush

VSRCS += $(SPINAL_VFILE)
VSRCS += $(shell find $(abspath playground/vsrc) -name "*.v")
CSRCS = $(shell find $(abspath csrc) -name "*.c" -or -name "*.cc" -or -name "*.cpp")
INCFLAGS = $(addprefix -I, $(INC_PATH))
CXXFLAGS += $(INCFLAGS) -DTOP_NAME="\"V$(TOPNAME)\"" -D__$(ISA)__ -D__$(PARTFORM)__ -DISA_NAME=$(ISA) -DPARTFORM_NAME=$(PARTFORM)
CXXFLAGS += $(shell pkg-config --cflags sdl2)
LDFLAGS += -lreadline $(shell pkg-config --libs sdl2)
override ARGS ?=
IMG ?=

# 反汇编规则，rules for disassemble，用于itrace
LIBCAPSTONE = tools/capstone/repo/libcapstone.so.5
INC_PATH +=  $(abspath tools/capstone/repo/include)
CXXFLAGS += -DCONFIG_ISA_riscv
$(abspath csrc/trace/disasm.c): $(LIBCAPSTONE)
$(LIBCAPSTONE):
	$(MAKE) -C tools/capstone

# ======================================== spinalhdl相关 ======================================== #
# 只操作spinal的两条命令:verilog与test
SPINAL_GEN_ENV := SPINAL_TOPNAME=$(SPINAL_TOPNAME) BUILD_DIR=$(BUILD_DIR) PARTFORM=$(PARTFORM)
SPINAL_SIM_ENV := SPINAL_SIM_DIR=$(BUILD_DIR)/simulations PARTFORM=$(PARTFORM)
# 用于生成sta时所需要的不含调试信号的ysyx_23060082.v,所以SPINAL_SIM_DEBUG信号要关闭，PARTFORM永远为ysyxsoc，命令行传PARTFORM也不会影响sta
SPINAL_STA_ENV := SPINAL_TOPNAME=$(STA_DESIGN) BUILD_DIR=$(STA_BUILD_DIR) SPINAL_SIM_DEBUG=0 PARTFORM=ysyxsoc
# 四值仿真要求从0x80000000开始，所以PARTFORM要选择npc
SPINAL_IV_ENV := SPINAL_TOPNAME=$(IV_DESIGN) BUILD_DIR=$(IV_BUILD_DIR) SPINAL_SIM_DEBUG=0 PARTFORM=npc
# ========================================  ======================================== #
verilog: $(SPINAL_SRCS)
	$(call git_commit, "generate verilog")
	@mkdir -p $(BUILD_DIR)
	@$(SPINAL_GEN_ENV) $(MILL) -i $(SPINAL_PRJ).runMain $(SPINAL_PRJ).SpinalToVerilog 

test: $(SPINAL_SRCS)
	@$(SPINAL_SIM_ENV) $(MILL) -i $(SPINAL_PRJ).runMain $(SPINAL_PRJ).SpinalSim 
	@$(GTKWAVE) $(SPINAL_SIM_DIR)/$(SPINAL_TOPNAME)/test/wave.fst

verilog-sta: $(SPINAL_SRCS)
	@mkdir -p $(STA_BUILD_DIR)
	@$(SPINAL_STA_ENV) $(MILL) -i $(SPINAL_PRJ).runMain $(SPINAL_PRJ).SpinalToVerilog

verilog-iverilog: $(SPINAL_SRCS)
	@mkdir -p $(IV_BUILD_DIR)
	@$(SPINAL_IV_ENV) $(MILL) -i $(SPINAL_PRJ).runMain $(SPINAL_PRJ).SpinalToVerilog

# 应该每次都重新生成一次verilog文件
$(SPINAL_VFILE): verilog
$(STA_VFILE): verilog-sta
$(IV_VFILE): verilog-iverilog

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
		--Mdir $(OBJ_DIR) --exe -o $(abspath $(BIN)) && \
	echo "[verilator] done ($$(printf "%.1f" $$(echo "$$(date +%s.%N) - $$_t" | bc))s)"
	@echo verilator[]: completed
	@echo [sim] $(BIN) running...; \
	_t=$$(date +%s.%N); \
	$(BIN) $(ARGS) $(IMG) && \
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
	@$(GTKWAVE) $(SIM_WAVE)

# 性能测试: 运行 microbench(test规模)并输出统计(周期/指令/IPC + PERF计数器)
# 配合 git checkout 历史 commit, 可复现当时代码的性能数据(记录见 PERF.md)
# 注意: 用 sim 而非 run(run 会调用 nvboard, 永不退出)
perf:
	@$(MAKE) -C $(abspath ../am-kernels/benchmarks/microbench) \
		ARCH=riscv32e-ysyxsoc sim mainargs=test sdb=n

# 终端图形配置界面: 切换 include/config.h 的功能开关(需终端运行)
menuconfig:
	@python3 tools/menuconfig.py

# sta: 自动用ysyx_23060082顶层 + 关闭仿真调试(SPINAL_SIM_DEBUG=0)生成综合用verilog
# 输出到 build/sta/独立目录, 避免覆盖仿真用的debug=true版本
sta: $(STA_VFILE)
	$(MAKE) -C $(YOSYS_STA_PATH) sta \
			DESIGN=$(STA_DESIGN) SDC_FILE=$(STA_SDC_FILE) \
			PDK=$(STA_PDK) CLK_FREQ_MHZ=$(STA_CLK_FREQ_MHZ) CLK_PORT_NAME=$(STA_CLK_PORT_NAME) O=$(STA_BUILD_DIR) \
			RTL_FILES="$(STA_VFILE)"

# 外部(命令行/环境变量)没有给NETLIST时才会综合
#   CI 约定: make sim-iverilog-netlist IMG=xxx.bin NETLIST=yyy CELLS=zzz
#   给了 NETLIST 就直接用外部网表(跳过 SpinalToVerilog 与 yosys), 只做 bin->hex + 编译 + 仿真
ifeq ($(filter command line environment override,$(origin NETLIST)),)
$(NETLIST): $(IV_VFILE)
	$(MAKE) -C $(YOSYS_STA_PATH) sta \
			DESIGN=$(IV_DESIGN) SDC_FILE=$(STA_SDC_FILE) \
			PDK=$(STA_PDK) CLK_FREQ_MHZ=$(STA_CLK_FREQ_MHZ) CLK_PORT_NAME=$(STA_CLK_PORT_NAME) O=$(IV_STA_BUILD_DIR) \
			RTL_FILES="$(IV_VFILE)"
	@test -f $@ || { echo "错误: 综合没有产出 $@"; exit 1; }
else
# 外部网表: 无前置依赖 ⇒ 文件存在时 make 认为最新、配方不跑; 不存在则给明确报错
$(NETLIST):
	@test -f $@ || { echo "错误: 找不到外部指定的网表 $@ (用法: NETLIST=<path>)"; exit 1; }
endif

# lint: 对STA的综合输入(build/sta/$(STA_DESIGN).v, SPINAL_SIM_DEBUG=0 那份)做verilator静态检查
#   只把"真问题"类警告当失败: 位宽(WIDTH*)/锁存器(LATCH)/组合环(UNOPTFLAT,COMBDLY)/
#   多驱动(MULTIDRIVEN)/未驱动(UNDRIVEN)/隐式线网(IMPLICIT)/阻塞赋值(BLKSEQ)/case 不全(CASEINCOMPLETE) ...
#   下面三类是 SpinalHDL 生成物的已知噪声, 显式屏蔽:
#     UNUSEDSIGNAL 未用到的端口/位(io_interrupt / io_slave_* / RegFile addr[4] / icache pc[1:0] ...)
#                  以及 `ifndef SYNTHESIS 包起来的枚举调试字符串(共约 35 个)
#     UNUSEDPARAM  枚举的 localparam(只被上面那些调试字符串引用)
#     DECLFILENAME 一个 .v 里放了 18 个模块
#   想看**全部**警告(含噪声)用: make lint-all
LINT_WAIVER = -Wno-UNUSEDSIGNAL -Wno-UNUSEDPARAM -Wno-DECLFILENAME

lint: $(STA_VFILE)
	$(VERILATOR) --lint-only -Wall $(LINT_WAIVER) --top-module $(STA_DESIGN) $(STA_VFILE)
	@echo "[lint] OK: 无'真问题'类警告(位宽/锁存/组合环/多驱动/未驱动/隐式线网)"

# 打印全部警告
lint-all: $(STA_VFILE)
	$(VERILATOR) --lint-only -Wall -Wno-fatal --top-module $(STA_DESIGN) $(STA_VFILE)
	@echo "[lint-all] 以上为全部警告(含噪声); 只挑真问题用 make lint"

# ================================ iverilog (四值仿真 / 网表仿真) ================================
# iverilog需要使用$readmemh装载指令，但是规定中IMG是bin格式文件，所以需要转化一下
$(IV_IMG): $(IMG) $(IV_BIN2MEM)
	@test -n "$(IMG)" || { echo "错误: 需要 IMG=<xxx.bin>"; exit 1; }
	@mkdir -p $(IV_IMG_DIR)
	@$(IV_BIN2MEM) $(IMG) $(IV_IMG)

# 四值仿真，使用平台为npc，复位值0x80000000,不接入
sim-iverilog: $(IV_TB) $(IV_VFILE) $(IV_IMG)
	@mkdir -p $(IV_BUILD_DIR)
	@$(IVERILOG) -g2012 -o $(IV_BUILD_DIR)/sim-iverilog.vvp -s $(IV_TB_TOP) $(IV_TB) $(IV_VFILE)
	@$(VVP) $(IV_BUILD_DIR)/sim-iverilog.vvp +img=$(IV_IMG) +img_size=$(shell stat -c%s $(IMG)) +max_cycles=$(IV_MAXCYC) $(IV_ARGS)

# 网表仿真
sim-iverilog-netlist: $(IV_TB) $(NETLIST) $(CELLS) $(IV_IMG)
	@mkdir -p $(IV_BUILD_DIR)
	@$(IVERILOG) -g2012 -DNETLIST -o $(IV_BUILD_DIR)/netlist-iverilog.vvp -s $(IV_TB_TOP) $(IV_TB) $(NETLIST) $(CELLS)
	@$(VVP) $(IV_BUILD_DIR)/netlist-iverilog.vvp +img=$(IV_IMG) +img_size=$(shell stat -c%s $(IMG)) +max_cycles=$(IV_MAXCYC) $(IV_ARGS)

clean:
	-rm -rf $(BUILD_DIR) ./simulations ./*.v

clean-all:
	-rm -rf out $(BUILD_DIR) ./simulations ./*.v

.PHONY: $(SRC_AUTO_BIND) $(SPINAL_VFILE)\
				test verilog run sim wave clean clean-all perf menuconfig\
				verilog-sta sta lint lint-all\
				$(IV_IMG) verilog-iverilog sim-iverilog sim-iverilog-netlist

-include ../Makefile
