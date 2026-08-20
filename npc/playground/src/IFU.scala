package playground

import spinal.core._
import spinal.lib._       // 使用spinal的模块库
import spinal.lib.bus.amba4.axi._

case class Ifu2Idu_data() extends Bundle {
  val pc    = UInt(32 bits)
  val instr = UInt(32 bits)
}

// ================================ ================================ //
case class ysyx_23060082_IFU(resetPc: BigInt) extends Component {
  val io = new Bundle {
    val input  = slave  Stream(Wbu2Ifu_data())
    val output = master Stream(Ifu2Idu_data())  
    val axi4   = master(Axi4ReadOnly(AxiConfig.axiConfig))
  }

  object IfuState extends SpinalEnum {              // 定义状态机枚举
    val Idle, WaitMem, Done = newElement()
  }
  val state = Reg(IfuState()) init(IfuState.Idle)   // 创建一个状态机
  // ============================== 用于确定复位结束 ============================== //
  val rstReg1 = RegNext(True) init(False)
  val rstReg2 = RegNext(rstReg1) init(False)
  val rstEnd = (rstReg1 && !rstReg2)
  // ================================ 数据有效信号 ================================ //
  val dataValid = RegInit(False)
  when(io.input.fire || rstEnd) {     // 上游握手成功，或者复位结束，说明当前数据处于有效状态
    dataValid := True
  }elsewhen(io.output.fire) {         // 下游握手成功，说明当前数据已经无用，进入无效状态
    dataValid := False
  }otherwise{
    dataValid := dataValid
  }
  // ================================ PC寄存器 ================================ //
  val pc = RegNextWhen(io.input.pc_next, io.input.fire) init(U(resetPc, 32 bits))
  // ================================ 读内存 ================================ //
  val axi4Ctrler = ysyx_23060082_Axi4_Ctrler_ReadOnly()
  io.axi4 <> axi4Ctrler.io.axi4
  axi4Ctrler.io.readReq := (state === IfuState.Idle) && dataValid   // 数据开始有效并且处于等待状态，触发一次读取
  axi4Ctrler.io.readAddr:= pc

  val rdataReg = RegNextWhen(axi4Ctrler.io.readData, state === IfuState.WaitMem && axi4Ctrler.io.readEnd) init(0)  // 读完时更新数据
  // ================================ 状态机 ================================ //
  switch(state) {
    is(IfuState.Idle) {
      when(dataValid) {state := IfuState.WaitMem}     // 握手成功或者复位结束，都会触发读取 
      .otherwise{state := state}
    }
    is(IfuState.WaitMem) {
      when(axi4Ctrler.io.readEnd) {
        when(io.output.fire){state := IfuState.Idle}     // 若已经握手成功，则返回到Idle状态
        .otherwise{state := IfuState.Done}
      }
      .otherwise{state := state}
    }
    is(IfuState.Done) {
      when(io.output.fire) {state := IfuState.Idle}    
      .otherwise{state := state}    
    }
  }

  // ================================ 用于握手的部分 ================================ //
  // willValid的意义就是当前周期就可以完成任务
  val willValid = (state === IfuState.WaitMem && axi4Ctrler.io.readEnd) ||       // 访存完成
                  (state === IfuState.Done)
  io.output.valid := dataValid && willValid  
  io.input.ready := !dataValid || io.output.fire
  // ================================ 数据传输部分 ================================ //
  io.output.pc    := pc
  io.output.instr := Mux(state === IfuState.WaitMem && axi4Ctrler.io.readEnd, axi4Ctrler.io.readData, rdataReg)
}

/* ****************************************************************
  只有读通道的axi总线控制器
**************************************************************** */
case class ysyx_23060082_Axi4_Ctrler_ReadOnly() extends Component {
  val io = new Bundle {
    val readReq  = in Bool()
    val readAddr = in UInt(32 bits)
    val readEnd  = out Bool()
    val readData = out UInt(32 bits)
    val axi4 = master(Axi4ReadOnly(AxiConfig.axiConfig))
  }

  io.axi4.ar.valid.setAsReg() init(False)
  io.axi4.ar.addr .setAsReg()
  io.axi4.ar.id   .setAsReg()
  io.axi4.ar.len  .setAsReg()
  io.axi4.ar.size .setAsReg()
  io.axi4.ar.burst.setAsReg()

  when(io.readReq) {
    io.axi4.ar.valid := True
  } elsewhen(io.axi4.ar.fire) {
    io.axi4.ar.valid := False
  } otherwise {
    io.axi4.ar.valid := io.axi4.ar.valid
  }

  when(io.readReq) {
    io.axi4.ar.addr := io.readAddr
    io.axi4.ar.id   := U"4'b0"
    io.axi4.ar.len  := U"8'b0"          // 突发长度1  
    io.axi4.ar.size := U"3'b010"        // 突发大小4字节
    io.axi4.ar.burst:= B"2'b01"         // 突发类型INCR
  } otherwise {
    io.axi4.ar.addr := io.axi4.ar.addr 
    io.axi4.ar.id   := io.axi4.ar.id 
    io.axi4.ar.len  := io.axi4.ar.len   // 突发长度1  
    io.axi4.ar.size := io.axi4.ar.size  // 突发大小4字节
    io.axi4.ar.burst:= io.axi4.ar.burst // 突发类型INCR
  }

  io.axi4.r.ready := io.axi4.r.valid
  io.readEnd := io.axi4.r.fire && io.axi4.r.last   // 突发结束(r.last)才算读完
  io.readData := io.axi4.r.data.asUInt

  // 读响应错误检查: 从机返回非 OKAY 时仿真报错
  when(io.axi4.r.fire && io.axi4.r.resp =/= Axi4.resp.OKAY) {
    report(Seq("[IFU] read resp error! resp =", io.axi4.r.resp, "addr =", io.axi4.ar.addr))
  }
}