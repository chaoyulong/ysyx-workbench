

// ----------------------------------- 内存读写 ----------------------------------- //
module NpcMemRW(
  input             clock,
  input             reset,
  input             valid,
  input             wen,
  input      [31:0] waddr,
  input      [31:0] wdata,
  input      [3:0]  wmask,
  input      [31:0] raddr,
  output reg [31:0] rdata
);
  import "DPI-C" function int pmem_read(input int raddr);
  import "DPI-C" function void pmem_write(input int waddr, input int wdata, input byte wmask);
  always @(posedge clock) begin
    if (reset) begin
    end
    else if(valid) begin
      if(wen) begin   // 写
        pmem_write(waddr, wdata, {4'b0, wmask});
      end
    end
  end

  always @(posedge clock) begin
    if (reset) begin 
      rdata <= 32'h0;
    end
    else if(valid) begin
      rdata <= pmem_read(raddr);
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
  input      [31:0] pcNext,
  input      [31:0] instr,
  input             difftestSkip 
);
  reg [31:0] itraceRetirePc;
  reg [31:0] itraceRetirePcNext;
  reg [31:0] itraceRetireInstr;
  reg        itraceRetireValid;
  reg        itraceRetireSkip;

  always @(posedge clock or posedge reset) begin
    if (reset) begin
      itraceRetirePc     <= 32'h0;
      itraceRetirePcNext <= 32'h0;
      itraceRetireInstr  <= 32'h0;
      itraceRetireValid  <= 1'b0;
      itraceRetireSkip   <= 1'b0;
    end
    else if (valid) begin
      itraceRetirePc     <= pc;
      itraceRetirePcNext <= pcNext;
      itraceRetireInstr  <= instr;
      itraceRetireValid  <= 1'b1;
      itraceRetireSkip   <= difftestSkip;
    end
    else begin
      itraceRetireValid  <= 1'b0;
    end
  end

  reg ebreak;

  always @(posedge clock or posedge reset) begin
    if (reset) begin
      ebreak  <= 1'b0;
    end
    else if (valid) begin
      if(instr == 32'b00000000000100000000000001110011) begin
        ebreak  <= 1'b1;
      end
      else begin
        ebreak  <= 1'b0;
      end
    end
    else begin
      ebreak <= ebreak;
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
  input             isDev,
  input      [31:0] addr,
  input      [31:0] wdata
);
  reg [63:0] mtraceCnt;
  reg        mtraceWen;
  reg        mtraceIsDev;
  reg [31:0] mtraceAddr;
  reg [31:0] mtraceWdata;

  always @(posedge clock or posedge reset) begin
    if (reset) begin
      mtraceCnt    <= 64'h0;
      mtraceWen    <= 1'b0;
      mtraceIsDev  <= 1'b0;
      mtraceAddr   <= 32'h0;
      mtraceWdata  <= 32'h0;
    end
    else if (valid) begin
      mtraceCnt    <= mtraceCnt + 1;
      mtraceWen    <= wen;
      mtraceIsDev  <= isDev;
      mtraceAddr   <= addr;
      mtraceWdata  <= wdata;
    end
  end
endmodule



// ------------------- 仿真专用: PerfReg 性能计数器 (仅仿真, 综合不实例化) ------------------- //
// 通用计数器: 4 组延迟测量(req拍记时间 -> rsp拍累加延迟/次数) + 8 个事件计数(每拍+1) + 全局周期
// 各模块(IFU/LSU/EXU/IDU)按需实例化, C 侧读字段打印统计
module PerfReg(
  input        clock,
  input        reset,
  input        valid,      // 周期计数使能(恒1)
  input  [3:0] req,        // 4 组延迟请求拍
  input  [3:0] rsp,        // 4 组延迟响应拍
  input  [7:0] evt         // 8 个事件计数(每拍+1)
);
  reg [63:0] perfCyc;
  reg [63:0] dlyReqTime0, dlyReqTime1, dlyReqTime2, dlyReqTime3;
  reg [63:0] dlySum0, dlySum1, dlySum2, dlySum3;
  reg [63:0] dlyCnt0, dlyCnt1, dlyCnt2, dlyCnt3;
  reg [63:0] evtCnt0, evtCnt1, evtCnt2, evtCnt3;
  reg [63:0] evtCnt4, evtCnt5, evtCnt6, evtCnt7;

  always @(posedge clock or posedge reset) begin
    if (reset) begin
      perfCyc <= 64'h0;
      dlyReqTime0 <= 64'h0; dlyReqTime1 <= 64'h0; dlyReqTime2 <= 64'h0; dlyReqTime3 <= 64'h0;
      dlySum0 <= 64'h0; dlySum1 <= 64'h0; dlySum2 <= 64'h0; dlySum3 <= 64'h0;
      dlyCnt0 <= 64'h0; dlyCnt1 <= 64'h0; dlyCnt2 <= 64'h0; dlyCnt3 <= 64'h0;
      evtCnt0 <= 64'h0; evtCnt1 <= 64'h0; evtCnt2 <= 64'h0; evtCnt3 <= 64'h0;
      evtCnt4 <= 64'h0; evtCnt5 <= 64'h0; evtCnt6 <= 64'h0; evtCnt7 <= 64'h0;
    end
    else if (valid) begin
      perfCyc <= perfCyc + 1;
      if (req[0]) dlyReqTime0 <= perfCyc;
      if (req[1]) dlyReqTime1 <= perfCyc;
      if (req[2]) dlyReqTime2 <= perfCyc;
      if (req[3]) dlyReqTime3 <= perfCyc;
      if (rsp[0]) begin dlySum0 <= dlySum0 + (perfCyc - dlyReqTime0); dlyCnt0 <= dlyCnt0 + 1; end
      if (rsp[1]) begin dlySum1 <= dlySum1 + (perfCyc - dlyReqTime1); dlyCnt1 <= dlyCnt1 + 1; end
      if (rsp[2]) begin dlySum2 <= dlySum2 + (perfCyc - dlyReqTime2); dlyCnt2 <= dlyCnt2 + 1; end
      if (rsp[3]) begin dlySum3 <= dlySum3 + (perfCyc - dlyReqTime3); dlyCnt3 <= dlyCnt3 + 1; end
      if (evt[0]) evtCnt0 <= evtCnt0 + 1;
      if (evt[1]) evtCnt1 <= evtCnt1 + 1;
      if (evt[2]) evtCnt2 <= evtCnt2 + 1;
      if (evt[3]) evtCnt3 <= evtCnt3 + 1;
      if (evt[4]) evtCnt4 <= evtCnt4 + 1;
      if (evt[5]) evtCnt5 <= evtCnt5 + 1;
      if (evt[6]) evtCnt6 <= evtCnt6 + 1;
      if (evt[7]) evtCnt7 <= evtCnt7 + 1;
    end
  end
endmodule
