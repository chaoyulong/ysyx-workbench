package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

case class Decoder() extends Component {
  val io = new Bundle {
    val instr       = in  UInt(32 bits)
    val ctrl        = out(Ctrl())
    val imm         = out UInt(32 bits)
  }

  val instr = io.instr
  val i     = io.instr.asBits

  val op   = instr( 6 downto  0)     
  val func3= instr(14 downto 12)
  val func7= instr(31 downto 25)

  io.ctrl.rf_si.rf_write_addr := instr(11 downto 7)   // 为了写起来简洁，写寄存器地址在此赋值
// --------------------------------------------------------- 指令匹配 --------------------------------------------------------- //    
  val i_add    = i === M"0000000----------000-----0110011"
  val i_sub    = i === M"0100000----------000-----0110011"
  val i_sll    = i === M"0000000----------001-----0110011"
  val i_slt    = i === M"0000000----------010-----0110011"
  val i_sltu   = i === M"0000000----------011-----0110011"
  val i_xor    = i === M"0000000----------100-----0110011"
  val i_srl    = i === M"0000000----------101-----0110011"
  val i_sra    = i === M"0100000----------101-----0110011"
  val i_or     = i === M"0000000----------110-----0110011"
  val i_and    = i === M"0000000----------111-----0110011"

  val i_addi   = i === M"-----------------000-----0010011"  
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
  val i_sb     = i === M"-----------------000-----0100011"
  val i_sh     = i === M"-----------------001-----0100011"
  val i_sw     = i === M"-----------------010-----0100011"

  val i_beq    = i === M"-----------------000-----1100011"
  val i_bne    = i === M"-----------------001-----1100011"
  val i_blt    = i === M"-----------------100-----1100011"
  val i_bge    = i === M"-----------------101-----1100011"
  val i_bltu   = i === M"-----------------110-----1100011"
  val i_bgeu   = i === M"-----------------111-----1100011"
  val i_jalr   = i === M"-----------------000-----1100111"
  val i_jal    = i === M"-------------------------1101111"
  val i_lui    = i === M"-------------------------0110111"
  val i_auipc  = i === M"-------------------------0010111"

  // val i_mul    = i === M"0000001----------000-----0110011"
  // val i_mulh   = i === M"0000001----------001-----0110011"
  // val i_mulhsu = i === M"0000001----------010-----0110011"
  // val i_mulhu  = i === M"0000001----------011-----0110011"

  // val i_div    = i === M"0000001----------100-----0110011"
  // val i_divu   = i === M"0000001----------101-----0110011"
  // val i_rem    = i === M"0000001----------110-----0110011"
  // val i_remu   = i === M"0000001----------111-----0110011"

  val i_csrrw  = i === M"-----------------001-----1110011"
  val i_csrrs  = i === M"-----------------010-----1110011"
  // val i_csrrc  = i === M"-----------------011-----1110011"
  // val i_csrrwi = i === M"-----------------101-----1110011"
  // val i_csrrsi = i === M"-----------------110-----1110011"
  // val i_csrrci = i === M"-----------------111-----1110011"

  val i_ecall  = i === M"00000000000000000000000001110011"
  val i_ebreak = i === M"00000000000100000000000001110011"
  val i_mret   = i === M"00110000001000000000000001110011"

  val i_fence_i= i === M"-----------------001-----0001111"
// --------------------------------------------------------- 指令类型 ------------------------------------------------------ //
  val type_U = op(4 downto 2) === U"101"
  val type_J = op(6 downto 2) === U"11011"
  val type_I = op(6 downto 2) === U"00100" || op(6 downto 2) === U"00000" || op(6 downto 2) === U"11100"
  val type_S = op(6 downto 2) === U"01000"
  val type_B = op(6 downto 2) === U"11000"
  val type_R = op(6 downto 2) === U"01100"
  // ------------------------------------------------------ 立即数生成 ------------------------------------------------------ //
  val immU = instr(31 downto 12) ## B"12'b0"
  val immJ = (instr(31) #* 12) ## instr(19 downto 12) ## instr(20) ## instr(30 downto 21)## B"0"
  val immI = (instr(31) #* 20) ## instr(31 downto 20)
  val immS = (instr(31) #* 20) ## instr(31 downto 25) ## instr(11 downto 7)
  val immB = (instr(31) #* 20) ## instr(7) ## instr(30 downto 25) ## instr(11 downto 8) ## B"0"

  // 独热码选择器
  val has_type = type_U | type_J | type_I | type_S | type_B
  val imm_values = Vec(immB, immS, immI, immJ, immU)          // 从低位到高位排序，为什么跟常规的有点不一样。。。。
  io.imm := Mux(has_type, MuxOH(type_U ## type_J ## type_I ## type_S ## type_B, imm_values).asUInt , U"32'h0")
// ------------------------------------------------------- 控制信号生成 ----------------------------------------------------- // 
  // ------------------ ebreak ------------------ //
  val my_ebreak = MyEbreak()
  my_ebreak.io.i_ebreak := i_ebreak
  // -------------------------------------------- //
  io.ctrl.rf_si.reg_wr := (i_lui|i_auipc|i_jal|i_jalr|i_csrrw| i_csrrs|
                            i_addi|i_slti|i_sltiu|i_xori|i_ori|i_andi|i_slli|i_srli|i_srai|
                            i_add|i_sub|i_xor|i_or|i_and|i_sll|i_srl|i_sra|i_slt|i_sltu|
                            i_lb|i_lh|i_lw|i_lbu|i_lhu)
  io.ctrl.alu_si.alu_asrc := (i_auipc | i_jal | i_jalr)       // 0：选通rdata1，1：选通PC。

  io.ctrl.alu_si.alu_bsrc :=Mux(type_R | type_B, U"00",       // 选通rdata2
                            Mux(i_jal  | i_jalr, U"10",       // 选通4，用于跳转
                            U"01" ))                          // 选通imm                                                   // 选通imm
  

  val alu_table = Seq(
    i_and | i_andi -> U"0111",   // 选择逻辑与输出
    i_or  | i_ori  ->   U"0110",   // 选择逻辑或输出
    i_xor | i_xori ->  U"0100",   // 选择异或输出
    i_sll | i_slli ->  U"0001",   // 选择移位器输出，左移
    i_srl | i_srli ->  U"0101",   // 选择移位器输出，逻辑右移
    i_sra | i_srai ->  U"1101",   // 选择移位器输出，算术右移
    i_sub ->          U"1000",   // 选择加法器输出，做减法
    // (i_add | i_addi,  U"0000"),// 选择加法器输出，做加法
    i_lui ->          U"0011",   // 选择ALU输入B的结果直接输出
    i_slt | i_slti | i_beq | i_bne | i_blt | i_bge -> U"0010",  // 做减法，选择带符号小于置位结果输出, Less按带符号结果设置
    i_sltu| i_sltiu| i_bltu| i_bgeu ->  U"1010"                // 做减法，选择无符号小于置位结果输出, Less按无符号结果设置
  )

  io.ctrl.alu_si.alu_ctr := PriorityMux(alu_table :+ (True -> U"0000"))

  // io.ctrl.alu_si.alu_ctr := Mux(i_and | i_andi, U"0111",                                 // 选择逻辑与输出
  //                           Mux(i_or  | i_ori , U"0110",                                 // 选择逻辑或输出
  //                           Mux(i_sra | i_srai, U"1101",                                 // 选择移位器输出，算术右移
  //                           Mux(i_srl | i_srli, U"0101",                                // 选择移位器输出，逻辑右移
  //                           Mux(i_xor | i_xori, U"0100",                                 // 选择异或输出
  //                           Mux(i_lui         , U"0011",                                         // 选择ALU输入B的结果直接输出
  //                           Mux(i_sltu| i_sltiu| i_bltu| i_bgeu, U"1010",              // 做减法，选择无符号小于置位结果输出, Less按无符号结果设置
  //                           Mux(i_slt | i_slti | i_beq | i_bne | i_blt | i_bge, U"0010",  // 做减法，选择带符号小于置位结果输出, Less按带符号结果设置
  //                           Mux(i_sll | i_slli, U"0001",                               // 选择移位器输出，左移
  //                           Mux(i_sub         , U"1000",                                          // 选择加法器输出，做减法
  //                           U"0000"  ))))))))))                                                    // 选择加法器输出，做加法
  io.ctrl.alu_si.branch :=  Mux(i_jal , U"001",                                            // 无条件跳转PC目标
                Mux(i_jalr, U"010",                                            // 无条件跳转寄存器目标
                Mux(i_beq , U"100",                                            // 条件分支，等于
                Mux(i_bne , U"101",                                             // 条件分支，不等于
                Mux(i_blt | i_bltu, U"110",                                  // 条件分支，小于
                Mux(i_bge | i_bgeu, U"111",                                  // 条件分支，大于等于
                U"000"))))))
  io.ctrl.rf_si.mem2reg  := (i_lb | i_lh | i_lw | i_lbu | i_lhu)
  io.ctrl.mem_si.mem_wr  := (i_sb | i_sh | i_sw)
  io.ctrl.mem_si.mem_op  := func3  
}

