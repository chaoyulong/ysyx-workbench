package playground

import spinal.core._

case class ysyx_23060082_IDU() extends Component {
  val io = new Bundle {
    val input  = slave Stream(Ifu2Idu_data)
    // val output = master(Stream(Ifu2Idu_data))    
  }

  val decoder = Decoder()
  // 接收数据
  when(io.input.fire) {
    decoder.instr := input.payload.instr
  }

  io.input.ready := io.input.valid
  
}