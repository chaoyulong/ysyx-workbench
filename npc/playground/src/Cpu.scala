package playground

import spinal.core._

// case class RegFile() extends Component {
//   val io = new Bundle {
//     val addr_a = in port UInt(5 bits)
//     val addr_b = in port UInt(5 bits)
//     val addr_w = in port UInt(5 bits)
//     val wdata  = in port UInt(32 bits)
//     val reg_wr = in port Bool

//     val rs1 = out port UInt(32 bits)
//     val rs2 = out port UInt(32 bits)
//   }

//   val rf = Vec(Reg(UInt(32 bits)), 16)    // riscv32e,有16个通用寄存器
//   when(io.reg_wr){
//     rf(io.addr_w(0 to 3)) := io.wdata
//   }
//   rf(U"4'h0") := U"32'h0"
  
//   io.rs1 := rf(io.addr_a(0 to 3))
//   io.rs2 := rf(io.addr_b(0 to 3)) 
// }

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
case class ALU() extends Component {
  val io = new Bundle {
    val rs1 = in port UInt(32 bits)
    val rs2 = in port UInt(32 bits)  
    val alu_ctr = in port UInt(4 bits) 

    val less = out port Bool
    val zero = out port Bool
    val alu_out = out port UInt(32 bits) 
  }

  val sub_add = io.alu_ctr(1) || io.alu_ctr(3) // 加法器的加减,经过卡诺图化简
  val u_s = io.alu_ctr(3) // 符号/无符号
  val a_l = io.alu_ctr(3) // 算数/逻辑
  val l_r = io.alu_ctr(2) // 左/右

  val adder = Adder()
  adder.io.rs1 := io.rs1
  adder.io.rs2 := io.rs2
  adder.io.sub_add := sub_add
  val carry = adder.io.carry
  val overflow = adder.io.overflow
  val result_adder = adder.io.result

  val shifter = Shifter() 
  shifter.io.din := io.rs1
  shifter.io.shamt := io.rs2(0 to 4)
  shifter.io.l_r := l_r
  shifter.io.a_l := a_l
  val result_shift = shifter.io.shift


  val less_0 = overflow ^ result_adder(31)
  val less_1 = carry ^ sub_add
  val less = Mux(u_s, less_1, less_0)

  val result_slt = less.resize(32)
  val result_outb = io.rs1

  val result_xor = io.rs1 ^ io.rs2;
  val result_or  = io.rs1 | io.rs2;
  val result_and = io.rs1 & io.rs2;

  io.less := less
  io.zero := adder.io.zero
  switch(io.alu_ctr(0 to 2)){
    is(U"3'b000") {io.alu_out := result_adder}
    is(U"3'b001") {io.alu_out := result_shift}
    is(U"3'b010") {io.alu_out := result_slt}
    is(U"3'b011") {io.alu_out := result_outb}
    is(U"3'b100") {io.alu_out := result_xor}
    is(U"3'b101") {io.alu_out := result_shift}
    is(U"3'b110") {io.alu_out := result_or}
    is(U"3'b111") {io.alu_out := result_and}
  }
}

case class Adder() extends Component {
  val io = new Bundle {
    val rs1 = in port UInt(32 bits)
    val rs2 = in port UInt(32 bits)  
    val sub_add = in port UInt(1 bits)

    val carry = out port Bool
    val zero = out port Bool
    val overflow = out port Bool
    val result = out port UInt(32 bits)
  }

  val dat_a = io.rs1
  val dat_b = Mux(io.sub_add === U(1), ~io.rs2, io.rs2)
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
    val l_r = in port Bool
    val a_l = in port Bool
    val shift = out port UInt(32 bits)
  }

  switch(io.a_l ## io.l_r){
    is(B"01") {io.shift := io.din |>> io.shamt}  // 逻辑右移
    is(B"11") {io.shift := U(S(io.din) >> io.shamt)}  // 算数右移
    default   {io.shift := io.din |<< io.shamt}   // 左移,使用的逻辑左移
  }
}

// case class Decoder() extends Component {
//   val io = new Bundle {
//     val instr    = in port UInt(32 bits)
//     val reg_wr   = out port Bool
//     val alu_asrc = out port Bool
//     val alu_bsrc = out port Bits(2 bits)
//     val alu_ctr  = out port Bits(4 bits)

//   }
// }