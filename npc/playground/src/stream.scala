package playground

import spinal.core._

// 打算用来存储数据

case class Lsu2Ifu_data() extends Bundle {
  val pc_next = UInt(32 bits)
}

case class Ifu2Idu_data() extends Bundle {
  val pc    = UInt(32 bits)
  val instr = UInt(32 bits)
}

case class Idu2Exu_data() extends Bundle {
  val pc    = UInt(32 bits)
  val instr = UInt(32 bits)
}