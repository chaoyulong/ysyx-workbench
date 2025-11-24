package playground

import spinal.core._
import spinal.lib._       // 使用spinal的模块库

case class Ifu2Idu_data() extends Bundle {
  val pc    = UInt(32 bits)
  val instr = UInt(32 bits)
}

object IfuState extends SpinalEnum {
  val Idle, WaitMem, Done = newElement()
}

case class ysyx_23060082_IFU() extends Component {
  val io = new Bundle {
    val input  = slave  Stream(Wbu2Ifu_data())
    val output = master Stream(Ifu2Idu_data())  
  }

  val state = Reg(IfuState()) init(IfuState.Idle)   // 创建一个状态机
  // --------------------------------- 用于确定复位结束 --------------------------------- //
  val rstReg1 = RegNext(True) init(False)
  val rstReg2 = RegNext(rstReg1) init(False)
  val rstEnd = (rstReg1 && !rstReg2)
  // ------------------------------------ -------------------------------------------- //
  val dataValid = RegInit(False)
  when(io.input.fire | rstEnd) {        // 上游握手成功，说明当前数据处于有效状态
    dataValid := True
  }elsewhen(io.output.fire) {   // 下游握手成功，说明当前数据已经无用，进入无效状态
    dataValid := False
  }otherwise{
    dataValid := dataValid
  }
  // ------------------------------------ PC寄存器 ------------------------------------ //
  val pc_reg = RegNextWhen(io.input.pc_next, io.input.fire) init(U"32'h80000000")
  // ------------------------------------- 读内存 ------------------------------------- //
  val mem_rd = Mem_Rd()
  val readReq = (state === IfuState.Idle) && dataValid
  val rdataReg  = Reg(UInt(32 bits)) init(0)   
  mem_rd.io.rd_req := readReq
  mem_rd.io.addr  := pc_reg

  when(state === IfuState.WaitMem && mem_rd.io.rd_end){   // 已读完
    rdataReg := mem_rd.io.rdata
  } otherwise{
    rdataReg := rdataReg
  }
  val rdata = Mux(state === IfuState.WaitMem && mem_rd.io.rd_end, mem_rd.io.rdata, rdataReg)
  // ------------------------------------- 状态机 ------------------------------------- //
  switch(state) {
    is(IfuState.Idle) {
      when(io.input.fire || rstEnd) {state := IfuState.WaitMem}     // 握手成功或者复位结束，都会触发读取 
    }
    is(IfuState.WaitMem) {
      when(mem_rd.io.rd_end) {
        when(io.output.fire){state := IfuState.Idle}     // 若已经握手成功，则返回到Idle状态
        .otherwise{state := IfuState.Done}
      }
    }
    is(IfuState.Done) {
      when(io.output.fire) {state := IfuState.Idle}        
    }
  }
  // ----------------------- 用于握手的部分 ----------------------- //
  // willValid的意义就是当前周期就可以完成任务
  val willValid = (state === IfuState.WaitMem && mem_rd.io.rd_end) ||       // 访存完成
                  (state === IfuState.Done)
  io.output.valid := dataValid && willValid  
  io.input.ready := !dataValid || io.output.fire
  // ----------------------- 数据传输部分 ----------------------- //
  io.output.pc    := pc_reg
  io.output.instr := rdata
}

// case class ysyx_23060082_IFU() extends Component {
//   val io = new Bundle {
//     val input  = slave  Stream(Wbu2Ifu_data())
//     val output = master Stream(Ifu2Idu_data())  
//   }

//   // ------------------ PC寄存器 ------------------ //
//   val pc_reg   = Reg(UInt(32 bits)) init(U"32'h80000000")

//   // ------------------ 用于确定复位结束 ------------------ //
//   val rst_reg_1 = RegInit(False)   // 用于确定复位结束
//   val rst_reg_2 = RegInit(False)
//   rst_reg_1 := True
//   rst_reg_2 := rst_reg_1
//   val rst_end = (rst_reg_1 && !rst_reg_2)
//   // ------------------    ------------------
//   val read_req = Reg(Bool())    // 读取请求信号，只会触发一个周期
//   val read_addr = pc_reg        // 读取的地址，在握手成功后触发请求，正好pc会被写入，所以使用pc而不是pc_next
//   when(io.input.fire | rst_end){   // 握手成功或者复位结束，都会触发读取
//     read_req := True
//   }otherwise{
//     read_req := False
//   }

//   val instr_reg = Reg(UInt(32 bits))  // 缓存指令的寄存器
//   val mem_rd = Mem_Rd()
//   mem_rd.io.rd_req := read_req
//   mem_rd.io.addr  := read_addr

//   when(mem_rd.io.rd_end){
//     instr_reg := mem_rd.io.rdata
//   } elsewhen(io.output.fire){ // 握手完成后置0
//     instr_reg := U"32'h0"
//   } otherwise{
//     instr_reg := instr_reg
//   }

//   // ------------------ 接收上一级数据 ------------------ //
//   when(io.input.fire){             // 在握手成功之后更新pc
//     pc_reg := io.input.pc_next   
//   } otherwise{
//     pc_reg := pc_reg
//   }
//   // io.input.ready := io.input.valid 


//   val read_end = Reg(Bool())
//   val ifu_busy = Reg(Bool())
//   when(mem_rd.io.rd_end){     // 读取完成后valid置1
//     read_end := True
//   } elsewhen(io.output.fire){ // 握手完成后置0
//     read_end := False
//   } otherwise{
//     read_end := read_end
//   }

//   when(io.input.fire | rst_end){     // 读取完成后valid置1
//     ifu_busy := True
//   } elsewhen(mem_rd.io.rd_end){ // 握手完成后置0
//     ifu_busy := False
//   } otherwise{
//     ifu_busy := ifu_busy
//   }

//   // ------------------ 用于握手的部分 ------------------ //
//   val willValid = read_end
//   io.output.valid := read_end    // io.input.valid为数据有效信号，是寄存器信号
//   io.input.ready := (!ifu_busy)
//   // ------------------ 数据传输部分 ------------------ //
//   io.output.pc    := pc_reg
//   io.output.instr := instr_reg

//   // io.output.valid := read_end

// }