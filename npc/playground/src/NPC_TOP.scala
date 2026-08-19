package playground

import spinal.core._
import spinal.lib._       
import spinal.lib.bus.amba4.axi._

// NPC使用的顶层模块
case class NPC_TOP(config: CpuConfig = CpuConfig.npc) extends Component {
  val io = new Bundle {
  }

  clockDomain.clock.setName("clock")  // 自定义时钟和复位信号名称，放在最顶层
  clockDomain.reset.setName("reset")

  val cpu = ysyx_23060082(config)

  // AXI 主接口接软件模拟内存 (NpcMemRW, 通过 DPI-C 访问宿主内存)
  val axi4MemSlave = ysyx_23060082_Axi4MemSlave()
  axi4MemSlave.io.axi4 <> cpu.io.io_master
}

/* ****************************************************************
  AXI4 从机接口 -> NpcMemRW (软件模拟内存, 通过 DPI-C 读写宿主内存)
  处理握手/响应, 并对 NpcMemRW 的黑盒接口做适配
**************************************************************** */
case class ysyx_23060082_Axi4MemSlave() extends Component {
  val io = new Bundle {
    val axi4 = slave(Axi4(AxiConfig.axiConfig))
  }

  Axi4SpecRenamer(io.axi4)

  val memRW = NpcMemRW()
  memRW.io.wen   := io.axi4.aw.valid && io.axi4.w.valid
  memRW.io.valid := io.axi4.ar.valid || memRW.io.wen
  memRW.io.addr  := memRW.io.wen ? io.axi4.aw.addr | io.axi4.ar.addr
  memRW.io.wdata := io.axi4.w.data.asUInt
  memRW.io.wmask := io.axi4.w.strb.asUInt

  // ------------------------- 读通道 ------------------------- //
  io.axi4.ar.ready := io.axi4.ar.valid
  val rValid = RegInit(False)
  when(io.axi4.ar.valid) {          // 读请求到达, 下一拍返回数据
    rValid := True
  } elsewhen(io.axi4.r.fire) {
    rValid := False
  } otherwise {
    rValid := rValid
  }
  io.axi4.r.valid := rValid
  io.axi4.r.data  := memRW.io.rdata.asBits
  io.axi4.r.resp  := B"2'b00"       // OKAY
  io.axi4.r.last  := rValid         // 突发长度1, 返回即最后
  io.axi4.r.id    := RegNextWhen(io.axi4.ar.id, io.axi4.ar.fire) init(0)

  // ------------------------- 写通道 ------------------------- //
  val wAllValid = io.axi4.aw.valid && io.axi4.w.valid
  io.axi4.aw.ready := wAllValid
  io.axi4.w.ready  := wAllValid
  val bValid = RegInit(False)
  when(wAllValid) {                 // 写请求到达, 下一拍返回写响应
    bValid := True
  } elsewhen(io.axi4.b.fire) {
    bValid := False
  } otherwise {
    bValid := bValid
  }
  io.axi4.b.valid := bValid
  io.axi4.b.resp  := B"2'b00"       // OKAY
  io.axi4.b.id    := RegNextWhen(io.axi4.aw.id, io.axi4.aw.fire) init(0)
}
