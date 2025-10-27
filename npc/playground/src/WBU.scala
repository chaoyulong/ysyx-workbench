package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

case class Wbu2Ifu_data() extends Bundle {
  val pc_next       = UInt(32 bits)
}

case class ysyx_23060082_WBU() extends Component {
  val io = new Bundle {
    val from_Lsu  = slave Stream(Exu2Lsu_data())
    val to_Ifu    = master Stream(Wbu2Ifu_data()) 
  }


  
}