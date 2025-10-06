package playground

import spinal.core._

case class RegFile extends Component {
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
  when(w_en){
    rf(addr_w(0 to 3)) := wdata
  }

  rf(0) := U"32'h0"
  rs1 := rf(addr_a(0 to 3))
  rs2 := rf(addr_b(0 to 3)) 
}