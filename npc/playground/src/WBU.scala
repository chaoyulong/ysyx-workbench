package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

case class ysyx_23060082_WBU(config: CpuConfig = CpuConfig()) extends Component {
  val io = new Bundle {
    val input   = slave Flow(Lsu2Wbu_data(config))

    val rfWrite = master(RegFileWriteBus())
    val forward = out(forwardData())
  }

  // ================================ 写寄存器总线 ================================ //
  io.rfWrite.addr   := io.input.rfCtrl.rfWriteAddr
  io.rfWrite.en     := io.input.rfCtrl.regWr && io.input.valid
  io.rfWrite.data   := io.input.rfWriteData
  // ================================ 数据前递 ================================ //
  val wr                = io.input.rfCtrl.regWr                 
  io.forward.state     := Mux(!io.input.valid || !wr, FwdState.NoWriter, FwdState.DataReady)  // 还没有有效数据，或者不是写寄存器的信号时，None，否则要写入的数据一定是准备好的
  io.forward.writeAddr := io.input.rfCtrl.rfWriteAddr
  io.forward.writeData := io.input.rfWriteData
  // ================================ itrace ================================ //
  if (config.enableSimDebug){           // 仿真用的，用来记录当前执行完成的指令与对应的pc值
    val itrace = ItraceReg()            // 黑盒: 寄存器在 dpi-c.v, C++ 侧直接读取
    itrace.io.valid  := io.input.valid  // 指令到达WBU = 执行完毕
    itrace.io.pc     := io.input.pc
    itrace.io.instr  := io.input.instr
    itrace.io.pcNext := io.input.pcNext
  }
}
