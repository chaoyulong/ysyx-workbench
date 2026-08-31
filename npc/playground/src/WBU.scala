package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

case class Wbu2Ifu_data() extends Bundle {
  val pcNext       = UInt(32 bits)
  val fenceI       = Bool()      // fence.i: 通知 IFU 失效 icache
}

case class ysyx_23060082_WBU() extends Component {
  val io = new Bundle {
    val input   = slave Flow(Lsu2Wbu_data())
    val output  = master Stream(Wbu2Ifu_data()) 

    val rfWrite = master(RegFileWriteBus())
  }

  io.output.pcNext := io.input.pcNext
  io.output.fenceI := io.input.fenceI
  io.rfWrite.addr := io.input.rfCtrl.rfWriteAddr
  io.rfWrite.en   := io.input.rfCtrl.regWr && io.input.valid
  io.rfWrite.data := Mux(io.input.rfCtrl.mem2reg | io.input.rfCtrl.csr2reg, 
                         io.input.mem_data_out, io.input.alu_data_out)

  io.output.valid := io.input.valid

}
