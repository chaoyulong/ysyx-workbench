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
    val csr_addr   = in  UInt(12 bits)    // csr地址
    val csr_wdata  = in  UInt(32 bits)    
    val csr_rdata  = out UInt(32 bits)
    val csrCmd    = in  UInt(3 bits)     // 0=NOP,1=CSRRW,2=CSRRS
    val csrWr     = in  Bool()           // CSR写使能: 仅真正需要写时置1(csrr读操作不写)
    val trapEnter = in  Bool()           // 异常进入
    val trapExit  = in  Bool()           // MRET

    val pc_in      = in  UInt(32 bits)    // 用于写mepc
    val cause_in   = in  UInt(32 bits)    // 异常原因
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

  mcycle := mcycle + 1
  when(io.instrRetire) { minstret := minstret + 1 }

  // 双向快照: 访问任一半(低/高)时同时锁存高低位, 保证两次读取组合的64位值一致
  val isCsrAccess   = io.csrCmd =/= 0                                  // 仅CSR指令(CSRRW/CSRRS)访问
  val accMcycle     = isCsrAccess && (io.csr_addr === CSR.mcycle  || io.csr_addr === CSR.mcycleh)
  val accMinstret   = isCsrAccess && (io.csr_addr === CSR.minstret || io.csr_addr === CSR.minstreth)
  val mcycleLowSnap    = RegNextWhen(mcycle(31 downto 0),    accMcycle)   init(0)
  val mcycleHighSnap   = RegNextWhen(mcycle(63 downto 32),   accMcycle)   init(0)
  val minstretLowSnap  = RegNextWhen(minstret(31 downto 0),  accMinstret) init(0)
  val minstretHighSnap = RegNextWhen(minstret(63 downto 32), accMinstret) init(0)

  io.csr_rdata := io.csr_addr.mux(
    CSR.mstatus   -> mstatus,
    CSR.mtvec     -> mtvec,
    CSR.mepc      -> mepc,
    CSR.mcause    -> mcause,
    CSR.mvendorid -> mvendorid,
    CSR.marchid   -> marchid,
    CSR.mcycle    -> mcycleLowSnap,
    CSR.mcycleh   -> mcycleHighSnap,
    CSR.minstret  -> minstretLowSnap,
    CSR.minstreth -> minstretHighSnap,
    default       -> U"32'h0"
  )

  // 调试: 观察所有 CSR 访问
  when(io.csrCmd =/= 0) {
    report(Seq("[CSR] addr=", io.csr_addr, " cmd=", io.csrCmd, " wr=", io.csrWr))
  }
  // 调试: 观察 mcycle 访问
  when(io.csrCmd =/= 0 && (io.csr_addr === CSR.mcycle || io.csr_addr === CSR.mcycleh)) {
    report(Seq("[CSR] mcycle addr=", io.csr_addr, " cmd=", io.csrCmd, " wr=", io.csrWr,
               " mcycle=", mcycle, " snapLo=", mcycleLowSnap, " snapHi=", mcycleHighSnap,
               " rdata=", io.csr_rdata))
  }

  val writeEnable = io.csrWr     // 只有真正写CSR的指令才使能(防止csrr读操作把计数寄存器写回旧值)
  val csr_old = io.csr_rdata

  val writeData = io.csrCmd.mux(
    U"3'd1" -> io.csr_wdata,                // CSRRW
    U"3'd2" -> (csr_old | io.csr_wdata),    // CSRRS
    default -> csr_old
  )

  when(writeEnable){
    switch(io.csr_addr){
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
    mepc   := io.pc_in
    mcause := io.cause_in
  }

  io.mtvec := mtvec
  io.mepc := mepc
}
