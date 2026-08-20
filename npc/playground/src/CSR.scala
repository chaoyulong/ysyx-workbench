package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

object CSR {
  def mstatus    = U"12'h300"    // 状态
  def mtvec      = U"12'h305"    // 异常入口地址
  def mepc       = U"12'h341"    // 异常pc
  def mcause     = U"12'h342"    // 原因
  def mvendorid  = U"12'hf11"    // VendorID，从中读出ysyx的ASCII码, 即0x79737978    
  def marchid    = U"12'hf12"    // ArchitectureID，从中读出学号数字部分的十进制表示, 读出23060082, 即0x15fde72
  def mcycle     = U"12'hB00"    // 机器周期计数器(低32位)
  def mcycleh    = U"12'hB80"    // 机器周期计数器(高32位)
  def minstret   = U"12'hB02"    // 机器指令退休计数器(低32位)
  def minstreth  = U"12'hB82"    // 机器指令退休计数器(高32位)
}

case class ysyx_23060082_CSR() extends Component {
  val io = new Bundle {
    val csrAddr   = in  UInt(12 bits)    // csr地址
    val csrWdata  = in  UInt(32 bits)    
    val csrRdata  = out UInt(32 bits)
    val csrCmd    = in  UInt(3 bits)     // 0=NOP,1=CSRRW,2=CSRRS
    val trapEnter = in  Bool()           // 异常进入
    val trapExit  = in  Bool()           // MRET

    val pcIn      = in  UInt(32 bits)    // 用于写mepc
    val causeIn   = in  UInt(32 bits)    // 异常原因
    val mtvec      = out UInt(32 bits)
    val mepc       = out UInt(32 bits)

    val instrRetire = in Bool()           // 指令退休信号(每完成一条指令拉高一拍), 用于计数minstret
  }

  val mstatus   = Reg(UInt(32 bits)) init(0)
  val mtvec     = Reg(UInt(32 bits)) init(0)
  val mepc      = Reg(UInt(32 bits)) init(0)
  val mcause    = Reg(UInt(32 bits)) init(0)
  val mvendorid = U"32'h79737978"   // 只读
  val marchid   = U"32'h15fde72"    // 只读
  val mcycle    = Reg(UInt(64 bits)) init(0)     // 周期计数器: 每周期+1
  val minstret  = Reg(UInt(64 bits)) init(0)     // 指令计数器: 每条退休指令+1

  val writeEnable = io.csrCmd =/= 0 // csr写使能

  val readMcycle  = io.csrAddr === CSR.mcycle
  val readMcycleh = io.csrAddr === CSR.mcycleh
  mcycle := mcycle + 1
  val mcyclehTmp = RegNextWhen(mcycle(63 downto 32), readMcycle) init(0)      // 规定先读低位，再立刻读高位

  val readMinstret  = io.csrAddr === CSR.minstret
  val readMinstreth = io.csrAddr === CSR.minstreth
  when(io.instrRetire) { minstret := minstret + 1 }
  val minstrethTmp = RegNextWhen(minstret(63 downto 32), readMinstret) init(0)  // 规定先读低位，再立刻读高位

  io.csrRdata := io.csrAddr.mux(
    CSR.mstatus   -> mstatus,
    CSR.mtvec     -> mtvec,
    CSR.mepc      -> mepc,
    CSR.mcause    -> mcause,
    CSR.mvendorid -> mvendorid,
    CSR.marchid   -> marchid,
    CSR.mcycle    -> mcycle(31 downto 0),
    CSR.mcycleh   -> mcyclehTmp,
    CSR.minstret  -> minstret(31 downto 0),
    CSR.minstreth -> minstrethTmp,
    default       -> U"32'h0"
  )

  val rdataWb = Mux(readMcycleh, mcycle(63 downto 32),
                Mux(readMinstreth, minstret(63 downto 32),  io.csrRdata))
  val writeData = io.csrCmd.mux(
    U"3'd1" -> io.csrWdata,                    // CSRRW
    U"3'd2" -> (rdataWb | io.csrWdata),   // CSRRS
    default -> rdataWb
  )

  when(writeEnable){
    switch(io.csrAddr){
      is(CSR.mstatus)   { mstatus   := writeData }
      is(CSR.mtvec)     { mtvec     := writeData }
      is(CSR.mepc)      { mepc      := writeData }
      is(CSR.mcause)    { mcause    := writeData }
      is(CSR.mcycle)    { mcycle(31 downto 0)   := writeData }
      is(CSR.mcycleh)   { mcycle(63 downto 32)  := writeData }
      is(CSR.minstret)  { minstret(31 downto 0) := writeData }
      is(CSR.minstreth) { minstret(63 downto 32):= writeData }
    }
  }

  when(io.trapEnter){
    mepc   := io.pcIn
    mcause := io.causeIn
  }

  io.mtvec := mtvec
  io.mepc := mepc
}
