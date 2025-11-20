package playground

import spinal.core._
import spinal.lib._



// case class pipelineConnect[T <: Data, T2 <: Data](dataType: HardType[T], dataType2: HardType[T2]) extends Component {
//   val io = new Bundle {
//     val prevOut = slave (Stream(dataType))    // 前一级的输出
//     val thisIn  = master(Stream(dataType))    // 这一级的输入  
//     val thisOut = slave (Stream(dataType2))   // 这一级的输出  
//   }
  
//   io.prevOut.ready := io.thisIn.ready
//   val prevFire = io.prevOut.valid && io.thisIn.ready    // 握手成功
//   val thisFire = io.thisOut.fire                        // 当前级与下一级握手成功，当前级的数据就没用了，可以用来接收数据
//   val payloadReg = RegNextWhen(io.prevOut.payload, prevFire)

//   val validReg = RegInit(False)   // 当前是否有效标志位
  
//   when(prevFire) {        // 有效寄存器更新
//     validReg := True
//   }.elsewhen(thisFire) {
//     validReg := False
//   }
  
//   // 输出连接
//   io.thisIn.payload := payloadReg
//   io.thisIn.valid := io.prevOut.valid   // 上级有效信号直通
  
//   // 输入就绪：当寄存器为空或数据已被下游接收
//   io.prevOut.ready := !validReg || thisFire
// }

