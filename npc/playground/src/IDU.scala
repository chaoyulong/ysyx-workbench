package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

case class Idu2Exu_data() extends Bundle {
  val pc    = UInt(32 bits)
  val instr = UInt(32 bits)

  val ctrl = Ctrl()
  val rf_read_data_1 =  UInt(32 bits)
  val rf_read_data_2 =  UInt(32 bits)
}

case class ysyx_23060082_IDU() extends Component {
  val io = new Bundle {
    val from_Ifu  = slave Stream(Ifu2Idu_data())
    val to_Exu = master Stream(Idu2Exu_data()) 

    val rf_read_addr_1 = out UInt(5 bits)
    val rf_read_addr_2 = out UInt(5 bits)
    val rf_read_data_1 = in  UInt(32 bits)
    val rf_read_data_2 = in  UInt(32 bits)
  }

  val idu_pc  = io.from_Ifu.pc
  val decoder = Decoder()

  decoder.instr := io.from_Ifu.instr         

  io.rf_read_addr_1 := io.from_Ifu.instr(19 downto 15)
  io.rf_read_addr_2 := io.from_Ifu.instr(24 downto 20)  

  io.to_Exu.payload.rf_read_data_1 := io.rf_read_data_1
  io.to_Exu.payload.rf_read_data_2 := io.rf_read_data_2
  io.to_Exu.payload.ctrl           := decoder.io.ctrl
  // io.idu_read_rf.addr_w := instr(11 downto 7)

  io.from_Ifu.ready := io.from_Ifu.valid
  
}