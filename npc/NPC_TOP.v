// Generator : SpinalHDL v1.12.3    git head : 591e64062329e5e2e2b81f4d52422948053edb97
// Component : NPC_TOP
// Git hash  : 8840a3aa353005d431dcd191de969b71ad1610d0
// Date      : 12/09/2026, 20:06:31

`timescale 1ns/1ps

module NPC_TOP (
  input  wire          reset,
  input  wire          clock
);

  wire                cpu_io_master_arvalid;
  wire       [31:0]   cpu_io_master_araddr;
  wire       [3:0]    cpu_io_master_arid;
  wire       [7:0]    cpu_io_master_arlen;
  wire       [2:0]    cpu_io_master_arsize;
  wire       [1:0]    cpu_io_master_arburst;
  wire                cpu_io_master_awvalid;
  wire       [31:0]   cpu_io_master_awaddr;
  wire       [3:0]    cpu_io_master_awid;
  wire       [7:0]    cpu_io_master_awlen;
  wire       [2:0]    cpu_io_master_awsize;
  wire       [1:0]    cpu_io_master_awburst;
  wire                cpu_io_master_wvalid;
  wire       [31:0]   cpu_io_master_wdata;
  wire       [3:0]    cpu_io_master_wstrb;
  wire                cpu_io_master_wlast;
  wire                cpu_io_master_rready;
  wire                cpu_io_master_bready;
  wire                cpu_io_slave_arready;
  wire                cpu_io_slave_awready;
  wire                cpu_io_slave_wready;
  wire                cpu_io_slave_rvalid;
  wire       [31:0]   cpu_io_slave_rdata;
  wire       [3:0]    cpu_io_slave_rid;
  wire       [1:0]    cpu_io_slave_rresp;
  wire                cpu_io_slave_rlast;
  wire                cpu_io_slave_bvalid;
  wire       [3:0]    cpu_io_slave_bid;
  wire       [1:0]    cpu_io_slave_bresp;
  wire                axi4MemSlave_axi4_arready;
  wire                axi4MemSlave_axi4_awready;
  wire                axi4MemSlave_axi4_wready;
  wire                axi4MemSlave_axi4_rvalid;
  wire       [31:0]   axi4MemSlave_axi4_rdata;
  wire       [3:0]    axi4MemSlave_axi4_rid;
  wire       [1:0]    axi4MemSlave_axi4_rresp;
  wire                axi4MemSlave_axi4_rlast;
  wire                axi4MemSlave_axi4_bvalid;
  wire       [3:0]    axi4MemSlave_axi4_bid;
  wire       [1:0]    axi4MemSlave_axi4_bresp;

  ysyx_23060082 cpu (
    .io_interrupt      (                             ), //i
    .io_master_awvalid (cpu_io_master_awvalid        ), //o
    .io_master_awready (axi4MemSlave_axi4_awready    ), //i
    .io_master_awaddr  (cpu_io_master_awaddr[31:0]   ), //o
    .io_master_awid    (cpu_io_master_awid[3:0]      ), //o
    .io_master_awlen   (cpu_io_master_awlen[7:0]     ), //o
    .io_master_awsize  (cpu_io_master_awsize[2:0]    ), //o
    .io_master_awburst (cpu_io_master_awburst[1:0]   ), //o
    .io_master_wvalid  (cpu_io_master_wvalid         ), //o
    .io_master_wready  (axi4MemSlave_axi4_wready     ), //i
    .io_master_wdata   (cpu_io_master_wdata[31:0]    ), //o
    .io_master_wstrb   (cpu_io_master_wstrb[3:0]     ), //o
    .io_master_wlast   (cpu_io_master_wlast          ), //o
    .io_master_bvalid  (axi4MemSlave_axi4_bvalid     ), //i
    .io_master_bready  (cpu_io_master_bready         ), //o
    .io_master_bid     (axi4MemSlave_axi4_bid[3:0]   ), //i
    .io_master_bresp   (axi4MemSlave_axi4_bresp[1:0] ), //i
    .io_master_arvalid (cpu_io_master_arvalid        ), //o
    .io_master_arready (axi4MemSlave_axi4_arready    ), //i
    .io_master_araddr  (cpu_io_master_araddr[31:0]   ), //o
    .io_master_arid    (cpu_io_master_arid[3:0]      ), //o
    .io_master_arlen   (cpu_io_master_arlen[7:0]     ), //o
    .io_master_arsize  (cpu_io_master_arsize[2:0]    ), //o
    .io_master_arburst (cpu_io_master_arburst[1:0]   ), //o
    .io_master_rvalid  (axi4MemSlave_axi4_rvalid     ), //i
    .io_master_rready  (cpu_io_master_rready         ), //o
    .io_master_rdata   (axi4MemSlave_axi4_rdata[31:0]), //i
    .io_master_rid     (axi4MemSlave_axi4_rid[3:0]   ), //i
    .io_master_rresp   (axi4MemSlave_axi4_rresp[1:0] ), //i
    .io_master_rlast   (axi4MemSlave_axi4_rlast      ), //i
    .io_slave_awvalid  (                             ), //i
    .io_slave_awready  (cpu_io_slave_awready         ), //o
    .io_slave_awaddr   (                             ), //i
    .io_slave_awid     (                             ), //i
    .io_slave_awlen    (                             ), //i
    .io_slave_awsize   (                             ), //i
    .io_slave_awburst  (                             ), //i
    .io_slave_wvalid   (                             ), //i
    .io_slave_wready   (cpu_io_slave_wready          ), //o
    .io_slave_wdata    (                             ), //i
    .io_slave_wstrb    (                             ), //i
    .io_slave_wlast    (                             ), //i
    .io_slave_bvalid   (cpu_io_slave_bvalid          ), //o
    .io_slave_bready   (                             ), //i
    .io_slave_bid      (cpu_io_slave_bid[3:0]        ), //o
    .io_slave_bresp    (cpu_io_slave_bresp[1:0]      ), //o
    .io_slave_arvalid  (                             ), //i
    .io_slave_arready  (cpu_io_slave_arready         ), //o
    .io_slave_araddr   (                             ), //i
    .io_slave_arid     (                             ), //i
    .io_slave_arlen    (                             ), //i
    .io_slave_arsize   (                             ), //i
    .io_slave_arburst  (                             ), //i
    .io_slave_rvalid   (cpu_io_slave_rvalid          ), //o
    .io_slave_rready   (                             ), //i
    .io_slave_rdata    (cpu_io_slave_rdata[31:0]     ), //o
    .io_slave_rid      (cpu_io_slave_rid[3:0]        ), //o
    .io_slave_rresp    (cpu_io_slave_rresp[1:0]      ), //o
    .io_slave_rlast    (cpu_io_slave_rlast           ), //o
    .reset             (reset                        ), //i
    .clock             (clock                        )  //i
  );
  ysyx_23060082_Axi4MemSlave axi4MemSlave (
    .axi4_awvalid (cpu_io_master_awvalid        ), //i
    .axi4_awready (axi4MemSlave_axi4_awready    ), //o
    .axi4_awaddr  (cpu_io_master_awaddr[31:0]   ), //i
    .axi4_awid    (cpu_io_master_awid[3:0]      ), //i
    .axi4_awlen   (cpu_io_master_awlen[7:0]     ), //i
    .axi4_awsize  (cpu_io_master_awsize[2:0]    ), //i
    .axi4_awburst (cpu_io_master_awburst[1:0]   ), //i
    .axi4_wvalid  (cpu_io_master_wvalid         ), //i
    .axi4_wready  (axi4MemSlave_axi4_wready     ), //o
    .axi4_wdata   (cpu_io_master_wdata[31:0]    ), //i
    .axi4_wstrb   (cpu_io_master_wstrb[3:0]     ), //i
    .axi4_wlast   (cpu_io_master_wlast          ), //i
    .axi4_bvalid  (axi4MemSlave_axi4_bvalid     ), //o
    .axi4_bready  (cpu_io_master_bready         ), //i
    .axi4_bid     (axi4MemSlave_axi4_bid[3:0]   ), //o
    .axi4_bresp   (axi4MemSlave_axi4_bresp[1:0] ), //o
    .axi4_arvalid (cpu_io_master_arvalid        ), //i
    .axi4_arready (axi4MemSlave_axi4_arready    ), //o
    .axi4_araddr  (cpu_io_master_araddr[31:0]   ), //i
    .axi4_arid    (cpu_io_master_arid[3:0]      ), //i
    .axi4_arlen   (cpu_io_master_arlen[7:0]     ), //i
    .axi4_arsize  (cpu_io_master_arsize[2:0]    ), //i
    .axi4_arburst (cpu_io_master_arburst[1:0]   ), //i
    .axi4_rvalid  (axi4MemSlave_axi4_rvalid     ), //o
    .axi4_rready  (cpu_io_master_rready         ), //i
    .axi4_rdata   (axi4MemSlave_axi4_rdata[31:0]), //o
    .axi4_rid     (axi4MemSlave_axi4_rid[3:0]   ), //o
    .axi4_rresp   (axi4MemSlave_axi4_rresp[1:0] ), //o
    .axi4_rlast   (axi4MemSlave_axi4_rlast      ), //o
    .reset        (reset                        ), //i
    .clock        (clock                        )  //i
  );

endmodule

module ysyx_23060082_Axi4MemSlave (
  input  wire          axi4_awvalid,
  output wire          axi4_awready,
  input  wire [31:0]   axi4_awaddr,
  input  wire [3:0]    axi4_awid,
  input  wire [7:0]    axi4_awlen,
  input  wire [2:0]    axi4_awsize,
  input  wire [1:0]    axi4_awburst,
  input  wire          axi4_wvalid,
  output wire          axi4_wready,
  input  wire [31:0]   axi4_wdata,
  input  wire [3:0]    axi4_wstrb,
  input  wire          axi4_wlast,
  output wire          axi4_bvalid,
  input  wire          axi4_bready,
  output wire [3:0]    axi4_bid,
  output wire [1:0]    axi4_bresp,
  input  wire          axi4_arvalid,
  output wire          axi4_arready,
  input  wire [31:0]   axi4_araddr,
  input  wire [3:0]    axi4_arid,
  input  wire [7:0]    axi4_arlen,
  input  wire [2:0]    axi4_arsize,
  input  wire [1:0]    axi4_arburst,
  output wire          axi4_rvalid,
  input  wire          axi4_rready,
  output wire [31:0]   axi4_rdata,
  output wire [3:0]    axi4_rid,
  output wire [1:0]    axi4_rresp,
  output wire          axi4_rlast,
  input  wire          reset,
  input  wire          clock
);

  wire                memRW_valid;
  wire                memRW_wen;
  wire       [31:0]   memRW_addr;
  wire       [31:0]   memRW_wdata;
  wire       [3:0]    memRW_wmask;
  wire       [31:0]   memRW_rdata;
  wire       [31:0]   _zz_addr;
  wire       [31:0]   _zz_addr_1;
  wire       [14:0]   _zz_addr_2;
  wire       [7:0]    _zz_addr_3;
  wire                arFire;
  reg        [31:0]   readBase;
  reg        [7:0]    readLen;
  reg        [7:0]    readCnt;
  reg                 readActive;
  wire                io_axi4_r_fire;
  wire                when_NPCTOP_l47;
  wire                when_NPCTOP_l55;
  reg        [3:0]    axi4_arid_regNextWhen;
  wire                wAllValid;
  reg                 bValid;
  wire                io_axi4_b_fire;
  wire                io_axi4_aw_fire;
  reg        [3:0]    axi4_awid_regNextWhen;

  assign _zz_addr = (readBase + _zz_addr_1);
  assign _zz_addr_2 = ({7'd0,_zz_addr_3} <<< axi4_arsize);
  assign _zz_addr_1 = {17'd0, _zz_addr_2};
  assign _zz_addr_3 = (readCnt + 8'h01);
  NpcMemRW memRW (
    .clock (clock            ), //i
    .reset (reset            ), //i
    .valid (memRW_valid      ), //i
    .wen   (memRW_wen        ), //i
    .addr  (memRW_addr[31:0] ), //i
    .wdata (memRW_wdata[31:0]), //i
    .wmask (memRW_wmask[3:0] ), //i
    .rdata (memRW_rdata[31:0])  //o
  );
  assign memRW_wen = (axi4_awvalid && axi4_wvalid); // @ NPC_TOP.scala l34
  assign memRW_wdata = axi4_wdata; // @ NPC_TOP.scala l35
  assign memRW_wmask = axi4_wstrb; // @ NPC_TOP.scala l36
  assign arFire = (axi4_arvalid && axi4_arready); // @ BaseType.scala l308
  assign io_axi4_r_fire = (axi4_rvalid && axi4_rready); // @ BaseType.scala l308
  assign when_NPCTOP_l47 = (io_axi4_r_fire && (readCnt != readLen)); // @ BaseType.scala l308
  assign when_NPCTOP_l55 = (io_axi4_r_fire && (readCnt == readLen)); // @ BaseType.scala l308
  assign axi4_arready = (! readActive); // @ NPC_TOP.scala l61
  assign axi4_rvalid = readActive; // @ NPC_TOP.scala l62
  assign axi4_rdata = memRW_rdata; // @ NPC_TOP.scala l63
  assign axi4_rresp = 2'b00; // @ NPC_TOP.scala l64
  assign axi4_rlast = (readActive && (readCnt == readLen)); // @ NPC_TOP.scala l65
  assign axi4_rid = axi4_arid_regNextWhen; // @ NPC_TOP.scala l66
  assign memRW_valid = ((memRW_wen || arFire) || (readActive && (readCnt < readLen))); // @ NPC_TOP.scala l70
  assign memRW_addr = (memRW_wen ? axi4_awaddr : (arFire ? axi4_araddr : _zz_addr)); // @ NPC_TOP.scala l71
  assign wAllValid = (axi4_awvalid && axi4_wvalid); // @ BaseType.scala l308
  assign axi4_awready = wAllValid; // @ NPC_TOP.scala l77
  assign axi4_wready = wAllValid; // @ NPC_TOP.scala l78
  assign io_axi4_b_fire = (axi4_bvalid && axi4_bready); // @ BaseType.scala l308
  assign axi4_bvalid = bValid; // @ NPC_TOP.scala l87
  assign axi4_bresp = 2'b00; // @ NPC_TOP.scala l88
  assign io_axi4_aw_fire = (axi4_awvalid && axi4_awready); // @ BaseType.scala l308
  assign axi4_bid = axi4_awid_regNextWhen; // @ NPC_TOP.scala l89
  always @(posedge clock) begin
    if(arFire) begin
      readBase <= axi4_araddr; // @ NPC_TOP.scala l40
    end
    if(arFire) begin
      readLen <= axi4_arlen; // @ NPC_TOP.scala l41
    end
  end

  always @(posedge clock or posedge reset) begin
    if(reset) begin
      readCnt <= 8'h0; // @ Data.scala l432
      readActive <= 1'b0; // @ Data.scala l432
      axi4_arid_regNextWhen <= 4'b0000; // @ Data.scala l432
      bValid <= 1'b0; // @ Data.scala l432
      axi4_awid_regNextWhen <= 4'b0000; // @ Data.scala l432
    end else begin
      if(arFire) begin
        readCnt <= 8'h0; // @ NPC_TOP.scala l46
      end else begin
        if(when_NPCTOP_l47) begin
          readCnt <= (readCnt + 8'h01); // @ NPC_TOP.scala l48
        end else begin
          readCnt <= readCnt; // @ NPC_TOP.scala l50
        end
      end
      if(arFire) begin
        readActive <= 1'b1; // @ NPC_TOP.scala l54
      end else begin
        if(when_NPCTOP_l55) begin
          readActive <= 1'b0; // @ NPC_TOP.scala l56
        end else begin
          readActive <= readActive; // @ NPC_TOP.scala l58
        end
      end
      if(arFire) begin
        axi4_arid_regNextWhen <= axi4_arid; // @ NPC_TOP.scala l66
      end
      if(wAllValid) begin
        bValid <= 1'b1; // @ NPC_TOP.scala l81
      end else begin
        if(io_axi4_b_fire) begin
          bValid <= 1'b0; // @ NPC_TOP.scala l83
        end else begin
          bValid <= bValid; // @ NPC_TOP.scala l85
        end
      end
      if(io_axi4_aw_fire) begin
        axi4_awid_regNextWhen <= axi4_awid; // @ NPC_TOP.scala l89
      end
    end
  end


endmodule

module ysyx_23060082 (
  input  wire          io_interrupt,
  output wire          io_master_awvalid,
  input  wire          io_master_awready,
  output wire [31:0]   io_master_awaddr,
  output wire [3:0]    io_master_awid,
  output wire [7:0]    io_master_awlen,
  output wire [2:0]    io_master_awsize,
  output wire [1:0]    io_master_awburst,
  output wire          io_master_wvalid,
  input  wire          io_master_wready,
  output wire [31:0]   io_master_wdata,
  output wire [3:0]    io_master_wstrb,
  output wire          io_master_wlast,
  input  wire          io_master_bvalid,
  output wire          io_master_bready,
  input  wire [3:0]    io_master_bid,
  input  wire [1:0]    io_master_bresp,
  output wire          io_master_arvalid,
  input  wire          io_master_arready,
  output wire [31:0]   io_master_araddr,
  output wire [3:0]    io_master_arid,
  output wire [7:0]    io_master_arlen,
  output wire [2:0]    io_master_arsize,
  output wire [1:0]    io_master_arburst,
  input  wire          io_master_rvalid,
  output wire          io_master_rready,
  input  wire [31:0]   io_master_rdata,
  input  wire [3:0]    io_master_rid,
  input  wire [1:0]    io_master_rresp,
  input  wire          io_master_rlast,
  input  wire          io_slave_awvalid,
  output wire          io_slave_awready,
  input  wire [31:0]   io_slave_awaddr,
  input  wire [3:0]    io_slave_awid,
  input  wire [7:0]    io_slave_awlen,
  input  wire [2:0]    io_slave_awsize,
  input  wire [1:0]    io_slave_awburst,
  input  wire          io_slave_wvalid,
  output wire          io_slave_wready,
  input  wire [31:0]   io_slave_wdata,
  input  wire [3:0]    io_slave_wstrb,
  input  wire          io_slave_wlast,
  output wire          io_slave_bvalid,
  input  wire          io_slave_bready,
  output wire [3:0]    io_slave_bid,
  output wire [1:0]    io_slave_bresp,
  input  wire          io_slave_arvalid,
  output wire          io_slave_arready,
  input  wire [31:0]   io_slave_araddr,
  input  wire [3:0]    io_slave_arid,
  input  wire [7:0]    io_slave_arlen,
  input  wire [2:0]    io_slave_arsize,
  input  wire [1:0]    io_slave_arburst,
  output wire          io_slave_rvalid,
  input  wire          io_slave_rready,
  output wire [31:0]   io_slave_rdata,
  output wire [3:0]    io_slave_rid,
  output wire [1:0]    io_slave_rresp,
  output wire          io_slave_rlast,
  input  wire          reset,
  input  wire          clock
);

  wire                ifu_io_output_ready;
  wire                idu_io_output_ready;
  wire                exu_io_output_ready;
  wire                lsu_io_output_ready;
  wire       [31:0]   itraceReg_1_instr;
  wire                mtraceReg_1_valid;
  wire                mtraceReg_1_isDev;
  wire       [31:0]   mtraceReg_1_addr;
  wire       [31:0]   mtraceReg_1_wdata;
  wire       [31:0]   regFile_io_readBus_data1;
  wire       [31:0]   regFile_io_readBus_data2;
  wire                ifu_io_input_ready;
  wire                ifu_io_output_valid;
  wire       [31:0]   ifu_io_output_payload_pc;
  wire       [31:0]   ifu_io_output_payload_instr;
  wire                ifu_io_axi4_ar_valid;
  wire       [31:0]   ifu_io_axi4_ar_payload_addr;
  wire       [3:0]    ifu_io_axi4_ar_payload_id;
  wire       [7:0]    ifu_io_axi4_ar_payload_len;
  wire       [2:0]    ifu_io_axi4_ar_payload_size;
  wire       [1:0]    ifu_io_axi4_ar_payload_burst;
  wire                ifu_io_axi4_r_ready;
  wire                idu_io_output_valid;
  wire       [31:0]   idu_io_output_payload_pc;
  wire                idu_io_output_payload_ctrl_rfCtrl_mem2reg;
  wire                idu_io_output_payload_ctrl_rfCtrl_csr2reg;
  wire                idu_io_output_payload_ctrl_rfCtrl_regWr;
  wire       [4:0]    idu_io_output_payload_ctrl_rfCtrl_rfWriteAddr;
  wire                idu_io_output_payload_ctrl_aluCtrl_aluAsrc;
  wire       [1:0]    idu_io_output_payload_ctrl_aluCtrl_aluBsrc;
  wire       [3:0]    idu_io_output_payload_ctrl_aluCtrl_aluCtr;
  wire       [2:0]    idu_io_output_payload_ctrl_aluCtrl_branch;
  wire                idu_io_output_payload_ctrl_memCtrl_memWr;
  wire       [2:0]    idu_io_output_payload_ctrl_memCtrl_memOp;
  wire       [2:0]    idu_io_output_payload_ctrl_csrCtrl_csrCmd;
  wire                idu_io_output_payload_ctrl_csrCtrl_illegal;
  wire                idu_io_output_payload_ctrl_csrCtrl_ebreak;
  wire                idu_io_output_payload_ctrl_csrCtrl_trapEnter;
  wire                idu_io_output_payload_ctrl_csrCtrl_trapExit;
  wire                idu_io_output_payload_ctrl_fenceI;
  wire       [31:0]   idu_io_output_payload_imm;
  wire       [31:0]   idu_io_output_payload_rfReadData1;
  wire       [31:0]   idu_io_output_payload_rfReadData2;
  wire                idu_io_output_payload_isCalc;
  wire       [4:0]    idu_io_rfRead_addr1;
  wire       [4:0]    idu_io_rfRead_addr2;
  wire                exu_io_output_valid;
  wire       [31:0]   exu_io_output_payload_pc;
  wire       [31:0]   exu_io_output_payload_pcNext;
  wire                exu_io_output_payload_rfCtrl_mem2reg;
  wire                exu_io_output_payload_rfCtrl_csr2reg;
  wire                exu_io_output_payload_rfCtrl_regWr;
  wire       [4:0]    exu_io_output_payload_rfCtrl_rfWriteAddr;
  wire                exu_io_output_payload_memCtrl_memWr;
  wire       [2:0]    exu_io_output_payload_memCtrl_memOp;
  wire       [2:0]    exu_io_output_payload_csrCtrl_csrCmd;
  wire                exu_io_output_payload_csrCtrl_illegal;
  wire                exu_io_output_payload_csrCtrl_ebreak;
  wire                exu_io_output_payload_csrCtrl_trapEnter;
  wire                exu_io_output_payload_csrCtrl_trapExit;
  wire       [11:0]   exu_io_output_payload_csrAddr;
  wire       [31:0]   exu_io_output_payload_rfReadData;
  wire       [31:0]   exu_io_output_payload_aluResult;
  wire                exu_io_output_payload_fenceI;
  wire                lsu_io_output_valid;
  wire       [31:0]   lsu_io_output_payload_pc;
  wire       [31:0]   lsu_io_output_payload_pcNext;
  wire       [31:0]   lsu_io_output_payload_mem_data_out;
  wire       [31:0]   lsu_io_output_payload_alu_data_out;
  wire                lsu_io_output_payload_rfCtrl_mem2reg;
  wire                lsu_io_output_payload_rfCtrl_csr2reg;
  wire                lsu_io_output_payload_rfCtrl_regWr;
  wire       [4:0]    lsu_io_output_payload_rfCtrl_rfWriteAddr;
  wire                lsu_io_output_payload_fenceI;
  wire                lsu_io_axi4_ar_valid;
  wire       [31:0]   lsu_io_axi4_ar_payload_addr;
  wire       [3:0]    lsu_io_axi4_ar_payload_id;
  wire       [7:0]    lsu_io_axi4_ar_payload_len;
  wire       [2:0]    lsu_io_axi4_ar_payload_size;
  wire       [1:0]    lsu_io_axi4_ar_payload_burst;
  wire                lsu_io_axi4_aw_valid;
  wire       [31:0]   lsu_io_axi4_aw_payload_addr;
  wire       [3:0]    lsu_io_axi4_aw_payload_id;
  wire       [7:0]    lsu_io_axi4_aw_payload_len;
  wire       [2:0]    lsu_io_axi4_aw_payload_size;
  wire       [1:0]    lsu_io_axi4_aw_payload_burst;
  wire                lsu_io_axi4_w_valid;
  wire       [31:0]   lsu_io_axi4_w_payload_data;
  wire       [3:0]    lsu_io_axi4_w_payload_strb;
  wire                lsu_io_axi4_w_payload_last;
  wire                lsu_io_axi4_r_ready;
  wire                lsu_io_axi4_b_ready;
  wire                wbu_io_output_valid;
  wire       [31:0]   wbu_io_output_payload_pcNext;
  wire                wbu_io_output_payload_fenceI;
  wire       [4:0]    wbu_io_rfWrite_addr;
  wire       [31:0]   wbu_io_rfWrite_data;
  wire                wbu_io_rfWrite_en;
  wire                xbar_io_ifuAxi4_ar_ready;
  wire                xbar_io_ifuAxi4_r_valid;
  wire       [31:0]   xbar_io_ifuAxi4_r_payload_data;
  wire       [3:0]    xbar_io_ifuAxi4_r_payload_id;
  wire       [1:0]    xbar_io_ifuAxi4_r_payload_resp;
  wire                xbar_io_ifuAxi4_r_payload_last;
  wire                xbar_io_lsuAxi4_ar_ready;
  wire                xbar_io_lsuAxi4_aw_ready;
  wire                xbar_io_lsuAxi4_w_ready;
  wire                xbar_io_lsuAxi4_r_valid;
  wire       [31:0]   xbar_io_lsuAxi4_r_payload_data;
  wire       [3:0]    xbar_io_lsuAxi4_r_payload_id;
  wire       [1:0]    xbar_io_lsuAxi4_r_payload_resp;
  wire                xbar_io_lsuAxi4_r_payload_last;
  wire                xbar_io_lsuAxi4_b_valid;
  wire       [3:0]    xbar_io_lsuAxi4_b_payload_id;
  wire       [1:0]    xbar_io_lsuAxi4_b_payload_resp;
  wire                xbar_io_clintAxi4_ar_valid;
  wire       [31:0]   xbar_io_clintAxi4_ar_payload_addr;
  wire       [3:0]    xbar_io_clintAxi4_ar_payload_id;
  wire       [7:0]    xbar_io_clintAxi4_ar_payload_len;
  wire       [2:0]    xbar_io_clintAxi4_ar_payload_size;
  wire       [1:0]    xbar_io_clintAxi4_ar_payload_burst;
  wire                xbar_io_clintAxi4_aw_valid;
  wire       [31:0]   xbar_io_clintAxi4_aw_payload_addr;
  wire       [3:0]    xbar_io_clintAxi4_aw_payload_id;
  wire       [7:0]    xbar_io_clintAxi4_aw_payload_len;
  wire       [2:0]    xbar_io_clintAxi4_aw_payload_size;
  wire       [1:0]    xbar_io_clintAxi4_aw_payload_burst;
  wire                xbar_io_clintAxi4_w_valid;
  wire       [31:0]   xbar_io_clintAxi4_w_payload_data;
  wire       [3:0]    xbar_io_clintAxi4_w_payload_strb;
  wire                xbar_io_clintAxi4_w_payload_last;
  wire                xbar_io_clintAxi4_r_ready;
  wire                xbar_io_clintAxi4_b_ready;
  wire                xbar_io_externalAxi4_ar_valid;
  wire       [31:0]   xbar_io_externalAxi4_ar_payload_addr;
  wire       [3:0]    xbar_io_externalAxi4_ar_payload_id;
  wire       [7:0]    xbar_io_externalAxi4_ar_payload_len;
  wire       [2:0]    xbar_io_externalAxi4_ar_payload_size;
  wire       [1:0]    xbar_io_externalAxi4_ar_payload_burst;
  wire                xbar_io_externalAxi4_aw_valid;
  wire       [31:0]   xbar_io_externalAxi4_aw_payload_addr;
  wire       [3:0]    xbar_io_externalAxi4_aw_payload_id;
  wire       [7:0]    xbar_io_externalAxi4_aw_payload_len;
  wire       [2:0]    xbar_io_externalAxi4_aw_payload_size;
  wire       [1:0]    xbar_io_externalAxi4_aw_payload_burst;
  wire                xbar_io_externalAxi4_w_valid;
  wire       [31:0]   xbar_io_externalAxi4_w_payload_data;
  wire       [3:0]    xbar_io_externalAxi4_w_payload_strb;
  wire                xbar_io_externalAxi4_w_payload_last;
  wire                xbar_io_externalAxi4_r_ready;
  wire                xbar_io_externalAxi4_b_ready;
  wire                clint_io_clintAxi4_ar_ready;
  wire                clint_io_clintAxi4_aw_ready;
  wire                clint_io_clintAxi4_w_ready;
  wire                clint_io_clintAxi4_r_valid;
  wire       [31:0]   clint_io_clintAxi4_r_payload_data;
  wire       [3:0]    clint_io_clintAxi4_r_payload_id;
  wire       [1:0]    clint_io_clintAxi4_r_payload_resp;
  wire                clint_io_clintAxi4_r_payload_last;
  wire                clint_io_clintAxi4_b_valid;
  wire       [3:0]    clint_io_clintAxi4_b_payload_id;
  wire       [1:0]    clint_io_clintAxi4_b_payload_resp;
  wire                ifu_io_output_fire;
  reg        [31:0]   ifu_io_output_payload_regNextWhen_pc;
  reg        [31:0]   ifu_io_output_payload_regNextWhen_instr;
  reg                 _zz_io_output_ready;
  wire                idu_io_output_fire;
  reg        [31:0]   idu_io_output_payload_regNextWhen_pc;
  reg                 idu_io_output_payload_regNextWhen_ctrl_rfCtrl_mem2reg;
  reg                 idu_io_output_payload_regNextWhen_ctrl_rfCtrl_csr2reg;
  reg                 idu_io_output_payload_regNextWhen_ctrl_rfCtrl_regWr;
  reg        [4:0]    idu_io_output_payload_regNextWhen_ctrl_rfCtrl_rfWriteAddr;
  reg                 idu_io_output_payload_regNextWhen_ctrl_aluCtrl_aluAsrc;
  reg        [1:0]    idu_io_output_payload_regNextWhen_ctrl_aluCtrl_aluBsrc;
  reg        [3:0]    idu_io_output_payload_regNextWhen_ctrl_aluCtrl_aluCtr;
  reg        [2:0]    idu_io_output_payload_regNextWhen_ctrl_aluCtrl_branch;
  reg                 idu_io_output_payload_regNextWhen_ctrl_memCtrl_memWr;
  reg        [2:0]    idu_io_output_payload_regNextWhen_ctrl_memCtrl_memOp;
  reg        [2:0]    idu_io_output_payload_regNextWhen_ctrl_csrCtrl_csrCmd;
  reg                 idu_io_output_payload_regNextWhen_ctrl_csrCtrl_illegal;
  reg                 idu_io_output_payload_regNextWhen_ctrl_csrCtrl_ebreak;
  reg                 idu_io_output_payload_regNextWhen_ctrl_csrCtrl_trapEnter;
  reg                 idu_io_output_payload_regNextWhen_ctrl_csrCtrl_trapExit;
  reg                 idu_io_output_payload_regNextWhen_ctrl_fenceI;
  reg        [31:0]   idu_io_output_payload_regNextWhen_imm;
  reg        [31:0]   idu_io_output_payload_regNextWhen_rfReadData1;
  reg        [31:0]   idu_io_output_payload_regNextWhen_rfReadData2;
  reg                 idu_io_output_payload_regNextWhen_isCalc;
  reg                 _zz_io_output_ready_1;
  wire                exu_io_output_fire;
  reg        [31:0]   exu_io_output_payload_regNextWhen_pc;
  reg        [31:0]   exu_io_output_payload_regNextWhen_pcNext;
  reg                 exu_io_output_payload_regNextWhen_rfCtrl_mem2reg;
  reg                 exu_io_output_payload_regNextWhen_rfCtrl_csr2reg;
  reg                 exu_io_output_payload_regNextWhen_rfCtrl_regWr;
  reg        [4:0]    exu_io_output_payload_regNextWhen_rfCtrl_rfWriteAddr;
  reg                 exu_io_output_payload_regNextWhen_memCtrl_memWr;
  reg        [2:0]    exu_io_output_payload_regNextWhen_memCtrl_memOp;
  reg        [2:0]    exu_io_output_payload_regNextWhen_csrCtrl_csrCmd;
  reg                 exu_io_output_payload_regNextWhen_csrCtrl_illegal;
  reg                 exu_io_output_payload_regNextWhen_csrCtrl_ebreak;
  reg                 exu_io_output_payload_regNextWhen_csrCtrl_trapEnter;
  reg                 exu_io_output_payload_regNextWhen_csrCtrl_trapExit;
  reg        [11:0]   exu_io_output_payload_regNextWhen_csrAddr;
  reg        [31:0]   exu_io_output_payload_regNextWhen_rfReadData;
  reg        [31:0]   exu_io_output_payload_regNextWhen_aluResult;
  reg                 exu_io_output_payload_regNextWhen_fenceI;
  reg                 _zz_io_output_ready_2;
  wire                lsu_io_output_fire;
  reg        [31:0]   lsu_io_output_payload_regNextWhen_pc;
  reg        [31:0]   lsu_io_output_payload_regNextWhen_pcNext;
  reg        [31:0]   lsu_io_output_payload_regNextWhen_mem_data_out;
  reg        [31:0]   lsu_io_output_payload_regNextWhen_alu_data_out;
  reg                 lsu_io_output_payload_regNextWhen_rfCtrl_mem2reg;
  reg                 lsu_io_output_payload_regNextWhen_rfCtrl_csr2reg;
  reg                 lsu_io_output_payload_regNextWhen_rfCtrl_regWr;
  reg        [4:0]    lsu_io_output_payload_regNextWhen_rfCtrl_rfWriteAddr;
  reg                 lsu_io_output_payload_regNextWhen_fenceI;
  reg                 _zz_io_input_valid;
  reg        [31:0]   _zz_instr;
  reg        [31:0]   _zz_instr_1;
  reg        [31:0]   _zz_instr_2;
  reg        [31:0]   _zz_instr_3;
  wire                xbar_io_lsuAxi4_ar_fire;
  wire                xbar_io_lsuAxi4_aw_fire;
  wire                xbar_io_lsuAxi4_w_fire;
  wire                _zz_valid;

  ysyx_23060082_RegFile regFile (
    .io_readBus_addr1 (idu_io_rfRead_addr1[4:0]      ), //i
    .io_readBus_addr2 (idu_io_rfRead_addr2[4:0]      ), //i
    .io_readBus_data1 (regFile_io_readBus_data1[31:0]), //o
    .io_readBus_data2 (regFile_io_readBus_data2[31:0]), //o
    .io_writeBus_addr (wbu_io_rfWrite_addr[4:0]      ), //i
    .io_writeBus_data (wbu_io_rfWrite_data[31:0]     ), //i
    .io_writeBus_en   (wbu_io_rfWrite_en             ), //i
    .clock            (clock                         ), //i
    .reset            (reset                         )  //i
  );
  ysyx_23060082_IFU ifu (
    .io_input_valid           (wbu_io_output_valid                 ), //i
    .io_input_ready           (ifu_io_input_ready                  ), //o
    .io_input_payload_pcNext  (wbu_io_output_payload_pcNext[31:0]  ), //i
    .io_input_payload_fenceI  (wbu_io_output_payload_fenceI        ), //i
    .io_output_valid          (ifu_io_output_valid                 ), //o
    .io_output_ready          (ifu_io_output_ready                 ), //i
    .io_output_payload_pc     (ifu_io_output_payload_pc[31:0]      ), //o
    .io_output_payload_instr  (ifu_io_output_payload_instr[31:0]   ), //o
    .io_axi4_ar_valid         (ifu_io_axi4_ar_valid                ), //o
    .io_axi4_ar_ready         (xbar_io_ifuAxi4_ar_ready            ), //i
    .io_axi4_ar_payload_addr  (ifu_io_axi4_ar_payload_addr[31:0]   ), //o
    .io_axi4_ar_payload_id    (ifu_io_axi4_ar_payload_id[3:0]      ), //o
    .io_axi4_ar_payload_len   (ifu_io_axi4_ar_payload_len[7:0]     ), //o
    .io_axi4_ar_payload_size  (ifu_io_axi4_ar_payload_size[2:0]    ), //o
    .io_axi4_ar_payload_burst (ifu_io_axi4_ar_payload_burst[1:0]   ), //o
    .io_axi4_r_valid          (xbar_io_ifuAxi4_r_valid             ), //i
    .io_axi4_r_ready          (ifu_io_axi4_r_ready                 ), //o
    .io_axi4_r_payload_data   (xbar_io_ifuAxi4_r_payload_data[31:0]), //i
    .io_axi4_r_payload_id     (xbar_io_ifuAxi4_r_payload_id[3:0]   ), //i
    .io_axi4_r_payload_resp   (xbar_io_ifuAxi4_r_payload_resp[1:0] ), //i
    .io_axi4_r_payload_last   (xbar_io_ifuAxi4_r_payload_last      ), //i
    .reset                    (reset                               ), //i
    .clock                    (clock                               )  //i
  );
  ysyx_23060082_IDU idu (
    .io_input_valid                            (_zz_io_output_ready                               ), //i
    .io_input_payload_pc                       (ifu_io_output_payload_regNextWhen_pc[31:0]        ), //i
    .io_input_payload_instr                    (ifu_io_output_payload_regNextWhen_instr[31:0]     ), //i
    .io_output_valid                           (idu_io_output_valid                               ), //o
    .io_output_ready                           (idu_io_output_ready                               ), //i
    .io_output_payload_pc                      (idu_io_output_payload_pc[31:0]                    ), //o
    .io_output_payload_ctrl_rfCtrl_mem2reg     (idu_io_output_payload_ctrl_rfCtrl_mem2reg         ), //o
    .io_output_payload_ctrl_rfCtrl_csr2reg     (idu_io_output_payload_ctrl_rfCtrl_csr2reg         ), //o
    .io_output_payload_ctrl_rfCtrl_regWr       (idu_io_output_payload_ctrl_rfCtrl_regWr           ), //o
    .io_output_payload_ctrl_rfCtrl_rfWriteAddr (idu_io_output_payload_ctrl_rfCtrl_rfWriteAddr[4:0]), //o
    .io_output_payload_ctrl_aluCtrl_aluAsrc    (idu_io_output_payload_ctrl_aluCtrl_aluAsrc        ), //o
    .io_output_payload_ctrl_aluCtrl_aluBsrc    (idu_io_output_payload_ctrl_aluCtrl_aluBsrc[1:0]   ), //o
    .io_output_payload_ctrl_aluCtrl_aluCtr     (idu_io_output_payload_ctrl_aluCtrl_aluCtr[3:0]    ), //o
    .io_output_payload_ctrl_aluCtrl_branch     (idu_io_output_payload_ctrl_aluCtrl_branch[2:0]    ), //o
    .io_output_payload_ctrl_memCtrl_memWr      (idu_io_output_payload_ctrl_memCtrl_memWr          ), //o
    .io_output_payload_ctrl_memCtrl_memOp      (idu_io_output_payload_ctrl_memCtrl_memOp[2:0]     ), //o
    .io_output_payload_ctrl_csrCtrl_csrCmd     (idu_io_output_payload_ctrl_csrCtrl_csrCmd[2:0]    ), //o
    .io_output_payload_ctrl_csrCtrl_illegal    (idu_io_output_payload_ctrl_csrCtrl_illegal        ), //o
    .io_output_payload_ctrl_csrCtrl_ebreak     (idu_io_output_payload_ctrl_csrCtrl_ebreak         ), //o
    .io_output_payload_ctrl_csrCtrl_trapEnter  (idu_io_output_payload_ctrl_csrCtrl_trapEnter      ), //o
    .io_output_payload_ctrl_csrCtrl_trapExit   (idu_io_output_payload_ctrl_csrCtrl_trapExit       ), //o
    .io_output_payload_ctrl_fenceI             (idu_io_output_payload_ctrl_fenceI                 ), //o
    .io_output_payload_imm                     (idu_io_output_payload_imm[31:0]                   ), //o
    .io_output_payload_rfReadData1             (idu_io_output_payload_rfReadData1[31:0]           ), //o
    .io_output_payload_rfReadData2             (idu_io_output_payload_rfReadData2[31:0]           ), //o
    .io_output_payload_isCalc                  (idu_io_output_payload_isCalc                      ), //o
    .io_rfRead_addr1                           (idu_io_rfRead_addr1[4:0]                          ), //o
    .io_rfRead_addr2                           (idu_io_rfRead_addr2[4:0]                          ), //o
    .io_rfRead_data1                           (regFile_io_readBus_data1[31:0]                    ), //i
    .io_rfRead_data2                           (regFile_io_readBus_data2[31:0]                    ), //i
    .reset                                     (reset                                             ), //i
    .clock                                     (clock                                             )  //i
  );
  ysyx_23060082_EXU exu (
    .io_input_valid                           (_zz_io_output_ready_1                                         ), //i
    .io_input_payload_pc                      (idu_io_output_payload_regNextWhen_pc[31:0]                    ), //i
    .io_input_payload_ctrl_rfCtrl_mem2reg     (idu_io_output_payload_regNextWhen_ctrl_rfCtrl_mem2reg         ), //i
    .io_input_payload_ctrl_rfCtrl_csr2reg     (idu_io_output_payload_regNextWhen_ctrl_rfCtrl_csr2reg         ), //i
    .io_input_payload_ctrl_rfCtrl_regWr       (idu_io_output_payload_regNextWhen_ctrl_rfCtrl_regWr           ), //i
    .io_input_payload_ctrl_rfCtrl_rfWriteAddr (idu_io_output_payload_regNextWhen_ctrl_rfCtrl_rfWriteAddr[4:0]), //i
    .io_input_payload_ctrl_aluCtrl_aluAsrc    (idu_io_output_payload_regNextWhen_ctrl_aluCtrl_aluAsrc        ), //i
    .io_input_payload_ctrl_aluCtrl_aluBsrc    (idu_io_output_payload_regNextWhen_ctrl_aluCtrl_aluBsrc[1:0]   ), //i
    .io_input_payload_ctrl_aluCtrl_aluCtr     (idu_io_output_payload_regNextWhen_ctrl_aluCtrl_aluCtr[3:0]    ), //i
    .io_input_payload_ctrl_aluCtrl_branch     (idu_io_output_payload_regNextWhen_ctrl_aluCtrl_branch[2:0]    ), //i
    .io_input_payload_ctrl_memCtrl_memWr      (idu_io_output_payload_regNextWhen_ctrl_memCtrl_memWr          ), //i
    .io_input_payload_ctrl_memCtrl_memOp      (idu_io_output_payload_regNextWhen_ctrl_memCtrl_memOp[2:0]     ), //i
    .io_input_payload_ctrl_csrCtrl_csrCmd     (idu_io_output_payload_regNextWhen_ctrl_csrCtrl_csrCmd[2:0]    ), //i
    .io_input_payload_ctrl_csrCtrl_illegal    (idu_io_output_payload_regNextWhen_ctrl_csrCtrl_illegal        ), //i
    .io_input_payload_ctrl_csrCtrl_ebreak     (idu_io_output_payload_regNextWhen_ctrl_csrCtrl_ebreak         ), //i
    .io_input_payload_ctrl_csrCtrl_trapEnter  (idu_io_output_payload_regNextWhen_ctrl_csrCtrl_trapEnter      ), //i
    .io_input_payload_ctrl_csrCtrl_trapExit   (idu_io_output_payload_regNextWhen_ctrl_csrCtrl_trapExit       ), //i
    .io_input_payload_ctrl_fenceI             (idu_io_output_payload_regNextWhen_ctrl_fenceI                 ), //i
    .io_input_payload_imm                     (idu_io_output_payload_regNextWhen_imm[31:0]                   ), //i
    .io_input_payload_rfReadData1             (idu_io_output_payload_regNextWhen_rfReadData1[31:0]           ), //i
    .io_input_payload_rfReadData2             (idu_io_output_payload_regNextWhen_rfReadData2[31:0]           ), //i
    .io_input_payload_isCalc                  (idu_io_output_payload_regNextWhen_isCalc                      ), //i
    .io_output_valid                          (exu_io_output_valid                                           ), //o
    .io_output_ready                          (exu_io_output_ready                                           ), //i
    .io_output_payload_pc                     (exu_io_output_payload_pc[31:0]                                ), //o
    .io_output_payload_pcNext                 (exu_io_output_payload_pcNext[31:0]                            ), //o
    .io_output_payload_rfCtrl_mem2reg         (exu_io_output_payload_rfCtrl_mem2reg                          ), //o
    .io_output_payload_rfCtrl_csr2reg         (exu_io_output_payload_rfCtrl_csr2reg                          ), //o
    .io_output_payload_rfCtrl_regWr           (exu_io_output_payload_rfCtrl_regWr                            ), //o
    .io_output_payload_rfCtrl_rfWriteAddr     (exu_io_output_payload_rfCtrl_rfWriteAddr[4:0]                 ), //o
    .io_output_payload_memCtrl_memWr          (exu_io_output_payload_memCtrl_memWr                           ), //o
    .io_output_payload_memCtrl_memOp          (exu_io_output_payload_memCtrl_memOp[2:0]                      ), //o
    .io_output_payload_csrCtrl_csrCmd         (exu_io_output_payload_csrCtrl_csrCmd[2:0]                     ), //o
    .io_output_payload_csrCtrl_illegal        (exu_io_output_payload_csrCtrl_illegal                         ), //o
    .io_output_payload_csrCtrl_ebreak         (exu_io_output_payload_csrCtrl_ebreak                          ), //o
    .io_output_payload_csrCtrl_trapEnter      (exu_io_output_payload_csrCtrl_trapEnter                       ), //o
    .io_output_payload_csrCtrl_trapExit       (exu_io_output_payload_csrCtrl_trapExit                        ), //o
    .io_output_payload_csrAddr                (exu_io_output_payload_csrAddr[11:0]                           ), //o
    .io_output_payload_rfReadData             (exu_io_output_payload_rfReadData[31:0]                        ), //o
    .io_output_payload_aluResult              (exu_io_output_payload_aluResult[31:0]                         ), //o
    .io_output_payload_fenceI                 (exu_io_output_payload_fenceI                                  ), //o
    .reset                                    (reset                                                         ), //i
    .clock                                    (clock                                                         )  //i
  );
  ysyx_23060082_LSU lsu (
    .io_input_valid                       (_zz_io_output_ready_2                                    ), //i
    .io_input_payload_pc                  (exu_io_output_payload_regNextWhen_pc[31:0]               ), //i
    .io_input_payload_pcNext              (exu_io_output_payload_regNextWhen_pcNext[31:0]           ), //i
    .io_input_payload_rfCtrl_mem2reg      (exu_io_output_payload_regNextWhen_rfCtrl_mem2reg         ), //i
    .io_input_payload_rfCtrl_csr2reg      (exu_io_output_payload_regNextWhen_rfCtrl_csr2reg         ), //i
    .io_input_payload_rfCtrl_regWr        (exu_io_output_payload_regNextWhen_rfCtrl_regWr           ), //i
    .io_input_payload_rfCtrl_rfWriteAddr  (exu_io_output_payload_regNextWhen_rfCtrl_rfWriteAddr[4:0]), //i
    .io_input_payload_memCtrl_memWr       (exu_io_output_payload_regNextWhen_memCtrl_memWr          ), //i
    .io_input_payload_memCtrl_memOp       (exu_io_output_payload_regNextWhen_memCtrl_memOp[2:0]     ), //i
    .io_input_payload_csrCtrl_csrCmd      (exu_io_output_payload_regNextWhen_csrCtrl_csrCmd[2:0]    ), //i
    .io_input_payload_csrCtrl_illegal     (exu_io_output_payload_regNextWhen_csrCtrl_illegal        ), //i
    .io_input_payload_csrCtrl_ebreak      (exu_io_output_payload_regNextWhen_csrCtrl_ebreak         ), //i
    .io_input_payload_csrCtrl_trapEnter   (exu_io_output_payload_regNextWhen_csrCtrl_trapEnter      ), //i
    .io_input_payload_csrCtrl_trapExit    (exu_io_output_payload_regNextWhen_csrCtrl_trapExit       ), //i
    .io_input_payload_csrAddr             (exu_io_output_payload_regNextWhen_csrAddr[11:0]          ), //i
    .io_input_payload_rfReadData          (exu_io_output_payload_regNextWhen_rfReadData[31:0]       ), //i
    .io_input_payload_aluResult           (exu_io_output_payload_regNextWhen_aluResult[31:0]        ), //i
    .io_input_payload_fenceI              (exu_io_output_payload_regNextWhen_fenceI                 ), //i
    .io_output_valid                      (lsu_io_output_valid                                      ), //o
    .io_output_ready                      (lsu_io_output_ready                                      ), //i
    .io_output_payload_pc                 (lsu_io_output_payload_pc[31:0]                           ), //o
    .io_output_payload_pcNext             (lsu_io_output_payload_pcNext[31:0]                       ), //o
    .io_output_payload_mem_data_out       (lsu_io_output_payload_mem_data_out[31:0]                 ), //o
    .io_output_payload_alu_data_out       (lsu_io_output_payload_alu_data_out[31:0]                 ), //o
    .io_output_payload_rfCtrl_mem2reg     (lsu_io_output_payload_rfCtrl_mem2reg                     ), //o
    .io_output_payload_rfCtrl_csr2reg     (lsu_io_output_payload_rfCtrl_csr2reg                     ), //o
    .io_output_payload_rfCtrl_regWr       (lsu_io_output_payload_rfCtrl_regWr                       ), //o
    .io_output_payload_rfCtrl_rfWriteAddr (lsu_io_output_payload_rfCtrl_rfWriteAddr[4:0]            ), //o
    .io_output_payload_fenceI             (lsu_io_output_payload_fenceI                             ), //o
    .io_axi4_aw_valid                     (lsu_io_axi4_aw_valid                                     ), //o
    .io_axi4_aw_ready                     (xbar_io_lsuAxi4_aw_ready                                 ), //i
    .io_axi4_aw_payload_addr              (lsu_io_axi4_aw_payload_addr[31:0]                        ), //o
    .io_axi4_aw_payload_id                (lsu_io_axi4_aw_payload_id[3:0]                           ), //o
    .io_axi4_aw_payload_len               (lsu_io_axi4_aw_payload_len[7:0]                          ), //o
    .io_axi4_aw_payload_size              (lsu_io_axi4_aw_payload_size[2:0]                         ), //o
    .io_axi4_aw_payload_burst             (lsu_io_axi4_aw_payload_burst[1:0]                        ), //o
    .io_axi4_w_valid                      (lsu_io_axi4_w_valid                                      ), //o
    .io_axi4_w_ready                      (xbar_io_lsuAxi4_w_ready                                  ), //i
    .io_axi4_w_payload_data               (lsu_io_axi4_w_payload_data[31:0]                         ), //o
    .io_axi4_w_payload_strb               (lsu_io_axi4_w_payload_strb[3:0]                          ), //o
    .io_axi4_w_payload_last               (lsu_io_axi4_w_payload_last                               ), //o
    .io_axi4_b_valid                      (xbar_io_lsuAxi4_b_valid                                  ), //i
    .io_axi4_b_ready                      (lsu_io_axi4_b_ready                                      ), //o
    .io_axi4_b_payload_id                 (xbar_io_lsuAxi4_b_payload_id[3:0]                        ), //i
    .io_axi4_b_payload_resp               (xbar_io_lsuAxi4_b_payload_resp[1:0]                      ), //i
    .io_axi4_ar_valid                     (lsu_io_axi4_ar_valid                                     ), //o
    .io_axi4_ar_ready                     (xbar_io_lsuAxi4_ar_ready                                 ), //i
    .io_axi4_ar_payload_addr              (lsu_io_axi4_ar_payload_addr[31:0]                        ), //o
    .io_axi4_ar_payload_id                (lsu_io_axi4_ar_payload_id[3:0]                           ), //o
    .io_axi4_ar_payload_len               (lsu_io_axi4_ar_payload_len[7:0]                          ), //o
    .io_axi4_ar_payload_size              (lsu_io_axi4_ar_payload_size[2:0]                         ), //o
    .io_axi4_ar_payload_burst             (lsu_io_axi4_ar_payload_burst[1:0]                        ), //o
    .io_axi4_r_valid                      (xbar_io_lsuAxi4_r_valid                                  ), //i
    .io_axi4_r_ready                      (lsu_io_axi4_r_ready                                      ), //o
    .io_axi4_r_payload_data               (xbar_io_lsuAxi4_r_payload_data[31:0]                     ), //i
    .io_axi4_r_payload_id                 (xbar_io_lsuAxi4_r_payload_id[3:0]                        ), //i
    .io_axi4_r_payload_resp               (xbar_io_lsuAxi4_r_payload_resp[1:0]                      ), //i
    .io_axi4_r_payload_last               (xbar_io_lsuAxi4_r_payload_last                           ), //i
    .reset                                (reset                                                    ), //i
    .clock                                (clock                                                    )  //i
  );
  ysyx_23060082_WBU wbu (
    .io_input_valid                      (_zz_io_input_valid                                       ), //i
    .io_input_payload_pc                 (lsu_io_output_payload_regNextWhen_pc[31:0]               ), //i
    .io_input_payload_pcNext             (lsu_io_output_payload_regNextWhen_pcNext[31:0]           ), //i
    .io_input_payload_mem_data_out       (lsu_io_output_payload_regNextWhen_mem_data_out[31:0]     ), //i
    .io_input_payload_alu_data_out       (lsu_io_output_payload_regNextWhen_alu_data_out[31:0]     ), //i
    .io_input_payload_rfCtrl_mem2reg     (lsu_io_output_payload_regNextWhen_rfCtrl_mem2reg         ), //i
    .io_input_payload_rfCtrl_csr2reg     (lsu_io_output_payload_regNextWhen_rfCtrl_csr2reg         ), //i
    .io_input_payload_rfCtrl_regWr       (lsu_io_output_payload_regNextWhen_rfCtrl_regWr           ), //i
    .io_input_payload_rfCtrl_rfWriteAddr (lsu_io_output_payload_regNextWhen_rfCtrl_rfWriteAddr[4:0]), //i
    .io_input_payload_fenceI             (lsu_io_output_payload_regNextWhen_fenceI                 ), //i
    .io_output_valid                     (wbu_io_output_valid                                      ), //o
    .io_output_ready                     (ifu_io_input_ready                                       ), //i
    .io_output_payload_pcNext            (wbu_io_output_payload_pcNext[31:0]                       ), //o
    .io_output_payload_fenceI            (wbu_io_output_payload_fenceI                             ), //o
    .io_rfWrite_addr                     (wbu_io_rfWrite_addr[4:0]                                 ), //o
    .io_rfWrite_data                     (wbu_io_rfWrite_data[31:0]                                ), //o
    .io_rfWrite_en                       (wbu_io_rfWrite_en                                        )  //o
  );
  ysyx_23060082_AXI4Xbar xbar (
    .io_ifuAxi4_ar_valid              (ifu_io_axi4_ar_valid                      ), //i
    .io_ifuAxi4_ar_ready              (xbar_io_ifuAxi4_ar_ready                  ), //o
    .io_ifuAxi4_ar_payload_addr       (ifu_io_axi4_ar_payload_addr[31:0]         ), //i
    .io_ifuAxi4_ar_payload_id         (ifu_io_axi4_ar_payload_id[3:0]            ), //i
    .io_ifuAxi4_ar_payload_len        (ifu_io_axi4_ar_payload_len[7:0]           ), //i
    .io_ifuAxi4_ar_payload_size       (ifu_io_axi4_ar_payload_size[2:0]          ), //i
    .io_ifuAxi4_ar_payload_burst      (ifu_io_axi4_ar_payload_burst[1:0]         ), //i
    .io_ifuAxi4_r_valid               (xbar_io_ifuAxi4_r_valid                   ), //o
    .io_ifuAxi4_r_ready               (ifu_io_axi4_r_ready                       ), //i
    .io_ifuAxi4_r_payload_data        (xbar_io_ifuAxi4_r_payload_data[31:0]      ), //o
    .io_ifuAxi4_r_payload_id          (xbar_io_ifuAxi4_r_payload_id[3:0]         ), //o
    .io_ifuAxi4_r_payload_resp        (xbar_io_ifuAxi4_r_payload_resp[1:0]       ), //o
    .io_ifuAxi4_r_payload_last        (xbar_io_ifuAxi4_r_payload_last            ), //o
    .io_lsuAxi4_aw_valid              (lsu_io_axi4_aw_valid                      ), //i
    .io_lsuAxi4_aw_ready              (xbar_io_lsuAxi4_aw_ready                  ), //o
    .io_lsuAxi4_aw_payload_addr       (lsu_io_axi4_aw_payload_addr[31:0]         ), //i
    .io_lsuAxi4_aw_payload_id         (lsu_io_axi4_aw_payload_id[3:0]            ), //i
    .io_lsuAxi4_aw_payload_len        (lsu_io_axi4_aw_payload_len[7:0]           ), //i
    .io_lsuAxi4_aw_payload_size       (lsu_io_axi4_aw_payload_size[2:0]          ), //i
    .io_lsuAxi4_aw_payload_burst      (lsu_io_axi4_aw_payload_burst[1:0]         ), //i
    .io_lsuAxi4_w_valid               (lsu_io_axi4_w_valid                       ), //i
    .io_lsuAxi4_w_ready               (xbar_io_lsuAxi4_w_ready                   ), //o
    .io_lsuAxi4_w_payload_data        (lsu_io_axi4_w_payload_data[31:0]          ), //i
    .io_lsuAxi4_w_payload_strb        (lsu_io_axi4_w_payload_strb[3:0]           ), //i
    .io_lsuAxi4_w_payload_last        (lsu_io_axi4_w_payload_last                ), //i
    .io_lsuAxi4_b_valid               (xbar_io_lsuAxi4_b_valid                   ), //o
    .io_lsuAxi4_b_ready               (lsu_io_axi4_b_ready                       ), //i
    .io_lsuAxi4_b_payload_id          (xbar_io_lsuAxi4_b_payload_id[3:0]         ), //o
    .io_lsuAxi4_b_payload_resp        (xbar_io_lsuAxi4_b_payload_resp[1:0]       ), //o
    .io_lsuAxi4_ar_valid              (lsu_io_axi4_ar_valid                      ), //i
    .io_lsuAxi4_ar_ready              (xbar_io_lsuAxi4_ar_ready                  ), //o
    .io_lsuAxi4_ar_payload_addr       (lsu_io_axi4_ar_payload_addr[31:0]         ), //i
    .io_lsuAxi4_ar_payload_id         (lsu_io_axi4_ar_payload_id[3:0]            ), //i
    .io_lsuAxi4_ar_payload_len        (lsu_io_axi4_ar_payload_len[7:0]           ), //i
    .io_lsuAxi4_ar_payload_size       (lsu_io_axi4_ar_payload_size[2:0]          ), //i
    .io_lsuAxi4_ar_payload_burst      (lsu_io_axi4_ar_payload_burst[1:0]         ), //i
    .io_lsuAxi4_r_valid               (xbar_io_lsuAxi4_r_valid                   ), //o
    .io_lsuAxi4_r_ready               (lsu_io_axi4_r_ready                       ), //i
    .io_lsuAxi4_r_payload_data        (xbar_io_lsuAxi4_r_payload_data[31:0]      ), //o
    .io_lsuAxi4_r_payload_id          (xbar_io_lsuAxi4_r_payload_id[3:0]         ), //o
    .io_lsuAxi4_r_payload_resp        (xbar_io_lsuAxi4_r_payload_resp[1:0]       ), //o
    .io_lsuAxi4_r_payload_last        (xbar_io_lsuAxi4_r_payload_last            ), //o
    .io_clintAxi4_aw_valid            (xbar_io_clintAxi4_aw_valid                ), //o
    .io_clintAxi4_aw_ready            (clint_io_clintAxi4_aw_ready               ), //i
    .io_clintAxi4_aw_payload_addr     (xbar_io_clintAxi4_aw_payload_addr[31:0]   ), //o
    .io_clintAxi4_aw_payload_id       (xbar_io_clintAxi4_aw_payload_id[3:0]      ), //o
    .io_clintAxi4_aw_payload_len      (xbar_io_clintAxi4_aw_payload_len[7:0]     ), //o
    .io_clintAxi4_aw_payload_size     (xbar_io_clintAxi4_aw_payload_size[2:0]    ), //o
    .io_clintAxi4_aw_payload_burst    (xbar_io_clintAxi4_aw_payload_burst[1:0]   ), //o
    .io_clintAxi4_w_valid             (xbar_io_clintAxi4_w_valid                 ), //o
    .io_clintAxi4_w_ready             (clint_io_clintAxi4_w_ready                ), //i
    .io_clintAxi4_w_payload_data      (xbar_io_clintAxi4_w_payload_data[31:0]    ), //o
    .io_clintAxi4_w_payload_strb      (xbar_io_clintAxi4_w_payload_strb[3:0]     ), //o
    .io_clintAxi4_w_payload_last      (xbar_io_clintAxi4_w_payload_last          ), //o
    .io_clintAxi4_b_valid             (clint_io_clintAxi4_b_valid                ), //i
    .io_clintAxi4_b_ready             (xbar_io_clintAxi4_b_ready                 ), //o
    .io_clintAxi4_b_payload_id        (clint_io_clintAxi4_b_payload_id[3:0]      ), //i
    .io_clintAxi4_b_payload_resp      (clint_io_clintAxi4_b_payload_resp[1:0]    ), //i
    .io_clintAxi4_ar_valid            (xbar_io_clintAxi4_ar_valid                ), //o
    .io_clintAxi4_ar_ready            (clint_io_clintAxi4_ar_ready               ), //i
    .io_clintAxi4_ar_payload_addr     (xbar_io_clintAxi4_ar_payload_addr[31:0]   ), //o
    .io_clintAxi4_ar_payload_id       (xbar_io_clintAxi4_ar_payload_id[3:0]      ), //o
    .io_clintAxi4_ar_payload_len      (xbar_io_clintAxi4_ar_payload_len[7:0]     ), //o
    .io_clintAxi4_ar_payload_size     (xbar_io_clintAxi4_ar_payload_size[2:0]    ), //o
    .io_clintAxi4_ar_payload_burst    (xbar_io_clintAxi4_ar_payload_burst[1:0]   ), //o
    .io_clintAxi4_r_valid             (clint_io_clintAxi4_r_valid                ), //i
    .io_clintAxi4_r_ready             (xbar_io_clintAxi4_r_ready                 ), //o
    .io_clintAxi4_r_payload_data      (clint_io_clintAxi4_r_payload_data[31:0]   ), //i
    .io_clintAxi4_r_payload_id        (clint_io_clintAxi4_r_payload_id[3:0]      ), //i
    .io_clintAxi4_r_payload_resp      (clint_io_clintAxi4_r_payload_resp[1:0]    ), //i
    .io_clintAxi4_r_payload_last      (clint_io_clintAxi4_r_payload_last         ), //i
    .io_externalAxi4_aw_valid         (xbar_io_externalAxi4_aw_valid             ), //o
    .io_externalAxi4_aw_ready         (io_master_awready                         ), //i
    .io_externalAxi4_aw_payload_addr  (xbar_io_externalAxi4_aw_payload_addr[31:0]), //o
    .io_externalAxi4_aw_payload_id    (xbar_io_externalAxi4_aw_payload_id[3:0]   ), //o
    .io_externalAxi4_aw_payload_len   (xbar_io_externalAxi4_aw_payload_len[7:0]  ), //o
    .io_externalAxi4_aw_payload_size  (xbar_io_externalAxi4_aw_payload_size[2:0] ), //o
    .io_externalAxi4_aw_payload_burst (xbar_io_externalAxi4_aw_payload_burst[1:0]), //o
    .io_externalAxi4_w_valid          (xbar_io_externalAxi4_w_valid              ), //o
    .io_externalAxi4_w_ready          (io_master_wready                          ), //i
    .io_externalAxi4_w_payload_data   (xbar_io_externalAxi4_w_payload_data[31:0] ), //o
    .io_externalAxi4_w_payload_strb   (xbar_io_externalAxi4_w_payload_strb[3:0]  ), //o
    .io_externalAxi4_w_payload_last   (xbar_io_externalAxi4_w_payload_last       ), //o
    .io_externalAxi4_b_valid          (io_master_bvalid                          ), //i
    .io_externalAxi4_b_ready          (xbar_io_externalAxi4_b_ready              ), //o
    .io_externalAxi4_b_payload_id     (io_master_bid[3:0]                        ), //i
    .io_externalAxi4_b_payload_resp   (io_master_bresp[1:0]                      ), //i
    .io_externalAxi4_ar_valid         (xbar_io_externalAxi4_ar_valid             ), //o
    .io_externalAxi4_ar_ready         (io_master_arready                         ), //i
    .io_externalAxi4_ar_payload_addr  (xbar_io_externalAxi4_ar_payload_addr[31:0]), //o
    .io_externalAxi4_ar_payload_id    (xbar_io_externalAxi4_ar_payload_id[3:0]   ), //o
    .io_externalAxi4_ar_payload_len   (xbar_io_externalAxi4_ar_payload_len[7:0]  ), //o
    .io_externalAxi4_ar_payload_size  (xbar_io_externalAxi4_ar_payload_size[2:0] ), //o
    .io_externalAxi4_ar_payload_burst (xbar_io_externalAxi4_ar_payload_burst[1:0]), //o
    .io_externalAxi4_r_valid          (io_master_rvalid                          ), //i
    .io_externalAxi4_r_ready          (xbar_io_externalAxi4_r_ready              ), //o
    .io_externalAxi4_r_payload_data   (io_master_rdata[31:0]                     ), //i
    .io_externalAxi4_r_payload_id     (io_master_rid[3:0]                        ), //i
    .io_externalAxi4_r_payload_resp   (io_master_rresp[1:0]                      ), //i
    .io_externalAxi4_r_payload_last   (io_master_rlast                           ), //i
    .clock                            (clock                                     ), //i
    .reset                            (reset                                     )  //i
  );
  ysyx_23060082_Clint clint (
    .io_clintAxi4_aw_valid         (xbar_io_clintAxi4_aw_valid             ), //i
    .io_clintAxi4_aw_ready         (clint_io_clintAxi4_aw_ready            ), //o
    .io_clintAxi4_aw_payload_addr  (xbar_io_clintAxi4_aw_payload_addr[31:0]), //i
    .io_clintAxi4_aw_payload_id    (xbar_io_clintAxi4_aw_payload_id[3:0]   ), //i
    .io_clintAxi4_aw_payload_len   (xbar_io_clintAxi4_aw_payload_len[7:0]  ), //i
    .io_clintAxi4_aw_payload_size  (xbar_io_clintAxi4_aw_payload_size[2:0] ), //i
    .io_clintAxi4_aw_payload_burst (xbar_io_clintAxi4_aw_payload_burst[1:0]), //i
    .io_clintAxi4_w_valid          (xbar_io_clintAxi4_w_valid              ), //i
    .io_clintAxi4_w_ready          (clint_io_clintAxi4_w_ready             ), //o
    .io_clintAxi4_w_payload_data   (xbar_io_clintAxi4_w_payload_data[31:0] ), //i
    .io_clintAxi4_w_payload_strb   (xbar_io_clintAxi4_w_payload_strb[3:0]  ), //i
    .io_clintAxi4_w_payload_last   (xbar_io_clintAxi4_w_payload_last       ), //i
    .io_clintAxi4_b_valid          (clint_io_clintAxi4_b_valid             ), //o
    .io_clintAxi4_b_ready          (xbar_io_clintAxi4_b_ready              ), //i
    .io_clintAxi4_b_payload_id     (clint_io_clintAxi4_b_payload_id[3:0]   ), //o
    .io_clintAxi4_b_payload_resp   (clint_io_clintAxi4_b_payload_resp[1:0] ), //o
    .io_clintAxi4_ar_valid         (xbar_io_clintAxi4_ar_valid             ), //i
    .io_clintAxi4_ar_ready         (clint_io_clintAxi4_ar_ready            ), //o
    .io_clintAxi4_ar_payload_addr  (xbar_io_clintAxi4_ar_payload_addr[31:0]), //i
    .io_clintAxi4_ar_payload_id    (xbar_io_clintAxi4_ar_payload_id[3:0]   ), //i
    .io_clintAxi4_ar_payload_len   (xbar_io_clintAxi4_ar_payload_len[7:0]  ), //i
    .io_clintAxi4_ar_payload_size  (xbar_io_clintAxi4_ar_payload_size[2:0] ), //i
    .io_clintAxi4_ar_payload_burst (xbar_io_clintAxi4_ar_payload_burst[1:0]), //i
    .io_clintAxi4_r_valid          (clint_io_clintAxi4_r_valid             ), //o
    .io_clintAxi4_r_ready          (xbar_io_clintAxi4_r_ready              ), //i
    .io_clintAxi4_r_payload_data   (clint_io_clintAxi4_r_payload_data[31:0]), //o
    .io_clintAxi4_r_payload_id     (clint_io_clintAxi4_r_payload_id[3:0]   ), //o
    .io_clintAxi4_r_payload_resp   (clint_io_clintAxi4_r_payload_resp[1:0] ), //o
    .io_clintAxi4_r_payload_last   (clint_io_clintAxi4_r_payload_last      ), //o
    .clock                         (clock                                  ), //i
    .reset                         (reset                                  )  //i
  );
  ItraceReg itraceReg_1 (
    .clock (clock                                     ), //i
    .reset (reset                                     ), //i
    .valid (_zz_io_input_valid                        ), //i
    .pc    (lsu_io_output_payload_regNextWhen_pc[31:0]), //i
    .instr (itraceReg_1_instr[31:0]                   )  //i
  );
  MtraceReg mtraceReg_1 (
    .clock (clock                  ), //i
    .reset (reset                  ), //i
    .valid (mtraceReg_1_valid      ), //i
    .wen   (_zz_valid              ), //i
    .isDev (mtraceReg_1_isDev      ), //i
    .addr  (mtraceReg_1_addr[31:0] ), //i
    .wdata (mtraceReg_1_wdata[31:0])  //i
  );
  assign io_slave_awready = 1'b0; // @ 23060082.scala l42
  assign io_slave_wready = 1'b0; // @ 23060082.scala l43
  assign io_slave_bvalid = 1'b0; // @ 23060082.scala l44
  assign io_slave_bid = 4'b0000; // @ 23060082.scala l45
  assign io_slave_bresp = 2'b00; // @ 23060082.scala l46
  assign io_slave_arready = 1'b0; // @ 23060082.scala l47
  assign io_slave_rvalid = 1'b0; // @ 23060082.scala l48
  assign io_slave_rdata = 32'h0; // @ 23060082.scala l49
  assign io_slave_rresp = 2'b00; // @ 23060082.scala l50
  assign io_slave_rlast = 1'b0; // @ 23060082.scala l51
  assign io_slave_rid = 4'b0000; // @ 23060082.scala l52
  assign ifu_io_output_fire = (ifu_io_output_valid && ifu_io_output_ready); // @ BaseType.scala l308
  assign idu_io_output_fire = (idu_io_output_valid && idu_io_output_ready); // @ BaseType.scala l308
  assign ifu_io_output_ready = ((! _zz_io_output_ready) || idu_io_output_fire); // @ 23060082.scala l74
  assign exu_io_output_fire = (exu_io_output_valid && exu_io_output_ready); // @ BaseType.scala l308
  assign idu_io_output_ready = ((! _zz_io_output_ready_1) || exu_io_output_fire); // @ 23060082.scala l74
  assign lsu_io_output_fire = (lsu_io_output_valid && lsu_io_output_ready); // @ BaseType.scala l308
  assign exu_io_output_ready = ((! _zz_io_output_ready_2) || lsu_io_output_fire); // @ 23060082.scala l74
  assign lsu_io_output_ready = 1'b1; // @ 23060082.scala l92
  assign io_master_awvalid = xbar_io_externalAxi4_aw_valid; // @ 23060082.scala l113
  assign io_master_awaddr = xbar_io_externalAxi4_aw_payload_addr; // @ 23060082.scala l113
  assign io_master_awid = xbar_io_externalAxi4_aw_payload_id; // @ 23060082.scala l113
  assign io_master_awlen = xbar_io_externalAxi4_aw_payload_len; // @ 23060082.scala l113
  assign io_master_awsize = xbar_io_externalAxi4_aw_payload_size; // @ 23060082.scala l113
  assign io_master_awburst = xbar_io_externalAxi4_aw_payload_burst; // @ 23060082.scala l113
  assign io_master_wvalid = xbar_io_externalAxi4_w_valid; // @ 23060082.scala l113
  assign io_master_wdata = xbar_io_externalAxi4_w_payload_data; // @ 23060082.scala l113
  assign io_master_wstrb = xbar_io_externalAxi4_w_payload_strb; // @ 23060082.scala l113
  assign io_master_wlast = xbar_io_externalAxi4_w_payload_last; // @ 23060082.scala l113
  assign io_master_bready = xbar_io_externalAxi4_b_ready; // @ 23060082.scala l113
  assign io_master_arvalid = xbar_io_externalAxi4_ar_valid; // @ 23060082.scala l113
  assign io_master_araddr = xbar_io_externalAxi4_ar_payload_addr; // @ 23060082.scala l113
  assign io_master_arid = xbar_io_externalAxi4_ar_payload_id; // @ 23060082.scala l113
  assign io_master_arlen = xbar_io_externalAxi4_ar_payload_len; // @ 23060082.scala l113
  assign io_master_arsize = xbar_io_externalAxi4_ar_payload_size; // @ 23060082.scala l113
  assign io_master_arburst = xbar_io_externalAxi4_ar_payload_burst; // @ 23060082.scala l113
  assign io_master_rready = xbar_io_externalAxi4_r_ready; // @ 23060082.scala l113
  assign itraceReg_1_instr = _zz_instr_3; // @ 23060082.scala l135
  assign xbar_io_lsuAxi4_ar_fire = (lsu_io_axi4_ar_valid && xbar_io_lsuAxi4_ar_ready); // @ BaseType.scala l308
  assign xbar_io_lsuAxi4_aw_fire = (lsu_io_axi4_aw_valid && xbar_io_lsuAxi4_aw_ready); // @ BaseType.scala l308
  assign xbar_io_lsuAxi4_w_fire = (lsu_io_axi4_w_valid && xbar_io_lsuAxi4_w_ready); // @ BaseType.scala l308
  assign _zz_valid = (xbar_io_lsuAxi4_aw_fire && xbar_io_lsuAxi4_w_fire); // @ BaseType.scala l308
  assign mtraceReg_1_valid = (xbar_io_lsuAxi4_ar_fire || _zz_valid); // @ 23060082.scala l146
  assign mtraceReg_1_isDev = (xbar_io_lsuAxi4_ar_fire ? ((lsu_io_axi4_ar_payload_addr < 32'h30000000) || (32'h38000000 <= lsu_io_axi4_ar_payload_addr)) : ((lsu_io_axi4_aw_payload_addr < 32'h30000000) || (32'h38000000 <= lsu_io_axi4_aw_payload_addr))); // @ 23060082.scala l148
  assign mtraceReg_1_addr = (xbar_io_lsuAxi4_ar_fire ? lsu_io_axi4_ar_payload_addr : lsu_io_axi4_aw_payload_addr); // @ 23060082.scala l150
  assign mtraceReg_1_wdata = lsu_io_axi4_w_payload_data; // @ 23060082.scala l151
  always @(posedge clock) begin
    if(ifu_io_output_fire) begin
      ifu_io_output_payload_regNextWhen_pc <= ifu_io_output_payload_pc; // @ 23060082.scala l60
      ifu_io_output_payload_regNextWhen_instr <= ifu_io_output_payload_instr; // @ 23060082.scala l60
    end
    if(idu_io_output_fire) begin
      idu_io_output_payload_regNextWhen_pc <= idu_io_output_payload_pc; // @ 23060082.scala l60
      idu_io_output_payload_regNextWhen_ctrl_rfCtrl_mem2reg <= idu_io_output_payload_ctrl_rfCtrl_mem2reg; // @ 23060082.scala l60
      idu_io_output_payload_regNextWhen_ctrl_rfCtrl_csr2reg <= idu_io_output_payload_ctrl_rfCtrl_csr2reg; // @ 23060082.scala l60
      idu_io_output_payload_regNextWhen_ctrl_rfCtrl_regWr <= idu_io_output_payload_ctrl_rfCtrl_regWr; // @ 23060082.scala l60
      idu_io_output_payload_regNextWhen_ctrl_rfCtrl_rfWriteAddr <= idu_io_output_payload_ctrl_rfCtrl_rfWriteAddr; // @ 23060082.scala l60
      idu_io_output_payload_regNextWhen_ctrl_aluCtrl_aluAsrc <= idu_io_output_payload_ctrl_aluCtrl_aluAsrc; // @ 23060082.scala l60
      idu_io_output_payload_regNextWhen_ctrl_aluCtrl_aluBsrc <= idu_io_output_payload_ctrl_aluCtrl_aluBsrc; // @ 23060082.scala l60
      idu_io_output_payload_regNextWhen_ctrl_aluCtrl_aluCtr <= idu_io_output_payload_ctrl_aluCtrl_aluCtr; // @ 23060082.scala l60
      idu_io_output_payload_regNextWhen_ctrl_aluCtrl_branch <= idu_io_output_payload_ctrl_aluCtrl_branch; // @ 23060082.scala l60
      idu_io_output_payload_regNextWhen_ctrl_memCtrl_memWr <= idu_io_output_payload_ctrl_memCtrl_memWr; // @ 23060082.scala l60
      idu_io_output_payload_regNextWhen_ctrl_memCtrl_memOp <= idu_io_output_payload_ctrl_memCtrl_memOp; // @ 23060082.scala l60
      idu_io_output_payload_regNextWhen_ctrl_csrCtrl_csrCmd <= idu_io_output_payload_ctrl_csrCtrl_csrCmd; // @ 23060082.scala l60
      idu_io_output_payload_regNextWhen_ctrl_csrCtrl_illegal <= idu_io_output_payload_ctrl_csrCtrl_illegal; // @ 23060082.scala l60
      idu_io_output_payload_regNextWhen_ctrl_csrCtrl_ebreak <= idu_io_output_payload_ctrl_csrCtrl_ebreak; // @ 23060082.scala l60
      idu_io_output_payload_regNextWhen_ctrl_csrCtrl_trapEnter <= idu_io_output_payload_ctrl_csrCtrl_trapEnter; // @ 23060082.scala l60
      idu_io_output_payload_regNextWhen_ctrl_csrCtrl_trapExit <= idu_io_output_payload_ctrl_csrCtrl_trapExit; // @ 23060082.scala l60
      idu_io_output_payload_regNextWhen_ctrl_fenceI <= idu_io_output_payload_ctrl_fenceI; // @ 23060082.scala l60
      idu_io_output_payload_regNextWhen_imm <= idu_io_output_payload_imm; // @ 23060082.scala l60
      idu_io_output_payload_regNextWhen_rfReadData1 <= idu_io_output_payload_rfReadData1; // @ 23060082.scala l60
      idu_io_output_payload_regNextWhen_rfReadData2 <= idu_io_output_payload_rfReadData2; // @ 23060082.scala l60
      idu_io_output_payload_regNextWhen_isCalc <= idu_io_output_payload_isCalc; // @ 23060082.scala l60
    end
    if(exu_io_output_fire) begin
      exu_io_output_payload_regNextWhen_pc <= exu_io_output_payload_pc; // @ 23060082.scala l60
      exu_io_output_payload_regNextWhen_pcNext <= exu_io_output_payload_pcNext; // @ 23060082.scala l60
      exu_io_output_payload_regNextWhen_rfCtrl_mem2reg <= exu_io_output_payload_rfCtrl_mem2reg; // @ 23060082.scala l60
      exu_io_output_payload_regNextWhen_rfCtrl_csr2reg <= exu_io_output_payload_rfCtrl_csr2reg; // @ 23060082.scala l60
      exu_io_output_payload_regNextWhen_rfCtrl_regWr <= exu_io_output_payload_rfCtrl_regWr; // @ 23060082.scala l60
      exu_io_output_payload_regNextWhen_rfCtrl_rfWriteAddr <= exu_io_output_payload_rfCtrl_rfWriteAddr; // @ 23060082.scala l60
      exu_io_output_payload_regNextWhen_memCtrl_memWr <= exu_io_output_payload_memCtrl_memWr; // @ 23060082.scala l60
      exu_io_output_payload_regNextWhen_memCtrl_memOp <= exu_io_output_payload_memCtrl_memOp; // @ 23060082.scala l60
      exu_io_output_payload_regNextWhen_csrCtrl_csrCmd <= exu_io_output_payload_csrCtrl_csrCmd; // @ 23060082.scala l60
      exu_io_output_payload_regNextWhen_csrCtrl_illegal <= exu_io_output_payload_csrCtrl_illegal; // @ 23060082.scala l60
      exu_io_output_payload_regNextWhen_csrCtrl_ebreak <= exu_io_output_payload_csrCtrl_ebreak; // @ 23060082.scala l60
      exu_io_output_payload_regNextWhen_csrCtrl_trapEnter <= exu_io_output_payload_csrCtrl_trapEnter; // @ 23060082.scala l60
      exu_io_output_payload_regNextWhen_csrCtrl_trapExit <= exu_io_output_payload_csrCtrl_trapExit; // @ 23060082.scala l60
      exu_io_output_payload_regNextWhen_csrAddr <= exu_io_output_payload_csrAddr; // @ 23060082.scala l60
      exu_io_output_payload_regNextWhen_rfReadData <= exu_io_output_payload_rfReadData; // @ 23060082.scala l60
      exu_io_output_payload_regNextWhen_aluResult <= exu_io_output_payload_aluResult; // @ 23060082.scala l60
      exu_io_output_payload_regNextWhen_fenceI <= exu_io_output_payload_fenceI; // @ 23060082.scala l60
    end
    if(lsu_io_output_fire) begin
      lsu_io_output_payload_regNextWhen_pc <= lsu_io_output_payload_pc; // @ 23060082.scala l81
      lsu_io_output_payload_regNextWhen_pcNext <= lsu_io_output_payload_pcNext; // @ 23060082.scala l81
      lsu_io_output_payload_regNextWhen_mem_data_out <= lsu_io_output_payload_mem_data_out; // @ 23060082.scala l81
      lsu_io_output_payload_regNextWhen_alu_data_out <= lsu_io_output_payload_alu_data_out; // @ 23060082.scala l81
      lsu_io_output_payload_regNextWhen_rfCtrl_mem2reg <= lsu_io_output_payload_rfCtrl_mem2reg; // @ 23060082.scala l81
      lsu_io_output_payload_regNextWhen_rfCtrl_csr2reg <= lsu_io_output_payload_rfCtrl_csr2reg; // @ 23060082.scala l81
      lsu_io_output_payload_regNextWhen_rfCtrl_regWr <= lsu_io_output_payload_rfCtrl_regWr; // @ 23060082.scala l81
      lsu_io_output_payload_regNextWhen_rfCtrl_rfWriteAddr <= lsu_io_output_payload_rfCtrl_rfWriteAddr; // @ 23060082.scala l81
      lsu_io_output_payload_regNextWhen_fenceI <= lsu_io_output_payload_fenceI; // @ 23060082.scala l81
    end
  end

  always @(posedge clock or posedge reset) begin
    if(reset) begin
      _zz_io_output_ready <= 1'b0; // @ Data.scala l432
      _zz_io_output_ready_1 <= 1'b0; // @ Data.scala l432
      _zz_io_output_ready_2 <= 1'b0; // @ Data.scala l432
      _zz_io_input_valid <= 1'b0; // @ Data.scala l432
      _zz_instr <= 32'h0; // @ Data.scala l432
      _zz_instr_1 <= 32'h0; // @ Data.scala l432
      _zz_instr_2 <= 32'h0; // @ Data.scala l432
      _zz_instr_3 <= 32'h0; // @ Data.scala l432
    end else begin
      if(ifu_io_output_fire) begin
        _zz_io_output_ready <= 1'b1; // @ 23060082.scala l64
      end else begin
        if(idu_io_output_fire) begin
          _zz_io_output_ready <= 1'b0; // @ 23060082.scala l66
        end else begin
          _zz_io_output_ready <= _zz_io_output_ready; // @ 23060082.scala l68
        end
      end
      if(idu_io_output_fire) begin
        _zz_io_output_ready_1 <= 1'b1; // @ 23060082.scala l64
      end else begin
        if(exu_io_output_fire) begin
          _zz_io_output_ready_1 <= 1'b0; // @ 23060082.scala l66
        end else begin
          _zz_io_output_ready_1 <= _zz_io_output_ready_1; // @ 23060082.scala l68
        end
      end
      if(exu_io_output_fire) begin
        _zz_io_output_ready_2 <= 1'b1; // @ 23060082.scala l64
      end else begin
        if(lsu_io_output_fire) begin
          _zz_io_output_ready_2 <= 1'b0; // @ 23060082.scala l66
        end else begin
          _zz_io_output_ready_2 <= _zz_io_output_ready_2; // @ 23060082.scala l68
        end
      end
      if(lsu_io_output_fire) begin
        _zz_io_input_valid <= 1'b1; // @ 23060082.scala l85
      end else begin
        _zz_io_input_valid <= 1'b0; // @ 23060082.scala l87
      end
      if(ifu_io_output_fire) begin
        _zz_instr <= ifu_io_output_payload_instr; // @ 23060082.scala l127
      end
      if(idu_io_output_fire) begin
        _zz_instr_1 <= _zz_instr; // @ 23060082.scala l128
      end
      if(exu_io_output_fire) begin
        _zz_instr_2 <= _zz_instr_1; // @ 23060082.scala l129
      end
      if(lsu_io_output_fire) begin
        _zz_instr_3 <= _zz_instr_2; // @ 23060082.scala l130
      end
    end
  end


endmodule

module ysyx_23060082_Clint (
  input  wire          io_clintAxi4_aw_valid,
  output wire          io_clintAxi4_aw_ready,
  input  wire [31:0]   io_clintAxi4_aw_payload_addr,
  input  wire [3:0]    io_clintAxi4_aw_payload_id,
  input  wire [7:0]    io_clintAxi4_aw_payload_len,
  input  wire [2:0]    io_clintAxi4_aw_payload_size,
  input  wire [1:0]    io_clintAxi4_aw_payload_burst,
  input  wire          io_clintAxi4_w_valid,
  output wire          io_clintAxi4_w_ready,
  input  wire [31:0]   io_clintAxi4_w_payload_data,
  input  wire [3:0]    io_clintAxi4_w_payload_strb,
  input  wire          io_clintAxi4_w_payload_last,
  output reg           io_clintAxi4_b_valid,
  input  wire          io_clintAxi4_b_ready,
  output wire [3:0]    io_clintAxi4_b_payload_id,
  output wire [1:0]    io_clintAxi4_b_payload_resp,
  input  wire          io_clintAxi4_ar_valid,
  output wire          io_clintAxi4_ar_ready,
  input  wire [31:0]   io_clintAxi4_ar_payload_addr,
  input  wire [3:0]    io_clintAxi4_ar_payload_id,
  input  wire [7:0]    io_clintAxi4_ar_payload_len,
  input  wire [2:0]    io_clintAxi4_ar_payload_size,
  input  wire [1:0]    io_clintAxi4_ar_payload_burst,
  output wire          io_clintAxi4_r_valid,
  input  wire          io_clintAxi4_r_ready,
  output wire [31:0]   io_clintAxi4_r_payload_data,
  output wire [3:0]    io_clintAxi4_r_payload_id,
  output wire [1:0]    io_clintAxi4_r_payload_resp,
  output wire          io_clintAxi4_r_payload_last,
  input  wire          clock,
  input  wire          reset
);

  reg        [63:0]   timeCount;
  wire                io_clintAxi4_ar_fire;
  wire                readLow;
  wire                readHigh;
  reg        [31:0]   timeCountHighSnap;
  reg        [7:0]    readLen;
  reg        [7:0]    readCnt;
  reg                 readActive;
  wire                io_clintAxi4_r_fire;
  wire                when_CLINT_l36;
  wire                when_CLINT_l44;
  reg        [31:0]   readData;
  reg        [31:0]   _zz_readData;
  wire                wAllValid;
  wire                io_clintAxi4_b_fire;

  assign io_clintAxi4_ar_fire = (io_clintAxi4_ar_valid && io_clintAxi4_ar_ready); // @ BaseType.scala l308
  assign readLow = (io_clintAxi4_ar_fire && (io_clintAxi4_ar_payload_addr == 32'h02000000)); // @ BaseType.scala l308
  assign readHigh = (io_clintAxi4_ar_fire && (io_clintAxi4_ar_payload_addr == 32'h02000004)); // @ BaseType.scala l308
  assign io_clintAxi4_r_fire = (io_clintAxi4_r_valid && io_clintAxi4_r_ready); // @ BaseType.scala l308
  assign when_CLINT_l36 = (io_clintAxi4_r_fire && (readCnt != readLen)); // @ BaseType.scala l308
  assign when_CLINT_l44 = (io_clintAxi4_r_fire && (readCnt == readLen)); // @ BaseType.scala l308
  always @(*) begin
    case(io_clintAxi4_ar_payload_addr)
      32'h02000004 : begin
        _zz_readData = timeCountHighSnap; // @ Misc.scala l258
      end
      32'h02000000 : begin
        _zz_readData = timeCount[31 : 0]; // @ Misc.scala l258
      end
      default : begin
        _zz_readData = 32'h0; // @ Misc.scala l254
      end
    endcase
  end

  assign io_clintAxi4_ar_ready = (! readActive); // @ CLINT.scala l60
  assign io_clintAxi4_r_valid = readActive; // @ CLINT.scala l61
  assign io_clintAxi4_r_payload_last = (readActive && (readCnt == readLen)); // @ CLINT.scala l62
  assign io_clintAxi4_r_payload_data = readData; // @ CLINT.scala l63
  assign io_clintAxi4_r_payload_id = 4'b0000; // @ CLINT.scala l64
  assign io_clintAxi4_r_payload_resp = 2'b00; // @ CLINT.scala l65
  assign wAllValid = (io_clintAxi4_aw_valid && io_clintAxi4_w_valid); // @ BaseType.scala l308
  assign io_clintAxi4_aw_ready = wAllValid; // @ CLINT.scala l68
  assign io_clintAxi4_w_ready = wAllValid; // @ CLINT.scala l69
  assign io_clintAxi4_b_fire = (io_clintAxi4_b_valid && io_clintAxi4_b_ready); // @ BaseType.scala l308
  assign io_clintAxi4_b_payload_id = 4'b0000; // @ CLINT.scala l80
  assign io_clintAxi4_b_payload_resp = 2'b00; // @ CLINT.scala l81
  always @(posedge clock or posedge reset) begin
    if(reset) begin
      timeCount <= 64'h0; // @ Data.scala l432
      timeCountHighSnap <= 32'h0; // @ Data.scala l432
      io_clintAxi4_b_valid <= 1'b0; // @ Data.scala l432
      readLen <= 8'h0; // @ Data.scala l432
      readCnt <= 8'h0; // @ Data.scala l432
      readActive <= 1'b0; // @ Data.scala l432
    end else begin
      timeCount <= (timeCount + 64'h0000000000000001); // @ CLINT.scala l19
      if(readLow) begin
        timeCountHighSnap <= timeCount[63 : 32]; // @ CLINT.scala l25
      end
      if(io_clintAxi4_ar_fire) begin
        readLen <= io_clintAxi4_ar_payload_len; // @ CLINT.scala l30
      end
      if(io_clintAxi4_ar_fire) begin
        readCnt <= 8'h0; // @ CLINT.scala l35
      end else begin
        if(when_CLINT_l36) begin
          readCnt <= (readCnt + 8'h01); // @ CLINT.scala l37
        end else begin
          readCnt <= readCnt; // @ CLINT.scala l39
        end
      end
      if(io_clintAxi4_ar_fire) begin
        readActive <= 1'b1; // @ CLINT.scala l43
      end else begin
        if(when_CLINT_l44) begin
          readActive <= 1'b0; // @ CLINT.scala l45
        end else begin
          readActive <= readActive; // @ CLINT.scala l47
        end
      end
      if(wAllValid) begin
        `ifndef SYNTHESIS
          `ifdef FORMAL
            assert(1'b0); // core.scala:L569
          `else
            if(!1'b0) begin
              $display("NOTE should not write to there!%x", io_clintAxi4_aw_payload_addr); // core.scala:L569
            end
          `endif
        `endif
      end
      if(wAllValid) begin
        io_clintAxi4_b_valid <= 1'b1; // @ CLINT.scala l74
      end else begin
        if(io_clintAxi4_b_fire) begin
          io_clintAxi4_b_valid <= 1'b0; // @ CLINT.scala l76
        end else begin
          io_clintAxi4_b_valid <= io_clintAxi4_b_valid; // @ CLINT.scala l78
        end
      end
    end
  end

  always @(posedge clock) begin
    if(io_clintAxi4_ar_fire) begin
      readData <= _zz_readData; // @ CLINT.scala l53
    end
  end


endmodule

module ysyx_23060082_AXI4Xbar (
  input  wire          io_ifuAxi4_ar_valid,
  output wire          io_ifuAxi4_ar_ready,
  input  wire [31:0]   io_ifuAxi4_ar_payload_addr,
  input  wire [3:0]    io_ifuAxi4_ar_payload_id,
  input  wire [7:0]    io_ifuAxi4_ar_payload_len,
  input  wire [2:0]    io_ifuAxi4_ar_payload_size,
  input  wire [1:0]    io_ifuAxi4_ar_payload_burst,
  output wire          io_ifuAxi4_r_valid,
  input  wire          io_ifuAxi4_r_ready,
  output wire [31:0]   io_ifuAxi4_r_payload_data,
  output wire [3:0]    io_ifuAxi4_r_payload_id,
  output wire [1:0]    io_ifuAxi4_r_payload_resp,
  output wire          io_ifuAxi4_r_payload_last,
  input  wire          io_lsuAxi4_aw_valid,
  output wire          io_lsuAxi4_aw_ready,
  input  wire [31:0]   io_lsuAxi4_aw_payload_addr,
  input  wire [3:0]    io_lsuAxi4_aw_payload_id,
  input  wire [7:0]    io_lsuAxi4_aw_payload_len,
  input  wire [2:0]    io_lsuAxi4_aw_payload_size,
  input  wire [1:0]    io_lsuAxi4_aw_payload_burst,
  input  wire          io_lsuAxi4_w_valid,
  output wire          io_lsuAxi4_w_ready,
  input  wire [31:0]   io_lsuAxi4_w_payload_data,
  input  wire [3:0]    io_lsuAxi4_w_payload_strb,
  input  wire          io_lsuAxi4_w_payload_last,
  output wire          io_lsuAxi4_b_valid,
  input  wire          io_lsuAxi4_b_ready,
  output wire [3:0]    io_lsuAxi4_b_payload_id,
  output wire [1:0]    io_lsuAxi4_b_payload_resp,
  input  wire          io_lsuAxi4_ar_valid,
  output wire          io_lsuAxi4_ar_ready,
  input  wire [31:0]   io_lsuAxi4_ar_payload_addr,
  input  wire [3:0]    io_lsuAxi4_ar_payload_id,
  input  wire [7:0]    io_lsuAxi4_ar_payload_len,
  input  wire [2:0]    io_lsuAxi4_ar_payload_size,
  input  wire [1:0]    io_lsuAxi4_ar_payload_burst,
  output wire          io_lsuAxi4_r_valid,
  input  wire          io_lsuAxi4_r_ready,
  output wire [31:0]   io_lsuAxi4_r_payload_data,
  output wire [3:0]    io_lsuAxi4_r_payload_id,
  output wire [1:0]    io_lsuAxi4_r_payload_resp,
  output wire          io_lsuAxi4_r_payload_last,
  output wire          io_clintAxi4_aw_valid,
  input  wire          io_clintAxi4_aw_ready,
  output wire [31:0]   io_clintAxi4_aw_payload_addr,
  output wire [3:0]    io_clintAxi4_aw_payload_id,
  output wire [7:0]    io_clintAxi4_aw_payload_len,
  output wire [2:0]    io_clintAxi4_aw_payload_size,
  output wire [1:0]    io_clintAxi4_aw_payload_burst,
  output wire          io_clintAxi4_w_valid,
  input  wire          io_clintAxi4_w_ready,
  output wire [31:0]   io_clintAxi4_w_payload_data,
  output wire [3:0]    io_clintAxi4_w_payload_strb,
  output wire          io_clintAxi4_w_payload_last,
  input  wire          io_clintAxi4_b_valid,
  output wire          io_clintAxi4_b_ready,
  input  wire [3:0]    io_clintAxi4_b_payload_id,
  input  wire [1:0]    io_clintAxi4_b_payload_resp,
  output wire          io_clintAxi4_ar_valid,
  input  wire          io_clintAxi4_ar_ready,
  output wire [31:0]   io_clintAxi4_ar_payload_addr,
  output wire [3:0]    io_clintAxi4_ar_payload_id,
  output wire [7:0]    io_clintAxi4_ar_payload_len,
  output wire [2:0]    io_clintAxi4_ar_payload_size,
  output wire [1:0]    io_clintAxi4_ar_payload_burst,
  input  wire          io_clintAxi4_r_valid,
  output wire          io_clintAxi4_r_ready,
  input  wire [31:0]   io_clintAxi4_r_payload_data,
  input  wire [3:0]    io_clintAxi4_r_payload_id,
  input  wire [1:0]    io_clintAxi4_r_payload_resp,
  input  wire          io_clintAxi4_r_payload_last,
  output wire          io_externalAxi4_aw_valid,
  input  wire          io_externalAxi4_aw_ready,
  output wire [31:0]   io_externalAxi4_aw_payload_addr,
  output wire [3:0]    io_externalAxi4_aw_payload_id,
  output wire [7:0]    io_externalAxi4_aw_payload_len,
  output wire [2:0]    io_externalAxi4_aw_payload_size,
  output wire [1:0]    io_externalAxi4_aw_payload_burst,
  output wire          io_externalAxi4_w_valid,
  input  wire          io_externalAxi4_w_ready,
  output wire [31:0]   io_externalAxi4_w_payload_data,
  output wire [3:0]    io_externalAxi4_w_payload_strb,
  output wire          io_externalAxi4_w_payload_last,
  input  wire          io_externalAxi4_b_valid,
  output wire          io_externalAxi4_b_ready,
  input  wire [3:0]    io_externalAxi4_b_payload_id,
  input  wire [1:0]    io_externalAxi4_b_payload_resp,
  output wire          io_externalAxi4_ar_valid,
  input  wire          io_externalAxi4_ar_ready,
  output wire [31:0]   io_externalAxi4_ar_payload_addr,
  output wire [3:0]    io_externalAxi4_ar_payload_id,
  output wire [7:0]    io_externalAxi4_ar_payload_len,
  output wire [2:0]    io_externalAxi4_ar_payload_size,
  output wire [1:0]    io_externalAxi4_ar_payload_burst,
  input  wire          io_externalAxi4_r_valid,
  output wire          io_externalAxi4_r_ready,
  input  wire [31:0]   io_externalAxi4_r_payload_data,
  input  wire [3:0]    io_externalAxi4_r_payload_id,
  input  wire [1:0]    io_externalAxi4_r_payload_resp,
  input  wire          io_externalAxi4_r_payload_last,
  input  wire          clock,
  input  wire          reset
);
  localparam ArbiterState_Idle = 2'd0;
  localparam ArbiterState_IfuUsing = 2'd1;
  localparam ArbiterState_LsuUsing = 2'd2;
  localparam CrossState_Idle = 2'd0;
  localparam CrossState_Clint = 2'd1;
  localparam CrossState_External = 2'd2;

  wire                busAxi4_aw_valid;
  wire                busAxi4_aw_ready;
  wire       [31:0]   busAxi4_aw_payload_addr;
  wire       [3:0]    busAxi4_aw_payload_id;
  wire       [7:0]    busAxi4_aw_payload_len;
  wire       [2:0]    busAxi4_aw_payload_size;
  wire       [1:0]    busAxi4_aw_payload_burst;
  wire                busAxi4_w_valid;
  wire                busAxi4_w_ready;
  wire       [31:0]   busAxi4_w_payload_data;
  wire       [3:0]    busAxi4_w_payload_strb;
  wire                busAxi4_w_payload_last;
  wire                busAxi4_b_valid;
  wire                busAxi4_b_ready;
  wire       [3:0]    busAxi4_b_payload_id;
  wire       [1:0]    busAxi4_b_payload_resp;
  wire                busAxi4_ar_valid;
  wire                busAxi4_ar_ready;
  wire       [31:0]   busAxi4_ar_payload_addr;
  wire       [3:0]    busAxi4_ar_payload_id;
  wire       [7:0]    busAxi4_ar_payload_len;
  wire       [2:0]    busAxi4_ar_payload_size;
  wire       [1:0]    busAxi4_ar_payload_burst;
  wire                busAxi4_r_valid;
  wire                busAxi4_r_ready;
  wire       [31:0]   busAxi4_r_payload_data;
  wire       [3:0]    busAxi4_r_payload_id;
  wire       [1:0]    busAxi4_r_payload_resp;
  wire                busAxi4_r_payload_last;
  reg        [1:0]    arbiterState_1;
  wire                io_ifuAxi4_r_fire;
  wire                when_Xbar_l45;
  wire                io_lsuAxi4_r_fire;
  wire                when_Xbar_l49;
  wire                _zz_busAxi4_ar_payload_addr;
  reg        [1:0]    readState;
  wire                when_Xbar_l81;
  wire                busAxi4_r_fire;
  wire                when_Xbar_l88;
  reg        [1:0]    writeState;
  wire                when_Xbar_l98;
  wire                busAxi4_b_fire;
  wire                _zz_busAxi4_r_payload_data;
  wire                _zz_busAxi4_b_payload_id;
  `ifndef SYNTHESIS
  reg [63:0] arbiterState_1_string;
  reg [63:0] readState_string;
  reg [63:0] writeState_string;
  `endif


  `ifndef SYNTHESIS
  always @(*) begin
    case(arbiterState_1)
      ArbiterState_Idle : arbiterState_1_string = "Idle    ";
      ArbiterState_IfuUsing : arbiterState_1_string = "IfuUsing";
      ArbiterState_LsuUsing : arbiterState_1_string = "LsuUsing";
      default : arbiterState_1_string = "????????";
    endcase
  end
  always @(*) begin
    case(readState)
      CrossState_Idle : readState_string = "Idle    ";
      CrossState_Clint : readState_string = "Clint   ";
      CrossState_External : readState_string = "External";
      default : readState_string = "????????";
    endcase
  end
  always @(*) begin
    case(writeState)
      CrossState_Idle : writeState_string = "Idle    ";
      CrossState_Clint : writeState_string = "Clint   ";
      CrossState_External : writeState_string = "External";
      default : writeState_string = "????????";
    endcase
  end
  `endif

  assign io_ifuAxi4_r_fire = (io_ifuAxi4_r_valid && io_ifuAxi4_r_ready); // @ BaseType.scala l308
  assign when_Xbar_l45 = (io_ifuAxi4_r_fire && io_ifuAxi4_r_payload_last); // @ BaseType.scala l308
  assign io_lsuAxi4_r_fire = (io_lsuAxi4_r_valid && io_lsuAxi4_r_ready); // @ BaseType.scala l308
  assign when_Xbar_l49 = (io_lsuAxi4_r_fire && io_lsuAxi4_r_payload_last); // @ BaseType.scala l308
  assign _zz_busAxi4_ar_payload_addr = (arbiterState_1 == ArbiterState_IfuUsing); // @ BaseType.scala l308
  assign busAxi4_ar_payload_addr = (_zz_busAxi4_ar_payload_addr ? io_ifuAxi4_ar_payload_addr : io_lsuAxi4_ar_payload_addr); // @ Xbar.scala l54
  assign busAxi4_ar_payload_id = (_zz_busAxi4_ar_payload_addr ? io_ifuAxi4_ar_payload_id : io_lsuAxi4_ar_payload_id); // @ Xbar.scala l54
  assign busAxi4_ar_payload_len = (_zz_busAxi4_ar_payload_addr ? io_ifuAxi4_ar_payload_len : io_lsuAxi4_ar_payload_len); // @ Xbar.scala l54
  assign busAxi4_ar_payload_size = (_zz_busAxi4_ar_payload_addr ? io_ifuAxi4_ar_payload_size : io_lsuAxi4_ar_payload_size); // @ Xbar.scala l54
  assign busAxi4_ar_payload_burst = (_zz_busAxi4_ar_payload_addr ? io_ifuAxi4_ar_payload_burst : io_lsuAxi4_ar_payload_burst); // @ Xbar.scala l54
  assign busAxi4_ar_valid = (((arbiterState_1 == ArbiterState_IfuUsing) && io_ifuAxi4_ar_valid) || ((arbiterState_1 == ArbiterState_LsuUsing) && io_lsuAxi4_ar_valid)); // @ Xbar.scala l55
  assign io_ifuAxi4_ar_ready = ((arbiterState_1 == ArbiterState_IfuUsing) && busAxi4_ar_ready); // @ Xbar.scala l57
  assign io_lsuAxi4_ar_ready = ((arbiterState_1 == ArbiterState_LsuUsing) && busAxi4_ar_ready); // @ Xbar.scala l58
  assign io_ifuAxi4_r_payload_data = busAxi4_r_payload_data; // @ Xbar.scala l60
  assign io_ifuAxi4_r_payload_id = busAxi4_r_payload_id; // @ Xbar.scala l60
  assign io_ifuAxi4_r_payload_resp = busAxi4_r_payload_resp; // @ Xbar.scala l60
  assign io_ifuAxi4_r_payload_last = busAxi4_r_payload_last; // @ Xbar.scala l60
  assign io_lsuAxi4_r_payload_data = busAxi4_r_payload_data; // @ Xbar.scala l61
  assign io_lsuAxi4_r_payload_id = busAxi4_r_payload_id; // @ Xbar.scala l61
  assign io_lsuAxi4_r_payload_resp = busAxi4_r_payload_resp; // @ Xbar.scala l61
  assign io_lsuAxi4_r_payload_last = busAxi4_r_payload_last; // @ Xbar.scala l61
  assign io_ifuAxi4_r_valid = ((arbiterState_1 == ArbiterState_IfuUsing) && busAxi4_r_valid); // @ Xbar.scala l62
  assign io_lsuAxi4_r_valid = ((arbiterState_1 == ArbiterState_LsuUsing) && busAxi4_r_valid); // @ Xbar.scala l63
  assign busAxi4_r_ready = (((arbiterState_1 == ArbiterState_IfuUsing) && io_ifuAxi4_r_ready) || ((arbiterState_1 == ArbiterState_LsuUsing) && io_lsuAxi4_r_ready)); // @ Xbar.scala l64
  assign busAxi4_aw_valid = io_lsuAxi4_aw_valid; // @ Xbar.scala l67
  assign io_lsuAxi4_aw_ready = busAxi4_aw_ready; // @ Xbar.scala l67
  assign busAxi4_aw_payload_addr = io_lsuAxi4_aw_payload_addr; // @ Xbar.scala l67
  assign busAxi4_aw_payload_id = io_lsuAxi4_aw_payload_id; // @ Xbar.scala l67
  assign busAxi4_aw_payload_len = io_lsuAxi4_aw_payload_len; // @ Xbar.scala l67
  assign busAxi4_aw_payload_size = io_lsuAxi4_aw_payload_size; // @ Xbar.scala l67
  assign busAxi4_aw_payload_burst = io_lsuAxi4_aw_payload_burst; // @ Xbar.scala l67
  assign busAxi4_w_valid = io_lsuAxi4_w_valid; // @ Xbar.scala l68
  assign io_lsuAxi4_w_ready = busAxi4_w_ready; // @ Xbar.scala l68
  assign busAxi4_w_payload_data = io_lsuAxi4_w_payload_data; // @ Xbar.scala l68
  assign busAxi4_w_payload_strb = io_lsuAxi4_w_payload_strb; // @ Xbar.scala l68
  assign busAxi4_w_payload_last = io_lsuAxi4_w_payload_last; // @ Xbar.scala l68
  assign io_lsuAxi4_b_valid = busAxi4_b_valid; // @ Xbar.scala l69
  assign busAxi4_b_ready = io_lsuAxi4_b_ready; // @ Xbar.scala l69
  assign io_lsuAxi4_b_payload_id = busAxi4_b_payload_id; // @ Xbar.scala l69
  assign io_lsuAxi4_b_payload_resp = busAxi4_b_payload_resp; // @ Xbar.scala l69
  assign when_Xbar_l81 = ((32'h02000000 <= busAxi4_ar_payload_addr) && (busAxi4_ar_payload_addr <= 32'h0200ffff)); // @ BaseType.scala l308
  assign busAxi4_r_fire = (busAxi4_r_valid && busAxi4_r_ready); // @ BaseType.scala l308
  assign when_Xbar_l88 = (busAxi4_r_fire && busAxi4_r_payload_last); // @ BaseType.scala l308
  assign when_Xbar_l98 = ((32'h02000000 <= busAxi4_aw_payload_addr) && (busAxi4_aw_payload_addr <= 32'h0200ffff)); // @ BaseType.scala l308
  assign busAxi4_b_fire = (busAxi4_b_valid && busAxi4_b_ready); // @ BaseType.scala l308
  assign io_clintAxi4_ar_valid = ((readState == CrossState_Clint) && busAxi4_ar_valid); // @ Xbar.scala l112
  assign io_clintAxi4_ar_payload_addr = busAxi4_ar_payload_addr; // @ Xbar.scala l113
  assign io_clintAxi4_ar_payload_id = busAxi4_ar_payload_id; // @ Xbar.scala l113
  assign io_clintAxi4_ar_payload_len = busAxi4_ar_payload_len; // @ Xbar.scala l113
  assign io_clintAxi4_ar_payload_size = busAxi4_ar_payload_size; // @ Xbar.scala l113
  assign io_clintAxi4_ar_payload_burst = busAxi4_ar_payload_burst; // @ Xbar.scala l113
  assign io_externalAxi4_ar_valid = ((readState == CrossState_External) && busAxi4_ar_valid); // @ Xbar.scala l115
  assign io_externalAxi4_ar_payload_addr = busAxi4_ar_payload_addr; // @ Xbar.scala l116
  assign io_externalAxi4_ar_payload_id = busAxi4_ar_payload_id; // @ Xbar.scala l116
  assign io_externalAxi4_ar_payload_len = busAxi4_ar_payload_len; // @ Xbar.scala l116
  assign io_externalAxi4_ar_payload_size = busAxi4_ar_payload_size; // @ Xbar.scala l116
  assign io_externalAxi4_ar_payload_burst = busAxi4_ar_payload_burst; // @ Xbar.scala l116
  assign busAxi4_ar_ready = (((readState == CrossState_Clint) && io_clintAxi4_ar_ready) || ((readState == CrossState_External) && io_externalAxi4_ar_ready)); // @ Xbar.scala l118
  assign busAxi4_r_valid = (((readState == CrossState_Clint) && io_clintAxi4_r_valid) || ((readState == CrossState_External) && io_externalAxi4_r_valid)); // @ Xbar.scala l121
  assign _zz_busAxi4_r_payload_data = (readState == CrossState_Clint); // @ BaseType.scala l308
  assign busAxi4_r_payload_data = (_zz_busAxi4_r_payload_data ? io_clintAxi4_r_payload_data : io_externalAxi4_r_payload_data); // @ Xbar.scala l123
  assign busAxi4_r_payload_id = (_zz_busAxi4_r_payload_data ? io_clintAxi4_r_payload_id : io_externalAxi4_r_payload_id); // @ Xbar.scala l123
  assign busAxi4_r_payload_resp = (_zz_busAxi4_r_payload_data ? io_clintAxi4_r_payload_resp : io_externalAxi4_r_payload_resp); // @ Xbar.scala l123
  assign busAxi4_r_payload_last = (_zz_busAxi4_r_payload_data ? io_clintAxi4_r_payload_last : io_externalAxi4_r_payload_last); // @ Xbar.scala l123
  assign io_clintAxi4_r_ready = ((readState == CrossState_Clint) && busAxi4_r_ready); // @ Xbar.scala l124
  assign io_externalAxi4_r_ready = ((readState == CrossState_External) && busAxi4_r_ready); // @ Xbar.scala l126
  assign io_clintAxi4_aw_valid = ((writeState == CrossState_Clint) && busAxi4_aw_valid); // @ Xbar.scala l129
  assign io_clintAxi4_aw_payload_addr = busAxi4_aw_payload_addr; // @ Xbar.scala l130
  assign io_clintAxi4_aw_payload_id = busAxi4_aw_payload_id; // @ Xbar.scala l130
  assign io_clintAxi4_aw_payload_len = busAxi4_aw_payload_len; // @ Xbar.scala l130
  assign io_clintAxi4_aw_payload_size = busAxi4_aw_payload_size; // @ Xbar.scala l130
  assign io_clintAxi4_aw_payload_burst = busAxi4_aw_payload_burst; // @ Xbar.scala l130
  assign io_externalAxi4_aw_valid = ((writeState == CrossState_External) && busAxi4_aw_valid); // @ Xbar.scala l132
  assign io_externalAxi4_aw_payload_addr = busAxi4_aw_payload_addr; // @ Xbar.scala l133
  assign io_externalAxi4_aw_payload_id = busAxi4_aw_payload_id; // @ Xbar.scala l133
  assign io_externalAxi4_aw_payload_len = busAxi4_aw_payload_len; // @ Xbar.scala l133
  assign io_externalAxi4_aw_payload_size = busAxi4_aw_payload_size; // @ Xbar.scala l133
  assign io_externalAxi4_aw_payload_burst = busAxi4_aw_payload_burst; // @ Xbar.scala l133
  assign busAxi4_aw_ready = (((writeState == CrossState_Clint) && io_clintAxi4_aw_ready) || ((writeState == CrossState_External) && io_externalAxi4_aw_ready)); // @ Xbar.scala l135
  assign io_clintAxi4_w_valid = ((writeState == CrossState_Clint) && busAxi4_w_valid); // @ Xbar.scala l138
  assign io_clintAxi4_w_payload_data = busAxi4_w_payload_data; // @ Xbar.scala l139
  assign io_clintAxi4_w_payload_strb = busAxi4_w_payload_strb; // @ Xbar.scala l139
  assign io_clintAxi4_w_payload_last = busAxi4_w_payload_last; // @ Xbar.scala l139
  assign io_externalAxi4_w_valid = ((writeState == CrossState_External) && busAxi4_w_valid); // @ Xbar.scala l141
  assign io_externalAxi4_w_payload_data = busAxi4_w_payload_data; // @ Xbar.scala l142
  assign io_externalAxi4_w_payload_strb = busAxi4_w_payload_strb; // @ Xbar.scala l142
  assign io_externalAxi4_w_payload_last = busAxi4_w_payload_last; // @ Xbar.scala l142
  assign busAxi4_w_ready = (((writeState == CrossState_Clint) && io_clintAxi4_w_ready) || ((writeState == CrossState_External) && io_externalAxi4_w_ready)); // @ Xbar.scala l144
  assign busAxi4_b_valid = (((writeState == CrossState_Clint) && io_clintAxi4_b_valid) || ((writeState == CrossState_External) && io_externalAxi4_b_valid)); // @ Xbar.scala l147
  assign _zz_busAxi4_b_payload_id = (writeState == CrossState_Clint); // @ BaseType.scala l308
  assign busAxi4_b_payload_id = (_zz_busAxi4_b_payload_id ? io_clintAxi4_b_payload_id : io_externalAxi4_b_payload_id); // @ Xbar.scala l149
  assign busAxi4_b_payload_resp = (_zz_busAxi4_b_payload_id ? io_clintAxi4_b_payload_resp : io_externalAxi4_b_payload_resp); // @ Xbar.scala l149
  assign io_clintAxi4_b_ready = ((writeState == CrossState_Clint) && busAxi4_b_ready); // @ Xbar.scala l150
  assign io_externalAxi4_b_ready = ((writeState == CrossState_External) && busAxi4_b_ready); // @ Xbar.scala l152
  always @(posedge clock or posedge reset) begin
    if(reset) begin
      arbiterState_1 <= ArbiterState_Idle; // @ Data.scala l432
      readState <= CrossState_Idle; // @ Data.scala l432
      writeState <= CrossState_Idle; // @ Data.scala l432
    end else begin
      case(arbiterState_1)
        ArbiterState_Idle : begin
          if(io_ifuAxi4_ar_valid) begin
            arbiterState_1 <= ArbiterState_IfuUsing; // @ Enum.scala l159
          end else begin
            if(io_lsuAxi4_ar_valid) begin
              arbiterState_1 <= ArbiterState_LsuUsing; // @ Enum.scala l159
            end else begin
              arbiterState_1 <= ArbiterState_Idle; // @ Enum.scala l159
            end
          end
        end
        ArbiterState_IfuUsing : begin
          if(when_Xbar_l45) begin
            arbiterState_1 <= ArbiterState_Idle; // @ Enum.scala l159
          end else begin
            arbiterState_1 <= ArbiterState_IfuUsing; // @ Enum.scala l159
          end
        end
        default : begin
          if(when_Xbar_l49) begin
            arbiterState_1 <= ArbiterState_Idle; // @ Enum.scala l159
          end else begin
            arbiterState_1 <= ArbiterState_LsuUsing; // @ Enum.scala l159
          end
        end
      endcase
      case(readState)
        CrossState_Idle : begin
          if(busAxi4_ar_valid) begin
            if(when_Xbar_l81) begin
              readState <= CrossState_Clint; // @ Enum.scala l159
            end else begin
              readState <= CrossState_External; // @ Enum.scala l159
            end
          end else begin
            readState <= CrossState_Idle; // @ Enum.scala l159
          end
        end
        default : begin
          if(when_Xbar_l88) begin
            readState <= CrossState_Idle; // @ Enum.scala l159
          end else begin
            readState <= readState; // @ Xbar.scala l89
          end
        end
      endcase
      case(writeState)
        CrossState_Idle : begin
          if(busAxi4_aw_valid) begin
            if(when_Xbar_l98) begin
              writeState <= CrossState_Clint; // @ Enum.scala l159
            end else begin
              writeState <= CrossState_External; // @ Enum.scala l159
            end
          end else begin
            writeState <= CrossState_Idle; // @ Enum.scala l159
          end
        end
        default : begin
          if(busAxi4_b_fire) begin
            writeState <= CrossState_Idle; // @ Enum.scala l159
          end else begin
            writeState <= writeState; // @ Xbar.scala l106
          end
        end
      endcase
    end
  end


endmodule

module ysyx_23060082_WBU (
  input  wire          io_input_valid,
  input  wire [31:0]   io_input_payload_pc,
  input  wire [31:0]   io_input_payload_pcNext,
  input  wire [31:0]   io_input_payload_mem_data_out,
  input  wire [31:0]   io_input_payload_alu_data_out,
  input  wire          io_input_payload_rfCtrl_mem2reg,
  input  wire          io_input_payload_rfCtrl_csr2reg,
  input  wire          io_input_payload_rfCtrl_regWr,
  input  wire [4:0]    io_input_payload_rfCtrl_rfWriteAddr,
  input  wire          io_input_payload_fenceI,
  output wire          io_output_valid,
  input  wire          io_output_ready,
  output wire [31:0]   io_output_payload_pcNext,
  output wire          io_output_payload_fenceI,
  output wire [4:0]    io_rfWrite_addr,
  output wire [31:0]   io_rfWrite_data,
  output wire          io_rfWrite_en
);


  assign io_output_payload_pcNext = io_input_payload_pcNext; // @ WBU.scala l19
  assign io_output_payload_fenceI = io_input_payload_fenceI; // @ WBU.scala l20
  assign io_rfWrite_addr = io_input_payload_rfCtrl_rfWriteAddr; // @ WBU.scala l21
  assign io_rfWrite_en = (io_input_payload_rfCtrl_regWr && io_input_valid); // @ WBU.scala l22
  assign io_rfWrite_data = ((io_input_payload_rfCtrl_mem2reg || io_input_payload_rfCtrl_csr2reg) ? io_input_payload_mem_data_out : io_input_payload_alu_data_out); // @ WBU.scala l23
  assign io_output_valid = io_input_valid; // @ WBU.scala l26

endmodule

module ysyx_23060082_LSU (
  input  wire          io_input_valid,
  input  wire [31:0]   io_input_payload_pc,
  input  wire [31:0]   io_input_payload_pcNext,
  input  wire          io_input_payload_rfCtrl_mem2reg,
  input  wire          io_input_payload_rfCtrl_csr2reg,
  input  wire          io_input_payload_rfCtrl_regWr,
  input  wire [4:0]    io_input_payload_rfCtrl_rfWriteAddr,
  input  wire          io_input_payload_memCtrl_memWr,
  input  wire [2:0]    io_input_payload_memCtrl_memOp,
  input  wire [2:0]    io_input_payload_csrCtrl_csrCmd,
  input  wire          io_input_payload_csrCtrl_illegal,
  input  wire          io_input_payload_csrCtrl_ebreak,
  input  wire          io_input_payload_csrCtrl_trapEnter,
  input  wire          io_input_payload_csrCtrl_trapExit,
  input  wire [11:0]   io_input_payload_csrAddr,
  input  wire [31:0]   io_input_payload_rfReadData,
  input  wire [31:0]   io_input_payload_aluResult,
  input  wire          io_input_payload_fenceI,
  output wire          io_output_valid,
  input  wire          io_output_ready,
  output wire [31:0]   io_output_payload_pc,
  output wire [31:0]   io_output_payload_pcNext,
  output wire [31:0]   io_output_payload_mem_data_out,
  output wire [31:0]   io_output_payload_alu_data_out,
  output wire          io_output_payload_rfCtrl_mem2reg,
  output wire          io_output_payload_rfCtrl_csr2reg,
  output wire          io_output_payload_rfCtrl_regWr,
  output wire [4:0]    io_output_payload_rfCtrl_rfWriteAddr,
  output wire          io_output_payload_fenceI,
  output wire          io_axi4_aw_valid,
  input  wire          io_axi4_aw_ready,
  output wire [31:0]   io_axi4_aw_payload_addr,
  output wire [3:0]    io_axi4_aw_payload_id,
  output wire [7:0]    io_axi4_aw_payload_len,
  output wire [2:0]    io_axi4_aw_payload_size,
  output wire [1:0]    io_axi4_aw_payload_burst,
  output wire          io_axi4_w_valid,
  input  wire          io_axi4_w_ready,
  output wire [31:0]   io_axi4_w_payload_data,
  output wire [3:0]    io_axi4_w_payload_strb,
  output wire          io_axi4_w_payload_last,
  input  wire          io_axi4_b_valid,
  output wire          io_axi4_b_ready,
  input  wire [3:0]    io_axi4_b_payload_id,
  input  wire [1:0]    io_axi4_b_payload_resp,
  output wire          io_axi4_ar_valid,
  input  wire          io_axi4_ar_ready,
  output wire [31:0]   io_axi4_ar_payload_addr,
  output wire [3:0]    io_axi4_ar_payload_id,
  output wire [7:0]    io_axi4_ar_payload_len,
  output wire [2:0]    io_axi4_ar_payload_size,
  output wire [1:0]    io_axi4_ar_payload_burst,
  input  wire          io_axi4_r_valid,
  output wire          io_axi4_r_ready,
  input  wire [31:0]   io_axi4_r_payload_data,
  input  wire [3:0]    io_axi4_r_payload_id,
  input  wire [1:0]    io_axi4_r_payload_resp,
  input  wire          io_axi4_r_payload_last,
  input  wire          reset,
  input  wire          clock
);
  localparam LsuState_Idle = 2'd0;
  localparam LsuState_WaitMem = 2'd1;
  localparam LsuState_Done = 2'd2;

  wire       [4:0]    dataProcess_io_addrOp;
  wire       [31:0]   dataProcess_io_rdata;
  wire                axi4Ctrler_io_readReq;
  wire                axi4Ctrler_io_writeReq;
  wire       [2:0]    axi4Ctrler_io_size;
  wire       [31:0]   csr_io_causeIn;
  reg        [3:0]    perfReg_1_req;
  reg        [3:0]    perfReg_1_rsp;
  wire       [31:0]   dataProcess_io_wdataReal;
  wire       [3:0]    dataProcess_io_wmask;
  wire       [31:0]   dataProcess_io_rdataReal;
  wire                axi4Ctrler_io_readEnd;
  wire       [31:0]   axi4Ctrler_io_readData;
  wire                axi4Ctrler_io_writeEnd;
  wire                axi4Ctrler_io_axi4_ar_valid;
  wire       [31:0]   axi4Ctrler_io_axi4_ar_payload_addr;
  wire       [3:0]    axi4Ctrler_io_axi4_ar_payload_id;
  wire       [7:0]    axi4Ctrler_io_axi4_ar_payload_len;
  wire       [2:0]    axi4Ctrler_io_axi4_ar_payload_size;
  wire       [1:0]    axi4Ctrler_io_axi4_ar_payload_burst;
  wire                axi4Ctrler_io_axi4_aw_valid;
  wire       [31:0]   axi4Ctrler_io_axi4_aw_payload_addr;
  wire       [3:0]    axi4Ctrler_io_axi4_aw_payload_id;
  wire       [7:0]    axi4Ctrler_io_axi4_aw_payload_len;
  wire       [2:0]    axi4Ctrler_io_axi4_aw_payload_size;
  wire       [1:0]    axi4Ctrler_io_axi4_aw_payload_burst;
  wire                axi4Ctrler_io_axi4_w_valid;
  wire       [31:0]   axi4Ctrler_io_axi4_w_payload_data;
  wire       [3:0]    axi4Ctrler_io_axi4_w_payload_strb;
  wire                axi4Ctrler_io_axi4_w_payload_last;
  wire                axi4Ctrler_io_axi4_r_ready;
  wire                axi4Ctrler_io_axi4_b_ready;
  wire       [31:0]   csr_io_csrRdata;
  wire       [31:0]   csr_io_mtvec;
  wire       [31:0]   csr_io_mepc;
  wire       [31:0]   _zz__zz_rsp;
  wire       [31:0]   _zz__zz_rsp_1;
  wire       [31:0]   _zz__zz_rsp_2;
  wire       [31:0]   _zz__zz_rsp_3;
  reg        [1:0]    state;
  wire                needRead;
  wire                needWrite;
  wire                needMem;
  wire                rdEnd;
  wire                wrEnd;
  reg        [31:0]   rdataReg;
  wire                when_LSU_l61;
  wire                io_output_fire;
  wire                willValid;
  wire                io_axi4_ar_fire;
  wire                io_axi4_aw_fire;
  reg                 _zz_rsp;
  wire                io_axi4_r_fire;
  wire                io_axi4_b_fire;
  `ifndef SYNTHESIS
  reg [55:0] state_string;
  `endif


  assign _zz__zz_rsp = 32'h30000000;
  assign _zz__zz_rsp_1 = 32'h40000000;
  assign _zz__zz_rsp_2 = 32'h80000000;
  assign _zz__zz_rsp_3 = 32'hc0000000;
  ysyx_23060082_DataProcess dataProcess (
    .io_addrOp    (dataProcess_io_addrOp[4:0]       ), //i
    .io_wdata     (io_input_payload_rfReadData[31:0]), //i
    .io_wdataReal (dataProcess_io_wdataReal[31:0]   ), //o
    .io_wmask     (dataProcess_io_wmask[3:0]        ), //o
    .io_rdata     (dataProcess_io_rdata[31:0]       ), //i
    .io_rdataReal (dataProcess_io_rdataReal[31:0]   )  //o
  );
  ysyx_23060082_Axi4_Ctrler axi4Ctrler (
    .io_readReq               (axi4Ctrler_io_readReq                   ), //i
    .io_writeReq              (axi4Ctrler_io_writeReq                  ), //i
    .io_size                  (axi4Ctrler_io_size[2:0]                 ), //i
    .io_readAddr              (io_input_payload_aluResult[31:0]        ), //i
    .io_writeAddr             (io_input_payload_aluResult[31:0]        ), //i
    .io_writeData             (dataProcess_io_wdataReal[31:0]          ), //i
    .io_writeMask             (dataProcess_io_wmask[3:0]               ), //i
    .io_readEnd               (axi4Ctrler_io_readEnd                   ), //o
    .io_readData              (axi4Ctrler_io_readData[31:0]            ), //o
    .io_writeEnd              (axi4Ctrler_io_writeEnd                  ), //o
    .io_axi4_aw_valid         (axi4Ctrler_io_axi4_aw_valid             ), //o
    .io_axi4_aw_ready         (io_axi4_aw_ready                        ), //i
    .io_axi4_aw_payload_addr  (axi4Ctrler_io_axi4_aw_payload_addr[31:0]), //o
    .io_axi4_aw_payload_id    (axi4Ctrler_io_axi4_aw_payload_id[3:0]   ), //o
    .io_axi4_aw_payload_len   (axi4Ctrler_io_axi4_aw_payload_len[7:0]  ), //o
    .io_axi4_aw_payload_size  (axi4Ctrler_io_axi4_aw_payload_size[2:0] ), //o
    .io_axi4_aw_payload_burst (axi4Ctrler_io_axi4_aw_payload_burst[1:0]), //o
    .io_axi4_w_valid          (axi4Ctrler_io_axi4_w_valid              ), //o
    .io_axi4_w_ready          (io_axi4_w_ready                         ), //i
    .io_axi4_w_payload_data   (axi4Ctrler_io_axi4_w_payload_data[31:0] ), //o
    .io_axi4_w_payload_strb   (axi4Ctrler_io_axi4_w_payload_strb[3:0]  ), //o
    .io_axi4_w_payload_last   (axi4Ctrler_io_axi4_w_payload_last       ), //o
    .io_axi4_b_valid          (io_axi4_b_valid                         ), //i
    .io_axi4_b_ready          (axi4Ctrler_io_axi4_b_ready              ), //o
    .io_axi4_b_payload_id     (io_axi4_b_payload_id[3:0]               ), //i
    .io_axi4_b_payload_resp   (io_axi4_b_payload_resp[1:0]             ), //i
    .io_axi4_ar_valid         (axi4Ctrler_io_axi4_ar_valid             ), //o
    .io_axi4_ar_ready         (io_axi4_ar_ready                        ), //i
    .io_axi4_ar_payload_addr  (axi4Ctrler_io_axi4_ar_payload_addr[31:0]), //o
    .io_axi4_ar_payload_id    (axi4Ctrler_io_axi4_ar_payload_id[3:0]   ), //o
    .io_axi4_ar_payload_len   (axi4Ctrler_io_axi4_ar_payload_len[7:0]  ), //o
    .io_axi4_ar_payload_size  (axi4Ctrler_io_axi4_ar_payload_size[2:0] ), //o
    .io_axi4_ar_payload_burst (axi4Ctrler_io_axi4_ar_payload_burst[1:0]), //o
    .io_axi4_r_valid          (io_axi4_r_valid                         ), //i
    .io_axi4_r_ready          (axi4Ctrler_io_axi4_r_ready              ), //o
    .io_axi4_r_payload_data   (io_axi4_r_payload_data[31:0]            ), //i
    .io_axi4_r_payload_id     (io_axi4_r_payload_id[3:0]               ), //i
    .io_axi4_r_payload_resp   (io_axi4_r_payload_resp[1:0]             ), //i
    .io_axi4_r_payload_last   (io_axi4_r_payload_last                  ), //i
    .clock                    (clock                                   ), //i
    .reset                    (reset                                   )  //i
  );
  ysyx_23060082_CSR csr (
    .io_csrAddr     (io_input_payload_csrAddr[11:0]      ), //i
    .io_csrWdata    (io_input_payload_rfReadData[31:0]   ), //i
    .io_csrRdata    (csr_io_csrRdata[31:0]               ), //o
    .io_csrCmd      (io_input_payload_csrCtrl_csrCmd[2:0]), //i
    .io_trapEnter   (io_input_payload_csrCtrl_trapEnter  ), //i
    .io_trapExit    (io_input_payload_csrCtrl_trapExit   ), //i
    .io_pcIn        (io_input_payload_pc[31:0]           ), //i
    .io_causeIn     (csr_io_causeIn[31:0]                ), //i
    .io_mtvec       (csr_io_mtvec[31:0]                  ), //o
    .io_mepc        (csr_io_mepc[31:0]                   ), //o
    .io_instrRetire (io_output_fire                      ), //i
    .clock          (clock                               ), //i
    .reset          (reset                               )  //i
  );
  PerfReg perfReg_1 (
    .clock (clock             ), //i
    .reset (reset             ), //i
    .valid (1'b1              ), //i
    .req   (perfReg_1_req[3:0]), //i
    .rsp   (perfReg_1_rsp[3:0]), //i
    .evt   (8'h0              )  //i
  );
  `ifndef SYNTHESIS
  always @(*) begin
    case(state)
      LsuState_Idle : state_string = "Idle   ";
      LsuState_WaitMem : state_string = "WaitMem";
      LsuState_Done : state_string = "Done   ";
      default : state_string = "???????";
    endcase
  end
  `endif

  assign needRead = (io_input_valid && io_input_payload_rfCtrl_mem2reg); // @ BaseType.scala l308
  assign needWrite = (io_input_valid && io_input_payload_memCtrl_memWr); // @ BaseType.scala l308
  assign needMem = (needRead || needWrite); // @ BaseType.scala l308
  assign dataProcess_io_addrOp = {io_input_payload_aluResult[1 : 0],io_input_payload_memCtrl_memOp}; // @ LSU.scala l37
  assign io_axi4_aw_valid = axi4Ctrler_io_axi4_aw_valid; // @ LSU.scala l40
  assign io_axi4_aw_payload_addr = axi4Ctrler_io_axi4_aw_payload_addr; // @ LSU.scala l40
  assign io_axi4_aw_payload_id = axi4Ctrler_io_axi4_aw_payload_id; // @ LSU.scala l40
  assign io_axi4_aw_payload_len = axi4Ctrler_io_axi4_aw_payload_len; // @ LSU.scala l40
  assign io_axi4_aw_payload_size = axi4Ctrler_io_axi4_aw_payload_size; // @ LSU.scala l40
  assign io_axi4_aw_payload_burst = axi4Ctrler_io_axi4_aw_payload_burst; // @ LSU.scala l40
  assign io_axi4_w_valid = axi4Ctrler_io_axi4_w_valid; // @ LSU.scala l40
  assign io_axi4_w_payload_data = axi4Ctrler_io_axi4_w_payload_data; // @ LSU.scala l40
  assign io_axi4_w_payload_strb = axi4Ctrler_io_axi4_w_payload_strb; // @ LSU.scala l40
  assign io_axi4_w_payload_last = axi4Ctrler_io_axi4_w_payload_last; // @ LSU.scala l40
  assign io_axi4_b_ready = axi4Ctrler_io_axi4_b_ready; // @ LSU.scala l40
  assign io_axi4_ar_valid = axi4Ctrler_io_axi4_ar_valid; // @ LSU.scala l40
  assign io_axi4_ar_payload_addr = axi4Ctrler_io_axi4_ar_payload_addr; // @ LSU.scala l40
  assign io_axi4_ar_payload_id = axi4Ctrler_io_axi4_ar_payload_id; // @ LSU.scala l40
  assign io_axi4_ar_payload_len = axi4Ctrler_io_axi4_ar_payload_len; // @ LSU.scala l40
  assign io_axi4_ar_payload_size = axi4Ctrler_io_axi4_ar_payload_size; // @ LSU.scala l40
  assign io_axi4_ar_payload_burst = axi4Ctrler_io_axi4_ar_payload_burst; // @ LSU.scala l40
  assign io_axi4_r_ready = axi4Ctrler_io_axi4_r_ready; // @ LSU.scala l40
  assign axi4Ctrler_io_readReq = (needRead && (state == LsuState_Idle)); // @ LSU.scala l41
  assign axi4Ctrler_io_writeReq = (needWrite && (state == LsuState_Idle)); // @ LSU.scala l42
  assign axi4Ctrler_io_size = {1'b0,io_input_payload_memCtrl_memOp[1 : 0]}; // @ LSU.scala l43
  assign rdEnd = (((state == LsuState_WaitMem) && axi4Ctrler_io_readEnd) && io_input_payload_rfCtrl_mem2reg); // @ BaseType.scala l308
  assign wrEnd = (((state == LsuState_WaitMem) && axi4Ctrler_io_writeEnd) && io_input_payload_memCtrl_memWr); // @ BaseType.scala l308
  assign dataProcess_io_rdata = (rdEnd ? axi4Ctrler_io_readData : rdataReg); // @ LSU.scala l52
  assign when_LSU_l61 = (rdEnd || wrEnd); // @ BaseType.scala l308
  assign io_output_fire = (io_output_valid && io_output_ready); // @ BaseType.scala l308
  assign csr_io_causeIn = (io_input_payload_csrCtrl_illegal ? 32'h00000002 : (io_input_payload_csrCtrl_ebreak ? 32'h00000003 : io_input_payload_rfReadData)); // @ LSU.scala l80
  assign willValid = (((rdEnd || wrEnd) || (state == LsuState_Done)) || (((state == LsuState_Idle) && io_input_valid) && (! needMem))); // @ BaseType.scala l308
  assign io_output_valid = (io_input_valid && willValid); // @ LSU.scala l89
  assign io_output_payload_pc = io_input_payload_pc; // @ LSU.scala l92
  assign io_output_payload_pcNext = (io_input_payload_csrCtrl_trapEnter ? csr_io_mtvec : (io_input_payload_csrCtrl_trapExit ? csr_io_mepc : io_input_payload_pcNext)); // @ LSU.scala l93
  assign io_output_payload_mem_data_out = ((io_input_payload_csrCtrl_csrCmd != 3'b000) ? csr_io_csrRdata : dataProcess_io_rdataReal); // @ LSU.scala l97
  assign io_output_payload_alu_data_out = io_input_payload_aluResult; // @ LSU.scala l98
  assign io_output_payload_rfCtrl_mem2reg = io_input_payload_rfCtrl_mem2reg; // @ LSU.scala l99
  assign io_output_payload_rfCtrl_csr2reg = io_input_payload_rfCtrl_csr2reg; // @ LSU.scala l99
  assign io_output_payload_rfCtrl_regWr = io_input_payload_rfCtrl_regWr; // @ LSU.scala l99
  assign io_output_payload_rfCtrl_rfWriteAddr = io_input_payload_rfCtrl_rfWriteAddr; // @ LSU.scala l99
  assign io_output_payload_fenceI = io_input_payload_fenceI; // @ LSU.scala l100
  assign io_axi4_ar_fire = (io_axi4_ar_valid && io_axi4_ar_ready); // @ BaseType.scala l308
  always @(*) begin
    perfReg_1_req[0] = (io_axi4_ar_fire && (! (! (((32'h30000000 <= io_axi4_ar_payload_addr) && (io_axi4_ar_payload_addr < 32'h40000000)) || ((32'h80000000 <= io_axi4_ar_payload_addr) && (io_axi4_ar_payload_addr < 32'hc0000000)))))); // @ LSU.scala l113
    perfReg_1_req[1] = (io_axi4_aw_fire && (! (! (((32'h30000000 <= io_axi4_aw_payload_addr) && (io_axi4_aw_payload_addr < 32'h40000000)) || ((32'h80000000 <= io_axi4_aw_payload_addr) && (io_axi4_aw_payload_addr < 32'hc0000000)))))); // @ LSU.scala l114
    perfReg_1_req[2] = (io_axi4_ar_fire && (! (((32'h30000000 <= io_axi4_ar_payload_addr) && (io_axi4_ar_payload_addr < 32'h40000000)) || ((32'h80000000 <= io_axi4_ar_payload_addr) && (io_axi4_ar_payload_addr < 32'hc0000000))))); // @ LSU.scala l115
    perfReg_1_req[3] = (io_axi4_aw_fire && (! (((32'h30000000 <= io_axi4_aw_payload_addr) && (io_axi4_aw_payload_addr < 32'h40000000)) || ((32'h80000000 <= io_axi4_aw_payload_addr) && (io_axi4_aw_payload_addr < 32'hc0000000))))); // @ LSU.scala l116
  end

  assign io_axi4_aw_fire = (io_axi4_aw_valid && io_axi4_aw_ready); // @ BaseType.scala l308
  assign io_axi4_r_fire = (io_axi4_r_valid && io_axi4_r_ready); // @ BaseType.scala l308
  always @(*) begin
    perfReg_1_rsp[0] = (io_axi4_r_fire && (! _zz_rsp)); // @ LSU.scala l121
    perfReg_1_rsp[1] = (io_axi4_b_fire && (! _zz_rsp)); // @ LSU.scala l122
    perfReg_1_rsp[2] = (io_axi4_r_fire && _zz_rsp); // @ LSU.scala l123
    perfReg_1_rsp[3] = (io_axi4_b_fire && _zz_rsp); // @ LSU.scala l124
  end

  assign io_axi4_b_fire = (io_axi4_b_valid && io_axi4_b_ready); // @ BaseType.scala l308
  always @(posedge clock or posedge reset) begin
    if(reset) begin
      state <= LsuState_Idle; // @ Data.scala l432
      rdataReg <= 32'h0; // @ Data.scala l432
      _zz_rsp <= 1'b0; // @ Data.scala l432
    end else begin
      if(rdEnd) begin
        rdataReg <= axi4Ctrler_io_readData; // @ LSU.scala l51
      end
      case(state)
        LsuState_Idle : begin
          if(needMem) begin
            state <= LsuState_WaitMem; // @ Enum.scala l159
          end else begin
            state <= state; // @ LSU.scala l58
          end
        end
        LsuState_WaitMem : begin
          if(when_LSU_l61) begin
            if(io_output_fire) begin
              state <= LsuState_Idle; // @ Enum.scala l159
            end else begin
              state <= LsuState_Done; // @ Enum.scala l159
            end
          end else begin
            state <= state; // @ LSU.scala l65
          end
        end
        default : begin
          if(io_output_fire) begin
            state <= LsuState_Idle; // @ Enum.scala l159
          end else begin
            state <= state; // @ LSU.scala l69
          end
        end
      endcase
      _zz_rsp <= (io_axi4_ar_fire ? (! (((32'h30000000 <= io_axi4_ar_payload_addr) && (io_axi4_ar_payload_addr < 32'h40000000)) || ((32'h80000000 <= io_axi4_ar_payload_addr) && (io_axi4_ar_payload_addr < 32'hc0000000)))) : (io_axi4_aw_fire ? (! (((_zz__zz_rsp <= io_axi4_aw_payload_addr) && (io_axi4_aw_payload_addr < _zz__zz_rsp_1)) || ((_zz__zz_rsp_2 <= io_axi4_aw_payload_addr) && (io_axi4_aw_payload_addr < _zz__zz_rsp_3)))) : _zz_rsp)); // @ LSU.scala l119
    end
  end


endmodule

module ysyx_23060082_EXU (
  input  wire          io_input_valid,
  input  wire [31:0]   io_input_payload_pc,
  input  wire          io_input_payload_ctrl_rfCtrl_mem2reg,
  input  wire          io_input_payload_ctrl_rfCtrl_csr2reg,
  input  wire          io_input_payload_ctrl_rfCtrl_regWr,
  input  wire [4:0]    io_input_payload_ctrl_rfCtrl_rfWriteAddr,
  input  wire          io_input_payload_ctrl_aluCtrl_aluAsrc,
  input  wire [1:0]    io_input_payload_ctrl_aluCtrl_aluBsrc,
  input  wire [3:0]    io_input_payload_ctrl_aluCtrl_aluCtr,
  input  wire [2:0]    io_input_payload_ctrl_aluCtrl_branch,
  input  wire          io_input_payload_ctrl_memCtrl_memWr,
  input  wire [2:0]    io_input_payload_ctrl_memCtrl_memOp,
  input  wire [2:0]    io_input_payload_ctrl_csrCtrl_csrCmd,
  input  wire          io_input_payload_ctrl_csrCtrl_illegal,
  input  wire          io_input_payload_ctrl_csrCtrl_ebreak,
  input  wire          io_input_payload_ctrl_csrCtrl_trapEnter,
  input  wire          io_input_payload_ctrl_csrCtrl_trapExit,
  input  wire          io_input_payload_ctrl_fenceI,
  input  wire [31:0]   io_input_payload_imm,
  input  wire [31:0]   io_input_payload_rfReadData1,
  input  wire [31:0]   io_input_payload_rfReadData2,
  input  wire          io_input_payload_isCalc,
  output wire          io_output_valid,
  input  wire          io_output_ready,
  output wire [31:0]   io_output_payload_pc,
  output wire [31:0]   io_output_payload_pcNext,
  output wire          io_output_payload_rfCtrl_mem2reg,
  output wire          io_output_payload_rfCtrl_csr2reg,
  output wire          io_output_payload_rfCtrl_regWr,
  output wire [4:0]    io_output_payload_rfCtrl_rfWriteAddr,
  output wire          io_output_payload_memCtrl_memWr,
  output wire [2:0]    io_output_payload_memCtrl_memOp,
  output wire [2:0]    io_output_payload_csrCtrl_csrCmd,
  output wire          io_output_payload_csrCtrl_illegal,
  output wire          io_output_payload_csrCtrl_ebreak,
  output wire          io_output_payload_csrCtrl_trapEnter,
  output wire          io_output_payload_csrCtrl_trapExit,
  output wire [11:0]   io_output_payload_csrAddr,
  output wire [31:0]   io_output_payload_rfReadData,
  output wire [31:0]   io_output_payload_aluResult,
  output wire          io_output_payload_fenceI,
  input  wire          reset,
  input  wire          clock
);

  reg        [7:0]    perfReg_1_evt;
  wire                alu_io_less;
  wire                alu_io_zero;
  wire       [31:0]   alu_io_aluResult;
  wire                banchCond_io_pcAsrc;
  wire                banchCond_io_pcBsrc;
  reg        [31:0]   _zz_io_aluIn1;
  reg        [31:0]   _zz_io_aluIn2;
  wire       [31:0]   pcDataA;
  wire       [31:0]   pcDataB;
  wire       [31:0]   pcDataTmp;
  reg        [31:0]   pcNext;
  wire                willValid;
  wire                useRs1;

  ysyx_23060082_ALU alu (
    .io_aluIn1    (_zz_io_aluIn1[31:0]                      ), //i
    .io_aluIn2    (_zz_io_aluIn2[31:0]                      ), //i
    .io_aluCtr    (io_input_payload_ctrl_aluCtrl_aluCtr[3:0]), //i
    .io_less      (alu_io_less                              ), //o
    .io_zero      (alu_io_zero                              ), //o
    .io_aluResult (alu_io_aluResult[31:0]                   )  //o
  );
  ysyx_23060082_BranchCond banchCond (
    .io_branch (io_input_payload_ctrl_aluCtrl_branch[2:0]), //i
    .io_less   (alu_io_less                              ), //i
    .io_zero   (alu_io_zero                              ), //i
    .io_pcAsrc (banchCond_io_pcAsrc                      ), //o
    .io_pcBsrc (banchCond_io_pcBsrc                      )  //o
  );
  PerfReg perfReg_1 (
    .clock (clock             ), //i
    .reset (reset             ), //i
    .valid (1'b1              ), //i
    .req   (4'b0000           ), //i
    .rsp   (4'b0000           ), //i
    .evt   (perfReg_1_evt[7:0])  //i
  );
  always @(*) begin
    case(io_input_payload_ctrl_aluCtrl_aluAsrc)
      1'b1 : begin
        _zz_io_aluIn1 = io_input_payload_pc; // @ Misc.scala l258
      end
      default : begin
        _zz_io_aluIn1 = io_input_payload_rfReadData1; // @ Misc.scala l258
      end
    endcase
  end

  always @(*) begin
    case(io_input_payload_ctrl_aluCtrl_aluBsrc)
      2'b00 : begin
        _zz_io_aluIn2 = io_input_payload_rfReadData2; // @ Misc.scala l258
      end
      2'b01 : begin
        _zz_io_aluIn2 = io_input_payload_imm; // @ Misc.scala l258
      end
      default : begin
        _zz_io_aluIn2 = 32'h00000004; // @ Misc.scala l254
      end
    endcase
  end

  assign pcDataA = (banchCond_io_pcAsrc ? io_input_payload_imm : 32'h00000004); // @ Expression.scala l1531
  assign pcDataB = (banchCond_io_pcBsrc ? io_input_payload_rfReadData1 : io_input_payload_pc); // @ Expression.scala l1531
  assign pcDataTmp = (pcDataA + pcDataB); // @ BaseType.scala l302
  always @(*) begin
    case(io_input_payload_ctrl_aluCtrl_branch)
      3'b010 : begin
        pcNext = {pcDataTmp[31 : 1],1'b0}; // @ Misc.scala l258
      end
      default : begin
        pcNext = pcDataTmp; // @ Misc.scala l254
      end
    endcase
  end

  assign willValid = 1'b1; // @ EXU.scala l53
  assign io_output_valid = (io_input_valid && willValid); // @ EXU.scala l54
  always @(*) begin
    perfReg_1_evt = 8'h0; // @ EXU.scala l62
    perfReg_1_evt[0] = ((io_input_valid && io_input_payload_isCalc) && willValid); // @ EXU.scala l63
    perfReg_1_evt[1] = (io_input_valid && io_input_payload_isCalc); // @ EXU.scala l64
  end

  assign useRs1 = (io_input_payload_ctrl_csrCtrl_trapEnter || (io_input_payload_ctrl_csrCtrl_csrCmd != 3'b000)); // @ BaseType.scala l308
  assign io_output_payload_rfReadData = (useRs1 ? io_input_payload_rfReadData1 : io_input_payload_rfReadData2); // @ EXU.scala l68
  assign io_output_payload_pc = io_input_payload_pc; // @ EXU.scala l69
  assign io_output_payload_pcNext = pcNext; // @ EXU.scala l70
  assign io_output_payload_aluResult = alu_io_aluResult; // @ EXU.scala l71
  assign io_output_payload_csrAddr = io_input_payload_imm[11 : 0]; // @ EXU.scala l72
  assign io_output_payload_fenceI = io_input_payload_ctrl_fenceI; // @ EXU.scala l73
  assign io_output_payload_rfCtrl_mem2reg = io_input_payload_ctrl_rfCtrl_mem2reg; // @ EXU.scala l74
  assign io_output_payload_rfCtrl_csr2reg = io_input_payload_ctrl_rfCtrl_csr2reg; // @ EXU.scala l74
  assign io_output_payload_rfCtrl_regWr = io_input_payload_ctrl_rfCtrl_regWr; // @ EXU.scala l74
  assign io_output_payload_rfCtrl_rfWriteAddr = io_input_payload_ctrl_rfCtrl_rfWriteAddr; // @ EXU.scala l74
  assign io_output_payload_memCtrl_memWr = io_input_payload_ctrl_memCtrl_memWr; // @ EXU.scala l75
  assign io_output_payload_memCtrl_memOp = io_input_payload_ctrl_memCtrl_memOp; // @ EXU.scala l75
  assign io_output_payload_csrCtrl_csrCmd = io_input_payload_ctrl_csrCtrl_csrCmd; // @ EXU.scala l76
  assign io_output_payload_csrCtrl_illegal = io_input_payload_ctrl_csrCtrl_illegal; // @ EXU.scala l76
  assign io_output_payload_csrCtrl_ebreak = io_input_payload_ctrl_csrCtrl_ebreak; // @ EXU.scala l76
  assign io_output_payload_csrCtrl_trapEnter = io_input_payload_ctrl_csrCtrl_trapEnter; // @ EXU.scala l76
  assign io_output_payload_csrCtrl_trapExit = io_input_payload_ctrl_csrCtrl_trapExit; // @ EXU.scala l76

endmodule

module ysyx_23060082_IDU (
  input  wire          io_input_valid,
  input  wire [31:0]   io_input_payload_pc,
  input  wire [31:0]   io_input_payload_instr,
  output wire          io_output_valid,
  input  wire          io_output_ready,
  output wire [31:0]   io_output_payload_pc,
  output wire          io_output_payload_ctrl_rfCtrl_mem2reg,
  output wire          io_output_payload_ctrl_rfCtrl_csr2reg,
  output wire          io_output_payload_ctrl_rfCtrl_regWr,
  output wire [4:0]    io_output_payload_ctrl_rfCtrl_rfWriteAddr,
  output wire          io_output_payload_ctrl_aluCtrl_aluAsrc,
  output wire [1:0]    io_output_payload_ctrl_aluCtrl_aluBsrc,
  output wire [3:0]    io_output_payload_ctrl_aluCtrl_aluCtr,
  output wire [2:0]    io_output_payload_ctrl_aluCtrl_branch,
  output wire          io_output_payload_ctrl_memCtrl_memWr,
  output wire [2:0]    io_output_payload_ctrl_memCtrl_memOp,
  output wire [2:0]    io_output_payload_ctrl_csrCtrl_csrCmd,
  output wire          io_output_payload_ctrl_csrCtrl_illegal,
  output wire          io_output_payload_ctrl_csrCtrl_ebreak,
  output wire          io_output_payload_ctrl_csrCtrl_trapEnter,
  output wire          io_output_payload_ctrl_csrCtrl_trapExit,
  output wire          io_output_payload_ctrl_fenceI,
  output wire [31:0]   io_output_payload_imm,
  output wire [31:0]   io_output_payload_rfReadData1,
  output wire [31:0]   io_output_payload_rfReadData2,
  output wire          io_output_payload_isCalc,
  output wire [4:0]    io_rfRead_addr1,
  output wire [4:0]    io_rfRead_addr2,
  input  wire [31:0]   io_rfRead_data1,
  input  wire [31:0]   io_rfRead_data2,
  input  wire          reset,
  input  wire          clock
);

  reg        [7:0]    perfReg_1_evt;
  wire                decoder_io_ctrl_rfCtrl_mem2reg;
  wire                decoder_io_ctrl_rfCtrl_csr2reg;
  wire                decoder_io_ctrl_rfCtrl_regWr;
  wire       [4:0]    decoder_io_ctrl_rfCtrl_rfWriteAddr;
  wire                decoder_io_ctrl_aluCtrl_aluAsrc;
  wire       [1:0]    decoder_io_ctrl_aluCtrl_aluBsrc;
  wire       [3:0]    decoder_io_ctrl_aluCtrl_aluCtr;
  wire       [2:0]    decoder_io_ctrl_aluCtrl_branch;
  wire                decoder_io_ctrl_memCtrl_memWr;
  wire       [2:0]    decoder_io_ctrl_memCtrl_memOp;
  wire       [2:0]    decoder_io_ctrl_csrCtrl_csrCmd;
  wire                decoder_io_ctrl_csrCtrl_illegal;
  wire                decoder_io_ctrl_csrCtrl_ebreak;
  wire                decoder_io_ctrl_csrCtrl_trapEnter;
  wire                decoder_io_ctrl_csrCtrl_trapExit;
  wire                decoder_io_ctrl_fenceI;
  wire       [31:0]   decoder_io_imm;
  wire                decoder_io_isCalc;
  wire                decoder_io_isMem;
  wire                decoder_io_isBranch;
  wire                decoder_io_isJump;
  wire                decoder_io_isCsr;
  wire                decoder_io_isSys;
  wire                willValid;
  reg        [4:0]    _zz_io_rfRead_addr1;

  ysyx_23060082_Decoder decoder (
    .io_instr                   (io_input_payload_instr[31:0]           ), //i
    .io_ctrl_rfCtrl_mem2reg     (decoder_io_ctrl_rfCtrl_mem2reg         ), //o
    .io_ctrl_rfCtrl_csr2reg     (decoder_io_ctrl_rfCtrl_csr2reg         ), //o
    .io_ctrl_rfCtrl_regWr       (decoder_io_ctrl_rfCtrl_regWr           ), //o
    .io_ctrl_rfCtrl_rfWriteAddr (decoder_io_ctrl_rfCtrl_rfWriteAddr[4:0]), //o
    .io_ctrl_aluCtrl_aluAsrc    (decoder_io_ctrl_aluCtrl_aluAsrc        ), //o
    .io_ctrl_aluCtrl_aluBsrc    (decoder_io_ctrl_aluCtrl_aluBsrc[1:0]   ), //o
    .io_ctrl_aluCtrl_aluCtr     (decoder_io_ctrl_aluCtrl_aluCtr[3:0]    ), //o
    .io_ctrl_aluCtrl_branch     (decoder_io_ctrl_aluCtrl_branch[2:0]    ), //o
    .io_ctrl_memCtrl_memWr      (decoder_io_ctrl_memCtrl_memWr          ), //o
    .io_ctrl_memCtrl_memOp      (decoder_io_ctrl_memCtrl_memOp[2:0]     ), //o
    .io_ctrl_csrCtrl_csrCmd     (decoder_io_ctrl_csrCtrl_csrCmd[2:0]    ), //o
    .io_ctrl_csrCtrl_illegal    (decoder_io_ctrl_csrCtrl_illegal        ), //o
    .io_ctrl_csrCtrl_ebreak     (decoder_io_ctrl_csrCtrl_ebreak         ), //o
    .io_ctrl_csrCtrl_trapEnter  (decoder_io_ctrl_csrCtrl_trapEnter      ), //o
    .io_ctrl_csrCtrl_trapExit   (decoder_io_ctrl_csrCtrl_trapExit       ), //o
    .io_ctrl_fenceI             (decoder_io_ctrl_fenceI                 ), //o
    .io_imm                     (decoder_io_imm[31:0]                   ), //o
    .io_isCalc                  (decoder_io_isCalc                      ), //o
    .io_isMem                   (decoder_io_isMem                       ), //o
    .io_isBranch                (decoder_io_isBranch                    ), //o
    .io_isJump                  (decoder_io_isJump                      ), //o
    .io_isCsr                   (decoder_io_isCsr                       ), //o
    .io_isSys                   (decoder_io_isSys                       )  //o
  );
  PerfReg perfReg_1 (
    .clock (clock             ), //i
    .reset (reset             ), //i
    .valid (1'b1              ), //i
    .req   (4'b0000           ), //i
    .rsp   (4'b0000           ), //i
    .evt   (perfReg_1_evt[7:0])  //i
  );
  always @(*) begin
    perfReg_1_evt[0] = (io_input_valid && decoder_io_isCalc); // @ IDU.scala l72
    perfReg_1_evt[1] = (io_input_valid && decoder_io_isMem); // @ IDU.scala l73
    perfReg_1_evt[2] = (io_input_valid && decoder_io_isBranch); // @ IDU.scala l74
    perfReg_1_evt[3] = (io_input_valid && decoder_io_isJump); // @ IDU.scala l75
    perfReg_1_evt[4] = (io_input_valid && decoder_io_isCsr); // @ IDU.scala l76
    perfReg_1_evt[5] = (io_input_valid && decoder_io_isSys); // @ IDU.scala l77
    perfReg_1_evt[6] = (io_input_valid && (! (((((decoder_io_isCalc || decoder_io_isMem) || decoder_io_isBranch) || decoder_io_isJump) || decoder_io_isCsr) || decoder_io_isSys))); // @ IDU.scala l78
    perfReg_1_evt[7] = io_input_valid; // @ IDU.scala l80
  end

  assign willValid = 1'b1; // @ IDU.scala l84
  assign io_output_valid = (io_input_valid && willValid); // @ IDU.scala l85
  always @(*) begin
    case(decoder_io_ctrl_csrCtrl_trapEnter)
      1'b1 : begin
        _zz_io_rfRead_addr1 = 5'h0f; // @ Misc.scala l258
      end
      default : begin
        _zz_io_rfRead_addr1 = io_input_payload_instr[19 : 15]; // @ Misc.scala l258
      end
    endcase
  end

  assign io_rfRead_addr1 = _zz_io_rfRead_addr1; // @ IDU.scala l87
  assign io_rfRead_addr2 = io_input_payload_instr[24 : 20]; // @ IDU.scala l90
  assign io_output_payload_pc = io_input_payload_pc; // @ IDU.scala l92
  assign io_output_payload_rfReadData1 = io_rfRead_data1; // @ IDU.scala l93
  assign io_output_payload_rfReadData2 = io_rfRead_data2; // @ IDU.scala l94
  assign io_output_payload_ctrl_rfCtrl_mem2reg = decoder_io_ctrl_rfCtrl_mem2reg; // @ IDU.scala l95
  assign io_output_payload_ctrl_rfCtrl_csr2reg = decoder_io_ctrl_rfCtrl_csr2reg; // @ IDU.scala l95
  assign io_output_payload_ctrl_rfCtrl_regWr = decoder_io_ctrl_rfCtrl_regWr; // @ IDU.scala l95
  assign io_output_payload_ctrl_rfCtrl_rfWriteAddr = decoder_io_ctrl_rfCtrl_rfWriteAddr; // @ IDU.scala l95
  assign io_output_payload_ctrl_aluCtrl_aluAsrc = decoder_io_ctrl_aluCtrl_aluAsrc; // @ IDU.scala l95
  assign io_output_payload_ctrl_aluCtrl_aluBsrc = decoder_io_ctrl_aluCtrl_aluBsrc; // @ IDU.scala l95
  assign io_output_payload_ctrl_aluCtrl_aluCtr = decoder_io_ctrl_aluCtrl_aluCtr; // @ IDU.scala l95
  assign io_output_payload_ctrl_aluCtrl_branch = decoder_io_ctrl_aluCtrl_branch; // @ IDU.scala l95
  assign io_output_payload_ctrl_memCtrl_memWr = decoder_io_ctrl_memCtrl_memWr; // @ IDU.scala l95
  assign io_output_payload_ctrl_memCtrl_memOp = decoder_io_ctrl_memCtrl_memOp; // @ IDU.scala l95
  assign io_output_payload_ctrl_csrCtrl_csrCmd = decoder_io_ctrl_csrCtrl_csrCmd; // @ IDU.scala l95
  assign io_output_payload_ctrl_csrCtrl_illegal = decoder_io_ctrl_csrCtrl_illegal; // @ IDU.scala l95
  assign io_output_payload_ctrl_csrCtrl_ebreak = decoder_io_ctrl_csrCtrl_ebreak; // @ IDU.scala l95
  assign io_output_payload_ctrl_csrCtrl_trapEnter = decoder_io_ctrl_csrCtrl_trapEnter; // @ IDU.scala l95
  assign io_output_payload_ctrl_csrCtrl_trapExit = decoder_io_ctrl_csrCtrl_trapExit; // @ IDU.scala l95
  assign io_output_payload_ctrl_fenceI = decoder_io_ctrl_fenceI; // @ IDU.scala l95
  assign io_output_payload_imm = decoder_io_imm; // @ IDU.scala l96
  assign io_output_payload_isCalc = decoder_io_isCalc; // @ IDU.scala l98

endmodule

module ysyx_23060082_IFU (
  input  wire          io_input_valid,
  output wire          io_input_ready,
  input  wire [31:0]   io_input_payload_pcNext,
  input  wire          io_input_payload_fenceI,
  output wire          io_output_valid,
  input  wire          io_output_ready,
  output wire [31:0]   io_output_payload_pc,
  output wire [31:0]   io_output_payload_instr,
  output wire          io_axi4_ar_valid,
  input  wire          io_axi4_ar_ready,
  output wire [31:0]   io_axi4_ar_payload_addr,
  output wire [3:0]    io_axi4_ar_payload_id,
  output wire [7:0]    io_axi4_ar_payload_len,
  output wire [2:0]    io_axi4_ar_payload_size,
  output wire [1:0]    io_axi4_ar_payload_burst,
  input  wire          io_axi4_r_valid,
  output wire          io_axi4_r_ready,
  input  wire [31:0]   io_axi4_r_payload_data,
  input  wire [3:0]    io_axi4_r_payload_id,
  input  wire [1:0]    io_axi4_r_payload_resp,
  input  wire          io_axi4_r_payload_last,
  input  wire          reset,
  input  wire          clock
);
  localparam IfuState_Idle = 2'd0;
  localparam IfuState_WaitMem = 2'd1;
  localparam IfuState_Done = 2'd2;

  wire                icache_io_reqIn_valid;
  reg        [3:0]    perfReg_1_req;
  reg        [3:0]    perfReg_1_rsp;
  reg        [7:0]    perfReg_1_evt;
  wire                icache_io_reqIn_ready;
  wire                icache_io_rspOut_valid;
  wire       [31:0]   icache_io_rspOut_payload_rdata;
  wire                icache_io_axi4_ar_valid;
  wire       [31:0]   icache_io_axi4_ar_payload_addr;
  wire       [3:0]    icache_io_axi4_ar_payload_id;
  wire       [7:0]    icache_io_axi4_ar_payload_len;
  wire       [2:0]    icache_io_axi4_ar_payload_size;
  wire       [1:0]    icache_io_axi4_ar_payload_burst;
  wire                icache_io_axi4_r_ready;
  wire                icache_io_miss;
  wire                icache_io_missDone;
  reg        [1:0]    state;
  reg                 rstReg1;
  reg                 rstReg2;
  wire                rstEnd;
  reg                 dataValid;
  wire                io_input_fire;
  wire                when_IFU_l30;
  wire                io_output_fire;
  reg        [31:0]   pc;
  wire                icache_io_reqIn_fire;
  reg        [31:0]   rdataReg;
  wire                when_IFU_l70;
  wire                willValid;
  `ifndef SYNTHESIS
  reg [55:0] state_string;
  `endif


  ysyx_23060082_Icache icache (
    .io_reqIn_valid           (icache_io_reqIn_valid               ), //i
    .io_reqIn_ready           (icache_io_reqIn_ready               ), //o
    .io_reqIn_payload_pc      (pc[31:0]                            ), //i
    .io_rspOut_valid          (icache_io_rspOut_valid              ), //o
    .io_rspOut_payload_rdata  (icache_io_rspOut_payload_rdata[31:0]), //o
    .io_axi4_ar_valid         (icache_io_axi4_ar_valid             ), //o
    .io_axi4_ar_ready         (io_axi4_ar_ready                    ), //i
    .io_axi4_ar_payload_addr  (icache_io_axi4_ar_payload_addr[31:0]), //o
    .io_axi4_ar_payload_id    (icache_io_axi4_ar_payload_id[3:0]   ), //o
    .io_axi4_ar_payload_len   (icache_io_axi4_ar_payload_len[7:0]  ), //o
    .io_axi4_ar_payload_size  (icache_io_axi4_ar_payload_size[2:0] ), //o
    .io_axi4_ar_payload_burst (icache_io_axi4_ar_payload_burst[1:0]), //o
    .io_axi4_r_valid          (io_axi4_r_valid                     ), //i
    .io_axi4_r_ready          (icache_io_axi4_r_ready              ), //o
    .io_axi4_r_payload_data   (io_axi4_r_payload_data[31:0]        ), //i
    .io_axi4_r_payload_id     (io_axi4_r_payload_id[3:0]           ), //i
    .io_axi4_r_payload_resp   (io_axi4_r_payload_resp[1:0]         ), //i
    .io_axi4_r_payload_last   (io_axi4_r_payload_last              ), //i
    .io_fenceI                (io_input_payload_fenceI             ), //i
    .io_miss                  (icache_io_miss                      ), //o
    .io_missDone              (icache_io_missDone                  ), //o
    .clock                    (clock                               ), //i
    .reset                    (reset                               )  //i
  );
  PerfReg perfReg_1 (
    .clock (clock             ), //i
    .reset (reset             ), //i
    .valid (1'b1              ), //i
    .req   (perfReg_1_req[3:0]), //i
    .rsp   (perfReg_1_rsp[3:0]), //i
    .evt   (perfReg_1_evt[7:0])  //i
  );
  `ifndef SYNTHESIS
  always @(*) begin
    case(state)
      IfuState_Idle : state_string = "Idle   ";
      IfuState_WaitMem : state_string = "WaitMem";
      IfuState_Done : state_string = "Done   ";
      default : state_string = "???????";
    endcase
  end
  `endif

  assign rstEnd = (rstReg1 && (! rstReg2)); // @ BaseType.scala l308
  assign io_input_fire = (io_input_valid && io_input_ready); // @ BaseType.scala l308
  assign when_IFU_l30 = (io_input_fire || rstEnd); // @ BaseType.scala l308
  assign io_output_fire = (io_output_valid && io_output_ready); // @ BaseType.scala l308
  assign io_axi4_ar_valid = icache_io_axi4_ar_valid; // @ IFU.scala l42
  assign io_axi4_ar_payload_addr = icache_io_axi4_ar_payload_addr; // @ IFU.scala l42
  assign io_axi4_ar_payload_id = icache_io_axi4_ar_payload_id; // @ IFU.scala l42
  assign io_axi4_ar_payload_len = icache_io_axi4_ar_payload_len; // @ IFU.scala l42
  assign io_axi4_ar_payload_size = icache_io_axi4_ar_payload_size; // @ IFU.scala l42
  assign io_axi4_ar_payload_burst = icache_io_axi4_ar_payload_burst; // @ IFU.scala l42
  assign io_axi4_r_ready = icache_io_axi4_r_ready; // @ IFU.scala l42
  assign icache_io_reqIn_valid = ((state == IfuState_Idle) && dataValid); // @ IFU.scala l44
  always @(*) begin
    perfReg_1_req = 4'b0000; // @ IFU.scala l51
    perfReg_1_req[0] = icache_io_reqIn_fire; // @ IFU.scala l54
    perfReg_1_req[1] = icache_io_miss; // @ IFU.scala l62
  end

  always @(*) begin
    perfReg_1_rsp = 4'b0000; // @ IFU.scala l52
    perfReg_1_rsp[0] = icache_io_rspOut_valid; // @ IFU.scala l55
    perfReg_1_rsp[1] = icache_io_missDone; // @ IFU.scala l63
  end

  always @(*) begin
    perfReg_1_evt = 8'h0; // @ IFU.scala l53
    perfReg_1_evt[0] = icache_io_reqIn_fire; // @ IFU.scala l56
    perfReg_1_evt[1] = icache_io_rspOut_valid; // @ IFU.scala l57
    perfReg_1_evt[2] = icache_io_miss; // @ IFU.scala l61
  end

  assign icache_io_reqIn_fire = (icache_io_reqIn_valid && icache_io_reqIn_ready); // @ BaseType.scala l308
  assign when_IFU_l70 = (dataValid && (! icache_io_rspOut_valid)); // @ BaseType.scala l308
  assign willValid = ((((state == IfuState_Idle) || (state == IfuState_WaitMem)) && icache_io_rspOut_valid) || (state == IfuState_Done)); // @ BaseType.scala l308
  assign io_output_valid = (dataValid && willValid); // @ IFU.scala l90
  assign io_input_ready = ((! dataValid) || io_output_fire); // @ IFU.scala l91
  assign io_output_payload_pc = pc; // @ IFU.scala l93
  assign io_output_payload_instr = (icache_io_rspOut_valid ? icache_io_rspOut_payload_rdata : rdataReg); // @ IFU.scala l94
  always @(posedge clock or posedge reset) begin
    if(reset) begin
      state <= IfuState_Idle; // @ Data.scala l432
      rstReg1 <= 1'b0; // @ Data.scala l432
      rstReg2 <= 1'b0; // @ Data.scala l432
      dataValid <= 1'b0; // @ Data.scala l432
      pc <= 32'h30000000; // @ Data.scala l432
      rdataReg <= 32'h0; // @ Data.scala l432
    end else begin
      rstReg1 <= 1'b1; // @ Reg.scala l42
      rstReg2 <= rstReg1; // @ Reg.scala l42
      if(when_IFU_l30) begin
        dataValid <= 1'b1; // @ IFU.scala l31
      end else begin
        if(io_output_fire) begin
          dataValid <= 1'b0; // @ IFU.scala l33
        end else begin
          dataValid <= dataValid; // @ IFU.scala l35
        end
      end
      if(io_input_fire) begin
        pc <= io_input_payload_pcNext; // @ IFU.scala l38
      end
      if(icache_io_rspOut_valid) begin
        rdataReg <= icache_io_rspOut_payload_rdata; // @ IFU.scala l66
      end
      case(state)
        IfuState_Idle : begin
          if(when_IFU_l70) begin
            state <= IfuState_WaitMem; // @ Enum.scala l159
          end else begin
            state <= state; // @ IFU.scala l71
          end
        end
        IfuState_WaitMem : begin
          if(icache_io_rspOut_valid) begin
            if(io_output_fire) begin
              state <= IfuState_Idle; // @ Enum.scala l159
            end else begin
              state <= IfuState_Done; // @ Enum.scala l159
            end
          end else begin
            state <= state; // @ IFU.scala l78
          end
        end
        default : begin
          if(io_output_fire) begin
            state <= IfuState_Idle; // @ Enum.scala l159
          end else begin
            state <= state; // @ IFU.scala l82
          end
        end
      endcase
    end
  end


endmodule

module ysyx_23060082_RegFile (
  input  wire [4:0]    io_readBus_addr1,
  input  wire [4:0]    io_readBus_addr2,
  output wire [31:0]   io_readBus_data1,
  output wire [31:0]   io_readBus_data2,
  input  wire [4:0]    io_writeBus_addr,
  input  wire [31:0]   io_writeBus_data,
  input  wire          io_writeBus_en,
  input  wire          clock,
  input  wire          reset
);

  reg        [31:0]   _zz_io_readBus_data1;
  wire       [3:0]    _zz_io_readBus_data1_1;
  reg        [31:0]   _zz_io_readBus_data2;
  wire       [3:0]    _zz_io_readBus_data2_1;
  reg        [31:0]   rf_0;
  reg        [31:0]   rf_1;
  reg        [31:0]   rf_2;
  reg        [31:0]   rf_3;
  reg        [31:0]   rf_4;
  reg        [31:0]   rf_5;
  reg        [31:0]   rf_6;
  reg        [31:0]   rf_7;
  reg        [31:0]   rf_8;
  reg        [31:0]   rf_9;
  reg        [31:0]   rf_10;
  reg        [31:0]   rf_11;
  reg        [31:0]   rf_12;
  reg        [31:0]   rf_13;
  reg        [31:0]   rf_14;
  reg        [31:0]   rf_15;
  wire                when_RegFile_l41;
  wire       [15:0]   _zz_1;

  assign _zz_io_readBus_data1_1 = io_readBus_addr1[3 : 0];
  assign _zz_io_readBus_data2_1 = io_readBus_addr2[3 : 0];
  always @(*) begin
    case(_zz_io_readBus_data1_1)
      4'b0000 : _zz_io_readBus_data1 = rf_0;
      4'b0001 : _zz_io_readBus_data1 = rf_1;
      4'b0010 : _zz_io_readBus_data1 = rf_2;
      4'b0011 : _zz_io_readBus_data1 = rf_3;
      4'b0100 : _zz_io_readBus_data1 = rf_4;
      4'b0101 : _zz_io_readBus_data1 = rf_5;
      4'b0110 : _zz_io_readBus_data1 = rf_6;
      4'b0111 : _zz_io_readBus_data1 = rf_7;
      4'b1000 : _zz_io_readBus_data1 = rf_8;
      4'b1001 : _zz_io_readBus_data1 = rf_9;
      4'b1010 : _zz_io_readBus_data1 = rf_10;
      4'b1011 : _zz_io_readBus_data1 = rf_11;
      4'b1100 : _zz_io_readBus_data1 = rf_12;
      4'b1101 : _zz_io_readBus_data1 = rf_13;
      4'b1110 : _zz_io_readBus_data1 = rf_14;
      default : _zz_io_readBus_data1 = rf_15;
    endcase
  end

  always @(*) begin
    case(_zz_io_readBus_data2_1)
      4'b0000 : _zz_io_readBus_data2 = rf_0;
      4'b0001 : _zz_io_readBus_data2 = rf_1;
      4'b0010 : _zz_io_readBus_data2 = rf_2;
      4'b0011 : _zz_io_readBus_data2 = rf_3;
      4'b0100 : _zz_io_readBus_data2 = rf_4;
      4'b0101 : _zz_io_readBus_data2 = rf_5;
      4'b0110 : _zz_io_readBus_data2 = rf_6;
      4'b0111 : _zz_io_readBus_data2 = rf_7;
      4'b1000 : _zz_io_readBus_data2 = rf_8;
      4'b1001 : _zz_io_readBus_data2 = rf_9;
      4'b1010 : _zz_io_readBus_data2 = rf_10;
      4'b1011 : _zz_io_readBus_data2 = rf_11;
      4'b1100 : _zz_io_readBus_data2 = rf_12;
      4'b1101 : _zz_io_readBus_data2 = rf_13;
      4'b1110 : _zz_io_readBus_data2 = rf_14;
      default : _zz_io_readBus_data2 = rf_15;
    endcase
  end

  assign when_RegFile_l41 = (io_writeBus_en && (io_writeBus_addr[3 : 0] != 4'b0000)); // @ BaseType.scala l308
  assign _zz_1 = ({15'd0,1'b1} <<< io_writeBus_addr[3 : 0]); // @ BaseType.scala l302
  assign io_readBus_data1 = _zz_io_readBus_data1; // @ RegFile.scala l46
  assign io_readBus_data2 = _zz_io_readBus_data2; // @ RegFile.scala l47
  always @(posedge clock) begin
    rf_0 <= 32'h0; // @ RegFile.scala l40
    if(when_RegFile_l41) begin
      if(_zz_1[0]) begin
        rf_0 <= io_writeBus_data; // @ RegFile.scala l42
      end
      if(_zz_1[1]) begin
        rf_1 <= io_writeBus_data; // @ RegFile.scala l42
      end
      if(_zz_1[2]) begin
        rf_2 <= io_writeBus_data; // @ RegFile.scala l42
      end
      if(_zz_1[3]) begin
        rf_3 <= io_writeBus_data; // @ RegFile.scala l42
      end
      if(_zz_1[4]) begin
        rf_4 <= io_writeBus_data; // @ RegFile.scala l42
      end
      if(_zz_1[5]) begin
        rf_5 <= io_writeBus_data; // @ RegFile.scala l42
      end
      if(_zz_1[6]) begin
        rf_6 <= io_writeBus_data; // @ RegFile.scala l42
      end
      if(_zz_1[7]) begin
        rf_7 <= io_writeBus_data; // @ RegFile.scala l42
      end
      if(_zz_1[8]) begin
        rf_8 <= io_writeBus_data; // @ RegFile.scala l42
      end
      if(_zz_1[9]) begin
        rf_9 <= io_writeBus_data; // @ RegFile.scala l42
      end
      if(_zz_1[10]) begin
        rf_10 <= io_writeBus_data; // @ RegFile.scala l42
      end
      if(_zz_1[11]) begin
        rf_11 <= io_writeBus_data; // @ RegFile.scala l42
      end
      if(_zz_1[12]) begin
        rf_12 <= io_writeBus_data; // @ RegFile.scala l42
      end
      if(_zz_1[13]) begin
        rf_13 <= io_writeBus_data; // @ RegFile.scala l42
      end
      if(_zz_1[14]) begin
        rf_14 <= io_writeBus_data; // @ RegFile.scala l42
      end
      if(_zz_1[15]) begin
        rf_15 <= io_writeBus_data; // @ RegFile.scala l42
      end
    end
  end


endmodule

module ysyx_23060082_CSR (
  input  wire [11:0]   io_csrAddr,
  input  wire [31:0]   io_csrWdata,
  output wire [31:0]   io_csrRdata,
  input  wire [2:0]    io_csrCmd,
  input  wire          io_trapEnter,
  input  wire          io_trapExit,
  input  wire [31:0]   io_pcIn,
  input  wire [31:0]   io_causeIn,
  output wire [31:0]   io_mtvec,
  output wire [31:0]   io_mepc,
  input  wire          io_instrRetire,
  input  wire          clock,
  input  wire          reset
);

  reg        [31:0]   mstatus;
  reg        [31:0]   mtvec;
  reg        [31:0]   mepc;
  reg        [31:0]   mcause;
  wire       [31:0]   mvendorid;
  wire       [31:0]   marchid;
  reg        [63:0]   mcycle;
  reg        [63:0]   minstret;
  wire                writeEnable;
  wire                readMcycle;
  wire                readMcycleh;
  reg        [31:0]   mcyclehTmp;
  wire                readMinstret;
  wire                readMinstreth;
  reg        [31:0]   minstrethTmp;
  reg        [31:0]   _zz_io_csrRdata;
  wire       [31:0]   rdataWb;
  reg        [31:0]   writeData;

  assign mvendorid = 32'h79737978; // @ Expression.scala l2466
  assign marchid = 32'h015fde72; // @ Expression.scala l2466
  assign writeEnable = (io_csrCmd != 3'b000); // @ BaseType.scala l308
  assign readMcycle = (io_csrAddr == 12'hb00); // @ BaseType.scala l308
  assign readMcycleh = (io_csrAddr == 12'hb80); // @ BaseType.scala l308
  assign readMinstret = (io_csrAddr == 12'hb02); // @ BaseType.scala l308
  assign readMinstreth = (io_csrAddr == 12'hb82); // @ BaseType.scala l308
  always @(*) begin
    case(io_csrAddr)
      12'h300 : begin
        _zz_io_csrRdata = mstatus; // @ Misc.scala l258
      end
      12'h305 : begin
        _zz_io_csrRdata = mtvec; // @ Misc.scala l258
      end
      12'h341 : begin
        _zz_io_csrRdata = mepc; // @ Misc.scala l258
      end
      12'h342 : begin
        _zz_io_csrRdata = mcause; // @ Misc.scala l258
      end
      12'hf11 : begin
        _zz_io_csrRdata = mvendorid; // @ Misc.scala l258
      end
      12'hf12 : begin
        _zz_io_csrRdata = marchid; // @ Misc.scala l258
      end
      12'hb00 : begin
        _zz_io_csrRdata = mcycle[31 : 0]; // @ Misc.scala l258
      end
      12'hb80 : begin
        _zz_io_csrRdata = mcyclehTmp; // @ Misc.scala l258
      end
      12'hb02 : begin
        _zz_io_csrRdata = minstret[31 : 0]; // @ Misc.scala l258
      end
      12'hb82 : begin
        _zz_io_csrRdata = minstrethTmp; // @ Misc.scala l258
      end
      default : begin
        _zz_io_csrRdata = 32'h0; // @ Misc.scala l254
      end
    endcase
  end

  assign io_csrRdata = _zz_io_csrRdata; // @ CSR.scala l57
  assign rdataWb = (readMcycleh ? mcycle[63 : 32] : (readMinstreth ? minstret[63 : 32] : io_csrRdata)); // @ Expression.scala l1531
  always @(*) begin
    case(io_csrCmd)
      3'b001 : begin
        writeData = io_csrWdata; // @ Misc.scala l258
      end
      3'b010 : begin
        writeData = (rdataWb | io_csrWdata); // @ Misc.scala l258
      end
      default : begin
        writeData = rdataWb; // @ Misc.scala l254
      end
    endcase
  end

  assign io_mtvec = mtvec; // @ CSR.scala l97
  assign io_mepc = mepc; // @ CSR.scala l98
  always @(posedge clock or posedge reset) begin
    if(reset) begin
      mstatus <= 32'h0; // @ Data.scala l432
      mtvec <= 32'h0; // @ Data.scala l432
      mepc <= 32'h0; // @ Data.scala l432
      mcause <= 32'h0; // @ Data.scala l432
      mcycle <= 64'h0; // @ Data.scala l432
      minstret <= 64'h0; // @ Data.scala l432
      mcyclehTmp <= 32'h0; // @ Data.scala l432
      minstrethTmp <= 32'h0; // @ Data.scala l432
    end else begin
      mcycle <= (mcycle + 64'h0000000000000001); // @ CSR.scala l49
      if(readMcycle) begin
        mcyclehTmp <= mcycle[63 : 32]; // @ CSR.scala l50
      end
      if(io_instrRetire) begin
        minstret <= (minstret + 64'h0000000000000001); // @ CSR.scala l54
      end
      if(readMinstret) begin
        minstrethTmp <= minstret[63 : 32]; // @ CSR.scala l55
      end
      if(writeEnable) begin
        case(io_csrAddr)
          12'h300 : begin
            mstatus <= writeData; // @ CSR.scala l81
          end
          12'h305 : begin
            mtvec <= writeData; // @ CSR.scala l82
          end
          12'h341 : begin
            mepc <= writeData; // @ CSR.scala l83
          end
          12'h342 : begin
            mcause <= writeData; // @ CSR.scala l84
          end
          12'hb00 : begin
            mcycle[31 : 0] <= writeData; // @ CSR.scala l85
          end
          12'hb80 : begin
            mcycle[63 : 32] <= writeData; // @ CSR.scala l86
          end
          12'hb02 : begin
            minstret[31 : 0] <= writeData; // @ CSR.scala l87
          end
          12'hb82 : begin
            minstret[63 : 32] <= writeData; // @ CSR.scala l88
          end
          default : begin
          end
        endcase
      end
      if(io_trapEnter) begin
        mepc <= io_pcIn; // @ CSR.scala l93
        mcause <= io_causeIn; // @ CSR.scala l94
      end
    end
  end


endmodule

module ysyx_23060082_Axi4_Ctrler (
  input  wire          io_readReq,
  input  wire          io_writeReq,
  input  wire [2:0]    io_size,
  input  wire [31:0]   io_readAddr,
  input  wire [31:0]   io_writeAddr,
  input  wire [31:0]   io_writeData,
  input  wire [3:0]    io_writeMask,
  output wire          io_readEnd,
  output wire [31:0]   io_readData,
  output wire          io_writeEnd,
  output reg           io_axi4_aw_valid,
  input  wire          io_axi4_aw_ready,
  output reg  [31:0]   io_axi4_aw_payload_addr,
  output wire [3:0]    io_axi4_aw_payload_id,
  output wire [7:0]    io_axi4_aw_payload_len,
  output wire [2:0]    io_axi4_aw_payload_size,
  output wire [1:0]    io_axi4_aw_payload_burst,
  output reg           io_axi4_w_valid,
  input  wire          io_axi4_w_ready,
  output reg  [31:0]   io_axi4_w_payload_data,
  output reg  [3:0]    io_axi4_w_payload_strb,
  output wire          io_axi4_w_payload_last,
  input  wire          io_axi4_b_valid,
  output wire          io_axi4_b_ready,
  input  wire [3:0]    io_axi4_b_payload_id,
  input  wire [1:0]    io_axi4_b_payload_resp,
  output reg           io_axi4_ar_valid,
  input  wire          io_axi4_ar_ready,
  output reg  [31:0]   io_axi4_ar_payload_addr,
  output wire [3:0]    io_axi4_ar_payload_id,
  output wire [7:0]    io_axi4_ar_payload_len,
  output wire [2:0]    io_axi4_ar_payload_size,
  output wire [1:0]    io_axi4_ar_payload_burst,
  input  wire          io_axi4_r_valid,
  output wire          io_axi4_r_ready,
  input  wire [31:0]   io_axi4_r_payload_data,
  input  wire [3:0]    io_axi4_r_payload_id,
  input  wire [1:0]    io_axi4_r_payload_resp,
  input  wire          io_axi4_r_payload_last,
  input  wire          clock,
  input  wire          reset
);

  wire                io_axi4_ar_fire;
  wire                io_axi4_r_fire;
  wire                when_LSU_l245;
  wire                io_axi4_aw_fire;
  wire                io_axi4_w_fire;
  wire                io_axi4_b_fire;
  wire                when_LSU_l298;

  assign io_axi4_ar_payload_id = 4'b0000; // @ LSU.scala l220
  assign io_axi4_ar_payload_len = 8'h0; // @ LSU.scala l221
  assign io_axi4_ar_payload_size = io_size; // @ LSU.scala l222
  assign io_axi4_ar_payload_burst = 2'b01; // @ LSU.scala l223
  assign io_axi4_ar_fire = (io_axi4_ar_valid && io_axi4_ar_ready); // @ BaseType.scala l308
  assign io_axi4_r_ready = io_axi4_r_valid; // @ LSU.scala l240
  assign io_axi4_r_fire = (io_axi4_r_valid && io_axi4_r_ready); // @ BaseType.scala l308
  assign io_readEnd = (io_axi4_r_fire && io_axi4_r_payload_last); // @ LSU.scala l241
  assign io_readData = io_axi4_r_payload_data; // @ LSU.scala l242
  assign when_LSU_l245 = (io_axi4_r_fire && (io_axi4_r_payload_resp != 2'b00)); // @ BaseType.scala l308
  assign io_axi4_w_payload_last = 1'b1; // @ LSU.scala l257
  assign io_axi4_aw_payload_id = 4'b0000; // @ LSU.scala l259
  assign io_axi4_aw_payload_len = 8'h0; // @ LSU.scala l260
  assign io_axi4_aw_payload_size = io_size; // @ LSU.scala l261
  assign io_axi4_aw_payload_burst = 2'b01; // @ LSU.scala l262
  assign io_axi4_aw_fire = (io_axi4_aw_valid && io_axi4_aw_ready); // @ BaseType.scala l308
  assign io_axi4_w_fire = (io_axi4_w_valid && io_axi4_w_ready); // @ BaseType.scala l308
  assign io_axi4_b_ready = io_axi4_b_valid; // @ LSU.scala l294
  assign io_axi4_b_fire = (io_axi4_b_valid && io_axi4_b_ready); // @ BaseType.scala l308
  assign io_writeEnd = io_axi4_b_fire; // @ LSU.scala l295
  assign when_LSU_l298 = (io_axi4_b_fire && (io_axi4_b_payload_resp != 2'b00)); // @ BaseType.scala l308
  always @(posedge clock or posedge reset) begin
    if(reset) begin
      io_axi4_ar_valid <= 1'b0; // @ Data.scala l432
      io_axi4_aw_valid <= 1'b0; // @ Data.scala l432
      io_axi4_w_valid <= 1'b0; // @ Data.scala l432
    end else begin
      if(io_readReq) begin
        io_axi4_ar_valid <= 1'b1; // @ LSU.scala l226
      end else begin
        if(io_axi4_ar_fire) begin
          io_axi4_ar_valid <= 1'b0; // @ LSU.scala l228
        end else begin
          io_axi4_ar_valid <= io_axi4_ar_valid; // @ LSU.scala l230
        end
      end
      if(when_LSU_l245) begin
        `ifndef SYNTHESIS
          `ifdef FORMAL
            assert(1'b0); // core.scala:L569
          `else
            if(!1'b0) begin
              $display("NOTE [LSU] read resp error! resp =%xaddr =%x", io_axi4_r_payload_resp, io_axi4_ar_payload_addr); // core.scala:L569
            end
          `endif
        `endif
      end
      if(io_writeReq) begin
        io_axi4_aw_valid <= 1'b1; // @ LSU.scala l265
      end else begin
        if(io_axi4_aw_fire) begin
          io_axi4_aw_valid <= 1'b0; // @ LSU.scala l267
        end else begin
          io_axi4_aw_valid <= io_axi4_aw_valid; // @ LSU.scala l269
        end
      end
      if(io_writeReq) begin
        io_axi4_w_valid <= 1'b1; // @ LSU.scala l279
      end else begin
        if(io_axi4_w_fire) begin
          io_axi4_w_valid <= 1'b0; // @ LSU.scala l281
        end else begin
          io_axi4_w_valid <= io_axi4_w_valid; // @ LSU.scala l283
        end
      end
      if(when_LSU_l298) begin
        `ifndef SYNTHESIS
          `ifdef FORMAL
            assert(1'b0); // core.scala:L569
          `else
            if(!1'b0) begin
              $display("NOTE [LSU] write resp error! resp =%x, addr =%x", io_axi4_b_payload_resp, io_axi4_aw_payload_addr); // core.scala:L569
            end
          `endif
        `endif
      end
    end
  end

  always @(posedge clock) begin
    if(io_readReq) begin
      io_axi4_ar_payload_addr <= io_readAddr; // @ LSU.scala l234
    end else begin
      io_axi4_ar_payload_addr <= io_axi4_ar_payload_addr; // @ LSU.scala l236
    end
    if(io_writeReq) begin
      io_axi4_aw_payload_addr <= io_writeAddr; // @ LSU.scala l273
    end else begin
      io_axi4_aw_payload_addr <= io_axi4_aw_payload_addr; // @ LSU.scala l275
    end
    if(io_writeReq) begin
      io_axi4_w_payload_data <= io_writeData; // @ LSU.scala l287
      io_axi4_w_payload_strb <= io_writeMask; // @ LSU.scala l288
    end else begin
      io_axi4_w_payload_data <= io_axi4_w_payload_data; // @ LSU.scala l290
      io_axi4_w_payload_strb <= io_axi4_w_payload_strb; // @ LSU.scala l291
    end
  end


endmodule

module ysyx_23060082_DataProcess (
  input  wire [4:0]    io_addrOp,
  input  wire [31:0]   io_wdata,
  output wire [31:0]   io_wdataReal,
  output wire [3:0]    io_wmask,
  input  wire [31:0]   io_rdata,
  output wire [31:0]   io_rdataReal
);

  wire       [31:0]   _zz__zz_io_rdataReal;
  wire       [15:0]   _zz__zz_io_rdataReal_1;
  wire       [31:0]   _zz__zz_io_rdataReal_2;
  wire       [7:0]    _zz__zz_io_rdataReal_3;
  wire       [15:0]   _zz__zz_io_rdataReal_4;
  wire       [7:0]    _zz__zz_io_rdataReal_5;
  wire       [31:0]   _zz__zz_io_rdataReal_6;
  wire       [15:0]   _zz__zz_io_rdataReal_7;
  wire       [31:0]   _zz__zz_io_rdataReal_8;
  wire       [7:0]    _zz__zz_io_rdataReal_9;
  wire       [15:0]   _zz__zz_io_rdataReal_10;
  wire       [7:0]    _zz__zz_io_rdataReal_11;
  wire       [31:0]   _zz__zz_io_rdataReal_12;
  wire       [15:0]   _zz__zz_io_rdataReal_13;
  wire       [31:0]   _zz__zz_io_rdataReal_14;
  wire       [7:0]    _zz__zz_io_rdataReal_15;
  wire       [15:0]   _zz__zz_io_rdataReal_16;
  wire       [7:0]    _zz__zz_io_rdataReal_17;
  wire       [31:0]   _zz__zz_io_rdataReal_18;
  wire       [7:0]    _zz__zz_io_rdataReal_19;
  wire       [7:0]    _zz__zz_io_rdataReal_20;
  reg        [31:0]   _zz_io_rdataReal;
  reg        [31:0]   _zz_io_wdataReal;
  reg        [3:0]    _zz_io_wmask;

  assign _zz__zz_io_rdataReal_1 = io_rdata[15 : 0];
  assign _zz__zz_io_rdataReal = {{16{_zz__zz_io_rdataReal_1[15]}}, _zz__zz_io_rdataReal_1};
  assign _zz__zz_io_rdataReal_3 = io_rdata[7 : 0];
  assign _zz__zz_io_rdataReal_2 = {{24{_zz__zz_io_rdataReal_3[7]}}, _zz__zz_io_rdataReal_3};
  assign _zz__zz_io_rdataReal_4 = io_rdata[15 : 0];
  assign _zz__zz_io_rdataReal_5 = io_rdata[7 : 0];
  assign _zz__zz_io_rdataReal_7 = io_rdata[23 : 8];
  assign _zz__zz_io_rdataReal_6 = {{16{_zz__zz_io_rdataReal_7[15]}}, _zz__zz_io_rdataReal_7};
  assign _zz__zz_io_rdataReal_9 = io_rdata[15 : 8];
  assign _zz__zz_io_rdataReal_8 = {{24{_zz__zz_io_rdataReal_9[7]}}, _zz__zz_io_rdataReal_9};
  assign _zz__zz_io_rdataReal_10 = io_rdata[23 : 8];
  assign _zz__zz_io_rdataReal_11 = io_rdata[15 : 8];
  assign _zz__zz_io_rdataReal_13 = io_rdata[31 : 16];
  assign _zz__zz_io_rdataReal_12 = {{16{_zz__zz_io_rdataReal_13[15]}}, _zz__zz_io_rdataReal_13};
  assign _zz__zz_io_rdataReal_15 = io_rdata[23 : 16];
  assign _zz__zz_io_rdataReal_14 = {{24{_zz__zz_io_rdataReal_15[7]}}, _zz__zz_io_rdataReal_15};
  assign _zz__zz_io_rdataReal_16 = io_rdata[31 : 16];
  assign _zz__zz_io_rdataReal_17 = io_rdata[23 : 16];
  assign _zz__zz_io_rdataReal_19 = io_rdata[31 : 24];
  assign _zz__zz_io_rdataReal_18 = {{24{_zz__zz_io_rdataReal_19[7]}}, _zz__zz_io_rdataReal_19};
  assign _zz__zz_io_rdataReal_20 = io_rdata[31 : 24];
  always @(*) begin
    case(io_addrOp)
      5'h02 : begin
        _zz_io_rdataReal = io_rdata; // @ Misc.scala l258
      end
      5'h01 : begin
        _zz_io_rdataReal = _zz__zz_io_rdataReal; // @ Misc.scala l258
      end
      5'h0 : begin
        _zz_io_rdataReal = _zz__zz_io_rdataReal_2; // @ Misc.scala l258
      end
      5'h05 : begin
        _zz_io_rdataReal = {16'd0, _zz__zz_io_rdataReal_4}; // @ Misc.scala l258
      end
      5'h04 : begin
        _zz_io_rdataReal = {24'd0, _zz__zz_io_rdataReal_5}; // @ Misc.scala l258
      end
      5'h09 : begin
        _zz_io_rdataReal = _zz__zz_io_rdataReal_6; // @ Misc.scala l258
      end
      5'h08 : begin
        _zz_io_rdataReal = _zz__zz_io_rdataReal_8; // @ Misc.scala l258
      end
      5'h0d : begin
        _zz_io_rdataReal = {16'd0, _zz__zz_io_rdataReal_10}; // @ Misc.scala l258
      end
      5'h0c : begin
        _zz_io_rdataReal = {24'd0, _zz__zz_io_rdataReal_11}; // @ Misc.scala l258
      end
      5'h11 : begin
        _zz_io_rdataReal = _zz__zz_io_rdataReal_12; // @ Misc.scala l258
      end
      5'h10 : begin
        _zz_io_rdataReal = _zz__zz_io_rdataReal_14; // @ Misc.scala l258
      end
      5'h15 : begin
        _zz_io_rdataReal = {16'd0, _zz__zz_io_rdataReal_16}; // @ Misc.scala l258
      end
      5'h14 : begin
        _zz_io_rdataReal = {24'd0, _zz__zz_io_rdataReal_17}; // @ Misc.scala l258
      end
      5'h18 : begin
        _zz_io_rdataReal = _zz__zz_io_rdataReal_18; // @ Misc.scala l258
      end
      5'h1c : begin
        _zz_io_rdataReal = {24'd0, _zz__zz_io_rdataReal_20}; // @ Misc.scala l258
      end
      default : begin
        _zz_io_rdataReal = 32'h0; // @ Misc.scala l254
      end
    endcase
  end

  assign io_rdataReal = _zz_io_rdataReal; // @ LSU.scala l143
  always @(*) begin
    case(io_addrOp)
      5'h02 : begin
        _zz_io_wdataReal = io_wdata; // @ Misc.scala l258
      end
      5'h01 : begin
        _zz_io_wdataReal = {16'h0,io_wdata[15 : 0]}; // @ Misc.scala l258
      end
      5'h0 : begin
        _zz_io_wdataReal = {24'h0,io_wdata[7 : 0]}; // @ Misc.scala l258
      end
      5'h09 : begin
        _zz_io_wdataReal = {{8'h0,io_wdata[15 : 0]},8'h0}; // @ Misc.scala l258
      end
      5'h08 : begin
        _zz_io_wdataReal = {{16'h0,io_wdata[7 : 0]},8'h0}; // @ Misc.scala l258
      end
      5'h11 : begin
        _zz_io_wdataReal = {io_wdata[15 : 0],16'h0}; // @ Misc.scala l258
      end
      5'h10 : begin
        _zz_io_wdataReal = {{8'h0,io_wdata[7 : 0]},16'h0}; // @ Misc.scala l258
      end
      5'h18 : begin
        _zz_io_wdataReal = {io_wdata[7 : 0],24'h0}; // @ Misc.scala l258
      end
      default : begin
        _zz_io_wdataReal = 32'h0; // @ Misc.scala l254
      end
    endcase
  end

  assign io_wdataReal = _zz_io_wdataReal; // @ LSU.scala l165
  always @(*) begin
    case(io_addrOp)
      5'h02 : begin
        _zz_io_wmask = 4'b1111; // @ Misc.scala l258
      end
      5'h01 : begin
        _zz_io_wmask = 4'b0011; // @ Misc.scala l258
      end
      5'h0 : begin
        _zz_io_wmask = 4'b0001; // @ Misc.scala l258
      end
      5'h09 : begin
        _zz_io_wmask = 4'b0110; // @ Misc.scala l258
      end
      5'h08 : begin
        _zz_io_wmask = 4'b0010; // @ Misc.scala l258
      end
      5'h11 : begin
        _zz_io_wmask = 4'b1100; // @ Misc.scala l258
      end
      5'h10 : begin
        _zz_io_wmask = 4'b0100; // @ Misc.scala l258
      end
      5'h18 : begin
        _zz_io_wmask = 4'b1000; // @ Misc.scala l258
      end
      default : begin
        _zz_io_wmask = 4'b0000; // @ Misc.scala l254
      end
    endcase
  end

  assign io_wmask = _zz_io_wmask; // @ LSU.scala l181

endmodule

module ysyx_23060082_BranchCond (
  input  wire [2:0]    io_branch,
  input  wire          io_less,
  input  wire          io_zero,
  output wire          io_pcAsrc,
  output wire          io_pcBsrc
);

  reg                 _zz_io_pcAsrc;

  always @(*) begin
    case(io_branch)
      3'b001 : begin
        _zz_io_pcAsrc = 1'b1; // @ Misc.scala l258
      end
      3'b010 : begin
        _zz_io_pcAsrc = 1'b1; // @ Misc.scala l258
      end
      3'b100 : begin
        _zz_io_pcAsrc = io_zero; // @ Misc.scala l258
      end
      3'b101 : begin
        _zz_io_pcAsrc = (! io_zero); // @ Misc.scala l258
      end
      3'b110 : begin
        _zz_io_pcAsrc = io_less; // @ Misc.scala l258
      end
      3'b111 : begin
        _zz_io_pcAsrc = (! io_less); // @ Misc.scala l258
      end
      default : begin
        _zz_io_pcAsrc = 1'b0; // @ Misc.scala l254
      end
    endcase
  end

  assign io_pcAsrc = _zz_io_pcAsrc; // @ EXU.scala l99
  assign io_pcBsrc = (io_branch == 3'b010); // @ EXU.scala l108

endmodule

module ysyx_23060082_ALU (
  input  wire [31:0]   io_aluIn1,
  input  wire [31:0]   io_aluIn2,
  input  wire [3:0]    io_aluCtr,
  output wire          io_less,
  output wire          io_zero,
  output wire [31:0]   io_aluResult
);

  wire       [32:0]   _zz_resultAdder33Bit;
  wire       [32:0]   _zz_resultAdder33Bit_1;
  wire       [32:0]   _zz_resultAdder33Bit_2;
  wire       [32:0]   _zz_resultAdder33Bit_3;
  wire       [31:0]   _zz_resultShift;
  wire       [31:0]   _zz_resultShift_1;
  wire       [0:0]    _zz_resultSlt;
  wire                subORadd;
  wire       [31:0]   adderDataB;
  wire       [0:0]    adderCin;
  wire       [32:0]   resultAdder33Bit;
  wire       [31:0]   resultAdder;
  wire                carryFlag;
  wire                zeroFlag;
  wire                overflowFlag;
  wire       [1:0]    switch_Misc_l245;
  reg        [31:0]   resultShift;
  wire                lessFlag0;
  wire                lessFlag1;
  wire                lessFlag;
  wire       [31:0]   resultSlt;
  wire       [31:0]   resultXor;
  wire       [31:0]   resultOr;
  wire       [31:0]   resultAnd;
  wire       [2:0]    switch_Misc_l245_1;
  reg        [31:0]   _zz_io_aluResult;

  assign _zz_resultAdder33Bit = (_zz_resultAdder33Bit_1 + _zz_resultAdder33Bit_2);
  assign _zz_resultAdder33Bit_1 = {1'd0, io_aluIn1};
  assign _zz_resultAdder33Bit_2 = {1'd0, adderDataB};
  assign _zz_resultAdder33Bit_3 = {32'd0, adderCin};
  assign _zz_resultShift = ($signed(_zz_resultShift_1) >>> io_aluIn2[4 : 0]);
  assign _zz_resultShift_1 = io_aluIn1;
  assign _zz_resultSlt = lessFlag;
  assign subORadd = (io_aluCtr[1] || io_aluCtr[3]); // @ BaseType.scala l308
  assign adderDataB = (subORadd ? (~ io_aluIn2) : io_aluIn2); // @ Expression.scala l1531
  assign adderCin = subORadd; // @ BaseType.scala l321
  assign resultAdder33Bit = (_zz_resultAdder33Bit + _zz_resultAdder33Bit_3); // @ BaseType.scala l302
  assign resultAdder = resultAdder33Bit[31 : 0]; // @ BaseType.scala l302
  assign carryFlag = resultAdder33Bit[32]; // @ BaseType.scala l308
  assign zeroFlag = (resultAdder == 32'h0); // @ BaseType.scala l308
  assign overflowFlag = ((io_aluIn1[31] == adderDataB[31]) && (resultAdder[31] != io_aluIn1[31])); // @ BaseType.scala l308
  assign switch_Misc_l245 = io_aluCtr[3 : 2]; // @ BaseType.scala l302
  always @(*) begin
    case(switch_Misc_l245)
      2'b01 : begin
        resultShift = (io_aluIn1 >>> io_aluIn2[4 : 0]); // @ Misc.scala l258
      end
      2'b11 : begin
        resultShift = _zz_resultShift; // @ Misc.scala l258
      end
      default : begin
        resultShift = (io_aluIn1 <<< io_aluIn2[4 : 0]); // @ Misc.scala l254
      end
    endcase
  end

  assign lessFlag0 = (overflowFlag ^ resultAdder[31]); // @ BaseType.scala l308
  assign lessFlag1 = (carryFlag ^ subORadd); // @ BaseType.scala l308
  assign lessFlag = (io_aluCtr[3] ? lessFlag1 : lessFlag0); // @ Expression.scala l1531
  assign resultSlt = {31'd0, _zz_resultSlt}; // @ BaseType.scala l302
  assign resultXor = (io_aluIn1 ^ io_aluIn2); // @ BaseType.scala l302
  assign resultOr = (io_aluIn1 | io_aluIn2); // @ BaseType.scala l302
  assign resultAnd = (io_aluIn1 & io_aluIn2); // @ BaseType.scala l302
  assign io_less = lessFlag; // @ EXU.scala l165
  assign io_zero = zeroFlag; // @ EXU.scala l166
  assign switch_Misc_l245_1 = io_aluCtr[2 : 0]; // @ BaseType.scala l302
  always @(*) begin
    case(switch_Misc_l245_1)
      3'b000 : begin
        _zz_io_aluResult = resultAdder; // @ Misc.scala l258
      end
      3'b001 : begin
        _zz_io_aluResult = resultShift; // @ Misc.scala l258
      end
      3'b010 : begin
        _zz_io_aluResult = resultSlt; // @ Misc.scala l258
      end
      3'b011 : begin
        _zz_io_aluResult = io_aluIn2; // @ Misc.scala l258
      end
      3'b100 : begin
        _zz_io_aluResult = resultXor; // @ Misc.scala l258
      end
      3'b101 : begin
        _zz_io_aluResult = resultShift; // @ Misc.scala l258
      end
      3'b110 : begin
        _zz_io_aluResult = resultOr; // @ Misc.scala l258
      end
      default : begin
        _zz_io_aluResult = resultAnd; // @ Misc.scala l258
      end
    endcase
  end

  assign io_aluResult = _zz_io_aluResult; // @ EXU.scala l167

endmodule

module ysyx_23060082_Decoder (
  input  wire [31:0]   io_instr,
  output wire          io_ctrl_rfCtrl_mem2reg,
  output wire          io_ctrl_rfCtrl_csr2reg,
  output wire          io_ctrl_rfCtrl_regWr,
  output wire [4:0]    io_ctrl_rfCtrl_rfWriteAddr,
  output wire          io_ctrl_aluCtrl_aluAsrc,
  output wire [1:0]    io_ctrl_aluCtrl_aluBsrc,
  output wire [3:0]    io_ctrl_aluCtrl_aluCtr,
  output wire [2:0]    io_ctrl_aluCtrl_branch,
  output wire          io_ctrl_memCtrl_memWr,
  output wire [2:0]    io_ctrl_memCtrl_memOp,
  output wire [2:0]    io_ctrl_csrCtrl_csrCmd,
  output wire          io_ctrl_csrCtrl_illegal,
  output wire          io_ctrl_csrCtrl_ebreak,
  output wire          io_ctrl_csrCtrl_trapEnter,
  output wire          io_ctrl_csrCtrl_trapExit,
  output wire          io_ctrl_fenceI,
  output wire [31:0]   io_imm,
  output wire          io_isCalc,
  output wire          io_isMem,
  output wire          io_isBranch,
  output wire          io_isJump,
  output wire          io_isCsr,
  output wire          io_isSys
);

  wire                _zz_isLegal;
  wire                _zz_isLegal_1;
  wire                _zz_io_isCalc;
  wire       [3:0]    _zz_io_ctrl_aluCtrl_aluCtr_7;
  wire       [3:0]    _zz_io_ctrl_aluCtrl_aluCtr_8;
  wire       [3:0]    _zz_io_ctrl_aluCtrl_aluCtr_9;
  wire       [3:0]    _zz_io_ctrl_aluCtrl_aluCtr_10;
  wire       [3:0]    _zz_io_ctrl_aluCtrl_aluCtr_11;
  wire       [3:0]    _zz_io_ctrl_aluCtrl_aluCtr_12;
  wire       [3:0]    _zz_io_ctrl_aluCtrl_aluCtr_13;
  wire       [3:0]    _zz_io_ctrl_aluCtrl_aluCtr_14;
  wire                _zz_io_ctrl_aluCtrl_aluCtr_15;
  wire       [31:0]   i;
  wire       [6:0]    op;
  wire       [2:0]    func3;
  wire                i_add;
  wire                i_sub;
  wire                i_sll;
  wire                i_slt;
  wire                i_sltu;
  wire                i_xor;
  wire                i_srl;
  wire                i_sra;
  wire                i_or;
  wire                i_and;
  wire                i_addi;
  wire                i_slli;
  wire                i_slti;
  wire                i_sltiu;
  wire                i_xori;
  wire                i_srli;
  wire                i_srai;
  wire                i_ori;
  wire                i_andi;
  wire                i_lb;
  wire                i_lh;
  wire                i_lw;
  wire                i_lbu;
  wire                i_lhu;
  wire                i_sb;
  wire                i_sh;
  wire                i_sw;
  wire                i_beq;
  wire                i_bne;
  wire                i_blt;
  wire                i_bge;
  wire                i_bltu;
  wire                i_bgeu;
  wire                i_jalr;
  wire                i_jal;
  wire                i_lui;
  wire                i_auipc;
  wire                i_csrrw;
  wire                i_csrrs;
  wire                i_ecall;
  wire                i_ebreak;
  wire                i_mret;
  wire                i_fence_i;
  wire                isLegal;
  wire                i_illegal;
  wire                typeU;
  wire                typeJ;
  wire                typeI;
  wire                typeS;
  wire                typeB;
  wire                typeR;
  wire       [31:0]   immU;
  wire       [31:0]   immJ;
  wire       [31:0]   immI;
  wire       [31:0]   immS;
  wire       [31:0]   immB;
  wire                _zz_io_imm;
  wire                csrWb;
  wire                _zz_io_ctrl_aluCtrl_aluCtr;
  wire                _zz_io_ctrl_aluCtrl_aluCtr_1;
  wire                _zz_io_ctrl_aluCtrl_aluCtr_2;
  wire                _zz_io_ctrl_aluCtrl_aluCtr_3;
  wire                _zz_io_ctrl_aluCtrl_aluCtr_4;
  wire                _zz_io_ctrl_aluCtrl_aluCtr_5;
  wire                _zz_io_ctrl_aluCtrl_aluCtr_6;
  wire                _zz_io_ctrl_aluCtrl_branch;
  wire                _zz_io_ctrl_aluCtrl_branch_1;

  assign _zz_isLegal = ((((((((((((((((_zz_isLegal_1 || i_slli) || i_slti) || i_sltiu) || i_xori) || i_srli) || i_srai) || i_ori) || i_andi) || i_lb) || i_lh) || i_lw) || i_lbu) || i_lhu) || i_sb) || i_sh) || i_sw);
  assign _zz_isLegal_1 = ((((((((((i_add || i_sub) || i_sll) || i_slt) || i_sltu) || i_xor) || i_srl) || i_sra) || i_or) || i_and) || i_addi);
  assign _zz_io_isCalc = ((((i_add || i_sub) || i_sll) || i_slt) || i_sltu);
  assign _zz_io_ctrl_aluCtrl_aluCtr_7 = 4'b0111;
  assign _zz_io_ctrl_aluCtrl_aluCtr_8 = 4'b0110;
  assign _zz_io_ctrl_aluCtrl_aluCtr_9 = 4'b0100;
  assign _zz_io_ctrl_aluCtrl_aluCtr_10 = 4'b0001;
  assign _zz_io_ctrl_aluCtrl_aluCtr_11 = 4'b0101;
  assign _zz_io_ctrl_aluCtrl_aluCtr_12 = 4'b1101;
  assign _zz_io_ctrl_aluCtrl_aluCtr_13 = 4'b1000;
  assign _zz_io_ctrl_aluCtrl_aluCtr_14 = 4'b0011;
  assign _zz_io_ctrl_aluCtrl_aluCtr_15 = ((i_sltu || i_sltiu) || i_bltu);
  assign i = io_instr; // @ BaseType.scala l321
  assign op = io_instr[6 : 0]; // @ BaseType.scala l302
  assign func3 = io_instr[14 : 12]; // @ BaseType.scala l302
  assign io_ctrl_rfCtrl_rfWriteAddr = io_instr[11 : 7]; // @ Decoder.scala l27
  assign i_add = ((i & 32'hfe00707f) == 32'h00000033); // @ BaseType.scala l308
  assign i_sub = ((i & 32'hfe00707f) == 32'h40000033); // @ BaseType.scala l308
  assign i_sll = ((i & 32'hfe00707f) == 32'h00001033); // @ BaseType.scala l308
  assign i_slt = ((i & 32'hfe00707f) == 32'h00002033); // @ BaseType.scala l308
  assign i_sltu = ((i & 32'hfe00707f) == 32'h00003033); // @ BaseType.scala l308
  assign i_xor = ((i & 32'hfe00707f) == 32'h00004033); // @ BaseType.scala l308
  assign i_srl = ((i & 32'hfe00707f) == 32'h00005033); // @ BaseType.scala l308
  assign i_sra = ((i & 32'hfe00707f) == 32'h40005033); // @ BaseType.scala l308
  assign i_or = ((i & 32'hfe00707f) == 32'h00006033); // @ BaseType.scala l308
  assign i_and = ((i & 32'hfe00707f) == 32'h00007033); // @ BaseType.scala l308
  assign i_addi = ((i & 32'h0000707f) == 32'h00000013); // @ BaseType.scala l308
  assign i_slli = ((i & 32'hfe00707f) == 32'h00001013); // @ BaseType.scala l308
  assign i_slti = ((i & 32'h0000707f) == 32'h00002013); // @ BaseType.scala l308
  assign i_sltiu = ((i & 32'h0000707f) == 32'h00003013); // @ BaseType.scala l308
  assign i_xori = ((i & 32'h0000707f) == 32'h00004013); // @ BaseType.scala l308
  assign i_srli = ((i & 32'hfe00707f) == 32'h00005013); // @ BaseType.scala l308
  assign i_srai = ((i & 32'hfe00707f) == 32'h40005013); // @ BaseType.scala l308
  assign i_ori = ((i & 32'h0000707f) == 32'h00006013); // @ BaseType.scala l308
  assign i_andi = ((i & 32'h0000707f) == 32'h00007013); // @ BaseType.scala l308
  assign i_lb = ((i & 32'h0000707f) == 32'h00000003); // @ BaseType.scala l308
  assign i_lh = ((i & 32'h0000707f) == 32'h00001003); // @ BaseType.scala l308
  assign i_lw = ((i & 32'h0000707f) == 32'h00002003); // @ BaseType.scala l308
  assign i_lbu = ((i & 32'h0000707f) == 32'h00004003); // @ BaseType.scala l308
  assign i_lhu = ((i & 32'h0000707f) == 32'h00005003); // @ BaseType.scala l308
  assign i_sb = ((i & 32'h0000707f) == 32'h00000023); // @ BaseType.scala l308
  assign i_sh = ((i & 32'h0000707f) == 32'h00001023); // @ BaseType.scala l308
  assign i_sw = ((i & 32'h0000707f) == 32'h00002023); // @ BaseType.scala l308
  assign i_beq = ((i & 32'h0000707f) == 32'h00000063); // @ BaseType.scala l308
  assign i_bne = ((i & 32'h0000707f) == 32'h00001063); // @ BaseType.scala l308
  assign i_blt = ((i & 32'h0000707f) == 32'h00004063); // @ BaseType.scala l308
  assign i_bge = ((i & 32'h0000707f) == 32'h00005063); // @ BaseType.scala l308
  assign i_bltu = ((i & 32'h0000707f) == 32'h00006063); // @ BaseType.scala l308
  assign i_bgeu = ((i & 32'h0000707f) == 32'h00007063); // @ BaseType.scala l308
  assign i_jalr = ((i & 32'h0000707f) == 32'h00000067); // @ BaseType.scala l308
  assign i_jal = ((i & 32'h0000007f) == 32'h0000006f); // @ BaseType.scala l308
  assign i_lui = ((i & 32'h0000007f) == 32'h00000037); // @ BaseType.scala l308
  assign i_auipc = ((i & 32'h0000007f) == 32'h00000017); // @ BaseType.scala l308
  assign i_csrrw = ((i & 32'h0000707f) == 32'h00001073); // @ BaseType.scala l308
  assign i_csrrs = ((i & 32'h0000707f) == 32'h00002073); // @ BaseType.scala l308
  assign i_ecall = ((i & 32'hffffffff) == 32'h00000073); // @ BaseType.scala l308
  assign i_ebreak = ((i & 32'hffffffff) == 32'h00100073); // @ BaseType.scala l308
  assign i_mret = ((i & 32'hffffffff) == 32'h30200073); // @ BaseType.scala l308
  assign i_fence_i = ((i & 32'h0000707f) == 32'h0000100f); // @ BaseType.scala l308
  assign isLegal = ((((((((((((((((_zz_isLegal || i_beq) || i_bne) || i_blt) || i_bge) || i_bltu) || i_bgeu) || i_jalr) || i_jal) || i_lui) || i_auipc) || i_csrrw) || i_csrrs) || i_ecall) || i_ebreak) || i_mret) || i_fence_i); // @ BaseType.scala l308
  assign i_illegal = ((io_instr != 32'h0) && (! isLegal)); // @ BaseType.scala l308
  assign typeU = (op[4 : 2] == 3'b101); // @ BaseType.scala l308
  assign typeJ = (op[6 : 2] == 5'h1b); // @ BaseType.scala l308
  assign typeI = ((((op[6 : 2] == 5'h04) || (op[6 : 2] == 5'h0)) || (op[6 : 2] == 5'h19)) || ((op[6 : 2] == 5'h1c) && (func3 != 3'b000))); // @ BaseType.scala l308
  assign typeS = (op[6 : 2] == 5'h08); // @ BaseType.scala l308
  assign typeB = (op[6 : 2] == 5'h18); // @ BaseType.scala l308
  assign typeR = (op[6 : 2] == 5'h0c); // @ BaseType.scala l308
  assign io_isCalc = ((((((((((((((((_zz_io_isCalc || i_xor) || i_srl) || i_sra) || i_or) || i_and) || i_addi) || i_slli) || i_slti) || i_sltiu) || i_xori) || i_srli) || i_srai) || i_ori) || i_andi) || i_lui) || i_auipc); // @ Decoder.scala l107
  assign io_isMem = (((((((i_lb || i_lh) || i_lw) || i_lbu) || i_lhu) || i_sb) || i_sh) || i_sw); // @ Decoder.scala l110
  assign io_isBranch = (((((i_beq || i_bne) || i_blt) || i_bge) || i_bltu) || i_bgeu); // @ Decoder.scala l111
  assign io_isJump = (i_jal || i_jalr); // @ Decoder.scala l112
  assign io_isCsr = (i_csrrw || i_csrrs); // @ Decoder.scala l113
  assign io_isSys = (((i_ecall || i_ebreak) || i_mret) || i_fence_i); // @ Decoder.scala l114
  assign immU = {io_instr[31 : 12],12'h0}; // @ BaseType.scala l302
  assign immJ = {{{{{12{io_instr[31]}},io_instr[19 : 12]},io_instr[20]},io_instr[30 : 21]},1'b0}; // @ BaseType.scala l302
  assign immI = {{20{io_instr[31]}},io_instr[31 : 20]}; // @ BaseType.scala l302
  assign immS = {{{20{io_instr[31]}},io_instr[31 : 25]},io_instr[11 : 7]}; // @ BaseType.scala l302
  assign immB = {{{{{20{io_instr[31]}},io_instr[7]},io_instr[30 : 25]},io_instr[11 : 8]},1'b0}; // @ BaseType.scala l302
  assign _zz_io_imm = (typeU || typeJ); // @ BaseType.scala l308
  assign io_imm = ((_zz_io_imm || (typeI || typeS)) ? (_zz_io_imm ? (typeU ? immU : immJ) : (typeI ? immI : immS)) : (typeB ? immB : 32'h0)); // @ Decoder.scala l125
  assign csrWb = (i_csrrw || i_csrrs); // @ BaseType.scala l308
  assign io_ctrl_rfCtrl_regWr = ((((typeU || typeJ) || typeI) || typeR) || csrWb); // @ Decoder.scala l135
  assign io_ctrl_aluCtrl_aluAsrc = ((i_auipc || i_jal) || i_jalr); // @ Decoder.scala l136
  assign io_ctrl_aluCtrl_aluBsrc = ((typeR || typeB) ? 2'b00 : ((i_jal || i_jalr) ? 2'b10 : 2'b01)); // @ Decoder.scala l138
  assign _zz_io_ctrl_aluCtrl_aluCtr = (i_and || i_andi); // @ BaseType.scala l308
  assign _zz_io_ctrl_aluCtrl_aluCtr_1 = (i_xor || i_xori); // @ BaseType.scala l308
  assign _zz_io_ctrl_aluCtrl_aluCtr_2 = (i_srl || i_srli); // @ BaseType.scala l308
  assign _zz_io_ctrl_aluCtrl_aluCtr_3 = (((((i_slt || i_slti) || i_beq) || i_bne) || i_blt) || i_bge); // @ BaseType.scala l308
  assign _zz_io_ctrl_aluCtrl_aluCtr_4 = (_zz_io_ctrl_aluCtrl_aluCtr || (i_or || i_ori)); // @ BaseType.scala l308
  assign _zz_io_ctrl_aluCtrl_aluCtr_5 = (_zz_io_ctrl_aluCtrl_aluCtr_2 || (i_sra || i_srai)); // @ BaseType.scala l308
  assign _zz_io_ctrl_aluCtrl_aluCtr_6 = (_zz_io_ctrl_aluCtrl_aluCtr_4 || (_zz_io_ctrl_aluCtrl_aluCtr_1 || (i_sll || i_slli))); // @ BaseType.scala l308
  assign io_ctrl_aluCtrl_aluCtr = ((_zz_io_ctrl_aluCtrl_aluCtr_6 || (_zz_io_ctrl_aluCtrl_aluCtr_5 || (i_sub || i_lui))) ? (_zz_io_ctrl_aluCtrl_aluCtr_6 ? (_zz_io_ctrl_aluCtrl_aluCtr_4 ? (_zz_io_ctrl_aluCtrl_aluCtr ? _zz_io_ctrl_aluCtrl_aluCtr_7 : _zz_io_ctrl_aluCtrl_aluCtr_8) : (_zz_io_ctrl_aluCtrl_aluCtr_1 ? _zz_io_ctrl_aluCtrl_aluCtr_9 : _zz_io_ctrl_aluCtrl_aluCtr_10)) : (_zz_io_ctrl_aluCtrl_aluCtr_5 ? (_zz_io_ctrl_aluCtrl_aluCtr_2 ? _zz_io_ctrl_aluCtrl_aluCtr_11 : _zz_io_ctrl_aluCtrl_aluCtr_12) : (i_sub ? _zz_io_ctrl_aluCtrl_aluCtr_13 : _zz_io_ctrl_aluCtrl_aluCtr_14))) : ((_zz_io_ctrl_aluCtrl_aluCtr_3 || (_zz_io_ctrl_aluCtrl_aluCtr_15 || i_bgeu)) ? (_zz_io_ctrl_aluCtrl_aluCtr_3 ? 4'b0010 : 4'b1010) : 4'b0000)); // @ Decoder.scala l142
  assign _zz_io_ctrl_aluCtrl_branch = (i_blt || i_bltu); // @ BaseType.scala l308
  assign _zz_io_ctrl_aluCtrl_branch_1 = (i_jal || i_jalr); // @ BaseType.scala l308
  assign io_ctrl_aluCtrl_branch = ((_zz_io_ctrl_aluCtrl_branch_1 || (i_beq || i_bne)) ? (_zz_io_ctrl_aluCtrl_branch_1 ? (i_jal ? 3'b001 : 3'b010) : (i_beq ? 3'b100 : 3'b101)) : ((_zz_io_ctrl_aluCtrl_branch || (i_bge || i_bgeu)) ? (_zz_io_ctrl_aluCtrl_branch ? 3'b110 : 3'b111) : 3'b000)); // @ Decoder.scala l156
  assign io_ctrl_rfCtrl_mem2reg = (op[6 : 2] == 5'h0); // @ Decoder.scala l165
  assign io_ctrl_rfCtrl_csr2reg = csrWb; // @ Decoder.scala l166
  assign io_ctrl_memCtrl_memWr = typeS; // @ Decoder.scala l167
  assign io_ctrl_fenceI = i_fence_i; // @ Decoder.scala l168
  assign io_ctrl_memCtrl_memOp = func3; // @ Decoder.scala l169
  assign io_ctrl_csrCtrl_csrCmd = (i_csrrw ? 3'b001 : (i_csrrs ? 3'b010 : 3'b000)); // @ Decoder.scala l179
  assign io_ctrl_csrCtrl_illegal = i_illegal; // @ Decoder.scala l181
  assign io_ctrl_csrCtrl_ebreak = i_ebreak; // @ Decoder.scala l182
  assign io_ctrl_csrCtrl_trapEnter = ((i_ecall || i_ebreak) || i_illegal); // @ Decoder.scala l183
  assign io_ctrl_csrCtrl_trapExit = i_mret; // @ Decoder.scala l184

endmodule

module ysyx_23060082_Icache (
  input  wire          io_reqIn_valid,
  output wire          io_reqIn_ready,
  input  wire [31:0]   io_reqIn_payload_pc,
  output wire          io_rspOut_valid,
  output wire [31:0]   io_rspOut_payload_rdata,
  output wire          io_axi4_ar_valid,
  input  wire          io_axi4_ar_ready,
  output wire [31:0]   io_axi4_ar_payload_addr,
  output wire [3:0]    io_axi4_ar_payload_id,
  output wire [7:0]    io_axi4_ar_payload_len,
  output wire [2:0]    io_axi4_ar_payload_size,
  output wire [1:0]    io_axi4_ar_payload_burst,
  input  wire          io_axi4_r_valid,
  output wire          io_axi4_r_ready,
  input  wire [31:0]   io_axi4_r_payload_data,
  input  wire [3:0]    io_axi4_r_payload_id,
  input  wire [1:0]    io_axi4_r_payload_resp,
  input  wire          io_axi4_r_payload_last,
  input  wire          io_fenceI,
  output wire          io_miss,
  output wire          io_missDone,
  input  wire          clock,
  input  wire          reset
);
  localparam IcacheState_Idle = 1'd0;
  localparam IcacheState_Miss = 1'd1;

  wire       [31:0]   axi4Ctrler_io_readAddr;
  wire                axi4Ctrler_io_readEnd;
  wire       [127:0]  axi4Ctrler_io_readData;
  wire                axi4Ctrler_io_axi4_ar_valid;
  wire       [31:0]   axi4Ctrler_io_axi4_ar_payload_addr;
  wire       [3:0]    axi4Ctrler_io_axi4_ar_payload_id;
  wire       [7:0]    axi4Ctrler_io_axi4_ar_payload_len;
  wire       [2:0]    axi4Ctrler_io_axi4_ar_payload_size;
  wire       [1:0]    axi4Ctrler_io_axi4_ar_payload_burst;
  wire                axi4Ctrler_io_axi4_r_ready;
  reg        [24:0]   _zz_hit;
  reg        [127:0]  _zz__zz_hitWordVec_0;
  reg        [31:0]   _zz_io_rspOut_payload_rdata;
  reg        [31:0]   _zz_io_rspOut_payload_rdata_1;
  wire       [24:0]   tag;
  wire       [2:0]    index;
  reg        [127:0]  dataMem_0;
  reg        [127:0]  dataMem_1;
  reg        [127:0]  dataMem_2;
  reg        [127:0]  dataMem_3;
  reg        [127:0]  dataMem_4;
  reg        [127:0]  dataMem_5;
  reg        [127:0]  dataMem_6;
  reg        [127:0]  dataMem_7;
  reg        [24:0]   tagMem_0;
  reg        [24:0]   tagMem_1;
  reg        [24:0]   tagMem_2;
  reg        [24:0]   tagMem_3;
  reg        [24:0]   tagMem_4;
  reg        [24:0]   tagMem_5;
  reg        [24:0]   tagMem_6;
  reg        [24:0]   tagMem_7;
  reg        [7:0]    validReg;
  wire                hit;
  wire       [127:0]  lineReg;
  wire       [1:0]    wordCnt;
  wire       [1:0]    wordSel;
  wire                reqFire;
  reg        [1:0]    wordSelReg;
  reg        [31:0]   pcReg;
  reg        [2:0]    indexReg;
  reg        [24:0]   tagReg;
  reg        [0:0]    state;
  wire                when_icache_l105;
  wire                when_icache_l106;
  wire                when_icache_l108;
  wire                missDone;
  wire       [31:0]   hitWordVec_0;
  wire       [31:0]   hitWordVec_1;
  wire       [31:0]   hitWordVec_2;
  wire       [31:0]   hitWordVec_3;
  wire       [31:0]   missWordVec_0;
  wire       [31:0]   missWordVec_1;
  wire       [31:0]   missWordVec_2;
  wire       [31:0]   missWordVec_3;
  wire       [127:0]  _zz_hitWordVec_0;
  wire                enterMiss;
  wire       [7:0]    _zz_1;
  wire       [7:0]    _zz_2;
  `ifndef SYNTHESIS
  reg [31:0] state_string;
  `endif


  ysyx_23060082_Axi4_Ctrler_ReadOnly_Burst axi4Ctrler (
    .io_readReq               (enterMiss                               ), //i
    .io_readAddr              (axi4Ctrler_io_readAddr[31:0]            ), //i
    .io_readEnd               (axi4Ctrler_io_readEnd                   ), //o
    .io_readData              (axi4Ctrler_io_readData[127:0]           ), //o
    .io_axi4_ar_valid         (axi4Ctrler_io_axi4_ar_valid             ), //o
    .io_axi4_ar_ready         (io_axi4_ar_ready                        ), //i
    .io_axi4_ar_payload_addr  (axi4Ctrler_io_axi4_ar_payload_addr[31:0]), //o
    .io_axi4_ar_payload_id    (axi4Ctrler_io_axi4_ar_payload_id[3:0]   ), //o
    .io_axi4_ar_payload_len   (axi4Ctrler_io_axi4_ar_payload_len[7:0]  ), //o
    .io_axi4_ar_payload_size  (axi4Ctrler_io_axi4_ar_payload_size[2:0] ), //o
    .io_axi4_ar_payload_burst (axi4Ctrler_io_axi4_ar_payload_burst[1:0]), //o
    .io_axi4_r_valid          (io_axi4_r_valid                         ), //i
    .io_axi4_r_ready          (axi4Ctrler_io_axi4_r_ready              ), //o
    .io_axi4_r_payload_data   (io_axi4_r_payload_data[31:0]            ), //i
    .io_axi4_r_payload_id     (io_axi4_r_payload_id[3:0]               ), //i
    .io_axi4_r_payload_resp   (io_axi4_r_payload_resp[1:0]             ), //i
    .io_axi4_r_payload_last   (io_axi4_r_payload_last                  ), //i
    .clock                    (clock                                   ), //i
    .reset                    (reset                                   )  //i
  );
  always @(*) begin
    case(index)
      3'b000 : begin
        _zz_hit = tagMem_0;
        _zz__zz_hitWordVec_0 = dataMem_0;
      end
      3'b001 : begin
        _zz_hit = tagMem_1;
        _zz__zz_hitWordVec_0 = dataMem_1;
      end
      3'b010 : begin
        _zz_hit = tagMem_2;
        _zz__zz_hitWordVec_0 = dataMem_2;
      end
      3'b011 : begin
        _zz_hit = tagMem_3;
        _zz__zz_hitWordVec_0 = dataMem_3;
      end
      3'b100 : begin
        _zz_hit = tagMem_4;
        _zz__zz_hitWordVec_0 = dataMem_4;
      end
      3'b101 : begin
        _zz_hit = tagMem_5;
        _zz__zz_hitWordVec_0 = dataMem_5;
      end
      3'b110 : begin
        _zz_hit = tagMem_6;
        _zz__zz_hitWordVec_0 = dataMem_6;
      end
      default : begin
        _zz_hit = tagMem_7;
        _zz__zz_hitWordVec_0 = dataMem_7;
      end
    endcase
  end

  always @(*) begin
    case(wordSel)
      2'b00 : _zz_io_rspOut_payload_rdata = hitWordVec_0;
      2'b01 : _zz_io_rspOut_payload_rdata = hitWordVec_1;
      2'b10 : _zz_io_rspOut_payload_rdata = hitWordVec_2;
      default : _zz_io_rspOut_payload_rdata = hitWordVec_3;
    endcase
  end

  always @(*) begin
    case(wordSelReg)
      2'b00 : _zz_io_rspOut_payload_rdata_1 = missWordVec_0;
      2'b01 : _zz_io_rspOut_payload_rdata_1 = missWordVec_1;
      2'b10 : _zz_io_rspOut_payload_rdata_1 = missWordVec_2;
      default : _zz_io_rspOut_payload_rdata_1 = missWordVec_3;
    endcase
  end

  `ifndef SYNTHESIS
  always @(*) begin
    case(state)
      IcacheState_Idle : state_string = "Idle";
      IcacheState_Miss : state_string = "Miss";
      default : state_string = "????";
    endcase
  end
  `endif

  assign io_axi4_ar_valid = axi4Ctrler_io_axi4_ar_valid; // @ icache.scala l77
  assign io_axi4_ar_payload_addr = axi4Ctrler_io_axi4_ar_payload_addr; // @ icache.scala l77
  assign io_axi4_ar_payload_id = axi4Ctrler_io_axi4_ar_payload_id; // @ icache.scala l77
  assign io_axi4_ar_payload_len = axi4Ctrler_io_axi4_ar_payload_len; // @ icache.scala l77
  assign io_axi4_ar_payload_size = axi4Ctrler_io_axi4_ar_payload_size; // @ icache.scala l77
  assign io_axi4_ar_payload_burst = axi4Ctrler_io_axi4_ar_payload_burst; // @ icache.scala l77
  assign io_axi4_r_ready = axi4Ctrler_io_axi4_r_ready; // @ icache.scala l77
  assign tag = io_reqIn_payload_pc[31 : 7]; // @ BaseType.scala l302
  assign index = io_reqIn_payload_pc[6 : 4]; // @ BaseType.scala l302
  assign hit = (validReg[index] && (_zz_hit == tag)); // @ BaseType.scala l308
  assign lineReg = 128'h0;
  assign wordCnt = 2'b00;
  assign wordSel = io_reqIn_payload_pc[3 : 2]; // @ BaseType.scala l302
  assign reqFire = (io_reqIn_valid && io_reqIn_ready); // @ BaseType.scala l308
  assign when_icache_l105 = (state == IcacheState_Idle); // @ BaseType.scala l308
  assign when_icache_l106 = (io_reqIn_valid && (! hit)); // @ BaseType.scala l308
  assign when_icache_l108 = (state == IcacheState_Miss); // @ BaseType.scala l308
  assign io_reqIn_ready = (state == IcacheState_Idle); // @ icache.scala l114
  assign missDone = ((state == IcacheState_Miss) && axi4Ctrler_io_readEnd); // @ BaseType.scala l308
  assign io_rspOut_valid = ((reqFire && hit) || missDone); // @ icache.scala l118
  assign io_missDone = missDone; // @ icache.scala l119
  assign _zz_hitWordVec_0 = _zz__zz_hitWordVec_0; // @ Vec.scala l225
  assign hitWordVec_0 = _zz_hitWordVec_0[31 : 0]; // @ icache.scala l125
  assign missWordVec_0 = axi4Ctrler_io_readData[31 : 0]; // @ icache.scala l126
  assign hitWordVec_1 = _zz_hitWordVec_0[63 : 32]; // @ icache.scala l125
  assign missWordVec_1 = axi4Ctrler_io_readData[63 : 32]; // @ icache.scala l126
  assign hitWordVec_2 = _zz_hitWordVec_0[95 : 64]; // @ icache.scala l125
  assign missWordVec_2 = axi4Ctrler_io_readData[95 : 64]; // @ icache.scala l126
  assign hitWordVec_3 = _zz_hitWordVec_0[127 : 96]; // @ icache.scala l125
  assign missWordVec_3 = axi4Ctrler_io_readData[127 : 96]; // @ icache.scala l126
  assign io_rspOut_payload_rdata = ((reqFire && hit) ? _zz_io_rspOut_payload_rdata : _zz_io_rspOut_payload_rdata_1); // @ icache.scala l129
  assign enterMiss = (((state == IcacheState_Idle) && io_reqIn_valid) && (! hit)); // @ BaseType.scala l308
  assign axi4Ctrler_io_readAddr = {io_reqIn_payload_pc[31 : 4],4'b0000}; // @ icache.scala l136
  assign io_miss = enterMiss; // @ icache.scala l140
  assign _zz_1 = ({7'd0,1'b1} <<< indexReg); // @ BaseType.scala l302
  assign _zz_2 = ({7'd0,1'b1} <<< indexReg); // @ BaseType.scala l302
  always @(posedge clock or posedge reset) begin
    if(reset) begin
      validReg <= 8'h0; // @ Data.scala l432
      wordSelReg <= 2'b00; // @ Data.scala l432
      pcReg <= 32'h0; // @ Data.scala l432
      indexReg <= 3'b000; // @ Data.scala l432
      tagReg <= 25'h0; // @ Data.scala l432
      state <= IcacheState_Idle; // @ Data.scala l432
    end else begin
      if(reqFire) begin
        wordSelReg <= wordSel; // @ icache.scala l93
      end
      if(reqFire) begin
        pcReg <= io_reqIn_payload_pc; // @ icache.scala l95
      end
      if(reqFire) begin
        indexReg <= index; // @ icache.scala l96
      end
      if(reqFire) begin
        tagReg <= tag; // @ icache.scala l97
      end
      if(when_icache_l105) begin
        if(when_icache_l106) begin
          state <= IcacheState_Miss; // @ Enum.scala l159
        end else begin
          state <= IcacheState_Idle; // @ Enum.scala l159
        end
      end else begin
        if(when_icache_l108) begin
          if(axi4Ctrler_io_readEnd) begin
            state <= IcacheState_Idle; // @ Enum.scala l159
          end else begin
            state <= IcacheState_Miss; // @ Enum.scala l159
          end
        end
      end
      if(io_fenceI) begin
        validReg <= 8'h0; // @ icache.scala l150
      end else begin
        if(missDone) begin
          validReg[indexReg] <= 1'b1; // @ icache.scala l152
        end else begin
          validReg <= validReg; // @ icache.scala l154
        end
      end
    end
  end

  always @(posedge clock) begin
    if(missDone) begin
      if(_zz_1[0]) begin
        dataMem_0 <= axi4Ctrler_io_readData; // @ icache.scala l144
      end
      if(_zz_1[1]) begin
        dataMem_1 <= axi4Ctrler_io_readData; // @ icache.scala l144
      end
      if(_zz_1[2]) begin
        dataMem_2 <= axi4Ctrler_io_readData; // @ icache.scala l144
      end
      if(_zz_1[3]) begin
        dataMem_3 <= axi4Ctrler_io_readData; // @ icache.scala l144
      end
      if(_zz_1[4]) begin
        dataMem_4 <= axi4Ctrler_io_readData; // @ icache.scala l144
      end
      if(_zz_1[5]) begin
        dataMem_5 <= axi4Ctrler_io_readData; // @ icache.scala l144
      end
      if(_zz_1[6]) begin
        dataMem_6 <= axi4Ctrler_io_readData; // @ icache.scala l144
      end
      if(_zz_1[7]) begin
        dataMem_7 <= axi4Ctrler_io_readData; // @ icache.scala l144
      end
      if(_zz_2[0]) begin
        tagMem_0 <= tagReg; // @ icache.scala l145
      end
      if(_zz_2[1]) begin
        tagMem_1 <= tagReg; // @ icache.scala l145
      end
      if(_zz_2[2]) begin
        tagMem_2 <= tagReg; // @ icache.scala l145
      end
      if(_zz_2[3]) begin
        tagMem_3 <= tagReg; // @ icache.scala l145
      end
      if(_zz_2[4]) begin
        tagMem_4 <= tagReg; // @ icache.scala l145
      end
      if(_zz_2[5]) begin
        tagMem_5 <= tagReg; // @ icache.scala l145
      end
      if(_zz_2[6]) begin
        tagMem_6 <= tagReg; // @ icache.scala l145
      end
      if(_zz_2[7]) begin
        tagMem_7 <= tagReg; // @ icache.scala l145
      end
    end
  end


endmodule

module ysyx_23060082_Axi4_Ctrler_ReadOnly_Burst (
  input  wire          io_readReq,
  input  wire [31:0]   io_readAddr,
  output wire          io_readEnd,
  output wire [127:0]  io_readData,
  output reg           io_axi4_ar_valid,
  input  wire          io_axi4_ar_ready,
  output reg  [31:0]   io_axi4_ar_payload_addr,
  output wire [3:0]    io_axi4_ar_payload_id,
  output wire [7:0]    io_axi4_ar_payload_len,
  output wire [2:0]    io_axi4_ar_payload_size,
  output wire [1:0]    io_axi4_ar_payload_burst,
  input  wire          io_axi4_r_valid,
  output wire          io_axi4_r_ready,
  input  wire [31:0]   io_axi4_r_payload_data,
  input  wire [3:0]    io_axi4_r_payload_id,
  input  wire [1:0]    io_axi4_r_payload_resp,
  input  wire          io_axi4_r_payload_last,
  input  wire          clock,
  input  wire          reset
);

  reg        [95:0]   lineReg;
  reg        [1:0]    wordCnt;
  wire                readOnce;
  wire                io_axi4_ar_fire;
  wire                when_icache_l203;
  wire                when_icache_l205;
  wire                when_icache_l205_1;
  wire                when_icache_l221;

  assign readOnce = (io_axi4_r_valid && io_axi4_r_ready); // @ BaseType.scala l308
  assign io_axi4_ar_payload_id = 4'b0000; // @ icache.scala l177
  assign io_axi4_ar_payload_len = 8'h03; // @ icache.scala l178
  assign io_axi4_ar_payload_size = 3'b010; // @ icache.scala l179
  assign io_axi4_ar_payload_burst = 2'b01; // @ icache.scala l180
  assign io_axi4_ar_fire = (io_axi4_ar_valid && io_axi4_ar_ready); // @ BaseType.scala l308
  assign io_axi4_r_ready = io_axi4_r_valid; // @ icache.scala l196
  assign io_readEnd = (readOnce && io_axi4_r_payload_last); // @ icache.scala l198
  assign io_readData = {io_axi4_r_payload_data,lineReg}; // @ icache.scala l199
  assign when_icache_l203 = (wordCnt == 2'b00); // @ BaseType.scala l308
  assign when_icache_l205 = (wordCnt == 2'b01); // @ BaseType.scala l308
  assign when_icache_l205_1 = (wordCnt == 2'b10); // @ BaseType.scala l308
  assign when_icache_l221 = (readOnce && (io_axi4_r_payload_resp != 2'b00)); // @ BaseType.scala l308
  always @(posedge clock or posedge reset) begin
    if(reset) begin
      io_axi4_ar_valid <= 1'b0; // @ Data.scala l432
    end else begin
      if(io_readReq) begin
        io_axi4_ar_valid <= 1'b1; // @ icache.scala l183
      end else begin
        if(io_axi4_ar_fire) begin
          io_axi4_ar_valid <= 1'b0; // @ icache.scala l185
        end else begin
          io_axi4_ar_valid <= io_axi4_ar_valid; // @ icache.scala l187
        end
      end
      if(when_icache_l221) begin
        `ifndef SYNTHESIS
          `ifdef FORMAL
            assert(1'b0); // core.scala:L569
          `else
            if(!1'b0) begin
              $display("NOTE [IFU] read resp error! resp =%xaddr =%x", io_axi4_r_payload_resp, io_axi4_ar_payload_addr); // core.scala:L569
            end
          `endif
        `endif
      end
    end
  end

  always @(posedge clock) begin
    if(io_readReq) begin
      io_axi4_ar_payload_addr <= io_readAddr; // @ icache.scala l191
    end else begin
      io_axi4_ar_payload_addr <= io_axi4_ar_payload_addr; // @ icache.scala l193
    end
    if(readOnce) begin
      if(when_icache_l203) begin
        lineReg[31 : 0] <= io_axi4_r_payload_data; // @ icache.scala l203
      end else begin
        if(when_icache_l205) begin
          lineReg[63 : 32] <= io_axi4_r_payload_data; // @ icache.scala l205
        end
        if(when_icache_l205_1) begin
          lineReg[95 : 64] <= io_axi4_r_payload_data; // @ icache.scala l205
        end
      end
    end
    if(io_readReq) begin
      wordCnt <= 2'b00; // @ icache.scala l213
    end else begin
      if(readOnce) begin
        wordCnt <= (wordCnt + 2'b01); // @ icache.scala l215
      end else begin
        wordCnt <= wordCnt; // @ icache.scala l217
      end
    end
  end


endmodule
