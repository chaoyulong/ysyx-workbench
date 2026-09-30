package playground

import spinal.core._
import spinal.lib._       // 使用spinal的模块库
import spinal.lib.bus.amba4.axi._

// CLINT: 64位系统计时器 (mtime)
// 地址由 AddressMap (定义于 Xbar.scala) 统一配置, 修改时只需改动 AddressMap
case class ysyx_23060082_Clint() extends Component {
  val io = new Bundle {
    val clintAxi4 = slave(Axi4(AxiConfig.axiConfig))
  }

  // mtime 寄存器地址 (由 AddressMap 派生, 可配置)
  private def MTIME  = U(AddressMap.CLINT_BASE + AddressMap.CLINT_MTIME,  32 bits)
  private def MTIMEH = U(AddressMap.CLINT_BASE + AddressMap.CLINT_MTIMEH, 32 bits)

  val timeCountLow  = RegInit(U"32'h0")
  val timeCountHigh = RegInit(U"32'h0")

// ================================ 读通道 ================================ //
  val readActive = RegInit(False)       // 空闲 = 不在传输中

  when(io.clintAxi4.ar.fire) {          // 读地址握手
    readActive := True
  } elsewhen(io.clintAxi4.r.fire) {     // 读数据被接受
    readActive := False
  } otherwise {
    readActive := readActive
  }

  // 地址暂存一下，打断从icache到clint这条不会存在的关键路径，之记录低4位，区分高低位即可
  val rid = RegNextWhen(io.clintAxi4.ar.id, io.clintAxi4.ar.fire)
  val raddrReg    = RegNextWhen(io.clintAxi4.ar.addr(3 downto 0), io.clintAxi4.ar.fire)
  val rdataReg     = Reg(UInt(32 bits))
  val arFireDelay = RegNext(io.clintAxi4.ar.fire) // 握手后的下一周期
  val dataFinish  = RegInit(False)

  // 读取协议: 先读低位(mtime), 硬件锁存当时的高位; 再读高位(mtimeh)返回锁存值
  val readLow  = raddrReg === U"4'h0"
  val timeCountHighSnap = RegNextWhen(timeCountHigh, arFireDelay && readLow)    // 读低那一拍锁存高位

  when(arFireDelay) {    
    dataFinish := True
  } elsewhen(io.clintAxi4.r.fire) {
    dataFinish := False
  } otherwise {
    dataFinish := dataFinish
  }

  when(arFireDelay) {
    rdataReg := raddrReg.mux(
      U"4'h4"  -> timeCountHighSnap,  // 高位
      U"4'h0"  -> timeCountLow,       // 低位
      default -> U(0)
    )
  }

  io.clintAxi4.ar.ready := !readActive                  // 传输中不应答新请求

  io.clintAxi4.r.valid  := dataFinish && readActive
  io.clintAxi4.r.last   := True
  io.clintAxi4.r.data   := rdataReg.asBits
  io.clintAxi4.r.id     := rid
  io.clintAxi4.r.resp   := Axi4.resp.OKAY
  // ================================ 写通道 ================================ //
  io.clintAxi4.b.valid.setAsReg() init(False)

  val wAllValid = io.clintAxi4.aw.valid && io.clintAxi4.w.valid
  io.clintAxi4.aw.ready := wAllValid
  io.clintAxi4.w.ready  := wAllValid

  val writeLow  = io.clintAxi4.aw.addr(3 downto 0) === U"4'h0"
  val writeHigh = io.clintAxi4.aw.addr(3 downto 0) === U"4'h4"

  val wid = RegNextWhen(io.clintAxi4.aw.id, io.clintAxi4.aw.fire)
  val wTempL    = RegNextWhen(io.clintAxi4.w.data.asUInt, io.clintAxi4.w.fire && writeLow) // 暂存低位数据，等到写高位时一并写入
  val wStrbFull   = io.clintAxi4.w.strb === B"1111"
  val wStrbFullReg = RegNextWhen(wStrbFull, io.clintAxi4.w.fire)

  when(io.clintAxi4.w.fire) {
    io.clintAxi4.b.valid := True
  } elsewhen (io.clintAxi4.b.fire) {
    io.clintAxi4.b.valid := False
  } otherwise {
    io.clintAxi4.b.valid := io.clintAxi4.b.valid
  }
  io.clintAxi4.b.id   := wid
  io.clintAxi4.b.resp := Mux(!wStrbFullReg, Axi4.resp.SLVERR, Axi4.resp.OKAY)
    

  when(io.clintAxi4.w.fire && wStrbFull && writeHigh) {// 写高位时一起更新
    timeCountLow := wTempL
  } otherwise {
    timeCountLow := timeCountLow + 1
  }

  when(io.clintAxi4.w.fire && wStrbFull && writeHigh) {
    timeCountHigh := io.clintAxi4.w.data.asUInt
  } elsewhen(timeCountLow === U"32'hffffffff") {
    timeCountHigh := timeCountHigh + 1
  }
}
