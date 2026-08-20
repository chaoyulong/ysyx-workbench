package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

case class Idu2Exu_data() extends Bundle {
  val pc    = UInt(32 bits)

  val ctrl        = CtrlSignals()
  val imm         = UInt(32 bits)
  val rfReadData1 = UInt(32 bits)
  val rfReadData2 = UInt(32 bits)
}

case class RfCtrl() extends Bundle {  // WBU中消耗的控制信号
  val mem2reg       = Bool()          // 选择写入寄存器的内容，为1时为存储器，为0时为alu
  val csr2reg       = Bool()          // 从csr读取数据写入寄存器
  val regWr         = Bool()          // 控制是否对寄存器rd进行写回，为1时写回寄存器。
  val rfWriteAddr = UInt(5 bits)
}

case class AluCtrl() extends Bundle { // EXU中消耗的控制信号
  val aluAsrc = Bool()               // 选择ALU输入端A的来源。为0时选择rs1，为1时选择PC。
  val aluBsrc = UInt(2 bits)   // 选择ALU输入端B的来源。为00时选择rs2，为01时选择imm，为10时选择常数4（用于跳转时计算返回地址PC+4）
  val aluCtr  = UInt(4 bits)   // 选择ALU执行的操作
  val branch   = UInt(3 bits)   // 说明分支和跳转的种类，用于生成最终的分支控制信号
}

case class MemCtrl() extends Bundle { // LSU中消耗的控制信号
  val memWr   = Bool()                // 为1时写入存储器
  val memOp   = UInt(3 bits)          // 控制数据存储器读写格式，为010时为4字节读写，为001时为2字节读写带符号扩展，为000时为1字节读写带符号扩展，为101时为2字节读写无符号扩展，为100时为1字节读写无符号扩展
}

case class CsrCtrl() extends Bundle { // CSR寄存器的控制信号
  val csrCmd    = UInt(3 bits)       // 0=NOP,1=CSRRW,2=CSRRS,其他后续可能有用
  val illegal  = Bool()             // 非法指令
  val ebreak   = Bool()     
  val trapEnter = Bool()             // 异常进入
  val trapExit  = Bool()             // MRET        
}

case class CtrlSignals() extends Bundle {   // 控制信号
  val rfCtrl  = out(RfCtrl())
  val aluCtrl = out(AluCtrl())
  val memCtrl = out(MemCtrl())
  val csrCtrl = out(CsrCtrl())
}

case class ysyx_23060082_IDU() extends Component {
  val io = new Bundle {
    val input  = slave  Flow(Ifu2Idu_data())
    val output = master Stream(Idu2Exu_data()) 

    val rfRead = master(RegFileReadBus())
  }

  val instr   = io.input.instr
  val decoder = ysyx_23060082_Decoder()
  decoder.instr := instr         
  // ================================ 用于握手的部分 ================================ //
  val willValid = True
  io.output.valid := io.input.valid && willValid    // io.input.valid为数据有效信号，是寄存器信号
  // ================================ 数据传输部分 ================================ //
  io.rfRead.addr1  := decoder.io.ctrl.csrCtrl.trapEnter.mux(   // 如果是触发异常的指令，则选择a5(第15个寄存器)作为数据输入
                         True  -> U"5'd15", 
                         False -> instr(19 downto 15))
  io.rfRead.addr2  := instr(24 downto 20)  // 为了写起来简洁，写寄存器地址在decoder中赋值
  
  io.output.pc          := io.input.pc
  io.output.rfReadData1 := io.rfRead.data1
  io.output.rfReadData2 := io.rfRead.data2
  io.output.ctrl        := decoder.io.ctrl
  io.output.imm         := decoder.io.imm
  // ====================================== ====================================== //
}
