package playground

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi._

object Axi4Define {
  object burst {
    def apply() = Bits(2 bits)
    def FIXED    = B"00"
    def INCR     = B"01"     
    def WRAP     = B"10"
    def RESERVED = B"11"
  }
  object size {
    def apply() = UInt(3 bits)
    def BYTE_1   = U"3'b000"   
    def BYTE_2   = U"3'b001"
    def BYTE_4   = U"3'b010"   
    def BYTE_8   = U"3'b011"   
    def BYTE_16  = U"3'b100"
    def BYTE_32  = U"3'b101"   
    def BYTE_64  = U"3'b110"   
    def BYTE_128 = U"3'b111"
  }
  object resp { 
    def OKAY     = B"00"
    def EXOKAY   = B"01"
    def SLVERR   = B"10"
    def DECERR   = B"11"
  }
}

// ================================ 简易指令缓存 (icache) ================================ //
// 组相联(ways: 1=直接映射 / 2=2路组相联), 寄存器实现, 参数化(块大小/总块数/相联度)
// 接口:
//   reqIn  : Stream 取指请求(pc) —— IFU 发请求, icache 接受(fire)后处理
//   rspOut : Flow   指令返回(rdata) —— 命中一拍返回; 缺失访存完成后返回
//   axi4   : Axi4ReadOnly —— 缺失时经只读控制器访问内存(4 拍突发读回一整行)
// 命中: 请求拍组合判断(任一 way 的 valid && tag 匹配), 同拍返回 rdata
// 缺失: 进入 Miss 状态, 读回后写入"受害路"(2 路时由每 set 一位的 LRU 选), 完成后返回
// 替换: 2 路用 LRU —— cachesim 实测【必需】: 同一 2 路结构下
//       LRU 缺失 -7.0% / rand -1.7% / FIFO 反而 +3.4%(比直接映射还差)
case class IcacheParams(
  lineBytes: Int = 16,      // 块大小(字节)
  lines:     Int = 8,       // 总块数(容量 = lines × lineBytes)
  ways:      Int = 2        // 相联度: 1=直接映射, 2=2路组相联
) {
  require(lines % ways == 0, s"lines($lines) 必须能被 ways($ways) 整除")
  require(ways == 1 || ways == 2, "icache 只实现 ways ∈ {1,2}(LRU 按 2 路设计)")
  // ---- 派生常量  ----
  val sets      = lines / ways                   // 组数
  val lineBits  = log2Up(lineBytes)              // 块内偏移位宽
  val indexBits = log2Up(sets)                   // 索引位宽(按【组数】算)
  val tagBits   = 32 - lineBits - indexBits      // tag 位宽
  val words     = lineBytes / 4                  // 每行字数 = 突发拍数
  val wordBits  = log2Up(words)                  // 字索引位宽
  val dataBits  = lineBytes * 8                  // 每行数据位宽
  val wayBits   = scala.math.max(log2Up(ways), 1)  // way 号位宽(log2Up(1)=0, 兜到 1)
}

// valid ready pc 三个信号
case class IcacheReqData() extends Bundle {
  val pc = UInt(32 bits)
}

// valid rdata 两个信号
case class IcacheRspData() extends Bundle {
  val rdata = UInt(32 bits)
}

