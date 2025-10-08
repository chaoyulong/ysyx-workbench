package playground

import spinal.core._

object Mem_rw{
  def apply(valid:Bool, wen:Bool, addr:UInt, wdata:UInt, wmask:UInt): Mem_rw ={
    val memory = new Mem_rw()
    memory.io.valid <> valid
    memory.io.wen   <> wen  
    memory.io.addr  <> addr 
    memory.io.wdata <> wdata
    memory.io.wmask <> wmask
    rdata
  }
}
class Mem_rw extends BlackBox{
  val io=new Bundle{
    val clock = in Bool
    val reset = in Bool
    val valid = in Bool
    val wen   = in Bool
    val addr  = in UInt(32 bits)
    val wdata = in UInt(32 bits)
    val wmask = in UInt(4 bits)
    val rdata = out UInt(32 bits)
  }
  noIoPrefix()
  mapClockDomain(clock = io.clock,reset = io.reset)
  addRTLPath(s"${sys.env("NPC_HOME")}/playground/verilog/memory.v")   
  
}
