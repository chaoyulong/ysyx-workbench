package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

case class ysyx_23060082_Decoder(config: CpuConfig = CpuConfig()) extends Component {
  val io = new Bundle {
    val instr       = in  UInt(32 bits)
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

  val op   = instr( 6 downto  0)     
  val func3= instr(14 downto 12)
  // val func7= instr(31 downto 25) // 用不到

  
// ================================ 指令匹配 ================================ //    
  val i_add    = i === M"0000000----------000-----0110011"    // typeR
  val i_sub    = i === M"0100000----------000-----0110011"
  val i_sll    = i === M"0000000----------001-----0110011"
  val i_slt    = i === M"0000000----------010-----0110011"
  val i_sltu   = i === M"0000000----------011-----0110011"
  val i_xor    = i === M"0000000----------100-----0110011"
  val i_srl    = i === M"0000000----------101-----0110011"
  val i_sra    = i === M"0100000----------101-----0110011"
  val i_or     = i === M"0000000----------110-----0110011"
  val i_and    = i === M"0000000----------111-----0110011"

  val i_addi   = i === M"-----------------000-----0010011"    // typeI
  val i_slli   = i === M"0000000----------001-----0010011"
  val i_slti   = i === M"-----------------010-----0010011"
  val i_sltiu  = i === M"-----------------011-----0010011"
  val i_xori   = i === M"-----------------100-----0010011"
  val i_srli   = i === M"0000000----------101-----0010011"
  val i_srai   = i === M"0100000----------101-----0010011"
  val i_ori    = i === M"-----------------110-----0010011"
  val i_andi   = i === M"-----------------111-----0010011"
  
  val i_lb     = i === M"-----------------000-----0000011"
  val i_lh     = i === M"-----------------001-----0000011"
  val i_lw     = i === M"-----------------010-----0000011"
  val i_lbu    = i === M"-----------------100-----0000011"
  val i_lhu    = i === M"-----------------101-----0000011"
  val i_sb     = i === M"-----------------000-----0100011"    // typeS
  val i_sh     = i === M"-----------------001-----0100011"
  val i_sw     = i === M"-----------------010-----0100011"

  val i_beq    = i === M"-----------------000-----1100011"    // typeB
  val i_bne    = i === M"-----------------001-----1100011"
  val i_blt    = i === M"-----------------100-----1100011"
  val i_bge    = i === M"-----------------101-----1100011"
  val i_bltu   = i === M"-----------------110-----1100011"
  val i_bgeu   = i === M"-----------------111-----1100011"
  val i_jalr   = i === M"-----------------000-----1100111"    // typeI
  val i_jal    = i === M"-------------------------1101111"    // typeJ
  val i_lui    = i === M"-------------------------0110111"    // typeU
  val i_auipc  = i === M"-------------------------0010111"

  // val i_mul    = i === M"0000001----------000-----0110011" // typeR
  // val i_mulh   = i === M"0000001----------001-----0110011"
  // val i_mulhsu = i === M"0000001----------010-----0110011"
  // val i_mulhu  = i === M"0000001----------011-----0110011"

  // val i_div    = i === M"0000001----------100-----0110011"
  // val i_divu   = i === M"0000001----------101-----0110011"
  // val i_rem    = i === M"0000001----------110-----0110011"
  // val i_remu   = i === M"0000001----------111-----0110011"

  val i_csrrw  = i === M"-----------------001-----1110011"  // 系统指令，暂且命名为typeN
  val i_csrrs  = i === M"-----------------010-----1110011"

  val i_ecall  = i === M"00000000000000000000000001110011"
  val i_ebreak = i === M"00000000000100000000000001110011"
  val i_mret   = i === M"00110000001000000000000001110011"

  val i_fence_i= i === M"-----------------001-----0001111"

  // 判断非法指令
  val isLegal = i_add | i_sub | i_sll | i_slt | i_sltu| i_xor | i_srl | i_sra | i_or  | i_and | 
                i_addi | i_slli | i_slti | i_sltiu | i_xori | i_srli | i_srai | i_ori | i_andi |
                i_lb | i_lh | i_lw | i_lbu | i_lhu | i_sb | i_sh | i_sw |
                i_beq | i_bne | i_blt | i_bge | i_bltu | i_bgeu | i_jalr | i_jal  | i_lui | i_auipc |
                i_csrrw | i_csrrs | i_ecall | i_ebreak | i_mret | i_fence_i
  val i_illegal = (instr =/= 0) && !isLegal

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

  // io.imm := PriorityMux(Seq(
  //             typeU -> immU,
  //             typeJ -> immJ,
  //             typeI -> immI,
  //             typeS -> immS,
  //             typeB -> immB,
  //             True -> B"32'h0")).asUInt
  // MuxOH 的选择端要求 IndexedSeq[Bool](普通 Seq 会报 overloaded method apply), 所以 toIndexedSeq
  io.imm := MuxOH(Seq(typeU, typeJ, typeI, typeS, typeB).toIndexedSeq,
                  Seq(immU, immJ, immI, immS, immB)).asUInt
// ================================ 控制信号生成 ================================ //
  val csrWb = i_csrrw | i_csrrs

  // 按字段译码: "用不用读/写寄存器"只看 opcode(和 csr 的 funct3), 不必等 typeX 那些宽 OR 树
  // (这几个信号落在 IDU 的 stall/前递判定路径上, 是 ifu.state_0 那条关键路径的源头)
  val opF      = instr(6 downto 0)
  val isOpImm  = opF === M"0010011"    // addi/slti/sltiu/xori/ori/andi/slli/srli/srai
  val isOp     = opF === M"0110011"    // R 型
  val isLoad   = opF === M"0000011"    // lb/lh/lw/lbu/lhu
  val isStore  = opF === M"0100011"
  val isBranch = opF === M"1100011"
  val isJalr   = opF === M"1100111"

  // io.useRf1 := isOpImm || isOp || isLoad || isStore || isBranch || isJalr || csrWb || i_ecall
  // io.useRf2 := isOp || isStore || isBranch

  io.useRf1 := typeS|typeR|typeB|typeI|i_ecall
  io.useRf2 := isOpImm||isOp||isLoad||isStore||isBranch||isJalr||csrWb||i_ecall

  // io.ctrl.rfCtrl.regWr       := typeU || typeJ || isOpImm || isLoad || isJalr || csrWb || isOp
  io.ctrl.rfCtrl.regWr       := typeU|typeJ|typeI|typeR
  io.ctrl.rfCtrl.rfWriteAddr := instr(11 downto 7)            // 为了写起来简洁，写寄存器地址在此赋值
  io.ctrl.rfCtrl.mem2reg     := i_lb | i_lh | i_lw | i_lbu | i_lhu
  io.ctrl.rfCtrl.csr2reg     := csrWb

  io.ctrl.aluCtrl.aluAsrc := i_auipc | i_jal | i_jalr         // 0：选通rdata1，1：选通PC。
  io.ctrl.aluCtrl.aluBsrc := Mux(typeR | typeB, U"00",        // 选通rdata2
                             Mux(i_jal  | i_jalr, U"10",      // 选通4，用于跳转
                             U"01" ))                         // 选通imm
  // 条件两两互斥(每个指令只出现在一组) → one-hot 一层选出, 代替 12 级级联的 PriorityMux
  io.ctrl.aluCtrl.aluCtr  := MuxOH(
                             Seq((i_and | i_andi), (i_or  | i_ori ), (i_xor | i_xori),
                                 (i_sll | i_slli), (i_srl | i_srli), (i_sra | i_srai),
                                 i_sub, i_lui,
                                 (i_slt | i_slti | i_beq | i_bne | i_blt | i_bge),   // 有符号
                                 (i_sltu| i_sltiu| i_bltu| i_bgeu)).toIndexedSeq,    // 无符号
                             Seq(U"0111", U"0110", U"0100", U"0001", U"0101", U"1101",
                                 U"1000", U"0011", U"0010", U"1010"))
                             // 都不命中(如 add/addi) → 0 = U"0000", 与原 PriorityMux 默认一致             
  // 同理: 6 个条件互斥 → one-hot; 都不命中 → 0 = U"000", 与原默认一致
  io.ctrl.aluCtrl.branch  := MuxOH(
                             Seq(i_jal, i_jalr, i_beq, i_bne,
                                 (i_blt | i_bltu), (i_bge | i_bgeu)).toIndexedSeq,
                             Seq(U"001", U"010", U"100", U"101", U"110", U"111"))

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
  io.ctrl.csrCtrl.csrCmd    := Mux(i_csrrw, U"3'd1",              // 0=NOP,1=CSRRW,2=CSRRS
                               Mux(i_csrrs, U"3'd2", U"3'd0"))
  io.ctrl.csrCtrl.illegal   := i_illegal
  io.ctrl.csrCtrl.ebreak    := i_ebreak
  io.ctrl.csrCtrl.trapEnter := i_ecall | i_ebreak | i_illegal     // 异常进入,主动进入或者出现非法指令
  io.ctrl.csrCtrl.trapExit  := i_mret                             // 退出异常,MRET

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

