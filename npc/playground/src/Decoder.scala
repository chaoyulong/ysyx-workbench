package playground

import spinal.core._

case class Decoder() extends Component {
  val io = new Bundle {
    val instr       = in  UInt(32 bits)
    val ctrl        = out(Ctrl())
    val instr_type  = out Bits(6 bits)
  }

  val instr = io.instr
  val i     = io.instr.asBits

  val op   = instr( 6 downto  0)     
  val func3= instr(14 downto 12)
  val func7= instr(31 downto 25)
  val rs1  = instr(19 downto 15) 
  val rs2  = instr(24 downto 20)
  val rd   = instr(11 downto 7)

  val func7_is_0 = ~(func7.orR)
  val func7_40_is_0 = ~(func7(4 downto 0).orR) 

  io.ctrl.rf_si.rf_write_addr := instr(11 downto 7)   // 为了写起来简洁，写寄存器地址在此赋值

// ****************************************** 指令通用部分译码 ************************************************ //    
  val op_decoder0 = Decoder_4_16()
  op_decoder0.io.input := op(3 downto 0)
  val op_decode_l = op_decoder0.io.output

  val op_decoder1 = Decoder_3_8()
  op_decoder1.io.input := op(6 downto 4)
  val op_decode_h = op_decoder1.io.output  

  val fun3_decoder = Decoder_3_8()
  fun3_decoder.io.input := func3
  val f3_decode = fun3_decoder.io.output  
// ****************************************** 指令匹配 ************************************************ //    


  def ADDI               = M"-----------------000-----0010011"
  def SLLI               = M"0000000----------001-----0010011"
  def SLTI               = M"-----------------010-----0010011"
  def SLTIU              = M"-----------------011-----0010011"
  def XORI               = M"-----------------100-----0010011"
  def SRLI               = M"0000000----------101-----0010011"
  def SRAI               = M"0100000----------101-----0010011"
  def ORI                = M"-----------------110-----0010011"
  def ANDI               = M"-----------------111-----0010011"

  def LB                 = M"-----------------000-----0000011"
  def LH                 = M"-----------------001-----0000011"
  def LW                 = M"-----------------010-----0000011"
  def LBU                = M"-----------------100-----0000011"
  def LHU                = M"-----------------101-----0000011"
  def SB                 = M"-----------------000-----0100011"
  def SH                 = M"-----------------001-----0100011"
  def SW                 = M"-----------------010-----0100011"

  def LR                 = M"00010--00000-----010-----0101111"
  def SC                 = M"00011------------010-----0101111"

  def AMOSWAP            = M"00001------------010-----0101111"
  def AMOADD             = M"00000------------010-----0101111"
  def AMOXOR             = M"00100------------010-----0101111"
  def AMOAND             = M"01100------------010-----0101111"
  def AMOOR              = M"01000------------010-----0101111"
  def AMOMIN             = M"10000------------010-----0101111"
  def AMOMAX             = M"10100------------010-----0101111"
  def AMOMINU            = M"11000------------010-----0101111"
  def AMOMAXU            = M"11100------------010-----0101111"

  def BEQ (rvc : Boolean) = if(rvc) M"-----------------000-----1100011" else M"-----------------000---0-1100011"
  def BNE (rvc : Boolean) = if(rvc) M"-----------------001-----1100011" else M"-----------------001---0-1100011"
  def BLT (rvc : Boolean) = if(rvc) M"-----------------100-----1100011" else M"-----------------100---0-1100011"
  def BGE (rvc : Boolean) = if(rvc) M"-----------------101-----1100011" else M"-----------------101---0-1100011"
  def BLTU(rvc : Boolean) = if(rvc) M"-----------------110-----1100011" else M"-----------------110---0-1100011"
  def BGEU(rvc : Boolean) = if(rvc) M"-----------------111-----1100011" else M"-----------------111---0-1100011"
  def JALR               = M"-----------------000-----1100111"
  def JAL(rvc : Boolean) = if(rvc) M"-------------------------1101111" else M"----------0--------------1101111"
  def LUI                = M"-------------------------0110111"
  def AUIPC              = M"-------------------------0010111"

  def MULX               = M"0000001----------0-------0110011"
  def DIVX               = M"0000001----------1-------0110011"

  def MUL                = M"0000001----------000-----0110011"
  def MULH               = M"0000001----------001-----0110011"
  def MULHSU             = M"0000001----------010-----0110011"
  def MULHU              = M"0000001----------011-----0110011"


  def DIV                = M"0000001----------100-----0110011"
  def DIVU               = M"0000001----------101-----0110011"
  def REM                = M"0000001----------110-----0110011"
  def REMU               = M"0000001----------111-----0110011"



  def CSRRW              = M"-----------------001-----1110011"
  def CSRRS              = M"-----------------010-----1110011"
  def CSRRC              = M"-----------------011-----1110011"
  def CSRRWI             = M"-----------------101-----1110011"
  def CSRRSI             = M"-----------------110-----1110011"
  def CSRRCI             = M"-----------------111-----1110011"

  def ECALL              = M"00000000000000000000000001110011"
  def EBREAK             = M"00000000000100000000000001110011"
  def FENCEI             = M"00000000000000000001000000001111"
  def MRET               = M"00110000001000000000000001110011"
  def SRET               = M"00010000001000000000000001110011"
  def WFI                = M"00010000010100000000000001110011"

  def FENCE              = M"-----------------000-----0001111"
  def FENCE_I            = M"-----------------001-----0001111"
  def SFENCE_VMA         = M"0001001----------000000001110011"

  def FMV_W_X            = M"111100000000-----000-----1010011"
  def FADD_S             = M"0000000------------------1010011"
  def FSUB_S             = M"0000100------------------1010011"
  def FMUL_S             = M"0001000------------------1010011"
  def FDIV_S             = M"0001100------------------1010011"
  def FSGNJ_S            = M"0010000----------000-----1010011"
  def FSGNJN_S           = M"0010000----------001-----1010011"
  def FSGNJX_S           = M"0010000----------010-----1010011"
  def FMIN_S             = M"0010100----------000-----1010011"
  def FMAX_S             = M"0010100----------001-----1010011"
  def FSQRT_S            = M"010110000000-------------1010011"
  def FCVT_S_W           = M"110100000000-------------1010011"
  def FCVT_S_WU          = M"110100000001-------------1010011"
  def FCVT_S_L           = M"110100000010-------------1010011"
  def FCVT_S_LU          = M"110100000011-------------1010011"
  def FCVT_W_S           = M"110000000000-------------1010011"
  def FCVT_WU_S          = M"110000000001-------------1010011"
  def FCVT_L_S           = M"110000000010-------------1010011"
  def FCVT_LU_S          = M"110000000011-------------1010011"
  def FCLASS_S           = M"111000000000-----001-----1010011"
  def FMADD_S            = M"-----00------------------1000011"
  def FMSUB_S            = M"-----00------------------1000111"
  def FNMSUB_S           = M"-----00------------------1001011"
  def FNMADD_S           = M"-----00------------------1001111"

  def FLE_S              = M"1010000----------000-----1010011"
  def FLT_S              = M"1010000----------001-----1010011"
  def FEQ_S              = M"1010000----------010-----1010011"
  def FADD_D             = M"0000001------------------1010011"
  def FSUB_D             = M"0000101------------------1010011"
  def FMUL_D             = M"0001001------------------1010011"
  def FDIV_D             = M"0001101------------------1010011"
  def FSGNJ_D            = M"0010001----------000-----1010011"
  def FSGNJN_D           = M"0010001----------001-----1010011"
  def FSGNJX_D           = M"0010001----------010-----1010011"
  def FMIN_D             = M"0010101----------000-----1010011"
  def FMAX_D             = M"0010101----------001-----1010011"
  def FSQRT_D            = M"010110100000-------------1010011"
  def FMV_X_W            = M"111000000000-----000-----1010011"
  def FCVT_W_D           = M"110000100000-------------1010011"
  def FCVT_WU_D          = M"110000100001-------------1010011"
  def FCVT_L_D           = M"110000100010-------------1010011"
  def FCVT_LU_D          = M"110000100011-------------1010011"
  def FMV_X_D            = M"111000100000-----000-----1010011"
  def FCLASS_D           = M"111000100000-----001-----1010011"
  def FCVT_D_W           = M"110100100000-------------1010011"
  def FCVT_D_WU          = M"110100100001-------------1010011"
  def FCVT_D_L           = M"110100100010-------------1010011"
  def FCVT_D_LU          = M"110100100011-------------1010011"
  def FMV_D_X            = M"111100100000-----000-----1010011"
  def FMADD_D            = M"-----01------------------1000011"
  def FMSUB_D            = M"-----01------------------1000111"
  def FNMSUB_D           = M"-----01------------------1001011"
  def FNMADD_D           = M"-----01------------------1001111"
  def FLE_D              = M"1010001----------000-----1010011"
  def FLT_D              = M"1010001----------001-----1010011"
  def FEQ_D              = M"1010001----------010-----1010011"

  def FCVT_S_D           = M"010000000001-------------1010011"
  def FCVT_D_S           = M"010000100000-------------1010011"

  def FLW                = M"-----------------010-----0000111"
  def FLD                = M"-----------------011-----0000111"
  def FSW                = M"-----------------010-----0100111"
  def FSD                = M"-----------------011-----0100111"

  val i_auipc  = op_decode_h(U"001") & op_decode_l(U"0111")
  val i_lui    = op_decode_h(U"011") & op_decode_l(U"0111")
  val i_jal    = op_decode_h(U"110") & op_decode_l(U"1111")
  val i_lb     = op_decode_h(U"000") & op_decode_l(U"0011") & f3_decode(U"000")
  val i_lh     = op_decode_h(U"000") & op_decode_l(U"0011") & f3_decode(U"001")
  val i_lw     = op_decode_h(U"000") & op_decode_l(U"0011") & f3_decode(U"010")
  val i_lbu    = op_decode_h(U"000") & op_decode_l(U"0011") & f3_decode(U"100")
  val i_lhu    = op_decode_h(U"000") & op_decode_l(U"0011") & f3_decode(U"101")
  val i_addi   = op_decode_h(U"001") & op_decode_l(U"0011") & f3_decode(U"000")
  val i_slti   = op_decode_h(U"001") & op_decode_l(U"0011") & f3_decode(U"010")
  val i_sltiu  = op_decode_h(U"001") & op_decode_l(U"0011") & f3_decode(U"011")
  val i_xori   = op_decode_h(U"001") & op_decode_l(U"0011") & f3_decode(U"100")
  val i_ori    = op_decode_h(U"001") & op_decode_l(U"0011") & f3_decode(U"110")
  val i_andi   = op_decode_h(U"001") & op_decode_l(U"0011") & f3_decode(U"111")
  val i_slli   = op_decode_h(U"001") & op_decode_l(U"0011") & f3_decode(U"001") & func7_is_0
  val i_srli   = op_decode_h(U"001") & op_decode_l(U"0011") & f3_decode(U"101") & func7_is_0
  val i_srai   = op_decode_h(U"001") & op_decode_l(U"0011") & f3_decode(U"101") & func7_40_is_0 & func7(5) & ~func7(6)
  val i_csrrw  = op_decode_h(U"111") & op_decode_l(U"0011") & f3_decode(U"001")
  val i_csrrs  = op_decode_h(U"111") & op_decode_l(U"0011") & f3_decode(U"010")
  // val i_csrrc  = op_decode_h(U"111") & op_decode_l(U"0011") & f3_decode(U"011")
  // val i_csrrwi = op_decode_h(U"111") & op_decode_l(U"0011") & f3_decode(U"101")
  // val i_csrrsi = op_decode_h(U"111") & op_decode_l(U"0011") & f3_decode(U"110")
  // val i_csrrci = op_decode_h(U"111") & op_decode_l(U"0011") & f3_decode(U"111")

    def ADD                = M"0000000----------000-----0110011"
  def SUB                = M"0100000----------000-----0110011"
  def SLL                = M"0000000----------001-----0110011"
  def SLT                = M"0000000----------010-----0110011"
  def SLTU               = M"0000000----------011-----0110011"
  def XOR                = M"0000000----------100-----0110011"
  def SRL                = M"0000000----------101-----0110011"
  def SRA                = M"0100000----------101-----0110011"
  def OR                 = M"0000000----------110-----0110011"
  def AND                = M"0000000----------111-----0110011"

  val i_jalr   = op_decode_h(U"110") & op_decode_l(U"0111") & f3_decode(U"000")
  val i_sb     = op_decode_h(U"010") & op_decode_l(U"0011") & f3_decode(U"000")
  val i_sh     = op_decode_h(U"010") & op_decode_l(U"0011") & f3_decode(U"001")
  val i_sw     = op_decode_h(U"010") & op_decode_l(U"0011") & f3_decode(U"010")
  val i_add    = i === M"0000000----------000-----0110011"
  val i_sub    = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"000") & func7_40_is_0 & func7(5) & ~func7(6)
  val i_xor    = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"100") & func7_is_0
  val i_or     = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"110") & func7_is_0
  val i_and    = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"111") & func7_is_0
  val i_sll    = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"001") & func7_is_0
  val i_srl    = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"101") & func7_is_0
  val i_sra    = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"101") & func7_40_is_0 & func7(5) & ~func7(6)
  val i_slt    = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"010") & func7_is_0
  val i_sltu   = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"011") & func7_is_0
  // val i_mul    = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"000") & func7_61_is_0 & func7(0);
  // val i_mulh   = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"001") & func7_61_is_0 & func7(0);
  // val i_mulhsu = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"010") & func7_61_is_0 & func7(0);
  // val i_mulhu  = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"011") & func7_61_is_0 & func7(0);
  // val i_div    = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"100") & func7_61_is_0 & func7(0);
  // val i_divu   = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"101") & func7_61_is_0 & func7(0);
  // val i_rem    = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"110") & func7_61_is_0 & func7(0);
  // val i_remu   = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"111") & func7_61_is_0 & func7(0);
  val i_beq    = op_decode_h(U"110") & op_decode_l(U"0011") & f3_decode(U"000")
  val i_bne    = op_decode_h(U"110") & op_decode_l(U"0011") & f3_decode(U"001")
  val i_blt    = op_decode_h(U"110") & op_decode_l(U"0011") & f3_decode(U"100")
  val i_bge    = op_decode_h(U"110") & op_decode_l(U"0011") & f3_decode(U"101")
  val i_bltu   = op_decode_h(U"110") & op_decode_l(U"0011") & f3_decode(U"110")
  val i_bgeu   = op_decode_h(U"110") & op_decode_l(U"0011") & f3_decode(U"111")
  val i_ecall  = op_decode_h(U"111") & op_decode_l(U"0011") & f3_decode(U"000") & func7 === U"0000000" & rs2 === U"00000" & rs1 === U"00000" & rd === U"00000"
  val i_ebreak = op_decode_h(U"111") & op_decode_l(U"0011") & f3_decode(U"000") & func7 === U"0000000" & rs2 === U"00001" & rs1 === U"00000" & rd === U"00000"
  val i_mret   = op_decode_h(U"111") & op_decode_l(U"0011") & f3_decode(U"000") & func7 === U"0011000" & rs2 === U"00010" & rs1 === U"00000" & rd === U"00000"
  val i_fence_i= op_decode_h(U"000") & op_decode_l(U"1111") & f3_decode(U"001");

