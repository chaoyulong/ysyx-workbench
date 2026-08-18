package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

case class Wbu2Ifu_data() extends Bundle {
  val pc_next       = UInt(32 bits)
}

case class ysyx_23060082_WBU() extends Component {
  val io = new Bundle {
    val input   = slave Flow(Lsu2Wbu_data())
    val output  = master Stream(Wbu2Ifu_data()) 

    val rfWrite = master(RegFileWriteBus())
  }

  io.output.pc_next := io.input.pc_next
  io.rfWrite.addr := io.input.rf_ctrl.rf_write_addr
  io.rfWrite.en   := io.input.rf_ctrl.regWr && io.input.valid
  io.rfWrite.data := Mux(io.input.rf_ctrl.mem2reg | io.input.rf_ctrl.csr2reg, 
                         io.input.mem_data_out, io.input.alu_data_out)

  io.output.valid := io.input.valid

}
