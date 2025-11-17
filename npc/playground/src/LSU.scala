package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库

case class Lsu2Wbu_data() extends Bundle {
  val pc            = UInt(32 bits)
  val pc_next       = UInt(32 bits)
  val mem_data_out  = UInt(32 bits)
  val alu_data_out  = UInt(32 bits) 
  val rf_ctrl       = RfCtrl()      // 其中的mem2reg信号会作为读内存信号被用到
}

case class ysyx_23060082_LSU() extends Component {
  val io = new Bundle {
    val from_Exu  = slave Stream(Exu2Lsu_data())
    val to_Wbu    = master Stream(Lsu2Wbu_data()) 
  }

  val mem_addr    = io.from_Exu.alu_result    // alu的输出结果就是访存地址

  val wmask       = UInt( 4 bits)
  val rdata       = Reg(UInt(32 bits)) init(0)   
  val rw_end      = Reg(Bool())
  val wdata       = UInt(32 bits)     
  val rdata_real  = UInt(32 bits)    // 真正读出的数据
  val wdata_real  = UInt(32 bits)    // 真正写入的数据  
  val addr_real   = (mem_addr(31 downto 2) ## U"2'h0").asUInt   // 真实地址要对齐

  wdata := io.from_Exu.rf_read_data_2 // 写数据为寄存器2的数据
  val mem_rw = Mem_RW()
  mem_rw.io.valid := io.from_Exu.rf_ctrl.mem2reg | io.from_Exu.mem_ctrl.mem_wr
  mem_rw.io.wen   := io.from_Exu.mem_ctrl.mem_wr
  mem_rw.io.addr  := addr_real
  mem_rw.io.wdata := wdata_real
  mem_rw.io.wmask := wmask
  // rdata := mem_rw.io.rdata
  when(io.from_Exu.rf_ctrl.mem2reg & mem_rw.io.rw_end){ // 是读内存指令并且已读完
    rdata := mem_rw.io.rdata
  } elsewhen(io.to_Wbu.fire){ // 握手完成后置0
    rdata := U"32'h0"
  } otherwise{
    rdata := rdata
  }

  when(mem_rw.io.rw_end){     // 读取完成后valid置1
    rw_end := True
  } elsewhen(io.to_Wbu.fire){ // 握手完成后置0
    rw_end := False
  } otherwise{
    rw_end := rw_end
  }

  // 合并 addr + MemOp 生成 5 位索引
  val addr_op = (mem_addr(1 downto 0) ## io.from_Exu.mem_ctrl.mem_op)

  // ------------------ 读操作 ------------------
  rdata_real := addr_op.mux(
    B"00010" -> rdata                                       ,   // LW
    B"00001" -> rdata(15 downto  0).asSInt.resize(32).asUInt,   // LH
    B"00000" -> rdata( 7 downto  0).asSInt.resize(32).asUInt,   // LB
    B"00101" -> rdata(15 downto  0).resize(32)              ,   // LHU
    B"00100" -> rdata( 7 downto  0).resize(32)              ,   // LBU
    // mem_addr[1:0] = 01
    B"01001" -> rdata(23 downto  8).asSInt.resize(32).asUInt,
    B"01000" -> rdata(15 downto  8).asSInt.resize(32).asUInt,
    B"01101" -> rdata(23 downto  8).resize(32)              ,
    B"01100" -> rdata(15 downto  8).resize(32)              ,
    // mem_addr[1:0] = 10
    B"10001" -> rdata(31 downto 16).asSInt.resize(32).asUInt,
    B"10000" -> rdata(23 downto 16).asSInt.resize(32).asUInt,
    B"10101" -> rdata(31 downto 16).resize(32)              ,
    B"10100" -> rdata(23 downto 16).resize(32)              ,
    // mem_addr[1:0] = 11
    B"11000" -> rdata(31 downto 24).asSInt.resize(32).asUInt,
    B"11100" -> rdata(31 downto 24).resize(32)              ,
    default  -> U"32'h0"
  )
  // ------------------ 写操作 ------------------
  wdata_real := addr_op.mux(
    // mem_addr[1:0] = 00
    B"00010" -> wdata.asBits                             ,// SW
    B"00001" -> U"16'h0" ## wdata(15 downto 0)           ,// SH
    B"00000" -> U"24'h0" ## wdata( 7 downto 0)           ,// SB
    // mem_addr[1:0] = 01
    B"01001" -> U"8'h0"  ## wdata(15 downto 0) ## U"8'h0",
    B"01000" -> U"16'h0" ## wdata( 7 downto 0) ## U"8'h0",
    // mem_addr[1:0] = 10
    B"10001" -> wdata(15 downto 0) ## U"16'h0"           ,
    B"10000" -> U"8'h0" ## wdata(7 downto 0) ## U"16'h0" ,
    // mem_addr[1:0] = 11
    B"11000" -> wdata(7 downto 0) ## U"24'h0"             ,
    default  -> B"32'h0"
  ).asUInt

  wmask := addr_op.mux(
    // mem_addr[1:0] = 00
    B"00010" -> U"1111" ,   // SW
    B"00001" -> U"0011" ,   // SH
    B"00000" -> U"0001" ,   // SB
    // mem_addr[1:0] = 01
    B"01001" -> U"0110" ,
    B"01000" -> U"0010" ,
    // mem_addr[1:0] = 10
    B"10001" -> U"1100" ,
    B"10000" -> U"0100" ,
    // mem_addr[1:0] = 11
    B"11000" -> U"1000" ,
    default  -> U"0000"
  )

  io.to_Wbu.pc          := io.from_Exu.pc
  io.to_Wbu.pc_next     := io.from_Exu.pc_next
  io.to_Wbu.mem_data_out:= rdata_real
  io.to_Wbu.alu_data_out:= io.from_Exu.alu_result
  io.to_Wbu.rf_ctrl     := io.from_Exu.rf_ctrl    

  io.from_Exu.ready := io.from_Exu.valid
  // io.to_Wbu.valid   := io.from_Exu.valid
  io.to_Wbu.valid   := rw_end | ~(io.from_Exu.rf_ctrl.mem2reg | io.from_Exu.mem_ctrl.mem_wr)  // 不是访存指令或已经读完
}

