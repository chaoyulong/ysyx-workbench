package playground

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi._

// ================================ 简易指令缓存 (icache) ================================ //
// 直接映射(direct-mapped), 寄存器实现, 参数化(块大小/块数)
// 接口:
//   reqIn  : Stream 取指请求(pc) —— IFU 发请求, icache 接受(fire)后处理
//   rspOut : Flow   指令返回(rdata) —— 命中一拍返回; 缺失访存完成后返回
//   axi4   : Axi4ReadOnly —— 缺失时经只读控制器访问内存(单次读, len=0)
// 命中: 请求拍组合判断(tag匹配 && valid), 同拍返回 rdata
// 缺失: 进入 Miss 状态, 经 AXI 读回并写回 cache(valid/tag/data), 完成后返回
case class IcacheParams(
  lineBytes: Int = 4,     // 块大小(字节), 4B 起步(后续可加大配合突发)
  lines:     Int = 16     // 块数(直接映射组数)
)

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
  }

  // ================================ 参数与地址划分 ================================ //
  val lineBits  = log2Up(param.lineBytes)   // 块内偏移位数(offset)
  val indexBits = log2Up(param.lines)       // 索引位数(index)
  val tagBits   = 32 - lineBits - indexBits // tag 位数

  // ================================ 存储阵列 (寄存器) ================================ //
  val dataMem  = Reg(Vec(UInt(32 bits), param.lines))       // 数据
  val tagMem   = Reg(Vec(UInt(tagBits bits), param.lines))  // 标签
  val validReg = Reg(Bits(param.lines bits)) init(0)        // 每块1位有效位

  // 命中判断，当前索引位有效并且tag相等
  val hit = validReg(index) && (tagMem(index) === tag)

  // ================================ 握手成功锁存数据 ================================ //
  val pcReg    = RegNextWhen(io.reqIn.pc, io.reqIn.fire) init(0)
  val indexReg = RegNextWhen(index      , io.reqIn.fire) init(0)
  val tagReg   = RegNextWhen(tag        , io.reqIn.fire) init(0)
  val reqFire  = io.reqIn.fire

  val tag    = io.reqIn.pc(31 downto indexBits + lineBits)            // 按照每块4字节，16块来计算的话，tag = io.reqIn.pc(31 downto 6)
  val index  = io.reqIn.pc(indexBits + lineBits - 1 downto lineBits)  // 按照每块4字节，16块来计算的话，index = io.reqIn.pc(5 downto 2)
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
  // ifu的axi交给icache控制
  val axi4Ctrler = ysyx_23060082_Axi4_Ctrler_ReadOnly()
  io.axi4 <> axi4Ctrler.io.axi4

  // 请求握手: Idle时接受(命中同拍组合返回rspOut, 缺失进入Miss)
  io.reqIn.ready := (state === IcacheState.Idle)

  // 命中 或 缺失但是读取完成
  val missDone = (state === IcacheState.Miss) && axi4Ctrler.io.readEnd
  io.rspOut.valid := (io.reqIn.fire && hit) || missDone
  io.rspOut.rdata := Mux(io.reqIn.fire && hit, dataMem(index), axi4Ctrler.io.readData)
                          
  // readReq要求只持续一个周期
  val enterMiss = (state === IcacheState.Idle) && io.reqIn.valid && !hit
  axi4Ctrler.io.readReq  := enterMiss
  axi4Ctrler.io.readAddr := (io.reqIn.pc(31 downto 2) ## U"2'b00").asUInt // 地址对齐

  // 缺失完成: 写回cache(valid/tag/data)
  when(missDone) {
    dataMem(indexReg)  := axi4Ctrler.io.readData
    tagMem(indexReg)   := tagReg
    validReg(indexReg) := True
  }
  // fence.i: 清空全部有效位(后续取指缺失重读新指令)
  when(io.fenceI) {
    validReg := 0
  }
}
