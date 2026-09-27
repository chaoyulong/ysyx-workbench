package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

case class Exu2Lsu_data(config: CpuConfig = CpuConfig()) extends Bundle {
  val pc          = UInt(32 bits) 
  val instr       = if (config.enableSimDebug) UInt(32 bits) else null
  val pcNextTrace = if (config.enableSimDebug) UInt(32 bits) else null
  
  val rfCtrl      = RfCtrl()        // 直通数据，在EXU中无作用
  val memCtrl     = MemCtrl()       // 直通数据，在EXU中无作用
  val csrCtrl     = CsrCtrl()       // 直通数据，在EXU中无作用 

  val csrAddr     = UInt(12 bits)   // 仅用于传给LSU的CSR寄存器寻址(与 EXU 的 imm 区分)
  val rfReadData  = UInt(32 bits)   // rs1只用于CSR类指令，rs2只用于store，一条指令不可能同时是两者
  val aluResult   = UInt(32 bits)
  val fenceI      = Bool()          // fence.i(直通, 通知 icache 失效)
}

case class ysyx_23060082_EXU(config: CpuConfig = CpuConfig()) extends Component {
  val io = new Bundle {
    val input    = slave  Flow  (Idu2Exu_data(config))
    val output   = master Stream(Exu2Lsu_data(config))
    val forward  = out(forwardData())
    val redirect = master Flow(RedirectReq())
  }

  val alu = ysyx_23060082_ALU()
  val banchCond = ysyx_23060082_BranchCond()

  alu.io.aluCtr := io.input.ctrl.aluCtrl.aluCtr
  alu.io.aluIn1 := io.input.ctrl.aluCtrl.aluAsrc.mux(// 为0时选择rs1，为1时选择PC。
    True  -> io.input.pc,
    False -> io.input.rfReadData1
  )
  alu.io.aluIn2 := io.input.ctrl.aluCtrl.aluBsrc.mux(          // 为00时选择rs2，为01时选择imm，为10时选择常数4（用于跳转时计算返回地址PC+4）
    U"00" -> io.input.rfReadData2,
    U"01" -> io.input.imm,
    default -> U"32'h4"
  )

  // ================================ 跳转指令 ================================ //
  banchCond.io.branch := io.input.ctrl.aluCtrl.branch
  banchCond.io.less   := alu.io.less
  banchCond.io.zero   := alu.io.zero

  // val pcDataA   = Mux(banchCond.io.pcAsrc, io.input.imm, U"32'd4")
  // val pcDataB   = Mux(banchCond.io.pcBsrc, io.input.rfReadData1, io.input.pc)
  // val pcDataTmp = pcDataA + pcDataB
  // 由于pcAsrc到达较晚，所以选择去掉，并且pc+4这个pcnext不需要得出，因为默认运行的就是这个
  val pcDataB   = Mux(banchCond.io.pcBsrc, io.input.rfReadData1, io.input.pc)      
  val pcDataTmp = io.input.imm + pcDataB
  val pcNextBit0= !banchCond.io.pcBsrc && pcDataTmp(0)          // jalr指令规定要将最后一位清零
  val pcNext    = (pcDataTmp(31 downto 1) ## pcNextBit0).asUInt

  // ================================ 异常检测 ================================ //
  val pcMisaligned = banchCond.io.pcAsrc && (pcNext(1) || pcNext(0))  // 跳转/分支目标未对齐

  io.output.csrCtrl.csrCmd    := io.input.ctrl.csrCtrl.csrCmd
  io.output.csrCtrl.ecall     := io.input.ctrl.csrCtrl.ecall
  io.output.csrCtrl.trapExit  := io.input.ctrl.csrCtrl.trapExit

  // 上级优先
  io.output.csrCtrl.trapEnter := io.input.ctrl.csrCtrl.trapEnter || pcMisaligned
  io.output.csrCtrl.excCause  := Mux(io.input.ctrl.csrCtrl.trapEnter, io.input.ctrl.csrCtrl.excCause, U(0, 4 bits)) // 0 = 跳转目标未对齐
               
  // ================================ 重定向 ================================ //
  io.redirect.valid  := io.input.valid && banchCond.io.pcAsrc && !pcMisaligned   // 数据有效并且是跳转指令(pcAsrc,pcBsrc有一个为1就是跳转指令，而pcBsrc为1时，pcAsrc也为1)
  io.redirect.pcNext := pcNext
  io.redirect.fenceI := False                                   // exu中执行的话，如果上一级lsu在写入，那么此时lsu写入的数据就不是icache可见的了，所以要延迟到lsu阶段再执行

  // ================================ 用于握手的部分 ================================ //
  val willValid = True
  io.output.valid := io.input.valid && willValid    // io.input.valid为数据有效信号，是寄存器信号
  
  // ================================ 数据传输部分 ================================ //
  val useRs1 = io.input.ctrl.csrCtrl.trapEnter || (io.input.ctrl.csrCtrl.csrCmd =/= 0)  // rs1: CSR，rs2: store
  io.output.rfReadData := Mux(useRs1, io.input.rfReadData1, io.input.rfReadData2)
  io.output.pc         := io.input.pc
  io.output.aluResult  := alu.io.aluResult
  io.output.csrAddr    := io.input.imm(11 downto 0)
  io.output.fenceI     := io.input.ctrl.fenceI
  io.output.rfCtrl     := io.input.ctrl.rfCtrl      // 直通数据，在EXU中无作用
  io.output.memCtrl    := io.input.ctrl.memCtrl     // 直通数据，在EXU中无作用
  // ================================ 数据前递 ================================ //
  val getDataInLsu      = io.input.ctrl.rfCtrl.mem2reg || io.input.ctrl.rfCtrl.csr2reg  // 要在lsu中才会得到的数据
  io.forward.state     := Mux(!io.input.valid || !io.input.ctrl.rfCtrl.regWr, FwdState.NoWriter,  // 还没有有效数据，或者不是写寄存器的信号时
                          Mux(getDataInLsu, FwdState.DataPendingLater, FwdState.DataReady)) // 如果要在lsu中才能得到数据，就WaitLater，如果在本级就能得到数据，Ready
  io.forward.writeAddr := io.input.ctrl.rfCtrl.rfWriteAddr
  io.forward.writeData := alu.io.aluResult

  // ================================ 仿真专用 ================================ //
  if (config.enableSimDebug) {
    val pcDataA   = Mux(banchCond.io.pcAsrc, io.input.imm, U"32'd4")
    io.output.pcNextTrace = pcDataA + pcDataB
    io.output.instr   := io.input.instr
  }
  // EXU 运算周期统计(isCalc && willValid; 当前单周期, 每条计算指令占1拍)
  if (config.enableSimDebug) {
    val perf = PerfReg()
    perf.io.valid := True
    perf.io.req   := B"4'b0"
    perf.io.rsp   := B"4'b0"
    perf.io.evt   := B"8'b0"
    perf.io.evt(0) := io.input.valid && io.input.isCalc && willValid  // EXU 运算周期
    perf.io.evt(1) := io.input.valid && io.input.isCalc               // 计算类指令数
  }
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

    val pcAsrc= out Bool()
    val pcBsrc= out Bool()
  }

  io.pcAsrc := io.branch.mux(
    U"001" -> True,
    U"010" -> True,
    U"100" -> io.zero,
    U"101" -> ~io.zero,
    U"110" -> io.less,
    U"111" -> ~io.less,
    default -> False
  )            
  io.pcBsrc := (io.branch === U"010")
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
  // ================================ 加法器 ================================ //
  val adderDataA       = io.aluIn1
  val adderDataB       = Mux(subORadd, ~io.aluIn2, io.aluIn2)
  val adderCin         = subORadd.asUInt                  // 减法的话相当于转为补码，取反加1
  val resultAdder33Bit = adderDataA.resize(33) + adderDataB.resize(33) + adderCin.resize(33)    // 扩展为33位计算，便于查看溢出情况

  val resultAdder      = resultAdder33Bit(31 downto 0)    // 计算结果
  val carryFlag        = resultAdder33Bit(32)             // 进位
  // val zeroFlag         = (resultAdder === U"32'h0")       // 判0
  val zeroFlag         = (io.aluIn1 === io.aluIn2)  // 跳过alu，缩短路径
  val overflowFlag     = (adderDataA(31) === adderDataB(31)) && (resultAdder(31) =/= adderDataA(31))  // 溢出
  // ================================ 移位寄存器 ================================ //
  val resultShift = io.aluCtr(3 downto 2).mux(            // 直接移位操作与自己写桶形移位器没有区别
    U"01"   -> (io.aluIn1 |>> io.aluIn2(4 downto 0)),     // 逻辑右移
    U"11"   -> U(S(io.aluIn1) >> io.aluIn2(4 downto 0)),  // 算数右移
    default -> (io.aluIn1 |<< io.aluIn2(4 downto 0))      // 左移,使用的逻辑左移
  )
  // ================================ 小于比较判断 ================================ //
  val lessFlag0 = overflowFlag ^ resultAdder(31)          // 有符号小于
  val lessFlag1 = carryFlag ^ subORadd                    // 无符号小于
  val lessFlag = Mux(io.aluCtr(3), lessFlag1, lessFlag0)
  // ================================ 输出结果 ================================ //
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

// case class ysyx_23060082_BarrelShifter() extends Component {
//   val io = new Bundle {
//     val din    = in UInt(32 bits)
//     val shamt  = in UInt(5 bits)
//     val bsCtr  = in UInt(2 bits)   // 01=逻辑右移, 11=算术右移, 00/10=左移
//     val result = out UInt(32 bits)
//   }

//   val isRight = io.bsCtr(0)               // 01/11: 右移; 00/10: 左移
//   val isArith = io.bsCtr === U"2'b11"     // 11: 算术右移

//   // 左移 = 反转 -> 右移 -> 反转, 这样三种移位共用一套右移器
//   val xb   = Mux(isRight, io.din, io.din.reversed).asBits
//   val fill = Mux(isArith, xb(31), False)  // 算术右移填符号位, 逻辑右移填 0

//   // 5 级右移: 分别移 1/2/4/8/16 位
//   val s0 = Mux(io.shamt(0), (fill #* 1)  ## xb(31 downto 1 ), xb)
//   val s1 = Mux(io.shamt(1), (fill #* 2)  ## s0(31 downto 2 ), s0)
//   val s2 = Mux(io.shamt(2), (fill #* 4)  ## s1(31 downto 4 ), s1)
//   val s3 = Mux(io.shamt(3), (fill #* 8)  ## s2(31 downto 8 ), s2)
//   val s4 = Mux(io.shamt(4), (fill #* 16) ## s3(31 downto 16), s3)

//   io.result := Mux(isRight, s4, s4.reversed).asUInt
// }
