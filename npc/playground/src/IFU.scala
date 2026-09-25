package playground

import spinal.core._
import spinal.lib._       // 使用spinal的模块库
import spinal.lib.bus.amba4.axi._

case class Ifu2Idu_data() extends Bundle {
  val pc    = UInt(32 bits)
  val instr = UInt(32 bits)
}

case class RedirectReq() extends Bundle {  // 真实的在exu中运算出来，或者lsu的csr中取出来的pc,同时需要一个valid信号
  val pcNext = UInt(32 bits)
  val fenceI = Bool()                     // 这次重定向同时要求失效 icache
}

// ================================ ================================ //
case class ysyx_23060082_IFU(config: CpuConfig = CpuConfig()) extends Component {
  val io = new Bundle {
    val redirect = slave Flow(RedirectReq())
    val output   = master Stream(Ifu2Idu_data())  
    val axi4     = master(Axi4ReadOnly(AxiConfig.axiConfig))
  }

  object IfuState extends SpinalEnum {              // 定义状态机枚举
    val Idle, WaitMem, Done = newElement()
  }
  val state = Reg(IfuState()) init(IfuState.Idle)   // 创建一个状态机

  // 原配置 8 行 × 16B(128B); 改为等容量 4 行 × 32B: 行更大 -> 顺序取指的空间局部性更好,
  // tag 从 8 份减到 4 份、tag 位宽还少 1 位 => 面积更小(cachesim 在 microbench itrace 上:
  // 8x16B 命中 91.82% / 缺失 47610 -> 4x32B 命中 93.77% / 缺失 36270)
  val icache = ysyx_23060082_Icache(IcacheParams(lineBytes = 32, lines = 4))
  // ============================== 用于确定复位结束 ============================== //
  val rstEnd = RegNext(True) init(False)
  // =================================== PC寄存器 =================================== //
  // pcFetch: 下一次要取的地址(重定向优先, 否则顺序+4),因为与icache握手之后，pcFetch就会+4以便于下一次取指
  // 所以需要一个额外的pcOfReq记录取指时的pc,如果icache未命中时，icache输入输出不在同一拍，那时就需要传递pcOfReq
  val pcFetch = Reg(UInt(32 bits)) init(U(config.resetPc, 32 bits))
  when(io.redirect.valid) {
    pcFetch := io.redirect.pcNext
  } elsewhen(icache.io.reqIn.fire) {
    pcFetch := pcFetch + 4
  } otherwise {
    pcFetch := pcFetch
  }

  // 每次请求的pc与它的响应配对，命中同拍用reqIn.pc, 缺失完成后用这一次请求锁存的pc
  val pcOfReq = RegNextWhen(icache.io.reqIn.pc, icache.io.reqIn.fire)

  // ================================ 指令缓存 (icache) ================================ //
  io.axi4 <> icache.io.axi4
  icache.io.fenceI      := io.redirect.valid && io.redirect.fenceI                    // fence.i: 清空 icache 有效位
  icache.io.reqIn.valid := (state === IfuState.Idle) && rstEnd                        // 复位完成，并且没有指令要发送，发出请求,如果是io.redirect.valid导致的打断，
                                                                                      // 此时icache应该不处于Idle状态，icache.io.reqIn.ready会为低
  icache.io.reqIn.pc    := pcFetch

  val rdataReg           = RegNextWhen(icache.io.rspOut.rdata, icache.io.rspOut.valid)// 响应时更新数据
  val rspIsCurrentHit    = icache.io.reqIn.fire && icache.io.rspOut.valid             // icache直接命中
  // ================================ 状态机 ================================ //
  switch(state) {
    is(IfuState.Idle) {                                                               // 手上没有指令，需要发出请求
      when(rspIsCurrentHit) {                                                         // icache命中，同拍就能输出结果
        when(io.output.fire) { state := IfuState.Idle }                               // 握手同时成功的话，就说明一切都在一周期内完成了，继续待在Idle状态进行下一次取指
        .otherwise           { state := IfuState.Done }                               // 同期握手没有成功，进入Done状态等待与idu的握手
      }
      .elsewhen(icache.io.reqIn.fire) { state := IfuState.WaitMem }                   // icache未命中, 进入等待
      .otherwise { state := IfuState.Idle }
    }
    is(IfuState.WaitMem) {                                                            // 等缺失完成
      when(icache.io.rspOut.valid) {                                                  // icache访存完成，出现命中
        when(io.output.fire) { state := IfuState.Idle }                               // icache访存完成立刻命中,转入Idle状态进行下一次取指
        .otherwise           { state := IfuState.Done }                               // 没有立刻命中,转入Done等待idu接收
      }
      .otherwise { state := IfuState.WaitMem }
    }
    is(IfuState.Done) {                                                               // 手上有指令, 等IDU接收
      when(io.output.fire) { state := IfuState.Idle }
      .otherwise           { state := IfuState.Done }
    }
  }
  when(io.redirect.valid) { state := IfuState.Idle }                                  // 重定向时，所有状态都强制回Idle(最高优先级)

  // ================================ 用于握手的部分 ================================ //
  // icache已经取出指令，或刚刚取出，或同一拍命中。并且没有重定向
  io.output.valid := ((state === IfuState.Done) ||    
                     (icache.io.rspOut.valid && state === IfuState.WaitMem) || 
                      rspIsCurrentHit) && !io.redirect.valid           
  // ================================ 数据传输部分 ================================ //
  io.output.pc    := Mux(icache.io.reqIn.fire, pcFetch, pcOfReq)                      // 同拍命中，直接用pcFetch，否则用请求时锁存的pc
  io.output.instr := Mux(icache.io.rspOut.valid, icache.io.rspOut.rdata, rdataReg)

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