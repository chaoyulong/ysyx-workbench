package playground

import spinal.core._
import spinal.lib._       // 使用spinal的模块库

case class Ifu2Idu_data() extends Bundle {
  val pc    = UInt(32 bits)
  val instr = UInt(32 bits)
}

case class ysyx_23060082_IFU() extends Component {
  val io = new Bundle {
    val input  = slave  Stream(Wbu2Ifu_data())
    val output = master Stream(Ifu2Idu_data())  
  }

  // ------------------ PC寄存器 ------------------ //
  val pc_reg   = Reg(UInt(32 bits)) init(U"32'h80000000")

  // ------------------ 用于确定复位结束 ------------------ //
  val rst_reg_1 = RegInit(False)   // 用于确定复位结束
  val rst_reg_2 = RegInit(False)
  rst_reg_1 := True
  rst_reg_2 := rst_reg_1
  val rst_end = (rst_reg_1 && !rst_reg_2)
  // ------------------    ------------------
  val read_req = Reg(Bool())    // 读取请求信号，只会触发一个周期
  val read_addr = pc_reg        // 读取的地址，在握手成功后触发请求，正好pc会被写入，所以使用pc而不是pc_next
  when(io.input.fire | rst_end){   // 握手成功或者复位结束，都会触发读取
    read_req := True
  }otherwise{
    read_req := False
  }

  val instr_reg = Reg(UInt(32 bits))  // 缓存指令的寄存器
  val mem_rd = Mem_Rd()
  mem_rd.io.rd_req := read_req
  mem_rd.io.addr  := read_addr

  when(mem_rd.io.rd_end){
    instr_reg := mem_rd.io.rdata
  } elsewhen(io.output.fire){ // 握手完成后置0
    instr_reg := U"32'h0"
  } otherwise{
    instr_reg := instr_reg
  }

  // ------------------ 接收上一级数据 ------------------ //
  when(io.input.fire){             // 在握手成功之后更新pc
    pc_reg := io.input.pc_next   
  } otherwise{
    pc_reg := pc_reg
  }
  io.input.ready := io.input.valid 


  val read_end = Reg(Bool())

  when(mem_rd.io.rd_end){     // 读取完成后valid置1
    read_end := True
  } elsewhen(io.output.fire){ // 握手完成后置0
    read_end := False
  } otherwise{
    read_end := read_end
  }

  // ------------------ 用于握手的部分 ------------------ //
  val willValid = read_end
  io.output.valid := io.input.valid && willValid    // io.input.valid为数据有效信号，是寄存器信号
  io.input.ready := willValid
  // ------------------ 数据传输部分 ------------------ //
  io.output.pc    := pc_reg
  io.output.instr := instr_reg

  // io.output.valid := read_end

}