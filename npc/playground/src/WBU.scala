package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

case class Wbu2Ifu_data() extends Bundle {
  val pc_next       = UInt(32 bits)
}

case class ysyx_23060082_WBU() extends Component {
  val io = new Bundle {
    val input  = slave Stream(Lsu2Wbu_data())
    val output    = master Stream(Wbu2Ifu_data()) 

    val rf_write_addr  = out UInt(5 bits)
    val rf_write_data  = out UInt(32 bits)
    val rf_write_en    = out Bool()
  }

  io.output.pc_next := io.input.pc_next
  io.rf_write_addr  := io.input.rf_ctrl.rf_write_addr
  io.rf_write_en    := io.input.rf_ctrl.reg_wr
  io.rf_write_data  := Mux(io.input.rf_ctrl.mem2reg, io.input.mem_data_out, io.input.alu_data_out)

  // ------------------ 用于握手的部分 ------------------ //
  val willValid = True
  val outValid = RegInit(False)
  when(io.input.valid){
    outValid := True
  }elsewhen(io.output.fire){
    outValid := False
  }otherwise{
    outValid := outValid
  }
  io.output.valid := outValid
  io.input.ready := io.input.valid
  // io.output.valid := io.input.valid && willValid    // io.input.valid为数据有效信号，是寄存器信号
  // io.input.ready := willValid
  // ------------------ 数据传输部分 ------------------ //
  // io.input.ready := io.input.valid
  // io.output.valid   := io.input.valid
}