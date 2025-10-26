package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

case class ysyx_23060082_IDU() extends Component {
  val io = new Bundle {
    val from_Ifu  = slave Stream(Ifu2Idu_data())
    // val output = master(Stream(Ifu2Idu_data))    
  }

  val idu_pc  = Reg(UInt(32 bits)) init(0)
  val decoder = Decoder()

  decoder.instr := U"32'h0"         
  idu_pc := idu_pc
  // 接收数据
  when(io.from_Ifu.fire) {
    idu_pc := io.from_Ifu.payload.pc
    decoder.instr := io.from_Ifu.payload.instr
  }

  io.from_Ifu.ready := io.from_Ifu.valid
  
}