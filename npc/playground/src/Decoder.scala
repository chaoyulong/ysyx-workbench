package playground

import spinal.core._

case class Decoder() extends Component {
  val io = new Bundle {
    val instr    = in  UInt(32 bits)

    val rs1      = out UInt(5 bits)  // 读寄存器1选择
    val rs2      = out UInt(5 bits)  // 读寄存器2选择
    val rd       = out UInt(5 bits)  // 写寄存器选择

    // val reg_wr   = out port Bool          // 控制是否对寄存器rd进行写回，为1时写回寄存器。
    // val alu_asrc = out port Bits(1 bits)  // 选择ALU输入端A的来源。为0时选择rs1，为1时选择PC。
    // val alu_bsrc = out port Bits(2 bits)  // 选择ALU输入端B的来源。为00时选择rs2，为01时选择imm，为10时选择常数4（用于跳转时计算返回地址PC+4），为11时选择csrrdata（用于将csr寄存器值写入通用寄存器）
    // val alu_ctr  = out port Bits(4 bits)  // 选择ALU执行的操作
    // val branch   = out port Bits(3 bits)  // 说明分支和跳转的种类，用于生成最终的分支控制信号
    // val mem2reg  = out port Bool          // 选择写入寄存器的内容，为1时为存储器，为0时为alu
    // val mem_wr   = out port Bool          // 为1时写入存储器
    // val mem_op   = out port Bits(3 bits)  // 控制数据存储器读写格式，为010时为4字节读写，为001时为2字节读写带符号扩展，为000时为1字节读写带符号扩展，为101时为2字节读写无符号扩展，为100时为1字节读写无符号扩展
    val imm      = out UInt(32 bits) // 立即数
  }

  val instr = io.instr

  val op   = instr( 6 downto  0)     
  val func3= instr(14 downto 12)
  val func7= instr(31 downto 25)
  val rs1  = instr(19 downto 15) 
  val rs2  = instr(24 downto 20)
  val rd   = instr(11 downto 7)

  val func7_is_0 = ~(func7.orR)
  val func7_40_is_0 = ~(func7(4 downto 0).orR) 
  val func7_61_is_0 = ~(func7(6 downto 1).orR) 

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
  val i_csrrc  = op_decode_h(U"111") & op_decode_l(U"0011") & f3_decode(U"011")
  val i_csrrwi = op_decode_h(U"111") & op_decode_l(U"0011") & f3_decode(U"101")
  val i_csrrsi = op_decode_h(U"111") & op_decode_l(U"0011") & f3_decode(U"110")
  val i_csrrci = op_decode_h(U"111") & op_decode_l(U"0011") & f3_decode(U"111")
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
  val i_mul    = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"000") & func7_61_is_0 & func7(0);
  val i_mulh   = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"001") & func7_61_is_0 & func7(0);
  val i_mulhsu = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"010") & func7_61_is_0 & func7(0);
  val i_mulhu  = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"011") & func7_61_is_0 & func7(0);
  val i_div    = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"100") & func7_61_is_0 & func7(0);
  val i_divu   = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"101") & func7_61_is_0 & func7(0);
  val i_rem    = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"110") & func7_61_is_0 & func7(0);
  val i_remu   = op_decode_h(U"011") & op_decode_l(U"0011") & f3_decode(U"111") & func7_61_is_0 & func7(0);
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
  val type_R = (i_add | i_sub | i_xor | i_or | i_and | i_sll | i_srl | i_sra | i_slt | i_sltu |
                   i_mul | i_mulh | i_mulhsu | i_mulhu | i_div | i_divu | i_rem | i_remu)

  // switch(True) {
  //   is(type_U) { io.imm := immU }
  //   is(type_J) { io.imm := immJ }
  //   is(type_I) { io.imm := immI }
  //   is(type_S) { io.imm := immS }
  //   is(type_B) { io.imm := immB }
  //   default    { io.imm := U(0, 32 bits) }
  // } 

