package playground

import spinal.core._


case class MyEbreak() extends BlackBox{
  val io = new Bundle{
    val i_ebreak = in Bool()
  }
  noIoPrefix()
  addRTLPath(s"${sys.env("NPC_HOME")}/playground/vsrc/dpi-c.v")   
}

case class GetInstr() extends BlackBox{
  val io = new Bundle{
    val pc_o = in UInt(32 bits)
    val instr = in UInt(32 bits)
  }
  noIoPrefix()
  addRTLPath(s"${sys.env("NPC_HOME")}/playground/vsrc/dpi-c.v")   
}

// object Mem_Rd{
//   def apply(addr:UInt): UInt ={
//     val memory = new Mem_Rd()
//     memory.io.addr  <> addr 
//     memory.io.rdata
//   }
// }
case class Mem_Rd() extends BlackBox{
  val io=new Bundle{
    val clock = in Bool()
    val reset = in Bool()
    val rd_en = in Bool()
    val addr  = in UInt(32 bits)
    val rd_end = out Bool() 
    val rdata = out UInt(32 bits)
  }
  noIoPrefix()
  mapClockDomain(clock = io.clock,reset = io.reset)
  addRTLPath(s"${sys.env("NPC_HOME")}/playground/vsrc/dpi-c.v")   
}


// object Mem_RW{
//   def apply(valid:Bool, wen:Bool, addr:UInt, wdata:UInt, wmask:UInt): UInt ={
//     val memory = new Mem_RW()
//     memory.io.valid <> valid
//     memory.io.wen   <> wen  
//     memory.io.addr  <> addr 
//     memory.io.wdata <> wdata
//     memory.io.wmask <> wmask
//     memory.io.rdata
//   }
// }
case class Mem_RW() extends BlackBox{
  val io=new Bundle{
    val clock = in Bool()
    val reset = in Bool()
    val valid = in Bool()
    val wen   = in Bool()
    val addr  = in UInt(32 bits)
    val wdata = in UInt(32 bits)
    val wmask = in UInt(4 bits)
    val rw_end = out Bool()
    val rdata = out UInt(32 bits)
  }
  noIoPrefix()
  mapClockDomain(clock = io.clock,reset = io.reset)
  addRTLPath(s"${sys.env("NPC_HOME")}/playground/vsrc/dpi-c.v")   
}


