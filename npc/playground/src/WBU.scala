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

  val payloadReg = RegNextWhen(io.input.payload, io.input.fire)     // 握手成功更新寄存器
  val validReg = RegInit(False)
    
  when(io.input.fire) {        // 上游握手成功，说明当前数据处于有效状态
    validReg := True
  }elsewhen(validReg) {        // 只存在一个周期，防止重复写入
    validReg := False
  }otherwise{
    validReg := validReg
  }
    
  io.input.ready := True

  io.output.pc_next := payloadReg.pc_next
  io.rf_write_addr  := payloadReg.rf_ctrl.rf_write_addr
  io.rf_write_en    := payloadReg.rf_ctrl.reg_wr && validReg
  io.rf_write_data  := Mux(io.input.rf_ctrl.mem2reg | io.input.rf_ctrl.csr2reg, io.input.mem_data_out, io.input.alu_data_out)

  val outValid = RegInit(False)
  when(io.input.fire) {        
    outValid := True
  }elsewhen(io.output.fire) {       
    outValid := False
  }otherwise{
    outValid := outValid
  } 
  io.output.valid := outValid 
  
  // ------------------ 用于握手的部分 ------------------ //
  // val willValid = True
  // val outValid = RegInit(False)
  // when(io.input.valid){
  //   outValid := True
  // }elsewhen(io.output.fire){
  //   outValid := False
  // }otherwise{
  //   outValid := outValid
  // }
  // io.output.valid := outValid
  // io.input.ready := True

  // val outValid = RegInit(False)
  // when(io.input.valid){
  //   outValid := True
  // }elsewhen(io.output.fire){
  //   outValid := False
  // }otherwise{
  //   outValid := outValid
  // }

  // io.output.valid := outValid    // io.input.valid为数据有效信号，是寄存器信号
  // io.input.ready := io.input.valid
  // ------------------ 数据传输部分 ------------------ //
  // io.input.ready := io.input.valid
  // io.output.valid   := io.input.valid
}