io.imm := Mux(type_U, immU,
          Mux(type_J, immJ,
          Mux(type_I, immI,
          Mux(type_S, immS,
          Mux(type_B, immB, U(0, 32 bits))))))
// ***************************************** 输出信号 *********************************************** //
  io.rs1 := rs1
  io.rs2 := rs2
  io.rd  := rd 
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



// module ysyx_23060082_decoder( 
//   input  [31:0] instr     ,

//   output [ 5:0] OpType    ,       // 指令类型
//   output [31:0] Imm       ,
//   output        RegWr     ,       // 控制是否对寄存器rd进行写回，为1时写回寄存器。
//   output        ALUAsrc   ,       // 选择ALU输入端A的来源。为0时选择rs1，为1时选择PC。
//   output [ 1:0] ALUBsrc   ,       // 选择ALU输入端B的来源。为00时选择rs2，为01时选择imm，为10时选择常数4（用于跳转时计算返回地址PC+4），为11时选择csrrdata（用于将csr寄存器值写入通用寄存器）
//   output [ 3:0] ALUctr    ,       // 选择ALU执行的操作
//   output [ 2:0] Branch    ,       // 说明分支和跳转的种类，用于生成最终的分支控制信号
//   output        MemRd     ,       // 为1时读存储器，并且选择寄存器rd写回数据来源，为0时选择ALU输出，为1时选择数据存储器输出。
//   output        MemWr     ,       // 控制是否对数据存储器进行写入，为1时写回存储器。
//   output [ 2:0] MemOP     ,       // 控制数据存储器读写格式，为010时为4字节读写，为001时为2字节读写带符号扩展，为000时为1字节读写带符号扩展，为101时为2字节读写无符号扩展，为100时为1字节读写无符号扩展。
//   output [ 2:0] CsrOp     ,
//   output [ 3:0] MulOp     ,       // 乘法控制信号{使能，符号(2)，结果}，被乘数与乘数的符号情况，11：有符号*有符号，10：有符号*无符号，00：无符号*无符号，乘法结果选择 1：高32位， 0：低32位  
//   output [ 2:0] DivOp     ,       // 除法控制信号{使能，符号，结果}，1：有符号除法，0：无符号除法，除法结果选择，1：余数，0：商  
//   output        op_fencei ,       // 执行fence.i指令
//   output        op_ebreak ,
//   output        op_ecall  ,
//   output        op_mret   ,
//   output [5:0]  op_type
// );

//   wire [ 6:0] op;     
//   wire [ 2:0] func3;   
//   wire [ 6:0] func7;  

//   wire [ 7:0] op_decode_h;
//   wire [15:0] op_decode_l;
//   wire [ 7:0] f3_decode;

//   assign op   = instr[ 6: 0];       
//   assign func3= instr[14:12];
//   assign func7= instr[31:25];

// // ************************************* 控制信号生成 ************************************* //
//   decoder_4_16 op_decoder0(
//     .in (op[3:0]      ),
//     .out(op_decode_l  )
//   );

//   decoder_3_8 op_decoder1(
//     .in (op[6:4]      ),
//     .out(op_decode_h  )
//   );

//   decoder_3_8 func3_decoder0(
//     .in (func3        ),
//     .out(f3_decode    )
//   );

  // wire i_auipc, i_lui, i_jal;
  // wire i_lb, i_lh, i_lw, i_lbu, i_lhu;
  // wire i_addi, i_slti, i_sltiu, i_xori, i_ori, i_andi, i_slli, i_srli, i_srai;
  // wire i_csrrw, i_csrrs, i_jalr;
  // wire i_sb, i_sh, i_sw;
  // wire i_add, i_sub, i_xor, i_or, i_and, i_sll, i_srl, i_sra, i_slt, i_sltu;
  // wire i_mul, i_mulh, i_mulhsu, i_mulhu, i_div, i_divu, i_rem, i_remu;
  // wire i_beq, i_bne, i_blt, i_bge, i_bltu, i_bgeu;
  // wire i_ecall, i_mret, i_ebreak, i_fence_i;

