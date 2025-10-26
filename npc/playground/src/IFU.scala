package playground

import spinal.core._
import spinal.lib._       // 使用spinal的模块库


case class ysyx_23060082_IFU() extends Component {
  val io = new Bundle {
    val from_Lsu  = slave  Stream(Lsu2Ifu_data())
    val to_Idu    = master Stream(Ifu2Idu_data())  
  }

  val pc_reg   = Reg(UInt(32 bits)) init(U"32'h80000000")
  val instr = Mem_Rd(pc_reg)  // 读取指令

  // io.input.ready := io.input.valid

  io.to_Idu.payload.pc    := pc_reg
  io.to_Idu.payload.instr := instr

  io.to_Idu.valid := True

  pc_reg := pc_reg + U"32'h4"

  // when(io.input.fire) {   // 当 valid && ready 时，说明成功发送
  //   io.output.valid := True
  // } elsewhen(io.output.fire) {
  //   io.output.valid := False
  // }

}