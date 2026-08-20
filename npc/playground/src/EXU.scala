package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

case class Exu2Lsu_data() extends Bundle {
  val pc          = UInt(32 bits)
  val pc_next     = UInt(32 bits)
  val rf_ctrl     = RfCtrl()        // 直通数据，在EXU中无作用
  val mem_ctrl    = MemCtrl()       // 直通数据，在EXU中无作用
  val csr_ctrl    = CsrCtrl()       // 直通数据，在EXU中无作用 

  val imm         = UInt(12 bits)   // csr(位于LSU)模块中用于寄存器寻址
  val rfReadData1 = UInt(32 bits)   // 从寄存器中读取的数据1,在EXU及csr(位于LSU)模块中均有作用
  val rfReadData2 = UInt(32 bits)   // 从寄存器中读取的数据2,在EXU及后续模块中均有作用
  val aluResult   = UInt(32 bits)
}

case class ysyx_23060082_EXU() extends Component {
  val io = new Bundle {
    val input  = slave  Flow  (Idu2Exu_data())
    val output = master Stream(Exu2Lsu_data()) 
  }

  val alu = ysyx_23060082_ALU()
  val banchCond = ysyx_23060082_BranchCond()

  alu.io.aluCtr := io.input.ctrl.alu_si.alu_ctr
  alu.io.aluIn1 := io.input.ctrl.alu_si.alu_asrc.mux(// 为0时选择rs1，为1时选择PC。
    True  -> io.input.pc,
    False -> io.input.rfReadData1
  )
  alu.io.aluIn2 := io.input.ctrl.alu_si.alu_bsrc.mux(          // 为00时选择rs2，为01时选择imm，为10时选择常数4（用于跳转时计算返回地址PC+4）
    U"00" -> io.input.rfReadData2,
    U"01" -> io.input.imm,
    default -> U"32'h4"
  )

  banchCond.io.branch := io.input.ctrl.alu_si.branch
  banchCond.io.less   := alu.io.less
  banchCond.io.zero   := alu.io.zero

  val pcDataA = Mux(banchCond.io.pc_asrc, io.input.imm, U"32'd4")
  val pcDataB = Mux(banchCond.io.pc_bsrc, io.input.rfReadData1, io.input.pc)
  val pcDataTmp = pcDataA + pcDataB
  // ------------------ 用于握手的部分 ----------------- //
  val willValid = True
  io.output.valid := io.input.valid && willValid    // io.input.valid为数据有效信号，是寄存器信号
  // ------------------ 数据传输部分 ------------------ //
  io.output.pc          := io.input.pc
  
  io.output.pc_next     := io.input.ctrl.alu_si.branch.mux(
    U"010"  -> (pcDataTmp(31 downto 1) ## B"1'b0").asUInt,
    default -> pcDataTmp
  )
  io.output.aluResult   := alu.io.aluResult
  io.output.imm         := io.input.imm(11 downto 0)
  io.output.rfReadData1 := io.input.rfReadData1
  io.output.rfReadData2 := io.input.rfReadData2
  io.output.rf_ctrl     := io.input.ctrl.rf_si      // 直通数据，在EXU中无作用
  io.output.mem_ctrl    := io.input.ctrl.mem_si     // 直通数据，在EXU中无作用
  io.output.csr_ctrl    := io.input.ctrl.csr_si     // 直通数据，在EXU中无作用
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
  io.pc_bsrc := (io.branch === U"010")
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
    val aluIn1 = in UInt(32 bits)
    val aluIn2 = in UInt(32 bits)  
    val aluCtr = in UInt(4 bits) 

    val less = out Bool()
    val zero = out Bool()
    val aluResult = out UInt(32 bits) 
  }

  val subORadd = io.aluCtr(1) | io.aluCtr(3) // 加法器的加减,经过卡诺图化简
  // ------------------ 加法器 ------------------ //
  val adderDataA       = io.aluIn1
  val adderDataB       = Mux(subORadd, ~io.aluIn2, io.aluIn2)
  val adderCin         = subORadd.asUInt                  // 减法的话相当于转为补码，取反加1
  val resultAdder33Bit = adderDataA.resize(33) + adderDataB.resize(33) + adderCin.resize(33)    // 扩展为33位计算，便于查看溢出情况

  val resultAdder      = resultAdder33Bit(31 downto 0)    // 计算结果
  val carryFlag        = resultAdder33Bit(32)             // 进位
  val zeroFlag         = (resultAdder === U"32'h0")       // 判0
  val overflowFlag     = (adderDataA(31) === adderDataB(31)) && (resultAdder(31) =/= adderDataA(31))  // 溢出
  // --------------------- 移位寄存器 --------------------- //
  val resultShift = io.aluCtr(3 downto 2).mux(
    U"01"   -> (io.aluIn1 |>> io.aluIn2(4 downto 0)),     // 逻辑右移
    U"11"   -> U(S(io.aluIn1) >> io.aluIn2(4 downto 0)),  // 算数右移
    default -> (io.aluIn1 |<< io.aluIn2(4 downto 0))      // 左移,使用的逻辑左移
  )
  // -------------------- 小于比较判断 -------------------- //
  val lessFlag0 = overflowFlag ^ resultAdder(31)          // 有符号小于
  val lessFlag1 = carryFlag ^ subORadd                    // 无符号小于
  val lessFlag = Mux(io.aluCtr(3), lessFlag1, lessFlag0)
  // ---------------------- 输出结果 --------------------- //
  val resultSlt = U(lessFlag).resize(32)                  // SLT/SLTU 结果: 0或1, 零扩展
  val resultDir = io.aluIn2                               // 直接输出aluIn2
  val resultXor = io.aluIn1 ^ io.aluIn2
  val resultOr  = io.aluIn1 | io.aluIn2
  val resultAnd = io.aluIn1 & io.aluIn2

  io.less := lessFlag
  io.zero := zeroFlag
  io.aluResult := io.aluCtr(2 downto 0).mux(
    U"3'b000" -> resultAdder,
    U"3'b001" -> resultShift,
    U"3'b010" -> resultSlt  ,
    U"3'b011" -> resultDir  ,
    U"3'b100" -> resultXor  ,
    U"3'b101" -> resultShift,
    U"3'b110" -> resultOr   ,
    U"3'b111" -> resultAnd  
  )
}
