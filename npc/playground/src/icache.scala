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
// 直接映射(direct-mapped), 寄存器实现, 参数化(块大小/块数)
// 接口:
//   reqIn  : Stream 取指请求(pc) —— IFU 发请求, icache 接受(fire)后处理
//   rspOut : Flow   指令返回(rdata) —— 命中一拍返回; 缺失访存完成后返回
//   axi4   : Axi4ReadOnly —— 缺失时经只读控制器访问内存(单次读, len=0)
// 命中: 请求拍组合判断(tag匹配 && valid), 同拍返回 rdata
// 缺失: 进入 Miss 状态, 经 AXI 读回并写回 cache(valid/tag/data), 完成后返回
case class IcacheParams(
  lineBytes: Int = 8,       // 块大小(字节), 4B 起步(后续可加大配合突发)
  lines:     Int = 8        // 块数(直接映射组数)
) {
  // ---- 派生常量  ----
  val lineBits  = log2Up(lineBytes)              // 块内偏移位宽
  val indexBits = log2Up(lines)                  // 索引位宽
  val tagBits   = 32 - lineBits - indexBits      // tag 位宽
  val words     = lineBytes / 4                  // 每行字数 = 突发拍数
  val wordBits  = log2Up(words)                  // 字索引位宽
  val dataBits  = lineBytes * 8                  // 每行数据位宽
}

// valid ready pc 三个信号
case class IcacheReqData() extends Bundle {
  val pc = UInt(32 bits)
}

// valid rdata 两个信号
case class IcacheRspData() extends Bundle {
  val rdata = UInt(32 bits)
}

case class ysyx_23060082_Icache(param: IcacheParams = IcacheParams()) extends Component {
  val io = new Bundle {
    val reqIn  = slave  Stream(IcacheReqData())
    val rspOut = master Flow(IcacheRspData())
    val axi4   = master(Axi4ReadOnly(AxiConfig.axiConfig))
    val fenceI = in Bool()   // fence.i: 清空有效位(后续取指缺失重读)
    val miss   = out Bool()  // 缺失拍脉冲(每次缺失一次), 供 IFU 的性能计数器统计命中率; STA 时无人使用会被剪掉
    val missDone = out Bool() // 缺失完成拍脉冲, 与 miss 配对可测出平均缺失代价(即 TMT 里的那一项)
  }

  // ================================ ifu的axi交给icache控制 ================================ //
  val axi4Ctrler = ysyx_23060082_Axi4_Ctrler_ReadOnly_Burst(param)
  io.axi4 <> axi4Ctrler.io.axi4
  // ================================ 参数与地址划分 ================================ //
  val tag    = io.reqIn.pc(31 downto param.indexBits + param.lineBits)                  // 按照每块8字节，8块来计算的话，tag = io.reqIn.pc(31 downto 5)
  val index  = io.reqIn.pc(param.indexBits + param.lineBits - 1 downto param.lineBits)  // 按照每块8字节，8块来计算的话，index = io.reqIn.pc(4 downto 2)
  // ================================ 存储阵列 (寄存器) ================================ //
  val dataMem  = Reg(Vec(UInt(param.dataBits bits), param.lines)) // 数据
  val tagMem   = Reg(Vec(UInt(param.tagBits bits), param.lines))  // 标签
  val validReg = Reg(Bits(param.lines bits)) init(0)              // 每块1位有效位

  // 命中判断，当前索引位有效并且tag相等
  val hit = validReg(index) && (tagMem(index) === tag)

  val lineReg    = Reg(Bits(param.dataBits bits)) init(0)   // 正在拼装的行(逐字填入)
  val wordCnt    = Reg(UInt(param.wordBits bits)) init(0)   // 已读到块内第几个字

  val wordSel = if (param.words > 1) io.reqIn.pc(param.lineBits - 1 downto 2) else U(0, 1 bits)
  val wordSelReg = RegNextWhen(wordSel, io.reqIn.fire) init(0)    // 本次请求的是块内第几个字
  // ================================ 握手成功锁存数据 ================================ //
  val pcReg    = RegNextWhen(io.reqIn.pc, io.reqIn.fire) init(0)
  val indexReg = RegNextWhen(index      , io.reqIn.fire) init(0)
  val tagReg   = RegNextWhen(tag        , io.reqIn.fire) init(0)
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
  io.missDone := missDone

  // 把一个 dataBits 位的"行"拆成 words 个 32 位字, 用动态索引选(SpinalHDL 会生成 mux 树)
  val hitWordVec  = Vec(UInt(32 bits), param.words)
  val missWordVec = Vec(UInt(32 bits), param.words)
  for (i <- 0 until param.words) {
    hitWordVec (i) := dataMem(index)(i*32 + 31 downto i*32)
    missWordVec(i) := axi4Ctrler.io.readData(i*32 + 31 downto i*32)
  }

  io.rspOut.rdata := Mux(io.reqIn.fire && hit,
                        hitWordVec (wordSel),      // 命中: 当前pc的字
                        missWordVec(wordSelReg))   // 缺失: 请求时锁存的字
                          
  // readReq要求只持续一个周期
  val enterMiss = (state === IcacheState.Idle) && io.reqIn.valid && !hit
  axi4Ctrler.io.readReq  := enterMiss
  axi4Ctrler.io.readAddr := (io.reqIn.pc(31 downto param.lineBits) ## U(0, param.lineBits bits)).asUInt // 地址对齐

  // 缺失脉冲: 每一次缺失拉高一拍(交给 IFU 的 PerfReg 计数, 用于统计命中率)
  // 等价于 io.reqIn.fire && !hit —— 因为 reqIn.ready 只在 Idle 时拉高
  io.miss := enterMiss

  // 缺失完成: 写回cache(valid/tag/data)
  when(missDone) {
    dataMem (indexReg) := axi4Ctrler.io.readData
    tagMem  (indexReg) := tagReg
  }

  // fence.i: 清空全部有效位(后续取指缺失重读新指令)
  when(io.fenceI) {
    validReg := 0
  } elsewhen(missDone) {
    validReg(indexReg) := True
  } otherwise {
    validReg := validReg
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
  val wordCnt = if (param.words > 1) UInt(param.wordBits bits) else UInt(1 bits)    // 读取到了第几个数，按照约束最多只会一次8个
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
    chain.otherwise { lineReg := lineReg }
  }

  when(io.readReq) {      // 请求开始时清零，所以不需要reset信号
    wordCnt := 0
  } elsewhen(readOnce) {
    wordCnt := wordCnt + 1
  } otherwise {
    wordCnt := wordCnt
  }

  // 读响应错误检查: 从机返回非 OKAY 时仿真报错
  when(io.axi4.r.fire && io.axi4.r.resp =/= Axi4Define.resp.OKAY) {
    report(Seq("[IFU] read resp error! resp =", io.axi4.r.resp, "addr =", io.axi4.ar.addr))
  }
}
