// --------------------------------- 程序运行结束 --------------------------------- //
module MyEbreak(
  input i_ebreak,
  input i_illegal
);
  import "DPI-C" function void my_ebreak();
  always @(*) begin
    if(i_illegal)
      $error("inst is illegal");  // 非法指令
    if(i_ebreak)
      my_ebreak();
  end
endmodule

// ---------------------------- 将取到的指令传入trice中 ---------------------------- //
module GetInstr(
  input [31:0] pc_o,
  input [31:0] instr
);
  import "DPI-C" function void get_instr(int pc_o, int instr);
  always @(*) begin
    get_instr(pc_o, instr);
  end
endmodule
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


