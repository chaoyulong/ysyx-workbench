//=====================================================================
// tb_npc.v —— NPC 的 iverilog 仿真顶层（四值仿真 / 网表仿真共用）
//
// 作用：替代 verilator 流程里的 C++/DPI 环境
//   1) 产生 时钟 / 复位（项目是【同步复位】：复位保持 RESET_CYCLES = 50 拍再释放）
//   2) 例化 CPU 顶层 ysyx_23060082（SPINAL_SIM_DEBUG=0 生成的那份，无 DPI 黑盒）
//   3) 用行为级代码实现一块【支持突发的 AXI4 从机】+ pmem
//      - icache 会发 4 拍突发(ARLEN=3)，只做单拍必挂
//      - 写通道按 WSTRB 逐字节落内存（注意窄传输的字节落点，见下面注释）
//   4) 地址译码（只实现三块东西）
//      - 0x80000000 起 0x8000000(128MB) : pmem，镜像就装在这里，第一条指令在 0x80000000
//      - 0x10000000                      : UART 写 -> $write("%c")，这是判断程序跑对了的唯一输出
//      - 0x30000000 (16 字节)            : trampoline(lui t0,0x80000; jr t0)
//            存在的唯一原因：CI 要求 make verilog 产物的 PC 复位值是 0x30000000，
//            而程序要放在 0x80000000 ⇒ 用两条指令跳过去。
//            如果 RTL 用 PARTFORM=npc 生成(复位值本来就是 0x80000000)，这段永远不会被执行。
//      - 其余地址                        : 返回 0，并打印有限次警告（方便发现漏译码的地址段）
//      ★ CLINT 在 ysyx_23060082 内部（Xbar 把 0x02000000 路由给内部 CLINT），
//        外总线看不到，所以这里【不需要】实现 CLINT
//   5) 结束条件
//      - RTL 仿真：层级引用 LSU 的提交信号，看到 ebreak 提交(cause=3) 就停
//      - 网表仿真：扁平化后层级没了，退化为 watchdog 超时停（+max_cycles= 可调）
//
// 用法（由 make sim-iverilog / sim-iverilog-netlist 调用）：
//   vvp sim.vvp +img=xxx.hex [+max_cycles=40000000] [+zero=16777216] [+vcd] [+quiet] [+trace_uart]
//   - img : $readmemh 用的十六进制文本（由 sim-iverilog/tools/bin2mem.sh 从 .bin 生成）
//   - zero: 启动时清零的字节数（模拟 verilator 流程里 malloc 出来的零内存；需覆盖 .bss/stack）
//   - vcd : 打开波形（默认关，波形会让仿真慢一个数量级）
//=====================================================================
`timescale 1ns/1ps

module tb_npc;

  //------------------------------------------------------------------
  // 参数
  //------------------------------------------------------------------
  localparam PMEM_BASE = 32'h8000_0000;
  localparam PMEM_SIZE = 32'h0800_0000;      // 0x8000000 = 128MB
  localparam SERIAL_BASE = 32'h1000_0000;    // AM: SERIAL_PORT
  localparam TRAMP_BASE  = 32'h3000_0000;    // 复位取指地址(CI 要求)

  parameter RESET_CYCLES = 50;
  parameter DEFAULT_MAX_CYCLES = 4_000_000;  // 实测 microbench 约 105 万拍 ⇒ 留约 4 倍余量
                                             // （卡住时 RTL 约等 1 分钟；网表很慢，请显式给 IV_MAXCYC=）
  parameter DEFAULT_ZERO = 1 << 24;          // 启动清零 16MB（要盖住 .bss/stack）

  //------------------------------------------------------------------
  // 时钟 / 复位
  //------------------------------------------------------------------
  reg clock = 1'b0;
  reg reset = 1'b1;

  always #1 clock = ~clock;                  // 2ns 周期

  //------------------------------------------------------------------
  // pmem：字节粒度，下标 = 地址 - 0x80000000
  //   （与 $readmemh 的 "每行一个字节" 格式对应；镜像链接在 0x80000000）
  //------------------------------------------------------------------
  reg [7:0] pmem [0:PMEM_SIZE-1];

  // trampoline: lui t0,0x80000 ; jr t0 ; nop ; nop
  reg [31:0] tramp [0:3];
  initial begin
    tramp[0] = 32'h8000_02B7;                // lui  t0, 0x80000
    tramp[1] = 32'h0002_8067;                // jalr x0, 0(t0)  == jr t0
    tramp[2] = 32'h0000_0013;                // nop
    tramp[3] = 32'h0000_0013;                // nop
  end

  //------------------------------------------------------------------
  // DUT
  //------------------------------------------------------------------
  wire        io_master_awvalid;
  wire        io_master_awready;
  wire [31:0] io_master_awaddr;
  wire [3:0]  io_master_awid;
  wire [7:0]  io_master_awlen;
  wire [2:0]  io_master_awsize;
  wire [1:0]  io_master_awburst;
  wire        io_master_wvalid;
  wire        io_master_wready;
  wire [31:0] io_master_wdata;
  wire [3:0]  io_master_wstrb;
  wire        io_master_wlast;
  wire        io_master_bvalid;
  wire        io_master_bready;
  wire [3:0]  io_master_bid;
  wire [1:0]  io_master_bresp;
  wire        io_master_arvalid;
  wire        io_master_arready;
  wire [31:0] io_master_araddr;
  wire [3:0]  io_master_arid;
  wire [7:0]  io_master_arlen;
  wire [2:0]  io_master_arsize;
  wire [1:0]  io_master_arburst;
  wire        io_master_rvalid;
  wire        io_master_rready;
  wire [31:0] io_master_rdata;
  wire [3:0]  io_master_rid;
  wire [1:0]  io_master_rresp;
  wire        io_master_rlast;

  ysyx_23060082 dut (
    .clock                    (clock                    ),
    .reset                    (reset                    ),
    .io_interrupt             (1'b0                     ),
    // ---- 主设备口(CPU -> TB 的 AXI 从机) ----
    .io_master_awvalid        (io_master_awvalid        ),
    .io_master_awready        (io_master_awready        ),
    .io_master_awaddr         (io_master_awaddr         ),
    .io_master_awid           (io_master_awid           ),
    .io_master_awlen          (io_master_awlen          ),
    .io_master_awsize         (io_master_awsize         ),
    .io_master_awburst        (io_master_awburst        ),
    .io_master_wvalid         (io_master_wvalid         ),
    .io_master_wready         (io_master_wready         ),
    .io_master_wdata          (io_master_wdata          ),
    .io_master_wstrb          (io_master_wstrb          ),
    .io_master_wlast          (io_master_wlast          ),
    .io_master_bvalid         (io_master_bvalid         ),
    .io_master_bready         (io_master_bready         ),
    .io_master_bid            (io_master_bid            ),
    .io_master_bresp          (io_master_bresp          ),
    .io_master_arvalid        (io_master_arvalid        ),
    .io_master_arready        (io_master_arready        ),
    .io_master_araddr         (io_master_araddr         ),
    .io_master_arid           (io_master_arid           ),
    .io_master_arlen          (io_master_arlen          ),
    .io_master_arsize         (io_master_arsize         ),
    .io_master_arburst        (io_master_arburst        ),
    .io_master_rvalid         (io_master_rvalid         ),
    .io_master_rready         (io_master_rready         ),
    .io_master_rdata          (io_master_rdata          ),
    .io_master_rid            (io_master_rid            ),
    .io_master_rresp          (io_master_rresp          ),
    .io_master_rlast          (io_master_rlast          ),
    // ---- 从设备口(CPU 收, 本环境不使用, 必须给确定值) ----
    .io_slave_awvalid         (1'b0                     ),
    .io_slave_awready         (                         ),
    .io_slave_awaddr          (32'b0                   ),
    .io_slave_awid            (4'b0                    ),
    .io_slave_awlen           (8'b0                    ),
    .io_slave_awsize          (3'b0                    ),
    .io_slave_awburst         (2'b0                    ),
    .io_slave_wvalid          (1'b0                     ),
    .io_slave_wready          (                         ),
    .io_slave_wdata           (32'b0                   ),
    .io_slave_wstrb           (4'b0                    ),
    .io_slave_wlast           (1'b0                     ),
    .io_slave_bvalid          (                         ),
    .io_slave_bready          (1'b1                     ),
    .io_slave_bid             (                         ),
    .io_slave_bresp           (                         ),
    .io_slave_arvalid         (1'b0                     ),
    .io_slave_arready         (                         ),
    .io_slave_araddr          (32'b0                   ),
    .io_slave_arid            (4'b0                    ),
    .io_slave_arlen           (8'b0                    ),
    .io_slave_arsize          (3'b0                    ),
    .io_slave_arburst         (2'b0                    ),
    .io_slave_rvalid          (                         ),
    .io_slave_rready          (1'b1                     ),
    .io_slave_rdata           (                         ),
    .io_slave_rid             (                         ),
    .io_slave_rresp           (                         ),
    .io_slave_rlast           (                         )
  );

  //------------------------------------------------------------------
  // 统计 / 状态
  //------------------------------------------------------------------
  reg [63:0] cycles = 64'd0;
  reg [63:0] max_cycles = DEFAULT_MAX_CYCLES;
  reg [31:0] zero_size = DEFAULT_ZERO;
  reg [31:0] img_size = 32'd0;      // 镜像字节数(由 Makefile 传 +img_size=), 用来区分"镜像区"和"清零区"
  reg [31:0] uart_stop = 32'd0;     // +uart_stop=N: UART 输出到 N 字节就结束(网表模式没法看 ebreak 时的收尾开关)
  integer    uart_bytes = 0;
  integer    unmapped_reads = 0;
  integer    unmapped_writes = 0;
  integer    unmapped_prints = 0;
  reg [31:0] last_unmapped = 32'hFFFF_FFFF;

  reg [1023:0] img_file;
  reg [1023:0] dump_file;
  reg quiet = 1'b0;
  reg trace_uart = 1'b0;

  always @(posedge clock) cycles <= cycles + 64'd1;

  //------------------------------------------------------------------
  // 小工具 / 地址译码
  //------------------------------------------------------------------
  function has_x32(input [31:0] v); has_x32 = (^v === 1'bx); endfunction
  function has_x1 (input        v); has_x1  = (^v === 1'bx); endfunction

  function in_pmem(input [31:0] addr);
    in_pmem = (addr >= PMEM_BASE) && (addr < PMEM_BASE + PMEM_SIZE);
  endfunction

  function in_serial(input [31:0] addr);
    in_serial = (addr >= SERIAL_BASE) && (addr < SERIAL_BASE + 8);
  endfunction

  function in_tramp(input [31:0] addr);
    in_tramp = (addr >= TRAMP_BASE) && (addr < TRAMP_BASE + 16);
  endfunction

  function [7:0] mem_byte(input [31:0] addr);
    begin
      if (in_pmem(addr))
        mem_byte = pmem[addr - PMEM_BASE];
      else if (in_tramp(addr))
        mem_byte = tramp[addr[3:2]][8*addr[1:0] +: 8];
      else
        mem_byte = 8'h00;                    // UART 读 / 未映射：返回 0
    end
  endfunction

  function [31:0] mem_word(input [31:0] addr);
    begin
      mem_word = { mem_byte({addr[31:2], 2'b11}), mem_byte({addr[31:2], 2'b10}),
                   mem_byte({addr[31:2], 2'b01}), mem_byte({addr[31:2], 2'b00}) };
    end
  endfunction

  task wr_byte(input [31:0] addr, input [7:0] data);
    begin
      if (in_pmem(addr)) begin
        // ★ 含 X 的写数据按 0 落内存：
        //   verilator 流程的内存是二值的(未初始化即 0)，若这里原样存 X，之后读回就是 X，
        //   会把"程序存了一个未初始化值"这种无害情况放大成大面积 X 传播(踩过)。
        //   四值仿真要盯的是【触发器/控制路径】上的 X，所以这里把数据"确定化"。
        if (has_x32({24'b0, data}))
          pmem[addr - PMEM_BASE] <= 8'h00;
        else
          pmem[addr - PMEM_BASE] <= data;
      end
      else if (in_serial(addr)) begin
        $write("%c", data);                  // UART: 直接打到 stdout（CI 也按 stdout 判定）
        uart_bytes <= uart_bytes + 1;
      end
      // 其余写：忽略（本环境只实现了 pmem + UART）
    end
  endtask

  //------------------------------------------------------------------
  // AXI4 从机 —— 读通道（支持突发）
  //------------------------------------------------------------------
  reg        r_busy = 1'b0;
  reg [31:0] r_addr = 32'd0;
  reg [3:0]  r_id   = 4'd0;
  reg [8:0]  r_left = 9'd0;                  // 剩余拍数(含当前拍)
  wire       r_fire = io_master_rvalid && io_master_rready;

  assign io_master_arready = ~r_busy;
  assign io_master_rvalid  = r_busy;
  assign io_master_rdata   = mem_word(r_addr);
  assign io_master_rresp   = 2'b00;          // OKAY
  assign io_master_rlast   = r_busy && (r_left == 9'd1);
  assign io_master_rid     = r_id;

  always @(posedge clock) begin
    if (reset) begin
      r_busy <= 1'b0; r_left <= 9'd0; r_addr <= 32'd0; r_id <= 4'd0;
    end else begin
      if (!r_busy) begin
        if (io_master_arvalid) begin         // 读地址握手
          r_busy <= 1'b1;
          r_id   <= io_master_arid;
          r_addr <= {io_master_araddr[31:2], 2'b00};
          r_left <= {1'b0, io_master_arlen} + 9'd1;
          if (io_master_arburst != 2'b01 && !quiet)
            $display("[TB][WARN] 非 INCR 读突发 burst=%b addr=%h (按 INCR 处理)",
                     io_master_arburst, io_master_araddr);
          if (!quiet && !in_pmem(io_master_araddr) && !in_tramp(io_master_araddr) &&
              (io_master_araddr != last_unmapped) && (unmapped_prints < 32)) begin
            $display("[TB][READ] 未译码读 addr=%h (返回 0)", io_master_araddr);
            last_unmapped <= io_master_araddr;
            unmapped_prints <= unmapped_prints + 1;
          end
        end
      end else if (r_fire) begin
        if (r_left == 9'd1) begin
          r_busy <= 1'b0;
        end else begin
          r_left <= r_left - 9'd1;
          r_addr <= r_addr + 32'd4;          // 32 位数据宽度 ⇒ 每拍 +4
        end
      end
    end
  end

  //------------------------------------------------------------------
  // AXI4 从机 —— 写通道（支持突发 + WSTRB）
  //------------------------------------------------------------------
  localparam W_IDLE = 2'd0, W_DATA = 2'd1, W_RESP = 2'd2;
  reg [1:0]  w_state = W_IDLE;
  reg [31:0] w_addr  = 32'd0;
  reg [3:0]  w_id    = 4'd0;
  reg [8:0]  w_left  = 9'd0;

  assign io_master_awready = (w_state == W_IDLE);
  assign io_master_wready  = (w_state == W_DATA);
  assign io_master_bvalid  = (w_state == W_RESP);
  assign io_master_bresp   = 2'b00;          // OKAY
  assign io_master_bid     = w_id;

  integer k;
  always @(posedge clock) begin
    if (reset) begin
      w_state <= W_IDLE; w_addr <= 32'd0; w_id <= 4'd0; w_left <= 9'd0;
    end else begin
      case (w_state)
        W_IDLE: begin
          if (io_master_awvalid) begin
            w_state <= W_DATA;
            w_addr  <= io_master_awaddr;
            w_id    <= io_master_awid;
            w_left  <= {1'b0, io_master_awlen} + 9'd1;
            if (io_master_awburst != 2'b01 && !quiet)
              $display("[TB][WARN] 非 INCR 写突发 burst=%b addr=%h (按 INCR 处理)",
                       io_master_awburst, io_master_awaddr);
          end
        end
        W_DATA: begin
          if (io_master_wvalid) begin
            if (trace_uart && in_serial(w_addr))
              $display("[TB][UART-W] addr=%h strb=%b data=%h", w_addr, io_master_wstrb, io_master_wdata);
            // ★ 窄传输(sb/sh)时 AWADDR 是【字节地址】，数据在 WSTRB 对应的【字节道】上：
            //   字节 k 的落点 = 字对齐基址 + k，不是 AWADDR + k
            //   （写成 AWADDR+k 会把 sb 写到后面几字节上 —— 踩过，症状是程序数据莫名其妙错）
            for (k = 0; k < 4; k = k + 1) begin
              if (io_master_wstrb[k])
                wr_byte({w_addr[31:2], 2'b00} + k, io_master_wdata[8*k +: 8]);
            end
            w_addr <= {w_addr[31:2], 2'b00} + 32'd4;   // INCR: 32 位数据宽度 ⇒ 每拍 +4
            if (io_master_wlast || w_left == 9'd1) begin
              w_state <= W_RESP;
            end else begin
              w_left <= w_left - 9'd1;
            end
          end
        end
        W_RESP: begin
          if (io_master_bready)
            w_state <= W_IDLE;
        end
      endcase
    end
  end

  //------------------------------------------------------------------
  // 结束判定：RTL 仿真用层级引用看 "ebreak 提交"(LSU 是异常生效点)
  //   SPINAL_SIM_DEBUG=0 时 WBU 的 payload 已瘦身(没有 pc/instr)，所以不看 WBU
  //   网表仿真(-DIV_NETLIST)层级不存在，只能靠 watchdog
  //------------------------------------------------------------------
`ifdef IV_NETLIST
  wire lsu_commit_trap    = 1'b0;
  wire [3:0] lsu_exc_cause = 4'd0;
  wire [31:0] lsu_commit_pc = 32'd0;
