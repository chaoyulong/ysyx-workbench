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
  memRW.io.wdata := io.axi4.w.data.asUInt
  memRW.io.wmask := io.axi4.w.strb.asUInt

  // ------------------------- 读通道 (支持 INCR 突发) ------------------------- //
  val arFire     = io.axi4.ar.fire
  val readBase   = RegNextWhen(io.axi4.ar.addr, io.axi4.ar.fire)  // 突发起始地址
  val readLen    = RegNextWhen(io.axi4.ar.len , io.axi4.ar.fire)  // 突发长度 (len)
  val readCnt    = Reg(UInt(8 bits)) init(0)    // 已返回数据节拍数
  val readActive = RegInit(False)               // 读传输进行中

  when(arFire) {                                // 读地址握手: 开始新的突发
    readCnt := 0
  } elsewhen (io.axi4.r.fire && readCnt =/= readLen) {  // 读数据握手, 还有后续拍
    readCnt := readCnt + 1
  } otherwise {
    readCnt := readCnt
  }

  when(arFire) {                                // 读地址握手: 传输开始
    readActive := True
  } elsewhen(io.axi4.r.fire && readCnt === readLen) {   // 最后一拍, 传输结束
    readActive := False
  } otherwise {
    readActive := readActive
  }

  io.axi4.ar.ready := !readActive                        // 传输中不应答新请求
  io.axi4.r.valid  := readActive
  io.axi4.r.data   := memRW.io.rdata.asBits
  io.axi4.r.resp   := Axi4.resp.OKAY                    // 正常访问成功
  io.axi4.r.last   := readActive && (readCnt === readLen)   // 最后一拍
  io.axi4.r.id     := RegNextWhen(io.axi4.ar.id, arFire) init(0)

  // NpcMemRW 读请求: arFire 拍读首地址, 之后仅在还有后续节拍(cnt < len)时继续读
  // 避免 len=0 时多发一次越界读
  memRW.io.valid := memRW.io.wen || arFire || (readActive && (readCnt < readLen))
  memRW.io.addr  := Mux(memRW.io.wen, io.axi4.aw.addr,
                    Mux(arFire, io.axi4.ar.addr,
                        readBase + ((readCnt + 1) << io.axi4.ar.size)))

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
  io.axi4.b.resp  := Axi4.resp.OKAY          // 正常访问成功
  io.axi4.b.id    := RegNextWhen(io.axi4.aw.id, io.axi4.aw.fire) init(0)
}
