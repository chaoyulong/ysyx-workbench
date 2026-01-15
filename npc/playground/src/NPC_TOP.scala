package playground

import spinal.core._
import spinal.lib._       
import spinal.lib.bus.amba4.axi._

// NPC使用的顶层模块
case class NPC_TOP(config: CpuConfig = CpuConfig(BigInt("80000000", 16))) extends Component {
  val io = new Bundle {
  }

  clockDomain.clock.setName("clock")  // 自定义时钟和复位信号名称，放在最顶层
  clockDomain.reset.setName("reset")

  val cpu = ysyx_23060082(config)

  val axiRValid = RegInit(False)
  val axiBValid = RegInit(False)
  val npcMemRW = NpcMemRW()
  npcMemRW.io.wen   := cpu.io.io_master.aw.valid && cpu.io.io_master.w.valid
  npcMemRW.io.valid := cpu.io.io_master.ar.valid || npcMemRW.io.wen
  npcMemRW.io.addr  := npcMemRW.io.wen ? cpu.io.io_master.aw.addr | cpu.io.io_master.ar.addr
  npcMemRW.io.wdata := cpu.io.io_master.w.data.asUInt
  npcMemRW.io.wmask := cpu.io.io_master.w.strb.asUInt

  cpu.io.io_master.ar.ready := cpu.io.io_master.ar.valid
  cpu.io.io_master.r.data := npcMemRW.io.rdata.asBits    // 数据
  when (cpu.io.io_master.ar.valid) {   // 读数据通道握手信号
    axiRValid := True
  } elsewhen (cpu.io.io_master.r.fire) {
    axiRValid := False
  } otherwise {
    axiRValid := axiRValid
  }
  cpu.io.io_master.r.valid := axiRValid
  //***************
  cpu.io.io_master.aw.ready := cpu.io.io_master.aw.valid && cpu.io.io_master.w.valid
  cpu.io.io_master.w.ready  := cpu.io.io_master.aw.valid && cpu.io.io_master.w.valid
  when (cpu.io.io_master.aw.valid && cpu.io.io_master.w.valid) {   
    axiBValid := True
  } elsewhen (cpu.io.io_master.b.fire) {
    axiBValid := False
  } otherwise {
    axiBValid := axiBValid
  }
  cpu.io.io_master.b.valid := axiBValid 

}