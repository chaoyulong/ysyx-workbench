import "DPI-C" function int pmem_read(input int raddr);
import "DPI-C" function void pmem_write(input int waddr, input int wdata, input byte wmask);
import "DPI-C" function void my_ebreak();
import "DPI-C" function void get_instr(int pc_o, int instr);

module Mem_Rd(
  input             clock,
  input             reset,
  input             rd_req,
  input      [31:0] addr,
  output reg        rd_end,
  output reg [31:0] rdata
);

  always @(posedge clock or posedge reset) begin
    if (reset) begin 
      rdata <= 32'h0;
    end
    else if(rd_req) begin
      rdata <= pmem_read(addr);
    end
  end

  always @(posedge clock or posedge reset) begin
    if (reset) begin 
      rd_end <= 1'b0;
    end
    else if(rd_req) begin
      rd_end <= 1'b1;
    end
    else begin
      rd_end <= 1'b0;
    end 
  end

endmodule

module Mem_RW(
  input             clock,
  input             reset,
  input             valid,
  input             wen,
  input      [31:0] addr,
  input      [31:0] wdata,
  input      [3:0]  wmask,
  output reg        rw_end,
  output reg [31:0] rdata
);

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

  always @(posedge clock or posedge reset) begin
    if (reset) begin 
      rw_end <= 1'b0;
    end
    else if(valid) begin
      rw_end <= 1'b1;
    end
    else begin
      rw_end <= 1'b0;
    end 
  end
  
endmodule

module MyEbreak(
  input i_ebreak
);

always @(*) begin
  if(i_ebreak)
    my_ebreak();
end

endmodule

module GetInstr(
  input [31:0] pc_o,
  input [31:0] instr
);

always @(*) begin
  get_instr(pc_o, instr);
end

endmodule