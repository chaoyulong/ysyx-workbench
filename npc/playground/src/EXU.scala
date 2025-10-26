package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

case class Exu2Lsu_data() extends Bundle {
  val pc      = UInt(32 bits)
  val pc_next = UInt(32 bits)
}

case class ysyx_23060082_EXU() extends Component {
  val io = new Bundle {
    val from_Idu  = slave Stream(Idu2Exu_data())
    // val to_Lsu = master Stream() 
  }

  val alu = ysyx_23060082_ALU()
  val branch_cond = ysyx_23060082_BranchCond()

  alu.io.alu_ctr := io.from_Idu.ctrl.alu_ctr
  alu.io.alu_in1 := Mux(io.from_Idu.ctrl.alu_asrc, io.from_Idu.rf_read_data_1, pc)
  alu.io.alu_in2 := io.from_Idu.ctrl.alu_bsrc.mux(
    U"00" -> io.from_Idu.rf_read_data_2,
    U"01" -> io.from_Idu.ctrl.imm,
    default -> U"32'h4"
  )
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
case class ysyx_23060082_BranchCond() extends Component {
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

/*              控制信号ALUctr的含义
    -----------------------------------------------
    ALUctr[3]   ALUctr[2:0]     ALU操作
    0           000             选择加法器输出，做加法
    1           000             选择加法器输出，做减法
    x           001             选择移位器输出，左移
    0           010             做减法，选择带符号小于置位结果输出, Less按带符号结果设置
    1           010             做减法，选择无符号小于置位结果输出, Less按无符号结果设置
    x           011             选择ALU输入B的结果直接输出
    x	          100             选择异或输出
    0           101             选择移位器输出，逻辑右移
    1           101             选择移位器输出，算术右移
    x           110             选择逻辑或输出
    x           111             选择逻辑与输出
*/
case class ysyx_23060082_ALU() extends Component {
  val io = new Bundle {
    val alu_in1 = in UInt(32 bits)
    val alu_in2 = in UInt(32 bits)  
    val alu_ctr = in UInt(4 bits) 

    val less = out Bool()
    val zero = out Bool()
    val alu_result = out UInt(32 bits) 
  }

  val sub_add = io.alu_ctr(1) | io.alu_ctr(3) // 加法器的加减,经过卡诺图化简
  val u_s = io.alu_ctr(3) // 符号/无符号
  val a_l = io.alu_ctr(3) // 算数/逻辑
  val l_r = io.alu_ctr(2) // 左/右

  val adder = Adder()
  adder.io.in1 := io.alu_in1
  adder.io.in2 := io.alu_in2
  adder.io.sub_add := sub_add.asUInt
  val carry = adder.io.carry
  val overflow = adder.io.overflow
  val result_adder = adder.io.result

  val shifter = Shifter() 
  shifter.io.din := io.alu_in1
  shifter.io.shamt := io.alu_in2(0 to 4)
  shifter.io.l_r := l_r
  shifter.io.a_l := a_l
  val result_shift = shifter.io.shift

  val less_0 = overflow ^ result_adder(31)
  val less_1 = carry ^ sub_add
  val less = Mux(u_s, less_1, less_0)

  val result_slt = less.asUInt.resize(32)
  val result_lui = io.alu_in2

  val result_xor = io.alu_in1 ^ io.alu_in2;
  val result_or  = io.alu_in1 | io.alu_in2;
  val result_and = io.alu_in1 & io.alu_in2;

  io.less := less
  io.zero := adder.io.zero
  switch(io.alu_ctr(0 to 2)){
    is(U"3'b000") {io.alu_result := result_adder}
    is(U"3'b001") {io.alu_result := result_shift}
    is(U"3'b010") {io.alu_result := result_slt}
    is(U"3'b011") {io.alu_result := result_lui}
    is(U"3'b100") {io.alu_result := result_xor}
    is(U"3'b101") {io.alu_result := result_shift}
    is(U"3'b110") {io.alu_result := result_or}
    is(U"3'b111") {io.alu_result := result_and}
  }
}

case class Adder() extends Component {
  val io = new Bundle {
    val in1 = in port UInt(32 bits)
    val in2 = in port UInt(32 bits)  
    val sub_add = in port UInt(1 bits)

    val carry = out port Bool()
    val zero = out port Bool()
    val overflow = out port Bool()
    val result = out port UInt(32 bits)
  }

  val dat_a = io.in1
  val dat_b = Mux(io.sub_add === U(1), ~io.in2, io.in2)
  val cin = io.sub_add
  val result_33 = dat_a.resize(33) + dat_b.resize(33) + cin.resize(33)

  io.carry := result_33(32)
  io.result := result_33(0 to 31)
  io.zero := io.result === U(0)
  io.overflow := (dat_a(31) === dat_b(31)) && (io.result(31) =/= dat_a(31));
}

case class Shifter() extends Component {
  val io = new Bundle {
    val din = in port UInt(32 bits)
    val shamt = in port UInt(5 bits)
    val l_r = in port Bool()
    val a_l = in port Bool()
    val shift = out port UInt(32 bits)
  }

  switch(io.a_l ## io.l_r){
    is(B"01") {io.shift := io.din |>> io.shamt}  // 逻辑右移
    is(B"11") {io.shift := U(S(io.din) >> io.shamt)}  // 算数右移
    default   {io.shift := io.din |<< io.shamt}   // 左移,使用的逻辑左移
  }
}