// ***************************************** 指令类型 *********************************************** //
  val type_U = (i_auipc | i_lui)
  val type_J = (i_jal)
  val type_I = (i_lb | i_lh | i_lw | i_lbu | i_lhu | i_addi | i_slti | i_sltiu | i_xori | 
                   i_ori | i_andi | i_slli | i_srli | i_srai | i_jalr | i_csrrw | i_csrrs)            
  val type_S = (i_sb | i_sh | i_sw)
  val type_B = (i_beq | i_bne | i_blt | i_bge | i_bltu | i_bgeu)
  val type_R = (i_add | i_sub | i_xor | i_or | i_and | i_sll | i_srl | i_sra | i_slt | i_sltu)

  io.instr_type := type_U ## type_J ## type_I ## type_S ## type_B ## type_R
// **************************************** 控制信号生成 ********************************************** // 
  val my_ebreak = MyEbreak()
  my_ebreak.io.i_ebreak := i_ebreak

  io.ctrl.rf_si.reg_wr := (i_lui|i_auipc|i_jal|i_jalr|i_csrrw| i_csrrs|
                            i_addi|i_slti|i_sltiu|i_xori|i_ori|i_andi|i_slli|i_srli|i_srai|
                            i_add|i_sub|i_xor|i_or|i_and|i_sll|i_srl|i_sra|i_slt|i_sltu|
                            i_lb|i_lh|i_lw|i_lbu|i_lhu)
  io.ctrl.alu_si.alu_asrc := (i_auipc | i_jal | i_jalr)                                     // 0：选通rdata1，1：选通PC。
  io.ctrl.alu_si.alu_bsrc := Mux(i_jal | i_jalr, U"10",                                  // 选通4，用于跳转
                              Mux(i_add|i_sub|i_xor|i_or|i_and|i_sll|i_srl|i_sra|i_slt|
                              i_sltu|i_beq|i_bne|i_blt|i_bge|i_bltu|i_bgeu, U"00",       // 选通rdata2
                              U"01" ))                                                        // 选通imm
  io.ctrl.alu_si.alu_ctr := Mux(i_and | i_andi, U"0111",                                 // 选择逻辑与输出
                            Mux(i_or  | i_ori , U"0110",                                 // 选择逻辑或输出
                            Mux(i_sra | i_srai, U"1101",                                 // 选择移位器输出，算术右移
                            Mux(i_srl | i_srli, U"0101",                                // 选择移位器输出，逻辑右移
                            Mux(i_xor | i_xori, U"0100",                                 // 选择异或输出
                            Mux(i_lui         , U"0011",                                         // 选择ALU输入B的结果直接输出
                            Mux(i_sltu| i_sltiu| i_bltu| i_bgeu, U"1010",              // 做减法，选择无符号小于置位结果输出, Less按无符号结果设置
                            Mux(i_slt | i_slti | i_beq | i_bne | i_blt | i_bge, U"0010",  // 做减法，选择带符号小于置位结果输出, Less按带符号结果设置
                            Mux(i_sll | i_slli, U"0001",                               // 选择移位器输出，左移
                            Mux(i_sub         , U"1000",                                          // 选择加法器输出，做减法
                            U"0000"  ))))))))))                                                    // 选择加法器输出，做加法
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

