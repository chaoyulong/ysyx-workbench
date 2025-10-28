package playground

import spinal.core._
import spinal.lib._       // 使用spinal的模块库


case class CPU() extends Component {
  val io = new Bundle {
  }

  clockDomain.clock.setName("clock")  // 自定义时钟和复位信号名称，放在最顶层
  clockDomain.reset.setName("reset")

  val reg_file = RegFile()
  val ifu = ysyx_23060082_IFU()
  val idu = ysyx_23060082_IDU()
  val exu = ysyx_23060082_EXU()
  val lsu = ysyx_23060082_LSU()
  val wbu = ysyx_23060082_WBU()
  
  ifu.io.to_Idu >-> idu.io.from_Ifu   // 
  idu.io.to_Exu >-> exu.io.from_Idu
  exu.io.to_Lsu >-> lsu.io.from_Exu
  lsu.io.to_Wbu >-> wbu.io.from_Lsu
  wbu.io.to_Ifu >-> ifu.io.from_Wbu

  reg_file.io.read_addr_1 <> idu.io.rf_read_addr_1
  reg_file.io.read_addr_2 <> idu.io.rf_read_addr_2
  reg_file.io.read_data_1 <> idu.io.rf_read_data_1
  reg_file.io.read_data_2 <> idu.io.rf_read_data_2
  reg_file.io.write_addr  <> wbu.io.rf_write_addr
  reg_file.io.write_data  <> wbu.io.rf_write_data
  reg_file.io.write_en    <> wbu.io.rf_write_en  


}

case class RegFile() extends Component {
  val io = new Bundle {
    val read_addr_1 = in UInt(5 bits)
    val read_addr_2 = in UInt(5 bits)
    val write_addr  = in UInt(5 bits)
    val write_data  = in UInt(32 bits)
    val write_en    = in Bool()

    val read_data_1 = out UInt(32 bits)
    val read_data_2 = out UInt(32 bits)
  }

  val rf = Vec(Reg(UInt(32 bits)),16)    // riscv32e,有16个通用寄存器
  when(io.write_en){
    rf(io.write_addr(0 to 3)) := io.write_data
  }
  rf(U"4'h0") := U"32'h0"
  
  io.read_data_1 := rf(io.read_addr_1(0 to 3))
  io.read_data_2 := rf(io.read_addr_2(0 to 3)) 
}


