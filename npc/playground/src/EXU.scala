package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

case class Exu2Lsu_data() extends Bundle {
  val pc            = UInt(32 bits)
  val pc_next       = UInt(32 bits)
  val rf_ctrl       = RfCtrl()    // 直通数据，在EXU中无作用
  val mem_ctrl      = MemCtrl()   // 直通数据，在EXU中无作用

  val rf_read_data_2 = UInt(32 bits)  // 从寄存器中读取的数据2,在EXU及后续模块中均有作用
  val alu_result    = UInt(32 bits)
}

case class ysyx_23060082_EXU() extends Component {
  val io = new Bundle {
    val input  = slave  Stream(Idu2Exu_data())
    val output = master Stream(Exu2Lsu_data()) 
  }

  val alu = ysyx_23060082_ALU()
  val banchCond = ysyx_23060082_BranchCond()

  alu.io.alu_ctr := io.input.ctrl.alu_si.alu_ctr
  alu.io.alu_in1 := io.input.ctrl.alu_si.alu_asrc.mux(// 为0时选择rs1，为1时选择PC。
    True  -> io.input.pc,
    False -> io.input.rf_read_data_1
  )
  alu.io.alu_in2 := io.input.ctrl.alu_si.alu_bsrc.mux(          // 为00时选择rs2，为01时选择imm，为10时选择常数4（用于跳转时计算返回地址PC+4）
    U"00" -> io.input.rf_read_data_2,
    U"01" -> io.input.imm,
    default -> U"32'h4"
  )

  banchCond.io.branch := io.input.ctrl.alu_si.branch
  banchCond.io.less   := alu.io.less
  banchCond.io.zero   := alu.io.zero

  val pc_data_a = Mux(banchCond.io.pc_asrc, io.input.imm, U"32'd4")
  val pc_data_b = Mux(banchCond.io.pc_bsrc, io.input.rf_read_data_1, io.input.pc)

  // ------------------ 用于握手的部分 ------------------ //
  val willValid = True
  io.output.valid := io.input.valid && willValid    // io.input.valid为数据有效信号，是寄存器信号
  io.input.ready := willValid
  // ------------------ 数据传输部分 ------------------ //
  io.output.pc          := io.input.pc
  io.output.pc_next     := pc_data_a + pc_data_b
  io.output.alu_result  := alu.io.alu_result
  io.output.rf_read_data_2 := io.input.rf_read_data_2
  io.output.rf_ctrl  := io.input.ctrl.rf_si      // 直通数据，在EXU中无作用
  io.output.mem_ctrl  := io.input.ctrl.mem_si    // 直通数据，在EXU中无作用

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

  io.pc_asrc := io.branch.mux(
    U"001" -> True,
    U"010" -> True,
    U"100" -> io.zero,
    U"101" -> ~io.zero,
    U"110" -> io.less,
    U"111" -> ~io.less,
    default -> False
  )            
  io.pc_bsrc := io.branch === U"010"
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

  // val adder = Adder()
  // adder.io.in1 := io.alu_in1
  // adder.io.in2 := io.alu_in2
  // adder.io.sub_add := sub_add
  // val result_adder = adder.io.result

  // ------------------ 加法器 ------------------ //
  val adder_dat_a = io.alu_in1
  val adder_dat_b = Mux(sub_add, ~io.alu_in2, io.alu_in2)
  val adder_cin = sub_add.asUInt
  val adder_result_33 = adder_dat_a.resize(33) + adder_dat_b.resize(33) + adder_cin.resize(33)    // 扩展为33位计算，便于查看溢出情况

  val result_adder = adder_result_33(31 downto 0)   // 计算结果
  val carry = adder_result_33(32)
  val zero = (result_adder === U"32'h0")
  val overflow = (adder_dat_a(31) === adder_dat_b(31)) && (result_adder(31) =/= adder_dat_a(31));
  // ------------------ 移位寄存器 ------------------ //
  val result_shift = io.alu_ctr(3 downto 2).mux(
    U"01"   -> (io.alu_in1 |>> io.alu_in2(4 downto 0)),         // 逻辑右移
    U"11"   -> (U(S(io.alu_in1) >> io.alu_in2(4 downto 0))),    // 算数右移
    default -> (io.alu_in1 |<< io.alu_in2(4 downto 0))          // 左移,使用的逻辑左移
  )
  // ------------------ --------------------------- //
  val less_0 = overflow ^ result_adder(31)
  val less_1 = carry ^ sub_add
  val less = Mux(io.alu_ctr(3), less_1, less_0)

  // val result_slt = less.asUInt.resize(32)
  val result_slt = U(less #* 32)        // 扩展为32位
  val result_lui = io.alu_in2
  val result_xor = io.alu_in1 ^ io.alu_in2;
  val result_or  = io.alu_in1 | io.alu_in2;
  val result_and = io.alu_in1 & io.alu_in2;

  io.less := less
  io.zero := zero
  io.alu_result := io.alu_ctr(2 downto 0).mux(
    U"3'b000" -> result_adder,
    U"3'b001" -> result_shift,
    U"3'b010" -> result_slt  ,
    U"3'b011" -> result_lui  ,
    U"3'b100" -> result_xor  ,
    U"3'b101" -> result_shift,
    U"3'b110" -> result_or   ,
    U"3'b111" -> result_and  ,
  )
}
