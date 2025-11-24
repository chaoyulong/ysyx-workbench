package playground

import spinal.core._
import spinal.lib._       // 使用spinal的模块库


case class CPU() extends Component {
  val io = new Bundle {
  }

  clockDomain.clock.setName("clock")  // 自定义时钟和复位信号名称，放在最顶层
  clockDomain.reset.setName("reset")

  // ------------------------------------------------------ 定义级间寄存器函数 ------------------------------------------------------ //
  def pipelineConnect[T <: Data, T2 <: Data](
    prevOut: Stream[T],     // 前一级的输出
    thisIn:  Stream[T],     // 这一级的输入  
    thisOut: Stream[T2]     // 这一级的输出  
  ) = {

    val prevFire = prevOut.valid && thisIn.ready                // 当前级与上一级握手成功
    val thisFire = thisOut.valid && thisOut.ready               // 当前级与下一级握手成功，当前级的数据就没用了，可以用来接收数据
    val payloadReg = RegNextWhen(prevOut.payload, prevFire)     // 握手成功更新寄存器
    val validReg = RegInit(False)
    
    when(prevFire) {        // 上游握手成功，说明当前数据处于有效状态
      validReg := True
    }elsewhen(thisFire) {   // 下游握手成功，说明当前数据已经无用，进入无效状态
      validReg := False
    }otherwise{
      validReg := validReg
    }
    
    thisIn.payload := payloadReg  // 接入到当前级
    thisIn.valid := validReg     // 每一级的有效状态为数据有效状态
    
    prevOut.ready := !validReg || thisFire   // 当数据无效，或者下游握手成功即将无效，此时ready置1,表示可以接收新的数据
  }

  def pipelineConnect2[T <: Data, T2 <: Data, T3 <: Data](
    prevOut: Stream[T],     // 前一级的输出
    thisIn:  Stream[T2],     // 这一级的输入  
    thisOut: Stream[T3],    // 这一级的输出  
    dataValid: Bool
  ) = {

    val prevFire = prevOut.valid && thisIn.ready                // 当前级与上一级握手成功
    val thisFire = thisOut.valid && thisOut.ready               // 当前级与下一级握手成功，当前级的数据就没用了，可以用来接收数据
    val payloadReg = RegNextWhen(prevOut.payload, prevFire)     // 握手成功更新寄存器
    val validReg = RegInit(False)
    
    when(prevFire) {        // 上游握手成功，说明当前数据处于有效状态
      validReg := True
    }elsewhen(thisFire) {   // 下游握手成功，说明当前数据已经无用，进入无效状态
      validReg := False
    }otherwise{
      validReg := validReg
    }
    
    thisIn.payload  := payloadReg    // 接入到当前级
    thisIn.valid    := validReg      // 每一级的有效状态为数据有效状态
    
    prevOut.ready := !validReg || thisFire   // 当数据无效，或者下游握手成功即将无效，此时ready置1,表示可以接收新的数据
  }
  // ------------------------------------------------------------------------------------------------------------------------- //

  val reg_file = RegFile()
  val ifu = ysyx_23060082_IFU()
  val idu = ysyx_23060082_IDU()
  val exu = ysyx_23060082_EXU()
  val lsu = ysyx_23060082_LSU()
  val wbu = ysyx_23060082_WBU()
  
  // ifu.io.to_Idu >-> idu.io.from_Ifu   // 
  pipelineConnect(ifu.io.output, idu.io.input, idu.io.output)
  pipelineConnect(idu.io.output, exu.io.input, exu.io.output)
  pipelineConnect2(exu.io.output, lsu.io.input, lsu.io.output)
  // pipelineConnect(lsu.io.output, wbu.io.input, wbu.io.output)
  lsu.io.output >> wbu.io.input
  wbu.io.output >> ifu.io.input
  // pipelineConnect(wbu.io.output, ifu.io.input, ifu.io.output)
  // idu.io.output >-> exu.io.input
  // exu.io.output >-> lsu.io.input
  // lsu.io.output >-> wbu.io.input
  // wbu.io.output >-> ifu.io.input

  reg_file.io.read_addr_1 <> idu.io.rf_read_addr_1
  reg_file.io.read_addr_2 <> idu.io.rf_read_addr_2
  reg_file.io.read_data_1 <> idu.io.rf_read_data_1
  reg_file.io.read_data_2 <> idu.io.rf_read_data_2
  reg_file.io.write_addr  <> wbu.io.rf_write_addr
  reg_file.io.write_data  <> wbu.io.rf_write_data
  reg_file.io.write_en    <> wbu.io.rf_write_en  


}

case class RegFile() extends Component {
  val io = new Bundle {
    val read_addr_1 = in UInt(5 bits)
    val read_addr_2 = in UInt(5 bits)
    val write_addr  = in UInt(5 bits)
    val write_data  = in UInt(32 bits)
    val write_en    = in Bool()

    val read_data_1 = out UInt(32 bits)
    val read_data_2 = out UInt(32 bits)
  }

  val rf = Vec(Reg(UInt(32 bits)),16)    // riscv32e,有16个通用寄存器
  when(io.write_en){
    rf(io.write_addr(0 to 3)) := io.write_data
  }
  rf(U"4'h0") := U"32'h0"
  
  io.read_data_1 := rf(io.read_addr_1(0 to 3))
  io.read_data_2 := rf(io.read_addr_2(0 to 3)) 
}


