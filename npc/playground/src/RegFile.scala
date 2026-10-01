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

  // riscv32e, 有16个通用寄存器
  // ★ 只给 x3/x4(gp/tp) 加复位: 它们按 ABI 是"先写后读", 但未复位的触发器在门级网表里是 X,
  //   一旦被读到就会经比较/前递漏进控制或数据路径(实测: 抽 Xbar 状态译码改变映射后,
  //   microbench 的 [md5] 就因为读到未复位的 x3/x4 而算错). 其余寄存器保持无复位以省面积.
  val rf = Vec((0 until 16).map { i =>
    if (i == 3 || i == 4) Reg(UInt(32 bits)) init(0) else Reg(UInt(32 bits))
  })

  rf(0) := U"32'h0" 
  when(io.writeBus.en && (io.writeBus.addr(3 downto 0) =/= U(0))){
    rf(io.writeBus.addr(3 downto 0)) := io.writeBus.data
  }
  

  io.readBus.data1 := rf(io.readBus.addr1(3 downto 0))
  io.readBus.data2 := rf(io.readBus.addr2(3 downto 0))
}
