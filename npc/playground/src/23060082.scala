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

case class ysyx_23060082(config: CpuConfig = CpuConfig()) extends Component {
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
  io.io_slave.b.resp   := Axi4.resp.OKAY
  io.io_slave.ar.ready := False
  io.io_slave.r.valid  := False
  io.io_slave.r.data   := B(0)
  io.io_slave.r.resp   := Axi4.resp.OKAY
  io.io_slave.r.last   := False
  io.io_slave.r.id     := U(0)

  // ================================ 定义级间寄存器函数 ================================ //
  def pipelineConnect[T <: Data, T2 <: Data](
    prevOut: Stream[T],     // 前一级的输出
    thisIn:  Flow[T],       // 这一级的输入  
    thisOut: Stream[T2],    // 这一级的输出  
    flush: Bool = False,    // 本级里的指令需要冲刷
    block: Bool = False     // 本级里可能留有冲刷指令的发起者，只挡上游, 不清
  ) = {

    val payloadReg = RegNextWhen(prevOut.payload, prevOut.fire)     // 握手成功更新寄存器
    val validReg = RegInit(False)
    
    when(flush){                                  // 冲刷寄存器，优先级最高
      validReg := False
    } elsewhen(prevOut.fire && !block) {          // 上游握手成功，说明当前数据处于有效状态，并且
      validReg := True
    } elsewhen(thisOut.fire) {                    // 下游握手成功，说明当前数据已经无用，进入无效状态
      validReg := False
    } otherwise {
      validReg := validReg
    }
    
    thisIn.payload := payloadReg                  // 接入到当前级
    thisIn.valid   := validReg                    // 每一级的有效状态为数据有效状态
    prevOut.ready  := !validReg || thisOut.fire   // 当数据无效，或者下游握手成功即将无效，此时ready置1,表示可以接收新的数据
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
  val ifu     = ysyx_23060082_IFU(config)
  val idu     = ysyx_23060082_IDU(config)
  val exu     = ysyx_23060082_EXU(config)
  val lsu     = ysyx_23060082_LSU(config)
  val wbu     = ysyx_23060082_WBU(config)
  

  // ================================ 重定向与冲刷 ================================ //
  val exuRedir    = exu.io.redirect.valid
  val lsuRedir    = lsu.io.redirect.valid
  val redirectAny = exuRedir || lsuRedir

  ifu.io.redirect.valid  := redirectAny                                                   // EXU,LSU有一个要重定向，就会导致ifu重启
  ifu.io.redirect.pcNext := Mux(lsuRedir, lsu.io.redirect.pcNext, exu.io.redirect.pcNext) // LSU 更老, 优先
  ifu.io.redirect.fenceI := Mux(lsuRedir, lsu.io.redirect.fenceI, exu.io.redirect.fenceI)
                                        

  // 只冲上游（即更年轻）的寄存器，WBU不动
  pipelineConnect(ifu.io.output, idu.io.input, idu.io.output, flush = redirectAny)               
  pipelineConnect(idu.io.output, exu.io.input, exu.io.output, flush = lsuRedir, block = redirectAny)  // lsu发起冲刷才会冲掉exu中的数据
  pipelineConnect(exu.io.output, lsu.io.input, lsu.io.output, block = lsuRedir)                       // 这级寄存器就是lsu本身寄存器，不能直接清除，直接阻挡
  pipelineConnectLast(lsu.io.output, wbu.io.input)   // wbu是最后一级，没有thisOut

  regFile.io.readBus  <> idu.io.rfRead
  regFile.io.writeBus <> wbu.io.rfWrite

  idu.io.exuForward <> exu.io.forward
  idu.io.lsuForward <> lsu.io.forward
  idu.io.wbuForward <> wbu.io.forward

  // ================================ xbar ================================ //
  val xbar  = ysyx_23060082_AXI4Xbar()
  val clint = ysyx_23060082_Clint()       
  xbar.io.externalAxi4 <> io.io_master    // 引到外部
  xbar.io.clintAxi4    <> clint.io.clintAxi4
  xbar.io.ifuAxi4      <> ifu.io.axi4
  xbar.io.lsuAxi4      <> lsu.io.axi4


  // ==================== 仿真专用: mtrace 访存踪迹 (与 itrace 同一块) ====================
  // 黑盒实例化(寄存器在 dpi-c.v), 端口连接提供fanout, 指令逐级传递到WBU
  if (config.enableSimDebug) {
    

  }
}

