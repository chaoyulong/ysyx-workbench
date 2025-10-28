package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

case class Wbu2Ifu_data() extends Bundle {
  val pc_next       = UInt(32 bits)
}

case class ysyx_23060082_WBU() extends Component {
  val io = new Bundle {
    val from_Lsu  = slave Stream(Lsu2Wbu_data())
    val to_Ifu    = master Stream(Wbu2Ifu_data()) 

    val rf_write_addr  = out UInt(5 bits)
    val rf_write_data  = out UInt(32 bits)
    val rf_write_en    = out Bool()
  }

  io.to_Ifu.pc_next := io.from_Lsu.pc_next
  io.rf_write_addr  := io.from_Lsu.rf_ctrl.rf_write_addr
  io.rf_write_en    := io.from_Lsu.rf_ctrl.reg_wr
  io.rf_write_data  := Mux(io.from_Lsu.rf_ctrl.mem2reg, io.from_Lsu.mem_data_out, io.from_Lsu.alu_data_out)


  io.from_Lsu.ready := io.from_Lsu.valid
  io.to_Ifu.valid   := io.from_Lsu.valid
}