package playground

import spinal.core._

case class RegFile() extends Component {
  val io = new Bundle {
    val addr_a = in port UInt(5 bits)
    val addr_b = in port UInt(5 bits)
    val addr_w = in port UInt(5 bits)
    val wdata  = in port UInt(32 bits)
    val w_en   = in port Bool

    val rs1 = out port UInt(32 bits)
    val rs2 = out port UInt(32 bits)
  }

  val rf = Vec(Reg(UInt(32 bits)), 16)    // riscv32e,有16个通用寄存器
  when(io.w_en){
    rf(io.addr_w(0 to 3)) := io.wdata
  }

  rf(0) := U"32'h0"
  io.rs1 := rf(io.addr_a(0 to 3))
  io.rs2 := rf(io.addr_b(0 to 3)) 
}