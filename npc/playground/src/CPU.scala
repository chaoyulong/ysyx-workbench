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
    thisIn:  Flow[T],       // 这一级的输入  
    thisOut: Stream[T2]     // 这一级的输出  
  ) = {

    val payloadReg = RegNextWhen(prevOut.payload, prevOut.fire)     // 握手成功更新寄存器
    val validReg = RegInit(False)
    
    when(prevOut.fire) {        // 上游握手成功，说明当前数据处于有效状态
      validReg := True
    }elsewhen(thisOut.fire) {   // 下游握手成功，说明当前数据已经无用，进入无效状态
      validReg := False
    }otherwise{
      validReg := validReg
    }
    
    thisIn.payload := payloadReg  // 接入到当前级
    thisIn.valid := validReg     // 每一级的有效状态为数据有效状态
    
    prevOut.ready := !validReg || thisOut.fire   // 当数据无效，或者下游握手成功即将无效，此时ready置1,表示可以接收新的数据
  }
  // ------------------------------------------------------------------------------------------------------------------------- //

  val reg_file = ysyx_23060082_RegFile()
  val ifu = ysyx_23060082_IFU()
  val idu = ysyx_23060082_IDU()
  val exu = ysyx_23060082_EXU()
  val lsu = ysyx_23060082_LSU()
  val wbu = ysyx_23060082_WBU()
  
  pipelineConnect(ifu.io.output, idu.io.input, idu.io.output)
  pipelineConnect(idu.io.output, exu.io.input, exu.io.output)
  pipelineConnect(exu.io.output, lsu.io.input, lsu.io.output)
  // pipelineConnect(lsu.io.output, wbu.io.input, wbu.io.output)
  lsu.io.output >> wbu.io.input   // wbu没有下一级，直接特殊对待，写回直接在内部处理
  wbu.io.output >> ifu.io.input


  reg_file.io.read_addr_1 <> idu.io.rf_read_addr_1
  reg_file.io.read_addr_2 <> idu.io.rf_read_addr_2
  reg_file.io.read_data_1 <> idu.io.rf_read_data_1
  reg_file.io.read_data_2 <> idu.io.rf_read_data_2
  reg_file.io.write_addr  <> wbu.io.rf_write_addr
  reg_file.io.write_data  <> wbu.io.rf_write_data
  reg_file.io.write_en    <> wbu.io.rf_write_en  


}

case class ysyx_23060082_RegFile() extends Component {
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
    rf(io.write_addr(3 downto 0)) := io.write_data
    rf(0) := U"32'h0"
  }
  

  io.read_data_1 := rf(io.read_addr_1(0 to 3))
  io.read_data_2 := rf(io.read_addr_2(0 to 3)) 
}


