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
BUILD_DIR = ./build
OBJ_DIR = $(BUILD_DIR)/obj_dir
BIN = $(BUILD_DIR)/V$(TOPNAME)
SIM_WAVE = $(BUILD_DIR)/waveform.fst

# spinal生成verilog相关
SPINAL_PRJ = playground
SPINAL_VFILE = $(abspath $(BUILD_DIR)/$(SPINAL_TOPNAME).v)
SPINAL_SIM_DIR = $(BUILD_DIR)/simulations

# yosys综合相关
# sta目标内自行生成到$(BUILD_DIR)/sta/ysyx_23060082.v(SPINAL_SIM_DEBUG=0)
STA_DESIGN=ysyx_23060082
STA_SDC_FILE=$(abspath constr/npc.sdc)
STA_CLK_FREQ_MHZ=500
STA_CLK_PORT_NAME=clock
STA_OUT=$(abspath $(BUILD_DIR)/sta)
# 综合工艺: nangate45(教程基准, 面积限制按此工艺) / icsprout55(流片工艺)
STA_PDK=nangate45

ifneq ($(words $(CURDIR)),1)
  $(error Unsupported: GNU Make cannot build in directories containing spaces, build elsewhere: '$(CURDIR)')
endif

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
VERILATOR = verilator
GTKWAVE = gtkwave

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

#rules for disassemble
LIBCAPSTONE = tools/capstone/repo/libcapstone.so.5
INC_PATH +=  $(abspath tools/capstone/repo/include)
CXXFLAGS += -DCONFIG_ISA_riscv
$(abspath csrc/trace/disasm.c): $(LIBCAPSTONE)
$(LIBCAPSTONE):
	$(MAKE) -C tools/capstone

# 只操作spinal的两条命令:verilog与test
SPINAL_GEN_ENV := SPINAL_TOPNAME=$(SPINAL_TOPNAME) BUILD_DIR=$(BUILD_DIR) PARTFORM=$(PARTFORM)
SPINAL_SIM_ENV := SPINAL_SIM_DIR=$(BUILD_DIR)/simulations PARTFORM=$(PARTFORM)

verilog:
	$(call git_commit, "generate verilog")
	@mkdir -p $(BUILD_DIR)
	@$(SPINAL_GEN_ENV) ./mill -i $(SPINAL_PRJ).runMain $(SPINAL_PRJ).SpinalToVerilog 

test:
	@$(SPINAL_SIM_ENV) ./mill -i $(SPINAL_PRJ).runMain $(SPINAL_PRJ).SpinalSim 
	@$(GTKWAVE) $(SPINAL_SIM_DIR)/$(SPINAL_TOPNAME)/test/wave.fst

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

# sta: 自动用 ysyx_23060082 顶层 + 关闭仿真调试(SPINAL_SIM_DEBUG=0)生成综合用 verilog
# 输出到 build/sta/ 独立目录, 避免覆盖仿真用的 debug=true 版本
sta:
	@mkdir -p $(BUILD_DIR)/sta
	@SPINAL_TOPNAME=$(STA_DESIGN) BUILD_DIR=$(BUILD_DIR)/sta SPINAL_SIM_DEBUG=0 \
		./mill -i $(SPINAL_PRJ).runMain $(SPINAL_PRJ).SpinalToVerilog
	make -C /home/cyl/tools/yosys-sta sta \
			DESIGN=$(STA_DESIGN) SDC_FILE=$(STA_SDC_FILE) \
			PDK=$(STA_PDK) CLK_FREQ_MHZ=$(STA_CLK_FREQ_MHZ) CLK_PORT_NAME=$(STA_CLK_PORT_NAME) O=$(STA_OUT) \
			RTL_FILES="$(abspath $(BUILD_DIR))/sta/$(STA_DESIGN).v"

# lint: 对 STA 的综合输入(build/sta/$(STA_DESIGN).v, SPINAL_SIM_DEBUG=0 那份)做 verilator 静态检查
#   只把"真问题"类警告当失败: 位宽(WIDTH*)/锁存器(LATCH)/组合环(UNOPTFLAT,COMBDLY)/
#   多驱动(MULTIDRIVEN)/未驱动(UNDRIVEN)/隐式线网(IMPLICIT)/阻塞赋值(BLKSEQ)/case 不全(CASEINCOMPLETE) ...
#   下面三类是 SpinalHDL 生成物的已知噪声, 显式屏蔽:
#     UNUSEDSIGNAL 未用到的端口/位(io_interrupt / io_slave_* / RegFile addr[4] / icache pc[1:0] ...)
#                  以及 `ifndef SYNTHESIS 包起来的枚举调试字符串(共约 35 个)
#     UNUSEDPARAM  枚举的 localparam(只被上面那些调试字符串引用)
#     DECLFILENAME 一个 .v 里放了 18 个模块
#   想看**全部**警告(含噪声)用: make lint-all
LINT_WAIVER = -Wno-UNUSEDSIGNAL -Wno-UNUSEDPARAM -Wno-DECLFILENAME

lint:
	@SPINAL_TOPNAME=$(STA_DESIGN) BUILD_DIR=$(BUILD_DIR)/sta SPINAL_SIM_DEBUG=0 \
		./mill -i $(SPINAL_PRJ).runMain $(SPINAL_PRJ).SpinalToVerilog
	verilator --lint-only -Wall $(LINT_WAIVER) --top-module $(STA_DESIGN) \
		$(BUILD_DIR)/sta/$(STA_DESIGN).v
	@echo "[lint] OK: 无'真问题'类警告(位宽/锁存/组合环/多驱动/未驱动/隐式线网)"

