//=====================================================================
// tb_npc.v —— NPC 的 iverilog 仿真顶层（四值仿真 / 网表仿真共用）
//
// 作用：替代 verilator 流程里的 C++/DPI 环境
//   1) 时钟/复位：period 2ns；同步复位，保持 RESET_CYCLES=50 拍
//   2) 例化 ysyx_23060082（SPINAL_SIM_DEBUG=0 生成的那份，无 DPI 黑盒）
//   3) 行为级 AXI4 从机 + pmem（读支持突发；写按 WSTRB 逐字节落内存）
//   4) 地址译码只有三块：
//        0x80000000 起 128MB : pmem（镜像装在这里）
//        0x10000000          : UART 写 -> stdout
//        0x30000000 (16B)    : trampoline(lui t0,0x80000; jr t0)
//            存在的唯一原因：CI 要求复位 PC=0x30000000 而程序在 0x80000000
//        ★ CLINT 在 ysyx_23060082 内部（Xbar 路由 0x02000000），TB 不需要实现
//   5) 结束条件（RTL/网表通用，判据只用顶层 io_master_* 与 TB 自己的 UART 计数）
//        E1: 从地址 0 取指         ⇒ 程序 trap 到 mtvec=0, 视为已退出（microbench）
//        E3: UART 出现 "AM Panic:" ⇒ 程序 panic 退出（rtthread）
//        RTL 额外加速: 层级引用看 LSU 的 ebreak 提交, 早几拍停并打印 pc
//          （网表被 flatten 后没有 lsu 实例 ⇒ 该信号恒 0, 自动只用 E1/E3；
//            +no_ebreak_stop 可关掉它, 让 RTL 与网表用完全相同的判据）
//        兜底: watchdog（+max_cycles=）
//
// 用法：vvp sim.vvp +img=xxx.hex [选项]   （由 make sim-iverilog[-netlist] 调用）
//   +img=<hex>      必需；每行一个字节的十六进制文本（sim-iverilog/tools/bin2mem.sh 生成）
//   +img_size=<N>   镜像字节数（区分"镜像区"和"清零区"）
//   +zero=<N>       启动清零字节数（模拟 malloc 的零内存；要盖住 .bss/stack，默认 16MB）
//   +max_cycles=<N> watchdog 上限
//   +no_ebreak_stop 关掉 RTL 专用的 ebreak 提前收尾
//   +xcheck         统计/报告端口 X（首次、每 x_report_period 拍汇总）
//   +traffic        打印每次 AR/AW
//   +memcheck       装载后自检 pmem
//   +trace_uart     打印每次 UART 写
//   +quiet          安静模式
//=====================================================================
`timescale 1ns/1ps

module tb_npc;

  localparam PMEM_BASE   = 32'h8000_0000;
  localparam PMEM_SIZE   = 32'h0800_0000;    // 128MB
  localparam SERIAL_BASE = 32'h1000_0000;
  localparam TRAMP_BASE  = 32'h3000_0000;

  // E3 终止串: CI 只跑 microbench / rtthread 两个 bin —— microbench 走 E1(取指 0),
  //   rtthread 跑完自动 microbench 后会在 msh 里 panic, 输出该串 ⇒ 用它收尾
  localparam PANIC_STR = "AM Panic:";

  parameter RESET_CYCLES       = 50;
  parameter DEFAULT_MAX_CYCLES = 4_000_000;  // microbench 实测约 105 万拍(网表很慢, 请显式给 IV_MAXCYC=)
  parameter DEFAULT_ZERO       = 1 << 24;    // 16MB

  reg clock = 1'b0;
  reg reset = 1'b1;
  always #1 clock = ~clock;

  // pmem：字节粒度，下标 = 地址 - 0x80000000
  reg [7:0] pmem [0:PMEM_SIZE-1];

  // trampoline: lui t0,0x80000 ; jr t0 ; nop ; nop
  reg [31:0] tramp [0:3];
  initial begin
    tramp[0] = 32'h8000_02B7;
    tramp[1] = 32'h0002_8067;
    tramp[2] = 32'h0000_0013;
    tramp[3] = 32'h0000_0013;
  end

  //------------------------------------------------------------------
  // DUT
  //------------------------------------------------------------------
  wire        io_master_awvalid, io_master_awready;
  wire [31:0] io_master_awaddr;
  wire [3:0]  io_master_awid;
  wire [7:0]  io_master_awlen;
  wire [2:0]  io_master_awsize;
  wire [1:0]  io_master_awburst;
  wire        io_master_wvalid, io_master_wready, io_master_wlast;
  wire [31:0] io_master_wdata;
  wire [3:0]  io_master_wstrb;
  wire        io_master_bvalid, io_master_bready;
  wire [3:0]  io_master_bid;
  wire [1:0]  io_master_bresp;
  wire        io_master_arvalid, io_master_arready;
  wire [31:0] io_master_araddr;
  wire [3:0]  io_master_arid;
  wire [7:0]  io_master_arlen;
  wire [2:0]  io_master_arsize;
  wire [1:0]  io_master_arburst;
  wire        io_master_rvalid, io_master_rready, io_master_rlast;
  wire [31:0] io_master_rdata;
  wire [3:0]  io_master_rid;
  wire [1:0]  io_master_rresp;

  ysyx_23060082 dut (
    .clock             (clock            ),
    .reset             (reset            ),
    .io_interrupt      (1'b0             ),
    // 主设备口（CPU -> TB 的 AXI 从机）
    .io_master_awvalid (io_master_awvalid), .io_master_awready(io_master_awready),
    .io_master_awaddr  (io_master_awaddr ), .io_master_awid   (io_master_awid   ),
    .io_master_awlen   (io_master_awlen  ), .io_master_awsize (io_master_awsize ),
    .io_master_awburst (io_master_awburst),
    .io_master_wvalid  (io_master_wvalid ), .io_master_wready (io_master_wready ),
    .io_master_wdata   (io_master_wdata  ), .io_master_wstrb  (io_master_wstrb  ),
    .io_master_wlast   (io_master_wlast  ),
    .io_master_bvalid  (io_master_bvalid ), .io_master_bready (io_master_bready ),
    .io_master_bid     (io_master_bid    ), .io_master_bresp  (io_master_bresp  ),
    .io_master_arvalid (io_master_arvalid), .io_master_arready(io_master_arready),
    .io_master_araddr  (io_master_araddr ), .io_master_arid   (io_master_arid   ),
    .io_master_arlen   (io_master_arlen  ), .io_master_arsize (io_master_arsize ),
    .io_master_arburst (io_master_arburst),
    .io_master_rvalid  (io_master_rvalid ), .io_master_rready (io_master_rready ),
    .io_master_rdata   (io_master_rdata  ), .io_master_rid    (io_master_rid    ),
    .io_master_rresp   (io_master_rresp  ), .io_master_rlast  (io_master_rlast  ),
    // 从设备口（本环境不使用，必须给确定值）
    .io_slave_awvalid  (1'b0), .io_slave_awready(), .io_slave_awaddr (32'b0),
    .io_slave_awid     (4'b0), .io_slave_awlen  (8'b0), .io_slave_awsize (3'b0),
    .io_slave_awburst  (2'b0),
    .io_slave_wvalid   (1'b0), .io_slave_wready (), .io_slave_wdata  (32'b0),
    .io_slave_wstrb    (4'b0), .io_slave_wlast  (1'b0),
    .io_slave_bvalid   (    ), .io_slave_bready (1'b1), .io_slave_bid    (    ),
    .io_slave_bresp    (    ),
    .io_slave_arvalid  (1'b0), .io_slave_arready(), .io_slave_araddr (32'b0),
    .io_slave_arid     (4'b0), .io_slave_arlen  (8'b0), .io_slave_arsize (3'b0),
    .io_slave_arburst  (2'b0),
    .io_slave_rvalid   (    ), .io_slave_rready (1'b1), .io_slave_rdata  (    ),
    .io_slave_rid      (    ), .io_slave_rresp  (    ), .io_slave_rlast  (    )
  );

  //------------------------------------------------------------------
  // 统计 / 状态
  //------------------------------------------------------------------
  reg [63:0] cycles     = 64'd0;
  reg [63:0] max_cycles = DEFAULT_MAX_CYCLES;
  reg [31:0] zero_size  = DEFAULT_ZERO;
  reg [31:0] img_size   = 32'd0;
  reg        ebreak_stop = 1'b1;
  integer    uart_bytes = 0;
  integer    unmapped_reads = 0;
  integer    unmapped_writes = 0;
  integer    unmapped_prints = 0;
  reg [31:0] last_unmapped = 32'hFFFF_FFFF;
  reg [1023:0] img_file;
  reg quiet = 1'b0, trace_uart = 1'b0;
  reg [8*9-1:0] uart_tail = 72'd0;           // 最近 9 个 UART 字节（给 E3 匹配 "AM Panic:"）

  always @(posedge clock) cycles <= cycles + 64'd1;

  //------------------------------------------------------------------
  // 地址译码 / 存储器
  //------------------------------------------------------------------
  function has_x32(input [31:0] v); has_x32 = (^v === 1'bx); endfunction
  function has_x1 (input        v); has_x1  = (^v === 1'bx); endfunction

  function in_pmem  (input [31:0] a); in_pmem   = (a >= PMEM_BASE)   && (a < PMEM_BASE + PMEM_SIZE); endfunction
  function in_serial(input [31:0] a); in_serial = (a >= SERIAL_BASE) && (a < SERIAL_BASE + 8);        endfunction
  function in_tramp (input [31:0] a); in_tramp  = (a >= TRAMP_BASE)  && (a < TRAMP_BASE + 16);        endfunction

  function [7:0] mem_byte(input [31:0] addr);
    if      (in_pmem(addr))  mem_byte = pmem[addr - PMEM_BASE];
    else if (in_tramp(addr)) mem_byte = tramp[addr[3:2]][8*addr[1:0] +: 8];
    else                     mem_byte = 8'h00;          // UART 读 / 未映射
  endfunction

  function [31:0] mem_word(input [31:0] addr);
    mem_word = { mem_byte({addr[31:2], 2'b11}), mem_byte({addr[31:2], 2'b10}),
                 mem_byte({addr[31:2], 2'b01}), mem_byte({addr[31:2], 2'b00}) };
  endfunction

  task wr_byte(input [31:0] addr, input [7:0] data);
    if (in_pmem(addr)) begin
      // ★ 含 X 的写数据按 0 落内存：verilator 流程的内存是二值的(未初始化即 0)，
      //   若原样存 X，之后读回就是 X，会把"存未初始化值"这种无害情况放大成大面积 X 传播。
      if (has_x32({24'b0, data})) pmem[addr - PMEM_BASE] <= 8'h00;
      else                        pmem[addr - PMEM_BASE] <= data;
    end
    else if (in_serial(addr)) begin
      $write("%c", data);                               // UART: 直接打到 stdout
      uart_bytes <= uart_bytes + 1;
      uart_tail  <= {uart_tail[8*9-1-8:0], data};       // 给 E3 保留最近 9 个字节
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
    end else if (!r_busy) begin
      if (io_master_arvalid) begin
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
          last_unmapped  <= io_master_araddr;
          unmapped_prints <= unmapped_prints + 1;
        end
      end
    end else if (r_fire) begin
      if (r_left == 9'd1) r_busy <= 1'b0;
      else begin
        r_left <= r_left - 9'd1;
        r_addr <= r_addr + 32'd4;              // 32 位数据宽度 ⇒ 每拍 +4
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
  integer    k;

  assign io_master_awready = (w_state == W_IDLE);
  assign io_master_wready  = (w_state == W_DATA);
  assign io_master_bvalid  = (w_state == W_RESP);
  assign io_master_bresp   = 2'b00;          // OKAY
  assign io_master_bid     = w_id;

  always @(posedge clock) begin
    if (reset) begin
      w_state <= W_IDLE; w_addr <= 32'd0; w_id <= 4'd0; w_left <= 9'd0;
    end else case (w_state)
      W_IDLE: if (io_master_awvalid) begin
        w_state <= W_DATA;
        w_addr  <= io_master_awaddr;
        w_id    <= io_master_awid;
        w_left  <= {1'b0, io_master_awlen} + 9'd1;
        if (io_master_awburst != 2'b01 && !quiet)
          $display("[TB][WARN] 非 INCR 写突发 burst=%b addr=%h (按 INCR 处理)",
                   io_master_awburst, io_master_awaddr);
      end
      W_DATA: if (io_master_wvalid) begin
        if (trace_uart && in_serial(w_addr))
          $display("[TB][UART-W] addr=%h strb=%b data=%h", w_addr, io_master_wstrb, io_master_wdata);
        // ★ 窄传输(sb/sh)时 AWADDR 是【字节地址】，数据在 WSTRB 对应的【字节道】上：
        //   字节 k 的落点 = 字对齐基址 + k，不是 AWADDR + k（写成后者会把 sb 写歪 —— 踩过）
        for (k = 0; k < 4; k = k + 1)
          if (io_master_wstrb[k])
            wr_byte({w_addr[31:2], 2'b00} + k, io_master_wdata[8*k +: 8]);
        w_addr <= {w_addr[31:2], 2'b00} + 32'd4;   // INCR: 每拍 +4
        if (io_master_wlast || w_left == 9'd1) w_state <= W_RESP;
        else                                   w_left  <= w_left - 9'd1;
      end
      W_RESP: if (io_master_bready) w_state <= W_IDLE;
      default: ;
    endcase
  end

  //------------------------------------------------------------------
  // 结束判定：RTL 专用的 ebreak 提前收尾（网表 flatten 后没有 lsu 实例 ⇒ 恒 0）
  //   通用判据 E1/E3 见下面的 negedge 块与 E3 块
  //------------------------------------------------------------------
`ifdef IV_NETLIST
  wire        lsu_commit_trap = 1'b0;
  wire [3:0]  lsu_exc_cause   = 4'd0;
  wire [31:0] lsu_commit_pc   = 32'd0;
