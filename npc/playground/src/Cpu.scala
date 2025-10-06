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
// case class ALU() extends Component {
//   val io = new Bundle {
//     val rs1 = in port UInt(32 bits)
//     val rs2 = in port UInt(32 bits)  
//     val alu_ctr = in port Bits(4 bits) 

//     val less = out port Bool
//     val zero = out port Bool
//     val alu_out = out port UInt(32bits) 
//   }


// }

case class Shifter() extends Component {
  val io = new Bundle {
    val din = in port Int(32 bits)
    val shamt = in port UInt(5 bits)
    val l_r = in port Bool
    val a_l = in port Bool
    val shift = out port UInt(32 bits)
  }

  switch(io.a_l ## io.l_r){
    is(B"01") {io.shift := io.din |>> io.shamt}  // 逻辑右移,
    is(B"11") {io.shift := io.din >> io.shamt}  // 算数右移
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