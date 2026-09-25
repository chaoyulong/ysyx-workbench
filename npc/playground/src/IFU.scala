package playground

import spinal.core._
import spinal.lib._       // 使用spinal的模块库
import spinal.lib.bus.amba4.axi._

case class Ifu2Idu_data() extends Bundle {
  val pc    = UInt(32 bits)
  val instr = UInt(32 bits)
}

// ================================ ================================ //
case class ysyx_23060082_IFU(config: CpuConfig = CpuConfig()) extends Component {
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
  val pc = RegNextWhen(io.input.pcNext, io.input.fire) init(U(config.resetPc, 32 bits))
  // ================================ 指令缓存 (icache) ================================ //
  // icache 内嵌: 持有只读 AXI 控制器(缺失访存); IFU 只发取指请求、等指令返回
  val icache = ysyx_23060082_Icache()
  io.axi4 <> icache.io.axi4
  icache.io.fenceI := io.input.fenceI    // fence.i: 清空 icache 有效位
  icache.io.reqIn.valid := (state === IfuState.Idle) && dataValid   // Idle 且数据有效: 发取指请求
  icache.io.reqIn.pc    := pc

  // 仿真专用: 取指性能统计(请求 -> 响应 延迟, 含命中/缺失)
  if (config.enableSimDebug) {
    val perf = PerfReg()
    perf.io.valid := True
    perf.io.req   := B"4'b0"
    perf.io.rsp   := B"4'b0"
    perf.io.evt   := B"8'b0"
    perf.io.req(0) := icache.io.reqIn.fire   // 取指请求拍(记时间)
    perf.io.rsp(0) := icache.io.rspOut.valid // 取指响应拍(算延迟)
    perf.io.evt(0) := icache.io.reqIn.fire   // 取指次数(命中率的分母)
    perf.io.evt(1) := icache.io.rspOut.valid // 响应次数
    // ==================== icache 命中率 / 缺失代价 ==================== //
    // evtCnt0 = 访问次数, evtCnt2 = 缺失次数  -> 命中率 = 1 - evt2/evt0
    // dlyCnt1/dlySum1 = 缺失次数/缺失总周期 -> 平均缺失代价 (即 TMT 里的那一项)
    perf.io.evt(2) := icache.io.miss         // 缺失次数
    perf.io.req(1) := icache.io.miss         // 缺失开始拍(记时间)
    perf.io.rsp(1) := icache.io.missDone     // 缺失完成拍(算延迟)
  }

  val rdataReg = RegNextWhen(icache.io.rspOut.rdata, icache.io.rspOut.valid) init(0)  // 响应时更新数据
  // ================================ 状态机 ================================ //
  switch(state) {
    is(IfuState.Idle) {
      when(dataValid && !icache.io.rspOut.valid) {state := IfuState.WaitMem}  // 请求未完成(缺失/等待), 进入等待
      .otherwise{state := state}
    }
    is(IfuState.WaitMem) {
      when(icache.io.rspOut.valid) {
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
  val willValid = ((state === IfuState.Idle || state === IfuState.WaitMem) && icache.io.rspOut.valid) ||  // 命中同拍/缺失完成
                  (state === IfuState.Done)
  io.output.valid := dataValid && willValid  
  io.input.ready := !dataValid || io.output.fire
  // ================================ 数据传输部分 ================================ //
  io.output.pc    := pc
  io.output.instr := Mux(icache.io.rspOut.valid, icache.io.rspOut.rdata, rdataReg)
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
  io.axi4.ar.addr .setAsReg()         // 地址要锁存
  // io.axi4.ar.id   .setAsReg()
  // io.axi4.ar.len  .setAsReg()
  // io.axi4.ar.size .setAsReg()
  // io.axi4.ar.burst.setAsReg()

  // 加不加突发，这些数值都会是常量，不需要寄存器锁存
  io.axi4.ar.id   := U"4'b0"
  io.axi4.ar.len  := U"8'b0"          // 突发长度1  
  io.axi4.ar.size := U"3'b010"        // 突发大小4字节
  io.axi4.ar.burst:= B"2'b01"         // 突发类型INCR

  when(io.readReq) {
    io.axi4.ar.valid := True
  } elsewhen(io.axi4.ar.fire) {
    io.axi4.ar.valid := False
  } otherwise {
    io.axi4.ar.valid := io.axi4.ar.valid
  }

  when(io.readReq) {
    io.axi4.ar.addr := io.readAddr
  } otherwise {
    io.axi4.ar.addr := io.axi4.ar.addr 
  }

  io.axi4.r.ready := io.axi4.r.valid
  io.readEnd      := io.axi4.r.fire && io.axi4.r.last   // 突发结束(r.last)才算读完
  io.readData     := U(io.axi4.r.data)

  // 读响应错误检查: 从机返回非 OKAY 时仿真报错
  when(io.axi4.r.fire && io.axi4.r.resp =/= Axi4.resp.OKAY) {
    report(Seq("[IFU] read resp error! resp =", io.axi4.r.resp, "addr =", io.axi4.ar.addr))
  }
}