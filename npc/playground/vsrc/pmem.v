
module Mem_rw(
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
  
  always @(*) begin
    if (valid) begin // 有读写请求时
      $display("mem read at %h", addr);
      // rdata = pmem_read(addr);
      // if (wen) begin // 有写请求时
      //   pmem_write(addr, wdata, {4'b0, wmask});
      // end
      // rdata = 32'h01c50513;
    end
    else begin
      rdata = 0;
    end
  end

endmodule