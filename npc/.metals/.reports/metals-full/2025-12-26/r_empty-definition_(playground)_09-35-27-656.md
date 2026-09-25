error id: file://<WORKSPACE>/playground/src/CPU.scala:wen
file://<WORKSPACE>/playground/src/CPU.scala
empty definition using pc, found symbol in pc: wen
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -spinal/core/npcMemRW/io/wen.
	 -spinal/core/npcMemRW/io/wen#
	 -spinal/core/npcMemRW/io/wen().
	 -spinal/lib/npcMemRW/io/wen.
	 -spinal/lib/npcMemRW/io/wen#
	 -spinal/lib/npcMemRW/io/wen().
	 -spinal/lib/bus/amba4/axi/npcMemRW/io/wen.
	 -spinal/lib/bus/amba4/axi/npcMemRW/io/wen#
	 -spinal/lib/bus/amba4/axi/npcMemRW/io/wen().
	 -npcMemRW/io/wen.
	 -npcMemRW/io/wen#
	 -npcMemRW/io/wen().
	 -scala/Predef.npcMemRW.io.wen.
	 -scala/Predef.npcMemRW.io.wen#
	 -scala/Predef.npcMemRW.io.wen().
offset: 3116
uri: file://<WORKSPACE>/playground/src/CPU.scala
text:
```scala
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
  xbar.io.externalAxi <> clint.io.clintAxi
  xbar.io.ifuAXI4 <> ifu.io.axi4
  xbar.io.lsuAXI4 <> lsu.io.axi4

  val axiRValid = RegInit(False)
  val axiBValid = RegInit(False)
  val npcMemRW = NpcMemRW()
  npcMemRW.io.wen   := xbar.io.clintAxi.aw.valid && xbar.io.clintAxi.w.valid
  npcMemRW.io.valid := xbar.io.clintAxi.ar.valid || npcMemRW.io.wen
  npcMemRW.io.addr  := npcMemRW.io.we@@n ? xbar.io.clintAxi.aw.addr | xbar.io.externalAxi.ar.addr
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

  // val xbar = ysyx_23060082_AXI4Xbar()
  // val clint = ysyx_23060082_Clint()
  // xbar.io.clintAxi <> clint.io.clintAxi
  // xbar.io.ifuAXI4 <> ifu.io.axi4
  // xbar.io.lsuAXI4 <> lsu.io.axi4

  // val axiRValid = RegInit(False)
  // val axiBValid = RegInit(False)
  // val npcMemRW = NpcMemRW()
  // npcMemRW.io.wen   := xbar.io.externalAxi.aw.valid && xbar.io.externalAxi.w.valid
  // npcMemRW.io.valid := xbar.io.externalAxi.ar.valid || npcMemRW.io.wen
  // npcMemRW.io.addr  := npcMemRW.io.wen ? xbar.io.externalAxi.aw.addr | xbar.io.externalAxi.ar.addr
  // npcMemRW.io.wdata := xbar.io.externalAxi.w.data.asUInt
  // npcMemRW.io.wmask := xbar.io.externalAxi.w.strb.asUInt

  // xbar.io.externalAxi.ar.ready := xbar.io.externalAxi.ar.valid
  // xbar.io.externalAxi.r.data := npcMemRW.io.rdata.asBits    // 数据
  // when (xbar.io.externalAxi.ar.valid) {   // 读数据通道握手信号
  //   axiRValid := True
  // } elsewhen (xbar.io.externalAxi.r.fire) {
  //   axiRValid := False
  // } otherwise {
  //   axiRValid := axiRValid
  // }
  // xbar.io.externalAxi.r.valid := axiRValid
  // //***************
  // xbar.io.externalAxi.aw.ready := xbar.io.externalAxi.aw.valid && xbar.io.externalAxi.w.valid
  // xbar.io.externalAxi.w.ready  := xbar.io.externalAxi.aw.valid && xbar.io.externalAxi.w.valid
  // when (xbar.io.externalAxi.aw.valid && xbar.io.externalAxi.w.valid) {   
  //   axiBValid := True
  // } elsewhen (xbar.io.externalAxi.b.fire) {
  //   axiBValid := False
  // } otherwise {
  //   axiBValid := axiBValid
  // }
  // xbar.io.externalAxi.b.valid := axiBValid 


//   val ifuRValid = RegInit(False)
//   ifu.io.axi4.ar.ready := ifu.io.axi4.ar.valid

//   val npcMemRead = NpcMemRead()
//   npcMemRead.io.valid := ifu.io.axi4.ar.valid
//   npcMemRead.io.addr  := ifu.io.axi4.ar.addr

//   ifu.io.axi4.r.data := npcMemRead.io.rdata.asBits    // 数据
//   when (ifu.io.axi4.ar.valid) {   // 读数据通道握手信号
//     ifuRValid := True
//   } elsewhen (ifu.io.axi4.r.fire) {
//     ifuRValid := False
//   } otherwise {
//     ifuRValid := ifuRValid
//   }
//   ifu.io.axi4.r.valid := ifuRValid
// //******************************************************
//   val lsuRValid = RegInit(False)
//   val lsuBValid = RegInit(False)
//   val npcMemRW = NpcMemRW()
//   npcMemRW.io.wen   := lsu.io.axi4.aw.valid && lsu.io.axi4.w.valid
//   npcMemRW.io.valid := lsu.io.axi4.ar.valid || npcMemRW.io.wen
//   npcMemRW.io.addr  := npcMemRW.io.wen ? lsu.io.axi4.aw.addr | lsu.io.axi4.ar.addr
//   npcMemRW.io.wdata := lsu.io.axi4.w.data.asUInt
//   npcMemRW.io.wmask := lsu.io.axi4.w.strb.asUInt

//   lsu.io.axi4.ar.ready := lsu.io.axi4.ar.valid
//   lsu.io.axi4.r.data := npcMemRW.io.rdata.asBits    // 数据
//   when (lsu.io.axi4.ar.valid) {   // 读数据通道握手信号
//     lsuRValid := True
//   } elsewhen (lsu.io.axi4.r.fire) {
//     lsuRValid := False
//   } otherwise {
//     lsuRValid := lsuRValid
//   }
//   lsu.io.axi4.r.valid := lsuRValid
//   //***************
//   lsu.io.axi4.aw.ready := lsu.io.axi4.aw.valid && lsu.io.axi4.w.valid
//   lsu.io.axi4.w.ready  := lsu.io.axi4.aw.valid && lsu.io.axi4.w.valid
//   when (lsu.io.axi4.aw.valid && lsu.io.axi4.w.valid) {   // 读数据通道握手信号
//     lsuBValid := True
//   } elsewhen (lsu.io.axi4.b.fire) {
//     lsuBValid := False
//   } otherwise {
//     lsuBValid := lsuBValid
//   }
//   lsu.io.axi4.b.valid := lsuBValid 

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
  .otherwise{rf := rf}

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

  val timeCount = RegInit(U"64'h0")
  val timeCountLow = RegInit(U"32'h0")
  val rValid = RegInit(False)
  val bValid = RegInit(False)
  val rdata = RegInit(U"32'h0")

  timeCount := timeCount + 1

  io.clintAxi.ar.ready := io.clintAxi.ar.valid
  io.clintAxi.r.data := rdata.asBits    // 数据
  when(io.clintAxi.ar.valid) {   // 读数据通道握手信号
    rValid := True
  } elsewhen (io.clintAxi.r.fire) {
    rValid := False
  } otherwise {
    rValid := rValid
  }

  when(io.clintAxi.ar.fire) {   // 读数据通道握手信号
    rdata := io.clintAxi.ar.addr.mux(
      // U"32'h02000000" -> timeCount(63 downto 32),
      // U"32'h02000004" -> timeCountLow,
      // default         -> U(0)
      U"32'h02000000" -> U"32'd1111",
      U"32'h02000004" -> U"32'd2222",
      default         -> U(0)
    )
  } otherwise {
    rdata := rdata
  }

  when(io.clintAxi.ar.fire && (io.clintAxi.ar.addr === U"32'h02000000")) {
    timeCountLow := timeCount(31 downto 0)
  } otherwise {
    timeCountLow := timeCountLow
  }

  io.clintAxi.r.valid := rValid
  //***************
  io.clintAxi.aw.ready := io.clintAxi.aw.valid && io.clintAxi.w.valid
  io.clintAxi.w.ready  := io.clintAxi.aw.valid && io.clintAxi.w.valid
  when (io.clintAxi.aw.valid && io.clintAxi.w.valid) {   
    bValid := True
  } elsewhen (io.clintAxi.b.fire) {
    bValid := False
  } otherwise {
    bValid := bValid
  }
  io.clintAxi.b.valid := bValid 
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: wen