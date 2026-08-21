

// ----------------------------------- 内存读写 ----------------------------------- //
module NpcMemRW(
  input             clock,
  input             reset,
  input             valid,
  input             wen,
  input      [31:0] addr,
  input      [31:0] wdata,
  input      [3:0]  wmask,
  output reg [31:0] rdata
);
  import "DPI-C" function int pmem_read(input int raddr);
  import "DPI-C" function void pmem_write(input int waddr, input int wdata, input byte wmask);
  always @(posedge clock or posedge reset) begin
    if (reset) begin 
      rdata <= 32'h0;
    end
    else if(valid) begin
      if(wen) begin   // 写
        pmem_write(addr, wdata, {4'b0, wmask});
      end
      else begin
        rdata <= pmem_read(addr);
      end
    end
  end
endmodule

// ------------------- 仿真专用: itrace 指令退休追踪 (仅仿真, 综合不实例化) ------------------- //
// 由 SpinalHDL 在 enableSimDebug 时实例化, 寄存器供 C++ 侧直接读取
module ItraceReg(
  input             clock,
  input             reset,
  input             valid,
  input      [31:0] pc,
  input      [31:0] instr
);
  reg [31:0] itraceRetirePc;
  reg [31:0] itraceRetireInstr;
  reg        itraceRetireValid;

  always @(posedge clock or posedge reset) begin
    if (reset) begin
      itraceRetirePc     <= 32'h0;
      itraceRetireInstr  <= 32'h0;
      itraceRetireValid  <= 1'b0;
    end
    else if (valid) begin
      itraceRetirePc     <= pc;
      itraceRetireInstr  <= instr;
      itraceRetireValid  <= 1'b1;
    end
    else begin
      itraceRetireValid  <= 1'b0;
    end
  end
endmodule

// ------------------- 仿真专用: mtrace 访存踪迹 (仅仿真, 综合不实例化) ------------------- //
// LSU 每次数据访存(load/store)时 valid 拉高, 锁存 wen/pc/addr/wdata, 计数器递增
// C 侧通过比较 mtraceCnt 判断是否有新访存, 不丢事件
module MtraceReg(
  input             clock,
  input             reset,
  input             valid,
  input             wen,
  input      [31:0] pc,
  input      [31:0] addr,
  input      [31:0] wdata
);
  reg [63:0] mtraceCnt;
  reg        mtraceWen;
  reg [31:0] mtracePc;
  reg [31:0] mtraceAddr;
  reg [31:0] mtraceWdata;

  always @(posedge clock or posedge reset) begin
    if (reset) begin
      mtraceCnt    <= 64'h0;
      mtraceWen    <= 1'b0;
      mtracePc     <= 32'h0;
      mtraceAddr   <= 32'h0;
      mtraceWdata  <= 32'h0;
    end
    else if (valid) begin
      mtraceCnt    <= mtraceCnt + 1;
      mtraceWen    <= wen;
      mtracePc     <= pc;
      mtraceAddr   <= addr;
      mtraceWdata  <= wdata;
    end
  end
endmodule


