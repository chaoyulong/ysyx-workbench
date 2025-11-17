package playground

import spinal.core._
import spinal.lib._       // 使用spinal的模块库

case class Ifu2Idu_data() extends Bundle {
  val pc    = UInt(32 bits)
  val instr = UInt(32 bits)
}

case class ysyx_23060082_IFU() extends Component {
  val io = new Bundle {
    val from_Wbu  = slave  Stream(Wbu2Ifu_data())
    val to_Idu    = master Stream(Ifu2Idu_data())  
  }

  // ------------------ PC寄存器 ------------------ //
  val pc_reg   = Reg(UInt(32 bits)) init(U"32'h80000000")

  // ------------------ 用于确定复位结束 ------------------ //
  val rst_reg_1 = Reg(Bool()) init(False)   // 用于确定复位结束
  val rst_reg_2 = Reg(Bool()) init(False)
  rst_reg_1 := True
  rst_reg_2 := rst_reg_1
  val rst_end = (rst_reg_1 && !rst_reg_2)
  // ------------------    ------------------
  val read_req = Reg(Bool())    // 读取请求信号，只会触发一个周期
  val read_addr = pc_reg        // 读取的地址，在握手成功后触发请求，正好pc会被写入，所以使用pc而不是pc_next
  when(io.from_Wbu.fire | rst_end){   // 握手成功或者复位结束，都会触发读取
    read_req := True
  }otherwise{
    read_req := False
  }

  val instr_reg = Reg(UInt(32 bits))  // 缓存指令的寄存器
  val mem_rd = Mem_Rd()
  mem_rd.io.rd_en := read_req
  mem_rd.io.addr  := read_addr

  when(mem_rd.io.rd_end){
    instr_reg := mem_rd.io.rdata
  } elsewhen(io.to_Idu.fire){ // 握手完成后置0
    instr_reg := U"32'h0"
  } otherwise{
    instr_reg := instr_reg
  }

  // ------------------ 接收上一级数据 ------------------ //
  when(io.from_Wbu.fire){             // 在握手成功之后更新pc
    pc_reg := io.from_Wbu.pc_next   
  } otherwise{
    pc_reg := pc_reg
  }
  io.from_Wbu.ready := io.from_Wbu.valid 

  // ------------------ 传递到下一级的数据 ------------------ //
  io.to_Idu.pc    := pc_reg
  io.to_Idu.instr := instr_reg

  when(mem_rd.io.rd_end){     // 读取完成后valid置1
    io.to_Idu.valid := True
  } elsewhen(io.to_Idu.fire){ // 握手完成后置0
    io.to_Idu.valid := False
  } otherwise{
    io.to_Idu.valid := io.to_Idu.valid
  }
}