# lint-all: 同上, 但**不屏蔽任何警告**(含 SpinalHDL 生成物噪声), 只用来人工查看全部警告
lint-all:
	@SPINAL_TOPNAME=$(STA_DESIGN) BUILD_DIR=$(BUILD_DIR)/sta SPINAL_SIM_DEBUG=0 \
		./mill -i $(SPINAL_PRJ).runMain $(SPINAL_PRJ).SpinalToVerilog
	-verilator --lint-only -Wall --top-module $(STA_DESIGN) $(BUILD_DIR)/sta/$(STA_DESIGN).v
	@echo "[lint-all] 以上为全部警告(含噪声); 只挑真问题用 make lint"

# ================================ iverilog (四值仿真 / 网表仿真) ================================
# 目标名按课程 CI 约定:
#   make sim-iverilog          IMG=xxx.bin
#   make sim-iverilog-netlist  IMG=xxx.bin [NETLIST=yyy.v] [CELLS=zzz.v]
#
# 几个刻意的设计:
#   1) 仿真的 RTL 单独生成到 build/iverilog/, 且用 SPINAL_SIM_DEBUG=0
#      ⇒ 生成结果里没有 DPI 黑盒(ItraceReg/MtraceReg/PerfReg/NpcMemRW) ⇒ iverilog 能编
#      (build/ysyx_23060082.v 那份是 debug=true 的, 含 DPI, 不能给 iverilog 用)
#   2) 【显式传 PARTFORM】: sta/lint 的配方没传 PARTFORM, Config 会退到默认 npc(resetPc=0x80000000);
#      这里按 CI 要求用 ysyxsoc(resetPc=0x30000000), 再由 TB 在 0x30000000 处放 trampoline 跳到 0x80000000
#   3) 网表默认用 ...netlist.v.sim: yosys.tcl 里 splitnets -ports 会把【端口也拆成单位】,
#      而 .sim 是拆端口之前写的 ⇒ 端口还是向量 ⇒ 同一个 TB 可以直接接 RTL 与网表
#   4) 不加 git_commit: iverilog 仿真会反复跑, 不该每次都在仓库里留一条自动提交
IVERILOG   ?= iverilog
VVP        ?= vvp
IV_DIR      = $(BUILD_DIR)/iverilog
IV_TB_DIR   = $(abspath sim-iverilog/testbench)
IV_TB       = $(IV_TB_DIR)/tb_npc.v
IV_BIN2MEM  = $(abspath sim-iverilog/tools/bin2mem.sh)
IV_RTL      = $(IV_DIR)/$(STA_DESIGN).v
IV_IMG      = $(IV_DIR)/img.hex
IV_MAXCYC  ?= 40000000
IV_ARGS    ?=
IV_SRCS     = $(shell find $(abspath playground/src) -name "*.scala") $(abspath ./build.sc)
NETLIST    ?= $(abspath $(BUILD_DIR))/sta/$(STA_DESIGN)-500MHz/$(STA_DESIGN).netlist.v.sim
CELLS      ?= $(IV_TB_DIR)/cells.v

$(IV_RTL): $(IV_SRCS)
	@mkdir -p $(IV_DIR)
	@SPINAL_TOPNAME=$(STA_DESIGN) BUILD_DIR=$(IV_DIR) SPINAL_SIM_DEBUG=0 PARTFORM=$(PARTFORM) \
		./mill -i $(SPINAL_PRJ).runMain $(SPINAL_PRJ).SpinalToVerilog

# 四值仿真: RTL(无 debug 黑盒) + TB(带突发的 AXI 从机 + 存储器)
sim-iverilog: $(IV_RTL) $(IV_TB)
	@test -n "$(IMG)" || { echo "错误: 需要 IMG=<xxx.bin>（例: make sim-iverilog IMG=sim-iverilog/bin/microbench-riscv32e-npc.bin）"; exit 1; }
	@mkdir -p $(IV_DIR)
	@$(IV_BIN2MEM) $(IMG) $(IV_IMG)
	$(IVERILOG) -g2012 -o $(IV_DIR)/sim-iv.vvp -s tb_npc $(IV_RTL) $(IV_TB)
	$(VVP) $(IV_DIR)/sim-iv.vvp +img=$(IV_IMG) +img_size=$(shell stat -c%s $(IMG)) +max_cycles=$(IV_MAXCYC) $(IV_ARGS)

# 网表仿真: 同一个 TB + yosys 网表 + nangate45 标准单元行为模型
sim-iverilog-netlist: $(IV_TB) $(IV_IMG)
	@test -n "$(IMG)" || { echo "错误: 需要 IMG=<xxx.bin>"; exit 1; }
	@test -f "$(NETLIST)" || { echo "错误: 找不到网表 $(NETLIST)（先 make sta, 或用 NETLIST= 指定）"; exit 1; }
	@test -f "$(CELLS)"   || { echo "错误: 找不到标准单元模型 $(CELLS)（或用 CELLS= 指定）"; exit 1; }
	@mkdir -p $(IV_DIR)
	@$(IV_BIN2MEM) $(IMG) $(IV_IMG)
	$(IVERILOG) -g2012 -DIV_NETLIST -o $(IV_DIR)/net-iv.vvp -s tb_npc $(NETLIST) $(CELLS) $(IV_TB)
	$(VVP) $(IV_DIR)/net-iv.vvp +img=$(IV_IMG) +img_size=$(shell stat -c%s $(IMG)) +max_cycles=$(IV_MAXCYC) $(IV_ARGS)

clean:
	-rm -rf $(BUILD_DIR) ./simulations ./*.v

.PHONY: $(SRC_AUTO_BIND) $(SPINAL_VFILE) test verilog run sim wave help reformat checkformat clean sta lint lint-all sim-iverilog sim-iverilog-netlist


-include ../Makefile
