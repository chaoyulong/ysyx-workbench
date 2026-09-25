package playground

import spinal.core._
import spinal.lib._

/* ****************************************************************
   D-Cache: 【按字有效(sectored)】 + 只读 + 直接映射

   - 一个 tag 管 8B(2 个字), 但【每个 32 位字一个 valid 位】
   - 缺失时【只取需要的那个字】(1 拍单字读), 直接复用 LSU 现有的单拍读通路
     => 缺失的事务数/延迟【与基线完全相同】, 不增加任何总线流量
        (整行填充版每次要多花 ~34 拍, 把命中收益全吃掉, 净 +0.52%)
   - 命中当拍组合返回
   - 只读: 不实现写数据通路; store 命中时【作废该字】(清 1 位), 保证不会读到旧值
   - ★ tag 是按行共享的, 所以【填充换了 tag 时必须把本行其它字的 valid 一起清掉】,
     否则那些字会带着新 tag 造成【假命中】(返回旧数据)。这是第一版卡死的根因。
**************************************************************** */
case class DcacheParams(
  lineBytes: Int = 8,      // 每行字节数(决定每行几个字槽)
  lines    : Int = 2       // 行数
) {
  val lineBits  = log2Up(lineBytes)
  val indexBits = log2Up(lines)
  val tagBits   = 32 - lineBits - indexBits
  val words     = lineBytes / 4
  val wordBits  = log2Up(words)
  require(isPow2(lineBytes) && isPow2(lines) && lineBytes >= 4, "lineBytes/lines 必须是 2 的幂且 lineBytes>=4")
}

case class ysyx_23060082_Dcache(param: DcacheParams = DcacheParams()) extends Component {
  val slots = param.lines * param.words                  // 总字槽数(2 行 x 2 字 = 4)

  val io = new Bundle {
    val reqValid  = in  Bool()                // 本拍确实是一个"要读内存"的 load
    val reqAddr   = in  UInt(32 bits)
    val cacheable = in  Bool()                // 该地址是否可缓存(设备为 0)
    val hit       = out Bool()                // 组合命中(请求拍)
    val hitData   = out UInt(32 bits)
    val fillNow   = in  Bool()                // 需求读完成那一拍: 把这一个字写进 cache
    val fillData  = in  UInt(32 bits)
    val storeNow  = in  Bool()                // store 命中: 作废该字
  }

  // ================================ 存储阵列(写法与 icache 一致) ================================ //
  val dataMem  = Reg(Vec(UInt(32 bits), slots))                    // 每个字槽一份数据
  val tagMem   = Reg(Vec(UInt(param.tagBits bits), param.lines))   // tag 按行(行内各字共享)
  val validReg = Reg(Bits(slots bits)) init(0)                     // 每个字 1 位有效位

  val index  = io.reqAddr(param.indexBits + param.lineBits - 1 downto param.lineBits)
  val offset = io.reqAddr(param.lineBits - 1 downto 0)
  val tag    = io.reqAddr(31 downto param.indexBits + param.lineBits)
  val lineU  = if (param.indexBits > 0) index else U(0, 1 bits)
  val wordU  = if (param.words > 1) offset(param.lineBits - 1 downto 2) else U(0, 1 bits)
  val slot   = (lineU ## wordU).asUInt     // 全局字槽号 = 行号*words + 行内字号

  val wordHit = validReg(slot) && (tagMem(lineU) === tag)
  io.hit     := wordHit && io.cacheable
  io.hitData := dataMem(slot)

  // ================================ 填充 / 作废 ================================ //
  val sameTag  = tagMem(lineU) === tag                                                // 本行旧 tag 是否与新 tag 相同
  // 注意: validReg 是 Bits, 掩码也用 Bits(不要用 U)
  val lineMask = (B((1 << param.words) - 1, slots bits) << (lineU * param.words)).resize(slots bits)  // 本行所有字
  val setMask  = (B(1, slots bits) << slot).resize(slots bits)                        // 本字

  when(io.fillNow) {                       // 读完成: 写这一个字
    dataMem(slot) := io.fillData
    tagMem(lineU) := tag
    // tag 相同(同一行的另一个字): 保留原有 valid
    // tag 不同: 先把整行作废(否则本行其它字会带着新 tag 假命中), 再只置本字
    validReg := Mux(sameTag, validReg | setMask, (validReg & ~lineMask) | setMask)
  } elsewhen(io.storeNow) {                // store 命中: 只作废这一个字
    validReg := validReg & ~setMask
  }
}
