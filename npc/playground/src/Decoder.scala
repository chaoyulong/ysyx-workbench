package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

case class ysyx_23060082_Decoder(config: CpuConfig = CpuConfig()) extends Component {
  val io = new Bundle {
    val instr       = in UInt(32 bits)
    val ifuTrapEnter= in Bool()          // 有异常
    val ifuExcCause = in UInt(4 bits)    // 异常号

    val ctrl        = out(CtrlSignals())
    val imm         = out UInt(32 bits)

    val useRf1      = out Bool()    // 读寄存器有效
    val useRf2      = out Bool()
    // 指令类别标志(性能统计用): 仅仿真(enableSimDebug)生成, STA 时为 null(无端口)
    val isCalc      = if (config.enableSimDebug) out Bool() else null   // 计算类(ALU/立即数)
    val isMem       = if (config.enableSimDebug) out Bool() else null   // 访存(load/store)
    val isBranch    = if (config.enableSimDebug) out Bool() else null   // 分支
    val isJump      = if (config.enableSimDebug) out Bool() else null   // 跳转
    val isCsr       = if (config.enableSimDebug) out Bool() else null   // CSR
    val isSys       = if (config.enableSimDebug) out Bool() else null   // 系统(ecall/ebreak/mret/fence)
  }

  val instr = io.instr
  val i     = io.instr.asBits

  // val op   = instr( 6 downto  0)     
  val func3= instr(14 downto 12)
  // val func7= instr(31 downto 25) // 用不到

  
// ================================ 指令匹配 ================================ //    
  // val i_add    = i === M"0000000----------000-----0110011"    // typeR
  // val i_sub    = i === M"0100000----------000-----0110011"
  // val i_sll    = i === M"0000000----------001-----0110011"
  // val i_slt    = i === M"0000000----------010-----0110011"
  // val i_sltu   = i === M"0000000----------011-----0110011"
  // val i_xor    = i === M"0000000----------100-----0110011"
  // val i_srl    = i === M"0000000----------101-----0110011"
  // val i_sra    = i === M"0100000----------101-----0110011"
  // val i_or     = i === M"0000000----------110-----0110011"
  // val i_and    = i === M"0000000----------111-----0110011"

  // val i_addi   = i === M"-----------------000-----0010011"    // typeI
  // val i_slli   = i === M"0000000----------001-----0010011"
  // val i_slti   = i === M"-----------------010-----0010011"
  // val i_sltiu  = i === M"-----------------011-----0010011"
  // val i_xori   = i === M"-----------------100-----0010011"
  // val i_srli   = i === M"0000000----------101-----0010011"
  // val i_srai   = i === M"0100000----------101-----0010011"
  // val i_ori    = i === M"-----------------110-----0010011"
  // val i_andi   = i === M"-----------------111-----0010011"
  
  // val i_lb     = i === M"-----------------000-----0000011"
  // val i_lh     = i === M"-----------------001-----0000011"
  // val i_lw     = i === M"-----------------010-----0000011"
  // val i_lbu    = i === M"-----------------100-----0000011"
  // val i_lhu    = i === M"-----------------101-----0000011"
  // val i_sb     = i === M"-----------------000-----0100011"    // typeS
  // val i_sh     = i === M"-----------------001-----0100011"
  // val i_sw     = i === M"-----------------010-----0100011"

  // val i_beq    = i === M"-----------------000-----1100011"    // typeB
  // val i_bne    = i === M"-----------------001-----1100011"
  // val i_blt    = i === M"-----------------100-----1100011"
  // val i_bge    = i === M"-----------------101-----1100011"
  // val i_bltu   = i === M"-----------------110-----1100011"
  // val i_bgeu   = i === M"-----------------111-----1100011"
  // val i_jalr   = i === M"-----------------000-----1100111"    // typeI
  // val i_jal    = i === M"-------------------------1101111"    // typeJ
  // val i_lui    = i === M"-------------------------0110111"    // typeU
  // val i_auipc  = i === M"-------------------------0010111"

  // // val i_mul    = i === M"0000001----------000-----0110011" // typeR
  // // val i_mulh   = i === M"0000001----------001-----0110011"
  // // val i_mulhsu = i === M"0000001----------010-----0110011"
  // // val i_mulhu  = i === M"0000001----------011-----0110011"

  // // val i_div    = i === M"0000001----------100-----0110011"
  // // val i_divu   = i === M"0000001----------101-----0110011"
  // // val i_rem    = i === M"0000001----------110-----0110011"
  // // val i_remu   = i === M"0000001----------111-----0110011"

  // val i_csrrw  = i === M"-----------------001-----1110011"  // 系统指令，暂且命名为typeN
  // val i_csrrs  = i === M"-----------------010-----1110011"

  // val i_ecall  = i === M"00000000000000000000000001110011"
  // val i_ebreak = i === M"00000000000100000000000001110011"
  // val i_mret   = i === M"00110000001000000000000001110011"

  // val i_fence_i= i === M"-----------------001-----0001111"

// ================================ 指令匹配(按 op / func3 / func7 及指定位域) ================================ //
  // ★ 与上面被注释掉的 32 位 M"..." 掩码匹配【完全等价】:
  //   原模式里每一个非 "-" 的位, 在下面都被显式匹配到 ——
  //     op    = instr[ 6: 0]
  //     func3 = instr[14:12]
  //     func7 = instr[31:25]
  //   ecall/ebreak/mret 原模式把 32 位写满, 所以这里仍然整字比较。
  //   好处: 由"32 位掩码与 + 整字比较"变成"窄字段比较 + 字段复用", 门数与扇出都小得多。
  val op    = instr( 6 downto  0)
  val func7 = instr(31 downto 25)

  // ---- opcode ----
  val isOpR      = op === U"7'b0110011"   // OP
  val isOpI      = op === U"7'b0010011"   // OP-IMM
  val isOpLoad   = op === U"7'b0000011"   // LOAD
  val isOpStore  = op === U"7'b0100011"   // STORE
  val isOpBranch = op === U"7'b1100011"   // BRANCH
  val isOpJalr   = op === U"7'b1100111"   // JALR
  val isOpJal    = op === U"7'b1101111"   // JAL
  val isOpLui    = op === U"7'b0110111"   // LUI
  val isOpAuipc  = op === U"7'b0010111"   // AUIPC
  val isOpSys    = op === U"7'b1110011"   // SYSTEM
  val isOpFence  = op === U"7'b0001111"   // MISC-MEM

  // ---- func7(原模式里显式写出的 0000000 / 0100000) ----
  val isF7_0 = func7 === U"7'b0000000"
  val isF7_1 = func7 === U"7'b0100000"

  // ---- func3 ----
  val isF3_0 = func3 === U"3'b000"
  val isF3_1 = func3 === U"3'b001"
  val isF3_2 = func3 === U"3'b010"
  val isF3_3 = func3 === U"3'b011"
  val isF3_4 = func3 === U"3'b100"
  val isF3_5 = func3 === U"3'b101"
  val isF3_6 = func3 === U"3'b110"
  val isF3_7 = func3 === U"3'b111"

  // ---- R 型: op + func3 + func7 全部匹配 ----
  val i_add  = isOpR && isF3_0 && isF7_0
  val i_sub  = isOpR && isF3_0 && isF7_1
  val i_sll  = isOpR && isF3_1 && isF7_0
  val i_slt  = isOpR && isF3_2 && isF7_0
  val i_sltu = isOpR && isF3_3 && isF7_0
  val i_xor  = isOpR && isF3_4 && isF7_0
  val i_srl  = isOpR && isF3_5 && isF7_0
  val i_sra  = isOpR && isF3_5 && isF7_1
  val i_or   = isOpR && isF3_6 && isF7_0
  val i_and  = isOpR && isF3_7 && isF7_0

  // ---- I 型算术: 原模式里 addi/slti/... 只写了 func3(立即数高位是数据, 不限),
  //      移位类额外写了 func7(区分 slli/srli/srai) ----
  val i_addi  = isOpI && isF3_0
  val i_slli  = isOpI && isF3_1 && isF7_0
  val i_slti  = isOpI && isF3_2
  val i_sltiu = isOpI && isF3_3
  val i_xori  = isOpI && isF3_4
  val i_srli  = isOpI && isF3_5 && isF7_0
  val i_srai  = isOpI && isF3_5 && isF7_1
  val i_ori   = isOpI && isF3_6
  val i_andi  = isOpI && isF3_7

  // ---- 载入 / 存储 ----
  val i_lb   = isOpLoad  && isF3_0
  val i_lh   = isOpLoad  && isF3_1
  val i_lw   = isOpLoad  && isF3_2
  val i_lbu  = isOpLoad  && isF3_4
  val i_lhu  = isOpLoad  && isF3_5
  val i_sb   = isOpStore && isF3_0
  val i_sh   = isOpStore && isF3_1
  val i_sw   = isOpStore && isF3_2

  // ---- 分支 ----
  val i_beq  = isOpBranch && isF3_0
  val i_bne  = isOpBranch && isF3_1
  val i_blt  = isOpBranch && isF3_4
  val i_bge  = isOpBranch && isF3_5
  val i_bltu = isOpBranch && isF3_6
  val i_bgeu = isOpBranch && isF3_7

  // ---- 跳转 / 高位立即数(原模式只约束 opcode) ----
  val i_jalr  = isOpJalr && isF3_0
  val i_jal   = isOpJal
  val i_lui   = isOpLui
  val i_auipc = isOpAuipc

  // ---- CSR ----
  val i_csrrw = isOpSys && isF3_1
  val i_csrrs = isOpSys && isF3_2

  // ---- 系统指令(原模式 32 位写满 ⇒ 整字比较) ----
  val i_ecall  = instr === U"32'h00000073"
  val i_ebreak = instr === U"32'h00100073"
  val i_mret   = instr === U"32'h30200073"

  // ---- fence.i ----
  val i_fence_i = isOpFence && isF3_1

  // 判断非法指令
  val isLegal = i_add | i_sub | i_sll | i_slt | i_sltu| i_xor | i_srl | i_sra | i_or  | i_and | 
                i_addi | i_slli | i_slti | i_sltiu | i_xori | i_srli | i_srai | i_ori | i_andi |
                i_lb | i_lh | i_lw | i_lbu | i_lhu | i_sb | i_sh | i_sw |
                i_beq | i_bne | i_blt | i_bge | i_bltu | i_bgeu | i_jalr | i_jal  | i_lui | i_auipc |
                i_csrrw | i_csrrs | i_ecall | i_ebreak | i_mret | i_fence_i
  val i_illegal = !isLegal

// ================================ 指令类型 ================================ //
  val typeU = i_lui | i_auipc
  val typeJ = i_jal
  val typeI = i_addi| i_slli | i_slti | i_sltiu | i_xori | i_srli | i_srai | i_ori | i_andi |
              i_jalr| i_lb  | i_lh | i_lw | i_lbu | i_lhu |
              i_csrrw | i_csrrs
  val typeS = i_sb | i_sh | i_sw
  val typeB = i_beq | i_bne | i_blt | i_bge | i_bltu | i_bgeu
  val typeR = i_add | i_sub | i_sll | i_slt | i_sltu | i_xor | i_srl | i_sra | i_or | i_and

  // val typeN = (op(6 downto 2) === U"11100" && func3 === U"000") || op(6 downto 2) === U"00011"   // 系统指令
  // ================================ 立即数生成 ================================ //
  val immU = instr(31 downto 12) ## B"12'b0"
  val immJ = (instr(31) #* 12) ## instr(19 downto 12) ## instr(20) ## instr(30 downto 21) ## B"0"
  val immI = (instr(31) #* 20) ## instr(31 downto 20)
  val immS = (instr(31) #* 20) ## instr(31 downto 25) ## instr(11 downto 7)
  val immB = (instr(31) #* 20) ## instr(7) ## instr(30 downto 25) ## instr(11 downto 8) ## B"0"

  io.imm := PriorityMux(Seq(
              typeU -> immU,
              typeJ -> immJ,
              typeI -> immI,
              typeS -> immS,
              typeB -> immB,
              True -> B"32'h0")).asUInt
// ================================ 控制信号生成 ================================ //
  val csrWb = i_csrrw | i_csrrs

  io.useRf1 := typeS | typeR | typeB | typeI | i_ecall
  io.useRf2 := typeS | typeR | typeB

  io.ctrl.rfCtrl.regWr       := typeU | typeJ | typeI | typeR
  io.ctrl.rfCtrl.rfWriteAddr := instr(11 downto 7)            // 为了写起来简洁，写寄存器地址在此赋值
  io.ctrl.rfCtrl.mem2reg     := i_lb | i_lh | i_lw | i_lbu | i_lhu
  io.ctrl.rfCtrl.csr2reg     := csrWb

  io.ctrl.aluCtrl.aluAsrc := i_auipc | i_jal | i_jalr         // 0：选通rdata1，1：选通PC。
  io.ctrl.aluCtrl.aluBsrc := Mux(typeR | typeB, U"00",        // 选通rdata2
                             Mux(i_jal  | i_jalr, U"10",      // 选通4，用于跳转
                             U"01" ))                         // 选通imm
  io.ctrl.aluCtrl.aluCtr  := PriorityMux(Seq(
                             (i_and | i_andi) -> U"0111",     // 选择逻辑与输出
                             (i_or  | i_ori ) -> U"0110",     // 选择逻辑或输出
                             (i_xor | i_xori) -> U"0100",     // 选择异或输出
                             (i_sll | i_slli) -> U"0001",     // 选择移位器输出，左移
                             (i_srl | i_srli) -> U"0101",     // 选择移位器输出，逻辑右移
                             (i_sra | i_srai) -> U"1101",     // 选择移位器输出，算术右移
                             (i_sub)          -> U"1000",     // 选择加法器输出，做减法
                             // (i_add | i_addi) -> U"0000",  // 选择加法器输出，做加法
                             (i_lui)          -> U"0011",     // 选择ALU输入B的结果直接输出
                             (i_slt | i_slti | i_beq | i_bne | i_blt | i_bge) -> U"0010",   // 做减法，选择带符号小于置位结果输出, Less按带符号结果设置
                             (i_sltu| i_sltiu| i_bltu| i_bgeu)                -> U"1010",   // 做减法，选择无符号小于置位结果输出, Less按无符号结果设置
                             True             -> U"0000"))             
  io.ctrl.aluCtrl.branch  := PriorityMux(Seq(
                             i_jal            -> U"001",      // 无条件跳转PC目标
                             i_jalr           -> U"010",      // 无条件跳转寄存器目标
                             i_beq            -> U"100",      // 条件分支，等于
                             i_bne            -> U"101",      // 条件分支，不等于
                             (i_blt | i_bltu) -> U"110",      // 条件分支，小于
                             (i_bge | i_bgeu) -> U"111",      // 条件分支，大于等于
                             True             -> U"000"))

  io.ctrl.memCtrl.memWr   := i_sb | i_sh | i_sw
  io.ctrl.memCtrl.memOp   := func3 

  io.ctrl.fenceI          := i_fence_i                         // fence.i: 指令内存屏障
   
  // ================================ csr寄存器 ================================ //
  // 操作：
  // csrrw:    R(rd) = CSR[imm]; CSR[imm] = src1; 
  // csrrs:    R(rd) = CSR[imm]; CSR[imm] |= src1;
  // ecall:    CSR[mcause] = R[15]
  //           CSR[mepc  ] = pc
  //           pc_next = CSR[mtvec]
  // mret:     pc_next = CSR[mepc  ]
  // ==================================== ==================================== //
  io.ctrl.csrCtrl.csrCmd    := Mux(i_csrrw, U"3'd1",                            // 0=NOP,1=CSRRW,2=CSRRS
                               Mux(i_csrrs, U"3'd2", U"3'd0"))
  io.ctrl.csrCtrl.trapEnter := io.ifuTrapEnter | i_ecall | i_ebreak | i_illegal // idu的异常进入,主动进入或者出现非法指令
  io.ctrl.csrCtrl.excCause  := Mux(io.ifuTrapEnter, io.ifuExcCause,             // 前级的异常优先
                               Mux(i_illegal, U(2, 4 bits), 
                               Mux(i_ebreak, U(3, 4 bits), U(0, 4 bits))))
  io.ctrl.csrCtrl.trapExit  := i_mret                                           // 退出异常,MRET
  io.ctrl.csrCtrl.ecall     := i_ecall
  // ================================ 指令类别(仅仿真, 性能统计) ================================ //
  if (config.enableSimDebug) {
    io.isCalc   := i_add | i_sub | i_sll | i_slt | i_sltu | i_xor | i_srl | i_sra | i_or | i_and |
                   i_addi | i_slli | i_slti | i_sltiu | i_xori | i_srli | i_srai | i_ori | i_andi |
                   i_lui | i_auipc
    io.isMem    := i_lb | i_lh | i_lw | i_lbu | i_lhu | i_sb | i_sh | i_sw
    io.isBranch := i_beq | i_bne | i_blt | i_bge | i_bltu | i_bgeu
    io.isJump   := i_jal | i_jalr
    io.isCsr    := i_csrrw | i_csrrs
    io.isSys    := i_ecall | i_ebreak | i_mret | i_fence_i
  }
}

