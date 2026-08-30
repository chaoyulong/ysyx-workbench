package playground

import spinal.core._
import spinal.core.sim._
import spinal.lib._       // 使用spinal的模块库
import spinal.lib.bus.amba4.axi._

// axi的配置信息
object AxiConfig {
  val axiConfig = Axi4Config(
    addressWidth = 32,
    dataWidth    = 32,
    idWidth      = 4 ,
    useId        = true,
    useBurst     = true,
    useSize      = true,
    useLen       = true,
    useLast      = true,
    useResp      = true,
    useStrb      = true,      
    useRegion    = false,
    useLock      = false,
    useCache     = false,
    useQos       = false,
    useProt      = false
  )
}

case class ysyx_23060082(config: CpuConfig = CpuConfig.default) extends Component {
  val io = new Bundle {
    val interrupt = in Bool()
    val io_master = master(Axi4(AxiConfig.axiConfig))
    val io_slave  = slave (Axi4(AxiConfig.axiConfig))
  }

  clockDomain.clock.setName("clock")  // 自定义时钟和复位信号名称，放在最顶层
  clockDomain.reset.setName("reset")
  ClockDomainConfig(resetActiveLevel = HIGH)  // 复位信号高有效
  Axi4SpecRenamer(io.io_master)               // 命名变为标准格式
  Axi4SpecRenamer(io.io_slave)

  io.io_slave.aw.ready := False
  io.io_slave.w.ready  := False
  io.io_slave.b.valid  := False
  io.io_slave.b.id     := U(0)
  io.io_slave.b.resp   := B(0)
  io.io_slave.ar.ready := False
  io.io_slave.r.valid  := False
  io.io_slave.r.data   := B(0)
  io.io_slave.r.resp   := B(0)
  io.io_slave.r.last   := False
  io.io_slave.r.id     := U(0)
  // ================================ 定义级间寄存器函数 ================================ //
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
  // ================================ 用于最后一级的连接 ================================ //
  def pipelineConnectLast[T <: Data](
    prevOut: Stream[T],
    thisIn:  Flow[T]
  ) = {
    val payloadReg = RegNextWhen(prevOut.payload, prevOut.fire)
    val validReg = RegInit(False)

    when(prevOut.fire) {
      validReg := True
    } otherwise {
      validReg := False
    }

    thisIn.payload := payloadReg
    thisIn.valid := validReg
    prevOut.ready := True
  }
  // ================================================================ //
  val regFile = ysyx_23060082_RegFile()
  val ifu     = ysyx_23060082_IFU(config.resetPc)
  val idu     = ysyx_23060082_IDU()
  val exu     = ysyx_23060082_EXU()
  val lsu     = ysyx_23060082_LSU()
  val wbu     = ysyx_23060082_WBU()
  
  pipelineConnect(ifu.io.output, idu.io.input, idu.io.output)
  pipelineConnect(idu.io.output, exu.io.input, exu.io.output)
  pipelineConnect(exu.io.output, lsu.io.input, lsu.io.output)
  pipelineConnectLast(lsu.io.output, wbu.io.input)   // wbu是最后一级，没有thisOut
  wbu.io.output >> ifu.io.input

  regFile.io.readBus  <> idu.io.rfRead
  regFile.io.writeBus <> wbu.io.rfWrite
  // ================================ xbar ================================ //
  val xbar = ysyx_23060082_AXI4Xbar()
  val clint = ysyx_23060082_Clint()       
  xbar.io.externalAxi4 <> io.io_master    // 引到外部
  xbar.io.clintAxi4    <> clint.io.clintAxi4
  xbar.io.ifuAxi4      <> ifu.io.axi4
  xbar.io.lsuAxi4      <> lsu.io.axi4

  // ==================== 仿真专用: itrace 指令退休追踪 (仅仿真, 不加顶层端口) ====================
  // 黑盒实例化(寄存器在 dpi-c.v), 端口连接提供fanout, 指令逐级传递到WBU
  if (config.enableSimDebug) {
    // 指令随流水线逐级传递(仿真专用寄存器链)
    val itraceInstrIdu = Reg(Bits(32 bits)) init(0)
    val itraceInstrExu = Reg(Bits(32 bits)) init(0)
    val itraceInstrLsu = Reg(Bits(32 bits)) init(0)
    val itraceInstrWbu = Reg(Bits(32 bits)) init(0)

    when(ifu.io.output.fire) { itraceInstrIdu := ifu.io.output.instr.asBits }
    when(idu.io.output.fire) { itraceInstrExu := itraceInstrIdu }
    when(exu.io.output.fire) { itraceInstrLsu := itraceInstrExu }
    when(lsu.io.output.fire) { itraceInstrWbu := itraceInstrLsu }

    val itrace = ItraceReg()          // 黑盒: 寄存器在 dpi-c.v, C++ 侧直接读取
    itrace.io.valid := wbu.io.input.valid   // 指令到达WBU = 执行完毕
    itrace.io.pc    := wbu.io.input.pc
    itrace.io.instr := itraceInstrWbu.asUInt

    // ==================== 仿真专用: mtrace 访存踪迹 (与 itrace 同一块) ====================
    // 截取 lsuAxi4(仅数据访存, 不含取指), 按地址范围区分内存/设备
    val mtrace = MtraceReg()
    val lsuArFire = xbar.io.lsuAxi4.ar.fire                       // 读请求握手
    val lsuWrFire = xbar.io.lsuAxi4.aw.fire && xbar.io.lsuAxi4.w.fire   // 写握手
    // 内存范围 [resetPc, resetPc + 128MB), 之外视为设备(串口/键盘/RTC/VGA)
    // 用 resetPc 派生: npc 与 ysyxSoc 统一布局(0x30000000)
    def isDevAddr(addr: UInt) = (addr < U(config.resetPc, 32 bits)) ||
                                (addr >= U(config.resetPc + 0x08000000L, 32 bits))
    mtrace.io.valid := lsuArFire || lsuWrFire
    mtrace.io.wen   := lsuWrFire
    mtrace.io.isDev := Mux(lsuArFire, isDevAddr(xbar.io.lsuAxi4.ar.addr),
                                      isDevAddr(xbar.io.lsuAxi4.aw.addr))
    mtrace.io.addr  := Mux(lsuArFire, xbar.io.lsuAxi4.ar.addr, xbar.io.lsuAxi4.aw.addr)
    mtrace.io.wdata := xbar.io.lsuAxi4.w.data.asUInt
  }
}

