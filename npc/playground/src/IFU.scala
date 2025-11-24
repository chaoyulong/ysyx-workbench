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
  // ----------------------------------- 数据有效信号 ----------------------------------- //
  val dataValid = RegInit(False)
  when(io.input.fire || rstEnd) {     // 上游握手成功，或者复位结束，说明当前数据处于有效状态
    dataValid := True
  }elsewhen(io.output.fire) {         // 下游握手成功，说明当前数据已经无用，进入无效状态
    dataValid := False
  }otherwise{
    dataValid := dataValid
  }
  // ------------------------------------ PC寄存器 ------------------------------------ //
  val pc_reg = RegNextWhen(io.input.pc_next, io.input.fire) init(U"32'h80000000")
  // ------------------------------------- 读内存 ------------------------------------- //
  val mem_rd = Mem_Rd()
  val readReq = (state === IfuState.Idle) && dataValid
  val rdataReg = RegNextWhen(mem_rd.io.rdata, state === IfuState.WaitMem && mem_rd.io.rd_end) init(0)  // 读完时更新数据
  mem_rd.io.rd_req := readReq
  mem_rd.io.addr  := pc_reg
  // ------------------------------------- 状态机 ------------------------------------- //
  switch(state) {
    is(IfuState.Idle) {
      when(dataValid) {state := IfuState.WaitMem}     // 握手成功或者复位结束，都会触发读取 
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
  // ---------------------------------- 用于握手的部分 ---------------------------------- //
  // willValid的意义就是当前周期就可以完成任务
  val willValid = (state === IfuState.WaitMem && mem_rd.io.rd_end) ||       // 访存完成
                  (state === IfuState.Done)
  io.output.valid := dataValid && willValid  
  io.input.ready := !dataValid || io.output.fire
  // ----------------------------------- 数据传输部分 ----------------------------------- //
  io.output.pc    := pc_reg
  io.output.instr := Mux(state === IfuState.WaitMem && mem_rd.io.rd_end, mem_rd.io.rdata, rdataReg)
}
