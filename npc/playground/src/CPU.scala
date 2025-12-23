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


  regFile.io.readAddr1 <> idu.io.rf_read_addr_1
  regFile.io.readAddr2 <> idu.io.rf_read_addr_2
  regFile.io.readData1 <> idu.io.rf_read_data_1
  regFile.io.readData2 <> idu.io.rf_read_data_2
  regFile.io.writeAddr <> wbu.io.rf_write_addr
  regFile.io.writeData <> wbu.io.rf_write_data
  regFile.io.writeEn   <> wbu.io.rf_write_en  


  // ----------------------------------- 暂时的axi从机 ----------------------------------- //
  val axi4Slave = slave(Axi4ReadOnly(AxiConfig.axiConfig))
  ifu.io.axi4 <> axi4Slave
  axi4Slave.r.valid.setAsReg() init(False)

  // val readAddr  = RegNextWhen(axi4Slave.ar.addr, axi4Slave.ar.valid)  init(U"32'b0")
  // val ar_id     = RegNextWhen(axi4Slave.ar.id, axi4Slave.ar.valid)    init(U"4'b0")
  // val ar_len    = RegNextWhen(axi4Slave.ar.len, axi4Slave.ar.valid)   init(U"8'b0"  )        // 突发长度
  // val ar_size   = RegNextWhen(axi4Slave.ar.size , axi4Slave.ar.valid) init(U"3'b010")        // 突发大小
  // val ar_burst  = RegNextWhen(axi4Slave.ar.burst, axi4Slave.ar.valid) init(B"2'b01" )        // 突发类型
  axi4Slave.ar.ready := axi4Slave.ar.valid

  val npcMemRead = NpcMemRead()
  npcMemRead.io.valid := axi4Slave.ar.valid
  npcMemRead.io.addr  := axi4Slave.ar.addr
  axi4Slave.r.data := npcMemRead.io.rdata.asBits
  when (axi4Slave.ar.valid) {
    axi4Slave.r.valid := True
  } elsewhen (axi4Slave.r.ready) {
    axi4Slave.r.valid := False
  } otherwise {
    axi4Slave.r.valid := axi4Slave.r.valid
  }
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


