package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

case class Decoder_out() extends Bundle {
  val reg_wr   = Bool()  // 控制是否对寄存器rd进行写回，为1时写回寄存器。
  val alu_asrc = Bool() // 选择ALU输入端A的来源。为0时选择rs1，为1时选择PC。
  val alu_bsrc = UInt(2 bits)  // 选择ALU输入端B的来源。为00时选择rs2，为01时选择imm，为10时选择常数4（用于跳转时计算返回地址PC+4）
  val alu_ctr  = UInt(4 bits)  // 选择ALU执行的操作
  val branch   = UInt(3 bits)  // 说明分支和跳转的种类，用于生成最终的分支控制信号
  val mem2reg  = Bool()  // 选择写入寄存器的内容，为1时为存储器，为0时为alu
  val mem_wr   = Bool()  // 为1时写入存储器
  val mem_op   = UInt(3 bits)  // 控制数据存储器读写格式，为010时为4字节读写，为001时为2字节读写带符号扩展，为000时为1字节读写带符号扩展，为101时为2字节读写无符号扩展，为100时为1字节读写无符号扩展
  val imm      = UInt(32 bits)  // 立即数
}

case class Rf_read() extends Bundle {
  val read_addr_1 = UInt(5 bits)
  val read_addr_2 = UInt(5 bits)
  val read_data_1 = UInt(32 bits)
  val read_data_2 = UInt(32 bits)
}

case class ysyx_23060082_IDU() extends Component {
  val io = new Bundle {
    val from_Ifu  = slave Stream(Ifu2Idu_data())
    val to_Exu = master Stream(Idu2Exu_data()) 

    val rf_read_addr_1 = UInt(5 bits)
    val rf_read_addr_2 = UInt(5 bits)
    val rf_read_data_1 = UInt(32 bits)
    val rf_read_data_2 = UInt(32 bits)
  }

  val idu_pc  = io.from_Ifu.pc
  val decoder = Decoder()

  decoder.instr := io.from_Ifu.instr         

  io.rf_read_addr_1 := io.from_Ifu.instr(19 downto 15)
  io.rf_read_addr_2 := io.from_Ifu.instr(24 downto 20)  
  val read_data_1 = io.rf_read_data_1
  val read_data_2 = io.rf_read_data_2
  // io.idu_read_rf.addr_w := instr(11 downto 7)

  io.from_Ifu.ready := io.from_Ifu.valid
  
}