package playground

import spinal.core._

object Memory{
  def apply(valid:Bool, wen:Bool, addr:UInt, wdata:UInt, wmask:UInt, rdata:UInt): Mem_rw ={
    val memory = new Mem_rw()
    memory.io.valid <> valid
    memory.io.wen   <> wen  
    memory.io.addr  <> addr 
    memory.io.wdata <> wdata
    memory.io.wmask <> wmask
    memory.io.rdata <> rdata
    memory
  }
}
class Mem_rw extends BlackBox{
  val io=new Bundle{
    val clk   = in Bool
    val rst   = in Bool
    val valid = in Bool
    val wen   = in Bool
    val addr  = in UInt(32 bits)
    val wdata = in UInt(32 bits)
    val wmask = in UInt(4 bits)
    val rdata = out UInt(32 bits)
  }
  noIoPrefix()
  mapClockDomain(clock = io.clk,reset = io.rst)
  // addRTLPath("./verilog/memory.v")   
  addRTLPath(s"${sys.env("NPC_HOME")}/playground/verilog/memory.v")   
  
}
