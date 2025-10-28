package playground

import spinal.core._
import spinal.lib._       // 使用spinal的模块库

case class Ifu2Idu_data() extends Bundle {
  val pc    = UInt(32 bits)
  val instr = UInt(32 bits)
}

case class ysyx_23060082_IFU() extends Component {
  val io = new Bundle {
    val from_Wbu  = slave  Stream(Wbu2Ifu_data())
    val to_Idu    = master Stream(Ifu2Idu_data())  
  }

  val pc_reg   = Reg(UInt(32 bits)) init(U"32'h80000000")
  val instr = Mem_Rd(pc_reg)  // 读取指令

  val rst_end = Reg(UInt(1 bits)) init(U"0")
  val rst_end_last = Reg(UInt(1 bits)) init(U"0")
  rst_end := U"1"
  rst_end_last := rst_end

  io.to_Idu.pc    := pc_reg
  io.to_Idu.instr := instr

  pc_reg := io.from_Wbu.pc_next   // 更新pc

  io.from_Wbu.ready := io.from_Wbu.valid 
  io.to_Idu.valid := io.from_Wbu.valid | (rst_end === U"1" && rst_end_last === U"0")

}