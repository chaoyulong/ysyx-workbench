package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

case class Idu2Exu_data() extends Bundle {
  val pc    = UInt(32 bits)

  val ctrl = Ctrl()
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

  val instr = io.from_Ifu.instr
  val decoder = Decoder()
  decoder.instr := instr         
// ***************************************** 立即数生成 *********************************************** //
  val immU = U(instr(31 downto 12) ## U(0, 12 bits))
  val immJ = U((instr(31) #* 12) ## instr(19 downto 12) ## instr(20) ## instr(30 downto 21)## U(0, 1 bits))
  val immI = U((instr(31) #* 20) ## instr(31 downto 20))
  val immS = U((instr(31) #* 20) ## instr(31 downto 25) ## instr(11 downto 7))
  val immB = U((instr(31) #* 20) ## instr(7) ## instr(30 downto 25) ## instr(11 downto 8) ## U(0, 1 bits))

  // 独热码选择器
  val has_type = decoder.io.instr_type(5 downto 1).orR
  val imm_values = Vec(immU, immJ, immI, immS, immB)
  io.to_Exu.imm := Mux(has_type, MuxOH(decoder.io.instr_type(5 downto 1), imm_values), U"32'h0")
// *****************************************  *********************************************** //
  io.rf_read_addr_1 := instr(19 downto 15)
  io.rf_read_addr_2 := instr(24 downto 20)  // 为了写起来简洁，写寄存器地址在decoder中赋值
  
  io.to_Exu.pc              := io.from_Ifu.pc
  io.to_Exu.rf_read_data_1  := io.rf_read_data_1
  io.to_Exu.rf_read_data_2  := io.rf_read_data_2
  io.to_Exu.ctrl            := decoder.io.ctrl

  io.from_Ifu.ready := io.from_Ifu.valid
  
}