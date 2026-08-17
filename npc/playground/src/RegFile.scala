package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

case class ysyx_23060082_RegFile() extends Component {
  val io = new Bundle {
    val readAddr1 = in UInt(5 bits)
    val readAddr2 = in UInt(5 bits)
    val writeAddr = in UInt(5 bits)
    val writeData = in UInt(32 bits)
    val writeEn   = in Bool()
    val readData1 = out UInt(32 bits)
    val readData2 = out UInt(32 bits)
  }

  val rf = Vec(Reg(UInt(32 bits)),16)    // riscv32e,有16个通用寄存器

  rf(0) := U"32'h0" 
  when(io.writeEn && (io.writeAddr(3 downto 0) =/= U(0))){
    rf(io.writeAddr(3 downto 0)) := io.writeData
  }

  io.readData1 := rf(io.readAddr1(3 downto 0))
  io.readData2 := rf(io.readAddr2(3 downto 0)) 
}