//   assign op_fencei  = i_fence_i;
//   assign op_ebreak  = i_ebreak;
//   assign op_ecall   = i_ecall;
//   assign op_mret    = i_mret;


//   // ***************************************** 立即数生成 *********************************************** //
//   wire [31:0] immI, immU, immS, immB, immJ;  
//   wire type_I, type_U, type_S, type_B, type_J, type_R;

//   assign immI = {{20{instr[31]}}, instr[31:20]};
//   assign immU = {instr[31:12], 12'b0};
//   assign immS = {{20{instr[31]}}, instr[31:25], instr[11:7]};
//   assign immB = {{20{instr[31]}}, instr[7], instr[30:25], instr[11:8], 1'b0};
//   assign immJ = {{12{instr[31]}}, instr[19:12], instr[20], instr[30:21], 1'b0};


//   assign type_U = (i_auipc | i_lui);
//   assign type_J = (i_jal);
//   assign type_I = (i_lb | i_lh | i_lw | i_lbu | i_lhu | i_addi | i_slti | i_sltiu | i_xori | 
//                    i_ori | i_andi | i_slli | i_srli | i_srai | i_jalr | i_csrrw | i_csrrs);                
//   assign type_S = (i_sb | i_sh | i_sw);
//   assign type_B = (i_beq | i_bne | i_blt | i_bge | i_bltu | i_bgeu);
//   assign type_R = (i_add | i_sub | i_xor | i_or | i_and | i_sll | i_srl | i_sra | i_slt | i_sltu |
//                    i_mul | i_mulh | i_mulhsu | i_mulhu | i_div | i_divu | i_rem | i_remu);

//   assign OpType = {type_I, type_U, type_S, type_B, type_J, type_R};

//   assign Imm =  type_U ? immU :
//                 type_J ? immJ :
//                 type_I ? immI :
//                 type_S ? immS :
//                 type_B ? immB : 32'b0;
//   // **************************************** 控制信号生成 ********************************************** //  
//   // assign RegWr = ~(i_ecall|i_mret|i_ebreak|i_fence_i|i_sb|i_sh|i_sw|i_beq|i_bne|i_blt|i_bge|i_bltu|i_bgeu); // 除去这几个其余全要写回寄存器
//   assign RegWr  = (i_lui|i_auipc|i_jal|i_jalr|i_csrrw| i_csrrs|
//                    i_addi|i_slti|i_sltiu|i_xori|i_ori|i_andi|i_slli|i_srli|i_srai|
//                    i_add|i_sub|i_xor|i_or|i_and|i_sll|i_srl|i_sra|i_slt|i_sltu|
//                    i_mul|i_mulh|i_mulhsu|i_mulhu|i_div|i_divu|i_rem|i_remu|
//                    i_lb|i_lh|i_lw|i_lbu|i_lhu);
//   assign ALUAsrc= (i_auipc | i_jal | i_jalr);                                   // 0：选通rdata1，1：选通PC。
//   assign ALUBsrc= (i_csrrw | i_csrrs) ? 2'b11 :                                 // 选通CsrRData，用于将从csr读出的数据写入通用寄存器
//                   (i_jal | i_jalr) ? 2'b10 :                                    // 选通4，用于跳转
//                   (i_ecall|i_add|i_sub|i_xor|i_or|i_and|i_sll|i_srl|i_sra|i_slt|
//                    i_mul|i_mulh|i_mulhsu|i_mulhu|i_div|i_divu|i_rem|i_remu|
//                    i_sltu|i_beq|i_bne|i_blt|i_bge|i_bltu|i_bgeu) ? 2'b00 :      // 选通rdata2
//                   2'b01;                                                        // 选通imm
//   assign ALUctr = (i_and | i_andi) ? 4'b0111 :                                  // 选择逻辑与输出
//                   (i_or  | i_ori ) ? 4'b0110 :                                  // 选择逻辑或输出
//                   (i_sra | i_srai) ? 4'b1101 :                                  // 选择移位器输出，算术右移
//                   (i_srl | i_srli) ? 4'b0101 :                                  // 选择移位器输出，逻辑右移
//                   (i_xor | i_xori) ? 4'b0100 :                                  // 选择异或输出
//                   (i_lui | i_csrrw | i_csrrs) ? 4'b0011 :                       // 选择ALU输入B的结果直接输出
//                   (i_sltu| i_sltiu | i_bltu | i_bgeu) ? 4'b1010 :               // 做减法，选择无符号小于置位结果输出, Less按无符号结果设置
//                   (i_slt | i_slti | i_beq | i_bne | i_blt | i_bge) ? 4'b0010 :  // 做减法，选择带符号小于置位结果输出, Less按带符号结果设置
//                   (i_sll | i_slli) ? 4'b0001 :                                  // 选择移位器输出，左移
//                   (i_sub) ? 4'b1000 :                                           // 选择加法器输出，做减法
//                   4'b0000;                                                      // 选择加法器输出，做加法
//   assign Branch = i_jal ? 3'b001 :                                              // 无条件跳转PC目标
//                   i_jalr? 3'b010 :                                              // 无条件跳转寄存器目标
//                   i_beq ? 3'b100 :                                              // 条件分支，等于
//                   i_bne ? 3'b101 :                                              // 条件分支，不等于
//                   (i_blt | i_bltu) ? 3'b110 :                                   // 条件分支，小于
//                   (i_bge | i_bgeu) ? 3'b111 :                                   // 条件分支，大于等于
//                   3'b000;
//   assign MemRd  = (i_lb | i_lh | i_lw | i_lbu | i_lhu);
//   assign MemWr  = (i_sb | i_sh | i_sw);
//   assign MemOP  = func3;  

