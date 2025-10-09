package playground

import spinal.core._

case class CPU() extends Component {
  val io = new Bundle {
  }

  clockDomain.clock.setName("clock")  // 自定义时钟和复位信号名称，放在最顶层
  clockDomain.reset.setName("reset")

  val inReset = RegInit(False)   // 创建一个寄存器来指示复位
  inReset := True

  val pc   = Reg(UInt(32 bits)) init(U"32'h80000000")
  // val instr = Mem_rw(inReset, False, reg_pc, U"32'h0", U"4'h0")    // 只用于读取指令
  val instr = IFU_Rd(pc)
  val decoder = Decoder() 
  val alu = ALU()
  val branch_cond = BranchCond()
  val reg_file = RegFile()

  // 控制信号
  val reg_wr   = decoder.io.reg_wr
  val alu_asrc = decoder.io.alu_asrc
  val alu_bsrc = decoder.io.alu_bsrc
  val alu_ctr  = decoder.io.alu_ctr 
  val branch   = decoder.io.branch  
  val mem2reg  = decoder.io.mem2reg 
  val mem_wr   = decoder.io.mem_wr  
  val mem_op   = decoder.io.mem_op  
  val imm      = decoder.io.imm

  val rs1      = reg_file.io.rs1
  val rs2      = reg_file.io.rs2

  val less     = alu.io.less
  val zero     = alu.io.zero
  val alu_result = alu.io.alu_result 

  val pc_asrc  = branch_cond.io.pc_asrc
  val pc_bsrc  = branch_cond.io.pc_bsrc


  val next_pc  = Mux(pc_asrc, imm, U"32'd4") + Mux(pc_bsrc, rs1, mem_op)

  // val data_mem_rdata = Mem_rw(mem2reg | mem_wr, mem_wr, reg_pc, U"32'h0", U"4'h0")
  val data_mem_rdata = LSU_RW(mem2reg | mem_wr, mem_wr, alu_result, rs2, U"4'h0")

  reg_file.io.addr_a := instr(19 downto 15)
  reg_file.io.addr_b := instr(24 downto 20)
  reg_file.io.addr_w := instr(11 downto 7)
  reg_file.io.wdata  := Mux(mem2reg, data_mem_rdata, alu_result)
  reg_file.io.reg_wr := reg_wr

  pc := next_pc

  decoder.io.instr := instr

  alu.io.alu_in1 := Mux(decoder.io.alu_asrc, rs1, pc)
  switch(alu_bsrc){
    is(U"00"){alu.io.alu_in2 := rs2}
    is(U"01"){alu.io.alu_in2 := imm}
    default  {alu.io.alu_in2 := U"32'h4"}
  }
  alu.io.alu_ctr := alu_ctr

  branch_cond.io.branch := branch
  branch_cond.io.less   := less
  branch_cond.io.zero   := zero

}

case class RegFile() extends Component {
  val io = new Bundle {
    val addr_a = in UInt(5 bits)
    val addr_b = in UInt(5 bits)
    val addr_w = in UInt(5 bits)
    val wdata  = in UInt(32 bits)
    val reg_wr = in Bool()

    val rs1 = out UInt(32 bits)
    val rs2 = out UInt(32 bits)
  }

  val rf = Vec(RegInit(U"32'h0"), 16)    // riscv32e,有16个通用寄存器
  when(io.reg_wr){
    rf(io.addr_w(0 to 3)) := io.wdata
  }
  rf(U"4'h0") := U"32'h0"
  
  io.rs1 := rf(io.addr_a(0 to 3))
  io.rs2 := rf(io.addr_b(0 to 3)) 
}

/*    Branch      跳转类型
  -------------------------------------
      000         非跳转指令
      001         无条件跳转PC目标
      010         无条件跳转通用寄存器目标
      100         条件分支，等于
      101         条件分支，不等于
      110         条件分支，小于
      111         条件分支，大于等于
*/
case class BranchCond() extends Component {
  val io = new Bundle {
    val branch = in UInt(3 bits)
    val less   = in Bool()
    val zero   = in Bool()

    val pc_asrc= out Bool()
    val pc_bsrc= out Bool()
  }

  val branch_decoder = Decoder_3_8()
  branch_decoder.io.input := io.branch
  val decode = branch_decoder.io.output  

  io.pc_asrc := Mux(decode(U"001") | decode(U"010"), True,
                Mux(decode(U"100"),  io.zero,
                Mux(decode(U"101"), ~io.zero,
                Mux(decode(U"110"),  io.less,
                Mux(decode(U"111"), ~io.less,
                False)))))

  io.pc_bsrc := decode(U"010")
}
