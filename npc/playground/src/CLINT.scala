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

  val timeCount = RegInit(U"64'h0")   // 系统计时器
  timeCount := timeCount + 1

  // 双向快照: 读任一侧(低位/高位)都同时锁存高低位, 任意读取顺序下高低位保持一致
  val readLow  = io.clintAxi4.ar.fire && (io.clintAxi4.ar.addr === MTIME)
  val readHigh = io.clintAxi4.ar.fire && (io.clintAxi4.ar.addr === MTIMEH)
  val timeCountLow  = RegNextWhen(timeCount(31 downto 0),  readLow || readHigh) init(0)
  val timeCountHigh = RegNextWhen(timeCount(63 downto 32), readLow || readHigh) init(0)

  io.clintAxi4.b.valid.setAsReg() init(False)

  // ---------- 读通道 (支持突发: 按 len 计数, last 在最后一拍) ---------- //
  val readLen    = RegNextWhen(io.clintAxi4.ar.len, io.clintAxi4.ar.fire) init(0)    // 突发长度 (len),读地址握手成功后更新
  val readCnt    = Reg(UInt(8 bits)) init(0)    // 已返回数据节拍数
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

  // 突发期间数据按 FIXED 语义保持: arFire 时锁存时间值, 传输期间不变
  val readData = Reg(UInt(32 bits))
  when(io.clintAxi4.ar.fire) {
    readData := io.clintAxi4.ar.addr.mux(
      MTIMEH -> timeCountHigh,
      MTIME  -> timeCountLow,
      default -> U(0)
    )
  }

  io.clintAxi4.ar.ready := !readActive                  // 传输中不应答新请求
  io.clintAxi4.r.valid  := readActive
  io.clintAxi4.r.last   := readActive && (readCnt === readLen)  // 最后一拍
  io.clintAxi4.r.data   := readData.asBits
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
