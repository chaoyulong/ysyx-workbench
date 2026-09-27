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

  // val timeCount = RegInit(U"64'h0")   // 系统计时器
  // timeCount := timeCount + 1
  val timeCountLow  = RegInit(U"32'h0")
  val timeCountHigh = RegInit(U"32'h0")

  timeCountLow := timeCountLow + 1
  when(timeCountLow === U"32'hffffffff") {
    timeCountHigh := timeCountHigh + 1
  }

  io.clintAxi4.b.valid.setAsReg() init(False)

  // ---------- 读通道 (支持突发: 按 len 计数, last 在最后一拍) ---------- //
  val readLen    = RegNextWhen(io.clintAxi4.ar.len, io.clintAxi4.ar.fire)// 突发长度 (len),读地址握手成功后更新
  val readCnt    = Reg(UInt(8 bits))            // 已返回数据节拍数
  val readActive = RegInit(False)               // 读传输进行中

  when(io.clintAxi4.ar.fire) {                  // 读地址握手
    readCnt := 0
  } elsewhen (io.clintAxi4.r.fire && readCnt =/= readLen) { // 读数据握手，当突发传输时，会连续握手几次
    readCnt := readCnt + 1                                  // 没到最后一拍, 每次握手计数+1
  } otherwise {
    readCnt := readCnt
  }

  when(io.clintAxi4.ar.fire) {                            // 读地址握手
    readActive := True
  } elsewhen(io.clintAxi4.r.fire && readCnt === readLen) {// 最后一拍, 传输结束
    readActive := False
  } otherwise {
    readActive := readActive
  }

  val addrReg     = RegNextWhen(io.clintAxi4.ar.addr, io.clintAxi4.ar.fire)     // 地址暂存一下，打断从icache到clint这条不会存在的关键路径
  val dataReg     = Reg(UInt(32 bits))
  val arFireDelay = RegNext(io.clintAxi4.ar.fire) // 握手后的下一周期
  val dataFinish  = RegInit(False)
  // 读取协议: 先读低位(mtime), 硬件锁存当时的高位; 再读高位(mtimeh)返回锁存值
  // 与 mcycle 的"先读低再读高"协议保持一致
  val readLow  = addrReg === MTIME
  val timeCountHighSnap = RegNextWhen(timeCountHigh, arFireDelay && readLow)    // 读低那一拍锁存高位

  when(arFireDelay) {
    dataFinish := True
  } elsewhen(io.clintAxi4.r.fire && readCnt === readLen) {// 最后一拍, 传输结束
    dataFinish := False
  } otherwise {
    dataFinish := dataFinish
  }

  when(arFireDelay) {
    dataReg := addrReg.mux(
      MTIMEH  -> timeCountHighSnap,
      MTIME   -> timeCountLow,
      default -> U(0)
    )
  }

  io.clintAxi4.ar.ready := !readActive                  // 传输中不应答新请求

  io.clintAxi4.r.valid  := dataFinish && readActive
  io.clintAxi4.r.last   := readActive && (readCnt === readLen)  // 最后一拍
  io.clintAxi4.r.data   := dataReg.asBits
  io.clintAxi4.r.id     := U(0)
  io.clintAxi4.r.resp   := Axi4.resp.OKAY
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
  io.clintAxi4.b.id   := U(0)
  io.clintAxi4.b.resp := Axi4.resp.OKAY
}
