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

case class NpcMemRW() extends BlackBox{
  val io=new Bundle{
    val clocka = in Bool()
    val reseta = in Bool()
    val valid = in Bool()
    val wen   = in Bool()
    val addr  = in UInt(32 bits)
    val wdata = in UInt(32 bits)
    val wmask = in UInt(4 bits)
    val rdata = out UInt(32 bits)
  }
  noIoPrefix()
  mapClockDomain(clock = io.clocka,reset = io.reseta)
  addRTLPath(s"${sys.env("NPC_HOME")}/playground/vsrc/dpi-c.v")   
}
