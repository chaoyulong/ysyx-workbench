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

case class ysyx_23060082(config: CpuConfig = CpuConfig(BigInt("30000000", 16))) extends Component {
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
  // ------------------------------------------------------------------------------------------------------------------------- //

  val regFile = ysyx_23060082_RegFile()
  val ifu = ysyx_23060082_IFU(config)
  val idu = ysyx_23060082_IDU()
  val exu = ysyx_23060082_EXU()
  val lsu = ysyx_23060082_LSU()
  val wbu = ysyx_23060082_WBU()
  
  pipelineConnect(ifu.io.output, idu.io.input, idu.io.output)
  pipelineConnect(idu.io.output, exu.io.input, exu.io.output)
  pipelineConnect(exu.io.output, lsu.io.input, lsu.io.output)
  // pipelineConnect(lsu.io.output, wbu.io.input, wbu.io.output)
  lsu.io.output >> wbu.io.input   // wbu没有下一级，直接特殊对待，写回直接在内部处理
  wbu.io.output >> ifu.io.input


  regFile.io.readAddr1 <> idu.io.rfReadAddr1
  regFile.io.readAddr2 <> idu.io.rfReadAddr2
  regFile.io.readData1 <> idu.io.rfReadData1
  regFile.io.readData2 <> idu.io.rfReadData2
  regFile.io.writeAddr <> wbu.io.rf_write_addr
  regFile.io.writeData <> wbu.io.rf_write_data
  regFile.io.writeEn   <> wbu.io.rf_write_en  

  // ----------------------------------- 暂时的axi从机 ----------------------------------- //
  val xbar = ysyx_23060082_AXI4Xbar()
  val clint = ysyx_23060082_Clint()
  xbar.io.externalAxi4 <> io.io_master   // 引到外部
  xbar.io.clintAxi4 <> clint.io.clintAxi4
  xbar.io.ifuAxi4 <> ifu.io.axi4
  xbar.io.lsuAxi4 <> lsu.io.axi4
}

case class ysyx_23060082_RegFile() extends Component {
  val io = new Bundle {
    val readAddr1 = in UInt(5 bits)
    val readAddr2 = in UInt(5 bits)
    val writeAddr = in UInt(5 bits)
    val writeData = in UInt(32 bits)
    val writeEn   = in Bool()
    val readData1 = out UInt(32 bits)
    val readData2 = out UInt(32 bits)
  }

  val rf = Vec(Reg(UInt(32 bits)),16)    // riscv32e,有16个通用寄存器

  rf(0) := U"32'h0" 
  when(io.writeEn && (io.writeAddr(3 downto 0) =/= U(0))){
    rf(io.writeAddr(3 downto 0)) := io.writeData
  }

  io.readData1 := rf(io.readAddr1(3 downto 0))
  io.readData2 := rf(io.readAddr2(3 downto 0)) 
}

case class ysyx_23060082_Clint() extends Component {
  val io = new Bundle {
    val clintAxi4 = slave(Axi4(AxiConfig.axiConfig))
  }

  val timeCount = RegInit(U"64'h0")   // 系统计时器
  val timeCountLow = RegNextWhen(timeCount(31 downto 0), io.clintAxi4.ar.fire && (io.clintAxi4.ar.addr === U"32'h02000004")) init(0)  // 当读取高位数据时暂存低位数据
  timeCount := timeCount + 1

  io.clintAxi4.r.valid.setAsReg() init(False)
  io.clintAxi4.b.valid.setAsReg() init(False)
  io.clintAxi4.r.data .setAsReg()

  // ---------- 读通道 ---------- //
  io.clintAxi4.ar.ready := io.clintAxi4.ar.valid
  when(io.clintAxi4.ar.valid) {   // 读数据通道握手信号
    io.clintAxi4.r.valid := True
  } elsewhen (io.clintAxi4.r.fire) {
    io.clintAxi4.r.valid := False
  } otherwise {
    io.clintAxi4.r.valid := io.clintAxi4.r.valid
  }

  io.clintAxi4.r.last := io.clintAxi4.r.valid
  when(io.clintAxi4.ar.fire) {   // 读数据通道握手信号
    io.clintAxi4.r.data := io.clintAxi4.ar.addr.mux(
      U"32'h02000004" -> timeCount(63 downto 32),
      U"32'h02000000" -> timeCountLow,
      default         -> U(0)
    ).asBits
  } otherwise {
    io.clintAxi4.r.data := io.clintAxi4.r.data
  }
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

}
