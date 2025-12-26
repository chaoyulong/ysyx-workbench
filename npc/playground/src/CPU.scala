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

case class CPU() extends Component {
  val io = new Bundle {
    val externalAxi = master(Axi4(AxiConfig.axiConfig))
  }

  clockDomain.clock.setName("clock")  // 自定义时钟和复位信号名称，放在最顶层
  clockDomain.reset.setName("reset")

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
  val ifu = ysyx_23060082_IFU()
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
  io.externalAxi <> xbar.io.externalAxi
  xbar.io.clintAxi <> clint.io.clintAxi
  xbar.io.ifuAXI4 <> ifu.io.axi4
  xbar.io.lsuAXI4 <> lsu.io.axi4

  val axiRValid = RegInit(False)
  val axiBValid = RegInit(False)
  val npcMemRW = NpcMemRW()
  npcMemRW.io.wen   := xbar.io.externalAxi.aw.valid && xbar.io.externalAxi.w.valid
  npcMemRW.io.valid := xbar.io.externalAxi.ar.valid || npcMemRW.io.wen
  npcMemRW.io.addr  := npcMemRW.io.wen ? xbar.io.externalAxi.aw.addr | xbar.io.externalAxi.ar.addr
  npcMemRW.io.wdata := xbar.io.externalAxi.w.data.asUInt
  npcMemRW.io.wmask := xbar.io.externalAxi.w.strb.asUInt

  xbar.io.externalAxi.ar.ready := xbar.io.externalAxi.ar.valid
  xbar.io.externalAxi.r.data := npcMemRW.io.rdata.asBits    // 数据
  when (xbar.io.externalAxi.ar.valid) {   // 读数据通道握手信号
    axiRValid := True
  } elsewhen (xbar.io.externalAxi.r.fire) {
    axiRValid := False
  } otherwise {
    axiRValid := axiRValid
  }
  xbar.io.externalAxi.r.valid := axiRValid
  //***************
  xbar.io.externalAxi.aw.ready := xbar.io.externalAxi.aw.valid && xbar.io.externalAxi.w.valid
  xbar.io.externalAxi.w.ready  := xbar.io.externalAxi.aw.valid && xbar.io.externalAxi.w.valid
  when (xbar.io.externalAxi.aw.valid && xbar.io.externalAxi.w.valid) {   
    axiBValid := True
  } elsewhen (xbar.io.externalAxi.b.fire) {
    axiBValid := False
  } otherwise {
    axiBValid := axiBValid
  }
  xbar.io.externalAxi.b.valid := axiBValid 
}

case class ysyx_23060082_RegFile() extends Component {
  val io = new Bundle {
    val readAddr1 = in UInt(5 bits)
    val readAddr2 = in UInt(5 bits)
    val writeAddr  = in UInt(5 bits)
    val writeData  = in UInt(32 bits)
    val writeEn    = in Bool()

    val readData1 = out UInt(32 bits)
    val readData2 = out UInt(32 bits)
  }

  val rf = Vec(Reg(UInt(32 bits)),16)    // riscv32e,有16个通用寄存器
  when(io.writeEn){
    rf(io.writeAddr(3 downto 0)) := io.writeData
  }

  when(True){
    rf(0) := U"32'h0"   // 0号寄存器固定为0
  }

  io.readData1 := rf(io.readAddr1(0 to 3))
  io.readData2 := rf(io.readAddr2(0 to 3)) 
}

case class ysyx_23060082_Clint() extends Component {
  val io = new Bundle {
    val clintAxi = slave(Axi4(AxiConfig.axiConfig))
  }

  val timeCount = RegInit(U"64'h0")   // 系统计时器
  val timeCountLow = RegNextWhen(timeCount(31 downto 0), io.clintAxi.ar.fire && (io.clintAxi.ar.addr === U"32'h02000004")) init(0)  // 当读取高位数据时暂存低位数据
  timeCount := timeCount + 1

  io.clintAxi.r.valid.setAsReg() init(False)
  io.clintAxi.b.valid.setAsReg() init(False)
  io.clintAxi.r.data .setAsReg() init(0)

  // ---------- 读通道 ---------- //
  io.clintAxi.ar.ready := io.clintAxi.ar.valid
  when(io.clintAxi.ar.valid) {   // 读数据通道握手信号
    io.clintAxi.r.valid := True
  } elsewhen (io.clintAxi.r.fire) {
    io.clintAxi.r.valid := False
  } otherwise {
    io.clintAxi.r.valid := io.clintAxi.r.valid
  }

  when(io.clintAxi.ar.fire) {   // 读数据通道握手信号
    io.clintAxi.r.data := io.clintAxi.ar.addr.mux(
      U"32'h02000004" -> timeCount(63 downto 32),
      U"32'h02000000" -> timeCountLow,
      default         -> U(0)
    ).asBits
  } otherwise {
    io.clintAxi.r.data := io.clintAxi.r.data
  }
  // ---------- 写通道 ---------- //
  val wAllValid = io.clintAxi.aw.valid && io.clintAxi.w.valid
  io.clintAxi.aw.ready := wAllValid
  io.clintAxi.w.ready  := wAllValid
  when(wAllValid) {   
    io.clintAxi.b.valid := True
  } elsewhen (io.clintAxi.b.fire) {
    io.clintAxi.b.valid := False
  } otherwise {
    io.clintAxi.b.valid := io.clintAxi.b.valid
  }
 
}
