package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

case class RegFileReadBus() extends Bundle with IMasterSlave {
  val addr1 = UInt(5 bits)
  val addr2 = UInt(5 bits)
  val data1 = UInt(32 bits)
  val data2 = UInt(32 bits)

  override def asMaster(): Unit = {
    out(addr1)
    out(addr2)
    in(data1)
    in(data2)
  }
}

case class RegFileWriteBus() extends Bundle with IMasterSlave {
  val addr = UInt(5 bits)
  val data = UInt(32 bits)
  val en   = Bool()

  override def asMaster(): Unit = {
    out(addr)
    out(data)
    out(en)
  }
}

case class ysyx_23060082_RegFile() extends Component {
  val io = new Bundle {
    val readBus  = slave(RegFileReadBus())
    val writeBus = slave(RegFileWriteBus())
  }

  val rf = Vec(Reg(UInt(32 bits)),16)    // riscv32e,有16个通用寄存器

  rf(0) := U"32'h0" 
  when(io.writeBus.en && (io.writeBus.addr(3 downto 0) =/= U(0))){
    rf(io.writeBus.addr(3 downto 0)) := io.writeBus.data
  }

  io.readBus.data1 := rf(io.readBus.addr1(3 downto 0))
  io.readBus.data2 := rf(io.readBus.addr2(3 downto 0))
}
