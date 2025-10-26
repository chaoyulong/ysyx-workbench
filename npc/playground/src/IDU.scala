package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

case class Idu2Exu_data() extends Bundle {
  val pc    = UInt(32 bits)

  val ctrl = Ctrl()
  val rf_write_addr  = UInt(5 bits)
  val imm            = UInt(32 bits)
  val rf_read_data_1 = UInt(32 bits)
  val rf_read_data_2 = UInt(32 bits)
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
// ***************************************** 立即数生成 *********************************************** //
  val instr = io.from_Ifu.instr
  val immU = U(instr(31 downto 12) ## U(0, 12 bits))
  val immJ = U((instr(31) #* 12) ## instr(19 downto 12) ## instr(20) ## instr(30 downto 21)## U(0, 1 bits))
  val immI = U((instr(31) #* 20) ## instr(31 downto 20))
  val immS = U((instr(31) #* 20) ## instr(31 downto 25) ## instr(11 downto 7))
  val immB = U((instr(31) #* 20) ## instr(7) ## instr(30 downto 25) ## instr(11 downto 8) ## U(0, 1 bits))

  io.to_Exu.imm := decoder.io.instr_type.muxOH(
    B"100000" -> immU,
    B"010000" -> immJ,
    B"001000" -> immI,
    B"000100" -> immS,
    B"000010" -> immB,
    default -> U"32'h0"
  )
  // io.ctrl.imm := Mux(type_U, immU,
  //         Mux(type_J, immJ,
  //         Mux(type_I, immI,
  //         Mux(type_S, immS,
  //         Mux(type_B, immB, U(0, 32 bits))))))
// *****************************************  *********************************************** //
  io.rf_read_addr_1 := io.from_Ifu.instr(19 downto 15)
  io.rf_read_addr_2 := io.from_Ifu.instr(24 downto 20)  

  io.to_Exu.rf_read_data_1 := io.rf_read_data_1
  io.to_Exu.rf_read_data_2 := io.rf_read_data_2
  io.to_Exu.ctrl           := decoder.io.ctrl
  io.to_Exu.rf_write_addr  := io.from_Ifu.instr(11 downto 7)

  io.from_Ifu.ready := io.from_Ifu.valid
  
}