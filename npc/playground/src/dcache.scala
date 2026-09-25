package playground

import spinal.core._
import spinal.lib._       // 使用spinal的模块库

/* ****************************************************************
   D-Cache: 按字有效(sectored) + 直写(write-through) + 写不分配 + 直接映射

   与之前那版(整行 8B + 2 拍突发填充)的关键区别:
   - 【每个 32 位字一个 valid 位】-> 缺失时只取"需要的那个字", 正好是 1 拍事务,
     直接复用 LSU 现有的单拍读通路(axi4Ctrler), 不需要第二个控制器、不需要 AXI mux
     => 总线事务数与每次访问的延迟【与基线完全相同】, 不会像预取/整行填充那样把
        多出来的流量变成争用代价(那是前两次翻车的原因)
   - store 仍走 LSU 原有的 AXI 写通路(直写); 命中则顺手更新该字 => 无 dirty 位、
     无淘汰写回、无写回缓冲, 因此不需要任何 writeback 通路
   - 设备地址由外部把 cacheable 拉低, dcache 完全不介入
   - LSU 一笔访存做完才退休 => 不需要 store->load 前递 / MSHR

   参数: 默认 2 行 × 8B = 4 个 32 位字槽
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
  require(isPow2(lineBytes), "lineBytes 必须是 2 的幂")
  require(isPow2(lines), "lines 必须是 2 的幂")
  require(lineBytes >= 4, "lineBytes 至少 4")
}

case class ysyx_23060082_Dcache(param: DcacheParams = DcacheParams()) extends Component {
  val slots = param.lines * param.words                    // 总字槽数

  val io = new Bundle {
    // ---- 来自 LSU 的访存请求(组合给出) ----
    val reqAddr   = in  UInt(32 bits)
    val reqWdata  = in  UInt(32 bits)
    val reqWmask  = in  UInt(4 bits)          // 字节使能(sw=1111, sh=0011/1100, sb=单个)
    val cacheable = in  Bool()                // 该地址是否可缓存(设备访问为 0)
    // ---- 命中应答(组合, 请求拍即有效) ----
    val hit       = out Bool()
    val hitData   = out UInt(32 bits)
    // ---- 填充: 需求读完成那一拍, 把数据写进本字(只置这一个字的 valid) ----
    val fillNow   = in  Bool()
    val fillData  = in  UInt(32 bits)
    // ---- 直写: 命中的 store 更新该字 ----
    val storeNow  = in  Bool()
  }

  // ================================ 存储阵列 ================================ //
  val dataMem  = Vec(Reg(UInt(32 bits)) init(0), slots)                 // 每个字槽一份数据
  val tagMem   = Vec(Reg(UInt(param.tagBits bits)) init(0), param.lines) // tag 按行(行内两个字共享)
  val validReg = Vec(RegInit(False), slots)                             // ★ 每个字一个 valid

  val index  = io.reqAddr(param.indexBits + param.lineBits - 1 downto param.lineBits)
  val offset = io.reqAddr(param.lineBits - 1 downto 0)
  val tag    = io.reqAddr(31 downto param.indexBits + param.lineBits)
  val lineU  = if (param.indexBits > 0) index else U(0, 1 bits)
  val wordU  = if (param.words > 1) offset(param.lineBits - 1 downto 2) else U(0, 1 bits)
  val slot   = (lineU ## wordU).asUInt                      // 全局字槽号 = 行号*words + 行内字号

  val wordHit = validReg(slot) && (tagMem(lineU) === tag)
  io.hit     := wordHit && io.cacheable
  io.hitData := dataMem(slot)

  // ================================ 写口(只有一个) ================================ //
  // 优先级: 填充(需求读完成) / 直写命中更新。两者互斥(一个是 load 完成, 一个是 store)
  val storeData = UInt(32 bits)
  storeData := dataMem(slot)                                // 读-改-写: 默认保持
  for (b <- 0 until 4) {                                    // 按字节使能改
    when(io.reqWmask(b)) {
      storeData(b * 8 + 7 downto b * 8) := io.reqWdata(b * 8 + 7 downto b * 8)
    }
  }

  when(io.fillNow) {                                        // 填充: 写数据 + tag + 该字的 valid
    dataMem(slot)  := io.fillData
    tagMem(lineU)  := tag
    validReg(slot) := True
  } elsewhen(io.storeNow) {                                 // 直写命中: 只更新该字(不置 valid)
    dataMem(slot)  := storeData
  }
}
