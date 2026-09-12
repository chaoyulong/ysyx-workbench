file://<WORKSPACE>/playground/src/NPC_TOP.scala
empty definition using pc, found symbol in pc: 
semanticdb not found
empty definition using fallback
non-local guesses:
	 -spinal/core/cpu/axiMaster.
	 -spinal/lib/cpu/axiMaster.
	 -spinal/lib/bus/amba4/axi/cpu/axiMaster.
	 -cpu/axiMaster.
	 -scala/Predef.cpu.axiMaster.
offset: 444
uri: file://<WORKSPACE>/playground/src/NPC_TOP.scala
text:
```scala
package playground

import spinal.core._
import spinal.lib._       
import spinal.lib.bus.amba4.axi._

// NPC使用的顶层模块
case class NPC_TOP() extends Component {
  val io = new Bundle {
  }

  clockDomain.clock.setName("clock")  // 自定义时钟和复位信号名称，放在最顶层
  clockDomain.reset.setName("reset")

  val cpu = ysyx_23060082()

  val axiRValid = RegInit(False)
  val axiBValid = RegInit(False)
  val npcMemRW = NpcMemRW()
  npcMemRW.io.wen   := cpu.axiMaster@@.aw.valid && cpu.axiMaster.w.valid
  npcMemRW.io.valid := cpu.axiMaster.ar.valid || npcMemRW.io.wen
  npcMemRW.io.addr  := npcMemRW.io.wen ? cpu.axiMaster.aw.addr | cpu.axiMaster.ar.addr
  npcMemRW.io.wdata := cpu.axiMaster.w.data.asUInt
  npcMemRW.io.wmask := cpu.axiMaster.w.strb.asUInt

  cpu.axiMaster.ar.ready := cpu.axiMaster.ar.valid
  cpu.axiMaster.r.data := npcMemRW.io.rdata.asBits    // 数据
  when (cpu.axiMaster.ar.valid) {   // 读数据通道握手信号
    axiRValid := True
  } elsewhen (cpu.axiMaster.r.fire) {
    axiRValid := False
  } otherwise {
    axiRValid := axiRValid
  }
  cpu.axiMaster.r.valid := axiRValid
  //***************
  cpu.axiMaster.aw.ready := cpu.axiMaster.aw.valid && cpu.axiMaster.w.valid
  cpu.axiMaster.w.ready  := cpu.axiMaster.aw.valid && cpu.axiMaster.w.valid
  when (cpu.axiMaster.aw.valid && cpu.axiMaster.w.valid) {   
    axiBValid := True
  } elsewhen (cpu.axiMaster.b.fire) {
    axiBValid := False
  } otherwise {
    axiBValid := axiBValid
  }
  cpu.axiMaster.b.valid := axiBValid 

}
```


#### Short summary: 

empty definition using pc, found symbol in pc: 