// Generator : SpinalHDL v1.12.3    git head : 591e64062329e5e2e2b81f4d52422948053edb97
// Component : ysyx_23060082
// Git hash  : 33870509f38696b4cba42d002017b691028f59ff

`timescale 1ns/1ps

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
  input  wire          clock,
  input  wire          reset
);

  wire                ifu_io_output_ready;
  wire                idu_io_output_ready;
  wire                exu_io_output_ready;
  wire       [31:0]   regFile_io_readData1;
  wire       [31:0]   regFile_io_readData2;
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
  wire                idu_io_output_payload_ctrl_rf_si_mem2reg;
  wire                idu_io_output_payload_ctrl_rf_si_csr2reg;
  wire                idu_io_output_payload_ctrl_rf_si_regWr;
  wire       [4:0]    idu_io_output_payload_ctrl_rf_si_rf_write_addr;
  wire                idu_io_output_payload_ctrl_alu_si_alu_asrc;
  wire       [1:0]    idu_io_output_payload_ctrl_alu_si_alu_bsrc;
  wire       [3:0]    idu_io_output_payload_ctrl_alu_si_alu_ctr;
  wire       [2:0]    idu_io_output_payload_ctrl_alu_si_branch;
  wire                idu_io_output_payload_ctrl_mem_si_memWr;
  wire       [2:0]    idu_io_output_payload_ctrl_mem_si_memOp;
  wire       [2:0]    idu_io_output_payload_ctrl_csr_si_csr_cmd;
  wire                idu_io_output_payload_ctrl_csr_si_trap_enter;
  wire                idu_io_output_payload_ctrl_csr_si_trap_exit;
  wire       [31:0]   idu_io_output_payload_imm;
  wire       [31:0]   idu_io_output_payload_rfReadData1;
  wire       [31:0]   idu_io_output_payload_rfReadData2;
  wire       [4:0]    idu_io_rfReadAddr1;
  wire       [4:0]    idu_io_rfReadAddr2;
  wire                exu_io_output_valid;
  wire       [31:0]   exu_io_output_payload_pc;
  wire       [31:0]   exu_io_output_payload_pc_next;
  wire                exu_io_output_payload_rf_ctrl_mem2reg;
  wire                exu_io_output_payload_rf_ctrl_csr2reg;
  wire                exu_io_output_payload_rf_ctrl_regWr;
  wire       [4:0]    exu_io_output_payload_rf_ctrl_rf_write_addr;
  wire                exu_io_output_payload_mem_ctrl_memWr;
  wire       [2:0]    exu_io_output_payload_mem_ctrl_memOp;
  wire       [2:0]    exu_io_output_payload_csr_ctrl_csr_cmd;
  wire                exu_io_output_payload_csr_ctrl_trap_enter;
  wire                exu_io_output_payload_csr_ctrl_trap_exit;
  wire       [11:0]   exu_io_output_payload_imm;
  wire       [31:0]   exu_io_output_payload_rfReadData1;
  wire       [31:0]   exu_io_output_payload_rfReadData2;
  wire       [31:0]   exu_io_output_payload_aluResult;
  wire                lsu_io_output_valid;
  wire       [31:0]   lsu_io_output_payload_pc;
  wire       [31:0]   lsu_io_output_payload_pc_next;
  wire       [31:0]   lsu_io_output_payload_mem_data_out;
  wire       [31:0]   lsu_io_output_payload_alu_data_out;
  wire                lsu_io_output_payload_rf_ctrl_mem2reg;
  wire                lsu_io_output_payload_rf_ctrl_csr2reg;
  wire                lsu_io_output_payload_rf_ctrl_regWr;
  wire       [4:0]    lsu_io_output_payload_rf_ctrl_rf_write_addr;
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
  wire                wbu_io_input_ready;
  wire                wbu_io_output_valid;
  wire       [31:0]   wbu_io_output_payload_pc_next;
  wire       [4:0]    wbu_io_rf_write_addr;
  wire       [31:0]   wbu_io_rf_write_data;
  wire                wbu_io_rf_write_en;
  wire                xbar_io_ifuAXI4_ar_ready;
  wire                xbar_io_ifuAXI4_r_valid;
  wire       [31:0]   xbar_io_ifuAXI4_r_payload_data;
  wire       [3:0]    xbar_io_ifuAXI4_r_payload_id;
  wire       [1:0]    xbar_io_ifuAXI4_r_payload_resp;
  wire                xbar_io_ifuAXI4_r_payload_last;
  wire                xbar_io_lsuAXI4_ar_ready;
  wire                xbar_io_lsuAXI4_aw_ready;
  wire                xbar_io_lsuAXI4_w_ready;
  wire                xbar_io_lsuAXI4_r_valid;
  wire       [31:0]   xbar_io_lsuAXI4_r_payload_data;
  wire       [3:0]    xbar_io_lsuAXI4_r_payload_id;
  wire       [1:0]    xbar_io_lsuAXI4_r_payload_resp;
  wire                xbar_io_lsuAXI4_r_payload_last;
  wire                xbar_io_lsuAXI4_b_valid;
  wire       [3:0]    xbar_io_lsuAXI4_b_payload_id;
  wire       [1:0]    xbar_io_lsuAXI4_b_payload_resp;
  wire                xbar_io_clintAxi_ar_valid;
  wire       [31:0]   xbar_io_clintAxi_ar_payload_addr;
  wire       [3:0]    xbar_io_clintAxi_ar_payload_id;
  wire       [7:0]    xbar_io_clintAxi_ar_payload_len;
  wire       [2:0]    xbar_io_clintAxi_ar_payload_size;
  wire       [1:0]    xbar_io_clintAxi_ar_payload_burst;
  wire                xbar_io_clintAxi_aw_valid;
  wire       [31:0]   xbar_io_clintAxi_aw_payload_addr;
  wire       [3:0]    xbar_io_clintAxi_aw_payload_id;
  wire       [7:0]    xbar_io_clintAxi_aw_payload_len;
  wire       [2:0]    xbar_io_clintAxi_aw_payload_size;
  wire       [1:0]    xbar_io_clintAxi_aw_payload_burst;
  wire                xbar_io_clintAxi_w_valid;
  wire       [31:0]   xbar_io_clintAxi_w_payload_data;
  wire       [3:0]    xbar_io_clintAxi_w_payload_strb;
  wire                xbar_io_clintAxi_w_payload_last;
  wire                xbar_io_clintAxi_r_ready;
  wire                xbar_io_clintAxi_b_ready;
  wire                xbar_io_externalAxi_ar_valid;
  wire       [31:0]   xbar_io_externalAxi_ar_payload_addr;
  wire       [3:0]    xbar_io_externalAxi_ar_payload_id;
  wire       [7:0]    xbar_io_externalAxi_ar_payload_len;
  wire       [2:0]    xbar_io_externalAxi_ar_payload_size;
  wire       [1:0]    xbar_io_externalAxi_ar_payload_burst;
  wire                xbar_io_externalAxi_aw_valid;
  wire       [31:0]   xbar_io_externalAxi_aw_payload_addr;
  wire       [3:0]    xbar_io_externalAxi_aw_payload_id;
  wire       [7:0]    xbar_io_externalAxi_aw_payload_len;
  wire       [2:0]    xbar_io_externalAxi_aw_payload_size;
  wire       [1:0]    xbar_io_externalAxi_aw_payload_burst;
  wire                xbar_io_externalAxi_w_valid;
  wire       [31:0]   xbar_io_externalAxi_w_payload_data;
  wire       [3:0]    xbar_io_externalAxi_w_payload_strb;
  wire                xbar_io_externalAxi_w_payload_last;
  wire                xbar_io_externalAxi_r_ready;
  wire                xbar_io_externalAxi_b_ready;
  wire                clint_io_clintAxi_ar_ready;
  wire                clint_io_clintAxi_aw_ready;
  wire                clint_io_clintAxi_w_ready;
  wire                clint_io_clintAxi_r_valid;
  wire       [31:0]   clint_io_clintAxi_r_payload_data;
  wire       [3:0]    clint_io_clintAxi_r_payload_id;
  wire       [1:0]    clint_io_clintAxi_r_payload_resp;
  wire                clint_io_clintAxi_r_payload_last;
  wire                clint_io_clintAxi_b_valid;
  wire       [3:0]    clint_io_clintAxi_b_payload_id;
  wire       [1:0]    clint_io_clintAxi_b_payload_resp;
  wire                io_output_fire;
  reg        [31:0]   io_output_payload_regNextWhen_pc;
  reg        [31:0]   io_output_payload_regNextWhen_instr;
  reg                 _zz_io_output_ready;
  wire                io_output_fire_1;
  reg        [31:0]   io_output_payload_regNextWhen_pc_1;
  reg                 io_output_payload_regNextWhen_ctrl_rf_si_mem2reg;
  reg                 io_output_payload_regNextWhen_ctrl_rf_si_csr2reg;
  reg                 io_output_payload_regNextWhen_ctrl_rf_si_regWr;
  reg        [4:0]    io_output_payload_regNextWhen_ctrl_rf_si_rf_write_addr;
  reg                 io_output_payload_regNextWhen_ctrl_alu_si_alu_asrc;
  reg        [1:0]    io_output_payload_regNextWhen_ctrl_alu_si_alu_bsrc;
  reg        [3:0]    io_output_payload_regNextWhen_ctrl_alu_si_alu_ctr;
  reg        [2:0]    io_output_payload_regNextWhen_ctrl_alu_si_branch;
  reg                 io_output_payload_regNextWhen_ctrl_mem_si_memWr;
  reg        [2:0]    io_output_payload_regNextWhen_ctrl_mem_si_memOp;
  reg        [2:0]    io_output_payload_regNextWhen_ctrl_csr_si_csr_cmd;
  reg                 io_output_payload_regNextWhen_ctrl_csr_si_trap_enter;
  reg                 io_output_payload_regNextWhen_ctrl_csr_si_trap_exit;
  reg        [31:0]   io_output_payload_regNextWhen_imm;
  reg        [31:0]   io_output_payload_regNextWhen_rfReadData1;
  reg        [31:0]   io_output_payload_regNextWhen_rfReadData2;
  reg                 _zz_io_output_ready_1;
  wire                io_output_fire_2;
  reg        [31:0]   io_output_payload_regNextWhen_pc_2;
  reg        [31:0]   io_output_payload_regNextWhen_pc_next;
  reg                 io_output_payload_regNextWhen_rf_ctrl_mem2reg;
  reg                 io_output_payload_regNextWhen_rf_ctrl_csr2reg;
  reg                 io_output_payload_regNextWhen_rf_ctrl_regWr;
  reg        [4:0]    io_output_payload_regNextWhen_rf_ctrl_rf_write_addr;
  reg                 io_output_payload_regNextWhen_mem_ctrl_memWr;
  reg        [2:0]    io_output_payload_regNextWhen_mem_ctrl_memOp;
  reg        [2:0]    io_output_payload_regNextWhen_csr_ctrl_csr_cmd;
  reg                 io_output_payload_regNextWhen_csr_ctrl_trap_enter;
  reg                 io_output_payload_regNextWhen_csr_ctrl_trap_exit;
  reg        [11:0]   io_output_payload_regNextWhen_imm_1;
  reg        [31:0]   io_output_payload_regNextWhen_rfReadData1_1;
  reg        [31:0]   io_output_payload_regNextWhen_rfReadData2_1;
  reg        [31:0]   io_output_payload_regNextWhen_aluResult;
  reg                 _zz_io_output_ready_2;
  wire                io_output_fire_3;

  ysyx_23060082_RegFile regFile (
    .io_readAddr1 (idu_io_rfReadAddr1[4:0]   ), //i
    .io_readAddr2 (idu_io_rfReadAddr2[4:0]   ), //i
    .io_writeAddr (wbu_io_rf_write_addr[4:0] ), //i
    .io_writeData (wbu_io_rf_write_data[31:0]), //i
    .io_writeEn   (wbu_io_rf_write_en        ), //i
    .io_readData1 (regFile_io_readData1[31:0]), //o
    .io_readData2 (regFile_io_readData2[31:0]), //o
    .clock        (clock                     ), //i
    .reset        (reset                     )  //i
  );
  ysyx_23060082_IFU ifu (
    .io_input_valid           (wbu_io_output_valid                 ), //i
    .io_input_ready           (ifu_io_input_ready                  ), //o
    .io_input_payload_pc_next (wbu_io_output_payload_pc_next[31:0] ), //i
    .io_output_valid          (ifu_io_output_valid                 ), //o
    .io_output_ready          (ifu_io_output_ready                 ), //i
    .io_output_payload_pc     (ifu_io_output_payload_pc[31:0]      ), //o
    .io_output_payload_instr  (ifu_io_output_payload_instr[31:0]   ), //o
    .io_axi4_ar_valid         (ifu_io_axi4_ar_valid                ), //o
    .io_axi4_ar_ready         (xbar_io_ifuAXI4_ar_ready            ), //i
    .io_axi4_ar_payload_addr  (ifu_io_axi4_ar_payload_addr[31:0]   ), //o
    .io_axi4_ar_payload_id    (ifu_io_axi4_ar_payload_id[3:0]      ), //o
    .io_axi4_ar_payload_len   (ifu_io_axi4_ar_payload_len[7:0]     ), //o
    .io_axi4_ar_payload_size  (ifu_io_axi4_ar_payload_size[2:0]    ), //o
    .io_axi4_ar_payload_burst (ifu_io_axi4_ar_payload_burst[1:0]   ), //o
    .io_axi4_r_valid          (xbar_io_ifuAXI4_r_valid             ), //i
    .io_axi4_r_ready          (ifu_io_axi4_r_ready                 ), //o
    .io_axi4_r_payload_data   (xbar_io_ifuAXI4_r_payload_data[31:0]), //i
    .io_axi4_r_payload_id     (xbar_io_ifuAXI4_r_payload_id[3:0]   ), //i
    .io_axi4_r_payload_resp   (xbar_io_ifuAXI4_r_payload_resp[1:0] ), //i
    .io_axi4_r_payload_last   (xbar_io_ifuAXI4_r_payload_last      ), //i
    .clock                    (clock                               ), //i
    .reset                    (reset                               )  //i
  );
  ysyx_23060082_IDU idu (
    .io_input_valid                             (_zz_io_output_ready                                ), //i
    .io_input_payload_pc                        (io_output_payload_regNextWhen_pc[31:0]             ), //i
    .io_input_payload_instr                     (io_output_payload_regNextWhen_instr[31:0]          ), //i
    .io_output_valid                            (idu_io_output_valid                                ), //o
    .io_output_ready                            (idu_io_output_ready                                ), //i
    .io_output_payload_pc                       (idu_io_output_payload_pc[31:0]                     ), //o
    .io_output_payload_ctrl_rf_si_mem2reg       (idu_io_output_payload_ctrl_rf_si_mem2reg           ), //o
    .io_output_payload_ctrl_rf_si_csr2reg       (idu_io_output_payload_ctrl_rf_si_csr2reg           ), //o
    .io_output_payload_ctrl_rf_si_regWr         (idu_io_output_payload_ctrl_rf_si_regWr             ), //o
    .io_output_payload_ctrl_rf_si_rf_write_addr (idu_io_output_payload_ctrl_rf_si_rf_write_addr[4:0]), //o
    .io_output_payload_ctrl_alu_si_alu_asrc     (idu_io_output_payload_ctrl_alu_si_alu_asrc         ), //o
    .io_output_payload_ctrl_alu_si_alu_bsrc     (idu_io_output_payload_ctrl_alu_si_alu_bsrc[1:0]    ), //o
    .io_output_payload_ctrl_alu_si_alu_ctr      (idu_io_output_payload_ctrl_alu_si_alu_ctr[3:0]     ), //o
    .io_output_payload_ctrl_alu_si_branch       (idu_io_output_payload_ctrl_alu_si_branch[2:0]      ), //o
    .io_output_payload_ctrl_mem_si_memWr        (idu_io_output_payload_ctrl_mem_si_memWr            ), //o
    .io_output_payload_ctrl_mem_si_memOp        (idu_io_output_payload_ctrl_mem_si_memOp[2:0]       ), //o
    .io_output_payload_ctrl_csr_si_csr_cmd      (idu_io_output_payload_ctrl_csr_si_csr_cmd[2:0]     ), //o
    .io_output_payload_ctrl_csr_si_trap_enter   (idu_io_output_payload_ctrl_csr_si_trap_enter       ), //o
    .io_output_payload_ctrl_csr_si_trap_exit    (idu_io_output_payload_ctrl_csr_si_trap_exit        ), //o
    .io_output_payload_imm                      (idu_io_output_payload_imm[31:0]                    ), //o
    .io_output_payload_rfReadData1              (idu_io_output_payload_rfReadData1[31:0]            ), //o
    .io_output_payload_rfReadData2              (idu_io_output_payload_rfReadData2[31:0]            ), //o
    .io_rfReadAddr1                             (idu_io_rfReadAddr1[4:0]                            ), //o
    .io_rfReadAddr2                             (idu_io_rfReadAddr2[4:0]                            ), //o
    .io_rfReadData1                             (regFile_io_readData1[31:0]                         ), //i
    .io_rfReadData2                             (regFile_io_readData2[31:0]                         )  //i
  );
  ysyx_23060082_EXU exu (
    .io_input_valid                            (_zz_io_output_ready_1                                      ), //i
    .io_input_payload_pc                       (io_output_payload_regNextWhen_pc_1[31:0]                   ), //i
    .io_input_payload_ctrl_rf_si_mem2reg       (io_output_payload_regNextWhen_ctrl_rf_si_mem2reg           ), //i
    .io_input_payload_ctrl_rf_si_csr2reg       (io_output_payload_regNextWhen_ctrl_rf_si_csr2reg           ), //i
    .io_input_payload_ctrl_rf_si_regWr         (io_output_payload_regNextWhen_ctrl_rf_si_regWr             ), //i
    .io_input_payload_ctrl_rf_si_rf_write_addr (io_output_payload_regNextWhen_ctrl_rf_si_rf_write_addr[4:0]), //i
    .io_input_payload_ctrl_alu_si_alu_asrc     (io_output_payload_regNextWhen_ctrl_alu_si_alu_asrc         ), //i
    .io_input_payload_ctrl_alu_si_alu_bsrc     (io_output_payload_regNextWhen_ctrl_alu_si_alu_bsrc[1:0]    ), //i
    .io_input_payload_ctrl_alu_si_alu_ctr      (io_output_payload_regNextWhen_ctrl_alu_si_alu_ctr[3:0]     ), //i
    .io_input_payload_ctrl_alu_si_branch       (io_output_payload_regNextWhen_ctrl_alu_si_branch[2:0]      ), //i
    .io_input_payload_ctrl_mem_si_memWr        (io_output_payload_regNextWhen_ctrl_mem_si_memWr            ), //i
    .io_input_payload_ctrl_mem_si_memOp        (io_output_payload_regNextWhen_ctrl_mem_si_memOp[2:0]       ), //i
    .io_input_payload_ctrl_csr_si_csr_cmd      (io_output_payload_regNextWhen_ctrl_csr_si_csr_cmd[2:0]     ), //i
    .io_input_payload_ctrl_csr_si_trap_enter   (io_output_payload_regNextWhen_ctrl_csr_si_trap_enter       ), //i
    .io_input_payload_ctrl_csr_si_trap_exit    (io_output_payload_regNextWhen_ctrl_csr_si_trap_exit        ), //i
    .io_input_payload_imm                      (io_output_payload_regNextWhen_imm[31:0]                    ), //i
    .io_input_payload_rfReadData1              (io_output_payload_regNextWhen_rfReadData1[31:0]            ), //i
    .io_input_payload_rfReadData2              (io_output_payload_regNextWhen_rfReadData2[31:0]            ), //i
    .io_output_valid                           (exu_io_output_valid                                        ), //o
    .io_output_ready                           (exu_io_output_ready                                        ), //i
    .io_output_payload_pc                      (exu_io_output_payload_pc[31:0]                             ), //o
    .io_output_payload_pc_next                 (exu_io_output_payload_pc_next[31:0]                        ), //o
    .io_output_payload_rf_ctrl_mem2reg         (exu_io_output_payload_rf_ctrl_mem2reg                      ), //o
    .io_output_payload_rf_ctrl_csr2reg         (exu_io_output_payload_rf_ctrl_csr2reg                      ), //o
    .io_output_payload_rf_ctrl_regWr           (exu_io_output_payload_rf_ctrl_regWr                        ), //o
    .io_output_payload_rf_ctrl_rf_write_addr   (exu_io_output_payload_rf_ctrl_rf_write_addr[4:0]           ), //o
    .io_output_payload_mem_ctrl_memWr          (exu_io_output_payload_mem_ctrl_memWr                       ), //o
    .io_output_payload_mem_ctrl_memOp          (exu_io_output_payload_mem_ctrl_memOp[2:0]                  ), //o
    .io_output_payload_csr_ctrl_csr_cmd        (exu_io_output_payload_csr_ctrl_csr_cmd[2:0]                ), //o
    .io_output_payload_csr_ctrl_trap_enter     (exu_io_output_payload_csr_ctrl_trap_enter                  ), //o
    .io_output_payload_csr_ctrl_trap_exit      (exu_io_output_payload_csr_ctrl_trap_exit                   ), //o
    .io_output_payload_imm                     (exu_io_output_payload_imm[11:0]                            ), //o
    .io_output_payload_rfReadData1             (exu_io_output_payload_rfReadData1[31:0]                    ), //o
    .io_output_payload_rfReadData2             (exu_io_output_payload_rfReadData2[31:0]                    ), //o
    .io_output_payload_aluResult               (exu_io_output_payload_aluResult[31:0]                      )  //o
  );
  ysyx_23060082_LSU lsu (
    .io_input_valid                          (_zz_io_output_ready_2                                   ), //i
    .io_input_payload_pc                     (io_output_payload_regNextWhen_pc_2[31:0]                ), //i
    .io_input_payload_pc_next                (io_output_payload_regNextWhen_pc_next[31:0]             ), //i
    .io_input_payload_rf_ctrl_mem2reg        (io_output_payload_regNextWhen_rf_ctrl_mem2reg           ), //i
    .io_input_payload_rf_ctrl_csr2reg        (io_output_payload_regNextWhen_rf_ctrl_csr2reg           ), //i
    .io_input_payload_rf_ctrl_regWr          (io_output_payload_regNextWhen_rf_ctrl_regWr             ), //i
    .io_input_payload_rf_ctrl_rf_write_addr  (io_output_payload_regNextWhen_rf_ctrl_rf_write_addr[4:0]), //i
    .io_input_payload_mem_ctrl_memWr         (io_output_payload_regNextWhen_mem_ctrl_memWr            ), //i
    .io_input_payload_mem_ctrl_memOp         (io_output_payload_regNextWhen_mem_ctrl_memOp[2:0]       ), //i
    .io_input_payload_csr_ctrl_csr_cmd       (io_output_payload_regNextWhen_csr_ctrl_csr_cmd[2:0]     ), //i
    .io_input_payload_csr_ctrl_trap_enter    (io_output_payload_regNextWhen_csr_ctrl_trap_enter       ), //i
    .io_input_payload_csr_ctrl_trap_exit     (io_output_payload_regNextWhen_csr_ctrl_trap_exit        ), //i
    .io_input_payload_imm                    (io_output_payload_regNextWhen_imm_1[11:0]               ), //i
    .io_input_payload_rfReadData1            (io_output_payload_regNextWhen_rfReadData1_1[31:0]       ), //i
    .io_input_payload_rfReadData2            (io_output_payload_regNextWhen_rfReadData2_1[31:0]       ), //i
    .io_input_payload_aluResult              (io_output_payload_regNextWhen_aluResult[31:0]           ), //i
    .io_output_valid                         (lsu_io_output_valid                                     ), //o
    .io_output_ready                         (wbu_io_input_ready                                      ), //i
    .io_output_payload_pc                    (lsu_io_output_payload_pc[31:0]                          ), //o
    .io_output_payload_pc_next               (lsu_io_output_payload_pc_next[31:0]                     ), //o
    .io_output_payload_mem_data_out          (lsu_io_output_payload_mem_data_out[31:0]                ), //o
    .io_output_payload_alu_data_out          (lsu_io_output_payload_alu_data_out[31:0]                ), //o
    .io_output_payload_rf_ctrl_mem2reg       (lsu_io_output_payload_rf_ctrl_mem2reg                   ), //o
    .io_output_payload_rf_ctrl_csr2reg       (lsu_io_output_payload_rf_ctrl_csr2reg                   ), //o
    .io_output_payload_rf_ctrl_regWr         (lsu_io_output_payload_rf_ctrl_regWr                     ), //o
    .io_output_payload_rf_ctrl_rf_write_addr (lsu_io_output_payload_rf_ctrl_rf_write_addr[4:0]        ), //o
    .io_axi4_aw_valid                        (lsu_io_axi4_aw_valid                                    ), //o
    .io_axi4_aw_ready                        (xbar_io_lsuAXI4_aw_ready                                ), //i
    .io_axi4_aw_payload_addr                 (lsu_io_axi4_aw_payload_addr[31:0]                       ), //o
    .io_axi4_aw_payload_id                   (lsu_io_axi4_aw_payload_id[3:0]                          ), //o
    .io_axi4_aw_payload_len                  (lsu_io_axi4_aw_payload_len[7:0]                         ), //o
    .io_axi4_aw_payload_size                 (lsu_io_axi4_aw_payload_size[2:0]                        ), //o
    .io_axi4_aw_payload_burst                (lsu_io_axi4_aw_payload_burst[1:0]                       ), //o
    .io_axi4_w_valid                         (lsu_io_axi4_w_valid                                     ), //o
    .io_axi4_w_ready                         (xbar_io_lsuAXI4_w_ready                                 ), //i
    .io_axi4_w_payload_data                  (lsu_io_axi4_w_payload_data[31:0]                        ), //o
    .io_axi4_w_payload_strb                  (lsu_io_axi4_w_payload_strb[3:0]                         ), //o
    .io_axi4_w_payload_last                  (lsu_io_axi4_w_payload_last                              ), //o
    .io_axi4_b_valid                         (xbar_io_lsuAXI4_b_valid                                 ), //i
    .io_axi4_b_ready                         (lsu_io_axi4_b_ready                                     ), //o
    .io_axi4_b_payload_id                    (xbar_io_lsuAXI4_b_payload_id[3:0]                       ), //i
    .io_axi4_b_payload_resp                  (xbar_io_lsuAXI4_b_payload_resp[1:0]                     ), //i
    .io_axi4_ar_valid                        (lsu_io_axi4_ar_valid                                    ), //o
    .io_axi4_ar_ready                        (xbar_io_lsuAXI4_ar_ready                                ), //i
    .io_axi4_ar_payload_addr                 (lsu_io_axi4_ar_payload_addr[31:0]                       ), //o
    .io_axi4_ar_payload_id                   (lsu_io_axi4_ar_payload_id[3:0]                          ), //o
    .io_axi4_ar_payload_len                  (lsu_io_axi4_ar_payload_len[7:0]                         ), //o
    .io_axi4_ar_payload_size                 (lsu_io_axi4_ar_payload_size[2:0]                        ), //o
    .io_axi4_ar_payload_burst                (lsu_io_axi4_ar_payload_burst[1:0]                       ), //o
    .io_axi4_r_valid                         (xbar_io_lsuAXI4_r_valid                                 ), //i
    .io_axi4_r_ready                         (lsu_io_axi4_r_ready                                     ), //o
    .io_axi4_r_payload_data                  (xbar_io_lsuAXI4_r_payload_data[31:0]                    ), //i
    .io_axi4_r_payload_id                    (xbar_io_lsuAXI4_r_payload_id[3:0]                       ), //i
    .io_axi4_r_payload_resp                  (xbar_io_lsuAXI4_r_payload_resp[1:0]                     ), //i
    .io_axi4_r_payload_last                  (xbar_io_lsuAXI4_r_payload_last                          ), //i
    .clock                                   (clock                                                   ), //i
    .reset                                   (reset                                                   )  //i
  );
  ysyx_23060082_WBU wbu (
    .io_input_valid                         (lsu_io_output_valid                             ), //i
    .io_input_ready                         (wbu_io_input_ready                              ), //o
    .io_input_payload_pc                    (lsu_io_output_payload_pc[31:0]                  ), //i
    .io_input_payload_pc_next               (lsu_io_output_payload_pc_next[31:0]             ), //i
    .io_input_payload_mem_data_out          (lsu_io_output_payload_mem_data_out[31:0]        ), //i
    .io_input_payload_alu_data_out          (lsu_io_output_payload_alu_data_out[31:0]        ), //i
    .io_input_payload_rf_ctrl_mem2reg       (lsu_io_output_payload_rf_ctrl_mem2reg           ), //i
    .io_input_payload_rf_ctrl_csr2reg       (lsu_io_output_payload_rf_ctrl_csr2reg           ), //i
    .io_input_payload_rf_ctrl_regWr         (lsu_io_output_payload_rf_ctrl_regWr             ), //i
    .io_input_payload_rf_ctrl_rf_write_addr (lsu_io_output_payload_rf_ctrl_rf_write_addr[4:0]), //i
    .io_output_valid                        (wbu_io_output_valid                             ), //o
    .io_output_ready                        (ifu_io_input_ready                              ), //i
    .io_output_payload_pc_next              (wbu_io_output_payload_pc_next[31:0]             ), //o
    .io_rf_write_addr                       (wbu_io_rf_write_addr[4:0]                       ), //o
    .io_rf_write_data                       (wbu_io_rf_write_data[31:0]                      ), //o
    .io_rf_write_en                         (wbu_io_rf_write_en                              ), //o
    .clock                                  (clock                                           ), //i
    .reset                                  (reset                                           )  //i
  );
  ysyx_23060082_AXI4Xbar xbar (
    .io_ifuAXI4_ar_valid             (ifu_io_axi4_ar_valid                     ), //i
    .io_ifuAXI4_ar_ready             (xbar_io_ifuAXI4_ar_ready                 ), //o
    .io_ifuAXI4_ar_payload_addr      (ifu_io_axi4_ar_payload_addr[31:0]        ), //i
    .io_ifuAXI4_ar_payload_id        (ifu_io_axi4_ar_payload_id[3:0]           ), //i
    .io_ifuAXI4_ar_payload_len       (ifu_io_axi4_ar_payload_len[7:0]          ), //i
    .io_ifuAXI4_ar_payload_size      (ifu_io_axi4_ar_payload_size[2:0]         ), //i
    .io_ifuAXI4_ar_payload_burst     (ifu_io_axi4_ar_payload_burst[1:0]        ), //i
    .io_ifuAXI4_r_valid              (xbar_io_ifuAXI4_r_valid                  ), //o
    .io_ifuAXI4_r_ready              (ifu_io_axi4_r_ready                      ), //i
    .io_ifuAXI4_r_payload_data       (xbar_io_ifuAXI4_r_payload_data[31:0]     ), //o
    .io_ifuAXI4_r_payload_id         (xbar_io_ifuAXI4_r_payload_id[3:0]        ), //o
    .io_ifuAXI4_r_payload_resp       (xbar_io_ifuAXI4_r_payload_resp[1:0]      ), //o
    .io_ifuAXI4_r_payload_last       (xbar_io_ifuAXI4_r_payload_last           ), //o
    .io_ifuAXI4Req                   (                                         ), //i
    .io_lsuAXI4_aw_valid             (lsu_io_axi4_aw_valid                     ), //i
    .io_lsuAXI4_aw_ready             (xbar_io_lsuAXI4_aw_ready                 ), //o
    .io_lsuAXI4_aw_payload_addr      (lsu_io_axi4_aw_payload_addr[31:0]        ), //i
    .io_lsuAXI4_aw_payload_id        (lsu_io_axi4_aw_payload_id[3:0]           ), //i
    .io_lsuAXI4_aw_payload_len       (lsu_io_axi4_aw_payload_len[7:0]          ), //i
    .io_lsuAXI4_aw_payload_size      (lsu_io_axi4_aw_payload_size[2:0]         ), //i
    .io_lsuAXI4_aw_payload_burst     (lsu_io_axi4_aw_payload_burst[1:0]        ), //i
    .io_lsuAXI4_w_valid              (lsu_io_axi4_w_valid                      ), //i
    .io_lsuAXI4_w_ready              (xbar_io_lsuAXI4_w_ready                  ), //o
    .io_lsuAXI4_w_payload_data       (lsu_io_axi4_w_payload_data[31:0]         ), //i
    .io_lsuAXI4_w_payload_strb       (lsu_io_axi4_w_payload_strb[3:0]          ), //i
    .io_lsuAXI4_w_payload_last       (lsu_io_axi4_w_payload_last               ), //i
    .io_lsuAXI4_b_valid              (xbar_io_lsuAXI4_b_valid                  ), //o
    .io_lsuAXI4_b_ready              (lsu_io_axi4_b_ready                      ), //i
    .io_lsuAXI4_b_payload_id         (xbar_io_lsuAXI4_b_payload_id[3:0]        ), //o
    .io_lsuAXI4_b_payload_resp       (xbar_io_lsuAXI4_b_payload_resp[1:0]      ), //o
    .io_lsuAXI4_ar_valid             (lsu_io_axi4_ar_valid                     ), //i
    .io_lsuAXI4_ar_ready             (xbar_io_lsuAXI4_ar_ready                 ), //o
    .io_lsuAXI4_ar_payload_addr      (lsu_io_axi4_ar_payload_addr[31:0]        ), //i
    .io_lsuAXI4_ar_payload_id        (lsu_io_axi4_ar_payload_id[3:0]           ), //i
    .io_lsuAXI4_ar_payload_len       (lsu_io_axi4_ar_payload_len[7:0]          ), //i
    .io_lsuAXI4_ar_payload_size      (lsu_io_axi4_ar_payload_size[2:0]         ), //i
    .io_lsuAXI4_ar_payload_burst     (lsu_io_axi4_ar_payload_burst[1:0]        ), //i
    .io_lsuAXI4_r_valid              (xbar_io_lsuAXI4_r_valid                  ), //o
    .io_lsuAXI4_r_ready              (lsu_io_axi4_r_ready                      ), //i
    .io_lsuAXI4_r_payload_data       (xbar_io_lsuAXI4_r_payload_data[31:0]     ), //o
    .io_lsuAXI4_r_payload_id         (xbar_io_lsuAXI4_r_payload_id[3:0]        ), //o
    .io_lsuAXI4_r_payload_resp       (xbar_io_lsuAXI4_r_payload_resp[1:0]      ), //o
    .io_lsuAXI4_r_payload_last       (xbar_io_lsuAXI4_r_payload_last           ), //o
    .io_lsuAXI4Req                   (                                         ), //i
    .io_clintAxi_aw_valid            (xbar_io_clintAxi_aw_valid                ), //o
    .io_clintAxi_aw_ready            (clint_io_clintAxi_aw_ready               ), //i
    .io_clintAxi_aw_payload_addr     (xbar_io_clintAxi_aw_payload_addr[31:0]   ), //o
    .io_clintAxi_aw_payload_id       (xbar_io_clintAxi_aw_payload_id[3:0]      ), //o
    .io_clintAxi_aw_payload_len      (xbar_io_clintAxi_aw_payload_len[7:0]     ), //o
    .io_clintAxi_aw_payload_size     (xbar_io_clintAxi_aw_payload_size[2:0]    ), //o
    .io_clintAxi_aw_payload_burst    (xbar_io_clintAxi_aw_payload_burst[1:0]   ), //o
    .io_clintAxi_w_valid             (xbar_io_clintAxi_w_valid                 ), //o
    .io_clintAxi_w_ready             (clint_io_clintAxi_w_ready                ), //i
    .io_clintAxi_w_payload_data      (xbar_io_clintAxi_w_payload_data[31:0]    ), //o
    .io_clintAxi_w_payload_strb      (xbar_io_clintAxi_w_payload_strb[3:0]     ), //o
    .io_clintAxi_w_payload_last      (xbar_io_clintAxi_w_payload_last          ), //o
    .io_clintAxi_b_valid             (clint_io_clintAxi_b_valid                ), //i
    .io_clintAxi_b_ready             (xbar_io_clintAxi_b_ready                 ), //o
    .io_clintAxi_b_payload_id        (clint_io_clintAxi_b_payload_id[3:0]      ), //i
    .io_clintAxi_b_payload_resp      (clint_io_clintAxi_b_payload_resp[1:0]    ), //i
    .io_clintAxi_ar_valid            (xbar_io_clintAxi_ar_valid                ), //o
    .io_clintAxi_ar_ready            (clint_io_clintAxi_ar_ready               ), //i
    .io_clintAxi_ar_payload_addr     (xbar_io_clintAxi_ar_payload_addr[31:0]   ), //o
    .io_clintAxi_ar_payload_id       (xbar_io_clintAxi_ar_payload_id[3:0]      ), //o
    .io_clintAxi_ar_payload_len      (xbar_io_clintAxi_ar_payload_len[7:0]     ), //o
    .io_clintAxi_ar_payload_size     (xbar_io_clintAxi_ar_payload_size[2:0]    ), //o
    .io_clintAxi_ar_payload_burst    (xbar_io_clintAxi_ar_payload_burst[1:0]   ), //o
    .io_clintAxi_r_valid             (clint_io_clintAxi_r_valid                ), //i
    .io_clintAxi_r_ready             (xbar_io_clintAxi_r_ready                 ), //o
    .io_clintAxi_r_payload_data      (clint_io_clintAxi_r_payload_data[31:0]   ), //i
    .io_clintAxi_r_payload_id        (clint_io_clintAxi_r_payload_id[3:0]      ), //i
    .io_clintAxi_r_payload_resp      (clint_io_clintAxi_r_payload_resp[1:0]    ), //i
    .io_clintAxi_r_payload_last      (clint_io_clintAxi_r_payload_last         ), //i
    .io_externalAxi_aw_valid         (xbar_io_externalAxi_aw_valid             ), //o
    .io_externalAxi_aw_ready         (io_master_awready                        ), //i
    .io_externalAxi_aw_payload_addr  (xbar_io_externalAxi_aw_payload_addr[31:0]), //o
    .io_externalAxi_aw_payload_id    (xbar_io_externalAxi_aw_payload_id[3:0]   ), //o
    .io_externalAxi_aw_payload_len   (xbar_io_externalAxi_aw_payload_len[7:0]  ), //o
    .io_externalAxi_aw_payload_size  (xbar_io_externalAxi_aw_payload_size[2:0] ), //o
    .io_externalAxi_aw_payload_burst (xbar_io_externalAxi_aw_payload_burst[1:0]), //o
    .io_externalAxi_w_valid          (xbar_io_externalAxi_w_valid              ), //o
    .io_externalAxi_w_ready          (io_master_wready                         ), //i
    .io_externalAxi_w_payload_data   (xbar_io_externalAxi_w_payload_data[31:0] ), //o
    .io_externalAxi_w_payload_strb   (xbar_io_externalAxi_w_payload_strb[3:0]  ), //o
    .io_externalAxi_w_payload_last   (xbar_io_externalAxi_w_payload_last       ), //o
    .io_externalAxi_b_valid          (io_master_bvalid                         ), //i
    .io_externalAxi_b_ready          (xbar_io_externalAxi_b_ready              ), //o
    .io_externalAxi_b_payload_id     (io_master_bid[3:0]                       ), //i
    .io_externalAxi_b_payload_resp   (io_master_bresp[1:0]                     ), //i
    .io_externalAxi_ar_valid         (xbar_io_externalAxi_ar_valid             ), //o
    .io_externalAxi_ar_ready         (io_master_arready                        ), //i
    .io_externalAxi_ar_payload_addr  (xbar_io_externalAxi_ar_payload_addr[31:0]), //o
    .io_externalAxi_ar_payload_id    (xbar_io_externalAxi_ar_payload_id[3:0]   ), //o
    .io_externalAxi_ar_payload_len   (xbar_io_externalAxi_ar_payload_len[7:0]  ), //o
    .io_externalAxi_ar_payload_size  (xbar_io_externalAxi_ar_payload_size[2:0] ), //o
    .io_externalAxi_ar_payload_burst (xbar_io_externalAxi_ar_payload_burst[1:0]), //o
    .io_externalAxi_r_valid          (io_master_rvalid                         ), //i
    .io_externalAxi_r_ready          (xbar_io_externalAxi_r_ready              ), //o
    .io_externalAxi_r_payload_data   (io_master_rdata[31:0]                    ), //i
    .io_externalAxi_r_payload_id     (io_master_rid[3:0]                       ), //i
    .io_externalAxi_r_payload_resp   (io_master_rresp[1:0]                     ), //i
    .io_externalAxi_r_payload_last   (io_master_rlast                          ), //i
    .clock                           (clock                                    ), //i
    .reset                           (reset                                    )  //i
  );
  ysyx_23060082_Clint clint (
    .io_clintAxi_aw_valid         (xbar_io_clintAxi_aw_valid             ), //i
    .io_clintAxi_aw_ready         (clint_io_clintAxi_aw_ready            ), //o
    .io_clintAxi_aw_payload_addr  (xbar_io_clintAxi_aw_payload_addr[31:0]), //i
    .io_clintAxi_aw_payload_id    (xbar_io_clintAxi_aw_payload_id[3:0]   ), //i
    .io_clintAxi_aw_payload_len   (xbar_io_clintAxi_aw_payload_len[7:0]  ), //i
    .io_clintAxi_aw_payload_size  (xbar_io_clintAxi_aw_payload_size[2:0] ), //i
    .io_clintAxi_aw_payload_burst (xbar_io_clintAxi_aw_payload_burst[1:0]), //i
    .io_clintAxi_w_valid          (xbar_io_clintAxi_w_valid              ), //i
    .io_clintAxi_w_ready          (clint_io_clintAxi_w_ready             ), //o
    .io_clintAxi_w_payload_data   (xbar_io_clintAxi_w_payload_data[31:0] ), //i
    .io_clintAxi_w_payload_strb   (xbar_io_clintAxi_w_payload_strb[3:0]  ), //i
    .io_clintAxi_w_payload_last   (xbar_io_clintAxi_w_payload_last       ), //i
    .io_clintAxi_b_valid          (clint_io_clintAxi_b_valid             ), //o
    .io_clintAxi_b_ready          (xbar_io_clintAxi_b_ready              ), //i
    .io_clintAxi_b_payload_id     (clint_io_clintAxi_b_payload_id[3:0]   ), //o
    .io_clintAxi_b_payload_resp   (clint_io_clintAxi_b_payload_resp[1:0] ), //o
    .io_clintAxi_ar_valid         (xbar_io_clintAxi_ar_valid             ), //i
    .io_clintAxi_ar_ready         (clint_io_clintAxi_ar_ready            ), //o
    .io_clintAxi_ar_payload_addr  (xbar_io_clintAxi_ar_payload_addr[31:0]), //i
    .io_clintAxi_ar_payload_id    (xbar_io_clintAxi_ar_payload_id[3:0]   ), //i
    .io_clintAxi_ar_payload_len   (xbar_io_clintAxi_ar_payload_len[7:0]  ), //i
    .io_clintAxi_ar_payload_size  (xbar_io_clintAxi_ar_payload_size[2:0] ), //i
    .io_clintAxi_ar_payload_burst (xbar_io_clintAxi_ar_payload_burst[1:0]), //i
    .io_clintAxi_r_valid          (clint_io_clintAxi_r_valid             ), //o
    .io_clintAxi_r_ready          (xbar_io_clintAxi_r_ready              ), //i
    .io_clintAxi_r_payload_data   (clint_io_clintAxi_r_payload_data[31:0]), //o
    .io_clintAxi_r_payload_id     (clint_io_clintAxi_r_payload_id[3:0]   ), //o
    .io_clintAxi_r_payload_resp   (clint_io_clintAxi_r_payload_resp[1:0] ), //o
    .io_clintAxi_r_payload_last   (clint_io_clintAxi_r_payload_last      ), //o
    .clock                        (clock                                 ), //i
    .reset                        (reset                                 )  //i
  );
  assign io_slave_awready = 1'b0;
  assign io_slave_wready = 1'b0;
  assign io_slave_bvalid = 1'b0;
  assign io_slave_bid = 4'b0000;
  assign io_slave_bresp = 2'b00;
  assign io_slave_arready = 1'b0;
  assign io_slave_rvalid = 1'b0;
  assign io_slave_rdata = 32'h0;
  assign io_slave_rresp = 2'b00;
  assign io_slave_rlast = 1'b0;
  assign io_slave_rid = 4'b0000;
  assign io_output_fire = (ifu_io_output_valid && ifu_io_output_ready);
  assign io_output_fire_1 = (idu_io_output_valid && idu_io_output_ready);
  assign ifu_io_output_ready = ((! _zz_io_output_ready) || io_output_fire_1);
  assign io_output_fire_2 = (exu_io_output_valid && exu_io_output_ready);
  assign idu_io_output_ready = ((! _zz_io_output_ready_1) || io_output_fire_2);
  assign io_output_fire_3 = (lsu_io_output_valid && wbu_io_input_ready);
  assign exu_io_output_ready = ((! _zz_io_output_ready_2) || io_output_fire_3);
  assign io_master_awvalid = xbar_io_externalAxi_aw_valid;
  assign io_master_awaddr = xbar_io_externalAxi_aw_payload_addr;
  assign io_master_awid = xbar_io_externalAxi_aw_payload_id;
  assign io_master_awlen = xbar_io_externalAxi_aw_payload_len;
  assign io_master_awsize = xbar_io_externalAxi_aw_payload_size;
  assign io_master_awburst = xbar_io_externalAxi_aw_payload_burst;
  assign io_master_wvalid = xbar_io_externalAxi_w_valid;
  assign io_master_wdata = xbar_io_externalAxi_w_payload_data;
  assign io_master_wstrb = xbar_io_externalAxi_w_payload_strb;
  assign io_master_wlast = xbar_io_externalAxi_w_payload_last;
  assign io_master_bready = xbar_io_externalAxi_b_ready;
  assign io_master_arvalid = xbar_io_externalAxi_ar_valid;
  assign io_master_araddr = xbar_io_externalAxi_ar_payload_addr;
  assign io_master_arid = xbar_io_externalAxi_ar_payload_id;
  assign io_master_arlen = xbar_io_externalAxi_ar_payload_len;
  assign io_master_arsize = xbar_io_externalAxi_ar_payload_size;
  assign io_master_arburst = xbar_io_externalAxi_ar_payload_burst;
  assign io_master_rready = xbar_io_externalAxi_r_ready;
  always @(posedge clock) begin
    if(io_output_fire) begin
      io_output_payload_regNextWhen_pc <= ifu_io_output_payload_pc;
      io_output_payload_regNextWhen_instr <= ifu_io_output_payload_instr;
    end
    if(io_output_fire_1) begin
      io_output_payload_regNextWhen_pc_1 <= idu_io_output_payload_pc;
      io_output_payload_regNextWhen_ctrl_rf_si_mem2reg <= idu_io_output_payload_ctrl_rf_si_mem2reg;
      io_output_payload_regNextWhen_ctrl_rf_si_csr2reg <= idu_io_output_payload_ctrl_rf_si_csr2reg;
      io_output_payload_regNextWhen_ctrl_rf_si_regWr <= idu_io_output_payload_ctrl_rf_si_regWr;
      io_output_payload_regNextWhen_ctrl_rf_si_rf_write_addr <= idu_io_output_payload_ctrl_rf_si_rf_write_addr;
      io_output_payload_regNextWhen_ctrl_alu_si_alu_asrc <= idu_io_output_payload_ctrl_alu_si_alu_asrc;
      io_output_payload_regNextWhen_ctrl_alu_si_alu_bsrc <= idu_io_output_payload_ctrl_alu_si_alu_bsrc;
      io_output_payload_regNextWhen_ctrl_alu_si_alu_ctr <= idu_io_output_payload_ctrl_alu_si_alu_ctr;
      io_output_payload_regNextWhen_ctrl_alu_si_branch <= idu_io_output_payload_ctrl_alu_si_branch;
      io_output_payload_regNextWhen_ctrl_mem_si_memWr <= idu_io_output_payload_ctrl_mem_si_memWr;
      io_output_payload_regNextWhen_ctrl_mem_si_memOp <= idu_io_output_payload_ctrl_mem_si_memOp;
      io_output_payload_regNextWhen_ctrl_csr_si_csr_cmd <= idu_io_output_payload_ctrl_csr_si_csr_cmd;
      io_output_payload_regNextWhen_ctrl_csr_si_trap_enter <= idu_io_output_payload_ctrl_csr_si_trap_enter;
      io_output_payload_regNextWhen_ctrl_csr_si_trap_exit <= idu_io_output_payload_ctrl_csr_si_trap_exit;
      io_output_payload_regNextWhen_imm <= idu_io_output_payload_imm;
      io_output_payload_regNextWhen_rfReadData1 <= idu_io_output_payload_rfReadData1;
      io_output_payload_regNextWhen_rfReadData2 <= idu_io_output_payload_rfReadData2;
    end
    if(io_output_fire_2) begin
      io_output_payload_regNextWhen_pc_2 <= exu_io_output_payload_pc;
      io_output_payload_regNextWhen_pc_next <= exu_io_output_payload_pc_next;
      io_output_payload_regNextWhen_rf_ctrl_mem2reg <= exu_io_output_payload_rf_ctrl_mem2reg;
      io_output_payload_regNextWhen_rf_ctrl_csr2reg <= exu_io_output_payload_rf_ctrl_csr2reg;
      io_output_payload_regNextWhen_rf_ctrl_regWr <= exu_io_output_payload_rf_ctrl_regWr;
      io_output_payload_regNextWhen_rf_ctrl_rf_write_addr <= exu_io_output_payload_rf_ctrl_rf_write_addr;
      io_output_payload_regNextWhen_mem_ctrl_memWr <= exu_io_output_payload_mem_ctrl_memWr;
      io_output_payload_regNextWhen_mem_ctrl_memOp <= exu_io_output_payload_mem_ctrl_memOp;
      io_output_payload_regNextWhen_csr_ctrl_csr_cmd <= exu_io_output_payload_csr_ctrl_csr_cmd;
      io_output_payload_regNextWhen_csr_ctrl_trap_enter <= exu_io_output_payload_csr_ctrl_trap_enter;
      io_output_payload_regNextWhen_csr_ctrl_trap_exit <= exu_io_output_payload_csr_ctrl_trap_exit;
      io_output_payload_regNextWhen_imm_1 <= exu_io_output_payload_imm;
      io_output_payload_regNextWhen_rfReadData1_1 <= exu_io_output_payload_rfReadData1;
      io_output_payload_regNextWhen_rfReadData2_1 <= exu_io_output_payload_rfReadData2;
      io_output_payload_regNextWhen_aluResult <= exu_io_output_payload_aluResult;
    end
  end

  always @(posedge clock or posedge reset) begin
    if(reset) begin
      _zz_io_output_ready <= 1'b0;
      _zz_io_output_ready_1 <= 1'b0;
      _zz_io_output_ready_2 <= 1'b0;
    end else begin
      if(io_output_fire) begin
        _zz_io_output_ready <= 1'b1;
      end else begin
        if(io_output_fire_1) begin
          _zz_io_output_ready <= 1'b0;
        end else begin
          _zz_io_output_ready <= _zz_io_output_ready;
        end
      end
      if(io_output_fire_1) begin
        _zz_io_output_ready_1 <= 1'b1;
      end else begin
        if(io_output_fire_2) begin
          _zz_io_output_ready_1 <= 1'b0;
        end else begin
          _zz_io_output_ready_1 <= _zz_io_output_ready_1;
        end
      end
      if(io_output_fire_2) begin
        _zz_io_output_ready_2 <= 1'b1;
      end else begin
        if(io_output_fire_3) begin
          _zz_io_output_ready_2 <= 1'b0;
        end else begin
          _zz_io_output_ready_2 <= _zz_io_output_ready_2;
        end
      end
    end
  end


endmodule

module ysyx_23060082_Clint (
  input  wire          io_clintAxi_aw_valid,
  output wire          io_clintAxi_aw_ready,
  input  wire [31:0]   io_clintAxi_aw_payload_addr,
  input  wire [3:0]    io_clintAxi_aw_payload_id,
  input  wire [7:0]    io_clintAxi_aw_payload_len,
  input  wire [2:0]    io_clintAxi_aw_payload_size,
  input  wire [1:0]    io_clintAxi_aw_payload_burst,
  input  wire          io_clintAxi_w_valid,
  output wire          io_clintAxi_w_ready,
  input  wire [31:0]   io_clintAxi_w_payload_data,
  input  wire [3:0]    io_clintAxi_w_payload_strb,
  input  wire          io_clintAxi_w_payload_last,
  output reg           io_clintAxi_b_valid,
  input  wire          io_clintAxi_b_ready,
  output wire [3:0]    io_clintAxi_b_payload_id,
  output wire [1:0]    io_clintAxi_b_payload_resp,
  input  wire          io_clintAxi_ar_valid,
  output wire          io_clintAxi_ar_ready,
  input  wire [31:0]   io_clintAxi_ar_payload_addr,
  input  wire [3:0]    io_clintAxi_ar_payload_id,
  input  wire [7:0]    io_clintAxi_ar_payload_len,
  input  wire [2:0]    io_clintAxi_ar_payload_size,
  input  wire [1:0]    io_clintAxi_ar_payload_burst,
  output reg           io_clintAxi_r_valid,
  input  wire          io_clintAxi_r_ready,
  output reg  [31:0]   io_clintAxi_r_payload_data,
  output wire [3:0]    io_clintAxi_r_payload_id,
  output wire [1:0]    io_clintAxi_r_payload_resp,
  output wire          io_clintAxi_r_payload_last,
  input  wire          clock,
  input  wire          reset
);

  reg        [63:0]   timeCount;
  wire                io_clintAxi_ar_fire;
  wire                when_23060082_l137;
  reg        [31:0]   timeCountLow;
  wire                io_clintAxi_r_fire;
  reg        [31:0]   _zz_io_clintAxi_r_payload_data;
  wire                wAllValid;
  wire                io_clintAxi_b_fire;

  assign io_clintAxi_ar_fire = (io_clintAxi_ar_valid && io_clintAxi_ar_ready);
  assign when_23060082_l137 = (io_clintAxi_ar_fire && (io_clintAxi_ar_payload_addr == 32'h02000004));
  assign io_clintAxi_ar_ready = io_clintAxi_ar_valid;
  assign io_clintAxi_r_fire = (io_clintAxi_r_valid && io_clintAxi_r_ready);
  assign io_clintAxi_r_payload_last = io_clintAxi_r_valid;
  always @(*) begin
    case(io_clintAxi_ar_payload_addr)
      32'h02000004 : begin
        _zz_io_clintAxi_r_payload_data = timeCount[63 : 32];
      end
      32'h02000000 : begin
        _zz_io_clintAxi_r_payload_data = timeCountLow;
      end
      default : begin
        _zz_io_clintAxi_r_payload_data = 32'h0;
      end
    endcase
  end

  assign wAllValid = (io_clintAxi_aw_valid && io_clintAxi_w_valid);
  assign io_clintAxi_aw_ready = wAllValid;
  assign io_clintAxi_w_ready = wAllValid;
  assign io_clintAxi_b_fire = (io_clintAxi_b_valid && io_clintAxi_b_ready);
  always @(posedge clock or posedge reset) begin
    if(reset) begin
      timeCount <= 64'h0;
      timeCountLow <= 32'h0;
      io_clintAxi_r_valid <= 1'b0;
      io_clintAxi_b_valid <= 1'b0;
    end else begin
      if(when_23060082_l137) begin
        timeCountLow <= timeCount[31 : 0];
      end
      timeCount <= (timeCount + 64'h0000000000000001);
      if(io_clintAxi_ar_valid) begin
        io_clintAxi_r_valid <= 1'b1;
      end else begin
        if(io_clintAxi_r_fire) begin
          io_clintAxi_r_valid <= 1'b0;
        end else begin
          io_clintAxi_r_valid <= io_clintAxi_r_valid;
        end
      end
      if(wAllValid) begin
        io_clintAxi_b_valid <= 1'b1;
      end else begin
        if(io_clintAxi_b_fire) begin
          io_clintAxi_b_valid <= 1'b0;
        end else begin
          io_clintAxi_b_valid <= io_clintAxi_b_valid;
        end
      end
    end
  end

  always @(posedge clock) begin
    if(io_clintAxi_ar_fire) begin
      io_clintAxi_r_payload_data <= _zz_io_clintAxi_r_payload_data;
    end else begin
      io_clintAxi_r_payload_data <= io_clintAxi_r_payload_data;
    end
  end


endmodule

module ysyx_23060082_AXI4Xbar (
  input  wire          io_ifuAXI4_ar_valid,
  output wire          io_ifuAXI4_ar_ready,
  input  wire [31:0]   io_ifuAXI4_ar_payload_addr,
  input  wire [3:0]    io_ifuAXI4_ar_payload_id,
  input  wire [7:0]    io_ifuAXI4_ar_payload_len,
  input  wire [2:0]    io_ifuAXI4_ar_payload_size,
  input  wire [1:0]    io_ifuAXI4_ar_payload_burst,
  output wire          io_ifuAXI4_r_valid,
  input  wire          io_ifuAXI4_r_ready,
  output wire [31:0]   io_ifuAXI4_r_payload_data,
  output wire [3:0]    io_ifuAXI4_r_payload_id,
  output wire [1:0]    io_ifuAXI4_r_payload_resp,
  output wire          io_ifuAXI4_r_payload_last,
  input  wire          io_ifuAXI4Req,
  input  wire          io_lsuAXI4_aw_valid,
  output wire          io_lsuAXI4_aw_ready,
  input  wire [31:0]   io_lsuAXI4_aw_payload_addr,
  input  wire [3:0]    io_lsuAXI4_aw_payload_id,
  input  wire [7:0]    io_lsuAXI4_aw_payload_len,
  input  wire [2:0]    io_lsuAXI4_aw_payload_size,
  input  wire [1:0]    io_lsuAXI4_aw_payload_burst,
  input  wire          io_lsuAXI4_w_valid,
  output wire          io_lsuAXI4_w_ready,
  input  wire [31:0]   io_lsuAXI4_w_payload_data,
  input  wire [3:0]    io_lsuAXI4_w_payload_strb,
  input  wire          io_lsuAXI4_w_payload_last,
  output wire          io_lsuAXI4_b_valid,
  input  wire          io_lsuAXI4_b_ready,
  output wire [3:0]    io_lsuAXI4_b_payload_id,
  output wire [1:0]    io_lsuAXI4_b_payload_resp,
  input  wire          io_lsuAXI4_ar_valid,
  output wire          io_lsuAXI4_ar_ready,
  input  wire [31:0]   io_lsuAXI4_ar_payload_addr,
  input  wire [3:0]    io_lsuAXI4_ar_payload_id,
  input  wire [7:0]    io_lsuAXI4_ar_payload_len,
  input  wire [2:0]    io_lsuAXI4_ar_payload_size,
  input  wire [1:0]    io_lsuAXI4_ar_payload_burst,
  output wire          io_lsuAXI4_r_valid,
  input  wire          io_lsuAXI4_r_ready,
  output wire [31:0]   io_lsuAXI4_r_payload_data,
  output wire [3:0]    io_lsuAXI4_r_payload_id,
  output wire [1:0]    io_lsuAXI4_r_payload_resp,
  output wire          io_lsuAXI4_r_payload_last,
  input  wire          io_lsuAXI4Req,
  output wire          io_clintAxi_aw_valid,
  input  wire          io_clintAxi_aw_ready,
  output wire [31:0]   io_clintAxi_aw_payload_addr,
  output wire [3:0]    io_clintAxi_aw_payload_id,
  output wire [7:0]    io_clintAxi_aw_payload_len,
  output wire [2:0]    io_clintAxi_aw_payload_size,
  output wire [1:0]    io_clintAxi_aw_payload_burst,
  output wire          io_clintAxi_w_valid,
  input  wire          io_clintAxi_w_ready,
  output wire [31:0]   io_clintAxi_w_payload_data,
  output wire [3:0]    io_clintAxi_w_payload_strb,
  output wire          io_clintAxi_w_payload_last,
  input  wire          io_clintAxi_b_valid,
  output wire          io_clintAxi_b_ready,
  input  wire [3:0]    io_clintAxi_b_payload_id,
  input  wire [1:0]    io_clintAxi_b_payload_resp,
  output wire          io_clintAxi_ar_valid,
  input  wire          io_clintAxi_ar_ready,
  output wire [31:0]   io_clintAxi_ar_payload_addr,
  output wire [3:0]    io_clintAxi_ar_payload_id,
  output wire [7:0]    io_clintAxi_ar_payload_len,
  output wire [2:0]    io_clintAxi_ar_payload_size,
  output wire [1:0]    io_clintAxi_ar_payload_burst,
  input  wire          io_clintAxi_r_valid,
  output wire          io_clintAxi_r_ready,
  input  wire [31:0]   io_clintAxi_r_payload_data,
  input  wire [3:0]    io_clintAxi_r_payload_id,
  input  wire [1:0]    io_clintAxi_r_payload_resp,
  input  wire          io_clintAxi_r_payload_last,
  output wire          io_externalAxi_aw_valid,
  input  wire          io_externalAxi_aw_ready,
  output wire [31:0]   io_externalAxi_aw_payload_addr,
  output wire [3:0]    io_externalAxi_aw_payload_id,
  output wire [7:0]    io_externalAxi_aw_payload_len,
  output wire [2:0]    io_externalAxi_aw_payload_size,
  output wire [1:0]    io_externalAxi_aw_payload_burst,
  output wire          io_externalAxi_w_valid,
  input  wire          io_externalAxi_w_ready,
  output wire [31:0]   io_externalAxi_w_payload_data,
  output wire [3:0]    io_externalAxi_w_payload_strb,
  output wire          io_externalAxi_w_payload_last,
  input  wire          io_externalAxi_b_valid,
  output wire          io_externalAxi_b_ready,
  input  wire [3:0]    io_externalAxi_b_payload_id,
  input  wire [1:0]    io_externalAxi_b_payload_resp,
  output wire          io_externalAxi_ar_valid,
  input  wire          io_externalAxi_ar_ready,
  output wire [31:0]   io_externalAxi_ar_payload_addr,
  output wire [3:0]    io_externalAxi_ar_payload_id,
  output wire [7:0]    io_externalAxi_ar_payload_len,
  output wire [2:0]    io_externalAxi_ar_payload_size,
  output wire [1:0]    io_externalAxi_ar_payload_burst,
  input  wire          io_externalAxi_r_valid,
  output wire          io_externalAxi_r_ready,
  input  wire [31:0]   io_externalAxi_r_payload_data,
  input  wire [3:0]    io_externalAxi_r_payload_id,
  input  wire [1:0]    io_externalAxi_r_payload_resp,
  input  wire          io_externalAxi_r_payload_last,
  input  wire          clock,
  input  wire          reset
);
  localparam ArbiterState_Idle = 2'd0;
  localparam ArbiterState_IfuUsing = 2'd1;
  localparam ArbiterState_LsuUsing = 2'd2;
  localparam CrossState_Idle = 2'd0;
  localparam CrossState_Clint = 2'd1;
  localparam CrossState_External = 2'd2;

  wire                axi4Bus_aw_valid;
  wire                axi4Bus_aw_ready;
  wire       [31:0]   axi4Bus_aw_payload_addr;
  wire       [3:0]    axi4Bus_aw_payload_id;
  wire       [7:0]    axi4Bus_aw_payload_len;
  wire       [2:0]    axi4Bus_aw_payload_size;
  wire       [1:0]    axi4Bus_aw_payload_burst;
  wire                axi4Bus_w_valid;
  wire                axi4Bus_w_ready;
  wire       [31:0]   axi4Bus_w_payload_data;
  wire       [3:0]    axi4Bus_w_payload_strb;
  wire                axi4Bus_w_payload_last;
  wire                axi4Bus_b_valid;
  wire                axi4Bus_b_ready;
  wire       [3:0]    axi4Bus_b_payload_id;
  wire       [1:0]    axi4Bus_b_payload_resp;
  wire                axi4Bus_ar_valid;
  wire                axi4Bus_ar_ready;
  wire       [31:0]   axi4Bus_ar_payload_addr;
  wire       [3:0]    axi4Bus_ar_payload_id;
  wire       [7:0]    axi4Bus_ar_payload_len;
  wire       [2:0]    axi4Bus_ar_payload_size;
  wire       [1:0]    axi4Bus_ar_payload_burst;
  wire                axi4Bus_r_valid;
  wire                axi4Bus_r_ready;
  wire       [31:0]   axi4Bus_r_payload_data;
  wire       [3:0]    axi4Bus_r_payload_id;
  wire       [1:0]    axi4Bus_r_payload_resp;
  wire                axi4Bus_r_payload_last;
  reg        [1:0]    arbiterState_1;
  wire                io_ifuAXI4_r_fire;
  wire                io_lsuAXI4_r_fire;
  wire                _zz_axi4Bus_ar_payload_addr;
  reg        [1:0]    crossState_1;
  wire                when_Xbar_l102;
  wire                when_Xbar_l108;
  wire                axi4Bus_r_fire;
  wire                axi4Bus_b_fire;
  wire                when_Xbar_l118;
  wire                _zz_axi4Bus_r_payload_data;
  wire                _zz_axi4Bus_b_payload_id;
  `ifndef SYNTHESIS
  reg [63:0] arbiterState_1_string;
  reg [63:0] crossState_1_string;
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
    case(crossState_1)
      CrossState_Idle : crossState_1_string = "Idle    ";
      CrossState_Clint : crossState_1_string = "Clint   ";
      CrossState_External : crossState_1_string = "External";
      default : crossState_1_string = "????????";
    endcase
  end
  `endif

  assign io_ifuAXI4_r_fire = (io_ifuAXI4_r_valid && io_ifuAXI4_r_ready);
  assign io_lsuAXI4_r_fire = (io_lsuAXI4_r_valid && io_lsuAXI4_r_ready);
  assign _zz_axi4Bus_ar_payload_addr = (arbiterState_1 == ArbiterState_IfuUsing);
  assign axi4Bus_ar_payload_addr = (_zz_axi4Bus_ar_payload_addr ? io_ifuAXI4_ar_payload_addr : io_lsuAXI4_ar_payload_addr);
  assign axi4Bus_ar_payload_id = (_zz_axi4Bus_ar_payload_addr ? io_ifuAXI4_ar_payload_id : io_lsuAXI4_ar_payload_id);
  assign axi4Bus_ar_payload_len = (_zz_axi4Bus_ar_payload_addr ? io_ifuAXI4_ar_payload_len : io_lsuAXI4_ar_payload_len);
  assign axi4Bus_ar_payload_size = (_zz_axi4Bus_ar_payload_addr ? io_ifuAXI4_ar_payload_size : io_lsuAXI4_ar_payload_size);
  assign axi4Bus_ar_payload_burst = (_zz_axi4Bus_ar_payload_addr ? io_ifuAXI4_ar_payload_burst : io_lsuAXI4_ar_payload_burst);
  assign axi4Bus_ar_valid = (((arbiterState_1 == ArbiterState_IfuUsing) && io_ifuAXI4_ar_valid) || ((arbiterState_1 == ArbiterState_LsuUsing) && io_lsuAXI4_ar_valid));
  assign io_ifuAXI4_ar_ready = ((arbiterState_1 == ArbiterState_IfuUsing) && axi4Bus_ar_ready);
  assign io_lsuAXI4_ar_ready = ((arbiterState_1 == ArbiterState_LsuUsing) && axi4Bus_ar_ready);
  assign io_ifuAXI4_r_payload_data = axi4Bus_r_payload_data;
  assign io_ifuAXI4_r_payload_id = axi4Bus_r_payload_id;
  assign io_ifuAXI4_r_payload_resp = axi4Bus_r_payload_resp;
  assign io_ifuAXI4_r_payload_last = axi4Bus_r_payload_last;
  assign io_lsuAXI4_r_payload_data = axi4Bus_r_payload_data;
  assign io_lsuAXI4_r_payload_id = axi4Bus_r_payload_id;
  assign io_lsuAXI4_r_payload_resp = axi4Bus_r_payload_resp;
  assign io_lsuAXI4_r_payload_last = axi4Bus_r_payload_last;
  assign io_ifuAXI4_r_valid = ((arbiterState_1 == ArbiterState_IfuUsing) && axi4Bus_r_valid);
  assign io_lsuAXI4_r_valid = ((arbiterState_1 == ArbiterState_LsuUsing) && axi4Bus_r_valid);
  assign axi4Bus_r_ready = (((arbiterState_1 == ArbiterState_IfuUsing) && io_ifuAXI4_r_ready) || ((arbiterState_1 == ArbiterState_LsuUsing) && io_lsuAXI4_r_ready));
  assign axi4Bus_aw_valid = io_lsuAXI4_aw_valid;
  assign io_lsuAXI4_aw_ready = axi4Bus_aw_ready;
  assign axi4Bus_aw_payload_addr = io_lsuAXI4_aw_payload_addr;
  assign axi4Bus_aw_payload_id = io_lsuAXI4_aw_payload_id;
  assign axi4Bus_aw_payload_len = io_lsuAXI4_aw_payload_len;
  assign axi4Bus_aw_payload_size = io_lsuAXI4_aw_payload_size;
  assign axi4Bus_aw_payload_burst = io_lsuAXI4_aw_payload_burst;
  assign axi4Bus_w_valid = io_lsuAXI4_w_valid;
  assign io_lsuAXI4_w_ready = axi4Bus_w_ready;
  assign axi4Bus_w_payload_data = io_lsuAXI4_w_payload_data;
  assign axi4Bus_w_payload_strb = io_lsuAXI4_w_payload_strb;
  assign axi4Bus_w_payload_last = io_lsuAXI4_w_payload_last;
  assign io_lsuAXI4_b_valid = axi4Bus_b_valid;
  assign axi4Bus_b_ready = io_lsuAXI4_b_ready;
  assign io_lsuAXI4_b_payload_id = axi4Bus_b_payload_id;
  assign io_lsuAXI4_b_payload_resp = axi4Bus_b_payload_resp;
  assign when_Xbar_l102 = ((32'h02000000 <= axi4Bus_ar_payload_addr) && (axi4Bus_ar_payload_addr <= 32'h0200ffff));
  assign when_Xbar_l108 = ((32'h02000000 <= axi4Bus_aw_payload_addr) && (axi4Bus_aw_payload_addr <= 32'h0200ffff));
  assign axi4Bus_r_fire = (axi4Bus_r_valid && axi4Bus_r_ready);
  assign axi4Bus_b_fire = (axi4Bus_b_valid && axi4Bus_b_ready);
  assign when_Xbar_l118 = (axi4Bus_r_fire || axi4Bus_b_fire);
  assign io_clintAxi_ar_valid = ((crossState_1 == CrossState_Clint) && axi4Bus_ar_valid);
  assign io_clintAxi_ar_payload_addr = axi4Bus_ar_payload_addr;
  assign io_clintAxi_ar_payload_id = axi4Bus_ar_payload_id;
  assign io_clintAxi_ar_payload_len = axi4Bus_ar_payload_len;
  assign io_clintAxi_ar_payload_size = axi4Bus_ar_payload_size;
  assign io_clintAxi_ar_payload_burst = axi4Bus_ar_payload_burst;
  assign io_externalAxi_ar_valid = ((crossState_1 == CrossState_External) && axi4Bus_ar_valid);
  assign io_externalAxi_ar_payload_addr = axi4Bus_ar_payload_addr;
  assign io_externalAxi_ar_payload_id = axi4Bus_ar_payload_id;
  assign io_externalAxi_ar_payload_len = axi4Bus_ar_payload_len;
  assign io_externalAxi_ar_payload_size = axi4Bus_ar_payload_size;
  assign io_externalAxi_ar_payload_burst = axi4Bus_ar_payload_burst;
  assign axi4Bus_ar_ready = (((crossState_1 == CrossState_Clint) && io_clintAxi_ar_ready) || ((crossState_1 == CrossState_External) && io_externalAxi_ar_ready));
  assign axi4Bus_r_valid = (((crossState_1 == CrossState_Clint) && io_clintAxi_r_valid) || ((crossState_1 == CrossState_External) && io_externalAxi_r_valid));
  assign _zz_axi4Bus_r_payload_data = (crossState_1 == CrossState_Clint);
  assign axi4Bus_r_payload_data = (_zz_axi4Bus_r_payload_data ? io_clintAxi_r_payload_data : io_externalAxi_r_payload_data);
  assign axi4Bus_r_payload_id = (_zz_axi4Bus_r_payload_data ? io_clintAxi_r_payload_id : io_externalAxi_r_payload_id);
  assign axi4Bus_r_payload_resp = (_zz_axi4Bus_r_payload_data ? io_clintAxi_r_payload_resp : io_externalAxi_r_payload_resp);
  assign axi4Bus_r_payload_last = (_zz_axi4Bus_r_payload_data ? io_clintAxi_r_payload_last : io_externalAxi_r_payload_last);
  assign io_clintAxi_r_ready = ((crossState_1 == CrossState_Clint) && axi4Bus_r_ready);
  assign io_externalAxi_r_ready = ((crossState_1 == CrossState_External) && axi4Bus_r_ready);
  assign io_clintAxi_aw_valid = ((crossState_1 == CrossState_Clint) && axi4Bus_aw_valid);
  assign io_clintAxi_aw_payload_addr = axi4Bus_aw_payload_addr;
  assign io_clintAxi_aw_payload_id = axi4Bus_aw_payload_id;
  assign io_clintAxi_aw_payload_len = axi4Bus_aw_payload_len;
  assign io_clintAxi_aw_payload_size = axi4Bus_aw_payload_size;
  assign io_clintAxi_aw_payload_burst = axi4Bus_aw_payload_burst;
  assign io_externalAxi_aw_valid = ((crossState_1 == CrossState_External) && axi4Bus_aw_valid);
  assign io_externalAxi_aw_payload_addr = axi4Bus_aw_payload_addr;
  assign io_externalAxi_aw_payload_id = axi4Bus_aw_payload_id;
  assign io_externalAxi_aw_payload_len = axi4Bus_aw_payload_len;
  assign io_externalAxi_aw_payload_size = axi4Bus_aw_payload_size;
  assign io_externalAxi_aw_payload_burst = axi4Bus_aw_payload_burst;
  assign axi4Bus_aw_ready = (((crossState_1 == CrossState_Clint) && io_clintAxi_aw_ready) || ((crossState_1 == CrossState_External) && io_externalAxi_aw_ready));
  assign io_clintAxi_w_valid = ((crossState_1 == CrossState_Clint) && axi4Bus_w_valid);
  assign io_clintAxi_w_payload_data = axi4Bus_w_payload_data;
  assign io_clintAxi_w_payload_strb = axi4Bus_w_payload_strb;
  assign io_clintAxi_w_payload_last = axi4Bus_w_payload_last;
  assign io_externalAxi_w_valid = ((crossState_1 == CrossState_External) && axi4Bus_w_valid);
  assign io_externalAxi_w_payload_data = axi4Bus_w_payload_data;
  assign io_externalAxi_w_payload_strb = axi4Bus_w_payload_strb;
  assign io_externalAxi_w_payload_last = axi4Bus_w_payload_last;
  assign axi4Bus_w_ready = (((crossState_1 == CrossState_Clint) && io_clintAxi_w_ready) || ((crossState_1 == CrossState_External) && io_externalAxi_w_ready));
  assign axi4Bus_b_valid = (((crossState_1 == CrossState_Clint) && io_clintAxi_b_valid) || ((crossState_1 == CrossState_External) && io_externalAxi_b_valid));
  assign _zz_axi4Bus_b_payload_id = (crossState_1 == CrossState_Clint);
  assign axi4Bus_b_payload_id = (_zz_axi4Bus_b_payload_id ? io_clintAxi_b_payload_id : io_externalAxi_b_payload_id);
  assign axi4Bus_b_payload_resp = (_zz_axi4Bus_b_payload_id ? io_clintAxi_b_payload_resp : io_externalAxi_b_payload_resp);
  assign io_clintAxi_b_ready = ((crossState_1 == CrossState_Clint) && axi4Bus_b_ready);
  assign io_externalAxi_b_ready = ((crossState_1 == CrossState_External) && axi4Bus_b_ready);
  always @(posedge clock or posedge reset) begin
    if(reset) begin
      arbiterState_1 <= ArbiterState_Idle;
      crossState_1 <= CrossState_Idle;
    end else begin
      case(arbiterState_1)
        ArbiterState_Idle : begin
          if(io_ifuAXI4_ar_valid) begin
            arbiterState_1 <= ArbiterState_IfuUsing;
          end else begin
            if(io_lsuAXI4_ar_valid) begin
              arbiterState_1 <= ArbiterState_LsuUsing;
            end else begin
              arbiterState_1 <= ArbiterState_Idle;
            end
          end
        end
        ArbiterState_IfuUsing : begin
          if(io_ifuAXI4_r_fire) begin
            arbiterState_1 <= ArbiterState_Idle;
          end else begin
            arbiterState_1 <= ArbiterState_IfuUsing;
          end
        end
        default : begin
          if(io_lsuAXI4_r_fire) begin
            arbiterState_1 <= ArbiterState_Idle;
          end else begin
            arbiterState_1 <= ArbiterState_LsuUsing;
          end
        end
      endcase
      case(crossState_1)
        CrossState_Idle : begin
          if(axi4Bus_ar_valid) begin
            if(when_Xbar_l102) begin
              crossState_1 <= CrossState_Clint;
            end else begin
              crossState_1 <= CrossState_External;
            end
          end else begin
            if(axi4Bus_aw_valid) begin
              if(when_Xbar_l108) begin
                crossState_1 <= CrossState_Clint;
              end else begin
                crossState_1 <= CrossState_External;
              end
            end else begin
              crossState_1 <= CrossState_Idle;
            end
          end
        end
        default : begin
          if(when_Xbar_l118) begin
            crossState_1 <= CrossState_Idle;
          end else begin
            crossState_1 <= crossState_1;
          end
        end
      endcase
    end
  end


endmodule

module ysyx_23060082_WBU (
  input  wire          io_input_valid,
  output wire          io_input_ready,
  input  wire [31:0]   io_input_payload_pc,
  input  wire [31:0]   io_input_payload_pc_next,
  input  wire [31:0]   io_input_payload_mem_data_out,
  input  wire [31:0]   io_input_payload_alu_data_out,
  input  wire          io_input_payload_rf_ctrl_mem2reg,
  input  wire          io_input_payload_rf_ctrl_csr2reg,
  input  wire          io_input_payload_rf_ctrl_regWr,
  input  wire [4:0]    io_input_payload_rf_ctrl_rf_write_addr,
  output wire          io_output_valid,
  input  wire          io_output_ready,
  output wire [31:0]   io_output_payload_pc_next,
  output wire [4:0]    io_rf_write_addr,
  output wire [31:0]   io_rf_write_data,
  output wire          io_rf_write_en,
  input  wire          clock,
  input  wire          reset
);

  wire                io_input_fire;
  reg        [31:0]   payloadReg_pc;
  reg        [31:0]   payloadReg_pc_next;
  reg        [31:0]   payloadReg_mem_data_out;
  reg        [31:0]   payloadReg_alu_data_out;
  reg                 payloadReg_rf_ctrl_mem2reg;
  reg                 payloadReg_rf_ctrl_csr2reg;
  reg                 payloadReg_rf_ctrl_regWr;
  reg        [4:0]    payloadReg_rf_ctrl_rf_write_addr;
  reg                 validReg;
  reg                 outValid;
  wire                io_output_fire;

  assign io_input_fire = (io_input_valid && io_input_ready);
  assign io_input_ready = 1'b1;
  assign io_output_payload_pc_next = payloadReg_pc_next;
  assign io_rf_write_addr = payloadReg_rf_ctrl_rf_write_addr;
  assign io_rf_write_en = (payloadReg_rf_ctrl_regWr && validReg);
  assign io_rf_write_data = ((payloadReg_rf_ctrl_mem2reg || payloadReg_rf_ctrl_csr2reg) ? payloadReg_mem_data_out : payloadReg_alu_data_out);
  assign io_output_fire = (io_output_valid && io_output_ready);
  assign io_output_valid = outValid;
  always @(posedge clock) begin
    if(io_input_fire) begin
      payloadReg_pc <= io_input_payload_pc;
      payloadReg_pc_next <= io_input_payload_pc_next;
      payloadReg_mem_data_out <= io_input_payload_mem_data_out;
      payloadReg_alu_data_out <= io_input_payload_alu_data_out;
      payloadReg_rf_ctrl_mem2reg <= io_input_payload_rf_ctrl_mem2reg;
      payloadReg_rf_ctrl_csr2reg <= io_input_payload_rf_ctrl_csr2reg;
      payloadReg_rf_ctrl_regWr <= io_input_payload_rf_ctrl_regWr;
      payloadReg_rf_ctrl_rf_write_addr <= io_input_payload_rf_ctrl_rf_write_addr;
    end
  end

  always @(posedge clock or posedge reset) begin
    if(reset) begin
      validReg <= 1'b0;
      outValid <= 1'b0;
    end else begin
      if(io_input_fire) begin
        validReg <= 1'b1;
      end else begin
        if(validReg) begin
          validReg <= 1'b0;
        end else begin
          validReg <= validReg;
        end
      end
      if(io_input_fire) begin
        outValid <= 1'b1;
      end else begin
        if(io_output_fire) begin
          outValid <= 1'b0;
        end else begin
          outValid <= outValid;
        end
      end
    end
  end


endmodule

module ysyx_23060082_LSU (
  input  wire          io_input_valid,
  input  wire [31:0]   io_input_payload_pc,
  input  wire [31:0]   io_input_payload_pc_next,
  input  wire          io_input_payload_rf_ctrl_mem2reg,
  input  wire          io_input_payload_rf_ctrl_csr2reg,
  input  wire          io_input_payload_rf_ctrl_regWr,
  input  wire [4:0]    io_input_payload_rf_ctrl_rf_write_addr,
  input  wire          io_input_payload_mem_ctrl_memWr,
  input  wire [2:0]    io_input_payload_mem_ctrl_memOp,
  input  wire [2:0]    io_input_payload_csr_ctrl_csr_cmd,
  input  wire          io_input_payload_csr_ctrl_trap_enter,
  input  wire          io_input_payload_csr_ctrl_trap_exit,
  input  wire [11:0]   io_input_payload_imm,
  input  wire [31:0]   io_input_payload_rfReadData1,
  input  wire [31:0]   io_input_payload_rfReadData2,
  input  wire [31:0]   io_input_payload_aluResult,
  output wire          io_output_valid,
  input  wire          io_output_ready,
  output wire [31:0]   io_output_payload_pc,
  output wire [31:0]   io_output_payload_pc_next,
  output wire [31:0]   io_output_payload_mem_data_out,
  output wire [31:0]   io_output_payload_alu_data_out,
  output wire          io_output_payload_rf_ctrl_mem2reg,
  output wire          io_output_payload_rf_ctrl_csr2reg,
  output wire          io_output_payload_rf_ctrl_regWr,
  output wire [4:0]    io_output_payload_rf_ctrl_rf_write_addr,
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
  input  wire          clock,
  input  wire          reset
);
  localparam LsuState_Idle = 2'd0;
  localparam LsuState_WaitMem = 2'd1;
  localparam LsuState_Done = 2'd2;

  wire       [4:0]    dataProcess_io_addrOp;
  wire       [31:0]   dataProcess_io_rdata;
  wire                axiCtrl_io_readReq;
  wire                axiCtrl_io_writeReq;
  wire       [2:0]    axiCtrl_io_size;
  wire       [31:0]   dataProcess_io_wdataReal;
  wire       [3:0]    dataProcess_io_wmask;
  wire       [31:0]   dataProcess_io_rdataReal;
  wire                axiCtrl_io_readEnd;
  wire       [31:0]   axiCtrl_io_readData;
  wire                axiCtrl_io_writeEnd;
  wire                axiCtrl_io_axi4_ar_valid;
  wire       [31:0]   axiCtrl_io_axi4_ar_payload_addr;
  wire       [3:0]    axiCtrl_io_axi4_ar_payload_id;
  wire       [7:0]    axiCtrl_io_axi4_ar_payload_len;
  wire       [2:0]    axiCtrl_io_axi4_ar_payload_size;
  wire       [1:0]    axiCtrl_io_axi4_ar_payload_burst;
  wire                axiCtrl_io_axi4_aw_valid;
  wire       [31:0]   axiCtrl_io_axi4_aw_payload_addr;
  wire       [3:0]    axiCtrl_io_axi4_aw_payload_id;
  wire       [7:0]    axiCtrl_io_axi4_aw_payload_len;
  wire       [2:0]    axiCtrl_io_axi4_aw_payload_size;
  wire       [1:0]    axiCtrl_io_axi4_aw_payload_burst;
  wire                axiCtrl_io_axi4_w_valid;
  wire       [31:0]   axiCtrl_io_axi4_w_payload_data;
  wire       [3:0]    axiCtrl_io_axi4_w_payload_strb;
  wire                axiCtrl_io_axi4_w_payload_last;
  wire                axiCtrl_io_axi4_r_ready;
  wire                axiCtrl_io_axi4_b_ready;
  wire       [31:0]   csr_io_csr_rdata;
  wire       [31:0]   csr_io_mtvec;
  wire       [31:0]   csr_io_mepc;
  reg        [1:0]    state;
  wire                needRead;
  wire                needWrite;
  wire                needMem;
  wire                rdEnd;
  wire                wrEnd;
  reg        [31:0]   rdataReg;
  wire                when_LSU_l56;
  wire                io_output_fire;
  wire                willValid;
  `ifndef SYNTHESIS
  reg [55:0] state_string;
  `endif


  ysyx_23060082_DataProcess dataProcess (
    .io_addrOp    (dataProcess_io_addrOp[4:0]        ), //i
    .io_wdata     (io_input_payload_rfReadData2[31:0]), //i
    .io_wdataReal (dataProcess_io_wdataReal[31:0]    ), //o
    .io_wmask     (dataProcess_io_wmask[3:0]         ), //o
    .io_rdata     (dataProcess_io_rdata[31:0]        ), //i
    .io_rdataReal (dataProcess_io_rdataReal[31:0]    )  //o
  );
  ysyx_23060082_AXI_Ctrl axiCtrl (
    .io_readReq               (axiCtrl_io_readReq                   ), //i
    .io_writeReq              (axiCtrl_io_writeReq                  ), //i
    .io_size                  (axiCtrl_io_size[2:0]                 ), //i
    .io_readAddr              (io_input_payload_aluResult[31:0]     ), //i
    .io_writeAddr             (io_input_payload_aluResult[31:0]     ), //i
    .io_writeData             (dataProcess_io_wdataReal[31:0]       ), //i
    .io_writeMask             (dataProcess_io_wmask[3:0]            ), //i
    .io_readEnd               (axiCtrl_io_readEnd                   ), //o
    .io_readData              (axiCtrl_io_readData[31:0]            ), //o
    .io_writeEnd              (axiCtrl_io_writeEnd                  ), //o
    .io_axi4_aw_valid         (axiCtrl_io_axi4_aw_valid             ), //o
    .io_axi4_aw_ready         (io_axi4_aw_ready                     ), //i
    .io_axi4_aw_payload_addr  (axiCtrl_io_axi4_aw_payload_addr[31:0]), //o
    .io_axi4_aw_payload_id    (axiCtrl_io_axi4_aw_payload_id[3:0]   ), //o
    .io_axi4_aw_payload_len   (axiCtrl_io_axi4_aw_payload_len[7:0]  ), //o
    .io_axi4_aw_payload_size  (axiCtrl_io_axi4_aw_payload_size[2:0] ), //o
    .io_axi4_aw_payload_burst (axiCtrl_io_axi4_aw_payload_burst[1:0]), //o
    .io_axi4_w_valid          (axiCtrl_io_axi4_w_valid              ), //o
    .io_axi4_w_ready          (io_axi4_w_ready                      ), //i
    .io_axi4_w_payload_data   (axiCtrl_io_axi4_w_payload_data[31:0] ), //o
    .io_axi4_w_payload_strb   (axiCtrl_io_axi4_w_payload_strb[3:0]  ), //o
    .io_axi4_w_payload_last   (axiCtrl_io_axi4_w_payload_last       ), //o
    .io_axi4_b_valid          (io_axi4_b_valid                      ), //i
    .io_axi4_b_ready          (axiCtrl_io_axi4_b_ready              ), //o
    .io_axi4_b_payload_id     (io_axi4_b_payload_id[3:0]            ), //i
    .io_axi4_b_payload_resp   (io_axi4_b_payload_resp[1:0]          ), //i
    .io_axi4_ar_valid         (axiCtrl_io_axi4_ar_valid             ), //o
    .io_axi4_ar_ready         (io_axi4_ar_ready                     ), //i
    .io_axi4_ar_payload_addr  (axiCtrl_io_axi4_ar_payload_addr[31:0]), //o
    .io_axi4_ar_payload_id    (axiCtrl_io_axi4_ar_payload_id[3:0]   ), //o
    .io_axi4_ar_payload_len   (axiCtrl_io_axi4_ar_payload_len[7:0]  ), //o
    .io_axi4_ar_payload_size  (axiCtrl_io_axi4_ar_payload_size[2:0] ), //o
    .io_axi4_ar_payload_burst (axiCtrl_io_axi4_ar_payload_burst[1:0]), //o
    .io_axi4_r_valid          (io_axi4_r_valid                      ), //i
    .io_axi4_r_ready          (axiCtrl_io_axi4_r_ready              ), //o
    .io_axi4_r_payload_data   (io_axi4_r_payload_data[31:0]         ), //i
    .io_axi4_r_payload_id     (io_axi4_r_payload_id[3:0]            ), //i
    .io_axi4_r_payload_resp   (io_axi4_r_payload_resp[1:0]          ), //i
    .io_axi4_r_payload_last   (io_axi4_r_payload_last               ), //i
    .clock                    (clock                                ), //i
    .reset                    (reset                                )  //i
  );
  ysyx_23060082_CSR csr (
    .io_csr_addr   (io_input_payload_imm[11:0]            ), //i
    .io_csr_wdata  (io_input_payload_rfReadData1[31:0]    ), //i
    .io_csr_rdata  (csr_io_csr_rdata[31:0]                ), //o
    .io_csr_cmd    (io_input_payload_csr_ctrl_csr_cmd[2:0]), //i
    .io_trap_enter (io_input_payload_csr_ctrl_trap_enter  ), //i
    .io_trap_exit  (io_input_payload_csr_ctrl_trap_exit   ), //i
    .io_pc_in      (io_input_payload_pc[31:0]             ), //i
    .io_cause_in   (io_input_payload_rfReadData1[31:0]    ), //i
    .io_mtvec      (csr_io_mtvec[31:0]                    ), //o
    .io_mepc       (csr_io_mepc[31:0]                     ), //o
    .clock         (clock                                 ), //i
    .reset         (reset                                 )  //i
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

  assign needRead = (io_input_valid && io_input_payload_rf_ctrl_mem2reg);
  assign needWrite = (io_input_valid && io_input_payload_mem_ctrl_memWr);
  assign needMem = (needRead || needWrite);
  assign rdEnd = (((state == LsuState_WaitMem) && axiCtrl_io_readEnd) && io_input_payload_rf_ctrl_mem2reg);
  assign wrEnd = (((state == LsuState_WaitMem) && axiCtrl_io_writeEnd) && io_input_payload_mem_ctrl_memWr);
  assign dataProcess_io_addrOp = {io_input_payload_aluResult[1 : 0],io_input_payload_mem_ctrl_memOp};
  assign dataProcess_io_rdata = (((state == LsuState_WaitMem) && rdEnd) ? axiCtrl_io_readData : rdataReg);
  assign io_axi4_aw_valid = axiCtrl_io_axi4_aw_valid;
  assign io_axi4_aw_payload_addr = axiCtrl_io_axi4_aw_payload_addr;
  assign io_axi4_aw_payload_id = axiCtrl_io_axi4_aw_payload_id;
  assign io_axi4_aw_payload_len = axiCtrl_io_axi4_aw_payload_len;
  assign io_axi4_aw_payload_size = axiCtrl_io_axi4_aw_payload_size;
  assign io_axi4_aw_payload_burst = axiCtrl_io_axi4_aw_payload_burst;
  assign io_axi4_w_valid = axiCtrl_io_axi4_w_valid;
  assign io_axi4_w_payload_data = axiCtrl_io_axi4_w_payload_data;
  assign io_axi4_w_payload_strb = axiCtrl_io_axi4_w_payload_strb;
  assign io_axi4_w_payload_last = axiCtrl_io_axi4_w_payload_last;
  assign io_axi4_b_ready = axiCtrl_io_axi4_b_ready;
  assign io_axi4_ar_valid = axiCtrl_io_axi4_ar_valid;
  assign io_axi4_ar_payload_addr = axiCtrl_io_axi4_ar_payload_addr;
  assign io_axi4_ar_payload_id = axiCtrl_io_axi4_ar_payload_id;
  assign io_axi4_ar_payload_len = axiCtrl_io_axi4_ar_payload_len;
  assign io_axi4_ar_payload_size = axiCtrl_io_axi4_ar_payload_size;
  assign io_axi4_ar_payload_burst = axiCtrl_io_axi4_ar_payload_burst;
  assign io_axi4_r_ready = axiCtrl_io_axi4_r_ready;
  assign axiCtrl_io_readReq = (needRead && (state == LsuState_Idle));
  assign axiCtrl_io_writeReq = (needWrite && (state == LsuState_Idle));
  assign axiCtrl_io_size = {1'b0,io_input_payload_mem_ctrl_memOp[1 : 0]};
  assign when_LSU_l56 = (rdEnd || wrEnd);
  assign io_output_fire = (io_output_valid && io_output_ready);
  assign willValid = (((((state == LsuState_WaitMem) && rdEnd) || wrEnd) || (state == LsuState_Done)) || (((state == LsuState_Idle) && io_input_valid) && (! needMem)));
  assign io_output_valid = (io_input_valid && willValid);
  assign io_output_payload_pc = io_input_payload_pc;
  assign io_output_payload_pc_next = (io_input_payload_csr_ctrl_trap_enter ? csr_io_mtvec : (io_input_payload_csr_ctrl_trap_exit ? csr_io_mepc : io_input_payload_pc_next));
  assign io_output_payload_mem_data_out = ((io_input_payload_csr_ctrl_csr_cmd != 3'b000) ? csr_io_csr_rdata : dataProcess_io_rdataReal);
  assign io_output_payload_alu_data_out = io_input_payload_aluResult;
  assign io_output_payload_rf_ctrl_mem2reg = io_input_payload_rf_ctrl_mem2reg;
  assign io_output_payload_rf_ctrl_csr2reg = io_input_payload_rf_ctrl_csr2reg;
  assign io_output_payload_rf_ctrl_regWr = io_input_payload_rf_ctrl_regWr;
  assign io_output_payload_rf_ctrl_rf_write_addr = io_input_payload_rf_ctrl_rf_write_addr;
  always @(posedge clock or posedge reset) begin
    if(reset) begin
      state <= LsuState_Idle;
      rdataReg <= 32'h0;
    end else begin
      if(rdEnd) begin
        rdataReg <= axiCtrl_io_readData;
      end
      case(state)
        LsuState_Idle : begin
          if(needMem) begin
            state <= LsuState_WaitMem;
          end else begin
            state <= state;
          end
        end
        LsuState_WaitMem : begin
          if(when_LSU_l56) begin
            if(io_output_fire) begin
              state <= LsuState_Idle;
            end else begin
              state <= LsuState_Done;
            end
          end else begin
            state <= state;
          end
        end
        default : begin
          if(io_output_fire) begin
            state <= LsuState_Idle;
          end else begin
            state <= state;
          end
        end
      endcase
    end
  end


endmodule

module ysyx_23060082_EXU (
  input  wire          io_input_valid,
  input  wire [31:0]   io_input_payload_pc,
  input  wire          io_input_payload_ctrl_rf_si_mem2reg,
  input  wire          io_input_payload_ctrl_rf_si_csr2reg,
  input  wire          io_input_payload_ctrl_rf_si_regWr,
  input  wire [4:0]    io_input_payload_ctrl_rf_si_rf_write_addr,
  input  wire          io_input_payload_ctrl_alu_si_alu_asrc,
  input  wire [1:0]    io_input_payload_ctrl_alu_si_alu_bsrc,
  input  wire [3:0]    io_input_payload_ctrl_alu_si_alu_ctr,
  input  wire [2:0]    io_input_payload_ctrl_alu_si_branch,
  input  wire          io_input_payload_ctrl_mem_si_memWr,
  input  wire [2:0]    io_input_payload_ctrl_mem_si_memOp,
  input  wire [2:0]    io_input_payload_ctrl_csr_si_csr_cmd,
  input  wire          io_input_payload_ctrl_csr_si_trap_enter,
  input  wire          io_input_payload_ctrl_csr_si_trap_exit,
  input  wire [31:0]   io_input_payload_imm,
  input  wire [31:0]   io_input_payload_rfReadData1,
  input  wire [31:0]   io_input_payload_rfReadData2,
  output wire          io_output_valid,
  input  wire          io_output_ready,
  output wire [31:0]   io_output_payload_pc,
  output wire [31:0]   io_output_payload_pc_next,
  output wire          io_output_payload_rf_ctrl_mem2reg,
  output wire          io_output_payload_rf_ctrl_csr2reg,
  output wire          io_output_payload_rf_ctrl_regWr,
  output wire [4:0]    io_output_payload_rf_ctrl_rf_write_addr,
  output wire          io_output_payload_mem_ctrl_memWr,
  output wire [2:0]    io_output_payload_mem_ctrl_memOp,
  output wire [2:0]    io_output_payload_csr_ctrl_csr_cmd,
  output wire          io_output_payload_csr_ctrl_trap_enter,
  output wire          io_output_payload_csr_ctrl_trap_exit,
  output wire [11:0]   io_output_payload_imm,
  output wire [31:0]   io_output_payload_rfReadData1,
  output wire [31:0]   io_output_payload_rfReadData2,
  output wire [31:0]   io_output_payload_aluResult
);

  wire                alu_io_less;
  wire                alu_io_zero;
  wire       [31:0]   alu_io_aluResult;
  wire                banchCond_io_pc_asrc;
  wire                banchCond_io_pc_bsrc;
  reg        [31:0]   _zz_io_aluIn1;
  reg        [31:0]   _zz_io_aluIn2;
  wire       [31:0]   pcDataA;
  wire       [31:0]   pcDataB;
  wire                willValid;

  ysyx_23060082_ALU alu (
    .io_aluIn1    (_zz_io_aluIn1[31:0]                      ), //i
    .io_aluIn2    (_zz_io_aluIn2[31:0]                      ), //i
    .io_aluCtr    (io_input_payload_ctrl_alu_si_alu_ctr[3:0]), //i
    .io_less      (alu_io_less                              ), //o
    .io_zero      (alu_io_zero                              ), //o
    .io_aluResult (alu_io_aluResult[31:0]                   )  //o
  );
  ysyx_23060082_BranchCond banchCond (
    .io_branch  (io_input_payload_ctrl_alu_si_branch[2:0]), //i
    .io_less    (alu_io_less                             ), //i
    .io_zero    (alu_io_zero                             ), //i
    .io_pc_asrc (banchCond_io_pc_asrc                    ), //o
    .io_pc_bsrc (banchCond_io_pc_bsrc                    )  //o
  );
  always @(*) begin
    case(io_input_payload_ctrl_alu_si_alu_asrc)
      1'b1 : begin
        _zz_io_aluIn1 = io_input_payload_pc;
      end
      default : begin
        _zz_io_aluIn1 = io_input_payload_rfReadData1;
      end
    endcase
  end

  always @(*) begin
    case(io_input_payload_ctrl_alu_si_alu_bsrc)
      2'b00 : begin
        _zz_io_aluIn2 = io_input_payload_rfReadData2;
      end
      2'b01 : begin
        _zz_io_aluIn2 = io_input_payload_imm;
      end
      default : begin
        _zz_io_aluIn2 = 32'h00000004;
      end
    endcase
  end

  assign pcDataA = (banchCond_io_pc_asrc ? io_input_payload_imm : 32'h00000004);
  assign pcDataB = (banchCond_io_pc_bsrc ? io_input_payload_rfReadData1 : io_input_payload_pc);
  assign willValid = 1'b1;
  assign io_output_valid = (io_input_valid && willValid);
  assign io_output_payload_pc = io_input_payload_pc;
  assign io_output_payload_pc_next = (pcDataA + pcDataB);
  assign io_output_payload_aluResult = alu_io_aluResult;
  assign io_output_payload_imm = io_input_payload_imm[11 : 0];
  assign io_output_payload_rfReadData1 = io_input_payload_rfReadData1;
  assign io_output_payload_rfReadData2 = io_input_payload_rfReadData2;
  assign io_output_payload_rf_ctrl_mem2reg = io_input_payload_ctrl_rf_si_mem2reg;
  assign io_output_payload_rf_ctrl_csr2reg = io_input_payload_ctrl_rf_si_csr2reg;
  assign io_output_payload_rf_ctrl_regWr = io_input_payload_ctrl_rf_si_regWr;
  assign io_output_payload_rf_ctrl_rf_write_addr = io_input_payload_ctrl_rf_si_rf_write_addr;
  assign io_output_payload_mem_ctrl_memWr = io_input_payload_ctrl_mem_si_memWr;
  assign io_output_payload_mem_ctrl_memOp = io_input_payload_ctrl_mem_si_memOp;
  assign io_output_payload_csr_ctrl_csr_cmd = io_input_payload_ctrl_csr_si_csr_cmd;
  assign io_output_payload_csr_ctrl_trap_enter = io_input_payload_ctrl_csr_si_trap_enter;
  assign io_output_payload_csr_ctrl_trap_exit = io_input_payload_ctrl_csr_si_trap_exit;

endmodule

module ysyx_23060082_IDU (
  input  wire          io_input_valid,
  input  wire [31:0]   io_input_payload_pc,
  input  wire [31:0]   io_input_payload_instr,
  output wire          io_output_valid,
  input  wire          io_output_ready,
  output wire [31:0]   io_output_payload_pc,
  output wire          io_output_payload_ctrl_rf_si_mem2reg,
  output wire          io_output_payload_ctrl_rf_si_csr2reg,
  output wire          io_output_payload_ctrl_rf_si_regWr,
  output wire [4:0]    io_output_payload_ctrl_rf_si_rf_write_addr,
  output wire          io_output_payload_ctrl_alu_si_alu_asrc,
  output wire [1:0]    io_output_payload_ctrl_alu_si_alu_bsrc,
  output wire [3:0]    io_output_payload_ctrl_alu_si_alu_ctr,
  output wire [2:0]    io_output_payload_ctrl_alu_si_branch,
  output wire          io_output_payload_ctrl_mem_si_memWr,
  output wire [2:0]    io_output_payload_ctrl_mem_si_memOp,
  output wire [2:0]    io_output_payload_ctrl_csr_si_csr_cmd,
  output wire          io_output_payload_ctrl_csr_si_trap_enter,
  output wire          io_output_payload_ctrl_csr_si_trap_exit,
  output wire [31:0]   io_output_payload_imm,
  output wire [31:0]   io_output_payload_rfReadData1,
  output wire [31:0]   io_output_payload_rfReadData2,
  output wire [4:0]    io_rfReadAddr1,
  output wire [4:0]    io_rfReadAddr2,
  input  wire [31:0]   io_rfReadData1,
  input  wire [31:0]   io_rfReadData2
);

  wire                decoder_1_io_ctrl_rf_si_mem2reg;
  wire                decoder_1_io_ctrl_rf_si_csr2reg;
  wire                decoder_1_io_ctrl_rf_si_regWr;
  wire       [4:0]    decoder_1_io_ctrl_rf_si_rf_write_addr;
  wire                decoder_1_io_ctrl_alu_si_alu_asrc;
  wire       [1:0]    decoder_1_io_ctrl_alu_si_alu_bsrc;
  wire       [3:0]    decoder_1_io_ctrl_alu_si_alu_ctr;
  wire       [2:0]    decoder_1_io_ctrl_alu_si_branch;
  wire                decoder_1_io_ctrl_mem_si_memWr;
  wire       [2:0]    decoder_1_io_ctrl_mem_si_memOp;
  wire       [2:0]    decoder_1_io_ctrl_csr_si_csr_cmd;
  wire                decoder_1_io_ctrl_csr_si_trap_enter;
  wire                decoder_1_io_ctrl_csr_si_trap_exit;
  wire       [31:0]   decoder_1_io_imm;
  wire                willValid;
  reg        [4:0]    _zz_io_rfReadAddr1;

  Decoder decoder_1 (
    .io_instr                    (io_input_payload_instr[31:0]              ), //i
    .io_ctrl_rf_si_mem2reg       (decoder_1_io_ctrl_rf_si_mem2reg           ), //o
    .io_ctrl_rf_si_csr2reg       (decoder_1_io_ctrl_rf_si_csr2reg           ), //o
    .io_ctrl_rf_si_regWr         (decoder_1_io_ctrl_rf_si_regWr             ), //o
    .io_ctrl_rf_si_rf_write_addr (decoder_1_io_ctrl_rf_si_rf_write_addr[4:0]), //o
    .io_ctrl_alu_si_alu_asrc     (decoder_1_io_ctrl_alu_si_alu_asrc         ), //o
    .io_ctrl_alu_si_alu_bsrc     (decoder_1_io_ctrl_alu_si_alu_bsrc[1:0]    ), //o
    .io_ctrl_alu_si_alu_ctr      (decoder_1_io_ctrl_alu_si_alu_ctr[3:0]     ), //o
    .io_ctrl_alu_si_branch       (decoder_1_io_ctrl_alu_si_branch[2:0]      ), //o
    .io_ctrl_mem_si_memWr        (decoder_1_io_ctrl_mem_si_memWr            ), //o
    .io_ctrl_mem_si_memOp        (decoder_1_io_ctrl_mem_si_memOp[2:0]       ), //o
    .io_ctrl_csr_si_csr_cmd      (decoder_1_io_ctrl_csr_si_csr_cmd[2:0]     ), //o
    .io_ctrl_csr_si_trap_enter   (decoder_1_io_ctrl_csr_si_trap_enter       ), //o
    .io_ctrl_csr_si_trap_exit    (decoder_1_io_ctrl_csr_si_trap_exit        ), //o
    .io_imm                      (decoder_1_io_imm[31:0]                    )  //o
  );
  assign willValid = 1'b1;
  assign io_output_valid = (io_input_valid && willValid);
  always @(*) begin
    case(decoder_1_io_ctrl_csr_si_trap_enter)
      1'b1 : begin
        _zz_io_rfReadAddr1 = 5'h0f;
      end
      default : begin
        _zz_io_rfReadAddr1 = io_input_payload_instr[19 : 15];
      end
    endcase
  end

  assign io_rfReadAddr1 = _zz_io_rfReadAddr1;
  assign io_rfReadAddr2 = io_input_payload_instr[24 : 20];
  assign io_output_payload_pc = io_input_payload_pc;
  assign io_output_payload_rfReadData1 = io_rfReadData1;
  assign io_output_payload_rfReadData2 = io_rfReadData2;
  assign io_output_payload_ctrl_rf_si_mem2reg = decoder_1_io_ctrl_rf_si_mem2reg;
  assign io_output_payload_ctrl_rf_si_csr2reg = decoder_1_io_ctrl_rf_si_csr2reg;
  assign io_output_payload_ctrl_rf_si_regWr = decoder_1_io_ctrl_rf_si_regWr;
  assign io_output_payload_ctrl_rf_si_rf_write_addr = decoder_1_io_ctrl_rf_si_rf_write_addr;
  assign io_output_payload_ctrl_alu_si_alu_asrc = decoder_1_io_ctrl_alu_si_alu_asrc;
  assign io_output_payload_ctrl_alu_si_alu_bsrc = decoder_1_io_ctrl_alu_si_alu_bsrc;
  assign io_output_payload_ctrl_alu_si_alu_ctr = decoder_1_io_ctrl_alu_si_alu_ctr;
  assign io_output_payload_ctrl_alu_si_branch = decoder_1_io_ctrl_alu_si_branch;
  assign io_output_payload_ctrl_mem_si_memWr = decoder_1_io_ctrl_mem_si_memWr;
  assign io_output_payload_ctrl_mem_si_memOp = decoder_1_io_ctrl_mem_si_memOp;
  assign io_output_payload_ctrl_csr_si_csr_cmd = decoder_1_io_ctrl_csr_si_csr_cmd;
  assign io_output_payload_ctrl_csr_si_trap_enter = decoder_1_io_ctrl_csr_si_trap_enter;
  assign io_output_payload_ctrl_csr_si_trap_exit = decoder_1_io_ctrl_csr_si_trap_exit;
  assign io_output_payload_imm = decoder_1_io_imm;

endmodule

module ysyx_23060082_IFU (
  input  wire          io_input_valid,
  output wire          io_input_ready,
  input  wire [31:0]   io_input_payload_pc_next,
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
  input  wire          clock,
  input  wire          reset
);
  localparam IfuState_Idle = 2'd0;
  localparam IfuState_WaitMem = 2'd1;
  localparam IfuState_Done = 2'd2;

  wire                axiCtrl_io_readReq;
  wire                axiCtrl_io_readEnd;
  wire       [31:0]   axiCtrl_io_readData;
  wire                axiCtrl_io_axi4_ar_valid;
  wire       [31:0]   axiCtrl_io_axi4_ar_payload_addr;
  wire       [3:0]    axiCtrl_io_axi4_ar_payload_id;
  wire       [7:0]    axiCtrl_io_axi4_ar_payload_len;
  wire       [2:0]    axiCtrl_io_axi4_ar_payload_size;
  wire       [1:0]    axiCtrl_io_axi4_ar_payload_burst;
  wire                axiCtrl_io_axi4_r_ready;
  reg        [1:0]    state;
  reg                 rstReg1;
  reg                 rstReg2;
  wire                rstEnd;
  reg                 dataValid;
  wire                io_input_fire;
  wire                when_IFU_l29;
  wire                io_output_fire;
  reg        [31:0]   pc;
  wire                when_IFU_l44;
  reg        [31:0]   rdataReg;
  wire                willValid;
  `ifndef SYNTHESIS
  reg [55:0] state_string;
  `endif


  ysyx_23060082_AXI_Ctrl_ReadOnly axiCtrl (
    .io_readReq               (axiCtrl_io_readReq                   ), //i
    .io_readAddr              (pc[31:0]                             ), //i
    .io_readEnd               (axiCtrl_io_readEnd                   ), //o
    .io_readData              (axiCtrl_io_readData[31:0]            ), //o
    .io_axi4_ar_valid         (axiCtrl_io_axi4_ar_valid             ), //o
    .io_axi4_ar_ready         (io_axi4_ar_ready                     ), //i
    .io_axi4_ar_payload_addr  (axiCtrl_io_axi4_ar_payload_addr[31:0]), //o
    .io_axi4_ar_payload_id    (axiCtrl_io_axi4_ar_payload_id[3:0]   ), //o
    .io_axi4_ar_payload_len   (axiCtrl_io_axi4_ar_payload_len[7:0]  ), //o
    .io_axi4_ar_payload_size  (axiCtrl_io_axi4_ar_payload_size[2:0] ), //o
    .io_axi4_ar_payload_burst (axiCtrl_io_axi4_ar_payload_burst[1:0]), //o
    .io_axi4_r_valid          (io_axi4_r_valid                      ), //i
    .io_axi4_r_ready          (axiCtrl_io_axi4_r_ready              ), //o
    .io_axi4_r_payload_data   (io_axi4_r_payload_data[31:0]         ), //i
    .io_axi4_r_payload_id     (io_axi4_r_payload_id[3:0]            ), //i
    .io_axi4_r_payload_resp   (io_axi4_r_payload_resp[1:0]          ), //i
    .io_axi4_r_payload_last   (io_axi4_r_payload_last               ), //i
    .clock                    (clock                                ), //i
    .reset                    (reset                                )  //i
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

  assign rstEnd = (rstReg1 && (! rstReg2));
  assign io_input_fire = (io_input_valid && io_input_ready);
  assign when_IFU_l29 = (io_input_fire || rstEnd);
  assign io_output_fire = (io_output_valid && io_output_ready);
  assign io_axi4_ar_valid = axiCtrl_io_axi4_ar_valid;
  assign io_axi4_ar_payload_addr = axiCtrl_io_axi4_ar_payload_addr;
  assign io_axi4_ar_payload_id = axiCtrl_io_axi4_ar_payload_id;
  assign io_axi4_ar_payload_len = axiCtrl_io_axi4_ar_payload_len;
  assign io_axi4_ar_payload_size = axiCtrl_io_axi4_ar_payload_size;
  assign io_axi4_ar_payload_burst = axiCtrl_io_axi4_ar_payload_burst;
  assign io_axi4_r_ready = axiCtrl_io_axi4_r_ready;
  assign axiCtrl_io_readReq = ((state == IfuState_Idle) && dataValid);
  assign when_IFU_l44 = ((state == IfuState_WaitMem) && axiCtrl_io_readEnd);
  assign willValid = (((state == IfuState_WaitMem) && axiCtrl_io_readEnd) || (state == IfuState_Done));
  assign io_output_valid = (dataValid && willValid);
  assign io_input_ready = ((! dataValid) || io_output_fire);
  assign io_output_payload_pc = pc;
  assign io_output_payload_instr = (((state == IfuState_WaitMem) && axiCtrl_io_readEnd) ? axiCtrl_io_readData : rdataReg);
  always @(posedge clock or posedge reset) begin
    if(reset) begin
      state <= IfuState_Idle;
      rstReg1 <= 1'b0;
      rstReg2 <= 1'b0;
      dataValid <= 1'b0;
      pc <= 32'h30000000;
      rdataReg <= 32'h0;
    end else begin
      rstReg1 <= 1'b1;
      rstReg2 <= rstReg1;
      if(when_IFU_l29) begin
        dataValid <= 1'b1;
      end else begin
        if(io_output_fire) begin
          dataValid <= 1'b0;
        end else begin
          dataValid <= dataValid;
        end
      end
      if(io_input_fire) begin
        pc <= io_input_payload_pc_next;
      end
      if(when_IFU_l44) begin
        rdataReg <= axiCtrl_io_readData;
      end
      case(state)
        IfuState_Idle : begin
          if(dataValid) begin
            state <= IfuState_WaitMem;
          end else begin
            state <= state;
          end
        end
        IfuState_WaitMem : begin
          if(axiCtrl_io_readEnd) begin
            if(io_output_fire) begin
              state <= IfuState_Idle;
            end else begin
              state <= IfuState_Done;
            end
          end else begin
            state <= state;
          end
        end
        default : begin
          if(io_output_fire) begin
            state <= IfuState_Idle;
          end else begin
            state <= state;
          end
        end
      endcase
    end
  end


endmodule

module ysyx_23060082_RegFile (
  input  wire [4:0]    io_readAddr1,
  input  wire [4:0]    io_readAddr2,
  input  wire [4:0]    io_writeAddr,
  input  wire [31:0]   io_writeData,
  input  wire          io_writeEn,
  output wire [31:0]   io_readData1,
  output wire [31:0]   io_readData2,
  input  wire          clock,
  input  wire          reset
);

  reg        [31:0]   _zz_io_readData1;
  wire       [3:0]    _zz_io_readData1_1;
  reg        [31:0]   _zz_io_readData2;
  wire       [3:0]    _zz_io_readData2_1;
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
  wire                when_23060082_l123;
  wire       [15:0]   _zz_1;

  assign _zz_io_readData1_1 = io_readAddr1[3 : 0];
  assign _zz_io_readData2_1 = io_readAddr2[3 : 0];
  always @(*) begin
    case(_zz_io_readData1_1)
      4'b0000 : _zz_io_readData1 = rf_0;
      4'b0001 : _zz_io_readData1 = rf_1;
      4'b0010 : _zz_io_readData1 = rf_2;
      4'b0011 : _zz_io_readData1 = rf_3;
      4'b0100 : _zz_io_readData1 = rf_4;
      4'b0101 : _zz_io_readData1 = rf_5;
      4'b0110 : _zz_io_readData1 = rf_6;
      4'b0111 : _zz_io_readData1 = rf_7;
      4'b1000 : _zz_io_readData1 = rf_8;
      4'b1001 : _zz_io_readData1 = rf_9;
      4'b1010 : _zz_io_readData1 = rf_10;
      4'b1011 : _zz_io_readData1 = rf_11;
      4'b1100 : _zz_io_readData1 = rf_12;
      4'b1101 : _zz_io_readData1 = rf_13;
      4'b1110 : _zz_io_readData1 = rf_14;
      default : _zz_io_readData1 = rf_15;
    endcase
  end

  always @(*) begin
    case(_zz_io_readData2_1)
      4'b0000 : _zz_io_readData2 = rf_0;
      4'b0001 : _zz_io_readData2 = rf_1;
      4'b0010 : _zz_io_readData2 = rf_2;
      4'b0011 : _zz_io_readData2 = rf_3;
      4'b0100 : _zz_io_readData2 = rf_4;
      4'b0101 : _zz_io_readData2 = rf_5;
      4'b0110 : _zz_io_readData2 = rf_6;
      4'b0111 : _zz_io_readData2 = rf_7;
      4'b1000 : _zz_io_readData2 = rf_8;
      4'b1001 : _zz_io_readData2 = rf_9;
      4'b1010 : _zz_io_readData2 = rf_10;
      4'b1011 : _zz_io_readData2 = rf_11;
      4'b1100 : _zz_io_readData2 = rf_12;
      4'b1101 : _zz_io_readData2 = rf_13;
      4'b1110 : _zz_io_readData2 = rf_14;
      default : _zz_io_readData2 = rf_15;
    endcase
  end

  assign when_23060082_l123 = (io_writeEn && (io_writeAddr[3 : 0] != 4'b0000));
  assign _zz_1 = ({15'd0,1'b1} <<< io_writeAddr[3 : 0]);
  assign io_readData1 = _zz_io_readData1;
  assign io_readData2 = _zz_io_readData2;
  always @(posedge clock) begin
    rf_0 <= 32'h0;
    if(when_23060082_l123) begin
      if(_zz_1[0]) begin
        rf_0 <= io_writeData;
      end
      if(_zz_1[1]) begin
        rf_1 <= io_writeData;
      end
      if(_zz_1[2]) begin
        rf_2 <= io_writeData;
      end
      if(_zz_1[3]) begin
        rf_3 <= io_writeData;
      end
      if(_zz_1[4]) begin
        rf_4 <= io_writeData;
      end
      if(_zz_1[5]) begin
        rf_5 <= io_writeData;
      end
      if(_zz_1[6]) begin
        rf_6 <= io_writeData;
      end
      if(_zz_1[7]) begin
        rf_7 <= io_writeData;
      end
      if(_zz_1[8]) begin
        rf_8 <= io_writeData;
      end
      if(_zz_1[9]) begin
        rf_9 <= io_writeData;
      end
      if(_zz_1[10]) begin
        rf_10 <= io_writeData;
      end
      if(_zz_1[11]) begin
        rf_11 <= io_writeData;
      end
      if(_zz_1[12]) begin
        rf_12 <= io_writeData;
      end
      if(_zz_1[13]) begin
        rf_13 <= io_writeData;
      end
      if(_zz_1[14]) begin
        rf_14 <= io_writeData;
      end
      if(_zz_1[15]) begin
        rf_15 <= io_writeData;
      end
    end
  end


endmodule

module ysyx_23060082_CSR (
  input  wire [11:0]   io_csr_addr,
  input  wire [31:0]   io_csr_wdata,
  output wire [31:0]   io_csr_rdata,
  input  wire [2:0]    io_csr_cmd,
  input  wire          io_trap_enter,
  input  wire          io_trap_exit,
  input  wire [31:0]   io_pc_in,
  input  wire [31:0]   io_cause_in,
  output wire [31:0]   io_mtvec,
  output wire [31:0]   io_mepc,
  input  wire          clock,
  input  wire          reset
);

  reg        [31:0]   mstatus;
  reg        [31:0]   mtvec;
  reg        [31:0]   mepc;
  reg        [31:0]   mcause;
  wire       [31:0]   mvendorid;
  wire       [31:0]   marchid;
  reg        [31:0]   _zz_io_csr_rdata;
  wire                writeEnable;
  reg        [31:0]   writeData;

  assign mvendorid = 32'h79737978;
  assign marchid = 32'h015fde72;
  always @(*) begin
    case(io_csr_addr)
      12'h300 : begin
        _zz_io_csr_rdata = mstatus;
      end
      12'h305 : begin
        _zz_io_csr_rdata = mtvec;
      end
      12'h341 : begin
        _zz_io_csr_rdata = mepc;
      end
      12'h342 : begin
        _zz_io_csr_rdata = mcause;
      end
      12'hf11 : begin
        _zz_io_csr_rdata = mvendorid;
      end
      12'hf12 : begin
        _zz_io_csr_rdata = marchid;
      end
      default : begin
        _zz_io_csr_rdata = 32'h0;
      end
    endcase
  end

  assign io_csr_rdata = _zz_io_csr_rdata;
  assign writeEnable = (io_csr_cmd != 3'b000);
  always @(*) begin
    case(io_csr_cmd)
      3'b001 : begin
        writeData = io_csr_wdata;
      end
      3'b010 : begin
        writeData = (io_csr_rdata | io_csr_wdata);
      end
      default : begin
        writeData = io_csr_rdata;
      end
    endcase
  end

  assign io_mtvec = mtvec;
  assign io_mepc = mepc;
  always @(posedge clock or posedge reset) begin
    if(reset) begin
      mstatus <= 32'h0;
      mtvec <= 32'h0;
      mepc <= 32'h0;
      mcause <= 32'h0;
    end else begin
      if(writeEnable) begin
        case(io_csr_addr)
          12'h300 : begin
            mstatus <= writeData;
          end
          12'h305 : begin
            mtvec <= writeData;
          end
          12'h341 : begin
            mepc <= writeData;
          end
          12'h342 : begin
            mcause <= writeData;
          end
          default : begin
          end
        endcase
      end
      if(io_trap_enter) begin
        mepc <= io_pc_in;
        mcause <= io_cause_in;
      end
    end
  end


endmodule

module ysyx_23060082_AXI_Ctrl (
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
  output reg  [3:0]    io_axi4_aw_payload_id,
  output reg  [7:0]    io_axi4_aw_payload_len,
  output reg  [2:0]    io_axi4_aw_payload_size,
  output reg  [1:0]    io_axi4_aw_payload_burst,
  output reg           io_axi4_w_valid,
  input  wire          io_axi4_w_ready,
  output reg  [31:0]   io_axi4_w_payload_data,
  output reg  [3:0]    io_axi4_w_payload_strb,
  output reg           io_axi4_w_payload_last,
  input  wire          io_axi4_b_valid,
  output wire          io_axi4_b_ready,
  input  wire [3:0]    io_axi4_b_payload_id,
  input  wire [1:0]    io_axi4_b_payload_resp,
  output reg           io_axi4_ar_valid,
  input  wire          io_axi4_ar_ready,
  output reg  [31:0]   io_axi4_ar_payload_addr,
  output reg  [3:0]    io_axi4_ar_payload_id,
  output reg  [7:0]    io_axi4_ar_payload_len,
  output reg  [2:0]    io_axi4_ar_payload_size,
  output reg  [1:0]    io_axi4_ar_payload_burst,
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
  wire                io_axi4_aw_fire;
  wire                io_axi4_w_fire;
  wire                io_axi4_b_fire;

  assign io_axi4_ar_fire = (io_axi4_ar_valid && io_axi4_ar_ready);
  assign io_axi4_r_ready = io_axi4_r_valid;
  assign io_axi4_r_fire = (io_axi4_r_valid && io_axi4_r_ready);
  assign io_readEnd = io_axi4_r_fire;
  assign io_readData = io_axi4_r_payload_data;
  assign io_axi4_aw_fire = (io_axi4_aw_valid && io_axi4_aw_ready);
  assign io_axi4_w_fire = (io_axi4_w_valid && io_axi4_w_ready);
  assign io_axi4_b_ready = io_axi4_b_valid;
  assign io_axi4_b_fire = (io_axi4_b_valid && io_axi4_b_ready);
  assign io_writeEnd = io_axi4_b_fire;
  always @(posedge clock or posedge reset) begin
    if(reset) begin
      io_axi4_ar_valid <= 1'b0;
      io_axi4_aw_valid <= 1'b0;
      io_axi4_w_valid <= 1'b0;
    end else begin
      if(io_readReq) begin
        io_axi4_ar_valid <= 1'b1;
      end else begin
        if(io_axi4_ar_fire) begin
          io_axi4_ar_valid <= 1'b0;
        end else begin
          io_axi4_ar_valid <= io_axi4_ar_valid;
        end
      end
      if(io_writeReq) begin
        io_axi4_aw_valid <= 1'b1;
      end else begin
        if(io_axi4_aw_fire) begin
          io_axi4_aw_valid <= 1'b0;
        end else begin
          io_axi4_aw_valid <= io_axi4_aw_valid;
        end
      end
      if(io_writeReq) begin
        io_axi4_w_valid <= 1'b1;
      end else begin
        if(io_axi4_w_fire) begin
          io_axi4_w_valid <= 1'b0;
        end else begin
          io_axi4_w_valid <= io_axi4_w_valid;
        end
      end
    end
  end

  always @(posedge clock) begin
    if(io_readReq) begin
      io_axi4_ar_payload_addr <= io_readAddr;
      io_axi4_ar_payload_id <= 4'b0000;
      io_axi4_ar_payload_len <= 8'h0;
      io_axi4_ar_payload_size <= io_size;
      io_axi4_ar_payload_burst <= 2'b01;
    end else begin
      io_axi4_ar_payload_addr <= io_axi4_ar_payload_addr;
      io_axi4_ar_payload_id <= io_axi4_ar_payload_id;
      io_axi4_ar_payload_len <= io_axi4_ar_payload_len;
      io_axi4_ar_payload_size <= io_axi4_ar_payload_size;
      io_axi4_ar_payload_burst <= io_axi4_ar_payload_burst;
    end
    if(io_writeReq) begin
      io_axi4_aw_payload_addr <= io_writeAddr;
      io_axi4_aw_payload_id <= 4'b0000;
      io_axi4_aw_payload_len <= 8'h0;
      io_axi4_aw_payload_size <= io_size;
      io_axi4_aw_payload_burst <= 2'b01;
    end else begin
      io_axi4_aw_payload_addr <= io_axi4_aw_payload_addr;
      io_axi4_aw_payload_id <= io_axi4_aw_payload_id;
      io_axi4_aw_payload_len <= io_axi4_aw_payload_len;
      io_axi4_aw_payload_size <= io_axi4_aw_payload_size;
      io_axi4_aw_payload_burst <= io_axi4_aw_payload_burst;
    end
    if(io_writeReq) begin
      io_axi4_w_payload_data <= io_writeData;
      io_axi4_w_payload_strb <= io_writeMask;
      io_axi4_w_payload_last <= 1'b1;
    end else begin
      io_axi4_w_payload_data <= io_axi4_w_payload_data;
      io_axi4_w_payload_strb <= io_axi4_w_payload_strb;
      io_axi4_w_payload_last <= io_axi4_w_payload_last;
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
        _zz_io_rdataReal = io_rdata;
      end
      5'h01 : begin
        _zz_io_rdataReal = _zz__zz_io_rdataReal;
      end
      5'h0 : begin
        _zz_io_rdataReal = _zz__zz_io_rdataReal_2;
      end
      5'h05 : begin
        _zz_io_rdataReal = {16'd0, _zz__zz_io_rdataReal_4};
      end
      5'h04 : begin
        _zz_io_rdataReal = {24'd0, _zz__zz_io_rdataReal_5};
      end
      5'h09 : begin
        _zz_io_rdataReal = _zz__zz_io_rdataReal_6;
      end
      5'h08 : begin
        _zz_io_rdataReal = _zz__zz_io_rdataReal_8;
      end
      5'h0d : begin
        _zz_io_rdataReal = {16'd0, _zz__zz_io_rdataReal_10};
      end
      5'h0c : begin
        _zz_io_rdataReal = {24'd0, _zz__zz_io_rdataReal_11};
      end
      5'h11 : begin
        _zz_io_rdataReal = _zz__zz_io_rdataReal_12;
      end
      5'h10 : begin
        _zz_io_rdataReal = _zz__zz_io_rdataReal_14;
      end
      5'h15 : begin
        _zz_io_rdataReal = {16'd0, _zz__zz_io_rdataReal_16};
      end
      5'h14 : begin
        _zz_io_rdataReal = {24'd0, _zz__zz_io_rdataReal_17};
      end
      5'h18 : begin
        _zz_io_rdataReal = _zz__zz_io_rdataReal_18;
      end
      5'h1c : begin
        _zz_io_rdataReal = {24'd0, _zz__zz_io_rdataReal_20};
      end
      default : begin
        _zz_io_rdataReal = 32'h0;
      end
    endcase
  end

  assign io_rdataReal = _zz_io_rdataReal;
  always @(*) begin
    case(io_addrOp)
      5'h02 : begin
        _zz_io_wdataReal = io_wdata;
      end
      5'h01 : begin
        _zz_io_wdataReal = {16'h0,io_wdata[15 : 0]};
      end
      5'h0 : begin
        _zz_io_wdataReal = {24'h0,io_wdata[7 : 0]};
      end
      5'h09 : begin
        _zz_io_wdataReal = {{8'h0,io_wdata[15 : 0]},8'h0};
      end
      5'h08 : begin
        _zz_io_wdataReal = {{16'h0,io_wdata[7 : 0]},8'h0};
      end
      5'h11 : begin
        _zz_io_wdataReal = {io_wdata[15 : 0],16'h0};
      end
      5'h10 : begin
        _zz_io_wdataReal = {{8'h0,io_wdata[7 : 0]},16'h0};
      end
      5'h18 : begin
        _zz_io_wdataReal = {io_wdata[7 : 0],24'h0};
      end
      default : begin
        _zz_io_wdataReal = 32'h0;
      end
    endcase
  end

  assign io_wdataReal = _zz_io_wdataReal;
  always @(*) begin
    case(io_addrOp)
      5'h02 : begin
        _zz_io_wmask = 4'b1111;
      end
      5'h01 : begin
        _zz_io_wmask = 4'b0011;
      end
      5'h0 : begin
        _zz_io_wmask = 4'b0001;
      end
      5'h09 : begin
        _zz_io_wmask = 4'b0110;
      end
      5'h08 : begin
        _zz_io_wmask = 4'b0010;
      end
      5'h11 : begin
        _zz_io_wmask = 4'b1100;
      end
      5'h10 : begin
        _zz_io_wmask = 4'b0100;
      end
      5'h18 : begin
        _zz_io_wmask = 4'b1000;
      end
      default : begin
        _zz_io_wmask = 4'b0000;
      end
    endcase
  end

  assign io_wmask = _zz_io_wmask;

endmodule

module ysyx_23060082_BranchCond (
  input  wire [2:0]    io_branch,
  input  wire          io_less,
  input  wire          io_zero,
  output wire          io_pc_asrc,
  output wire          io_pc_bsrc
);

  reg                 _zz_io_pc_asrc;

  always @(*) begin
    case(io_branch)
      3'b001 : begin
        _zz_io_pc_asrc = 1'b1;
      end
      3'b010 : begin
        _zz_io_pc_asrc = 1'b1;
      end
      3'b100 : begin
        _zz_io_pc_asrc = io_zero;
      end
      3'b101 : begin
        _zz_io_pc_asrc = (! io_zero);
      end
      3'b110 : begin
        _zz_io_pc_asrc = io_less;
      end
      3'b111 : begin
        _zz_io_pc_asrc = (! io_less);
      end
      default : begin
        _zz_io_pc_asrc = 1'b0;
      end
    endcase
  end

  assign io_pc_asrc = _zz_io_pc_asrc;
  assign io_pc_bsrc = (io_branch == 3'b010);

endmodule

module ysyx_23060082_ALU (
  input  wire [31:0]   io_aluIn1,
  input  wire [31:0]   io_aluIn2,
  input  wire [3:0]    io_aluCtr,
  output wire          io_less,
  output wire          io_zero,
  output wire [31:0]   io_aluResult
);

  wire       [32:0]   _zz_adder_result_33;
  wire       [32:0]   _zz_adder_result_33_1;
  wire       [32:0]   _zz_adder_result_33_2;
  wire       [32:0]   _zz_adder_result_33_3;
  wire       [31:0]   _zz_result_shift;
  wire       [31:0]   _zz_result_shift_1;
  wire       [0:0]    _zz_result_slt;
  wire                sub_add;
  wire       [31:0]   adder_dat_b;
  wire       [0:0]    adder_cin;
  wire       [32:0]   adder_result_33;
  wire       [31:0]   result_adder;
  wire                carry;
  wire                zero;
  wire                overflow;
  wire       [1:0]    switch_Misc_l245;
  reg        [31:0]   result_shift;
  wire                less_0;
  wire                less_1;
  wire                less;
  wire       [31:0]   result_slt;
  wire       [31:0]   result_xor;
  wire       [31:0]   result_or;
  wire       [31:0]   result_and;
  wire       [2:0]    switch_Misc_l245_1;
  reg        [31:0]   _zz_io_aluResult;

  assign _zz_adder_result_33 = (_zz_adder_result_33_1 + _zz_adder_result_33_2);
  assign _zz_adder_result_33_1 = {1'd0, io_aluIn1};
  assign _zz_adder_result_33_2 = {1'd0, adder_dat_b};
  assign _zz_adder_result_33_3 = {32'd0, adder_cin};
  assign _zz_result_shift = ($signed(_zz_result_shift_1) >>> io_aluIn2[4 : 0]);
  assign _zz_result_shift_1 = io_aluIn1;
  assign _zz_result_slt = less;
  assign sub_add = (io_aluCtr[1] || io_aluCtr[3]);
  assign adder_dat_b = (sub_add ? (~ io_aluIn2) : io_aluIn2);
  assign adder_cin = sub_add;
  assign adder_result_33 = (_zz_adder_result_33 + _zz_adder_result_33_3);
  assign result_adder = adder_result_33[31 : 0];
  assign carry = adder_result_33[32];
  assign zero = (result_adder == 32'h0);
  assign overflow = ((io_aluIn1[31] == adder_dat_b[31]) && (result_adder[31] != io_aluIn1[31]));
  assign switch_Misc_l245 = io_aluCtr[3 : 2];
  always @(*) begin
    case(switch_Misc_l245)
      2'b01 : begin
        result_shift = (io_aluIn1 >>> io_aluIn2[4 : 0]);
      end
      2'b11 : begin
        result_shift = _zz_result_shift;
      end
      default : begin
        result_shift = (io_aluIn1 <<< io_aluIn2[4 : 0]);
      end
    endcase
  end

  assign less_0 = (overflow ^ result_adder[31]);
  assign less_1 = (carry ^ sub_add);
  assign less = (io_aluCtr[3] ? less_1 : less_0);
  assign result_slt = {31'd0, _zz_result_slt};
  assign result_xor = (io_aluIn1 ^ io_aluIn2);
  assign result_or = (io_aluIn1 | io_aluIn2);
  assign result_and = (io_aluIn1 & io_aluIn2);
  assign io_less = less;
  assign io_zero = zero;
  assign switch_Misc_l245_1 = io_aluCtr[2 : 0];
  always @(*) begin
    case(switch_Misc_l245_1)
      3'b000 : begin
        _zz_io_aluResult = result_adder;
      end
      3'b001 : begin
        _zz_io_aluResult = result_shift;
      end
      3'b010 : begin
        _zz_io_aluResult = result_slt;
      end
      3'b011 : begin
        _zz_io_aluResult = io_aluIn2;
      end
      3'b100 : begin
        _zz_io_aluResult = result_xor;
      end
      3'b101 : begin
        _zz_io_aluResult = result_shift;
      end
      3'b110 : begin
        _zz_io_aluResult = result_or;
      end
      default : begin
        _zz_io_aluResult = result_and;
      end
    endcase
  end

  assign io_aluResult = _zz_io_aluResult;

endmodule

module Decoder (
  input  wire [31:0]   io_instr,
  output wire          io_ctrl_rf_si_mem2reg,
  output wire          io_ctrl_rf_si_csr2reg,
  output wire          io_ctrl_rf_si_regWr,
  output wire [4:0]    io_ctrl_rf_si_rf_write_addr,
  output wire          io_ctrl_alu_si_alu_asrc,
  output wire [1:0]    io_ctrl_alu_si_alu_bsrc,
  output wire [3:0]    io_ctrl_alu_si_alu_ctr,
  output wire [2:0]    io_ctrl_alu_si_branch,
  output wire          io_ctrl_mem_si_memWr,
  output wire [2:0]    io_ctrl_mem_si_memOp,
  output wire [2:0]    io_ctrl_csr_si_csr_cmd,
  output wire          io_ctrl_csr_si_trap_enter,
  output wire          io_ctrl_csr_si_trap_exit,
  output wire [31:0]   io_imm
);

  wire       [3:0]    _zz_io_ctrl_alu_si_alu_ctr_7;
  wire       [3:0]    _zz_io_ctrl_alu_si_alu_ctr_8;
  wire       [3:0]    _zz_io_ctrl_alu_si_alu_ctr_9;
  wire       [3:0]    _zz_io_ctrl_alu_si_alu_ctr_10;
  wire       [3:0]    _zz_io_ctrl_alu_si_alu_ctr_11;
  wire       [3:0]    _zz_io_ctrl_alu_si_alu_ctr_12;
  wire       [3:0]    _zz_io_ctrl_alu_si_alu_ctr_13;
  wire       [3:0]    _zz_io_ctrl_alu_si_alu_ctr_14;
  wire                _zz_io_ctrl_alu_si_alu_ctr_15;
  wire       [31:0]   i;
  wire       [6:0]    op;
  wire       [2:0]    func3;
  wire       [6:0]    func7;
  wire                i_sub;
  wire                i_sll;
  wire                i_slt;
  wire                i_sltu;
  wire                i_xor;
  wire                i_srl;
  wire                i_sra;
  wire                i_or;
  wire                i_and;
  wire                i_slli;
  wire                i_slti;
  wire                i_sltiu;
  wire                i_xori;
  wire                i_srli;
  wire                i_srai;
  wire                i_ori;
  wire                i_andi;
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
  wire                type_U;
  wire                type_J;
  wire                type_I;
  wire                type_S;
  wire                type_B;
  wire                type_R;
  wire                type_N;
  wire       [31:0]   immU;
  wire       [31:0]   immJ;
  wire       [31:0]   immI;
  wire       [31:0]   immS;
  wire       [31:0]   immB;
  wire                _zz_io_imm;
  wire                _zz_io_ctrl_alu_si_alu_ctr;
  wire                _zz_io_ctrl_alu_si_alu_ctr_1;
  wire                _zz_io_ctrl_alu_si_alu_ctr_2;
  wire                _zz_io_ctrl_alu_si_alu_ctr_3;
  wire                _zz_io_ctrl_alu_si_alu_ctr_4;
  wire                _zz_io_ctrl_alu_si_alu_ctr_5;
  wire                _zz_io_ctrl_alu_si_alu_ctr_6;
  wire                _zz_io_ctrl_alu_si_branch;
  wire                _zz_io_ctrl_alu_si_branch_1;

  assign _zz_io_ctrl_alu_si_alu_ctr_7 = 4'b0111;
  assign _zz_io_ctrl_alu_si_alu_ctr_8 = 4'b0110;
  assign _zz_io_ctrl_alu_si_alu_ctr_9 = 4'b0100;
  assign _zz_io_ctrl_alu_si_alu_ctr_10 = 4'b0001;
  assign _zz_io_ctrl_alu_si_alu_ctr_11 = 4'b0101;
  assign _zz_io_ctrl_alu_si_alu_ctr_12 = 4'b1101;
  assign _zz_io_ctrl_alu_si_alu_ctr_13 = 4'b1000;
  assign _zz_io_ctrl_alu_si_alu_ctr_14 = 4'b0011;
  assign _zz_io_ctrl_alu_si_alu_ctr_15 = ((i_sltu || i_sltiu) || i_bltu);
  MyEbreak my_ebreak (
    .i_ebreak (i_ebreak)  //i
  );
  assign i = io_instr;
  assign op = io_instr[6 : 0];
  assign func3 = io_instr[14 : 12];
  assign func7 = io_instr[31 : 25];
  assign io_ctrl_rf_si_rf_write_addr = io_instr[11 : 7];
  assign i_sub = ((i & 32'hfe00707f) == 32'h40000033);
  assign i_sll = ((i & 32'hfe00707f) == 32'h00001033);
  assign i_slt = ((i & 32'hfe00707f) == 32'h00002033);
  assign i_sltu = ((i & 32'hfe00707f) == 32'h00003033);
  assign i_xor = ((i & 32'hfe00707f) == 32'h00004033);
  assign i_srl = ((i & 32'hfe00707f) == 32'h00005033);
  assign i_sra = ((i & 32'hfe00707f) == 32'h40005033);
  assign i_or = ((i & 32'hfe00707f) == 32'h00006033);
  assign i_and = ((i & 32'hfe00707f) == 32'h00007033);
  assign i_slli = ((i & 32'hfe00707f) == 32'h00001013);
  assign i_slti = ((i & 32'h0000707f) == 32'h00002013);
  assign i_sltiu = ((i & 32'h0000707f) == 32'h00003013);
  assign i_xori = ((i & 32'h0000707f) == 32'h00004013);
  assign i_srli = ((i & 32'hfe00707f) == 32'h00005013);
  assign i_srai = ((i & 32'hfe00707f) == 32'h40005013);
  assign i_ori = ((i & 32'h0000707f) == 32'h00006013);
  assign i_andi = ((i & 32'h0000707f) == 32'h00007013);
  assign i_beq = ((i & 32'h0000707f) == 32'h00000063);
  assign i_bne = ((i & 32'h0000707f) == 32'h00001063);
  assign i_blt = ((i & 32'h0000707f) == 32'h00004063);
  assign i_bge = ((i & 32'h0000707f) == 32'h00005063);
  assign i_bltu = ((i & 32'h0000707f) == 32'h00006063);
  assign i_bgeu = ((i & 32'h0000707f) == 32'h00007063);
  assign i_jalr = ((i & 32'h0000707f) == 32'h00000067);
  assign i_jal = ((i & 32'h0000007f) == 32'h0000006f);
  assign i_lui = ((i & 32'h0000007f) == 32'h00000037);
  assign i_auipc = ((i & 32'h0000007f) == 32'h00000017);
  assign i_csrrw = ((i & 32'h0000707f) == 32'h00001073);
  assign i_csrrs = ((i & 32'h0000707f) == 32'h00002073);
  assign i_ecall = ((i & 32'hffffffff) == 32'h00000073);
  assign i_ebreak = ((i & 32'hffffffff) == 32'h00100073);
  assign i_mret = ((i & 32'hffffffff) == 32'h30200073);
  assign i_fence_i = ((i & 32'h0000707f) == 32'h0000100f);
  assign type_U = (op[4 : 2] == 3'b101);
  assign type_J = (op[6 : 2] == 5'h1b);
  assign type_I = ((((op[6 : 2] == 5'h04) || (op[6 : 2] == 5'h0)) || (op[6 : 2] == 5'h19)) || ((op[6 : 2] == 5'h1c) && (func3 != 3'b000)));
  assign type_S = (op[6 : 2] == 5'h08);
  assign type_B = (op[6 : 2] == 5'h18);
  assign type_R = (op[6 : 2] == 5'h0c);
  assign type_N = (((op[6 : 2] == 5'h1c) && (func3 == 3'b000)) || (op[6 : 2] == 5'h03));
  assign immU = {io_instr[31 : 12],12'h0};
  assign immJ = {{{{{12{io_instr[31]}},io_instr[19 : 12]},io_instr[20]},io_instr[30 : 21]},1'b0};
  assign immI = {{20{io_instr[31]}},io_instr[31 : 20]};
  assign immS = {{{20{io_instr[31]}},io_instr[31 : 25]},io_instr[11 : 7]};
  assign immB = {{{{{20{io_instr[31]}},io_instr[7]},io_instr[30 : 25]},io_instr[11 : 8]},1'b0};
  assign _zz_io_imm = (type_U || type_J);
  assign io_imm = ((_zz_io_imm || (type_I || type_S)) ? (_zz_io_imm ? (type_U ? immU : immJ) : (type_I ? immI : immS)) : (type_B ? immB : 32'h0));
  assign io_ctrl_rf_si_regWr = (((type_U || type_J) || type_I) || type_R);
  assign io_ctrl_alu_si_alu_asrc = ((i_auipc || i_jal) || i_jalr);
  assign io_ctrl_alu_si_alu_bsrc = ((type_R || type_B) ? 2'b00 : ((i_jal || i_jalr) ? 2'b10 : 2'b01));
  assign _zz_io_ctrl_alu_si_alu_ctr = (i_and || i_andi);
  assign _zz_io_ctrl_alu_si_alu_ctr_1 = (i_xor || i_xori);
  assign _zz_io_ctrl_alu_si_alu_ctr_2 = (i_srl || i_srli);
  assign _zz_io_ctrl_alu_si_alu_ctr_3 = (((((i_slt || i_slti) || i_beq) || i_bne) || i_blt) || i_bge);
  assign _zz_io_ctrl_alu_si_alu_ctr_4 = (_zz_io_ctrl_alu_si_alu_ctr || (i_or || i_ori));
  assign _zz_io_ctrl_alu_si_alu_ctr_5 = (_zz_io_ctrl_alu_si_alu_ctr_2 || (i_sra || i_srai));
  assign _zz_io_ctrl_alu_si_alu_ctr_6 = (_zz_io_ctrl_alu_si_alu_ctr_4 || (_zz_io_ctrl_alu_si_alu_ctr_1 || (i_sll || i_slli)));
  assign io_ctrl_alu_si_alu_ctr = ((_zz_io_ctrl_alu_si_alu_ctr_6 || (_zz_io_ctrl_alu_si_alu_ctr_5 || (i_sub || i_lui))) ? (_zz_io_ctrl_alu_si_alu_ctr_6 ? (_zz_io_ctrl_alu_si_alu_ctr_4 ? (_zz_io_ctrl_alu_si_alu_ctr ? _zz_io_ctrl_alu_si_alu_ctr_7 : _zz_io_ctrl_alu_si_alu_ctr_8) : (_zz_io_ctrl_alu_si_alu_ctr_1 ? _zz_io_ctrl_alu_si_alu_ctr_9 : _zz_io_ctrl_alu_si_alu_ctr_10)) : (_zz_io_ctrl_alu_si_alu_ctr_5 ? (_zz_io_ctrl_alu_si_alu_ctr_2 ? _zz_io_ctrl_alu_si_alu_ctr_11 : _zz_io_ctrl_alu_si_alu_ctr_12) : (i_sub ? _zz_io_ctrl_alu_si_alu_ctr_13 : _zz_io_ctrl_alu_si_alu_ctr_14))) : ((_zz_io_ctrl_alu_si_alu_ctr_3 || (_zz_io_ctrl_alu_si_alu_ctr_15 || i_bgeu)) ? (_zz_io_ctrl_alu_si_alu_ctr_3 ? 4'b0010 : 4'b1010) : 4'b0000));
  assign _zz_io_ctrl_alu_si_branch = (i_blt || i_bltu);
  assign _zz_io_ctrl_alu_si_branch_1 = (i_jal || i_jalr);
  assign io_ctrl_alu_si_branch = ((_zz_io_ctrl_alu_si_branch_1 || (i_beq || i_bne)) ? (_zz_io_ctrl_alu_si_branch_1 ? (i_jal ? 3'b001 : 3'b010) : (i_beq ? 3'b100 : 3'b101)) : ((_zz_io_ctrl_alu_si_branch || (i_bge || i_bgeu)) ? (_zz_io_ctrl_alu_si_branch ? 3'b110 : 3'b111) : 3'b000));
  assign io_ctrl_rf_si_mem2reg = (op[6 : 2] == 5'h0);
  assign io_ctrl_rf_si_csr2reg = (i_csrrw || i_csrrs);
  assign io_ctrl_mem_si_memWr = type_S;
  assign io_ctrl_mem_si_memOp = func3;
  assign io_ctrl_csr_si_csr_cmd = (i_csrrw ? 3'b001 : (i_csrrs ? 3'b010 : 3'b000));
  assign io_ctrl_csr_si_trap_enter = i_ecall;
  assign io_ctrl_csr_si_trap_exit = i_mret;

endmodule

module ysyx_23060082_AXI_Ctrl_ReadOnly (
  input  wire          io_readReq,
  input  wire [31:0]   io_readAddr,
  output wire          io_readEnd,
  output wire [31:0]   io_readData,
  output reg           io_axi4_ar_valid,
  input  wire          io_axi4_ar_ready,
  output reg  [31:0]   io_axi4_ar_payload_addr,
  output reg  [3:0]    io_axi4_ar_payload_id,
  output reg  [7:0]    io_axi4_ar_payload_len,
  output reg  [2:0]    io_axi4_ar_payload_size,
  output reg  [1:0]    io_axi4_ar_payload_burst,
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

  assign io_axi4_ar_fire = (io_axi4_ar_valid && io_axi4_ar_ready);
  assign io_axi4_r_ready = io_axi4_r_valid;
  assign io_axi4_r_fire = (io_axi4_r_valid && io_axi4_r_ready);
  assign io_readEnd = io_axi4_r_fire;
  assign io_readData = io_axi4_r_payload_data;
  always @(posedge clock or posedge reset) begin
    if(reset) begin
      io_axi4_ar_valid <= 1'b0;
    end else begin
      if(io_readReq) begin
        io_axi4_ar_valid <= 1'b1;
      end else begin
        if(io_axi4_ar_fire) begin
          io_axi4_ar_valid <= 1'b0;
        end else begin
          io_axi4_ar_valid <= io_axi4_ar_valid;
        end
      end
    end
  end

  always @(posedge clock) begin
    if(io_readReq) begin
      io_axi4_ar_payload_addr <= io_readAddr;
      io_axi4_ar_payload_id <= 4'b0000;
      io_axi4_ar_payload_len <= 8'h0;
      io_axi4_ar_payload_size <= 3'b010;
      io_axi4_ar_payload_burst <= 2'b01;
    end else begin
      io_axi4_ar_payload_addr <= io_axi4_ar_payload_addr;
      io_axi4_ar_payload_id <= io_axi4_ar_payload_id;
      io_axi4_ar_payload_len <= io_axi4_ar_payload_len;
      io_axi4_ar_payload_size <= io_axi4_ar_payload_size;
      io_axi4_ar_payload_burst <= io_axi4_ar_payload_burst;
    end
  end


endmodule
