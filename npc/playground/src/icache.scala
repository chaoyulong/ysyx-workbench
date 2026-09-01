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
  }

  // ================================ 参数与地址划分 ================================ //
  val lineBits  = log2Up(param.lineBytes)   // 块内偏移位数(offset)
  val indexBits = log2Up(param.lines)       // 索引位数(index)
  val tagBits   = 32 - lineBits - indexBits // tag 位数

  val tag    = io.reqIn.pc(tagBits + indexBits + lineBits - 1 downto indexBits + lineBits)
  val index  = io.reqIn.pc(indexBits + lineBits - 1 downto lineBits)

  // ================================ 存储阵列 (寄存器) ================================ //
  // data/tag 不 init(读由 valid 保护——未命中不读, 无 x 传播)
  val dataMem  = Mem(UInt(32 bits), param.lines)
  val tagMem   = Mem(UInt(tagBits bits), param.lines)
  // valid 用寄存器数组(复位清零); data/tag 用 Mem(读由 valid 保护)
  val validReg = Reg(Bits(param.lines bits)) init(0)   // 每块 1 位有效位

  // 命中判断(组合, 当前请求)
  val hit = validReg(index) && (tagMem.readAsync(index) === tag)

  // ================================ 请求锁存(Miss 期间 reqIn 可能变化) ================================ //
  val pcReg   = Reg(UInt(32 bits)) init(0)
  val indexReg = Reg(UInt(indexBits bits)) init(0)
  val tagReg   = Reg(UInt(tagBits bits)) init(0)
  val reqFire  = io.reqIn.valid && io.reqIn.ready
  when(reqFire) {
    pcReg   := io.reqIn.pc
    indexReg := index
    tagReg   := tag
  }

  // ================================ 状态机 ================================ //
  object IcacheState extends SpinalEnum {
    val Idle, Miss = newElement()
  }
  val state = Reg(IcacheState()) init(IcacheState.Idle)

  // 只读 AXI 控制器(缺失访存)
  val axi4Ctrler = ysyx_23060082_Axi4_Ctrler_ReadOnly()
  io.axi4 <> axi4Ctrler.io.axi4

  // 请求握手: Idle 时接受(命中同拍返回, 缺失进入 Miss)
  io.reqIn.ready := (state === IcacheState.Idle) && !io.rspOut.valid

  // 响应: 命中(请求拍组合) 或 缺失完成拍
  val missDone = (state === IcacheState.Miss) && axi4Ctrler.io.readEnd
  io.rspOut.valid := (reqFire && hit) || missDone
  io.rspOut.rdata := Mux(reqFire && hit,
                          dataMem.readAsync(index),
                          axi4Ctrler.io.readData)

  // 状态转移
  when(state === IcacheState.Idle) {
    when(io.reqIn.valid && !hit) {
      state := IcacheState.Miss   // 缺失: 访存
    }
  }
  when(state === IcacheState.Miss) {
    when(axi4Ctrler.io.readEnd) {
      state := IcacheState.Idle   // 访存完成, 返回
    }
  }

  // 访存控制(用锁存的请求)
  axi4Ctrler.io.readReq  := (state === IcacheState.Miss)
  axi4Ctrler.io.readAddr := (pcReg(31 downto 2) ## U"2'b00").asUInt   // 4B 块对齐地址

  // 缺失完成: 写回 cache(valid/tag/data)
  when(missDone) {
    dataMem.write(indexReg, axi4Ctrler.io.readData)
    tagMem.write(indexReg, tagReg)
    validReg(indexReg) := True
  }
}