case class ysyx_23060082_Icache(config: CpuConfig = CpuConfig(), param: IcacheParams = IcacheParams()) extends Component {
  val io = new Bundle {
    val reqIn    = slave  Stream(IcacheReqData())
    val rspOut   = master Flow(IcacheRspData())
    val axi4     = master(Axi4ReadOnly(AxiConfig.axiConfig))
    val fenceI   = in Bool()    // fence.i: 清空有效位(后续取指缺失重读)
    val miss     = if (config.enableSimDebug) out Bool() else null   // 缺失拍脉冲(每次缺失一次), 供 IFU 的性能计数器统计命中率; STA 时无人使用会被剪掉
    val missDone = if (config.enableSimDebug) out Bool() else null   // 缺失完成拍脉冲, 与 miss 配对可测出平均缺失代价(即 TMT 里的那一项)
    val rspErr   = out Bool()   // 总线读取有错误
  }

  // ================================ ifu的axi交给icache控制 ================================ //
  val axi4Ctrler = ysyx_23060082_Axi4_Ctrler_ReadOnly_Burst(param)
  io.axi4 <> axi4Ctrler.io.axi4
  // ================================ 参数与地址划分 ================================ //
  // 2 路时 index/tag 按【组数】划分(容量仍是 lines × lineBytes); "行地址"仍是 pc(31 downto lineBits)
  val tag    = io.reqIn.pc(31 downto param.indexBits + param.lineBits)
  val index  = io.reqIn.pc(param.indexBits + param.lineBits - 1 downto param.lineBits)
  // ================================ 存储阵列 (寄存器, 按 way 分开以便并行比较 tag) ================================ //
  val dataMem  = Vec(Vec(Reg(UInt(param.dataBits bits)), param.sets), param.ways)  // data[way][set]
  val tagMem   = Vec(Vec(Reg(UInt(param.tagBits  bits)), param.sets), param.ways)  // tag [way][set]
  val validReg = Vec(Reg(Bits(param.sets bits)) init(0), param.ways)               // 每 way 每 set 一位有效位
  // LRU: 每个 set 一位, 记录"下次该替换哪一路"(只有 ways>1 需要)
  val lruReg   = if (param.ways > 1) Reg(Bits(param.sets bits)) init(0) else null

  // 命中判断: 任一 way 的 (valid && tag 相等); ways>1 时各路并行比较
  val wayHit = Vec(Bool(), param.ways)
  for (w <- 0 until param.ways) {
    wayHit(w) := validReg(w)(index) && (tagMem(w)(index) === tag)
  }
  val hit = wayHit.reduce(_ || _)

  val wordSel = if (param.words > 1) io.reqIn.pc(param.lineBits - 1 downto 2) else U(0, 1 bits)
  val wordSelReg = RegNextWhen(wordSel, io.reqIn.fire)            // 本次请求的是块内第几个字
  // ================================ 握手成功锁存数据 ================================ //
  val indexReg = RegNextWhen(index      , io.reqIn.fire)
  val tagReg   = RegNextWhen(tag        , io.reqIn.fire)
  val reqFire  = io.reqIn.fire
  // ================================ 状态机 ================================ //
  object IcacheState extends SpinalEnum {
    val Idle, Miss = newElement()
  }
  val state = Reg(IcacheState()) init(IcacheState.Idle)

  when(state === IcacheState.Idle) {
    when(io.reqIn.valid && !hit) { state := IcacheState.Miss }  // 有请求但是没有命中
    .otherwise { state := IcacheState.Idle }
  } elsewhen(state === IcacheState.Miss) {
    when(axi4Ctrler.io.readEnd) { state := IcacheState.Idle }   // 访存完成, 返回
    .otherwise { state := IcacheState.Miss }
  } 
  // ================================  ================================ //
  // 请求握手: Idle时接受(命中同拍组合返回rspOut, 缺失进入Miss)
  io.reqIn.ready := (state === IcacheState.Idle)

  // 命中 或 缺失但是读取完成
  val missDone = (state === IcacheState.Miss) && axi4Ctrler.io.readEnd
  io.rspOut.valid := (io.reqIn.fire && hit) || missDone

  // 命中数据: 每路先按 wordSel 选出本路的字(32 位), 再按命中的 way 选一路 —— 不用 128 位 mux
  val hitWord = Vec(UInt(32 bits), param.ways)
  for (w <- 0 until param.ways) {
    val wayWordVec = Vec(UInt(32 bits), param.words)
    for (i <- 0 until param.words) {
      wayWordVec(i) := dataMem(w)(index)(i*32 + 31 downto i*32)
    }
    hitWord(w) := wayWordVec(wordSel)
  }
  val hitRdata = if (param.ways > 1) Mux(wayHit(1), hitWord(1), hitWord(0)) else hitWord(0)

  val missWordVec = Vec(UInt(32 bits), param.words)
  for (i <- 0 until param.words) {
    missWordVec(i) := axi4Ctrler.io.readData(i*32 + 31 downto i*32)
  }

  io.rspOut.rdata := Mux(io.reqIn.fire && hit,
                        hitRdata,                  // 命中: 命中路 + 当前pc的字
                        missWordVec(wordSelReg))   // 缺失: 请求时锁存的字

  // readReq要求只持续一个周期
  val enterMiss = (state === IcacheState.Idle) && io.reqIn.valid && !hit
  axi4Ctrler.io.readReq  := enterMiss
  axi4Ctrler.io.readAddr := (io.reqIn.pc(31 downto param.lineBits) ## U(0, param.lineBits bits)).asUInt // 整行对齐地址

  // fence.i: 清空全部有效位(后续取指缺失重读新指令)
  val discardMiss = RegInit(False)        // fence到达时正在读取的miss
  when(io.fenceI) {
    discardMiss := (state === IcacheState.Miss)
  }elsewhen(missDone) {                   // 只是为了卡住访存完成后的那一个周期，所以之后就可以清空
    discardMiss := False
  }

  val rspErr = axi4Ctrler.io.axi4.r.fire && (axi4Ctrler.io.axi4.r.payload.resp =/= B"2'b00")
  io.rspErr := rspErr

  // ================================ 受害路 & 填充 ================================ //
  // ways=1: 恒为 way0(等价直接映射); ways>1: 由该 set 的 LRU 位选(0=替换way0, 1=替换way1)
  val victimWay = if (param.ways > 1) lruReg(indexReg).asUInt else U(0, 1 bits)

  // 缺失完成: 整行写进受害路的 data/tag(valid 单独处理, 被丢弃的 miss 不置 valid)
  when(missDone) {
    for (w <- 0 until param.ways) {
      when(victimWay === U(w, param.wayBits bits)) {
        dataMem(w)(indexReg) := axi4Ctrler.io.readData
        tagMem (w)(indexReg) := tagReg
      }
    }
  }

  // LRU 更新: 被访问的那一路成为 MRU, 另一路成为下次的受害路
  if (param.ways > 1) {
    when(io.reqIn.fire && hit) {                    // 命中(含同拍命中): 另一路变 LRU
      lruReg(index) := ~wayHit(1)
    } elsewhen(missDone && !discardMiss) {          // 填充完成: 刚填入的那一路不是 LRU
      lruReg(indexReg) := ~victimWay(0)
    }
  }

  when(io.fenceI) {                                 // fence.i: 全部失效 + LRU 复位(等价冷 cache)
    for (w <- 0 until param.ways) validReg(w) := 0
    if (param.ways > 1) lruReg := 0
  } elsewhen(missDone && !discardMiss && !rspErr) { // 如果是卡住的话，vaild不会置起，所以会开始下一次访存
    for (w <- 0 until param.ways) {
      when(victimWay === U(w, param.wayBits bits)) { validReg(w)(indexReg) := True }
    }
  }

  if (config.enableSimDebug) {
    // 缺失脉冲: 每一次缺失拉高一拍(交给 IFU 的 PerfReg 计数, 用于统计命中率)
    // 等价于 io.reqIn.fire && !hit —— 因为 reqIn.ready 只在 Idle 时拉高
    io.miss := enterMiss
    io.missDone := missDone
  }
  
}

/* ****************************************************************
  只有读通道的axi总线控制器
**************************************************************** */
case class ysyx_23060082_Axi4_Ctrler_ReadOnly_Burst(param: IcacheParams = IcacheParams()) extends Component {
  val io = new Bundle {
    val readReq  = in  Bool()
    val readAddr = in  UInt(32 bits)
    val readEnd  = out Bool()
    val readData = out UInt(param.dataBits bits)
    val axi4 = master(Axi4ReadOnly(AxiConfig.axiConfig))
  }

  // 正在拼装的行(逐字填入)，最后一位由于之前代码的规则，直接靠拼接
  val lineReg = if (param.words > 1) Reg(Bits(param.dataBits - 32 bits)) else Reg(Bits(32 bits))
  val wordCnt = if (param.words > 1) Reg(UInt(param.wordBits bits)) else Reg(UInt(1 bits))    // 读取到了第几个数，按照约束最多只会一次8个
  val readOnce = io.axi4.r.fire                   // 每次读回一个数据完成的信号，用于icache数据的存储

  io.axi4.ar.valid.setAsReg() init(False)
  io.axi4.ar.addr .setAsReg()                     // 地址要锁存
  io.axi4.ar.id   := U"4'b0"                      // 加不加突发，这些数值都会是常量，不需要寄存器锁存
  io.axi4.ar.len  := U(param.words - 1, 8 bits)   // 突发长度实际要-1,1+1=2
  io.axi4.ar.size := Axi4Define.size.BYTE_4       // 突发大小4字节，固定是4字节，意义是每次读取多少
  io.axi4.ar.burst:= Axi4Define.burst.INCR        // 突发类型INCR

  when(io.readReq) {
    io.axi4.ar.valid := True
  } elsewhen(io.axi4.ar.fire) {
    io.axi4.ar.valid := False
  } otherwise {
    io.axi4.ar.valid := io.axi4.ar.valid
  }

  when(io.readReq) {
    io.axi4.ar.addr := io.readAddr
  } otherwise {
    io.axi4.ar.addr := io.axi4.ar.addr 
  }
  
  io.axi4.r.ready := io.axi4.r.valid
  
  io.readEnd  := io.axi4.r.fire && io.axi4.r.last     // 突发结束(r.last)才算读完
  io.readData := (if (param.words > 1) io.axi4.r.data ## lineReg else io.axi4.r.data).asUInt  // 拼接成一行数据                              

  when(readOnce) {
    val chain = when(wordCnt === 0) { lineReg(31 downto 0)  := io.axi4.r.data }
    for (i <- 1 until param.words - 1) {          // i = 1, .. ,  words-2
      chain.elsewhen(wordCnt === i) { lineReg(i*32 + 31 downto i*32) := io.axi4.r.data }
    }
    // 注意: 这里【不能】写 chain.otherwise { lineReg := lineReg }
    //   lineReg 是 Reg, 分支不赋值即保持; 而整体赋值会和上面的分片赋值冲突,
    //   触发 SpinalHDL 的 ASSIGNMENT OVERLAP (words=2 时循环为空才侥幸没报)
  }

  when(io.readReq) {      // 请求开始时清零，所以不需要reset信号
    wordCnt := 0
  } elsewhen(readOnce) {
    wordCnt := wordCnt + 1
  } otherwise {
    wordCnt := wordCnt
  }

}
