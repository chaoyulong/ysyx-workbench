package playground

import spinal.core._

case class LSU_RW() extends BlackBox{
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
  val rdata = mem_rw.io.rdata

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

  val mem_rw = Mem_RW()

  mem_rw.io.valid := io.mem2reg | io.mem_wr
  mem_rw.io.wen   := io.mem_wr
  mem_rw.io.addr  := io.mem_addr
  mem_rw.io.wdata := wdata_real
  mem_rw.io.wmask := wmask


  io.rdata := rdata_real

}

