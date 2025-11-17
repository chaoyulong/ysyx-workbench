package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

case class Idu2Exu_data() extends Bundle {
  val pc    = UInt(32 bits)

  val ctrl = Ctrl()
  val imm            = UInt(32 bits)
  val rf_read_data_1 = UInt(32 bits)
  val rf_read_data_2 = UInt(32 bits)
}

case class RfCtrl() extends Bundle {   // WBU中消耗的控制信号
  val mem2reg       = Bool()    // 选择写入寄存器的内容，为1时为存储器，为0时为alu
  val reg_wr        =  Bool()   // 控制是否对寄存器rd进行写回，为1时写回寄存器。
  val rf_write_addr = UInt(5 bits)
}

case class AluCtrl() extends Bundle {   // EXU中消耗的控制信号
  val alu_asrc = Bool() // 选择ALU输入端A的来源。为0时选择rs1，为1时选择PC。
  val alu_bsrc = UInt(2 bits)  // 选择ALU输入端B的来源。为00时选择rs2，为01时选择imm，为10时选择常数4（用于跳转时计算返回地址PC+4）
  val alu_ctr  = UInt(4 bits)  // 选择ALU执行的操作
  val branch   = UInt(3 bits)  // 说明分支和跳转的种类，用于生成最终的分支控制信号
}

case class MemCtrl() extends Bundle {   // LSU中消耗的控制信号
  val mem_wr   = Bool()  // 为1时写入存储器
  val mem_op   = UInt(3 bits)  // 控制数据存储器读写格式，为010时为4字节读写，为001时为2字节读写带符号扩展，为000时为1字节读写带符号扩展，为101时为2字节读写无符号扩展，为100时为1字节读写无符号扩展
}

case class Ctrl() extends Bundle {   // 控制信号
  val rf_si  = out(RfCtrl())
  val alu_si = out(AluCtrl())
  val mem_si = out(MemCtrl())
}

case class ysyx_23060082_IDU() extends Component {
  val io = new Bundle {
    val from_Ifu  = slave Stream(Ifu2Idu_data())
    val to_Exu = master Stream(Idu2Exu_data()) 

    val rf_read_addr_1 = out UInt(5 bits)
    val rf_read_addr_2 = out UInt(5 bits)
    val rf_read_data_1 = in  UInt(32 bits)
    val rf_read_data_2 = in  UInt(32 bits)
  }

  val instr = io.from_Ifu.instr
  val decoder = Decoder()
  decoder.instr := instr         
// ***************************************** 立即数生成 *********************************************** //
  val immU = U(instr(31 downto 12) ## U(0, 12 bits))
  val immJ = U((instr(31) #* 12) ## instr(19 downto 12) ## instr(20) ## instr(30 downto 21)## U(0, 1 bits))
  val immI = U((instr(31) #* 20) ## instr(31 downto 20))
  val immS = U((instr(31) #* 20) ## instr(31 downto 25) ## instr(11 downto 7))
  val immB = U((instr(31) #* 20) ## instr(7) ## instr(30 downto 25) ## instr(11 downto 8) ## U(0, 1 bits))

  // 独热码选择器
  val has_type = decoder.io.instr_type(5 downto 1).orR
  // val imm_values = Vec(immU, immJ, immI, immS, immB)
  val imm_values = Vec(immB, immS, immI, immJ, immU)
  io.to_Exu.imm := Mux(has_type, MuxOH(decoder.io.instr_type(5 downto 1), imm_values), U"32'h0")
// *****************************************  *********************************************** //
  io.rf_read_addr_1 := instr(19 downto 15)
  io.rf_read_addr_2 := instr(24 downto 20)  // 为了写起来简洁，写寄存器地址在decoder中赋值
  
  io.to_Exu.pc              := io.from_Ifu.pc
  io.to_Exu.rf_read_data_1  := io.rf_read_data_1
  io.to_Exu.rf_read_data_2  := io.rf_read_data_2
  io.to_Exu.ctrl            := decoder.io.ctrl

  io.from_Ifu.ready := io.from_Ifu.valid
  io.to_Exu.valid := io.from_Ifu.valid
  
}