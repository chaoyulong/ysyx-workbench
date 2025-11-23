package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

object CSR {
  val mstatus    = 0x300    // 状态
  val mtvec      = 0x305    // 异常入口地址
  val mepc       = 0x341    // 异常pc
  val mcause     = 0x342    // 原因
  val mvendorid  = 0xF11    // VendorID，从中读出ysyx的ASCII码, 即0x79737978    
  val marchid    = 0xF12    // ArchitectureID，从中读出学号数字部分的十进制表示, 读出23060082, 即0x15fde72
}

case class ysyx_23060082_CSR() extends Component {
  val io = new Bundle {
    val csr_addr  = in UInt(12 bits)    // csr地址
    val csr_wdata = in UInt(32 bits)    
    val csr_rdata = out UInt(32 bits)
    val csr_cmd   = in UInt(3 bits)     // 0=NOP,1=CSRRW,2=CSRRS
    val trap_enter = in Bool()          // 异常进入
    val trap_exit  = in Bool()          // MRET

    val pc_in  = in UInt(32 bits)    // 用于写mepc
    val cause_in = in UInt(32 bits)  // 异常原因
  }

  val mstatus   = Reg(UInt(32 bits)) init(0)
  val mtvec     = Reg(UInt(32 bits)) init(0)
  val mepc      = Reg(UInt(32 bits)) init(0)
  val mcause    = Reg(UInt(32 bits)) init(0)
  val mvendorid = U"32'h79737978"   // 只读
  val marchid   = U"32'h15fde72"    // 只读

  io.csr_rdata := io.csr_addr.mux(
    CSR.mstatus   -> mstatus,
    CSR.mtvec     -> mtvec,
    CSR.mepc      -> mepc,
    CSR.mcause    -> mcause,
    CSR.mvendorid -> mvendorid,
    CSR.marchid   -> marchid,
    default       -> U"32'h0"
  )

  val writeEnable = io.csr_cmd =/= 0
  val csr_old = io.csr_rdata

  val writeData = io.csr_cmd.mux(
    U"3'd1" -> io.csr_wdata,                // CSRRW
    U"3'd2" -> (csr_old | io.csr_wdata),    // CSRRS
    default -> csr_old
  )

  when(writeEnable){
    switch(io.csr_addr){
      is(CSR.mstatus) { mstatus := writeData }
      is(CSR.mtvec)   { mtvec   := writeData }
      is(CSR.mepc)    { mepc    := writeData }
      is(CSR.mcause)  { mcause  := writeData }
    }
  }

  when(io.trap_enter){
    mepc   := io.pc_in
    mcause := io.cause_in
    mstatus(7) := False  // MIE 清除
  }

  when(io.trap_exit){
    mstatus(7) := True   // 恢复 MIE
  }
}
