error id: file://<WORKSPACE>/playground/src/NPC_TOP.scala:
file://<WORKSPACE>/playground/src/NPC_TOP.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -spinal/core/cpu/io/axiMaster.
	 -spinal/lib/cpu/io/axiMaster.
	 -spinal/lib/bus/amba4/axi/cpu/io/axiMaster.
	 -cpu/io/axiMaster.
	 -scala/Predef.cpu.io.axiMaster.
offset: 443
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
  npcMemRW.io.wen   := cpu.io.axiMa@@ster.aw.valid && cpu.io.axiMaster.w.valid
  npcMemRW.io.valid := cpu.io.axiMaster.ar.valid || npcMemRW.io.wen
  npcMemRW.io.addr  := npcMemRW.io.wen ? cpu.io.axiMaster.aw.addr | cpu.io.axiMaster.ar.addr
  npcMemRW.io.wdata := cpu.io.axiMaster.w.data.asUInt
  npcMemRW.io.wmask := cpu.io.axiMaster.w.strb.asUInt

  cpu.io.axiMaster.ar.ready := cpu.io.axiMaster.ar.valid
  cpu.io.axiMaster.r.data := npcMemRW.io.rdata.asBits    // 数据
  when (cpu.io.axiMaster.ar.valid) {   // 读数据通道握手信号
    axiRValid := True
  } elsewhen (cpu.io.axiMaster.r.fire) {
    axiRValid := False
  } otherwise {
    axiRValid := axiRValid
  }
  cpu.io.axiMaster.r.valid := axiRValid
  //***************
  cpu.io.axiMaster.aw.ready := cpu.io.axiMaster.aw.valid && cpu.io.axiMaster.w.valid
  cpu.io.axiMaster.w.ready  := cpu.io.axiMaster.aw.valid && cpu.io.axiMaster.w.valid
  when (cpu.io.axiMaster.aw.valid && cpu.io.axiMaster.w.valid) {   
    axiBValid := True
  } elsewhen (cpu.io.axiMaster.b.fire) {
    axiBValid := False
  } otherwise {
    axiBValid := axiBValid
  }
  cpu.io.axiMaster.b.valid := axiBValid 

}
```


#### Short summary: 

empty definition using pc, found symbol in pc: 