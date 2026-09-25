package playground

import spinal.core._
import spinal.lib.bus.amba4.axi._
import spinal.lib._       // 使用spinal的模块库

/* ****************************************************************
   D-Cache: 只读 + 直接映射 + 整行突发填充(结构照抄 icache)

   - 【只缓存 SDRAM】(0xa000_0000~0xbfff_ffff): trace 实测 flash 阶段数据命中率 0.00%
     (纯流式零复用), 缓存它只是白花总线时间; 设备访问完全不介入
   - 结构照抄 icache: 同一套 params / dataMem·tagMem·validReg / Idle-Fill 状态机,
     并直接复用 ysyx_23060082_Axi4_Ctrler_ReadOnly_Burst 做填充
   - 【只读】: 不实现写数据通路。但 store 命中时必须【作废该行】(1 位清零), 否则 cache
     里的副本会变旧, 之后的 load 读到旧值 —— 这是"只读 cache"唯一的正确性要点
   - 缺失整行填充(2 行 x 8B => 2 拍突发), 命中当拍返回
**************************************************************** */
case class DcacheParams(
  lineBytes: Int = 8,      // 块大小(字节)
  lines    : Int = 2       // 块数(直接映射组数)
) {
  val lineBits  = log2Up(lineBytes)
  val indexBits = log2Up(lines)
  val tagBits   = 32 - lineBits - indexBits
  val words     = lineBytes / 4
  val wordBits  = log2Up(words)
  val dataBits  = lineBytes * 8
  require(isPow2(lineBytes) && isPow2(lines) && lineBytes >= 4, "lineBytes/lines 必须是 2 的幂且 lineBytes>=4")
}

case class ysyx_23060082_Dcache(param: DcacheParams = DcacheParams()) extends Component {
  val io = new Bundle {
    val reqValid  = in  Bool()                // 本拍确实是一个"要读内存"的 load
    val reqAddr   = in  UInt(32 bits)
    val cacheable = in  Bool()                // 该地址是否可缓存(仅 SDRAM 为 1)
    val rspValid  = out Bool()                // 命中(请求拍) 或 填充完成(拍)
    val rspData   = out UInt(32 bits)
    val invalid   = in  Bool()                // store 命中: 作废该行(只清 valid, 不写数据)
    val busy      = out Bool()                // 正在填充 -> LSU 用它来切换 AXI 读通道
    val axi4      = master(Axi4ReadOnly(AxiConfig.axiConfig))
  }

  // ================================ 存储阵列(写法与 icache 一致) ================================ //
  val dataMem  = Reg(Vec(UInt(param.dataBits bits), param.lines))   // 数据
  val tagMem   = Reg(Vec(UInt(param.tagBits bits), param.lines))    // 标签
  val validReg = Reg(Bits(param.lines bits)) init(0)                // 每行 1 位有效位

  val index   = io.reqAddr(param.indexBits + param.lineBits - 1 downto param.lineBits)
  val tag     = io.reqAddr(31 downto param.indexBits + param.lineBits)
  val wordSel = if (param.words > 1) io.reqAddr(param.lineBits - 1 downto 2) else U(0, 1 bits)

  val lineHit = validReg(index) && (tagMem(index) === tag)
  val hit     = lineHit && io.cacheable

  // ================================ 状态机 ================================ //
  object DcState extends SpinalEnum {
    val Idle, Fill = newElement()
  }
  val state = Reg(DcState()) init(DcState.Idle)

  // 填充用与 icache 完全相同的突发读控制器
  val fill = ysyx_23060082_Axi4_Ctrler_ReadOnly_Burst(
               IcacheParams(lineBytes = param.lineBytes, lines = param.lines))
  io.axi4 <> fill.io.axi4

  val startFill = (state === DcState.Idle) && io.reqValid && io.cacheable && !lineHit
  fill.io.readReq  := startFill                                          // 只持续一个周期
  fill.io.readAddr := (io.reqAddr(31 downto param.lineBits) ## U(0, param.lineBits bits)).asUInt  // 地址对齐

  val indexReg = RegNextWhen(index  , startFill) init(0)
  val tagReg   = RegNextWhen(tag    , startFill) init(0)
  val wordReg  = RegNextWhen(wordSel, startFill) init(0)
  val fillDone = (state === DcState.Fill) && fill.io.readEnd

  when(state === DcState.Idle) {
    when(startFill) { state := DcState.Fill }
  } elsewhen(state === DcState.Fill) {
    when(fill.io.readEnd) { state := DcState.Idle }
  }
  io.busy := (state === DcState.Fill)

  // ================================ 写回 / 作废 ================================ //
  when(fillDone) {                                    // 填充完成: 写回整行
    dataMem(indexReg)  := fill.io.readData
    tagMem(indexReg)   := tagReg
    validReg(indexReg) := True
  } elsewhen(io.invalid) {                            // store 命中: 只作废这一行(不写数据)
    validReg := validReg & ~(B(1, param.lines bits) << index).resize(param.lines bits)
  }

  // ================================ 响应 ================================ //
  val hitWordVec  = Vec(UInt(32 bits), param.words)
  val fillWordVec = Vec(UInt(32 bits), param.words)
  for (i <- 0 until param.words) {
    hitWordVec (i) := dataMem(index)(i * 32 + 31 downto i * 32)
    fillWordVec(i) := fill.io.readData(i * 32 + 31 downto i * 32)
  }

  io.rspValid := (hit && (state === DcState.Idle)) || fillDone
  io.rspData  := Mux(hit && (state === DcState.Idle),
                     hitWordVec (wordSel),     // 命中: 当前地址那个字
                     fillWordVec(wordReg))     // 填充完成: 请求时锁存的字
}
