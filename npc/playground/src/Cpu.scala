package playground

import spinal.core._



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

  val rf = Vec(Reg(UInt(32 bits)), 16)    // riscv32e,有16个通用寄存器
  when(io.reg_wr){
    rf(io.addr_w(0 to 3)) := io.wdata
  }
  rf(U"4'h0") := U"32'h0"
  
  io.rs1 := rf(io.addr_a(0 to 3))
  io.rs2 := rf(io.addr_b(0 to 3)) 
}

case class PCUpdate() extends Component {
  val io = new Bundle {
    val rs1 = in UInt(32 bits)
    val imm = in UInt(32 bits)
    val pc_asrc = in Bool()
    val pc_bsrc = in Bool()

    val pc = out UInt(32 bits)
  }
  
  val reg_pc = Reg(UInt(32 bits)) init(U"32'h80000000")
  val pc_add1 = Mux(io.pc_asrc, io.imm, U"32'd4")
  val pc_add2 = Mux(io.pc_bsrc, io.rs1, pc)
  val next_pc = pc_add1 + pc_add2
  val reg_pc := next_pc
  io.pc = reg_pc

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

  io.pc_bsrc := Mux(decode(U"001") | decode(U"010"), True,
                Mux(decode(U"100"),  io.zero,
                Mux(decode(U"101"), ~io.zero,
                Mux(decode(U"110"),  io.less,
                Mux(decode(U"111"), ~io.less,
                False)))))

  io.pc_bsrc := decode(U"010")
}