`else
  wire        lsu_commit_trap = dut.lsu.io_input_valid &&
                                dut.lsu.io_input_payload_csrCtrl_trapEnter;
  wire [3:0]  lsu_exc_cause   = dut.lsu.io_input_payload_csrCtrl_excCause;
  wire [31:0] lsu_commit_pc   = dut.lsu.io_input_payload_pc;
`endif

  //------------------------------------------------------------------
  // X 检查 / AXI 计数 / 结束判据 E1
  //   ★ negedge 采样：门级网表在 posedge 那一刻组合逻辑还在收敛，posedge 采样有假阳性
  //------------------------------------------------------------------
  // 端口 X 位图：[arv awv wv rv bv araddr awaddr wdata rdata]
  wire [8:0] x_bits = { has_x1(io_master_arvalid), has_x1(io_master_awvalid),
                        has_x1(io_master_wvalid),  has_x1(io_master_rvalid),
                        has_x1(io_master_bvalid),  has_x32(io_master_araddr),
                        has_x32(io_master_awaddr), has_x32(io_master_wdata),
                        has_x32(io_master_rdata) };
  wire x_now = |x_bits;

  reg        xcheck = 1'b0, traffic = 1'b0;
  reg        first_x_reported = 1'b0;
  reg        x_ar_reported = 1'b0, x_aw_reported = 1'b0, x_w_reported = 1'b0;
  integer    late_x_prints = 0;
  integer    x_report_period = 20000;
  reg [63:0] x_cycles = 64'd0;
  reg [63:0] ar_fires = 64'd0, aw_fires = 64'd0, r_beats = 64'd0, w_beats = 64'd0;
  integer    x_w_beats = 0;                  // 有效 W 拍里 wdata 含 X 的次数
  integer    x_w_dev   = 0;                  // 其中落到 UART(设备) 的次数
  reg [31:0] x_w_first_addr = 32'hFFFF_FFFF;

  always @(negedge clock) begin
    if (!reset) begin
      if (io_master_arvalid && io_master_arready) begin
        ar_fires <= ar_fires + 64'd1;
        if (traffic) $display("[TB][AR] cyc=%0d addr=%h len=%0d", cycles, io_master_araddr, io_master_arlen);
        if (!x_ar_reported && has_x32(io_master_araddr)) begin
          x_ar_reported <= 1'b1;
          $display("[TB][XCHK] ★★ 第一次【有效读事务上出现 X 地址】: cyc=%0d araddr=%h len=%0d arsize=%b arburst=%b",
                   cycles, io_master_araddr, io_master_arlen, io_master_arsize, io_master_arburst);
        end
        // E1: 程序 ebreak 且 mtvec=0 ⇒ 从地址 0 取指（正常程序不会），视为已退出
        if (io_master_araddr == 32'h0000_0000) begin
          $display("");
          $display("================ [TB] 程序结束 (检测到 trap 到 0) ================");
          $display("  E1: 从地址 0 取指 ⇒ 程序已 trap 退出 (mtvec 未被程序写过, 保持复位值 0)");
          $display("  共 %0d 个周期, UART 输出 %0d 字节, AXI: ar=%0d aw=%0d rbeat=%0d wbeat=%0d",
                   cycles, uart_bytes, ar_fires, aw_fires, r_beats, w_beats);
          $display("==============================================================");
          $finish;
        end
      end
      if (io_master_rvalid && io_master_rready) r_beats <= r_beats + 64'd1;
      if (io_master_awvalid && io_master_awready) begin
        aw_fires <= aw_fires + 64'd1;
        if (traffic) $display("[TB][AW] cyc=%0d addr=%h", cycles, io_master_awaddr);
        if (!x_aw_reported && has_x32(io_master_awaddr)) begin
          x_aw_reported <= 1'b1;
          $display("[TB][XCHK] ★★ 第一次【有效写事务上出现 X 地址】: cyc=%0d awaddr=%h len=%0d",
                   cycles, io_master_awaddr, io_master_awlen);
        end
      end
      if (io_master_wvalid && io_master_wready) begin
        w_beats <= w_beats + 64'd1;
        if (has_x32(io_master_wdata)) begin
          x_w_beats <= x_w_beats + 1;
          if (in_serial({w_addr[31:2], 2'b00})) x_w_dev <= x_w_dev + 1;
          if (!x_w_reported) begin
            x_w_reported    <= 1'b1;
            x_w_first_addr  <= w_addr;
            $display("[TB][XCHK] ★★ 第一次【有效写数据上出现 X】: cyc=%0d addr=%h wdata=%h wstrb=%b",
                     cycles, w_addr, io_master_wdata, io_master_wstrb);
            if (in_serial({w_addr[31:2], 2'b00}))
              $display("[TB][XCHK]    ★ 这笔 X 写打到 UART 设备!");
          end
        end
      end
      if (xcheck) begin
        if (x_now) begin
          x_cycles <= x_cycles + 64'd1;
          if (!first_x_reported) begin
            first_x_reported <= 1'b1;
            $display("[TB][XCHK] ★首次出现 X: cyc=%0d  X[arv awv wv rv bv araddr awaddr wdata rdata]=%b", cycles, x_bits);
            $display("[TB][XCHK]   当时的值: arv=%b awv=%b wv=%b rv=%b bv=%b araddr=%h awaddr=%h wdata=%h rdata=%h",
                     io_master_arvalid, io_master_awvalid, io_master_wvalid, io_master_rvalid,
                     io_master_bvalid, io_master_araddr, io_master_awaddr, io_master_wdata, io_master_rdata);
          end
          // 启动瞬态之后的 X 才"有意义"（空闲总线的 X 无害）⇒ 单独打几条
          else if (cycles > 2000 && late_x_prints < 24) begin
            late_x_prints <= late_x_prints + 1;
            $display("[TB][XCHK] X@cyc=%0d bitmap=%b TB内部: r_busy=%b r_addr=%h r_left=%0d w_state=%0d w_addr=%h",
                     cycles, x_bits, r_busy, r_addr, r_left, w_state, w_addr);
          end
        end
        if ((cycles % x_report_period) == 0)
          $display("[TB][XCHK] cyc=%0d X=%b 累计=%0d  ar=%0d aw=%0d rbeat=%0d wbeat=%0d",
                   cycles, x_bits, x_cycles, ar_fires, aw_fires, r_beats, w_beats);
      end
    end
  end

  //------------------------------------------------------------------
  // (临时调试) VCD 窗口
  //------------------------------------------------------------------
  reg vcdwin = 1'b0;
  reg [31:0] vcd_from = 32'd0, vcd_to = 32'd0;
  always @(posedge clock) begin
    if (vcdwin) begin
      if (cycles == vcd_from) begin $dumpfile("tb_npc_win.vcd"); $dumpvars(0, dut); end
      if (cycles == vcd_to)   $dumpoff;
    end
  end

  //------------------------------------------------------------------
  // 结束判据 E3：UART 输出里出现 PANIC_STR（rtthread 跑完自动 microbench 后 panic）
  //------------------------------------------------------------------
  always @(posedge clock) begin
    if (!reset && uart_tail == PANIC_STR) begin
      $display("");
      $display("================ [TB] 程序结束 (UART 出现 \"%0s\") ================", PANIC_STR);
      $display("  UART 输出 %0d 字节, 共 %0d 个周期", uart_bytes, cycles);
      $display("  AXI 事务: ar=%0d aw=%0d rbeat=%0d wbeat=%0d", ar_fires, aw_fires, r_beats, w_beats);
      $display("================================================================");
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
    if ($test$plusargs("no_ebreak_stop")) ebreak_stop = 1'b0;
    if ($test$plusargs("vcdwin")) begin
      vcdwin = 1'b1;
      if ($value$plusargs("vcd_from=%d", vcd_from)) begin end
      if ($value$plusargs("vcd_to=%d",   vcd_to  )) begin end
    end
    if ($value$plusargs("max_cycles=%d", max_cycles)) begin end
    if ($value$plusargs("zero=%d",       zero_size )) begin end
    if ($value$plusargs("img_size=%d",   img_size  )) begin end
    if (!$value$plusargs("img=%s", img_file)) begin
      $display("[TB][FATAL] 需要 +img=<hex 文件>（用 sim-iverilog/tools/bin2mem.sh 从 .bin 生成）");
      $finish;
    end

    // ★ 顺序很重要：iverilog 的 $readmemh 会把文件没覆盖到的部分也置 X，
    //   所以必须【先装载、再清零镜像之外】，否则 .bss/stack 区会读到 X（踩过）
    $readmemh(img_file, pmem);
    if (!quiet) $display("[TB] 已装载镜像 %0s (%0d 字节) -> pmem[0] (= 0x80000000)", img_file, img_size);

    // 清零"镜像之外"（模拟 verilator 流程 malloc 出来的零内存）。
    // 范围要盖住 .bss/stack：microbench 约 1MB；rtthread 栈在 ~0x80a61000 ⇒ 约 11MB ⇒ 默认 16MB
    for (i = img_size; i < zero_size; i = i + 1)
      if (i < PMEM_SIZE) pmem[i] = 8'h00;

    if ($test$plusargs("memcheck"))
      $display("[TB][MEM] img_size=%0d zero_size=%0d | pmem[0]=%h pmem[%0d]=%h pmem[65520]=%h pmem[1000000]=%h",
               img_size, zero_size, pmem[0], img_size-1, pmem[img_size-1], pmem[65520], pmem[1000000]);

    if (!quiet) $display("[TB] 复位中...（%0d 拍）", RESET_CYCLES);
    reset = 1'b1;
    repeat (RESET_CYCLES) @(posedge clock);
    @(negedge clock);
    reset = 1'b0;
    if (!quiet) $display("[TB] 复位释放，开始执行（watchdog = %0d 拍）", max_cycles);
  end

  //------------------------------------------------------------------
  // 结束 / 超时汇总
  //------------------------------------------------------------------
  always @(posedge clock) begin
    if (!reset) begin
      // RTL 专用：ebreak 提交（网表里该信号恒 0，见上面的 ifdef）
      if (ebreak_stop && lsu_commit_trap && lsu_exc_cause == 4'd3) begin
        $display("");
        $display("================ [TB] 程序结束 ================");
        $display("  ebreak 提交(cause=3) 于 pc=%h, 共 %0d 个周期", lsu_commit_pc, cycles);
        $display("  UART 输出 %0d 字节", uart_bytes);
        $display("  未译码访问: 读 %0d 次 / 写 %0d 次", unmapped_reads, unmapped_writes);
        report_x();
        $display("===============================================");
        $finish;
      end 
      if (cycles >= max_cycles) begin
        $display("");
        $display("================ [TB] watchdog 超时 ================");
        $display("  跑了 %0d 个周期仍未看到 ebreak", cycles);
        $display("  UART 输出 %0d 字节", uart_bytes);
        report_x();
        $display("===================================================");
        $finish;
      end
    end
  end

  task report_x;
    begin
      if (xcheck) $display("  端口含 X 的周期数: %0d", x_cycles);
      else        $display("  端口含 X 的周期数: 未统计 (加 +xcheck 才打开 X 检查)");
      $display("  AXI 事务: ar=%0d aw=%0d rbeat=%0d wbeat=%0d", ar_fires, aw_fires, r_beats, w_beats);
      $display("  写数据含 X: %0d 拍 (其中打到 UART 设备: %0d 拍; 首次 addr=%h)",
               x_w_beats, x_w_dev, x_w_first_addr);
    end
  endtask

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
