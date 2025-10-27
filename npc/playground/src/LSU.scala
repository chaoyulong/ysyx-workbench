package playground

import spinal.core._

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
    // val to_Wbu = master Stream(Lsu2Wbu_data()) 
  }

  val mem_addr    = io.from_Exu.alu_result    // alu的输出结果就是访存地址

  val wmask       = UInt( 4 bits)
  val rdata       = UInt(32 bits)    
  val wdata       = UInt(32 bits)     
  val rdata_real  = UInt(32 bits)    // 真正读出的数据
  val wdata_real  = UInt(32 bits)    // 真正写入的数据  
  val addr_real   = (mem_addr(31 downto 2) ## U"2'h0").asUInt   // 真实地址要对齐

  val mem_rw = Mem_RW()
  rdata := mem_rw.io.rdata
  wdata := io.from_Exu.rf_read_data_2 // 写数据为寄存器2的数据
  

  mem_rw.io.valid := io.from_Exu.rf_ctrl.mem2reg | io.from_Exu.mem_ctrl.mem_wr
  mem_rw.io.wen   := io.from_Exu.mem_ctrl.mem_wr
  mem_rw.io.addr  := addr_real
  mem_rw.io.wdata := wdata_real
  mem_rw.io.wmask := wmask


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
    B"00001" -> U"16'h0" ## wdata(15 downto 0)    ,// SH
    B"00000" -> U"24'h0" ## wdata(7 downto 0)     ,// SB
    // mem_addr[1:0] = 01
    B"01001" -> U"8'h0"  ## wdata(15 downto 0) ## U"8'h0",
    B"01000" -> U"16'h0" ## wdata(7 downto 0)  ## U"8'h0",
    // mem_addr[1:0] = 10
    B"10001" -> wdata(15 downto 0) ## U"16'h0"        ,
    B"10000" -> U"8'h0" ## wdata(7 downto 0) ## U"16'h0",

    // mem_addr[1:0] = 11
    B"11000" -> wdata(7 downto 0) ## U"24'h0",

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
    default  -> U"4'h0"
  )

  // io.to_Wbu.pc          := io.from_Exu.pc
  // io.to_Wbu.pc_next     := io.from_Exu.pc_next

  io.from_Exu.ready := io.from_Exu.valid

}



case class LSU_RW() extends Component{
  val io = new Bundle{
    val mem2reg  = in Bool()
    val mem_wr   = in Bool()
    val mem_op   = in UInt(3 bits)
    val mem_addr = in UInt(32 bits)
    val mem_wdata= in UInt(32 bits)

    val rdata = out UInt(32 bits)
  }

  val wdata = io.mem_wdata
  val rdata_real = UInt(32 bits)
  val wdata_real = UInt(32 bits)
  val wmask      = UInt(4 bits)
  val rdata      = UInt(32 bits)

  val mem_rw = Mem_RW()
  mem_rw.io.valid := io.mem2reg | io.mem_wr
  mem_rw.io.wen   := io.mem_wr
  mem_rw.io.addr  := io.mem_addr
  mem_rw.io.wdata := wdata_real
  mem_rw.io.wmask := wmask
  rdata := mem_rw.io.rdata

  io.rdata := rdata_real

  // 合并 addr + MemOp 生成 5 位索引
  val addr_op = (io.mem_addr(1 downto 0) ## io.mem_op).asUInt

  // ------------------ 读操作 ------------------
  switch(addr_op) {
    // mem_addr[1:0] = 00
    is(U"00010") { rdata_real := rdata }                                  // LW
    is(U"00001") { rdata_real := rdata(15 downto 0).asSInt.resize(32).asUInt } // LH
    is(U"00000") { rdata_real := rdata(7 downto 0).asSInt.resize(32).asUInt }  // LB
    is(U"00101") { rdata_real := rdata(15 downto 0).resize(32) }          // LHU
    is(U"00100") { rdata_real := rdata(7 downto 0).resize(32) }           // LBU

    // mem_addr[1:0] = 01
    is(U"01001") { rdata_real := rdata(23 downto 8).asSInt.resize(32).asUInt }
    is(U"01000") { rdata_real := rdata(15 downto 8).asSInt.resize(32).asUInt }
    is(U"01101") { rdata_real := rdata(23 downto 8).resize(32) }
    is(U"01100") { rdata_real := rdata(15 downto 8).resize(32) }

    // mem_addr[1:0] = 10
    is(U"10001") { rdata_real := rdata(31 downto 16).asSInt.resize(32).asUInt }
    is(U"10000") { rdata_real := rdata(23 downto 16).asSInt.resize(32).asUInt }
    is(U"10101") { rdata_real := rdata(31 downto 16).resize(32) }
    is(U"10100") { rdata_real := rdata(23 downto 16).resize(32) }

    // mem_addr[1:0] = 11
    is(U"11000") { rdata_real := rdata(31 downto 24).asSInt.resize(32).asUInt }
    is(U"11100") { rdata_real := rdata(31 downto 24).resize(32) }

    default { rdata_real := U"32'h0" }
  }

  // ------------------ 写操作 ------------------
  switch(addr_op) {
    // mem_addr[1:0] = 00
    is(U"00010") { wdata_real := wdata;                                       wmask := U"1111" } // SW
    is(U"00001") { wdata_real := (U"16'h0" ## wdata(15 downto 0)).asUInt; wmask := U"0011" } // SH
    is(U"00000") { wdata_real := (U"24'h0" ## wdata(7 downto 0) ).asUInt; wmask := U"0001" } // SB

    // mem_addr[1:0] = 01
    is(U"01001") { wdata_real := (U"8'h0"  ## wdata(15 downto 0) ## U"8'h0").asUInt;  wmask := U"0110" }
    is(U"01000") { wdata_real := (U"16'h0" ## wdata(7 downto 0)  ## U"8'h0").asUInt;  wmask := U"0010" }

    // mem_addr[1:0] = 10
    is(U"10001") { wdata_real := (wdata(15 downto 0) ## U"16'h0").asUInt;             wmask := U"1100" }
    is(U"10000") { wdata_real := (U"8'h0" ## wdata(7 downto 0) ## U"16'h0").asUInt;   wmask := U"0100" }

    // mem_addr[1:0] = 11
    is(U"11000") { wdata_real := (wdata(7 downto 0) ## U"24'h0").asUInt;              wmask := U"1000" }

    default { wdata_real := U"32'h0"; wmask := U"4'b0" }
  }

}