case class Decoder_4_16() extends Component {   // 4-16译码器
  val io = new Bundle {
    val input  = in  UInt(4 bits)            
    val output = out UInt(16 bits)        
  }
  
  switch(io.input) {
    is(U"0000") { io.output := U"0000_0000_0000_0001" }
    is(U"0001") { io.output := U"0000_0000_0000_0010" }
    is(U"0010") { io.output := U"0000_0000_0000_0100" }
    is(U"0011") { io.output := U"0000_0000_0000_1000" }
    is(U"0100") { io.output := U"0000_0000_0001_0000" }
    is(U"0101") { io.output := U"0000_0000_0010_0000" }
    is(U"0110") { io.output := U"0000_0000_0100_0000" }
    is(U"0111") { io.output := U"0000_0000_1000_0000" }
    is(U"1000") { io.output := U"0000_0001_0000_0000" }
    is(U"1001") { io.output := U"0000_0010_0000_0000" }
    is(U"1010") { io.output := U"0000_0100_0000_0000" }
    is(U"1011") { io.output := U"0000_1000_0000_0000" }
    is(U"1100") { io.output := U"0001_0000_0000_0000" }
    is(U"1101") { io.output := U"0010_0000_0000_0000" }
    is(U"1110") { io.output := U"0100_0000_0000_0000" }
    is(U"1111") { io.output := U"1000_0000_0000_0000" }
  }
}

case class Decoder_3_8() extends Component {   // 3-8译码器
  val io = new Bundle {
    val input  = in  UInt(3 bits)          
    val output = out UInt(8 bits)            
  }
  
  switch(io.input) {
    is(U"000") { io.output := U"0000_0001" }
    is(U"001") { io.output := U"0000_0010" }
    is(U"010") { io.output := U"0000_0100" }
    is(U"011") { io.output := U"0000_1000" }
    is(U"100") { io.output := U"0001_0000" }
    is(U"101") { io.output := U"0010_0000" }
    is(U"110") { io.output := U"0100_0000" }
    is(U"111") { io.output := U"1000_0000" }
  }
}