`else
  wire lsu_commit_trap    = dut.lsu.io_input_valid &&
                            dut.lsu.io_input_payload_csrCtrl_trapEnter;
  wire [3:0] lsu_exc_cause = dut.lsu.io_input_payload_csrCtrl_excCause;
  wire [31:0] lsu_commit_pc = dut.lsu.io_input_payload_pc;
`endif

  // ---- 探针: 网表扁平化后仍保留带点的名字(iverilog 用 escaped 名引用) ----
  //   用来确认 arvalid/araddr 变 X 时, 到底是哪个状态寄存器先 X
`ifdef IV_NETLIST
  wire p_arb0=1'b0, p_arb1=1'b0, p_arb2=1'b0, p_rds0=1'b0, p_rds1=1'b0, p_wrs0=1'b0, p_wrs1=1'b0;  // 探针(已弃用: iverilog 绑不上带点的扁平名)
`else
  wire p_arb0 = 1'b0, p_arb1 = 1'b0, p_arb2 = 1'b0;
  wire p_rds0 = 1'b0, p_rds1 = 1'b0, p_wrs0 = 1'b0, p_wrs1 = 1'b0;
`endif

  //------------------------------------------------------------------
  // X 检查（+xcheck）：只看【顶层端口】有没有 X
  //   为什么只看端口：网表被 yosys flatten 过，层级引用不可靠；端口两边都有
  //   用途：快速判断"门级网表里是不是有 X 跑到控制路径上"
  //        （例如让命中比较变 X ⇒ 表现得像 cache 永远不命中 ⇒ 周期数暴涨）
  //------------------------------------------------------------------
  reg        xcheck = 1'b0;
  reg [63:0] x_cycles = 64'd0;
  reg        first_x_reported = 1'b0;
  reg        traffic = 1'b0;
  reg        vcdwin = 1'b0;
  reg [31:0] vcd_from = 32'd42000, vcd_to = 32'd42400;
  integer    late_x_prints = 0;
  reg        x_ar_reported = 1'b0;
  reg        x_aw_reported = 1'b0;
  reg        x_w_reported  = 1'b0;
  integer    x_report_period = 20000;

  wire x_now = has_x1(io_master_arvalid) || has_x1(io_master_awvalid) ||
               has_x1(io_master_wvalid)  || has_x1(io_master_bvalid)  ||
               has_x1(io_master_rvalid)  || has_x32(io_master_araddr) ||
               has_x32(io_master_awaddr) || has_x32(io_master_rdata)  ||
               has_x32(io_master_wdata);

  // AXI 事务计数（用来判断"是不是每拍做的事变少了"）
  reg [63:0] ar_fires = 64'd0;
  reg [63:0] aw_fires = 64'd0;
  reg [63:0] r_beats  = 64'd0;
  reg [63:0] w_beats  = 64'd0;
  // 写数据含 X 的统计: 用来量化"存未初始化值"这类事件的规模, 以及是否会打到设备(串口)
  integer    x_w_beats = 0;                 // 有效 W 拍里 wdata 含 X 的次数
  integer    x_w_dev   = 0;                 // 其中落到 UART(设备) 的次数 —— 这类才可能污染输出
  reg [31:0] x_w_first_addr = 32'hFFFF_FFFF;

  // ★ 用 negedge 采样：门级网表的组合逻辑在 posedge 那一刻还在收敛，
  //   在 posedge 采样会看到未稳定的值（delta-cycle 竞态）⇒ 会在 X 统计上产生假阳性
  always @(negedge clock) begin
    if (!reset && xcheck) begin
      if (x_now) begin
        x_cycles <= x_cycles + 64'd1;
        if (!first_x_reported) begin
          first_x_reported <= 1'b1;
          $display("[TB][XCHK] ★首次出现 X: cyc=%0d  X[arv awv wv rv bv araddr awaddr wdata rdata]=%b%b%b%b%b%b%b%b%b",
                   cycles,
                   has_x1(io_master_arvalid), has_x1(io_master_awvalid), has_x1(io_master_wvalid),
                   has_x1(io_master_rvalid),  has_x1(io_master_bvalid),  has_x32(io_master_araddr),
                   has_x32(io_master_awaddr), has_x32(io_master_wdata),  has_x32(io_master_rdata));
          $display("[TB][XCHK]   当时的值: arv=%b awv=%b wv=%b rv=%b bv=%b araddr=%h awaddr=%h wdata=%h rdata=%h",
                   io_master_arvalid, io_master_awvalid, io_master_wvalid, io_master_rvalid,
                   io_master_bvalid, io_master_araddr, io_master_awaddr,
                   io_master_wdata, io_master_rdata);
        end
        // 启动瞬态之后的 X 才是"有意义的 X"（空闲总线的 X 无害）⇒ 单独打几条
        else if (cycles > 2000 && late_x_prints < 24) begin
          late_x_prints <= late_x_prints + 1;
          $display("[TB][XCHK] X@cyc=%0d bitmap[arv awv wv rv bv araddr awaddr wdata rdata]=%b%b%b%b%b%b%b%b%b TB内部: r_busy=%b r_addr=%h r_left=%0d w_state=%0d w_addr=%h | 值: arv=%b arready=%b rv=%b araddr=%h rdata=%h",
                   cycles,
                   has_x1(io_master_arvalid), has_x1(io_master_awvalid), has_x1(io_master_wvalid),
                   has_x1(io_master_rvalid),  has_x1(io_master_bvalid),  has_x32(io_master_araddr),
                   has_x32(io_master_awaddr), has_x32(io_master_wdata),  has_x32(io_master_rdata),
                   r_busy, r_addr, r_left, w_state, w_addr,
                   io_master_arvalid, io_master_arready, io_master_rvalid,
                   io_master_araddr, io_master_rdata);
          $display("[TB][XCHK]   探针: arbiterState=%b%b%b readState=%b%b writeState=%b%b", p_arb2,p_arb1,p_arb0, p_rds1,p_rds0, p_wrs1,p_wrs0);
        end
      end
      if ((cycles % x_report_period) == 0)
        $display("[TB][XCHK] cyc=%0d X[arv awv wv rv bv araddr awaddr wdata rdata]=%b%b%b%b%b%b%b%b%b  累计=%0d  ar=%0d aw=%0d rbeat=%0d wbeat=%0d",
                 cycles,
                 has_x1(io_master_arvalid), has_x1(io_master_awvalid), has_x1(io_master_wvalid),
                 has_x1(io_master_rvalid),  has_x1(io_master_bvalid),  has_x32(io_master_araddr),
                 has_x32(io_master_awaddr), has_x32(io_master_wdata),  has_x32(io_master_rdata),
                 x_cycles, ar_fires, aw_fires, r_beats, w_beats);
    end
  end

  // AXI 事务计数（同样用 negedge 采样避免竞态）
  always @(negedge clock) begin
    if (!reset) begin
      if (io_master_arvalid && io_master_arready) begin
        ar_fires <= ar_fires + 64'd1;
        if (traffic) $display("[TB][AR] cyc=%0d addr=%h len=%0d", cycles, io_master_araddr, io_master_arlen);
        // ★ 有效读事务上出现 X 地址 —— 这就是"源头"(之后 TB 会把 X 地址锁存, 越传越远)
        if (!x_ar_reported && has_x32(io_master_araddr)) begin
          x_ar_reported <= 1'b1;
          $display("[TB][XCHK] ★★ 第一次【有效读事务上出现 X 地址】: cyc=%0d araddr=%h len=%0d arsize=%b arburst=%b",
                   cycles, io_master_araddr, io_master_arlen, io_master_arsize, io_master_arburst);
        end
`ifdef IV_NETLIST
        // ★ 网表模式收尾: 扁平的 yosys 网表里没有 lsu 这个实例, TB 无法用层次引用看
        //   ebreak 提交(见文件头/结束判定处的说明) ⇒ 改用行为签名:
        //   程序没写过 mtvec(保持复位值 0) 时, ebreak/trap 会跳到 0 ⇒ CPU 从地址 0 取指,
        //   这是一次"未译码读"。正常程序不会从 0 取指, 所以把它当作"程序已 trap 退出"。
        if (io_master_araddr == 32'h0000_0000) begin
          $display("");
          $display("================ [TB] 程序结束 (网表模式, 检测到 trap 到 0) ================");
          $display("  从地址 0 取指 ⇒ 程序已 trap 退出 (mtvec 未被程序写过, 保持复位值 0)");
          $display("  共 %0d 个周期, UART 输出 %0d 字节, AXI: ar=%0d aw=%0d rbeat=%0d wbeat=%0d",
                   cycles, uart_bytes, ar_fires, aw_fires, r_beats, w_beats);
          $display("==========================================================================");
          $finish;
        end
`endif
      end
      if (io_master_rvalid  && io_master_rready ) r_beats  <= r_beats  + 64'd1;
      if (io_master_awvalid && io_master_awready) begin
        aw_fires <= aw_fires + 64'd1;
        if (traffic) $display("[TB][AW] cyc=%0d addr=%h", cycles, io_master_awaddr);
        if (!x_aw_reported && has_x32(io_master_awaddr)) begin
          x_aw_reported <= 1'b1;
          $display("[TB][XCHK] ★★ 第一次【有效写事务上出现 X 地址】: cyc=%0d awaddr=%h len=%0d", cycles, io_master_awaddr, io_master_awlen);
        end
      end
      if (io_master_wvalid  && io_master_wready ) begin
        w_beats <= w_beats + 64'd1;
        if (has_x32(io_master_wdata)) begin
          x_w_beats <= x_w_beats + 1;
          if (in_serial({w_addr[31:2], 2'b00})) x_w_dev <= x_w_dev + 1;
          if (!x_w_reported) begin
            x_w_reported <= 1'b1;
            x_w_first_addr <= w_addr;
            $display("[TB][XCHK] ★★ 第一次【有效写数据上出现 X】: cyc=%0d addr=%h wdata=%h wstrb=%b",
                     cycles, w_addr, io_master_wdata, io_master_wstrb);
            if (in_serial({w_addr[31:2], 2'b00}))
              $display("[TB][XCHK]    ★ 这笔 X 写打到 UART 设备!");
          end
        end
      end
    end
  end

  //------------------------------------------------------------------
  // VCD 窗口 dump（+vcdwin）
  //   ★ 必须是"窗口开始那一拍才 $dumpfile/$dumpvars"。
  //     不能在 t=0 先 $dumpvars 再 $dumpoff —— iverilog 下那样只留下 t=0 的快照，抓不到窗口。
  //   +vcd_from=42000 +vcd_to=42400 可覆盖；默认 42000~42400。
  //------------------------------------------------------------------
  always @(posedge clock) begin
    if (vcdwin) begin
      if (cycles == vcd_from) begin
        $dumpfile("tb_npc_win.vcd");
        $dumpvars(0, dut);          // dump DUT 内部全部信号(网表已扁平化)
        if (!quiet) $display("[TB][VCD] 窗口开始: cyc=%0d", cycles);
      end
      if (cycles == vcd_to) begin
        $dumpoff;
        if (!quiet) $display("[TB][VCD] 窗口结束: cyc=%0d", cycles);
      end
    end
  end

  //------------------------------------------------------------------
  // 按 UART 字节数收尾（+uart_stop=N）
  //   网表模式看不到 ebreak, 又不想等几十分钟的 watchdog 时:
  //   程序输出总量已知(例: microbench = 539 字节)时, 用它精确收尾。
  //------------------------------------------------------------------
  always @(posedge clock) begin
    if (!reset && uart_stop != 32'd0 && uart_bytes >= uart_stop) begin
      $display("");
      $display("================ [TB] 程序结束 (按 +uart_stop=%0d) ================", uart_stop);
      $display("  UART 输出 %0d 字节, 共 %0d 个周期", uart_bytes, cycles);
      $display("  AXI 事务: ar=%0d aw=%0d rbeat=%0d wbeat=%0d", ar_fires, aw_fires, r_beats, w_beats);
      $display("=================================================================");
      $finish;
    end
  end

  //------------------------------------------------------------------
  // 主流程
  //------------------------------------------------------------------
  integer i;
  initial begin
    if ($test$plusargs("quiet"))      quiet = 1'b1;
    if ($test$plusargs("trace_uart")) trace_uart = 1'b1;
    if ($test$plusargs("xcheck"))     xcheck = 1'b1;
    if ($test$plusargs("traffic"))    traffic = 1'b1;
    if ($test$plusargs("vcdwin")) begin
      vcdwin = 1'b1;                            // 真正的 dump 放到窗口起始那一拍(见下面的 always 块)
      if ($value$plusargs("vcd_from=%d", vcd_from)) begin end
      if ($value$plusargs("vcd_to=%d",   vcd_to  )) begin end
    end
    if ($value$plusargs("max_cycles=%d", max_cycles)) begin end
    if ($value$plusargs("zero=%d", zero_size)) begin end
    if ($value$plusargs("img_size=%d", img_size)) begin end
    if ($value$plusargs("uart_stop=%d", uart_stop)) begin end
    // ★ 注意: iverilog 的 $test$plusargs 是【前缀匹配】, "vcdwin" 也满足 "vcd"
    //   ⇒ 必须显式排除, 否则 +vcdwin 会在这里先 $dumpvars(t=0), 窗口 $dumpfile 被忽略(踩过)
    if (!$test$plusargs("vcdwin") && $test$plusargs("vcd")) begin
      if ($value$plusargs("vcd_file=%s", dump_file)) begin end
      else dump_file = "tb_npc.vcd";
      $dumpfile(dump_file);
      $dumpvars(1, tb_npc);                  // 只 dump 顶层，全 dump 太大
      if (!quiet) $display("[TB] wave dump -> %0s", dump_file);
    end
    if (!$value$plusargs("img=%s", img_file)) begin
      $display("[TB][FATAL] 需要 +img=<hex 文件>（用 sim-iverilog/tools/bin2mem.sh 从 .bin 生成）");
      $finish;
    end

    // 装载镜像（$readmemh：每行一个字节的十六进制文本；下标 0 = 0x80000000）
    // ★ 顺序很重要：iverilog 的 $readmemh 会把数组里"文件没覆盖到的部分"也刷掉/置 X，
    //   所以必须【先装载、再清零镜像之外的部分】，否则 .bss/stack 区会读到 X（踩过）
    $readmemh(img_file, pmem);
    if (!quiet) $display("[TB] 已装载镜像 %0s (%0d 字节) -> pmem[0] (= 0x80000000)", img_file, img_size);

    // pmem 清零：verilator 流程的内存是 malloc 出来的(全 0)，这里显式清零以保持两条流程可比。
    // 注意 ① 只清【镜像之外】，不能把镜像本身清掉；
    //      ② 清零范围要盖住 .bss/stack（microbench 约 1MB；rtthread 的栈在 ~0x80a61000 ⇒ 约 11MB）
    //         ⇒ 默认 16MB；太大会拖慢启动，可用 +zero=<字节数> 调
    for (i = img_size; i < zero_size; i = i + 1)
      if (i < PMEM_SIZE) pmem[i] = 8'h00;

    // 存储器自检：确认"镜像内"有数据、"镜像外"是 0（排查 rdata 为什么是 X）
    if ($test$plusargs("memcheck")) begin
      $display("[TB][MEM] img_size=%0d zero_size=%0d | pmem[0]=%h pmem[%0d]=%h pmem[65520]=%h pmem[1000000]=%h",
               img_size, zero_size, pmem[0], img_size-1, pmem[img_size-1], pmem[65520], pmem[1000000]);
    end

    if (!quiet) $display("[TB] 复位中...（%0d 拍）", RESET_CYCLES);
    reset = 1'b1;
    repeat (RESET_CYCLES) @(posedge clock);
    @(negedge clock);
    reset = 1'b0;
    if (!quiet) $display("[TB] 复位释放，开始执行（watchdog = %0d 拍）", max_cycles);
  end

  // 结束 / 超时
  always @(posedge clock) begin
    if (!reset) begin
      // ebreak 提交 => 程序 halt
      if (lsu_commit_trap && lsu_exc_cause == 4'd3) begin
        $display("");
        $display("================ [TB] 程序结束 ================");
        $display("  ebreak 提交(cause=3) 于 pc=%h, 共 %0d 个周期", lsu_commit_pc, cycles);
        $display("  UART 输出 %0d 字节", uart_bytes);
        $display("  未译码访问: 读 %0d 次 / 写 %0d 次", unmapped_reads, unmapped_writes);
        if (xcheck) $display("  端口含 X 的周期数: %0d", x_cycles);
        else        $display("  端口含 X 的周期数: 未统计 (加 +xcheck 才打开 X 检查)");
        $display("  AXI 事务: ar=%0d aw=%0d rbeat=%0d wbeat=%0d", ar_fires, aw_fires, r_beats, w_beats);
        $display("  写数据含 X: %0d 拍 (其中打到 UART 设备: %0d 拍; 首次 addr=%h)",
                 x_w_beats, x_w_dev, x_w_first_addr);
        $display("===============================================");
        $finish;
      end
      if (cycles >= max_cycles) begin
        $display("");
        $display("================ [TB] watchdog 超时 ================");
        $display("  跑了 %0d 个周期仍未看到 ebreak", cycles);
        $display("  UART 输出 %0d 字节", uart_bytes);
        if (xcheck) $display("  端口含 X 的周期数: %0d", x_cycles);
        else        $display("  端口含 X 的周期数: 未统计 (加 +xcheck 才打开 X 检查)");
        $display("  AXI 事务: ar=%0d aw=%0d rbeat=%0d wbeat=%0d", ar_fires, aw_fires, r_beats, w_beats);
        $display("  写数据含 X: %0d 拍 (其中打到 UART 设备: %0d 拍; 首次 addr=%h)",
                 x_w_beats, x_w_dev, x_w_first_addr);
        $display("===================================================");
        $finish;
      end
    end
  end

  // 未译码访问计数
  always @(posedge clock) begin
    if (!reset) begin
      if (io_master_arvalid && io_master_arready &&
          !in_pmem(io_master_araddr) && !in_tramp(io_master_araddr))
        unmapped_reads <= unmapped_reads + 1;
      if (io_master_awvalid && io_master_awready &&
          !in_pmem(io_master_awaddr) && !in_serial(io_master_awaddr))
        unmapped_writes <= unmapped_writes + 1;
    end
  end

endmodule
