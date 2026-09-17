package playground

import spinal.core._
import spinal.lib._       // 使用spinal的模块库
import spinal.lib.bus.amba4.axi._

/* ****************************************************************
   D-Cache: 直写(write-through) + 写不分配(no-write-allocate) + 直接映射

   设计要点(与 LSU 的"顺序执行 + 单笔未完成"配合):
   - 只缓存 load: 命中当拍就出数据(和 icache 的命中同拍交付一致); 缺失时发起一次
     2 拍突发填充(复用 icache 的 Axi4_Ctrler_ReadOnly_Burst)
   - store 仍走 LSU 原有的 AXI 写通路(直写); 若命中则顺手更新本行
     => 没有 dirty 位、没有淘汰写回、没有写回缓冲, 因此不需要任何 writeback 通路
   - 设备地址(CLINT/UART/VGA 等)由外部把 cacheable 拉低, dcache 完全不介入
   - LSU 一笔访存做完才退休 => 不需要 store->load 前递 / MSHR / 多笔未完成

   参数: 默认 2 行 × 8B = 16 字节
**************************************************************** */
case class DcacheParams(
  lineBytes: Int = 8,      // 每行字节数
  lines    : Int = 2       // 行数
) {
  val lineBits   = log2Up(lineBytes)          // 行内偏移位数
  val indexBits  = log2Up(lines)              // 索引位数
  val tagBits    = 32 - indexBits - lineBits  // tag 位数
  val words      = lineBytes / 4              // 一行几个 32 位字
  val wordBits   = log2Up(words)              // 字选择位数
  val dataBits   = lineBytes * 8              // 一行数据位宽
  require(lineBytes >= 4, "lineBytes 至少 4")
  require(isPow2(lineBytes), "lineBytes 必须是 2 的幂")
  require(isPow2(lines), "lines 必须是 2 的幂")
}

case class ysyx_23060082_Dcache(param: DcacheParams = DcacheParams()) extends Component {
  val io = new Bundle {
    // ---- 来自 LSU 的访存请求(组合给出) ----
    val reqAddr   = in  UInt(32 bits)
    val reqWdata  = in  UInt(32 bits)
    val reqWmask  = in  UInt(4 bits)          // 字节使能(sw=1111, sh=0011/1100, sb=单个)
    val cacheable = in  Bool()                // 该地址是否可缓存(设备访问为 0)
    val reqValid  = in  Bool()                // 本拍确实是一个"要读内存"的 load(非访存指令的地址是垃圾)
    val storeNow  = in  Bool()                // 本拍是一个"可缓存且命中"的 store
    // ---- 命中应答(组合, 请求拍即有效) ----
    val hit       = out Bool()
    val hitData   = out UInt(32 bits)
    // ---- 缺失填充 ----
    val fillBusy  = out Bool()                // 填充进行中(此时 AXI 主口归 dcache)
    val fillEnd   = out Bool()                // 填充完成那一拍
    val fillData  = out UInt(param.dataBits bits)  // 填好的整行
    // ---- AXI(只读, 用于填充) ----
    val axi4      = master(Axi4ReadOnly(AxiConfig.axiConfig))
  }

  // ================================ 存储阵列 ================================ //
  val dataMem  = Vec(Reg(UInt(param.dataBits bits)) init(0), param.lines)
  val tagMem   = Vec(Reg(UInt(param.tagBits  bits)) init(0), param.lines)
  val validReg = Vec(RegInit(False), param.lines)

  val index  = io.reqAddr(param.indexBits + param.lineBits - 1 downto param.lineBits)
  val offset = io.reqAddr(param.lineBits - 1 downto 0)
  val tag    = io.reqAddr(31 downto param.indexBits + param.lineBits)

  val idxU    = if (param.indexBits > 0) index else U(0, 1 bits)
  val lineHit = validReg(idxU) && (tagMem(idxU) === tag)
  io.hit := lineHit && io.cacheable

  // 命中数据: 用地址的"字选择"位从行里取 32 位
  val wordSel = if (param.words > 1) offset(param.lineBits - 1 downto 2) else U(0, 1 bits)
  val words   = Vec((0 until param.words).map(i => dataMem(idxU)(i * 32 + 31 downto i * 32)))
  io.hitData := words(wordSel)

  // ================================ 缺失填充 ================================ //
  // 复用 icache 的"只读突发"控制器: 8B 一行 = 2 拍突发
  val fill = ysyx_23060082_Axi4_Ctrler_ReadOnly_Burst(
               IcacheParams(lineBytes = param.lineBytes, lines = param.lines))
  io.axi4 <> fill.io.axi4

  object DcState extends SpinalEnum { val Idle, Fill = newElement() }
  val state = Reg(DcState()) init(DcState.Idle)

  val startFill = (state === DcState.Idle) && io.reqValid && io.cacheable && !lineHit
  when(state === DcState.Idle) {
    when(startFill) { state := DcState.Fill }
    .otherwise      { state := DcState.Idle }
  } elsewhen(state === DcState.Fill) {
    when(fill.io.readEnd) { state := DcState.Idle }
    .otherwise            { state := DcState.Fill }
  }

  fill.io.readReq  := startFill
  fill.io.readAddr := (io.reqAddr(31 downto param.lineBits) ## U(0, param.lineBits bits)).asUInt  // 行对齐

  io.fillBusy := (state === DcState.Fill)
  io.fillEnd  := (state === DcState.Fill) && fill.io.readEnd
  io.fillData := fill.io.readData

  // 填充完成: 写回该行(用发起填充时锁存的 index/tag, 因为请求地址这时可能已经变了)
  val indexReg = RegNextWhen(idxU, startFill) init(0)
  val tagReg   = RegNextWhen(tag,  startFill) init(0)
  when(io.fillEnd) {
    dataMem(indexReg)  := fill.io.readData
    tagMem(indexReg)   := tagReg
    validReg(indexReg) := True
  }

  // ================================ 直写命中时更新本行 ================================ //
  // 只写内存而不更新行 => 下一次 load 会命中旧数据, 所以必须同步更新
  val lineWr = UInt(param.dataBits bits)
  lineWr := dataMem(idxU)
  for (w <- 0 until param.words) {                     // 先按字选择
    when(wordSel === U(w, param.wordBits bits)) {
      for (b <- 0 until 4) {                           // 再按字节使能
        when(io.reqWmask(b)) {
          lineWr(w * 32 + b * 8 + 7 downto w * 32 + b * 8) := io.reqWdata(b * 8 + 7 downto b * 8)
        }
      }
    }
  }
  when(io.storeNow) { dataMem(idxU) := lineWr }
}
