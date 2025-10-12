package playground

import spinal.core._
import spinal.lib._       // 使用spinal的模块库


case class ysyx_23060082_IFU() extends Component {
  val io = new Bundle {
    // val input  = slave(Stream(Lsu2Ifu_data))
    val output = master Stream(Ifu2Idu_data())  
  }


  val pc_reg   = Reg(UInt(32 bits)) init(U"32'h80000000")
  val instr = Mem_Rd(pc_reg)  // 读取指令

  // io.input.ready := io.input.valid

  io.output.payload.pc    := pc_reg
  io.output.payload.instr := instr

  io.output.valid := True

  // when(io.input.fire) {   // 当 valid && ready 时，说明成功发送
  //   io.output.valid := True
  // } elsewhen(io.output.fire) {
  //   io.output.valid := False
  // }

}