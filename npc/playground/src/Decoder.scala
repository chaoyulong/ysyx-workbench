package playground

import spinal.core._

case class Ctrl() extends Bundle {   // 控制信号
    val reg_wr   = out Bool()  // 控制是否对寄存器rd进行写回，为1时写回寄存器。
    val alu_asrc = out Bool() // 选择ALU输入端A的来源。为0时选择rs1，为1时选择PC。
    val alu_bsrc = out UInt(2 bits)  // 选择ALU输入端B的来源。为00时选择rs2，为01时选择imm，为10时选择常数4（用于跳转时计算返回地址PC+4）
    val alu_ctr  = out UInt(4 bits)  // 选择ALU执行的操作
    val branch   = out UInt(3 bits)  // 说明分支和跳转的种类，用于生成最终的分支控制信号
    val mem2reg  = out Bool()  // 选择写入寄存器的内容，为1时为存储器，为0时为alu
    val mem_wr   = out Bool()  // 为1时写入存储器
    val mem_op   = out UInt(3 bits)  // 控制数据存储器读写格式，为010时为4字节读写，为001时为2字节读写带符号扩展，为000时为1字节读写带符号扩展，为101时为2字节读写无符号扩展，为100时为1字节读写无符号扩展
    val imm      = out UInt(32 bits)  // 立即数
}

case class Decoder() extends Component {
  val io = new Bundle {
    val instr    = in  UInt(32 bits)
    val ctrl = out(Ctrl)
  }

  val instr = io.ctrl

  val op   = instr( 6 downto  0)     
  val func3= instr(14 downto 12)
  val func7= instr(31 downto 25)
  val rs1  = instr(19 downto 15) 
  val rs2  = instr(24 downto 20)
  val rd   = instr(11 downto 7)

  val func7_is_0 = ~(func7.orR)
  val func7_40_is_0 = ~(func7(4 downto 0).orR) 
  // val func7_61_is_0 = ~(func7(6 downto 1).orR) 

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
  val i_jalr   = op_decode_h(U"110") & op_decode_l(U"0111") & f3_decode(U"000")
  val i_sb     = op_decode_h(U"010") & op_decode_l(U"0011") & f3_decode(U"000")
  val i_sh     = op_decode_h(U"010") & op_decode_l(U"0011") & f3_decode(U"001")
  val i_sw     = op_decode_h(U"010") & op_decode_l(U"0011") & f3_decode(U"010")
  val i_add    = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"000") & func7_is_0
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

// ***************************************** 立即数生成 *********************************************** //
  val immI = U((instr(31) #* 20) ## instr(31 downto 20))
  val immU = U(instr(31 downto 12) ## U(0, 12 bits))
  val immS = U((instr(31) #* 20) ## instr(31 downto 25) ## instr(11 downto 7))
  val immB = U((instr(31) #* 20) ## instr(7) ## instr(30 downto 25) ## instr(11 downto 8) ## U(0, 1 bits))
  val immJ = U((instr(31) #* 12) ## instr(19 downto 12) ## instr(20) ## instr(30 downto 21)## U(0, 1 bits))

  val type_U = (i_auipc | i_lui)
  val type_J = (i_jal)
  val type_I = (i_lb | i_lh | i_lw | i_lbu | i_lhu | i_addi | i_slti | i_sltiu | i_xori | 
                   i_ori | i_andi | i_slli | i_srli | i_srai | i_jalr | i_csrrw | i_csrrs)            
  val type_S = (i_sb | i_sh | i_sw)
  val type_B = (i_beq | i_bne | i_blt | i_bge | i_bltu | i_bgeu)
  val type_R = (i_add | i_sub | i_xor | i_or | i_and | i_sll | i_srl | i_sra | i_slt | i_sltu)

  io.ctrl.imm := Mux(type_U, immU,
            Mux(type_J, immJ,
            Mux(type_I, immI,
            Mux(type_S, immS,
            Mux(type_B, immB, U(0, 32 bits))))))

// **************************************** 控制信号生成 ********************************************** // 
  val my_ebreak = MyEbreak()
  my_ebreak.io.i_ebreak := i_ebreak

  io.ctrl.reg_wr := (i_lui|i_auipc|i_jal|i_jalr|i_csrrw| i_csrrs|
              i_addi|i_slti|i_sltiu|i_xori|i_ori|i_andi|i_slli|i_srli|i_srai|
              i_add|i_sub|i_xor|i_or|i_and|i_sll|i_srl|i_sra|i_slt|i_sltu|
              i_lb|i_lh|i_lw|i_lbu|i_lhu)
  io.ctrl.alu_asrc := (i_auipc | i_jal | i_jalr)                                     // 0：选通rdata1，1：选通PC。
  io.ctrl.alu_bsrc := Mux(i_jal | i_jalr, U"10",                                  // 选通4，用于跳转
                 Mux(i_add|i_sub|i_xor|i_or|i_and|i_sll|i_srl|i_sra|i_slt|
                 i_sltu|i_beq|i_bne|i_blt|i_bge|i_bltu|i_bgeu, U"00",       // 选通rdata2
                 U"01" ))                                                        // 选通imm
  io.ctrl.alu_ctr := Mux(i_and | i_andi, U"0111",                                 // 选择逻辑与输出
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
  io.ctrl.branch :=  Mux(i_jal , U"001",                                            // 无条件跳转PC目标
                Mux(i_jalr, U"010",                                            // 无条件跳转寄存器目标
                Mux(i_beq , U"100",                                            // 条件分支，等于
                Mux(i_bne , U"101",                                             // 条件分支，不等于
                Mux(i_blt | i_bltu, U"110",                                  // 条件分支，小于
                Mux(i_bge | i_bgeu, U"111",                                  // 条件分支，大于等于
                U"000"))))))
  io.ctrl.mem2reg  := (i_lb | i_lh | i_lw | i_lbu | i_lhu)
  io.ctrl.mem_wr  := (i_sb | i_sh | i_sw)
  io.ctrl.mem_op  := func3  
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
