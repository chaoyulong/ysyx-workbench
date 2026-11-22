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
  lineBytes: Int = 16,      // 块大小(字节), 4B 起步(后续可加大配合突发)
  lines:     Int = 2        // 块数(直接映射组数)
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
  val tag    = io.reqIn.pc(31 downto param.indexBits + param.lineBits)                  // 按照每块8字节，8块来计算的话，tag = io.reqIn.pc(31 downto 5)
  val index  = io.reqIn.pc(param.indexBits + param.lineBits - 1 downto param.lineBits)  // 按照每块8字节，8块来计算的话，index = io.reqIn.pc(4 downto 2)
  // ================================ 存储阵列 (寄存器) ================================ //
  val dataMem  = Reg(Vec(UInt(param.dataBits bits), param.lines)) // 数据
  val tagMem   = Reg(Vec(UInt(param.tagBits bits), param.lines))  // 标签
  val validReg = Reg(Bits(param.lines bits)) init(0)              // 每块1位有效位

  // 命中判断，当前索引位有效并且tag相等
  val hit = validReg(index) && (tagMem(index) === tag)

  val wordSel = if (param.words > 1) io.reqIn.pc(param.lineBits - 1 downto 2) else U(0, 1 bits)
  val wordSelReg = RegNextWhen(wordSel, io.reqIn.fire)            // 本次请求的是块内第几个字
  // ================================ 握手成功锁存数据 ================================ //
  val indexReg = RegNextWhen(index      , io.reqIn.fire)
  val tagReg   = RegNextWhen(tag        , io.reqIn.fire)

  val readEnd = axi4Ctrler.io.readEnd
  // ================================ 状态机 ================================ //
  object IcacheState extends SpinalEnum {
    val Idle, Miss, MissDone = newElement()
  }
  val state = RegInit(IcacheState.Idle)

  when(state === IcacheState.Idle) {
    when(io.reqIn.valid && !hit) { state := IcacheState.Miss }  // 有请求但是没有命中
    .otherwise { state := IcacheState.Idle }
  } elsewhen(state === IcacheState.Miss) {
    when(readEnd) { state := IcacheState.MissDone }             // 访存完成进入完成状态，这个状态是必须的，因为需要readEnd为1时需要将最后一个数据写入cache
    .otherwise { state := IcacheState.Miss }
  } elsewhen(state === IcacheState.MissDone) {
    state := IcacheState.Idle                                   // rspOut是flow类型，不需要握手
  }
  // ================================  ================================ //
  // 请求握手: Idle时接受(命中同拍组合返回rspOut, 缺失进入Miss)
  io.reqIn.ready := (state === IcacheState.Idle)

  val readEndOnce = (state === IcacheState.Miss) && axi4Ctrler.io.readEndOnce
  io.rspOut.valid := (io.reqIn.fire && hit) || (state === IcacheState.MissDone)   // 命中，或者未命中但读取完成
  
  // 把一个 dataBits 位的"行"拆成 words 个 32 位字, 用动态索引选(SpinalHDL 会生成 mux 树)
  val wordVec = Vec(UInt(32 bits), param.words)
  val indexRead   = Mux(io.reqIn.fire, index, indexReg)               // 如果命中，当前周期就要读出，此时indexReg没有更新，要使用index
  val wordSelRead = Mux(io.reqIn.fire && hit, wordSel, wordSelReg)    // 如果命中，当前周期就要读出，此时wordSelReg没有更新，要使用wordSel
  for (i <- 0 until param.words) {
    wordVec(i) := dataMem(indexRead)(i*32 + 31 downto i*32)
  }
 
  io.rspOut.rdata := wordVec(wordSelRead)
                          
  // readReq要求只持续一个周期
  val enterMiss = (state === IcacheState.Idle) && io.reqIn.valid && !hit
  axi4Ctrler.io.readReq  := enterMiss
  axi4Ctrler.io.readAddr := (io.reqIn.pc(31 downto param.lineBits) ## U(0, param.lineBits bits)).asUInt // 地址对齐

  val readErr = RegInit(False)
  val rspErr = readErr || axi4Ctrler.io.rspErr // 最后一个数据等不等更新完成，相当于提前一个周期
  
  when(state === IcacheState.MissDone) {        // 发送到IFU后清空
    readErr := False
  } elsewhen(axi4Ctrler.io.rspErr) {
    readErr := True
  }

  io.rspErr := (state === IcacheState.MissDone) && rspErr

  when(readEndOnce && !rspErr) {              // 读一个存一个
    for (i <- 0 until param.words) {
      when (axi4Ctrler.io.wordCnt === i) {
        dataMem(indexReg)(i*32 + 31 downto i*32) := axi4Ctrler.io.readDataOnce
      }    
    }
  }

  when(readEnd && !rspErr) {                // tag最后更新
    tagMem (indexReg) := tagReg
  }

  // fence.i: 清空全部有效位(后续取指缺失重读新指令)
  val discardMiss = RegInit(False)              // fence到达时正在读取的miss
  when(io.fenceI) {
    discardMiss := (state === IcacheState.Miss)
  } elsewhen(state === IcacheState.MissDone) {  // 只是为了卡住访存完成后的那一个周期，所以之后就可以清空
    discardMiss := False
  } otherwise {
    discardMiss := discardMiss
  }

  when(io.fenceI) {
    validReg := 0
  } elsewhen(io.rspErr) {   // 如果有错误，直接将这一行清掉
    validReg(indexReg) := False
  } elsewhen(readEnd && !discardMiss) {  // 如果是卡住的话，vaild不会置起，所以会开始下一次访存
    validReg(indexReg) := True
  } otherwise {
    validReg := validReg
  }

  if (config.enableSimDebug) {
    // 缺失脉冲: 每一次缺失拉高一拍(交给 IFU 的 PerfReg 计数, 用于统计命中率)
    // 等价于 io.reqIn.fire && !hit —— 因为 reqIn.ready 只在 Idle 时拉高
    io.miss := enterMiss
    io.missDone := state === IcacheState.MissDone
  } 
}

/* ****************************************************************
  只有读通道的axi总线控制器
**************************************************************** */
case class ysyx_23060082_Axi4_Ctrler_ReadOnly_Burst(param: IcacheParams = IcacheParams()) extends Component {
  val io = new Bundle {
    val readReq     = in  Bool()
    val readAddr    = in  UInt(32 bits)
    val readEnd     = out Bool()
    val readDataOnce= out UInt(32 bits)
    val readEndOnce = out Bool()
    val wordCnt     = if (param.words > 1) out UInt(param.wordBits bits) else out UInt(1 bits)
    val rspErr      = out Bool()   // 读到的数据有错误
    val axi4        = master(Axi4ReadOnly(AxiConfig.axiConfig))
  }

  val wordCnt  = if (param.words > 1) Reg(UInt(param.wordBits bits)) else Reg(UInt(1 bits))     // 读取到了第几个数，按照约束最多只会一次8个
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
  io.readEnd  := readOnce && io.axi4.r.last     // 突发结束(r.last)才算读完                          

  io.readEndOnce := readOnce
  io.readDataOnce := io.axi4.r.data.asUInt
  io.wordCnt := wordCnt

  when(io.readReq) {      // 请求开始时清零，所以不需要reset信号
    wordCnt := 0
  } elsewhen(readOnce) {
    wordCnt := wordCnt + 1
  } otherwise {
    wordCnt := wordCnt
  }

  io.rspErr := io.axi4.r.fire && (io.axi4.r.payload.resp =/= B"2'b00")
}
