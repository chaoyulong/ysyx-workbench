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

object LsuState extends SpinalEnum {
  val Idle, WaitMem, Done = newElement()
}

case class ysyx_23060082_LSU() extends Component {
  val io = new Bundle {
    val input     = slave  Flow(Exu2Lsu_data())
    val output    = master Stream(Lsu2Wbu_data()) 
  }
  val state = Reg(LsuState()) init(LsuState.Idle)   // 创建一个状态机
  val memAddr    = io.input.alu_result    // alu的输出结果就是访存地址
  val needMem = io.input.valid && (io.input.rf_ctrl.mem2reg || io.input.mem_ctrl.mem_wr) // 需要访问内存

  val dataProcess = ysyx_23060082_DataProcess()   // 数据处理
  val mem_rw = Mem_RW()
  val rdEnd = (state === LsuState.WaitMem) && mem_rw.io.rw_end && io.input.rf_ctrl.mem2reg  // 读内存结束，需要更新数据
  val rdata_reg = RegNextWhen(mem_rw.io.rdata, rdEnd) init(0)  // 是读内存指令并且已读完

    dataProcess.io.addrOp := memAddr(1 downto 0) ## io.input.mem_ctrl.mem_op      // 合并 addr + MemOp 生成 5 位索引
    dataProcess.io.wdata  := io.input.rf_read_data_2 // 写数据为寄存器2的数据
    dataProcess.io.rdata  := Mux(state === LsuState.WaitMem && mem_rw.io.rw_end, mem_rw.io.rdata, rdata_reg)

    mem_rw.io.valid := needMem && (state === LsuState.Idle)
    mem_rw.io.wen   := io.input.mem_ctrl.mem_wr
    mem_rw.io.addr  := U(memAddr(31 downto 2) ## U"00")   // 真实地址要对齐
    mem_rw.io.wdata := dataProcess.io.wdataReal
    mem_rw.io.wmask := dataProcess.io.wmask
  // ------------------------------------- 状态机 ------------------------------------- // 
  switch(state) {
    is(LsuState.Idle) {
      when(needMem) {state := LsuState.WaitMem}      
      .otherwise{state := state} 
    }
    is(LsuState.WaitMem) {
      when(mem_rw.io.rw_end) {
        when(io.output.fire){state := LsuState.Idle}     // 若已经握手成功，则返回到Idle状态
        .otherwise{state := LsuState.Done}
      }
      .otherwise{state := state}
    }
    is(LsuState.Done) {
      when(io.output.fire) {state := LsuState.Idle}   
      .otherwise{state := state}     
    }
  }
  // ----------------------------------- csr寄存器 ----------------------------------- // 
  val csr = ysyx_23060082_CSR()
    csr.io.csr_addr   := io.input.imm
    csr.io.csr_wdata  := io.input.rf_read_data_1
    csr.io.csr_cmd    := io.input.csr_ctrl.csr_cmd
    csr.io.trap_enter := io.input.csr_ctrl.trap_enter
    csr.io.trap_exit  := io.input.csr_ctrl.trap_exit
    csr.io.pc_in      := io.input.pc
    csr.io.cause_in   := io.input.rf_read_data_1

  // --------------------------------- 用于握手的部分 --------------------------------- //
  // willValid的意义就是当前周期就可以完成任务
  val willValid = (state === LsuState.WaitMem && mem_rw.io.rw_end) ||       // 需要访存并且访存成功
                  (state === LsuState.Done) ||
                  (state === LsuState.Idle && io.input.valid && !needMem)
  io.output.valid := io.input.valid && willValid  
  // ---------------------------------- 数据传输部分 ---------------------------------- //
  io.output.pc          := io.input.pc
  io.output.pc_next     := Mux(io.input.csr_ctrl.trap_enter, csr.io.mtvec,
                           Mux(io.input.csr_ctrl.trap_exit, csr.io.mepc,
                               io.input.pc_next))

  io.output.mem_data_out:= Mux(io.input.csr_ctrl.csr_cmd =/= U"3'd0", csr.io.csr_rdata, dataProcess.io.rdataReal)           // 借用mem_data_out来输出读出的值
  io.output.alu_data_out:= io.input.alu_result
  io.output.rf_ctrl     := io.input.rf_ctrl    
}

// ---------------------------------- 数据处理单元 ---------------------------------- //
case class ysyx_23060082_DataProcess() extends Component {
  val io = new Bundle {
    val addrOp    = in  Bits( 5 bits)
    val wdata     = in  UInt(32 bits)
    val wdataReal = out UInt(32 bits)
    val wmask     = out UInt( 4 bits)
    val rdata     = in  UInt(32 bits)
    val rdataReal = out UInt(32 bits)
  }

  val wdata = io.wdata
  val rdata = io.rdata
// ------------------------------- 读操作 ------------------------------- //
  io.rdataReal := io.addrOp.mux(
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
// ------------------------------- 写操作 ------------------------------- //
  io.wdataReal := io.addrOp.mux(
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
    B"11000" -> wdata(7 downto 0) ## U"24'h0"            ,
    default  -> B"32'h0"
  ).asUInt

  io.wmask := io.addrOp.mux(
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
}

