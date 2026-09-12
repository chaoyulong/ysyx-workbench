package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

case class Wbu2Ifu_data() extends Bundle {
  val pcNext  = UInt(32 bits)
  val fenceI  = Bool()        // fence.i: 通知 IFU 失效 icache
}

case class ysyx_23060082_WBU(config: CpuConfig = CpuConfig()) extends Component {
  val io = new Bundle {
    val input   = slave Flow(Lsu2Wbu_data(config))
    val output  = master Stream(Wbu2Ifu_data()) 

    val rfWrite = master(RegFileWriteBus())
  }

  if (config.enableSimDebug){           // 仿真用的，用来记录当前执行完成的指令与对应的pc值
    val itrace = ItraceReg()            // 黑盒: 寄存器在 dpi-c.v, C++ 侧直接读取
    itrace.io.valid := io.input.valid   // 指令到达WBU = 执行完毕
    itrace.io.pc    := io.input.pc
    itrace.io.instr := io.input.instr
  }

  io.output.pcNext := io.input.pcNext
  io.output.fenceI := io.input.fenceI
  io.rfWrite.addr := io.input.rfCtrl.rfWriteAddr
  io.rfWrite.en   := io.input.rfCtrl.regWr && io.input.valid
  io.rfWrite.data := Mux(io.input.rfCtrl.mem2reg | io.input.rfCtrl.csr2reg, 
                         io.input.mem_data_out, io.input.alu_data_out)

  io.output.valid := io.input.valid

}