// // CsrOp的说明：bit[2]：需要写入csr，bit[1]：需要操作pc寄存器，bit[0]：选择直接写入还是进行或运算
// // 000：不是csr指令，110：ecall， 100：csrrw，101：csrrs，010：mret
//   assign CsrOp  = i_ecall ? 3'b110 :    
//                   i_csrrw ? 3'b100 :    
//                   i_csrrs ? 3'b101 :    
//                   i_mret  ? 3'b010      
//                   : 3'b000;                                                  

//   assign MulOp  = i_mul ? 4'b1110 :
//                   i_mulh ? 4'b1111 :
//                   i_mulhsu ? 4'b1101 :
//                   i_mulhu ? 4'b1001 : 4'b0;
//   assign DivOp  = i_div ? 3'b110 :
//                   i_divu ? 3'b100 :                
//                   i_rem ? 3'b111 :
//                   i_remu ? 3'b101 : 3'b0;

//   // ***************************************** 性能计数器 *********************************************** //                    
//   wire op_mem =   i_lb | i_lh | i_lw | i_lbu | i_lhu | i_sb | i_sh | i_sw;
//   wire op_math =  i_add | i_sub | i_xor | i_or | i_and | i_sll | i_srl | i_sra | i_slt | i_sltu |
//                   i_mul | i_mulh | i_mulhsu | i_mulhu | i_div | i_divu | i_rem | i_remu |
//                   i_addi | i_slti | i_sltiu | i_xori | 
//                   i_ori | i_andi | i_slli | i_srli | i_srai;
//   wire op_csr =   i_ecall | i_mret | i_csrrw | i_csrrs ;   
//   wire op_jump =  i_jal | i_jalr | i_beq | i_bne | i_blt | i_bge | i_bltu | i_bgeu;  
//   wire op_other = i_lui | i_auipc | i_ebreak | i_fence_i; 
//   wire op_nop = ~(|instr);
//   assign op_type = {op_nop, op_mem, op_math, op_csr, op_jump, op_other};
//   // ****************************************** 指令匹配 ************************************************ //        
//   assign i_auipc  = op_decode_h[3'b001] & op_decode_l[4'b0111];
//   assign i_lui    = op_decode_h[3'b011] & op_decode_l[4'b0111];
//   assign i_jal    = op_decode_h[3'b110] & op_decode_l[4'b1111];
//   assign i_lb     = op_decode_h[3'b000] & op_decode_l[4'b0011] & f3_decode[3'b000];
//   assign i_lh     = op_decode_h[3'b000] & op_decode_l[4'b0011] & f3_decode[3'b001];
//   assign i_lw     = op_decode_h[3'b000] & op_decode_l[4'b0011] & f3_decode[3'b010];
//   assign i_lbu    = op_decode_h[3'b000] & op_decode_l[4'b0011] & f3_decode[3'b100];
//   assign i_lhu    = op_decode_h[3'b000] & op_decode_l[4'b0011] & f3_decode[3'b101];
//   assign i_addi   = op_decode_h[3'b001] & op_decode_l[4'b0011] & f3_decode[3'b000];
//   assign i_slti   = op_decode_h[3'b001] & op_decode_l[4'b0011] & f3_decode[3'b010];
//   assign i_sltiu  = op_decode_h[3'b001] & op_decode_l[4'b0011] & f3_decode[3'b011];
//   assign i_xori   = op_decode_h[3'b001] & op_decode_l[4'b0011] & f3_decode[3'b100];
//   assign i_ori    = op_decode_h[3'b001] & op_decode_l[4'b0011] & f3_decode[3'b110];
//   assign i_andi   = op_decode_h[3'b001] & op_decode_l[4'b0011] & f3_decode[3'b111];
//   assign i_slli   = op_decode_h[3'b001] & op_decode_l[4'b0011] & f3_decode[3'b001] & ~(|func7);
//   assign i_srli   = op_decode_h[3'b001] & op_decode_l[4'b0011] & f3_decode[3'b101] & ~(|func7);
//   assign i_srai   = op_decode_h[3'b001] & op_decode_l[4'b0011] & f3_decode[3'b101] & ~(|func7[4:0]) & func7[5] & ~func7[6];
//   assign i_csrrw  = op_decode_h[3'b111] & op_decode_l[4'b0011] & f3_decode[3'b001];
//   assign i_csrrs  = op_decode_h[3'b111] & op_decode_l[4'b0011] & f3_decode[3'b010];
//   // assign i_csrrc  = op_decode_h[3'b111] & op_decode_l[4'b0011] & f3_decode[3'b011];
//   // assign i_csrrwi = op_decode_h[3'b111] & op_decode_l[4'b0011] & f3_decode[3'b101];
//   // assign i_csrrsi = op_decode_h[3'b111] & op_decode_l[4'b0011] & f3_decode[3'b110];
//   // assign i_csrrci = op_decode_h[3'b111] & op_decode_l[4'b0011] & f3_decode[3'b111];
//   assign i_jalr   = op_decode_h[3'b110] & op_decode_l[4'b0111] & f3_decode[3'b000];
//   assign i_sb     = op_decode_h[3'b010] & op_decode_l[4'b0011] & f3_decode[3'b000];
//   assign i_sh     = op_decode_h[3'b010] & op_decode_l[4'b0011] & f3_decode[3'b001];
//   assign i_sw     = op_decode_h[3'b010] & op_decode_l[4'b0011] & f3_decode[3'b010];
//   assign i_add    = op_decode_h[3'b011] & op_decode_l[4'b0011] & f3_decode[3'b000] & ~(|func7);
//   assign i_sub    = op_decode_h[3'b011] & op_decode_l[4'b0011] & f3_decode[3'b000] & ~(|func7[4:0]) & func7[5] & ~func7[6];
//   assign i_xor    = op_decode_h[3'b011] & op_decode_l[4'b0011] & f3_decode[3'b100] & ~(|func7);
//   assign i_or     = op_decode_h[3'b011] & op_decode_l[4'b0011] & f3_decode[3'b110] & ~(|func7);
//   assign i_and    = op_decode_h[3'b011] & op_decode_l[4'b0011] & f3_decode[3'b111] & ~(|func7);
//   assign i_sll    = op_decode_h[3'b011] & op_decode_l[4'b0011] & f3_decode[3'b001] & ~(|func7);
//   assign i_srl    = op_decode_h[3'b011] & op_decode_l[4'b0011] & f3_decode[3'b101] & ~(|func7);
//   assign i_sra    = op_decode_h[3'b011] & op_decode_l[4'b0011] & f3_decode[3'b101] & ~(|func7[4:0]) & func7[5] & ~func7[6];
//   assign i_slt    = op_decode_h[3'b011] & op_decode_l[4'b0011] & f3_decode[3'b010] & ~(|func7);
//   assign i_sltu   = op_decode_h[3'b011] & op_decode_l[4'b0011] & f3_decode[3'b011] & ~(|func7);
//   assign i_mul    = op_decode_h[3'b011] & op_decode_l[4'b0011] & f3_decode[3'b000] & ~(|func7[6:1]) & func7[0];
//   assign i_mulh   = op_decode_h[3'b011] & op_decode_l[4'b0011] & f3_decode[3'b001] & ~(|func7[6:1]) & func7[0];
//   assign i_mulhsu = op_decode_h[3'b011] & op_decode_l[4'b0011] & f3_decode[3'b010] & ~(|func7[6:1]) & func7[0];
//   assign i_mulhu  = op_decode_h[3'b011] & op_decode_l[4'b0011] & f3_decode[3'b011] & ~(|func7[6:1]) & func7[0];
//   assign i_div    = op_decode_h[3'b011] & op_decode_l[4'b0011] & f3_decode[3'b100] & ~(|func7[6:1]) & func7[0];
//   assign i_divu   = op_decode_h[3'b011] & op_decode_l[4'b0011] & f3_decode[3'b101] & ~(|func7[6:1]) & func7[0];
//   assign i_rem    = op_decode_h[3'b011] & op_decode_l[4'b0011] & f3_decode[3'b110] & ~(|func7[6:1]) & func7[0];
//   assign i_remu   = op_decode_h[3'b011] & op_decode_l[4'b0011] & f3_decode[3'b111] & ~(|func7[6:1]) & func7[0];
//   assign i_beq    = op_decode_h[3'b110] & op_decode_l[4'b0011] & f3_decode[3'b000];
//   assign i_bne    = op_decode_h[3'b110] & op_decode_l[4'b0011] & f3_decode[3'b001];
//   assign i_blt    = op_decode_h[3'b110] & op_decode_l[4'b0011] & f3_decode[3'b100];
//   assign i_bge    = op_decode_h[3'b110] & op_decode_l[4'b0011] & f3_decode[3'b101];
//   assign i_bltu   = op_decode_h[3'b110] & op_decode_l[4'b0011] & f3_decode[3'b110];
//   assign i_bgeu   = op_decode_h[3'b110] & op_decode_l[4'b0011] & f3_decode[3'b111];
//   assign i_ecall  = op_decode_h[3'b111] & op_decode_l[4'b0011] & ~(|instr[31:7]);
//   assign i_mret   = op_decode_h[3'b111] & op_decode_l[4'b0011] & ~(|instr[31:30]) & (&instr[29:28]) & ~(|instr[27:22]) & instr[21] & ~(|instr[20:7]);
//   assign i_ebreak = op_decode_h[3'b111] & op_decode_l[4'b0011] & ~(|instr[31:21]) & instr[20] & ~(|instr[19:7]);
//   assign i_fence_i= op_decode_h[3'b000] & op_decode_l[4'b1111] & f3_decode[3'b001];

// endmodule