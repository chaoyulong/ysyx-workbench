package playground

import spinal.core._
import spinal.lib._       // 使用spinal的模块库
import spinal.lib.bus.amba4.axi._

case class Ifu2Idu_data() extends Bundle {
  val pc           = UInt(32 bits)
  val instr        = UInt(32 bits)
  val ifuTrapEnter = Bool()          // 有异常
  val ifuExcCause  = UInt(4 bits)    // 异常号
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

  // 曾试过等容量的 4 行 × 32B(8 拍突发): cachesim 预测命中率 91.82%->93.77%, 但实测直接崩
  // (icache 缺失 avg 2615 拍、LSU 读 avg 732 拍 -> 平台不支持 8 拍突发, boot 阶段就 ABORT)
  // => 这个平台上 icache 的行长不能超过 16B(4 拍突发), 保持 8 行 × 16B
  val icache = ysyx_23060082_Icache(config, IcacheParams())
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
  val stopFetch   = RegInit(False)                    // 如果是直接jal与jalr指令，直接等待pcNext反馈比取pc+4更快
  val pcMisaligned= pcFetch(1) || pcFetch(0)          // 未对齐异常
  val rspFault    = icache.io.rspErr                  // AXI响应异常
  val pfFault     = False                             // MMU接口,页错误，未实现

  val excStop     = RegInit(False)                    // 出现异常时要暂停流水线，直到重定向信号到来，也就是进入__am_irq_handle 
  val excCause    = Mux(pcMisaligned, U(0, 4 bits),   // 异常的cause号
                    Mux(rspFault,     U(1, 4 bits), U(12, 4 bits)))
  // 复位完成，并且没有指令要发送，发出请求,如果是io.redirect.valid导致的打断，此时icache应该不处于Idle状态，icache.io.reqIn.ready会为低
  val tryFetch    = (state === IfuState.Idle) && rstEnd && !stopFetch && !excStop          
  val fetchExc    = tryFetch && (pcMisaligned || rspFault || pfFault)                 // 有异常

  when(io.redirect.valid) {
    excStop := False
  } elsewhen(fetchExc && io.output.fire) {
    excStop := True
  } 

  io.axi4 <> icache.io.axi4
  icache.io.fenceI      := io.redirect.valid && io.redirect.fenceI                    // fence.i: 清空 icache 有效位
  icache.io.reqIn.valid := tryFetch && !fetchExc                                      // 没有异常才发送请求          
  icache.io.reqIn.pc    := pcFetch

  val rdataReg           = RegNextWhen(icache.io.rspOut.rdata, icache.io.rspOut.valid)// 响应时更新数据
  val rspIsCurrentHit    = icache.io.reqIn.fire && icache.io.rspOut.valid             // icache直接命中
  // =================================== 预先译码出跳转指令 =================================== //
  val instrOut  = io.output.instr
  val i_jalr = instrOut === M"-----------------000-----1100111"
  val i_jal  = instrOut === M"-------------------------1101111"
  val i_beq  = instrOut === M"-----------------000-----1100011"
  val i_bne  = instrOut === M"-----------------001-----1100011"
  val i_blt  = instrOut === M"-----------------100-----1100011"
  val i_bge  = instrOut === M"-----------------101-----1100011"
  val i_bltu = instrOut === M"-----------------110-----1100011"
  val i_bgeu = instrOut === M"-----------------111-----1100011"
  val isJump = i_jalr | i_jal i_beq | i_bne | i_blt | i_bge | i_bltu | i_bgeu
  // 取到无条件跳转就关闭取指，直到重定向把前端重启
  when(io.redirect.valid) {   // 靠重定向信号来关闭阻塞
    stopFetch := False
  } elsewhen(io.output.valid && isJump) {
    stopFetch := True
  } otherwise {
    stopFetch := stopFetch
  }
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

        
  // ================================ 数据传输部分 ================================ //
  val normalValid = ((state === IfuState.Done) ||                                     // icache已经取出指令，或刚刚取出，或同一拍命中。并且没有重定向
                     (icache.io.rspOut.valid && state === IfuState.WaitMem) || 
                      rspIsCurrentHit) && !io.redirect.valid
  val normalInstr = Mux(icache.io.rspOut.valid, icache.io.rspOut.rdata, rdataReg)

  io.output.valid := normalValid || fetchExc                                          // 正常有效信号，或者取指错误
  io.output.pc    := Mux(pcMisaligned || icache.io.reqIn.fire, pcFetch, pcOfReq)      // 同拍命中，直接用pcFetch，否则用请求时锁存的pc,未对齐异常在要请求访存时就能发现
  io.output.instr := Mux(fetchExc, U"32'b0", normalInstr)
  io.output.ifuTrapEnter := fetchExc
  io.output.ifuExcCause  := excCause

  // ================================ 仿真 ================================ //
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