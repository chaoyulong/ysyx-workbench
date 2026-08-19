package playground

import spinal.core._
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

case class ysyx_23060082(config: CpuConfig = CpuConfig.ysyxSoc) extends Component {
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
  // ------------------------------------------------------ 定义级间寄存器函数 ------------------------------------------------------ //
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
  // ------------------------ 用于最后一级的连接
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
  // ------------------------------------------------------------------------------------------------------------------------- //

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

  // ----------------------------------- 暂时的axi从机 ----------------------------------- //
  val xbar = ysyx_23060082_AXI4Xbar()
  val clint = ysyx_23060082_Clint()
  xbar.io.externalAxi4 <> io.io_master   // 引到外部
  xbar.io.clintAxi4 <> clint.io.clintAxi4
  xbar.io.ifuAxi4 <> ifu.io.axi4
  xbar.io.lsuAxi4 <> lsu.io.axi4
}

case class ysyx_23060082_Clint() extends Component {
  val io = new Bundle {
    val clintAxi4 = slave(Axi4(AxiConfig.axiConfig))
  }

  val timeCount = RegInit(U"64'h0")   // 系统计时器
  timeCount := timeCount + 1

  // 双向快照: 读任一侧(低位/高位)都同时锁存高低位, 任意读取顺序下高低位保持一致
  val readLow  = io.clintAxi4.ar.fire && (io.clintAxi4.ar.addr === U"32'h02000000")
  val readHigh = io.clintAxi4.ar.fire && (io.clintAxi4.ar.addr === U"32'h02000004")
  val timeCountLow  = RegNextWhen(timeCount(31 downto 0),  readLow || readHigh) init(0)
  val timeCountHigh = RegNextWhen(timeCount(63 downto 32), readLow || readHigh) init(0)

  io.clintAxi4.b.valid.setAsReg() init(False)

  // ---------- 读通道 (支持突发: 按 len 计数, last 在最后一拍) ---------- //
  val readLen    = RegNextWhen(io.clintAxi4.ar.len, io.clintAxi4.ar.fire) init(0)    // 突发长度 (len),读地址握手成功后更新
  val readCnt    = Reg(UInt(8 bits)) init(0)    // 已返回数据节拍数
  val readActive = RegInit(False)               // 读传输进行中

  when(io.clintAxi4.ar.fire) {                  // 读地址握手
    readCnt := 0
  } elsewhen (io.clintAxi4.r.fire && readCnt =/= readLen) { // 读数据握手，当突发传输时，会连续握手几次
    readCnt := readCnt + 1                                  // 没到最后一拍, 每次握手计数+1
  } otherwise {
    readCnt := readCnt
  }

  when(io.clintAxi4.ar.fire) {                            // 读地址握手
    readActive := True
  } elsewhen(io.clintAxi4.r.fire && readCnt === readLen) {// 最后一拍, 传输结束
    readActive := False
  } otherwise {
    readActive := readActive
  }

  // 突发期间数据按 FIXED 语义保持: arFire 时锁存时间值, 传输期间不变
  val readData = Reg(UInt(32 bits))
  when(io.clintAxi4.ar.fire) {
    readData := io.clintAxi4.ar.addr.mux(
      U"32'h02000004" -> timeCountHigh,
      U"32'h02000000" -> timeCountLow,
      default         -> U(0)
    )
  }

  io.clintAxi4.ar.ready := !readActive                  // 传输中不应答新请求
  io.clintAxi4.r.valid  := readActive
  io.clintAxi4.r.last   := readActive && (readCnt === readLen)  // 最后一拍
  io.clintAxi4.r.data   := readData.asBits
  io.clintAxi4.r.id     := U(0)
  io.clintAxi4.r.resp   := Axi4.resp.OKAY
  // ---------- 写通道 ---------- //
  val wAllValid = io.clintAxi4.aw.valid && io.clintAxi4.w.valid
  io.clintAxi4.aw.ready := wAllValid
  io.clintAxi4.w.ready  := wAllValid
  when(wAllValid) {  
    report(Seq("should not write to there!", io.clintAxi4.aw.addr))
  }
  when(wAllValid) {   
    io.clintAxi4.b.valid := True
  } elsewhen (io.clintAxi4.b.fire) {
    io.clintAxi4.b.valid := False
  } otherwise {
    io.clintAxi4.b.valid := io.clintAxi4.b.valid
  }
  io.clintAxi4.b.id   := U(0)
  io.clintAxi4.b.resp := Axi4.resp.OKAY
}
