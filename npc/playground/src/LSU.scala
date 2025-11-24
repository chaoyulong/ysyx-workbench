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
    val input  = slave Stream(Exu2Lsu_data())
    val output    = master Stream(Lsu2Wbu_data()) 
  }
  val memAddr    = io.input.alu_result    // alu的输出结果就是访存地址
  val rdata      = UInt(32 bits)
  val rdata_reg  = Reg(UInt(32 bits)) init(0)   

  val state = Reg(LsuState()) init(LsuState.Idle)   // 创建一个状态机
  val needMem = io.input.valid && (io.input.rf_ctrl.mem2reg || io.input.mem_ctrl.mem_wr) // 需要访问内存

  val dataProcess = ysyx_23060082_DataProcess()   // 数据处理
    dataProcess.io.addrOp := memAddr(1 downto 0) ## io.input.mem_ctrl.mem_op      // 合并 addr + MemOp 生成 5 位索引
    dataProcess.io.wdata  := io.input.rf_read_data_2 // 写数据为寄存器2的数据
    dataProcess.io.rdata  := rdata

  val mem_rw = Mem_RW()
    mem_rw.io.valid := needMem && (state === LsuState.Idle)
    mem_rw.io.wen   := io.input.mem_ctrl.mem_wr
    mem_rw.io.addr  := U(memAddr(31 downto 2) ## U"00")   // 真实地址要对齐
    mem_rw.io.wdata := dataProcess.io.wdataReal
    mem_rw.io.wmask := dataProcess.io.wmask

  when(state === LsuState.WaitMem && mem_rw.io.rw_end){ // 是读内存指令并且已读完
    rdata_reg := mem_rw.io.rdata
  } otherwise{
    rdata_reg := rdata_reg
  }
  rdata := Mux(state === LsuState.WaitMem && mem_rw.io.rw_end, mem_rw.io.rdata, rdata_reg)

  switch(state) {
    is(LsuState.Idle) {
      when(needMem) {
        state := LsuState.WaitMem
      }
    }

    is(LsuState.WaitMem) {
      when(mem_rw.io.rw_end) {
        when(io.output.fire){    // 若已经握手成功，则返回到Idle状态
          state := LsuState.Idle
        } otherwise{
          state := LsuState.Done
        }
      }
    }

    is(LsuState.Done) {
      when(io.output.fire) {
        state := LsuState.Idle
      }
    }
  }

  // -------------------------------------------------------------------- //

  // ------------------------------- csr寄存器 ------------------------------- // 
  val csr = ysyx_23060082_CSR()
    csr.io.csr_addr   := io.input.imm
    csr.io.csr_wdata  := io.input.rf_read_data_1
    csr.io.csr_cmd    := io.input.csr_ctrl.csr_cmd
    csr.io.trap_enter := io.input.csr_ctrl.trap_enter
    csr.io.trap_exit  := io.input.csr_ctrl.trap_exit
    csr.io.pc_in      := io.input.pc
    csr.io.cause_in   := io.input.rf_read_data_1

  // ----------------------- 用于握手的部分 ----------------------- //
  // willValid的意义就是当前周期就可以完成任务
  val willValid = (state === LsuState.WaitMem && mem_rw.io.rw_end) ||       // 需要访存并且访存成功
                  (state === LsuState.Idle && io.input.valid && !needMem)
  io.output.valid := io.input.valid && willValid  
  io.input.ready := (state === LsuState.Idle)
  // ----------------------- 数据传输部分 ----------------------- //
  io.output.pc          := io.input.pc
  io.output.pc_next     := Mux(io.input.csr_ctrl.trap_enter, csr.io.mtvec,
                           Mux(io.input.csr_ctrl.trap_exit, csr.io.mepc,
                               io.input.pc_next))

  io.output.mem_data_out:= Mux(io.input.csr_ctrl.csr_cmd =/= U"3'd0", csr.io.csr_rdata, dataProcess.io.rdataReal)           // 借用mem_data_out来输出读出的值
  io.output.alu_data_out:= io.input.alu_result
  io.output.rf_ctrl     := io.input.rf_ctrl    

}

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


// case class Lsu2Wbu_data() extends Bundle {
//   val pc            = UInt(32 bits)
//   val pc_next       = UInt(32 bits)
//   val mem_data_out  = UInt(32 bits)
//   val alu_data_out  = UInt(32 bits) 
//   val rf_ctrl       = RfCtrl()      // 其中的mem2reg信号会作为读内存信号被用到
// }

// case class ysyx_23060082_LSU() extends Component {
//   val io = new Bundle {
//     val input  = slave Stream(Exu2Lsu_data())
//     val output    = master Stream(Lsu2Wbu_data()) 
//   }

//   // val needMem = io.in.valid && (io.in.payload.rf_ctrl.mem2reg || io.in.payload.mem_ctrl.mem_wr) // 需要访问内存
//   val memAddr    = io.input.alu_result    // alu的输出结果就是访存地址
//   val rw_valid    = io.input.rf_ctrl.mem2reg | io.input.mem_ctrl.mem_wr
//   val rdata       = Reg(UInt(32 bits)) init(0)   
//   val lsu_end     = Reg(Bool())       // lsu结束标志

//   val dataProcess = ysyx_23060082_DataProcess()   // 数据处理
//     dataProcess.io.addrOp := memAddr(1 downto 0) ## io.input.mem_ctrl.mem_op      // 合并 addr + MemOp 生成 5 位索引
//     dataProcess.io.wdata  := io.input.rf_read_data_2 // 写数据为寄存器2的数据
//     dataProcess.io.rdata  := rdata

//   val mem_rw = Mem_RW()
//     mem_rw.io.wen   := io.input.mem_ctrl.mem_wr
//     mem_rw.io.addr  := U(memAddr(31 downto 2) ## U"2'h0")   // 真实地址要对齐
//     mem_rw.io.wdata := dataProcess.io.wdataReal
//     mem_rw.io.wmask := dataProcess.io.wmask

//   when(io.input.fire & rw_valid){  // 握手成功时判断是否需要访存
//     mem_rw.io.valid := True
//   } otherwise{
//     mem_rw.io.valid := False
//   }

//   when(io.input.rf_ctrl.mem2reg & mem_rw.io.rw_end){ // 是读内存指令并且已读完
//     rdata := mem_rw.io.rdata
//   } elsewhen(io.output.fire){ // 握手完成后置0
//     rdata := U"32'h0"
//   } otherwise{
//     rdata := rdata
//   }

//   when(io.input.fire & ~rw_valid){    // 不需要访存
//     lsu_end := True
//   } elsewhen(mem_rw.io.rw_end & rw_valid){     // 需要访存并且访存完成
//     lsu_end := True
//   } elsewhen(io.output.fire){ // 握手完成后置0
//     lsu_end := False
//   } otherwise{
//     lsu_end := lsu_end
//   }

//   // -------------------------------------------------------------------- //

//   // ------------------------------- csr寄存器 ------------------------------- // 
//   val csr = ysyx_23060082_CSR()
//     csr.io.csr_addr   := io.input.imm
//     csr.io.csr_wdata  := io.input.rf_read_data_1
//     csr.io.csr_cmd    := io.input.csr_ctrl.csr_cmd
//     csr.io.trap_enter := io.input.csr_ctrl.trap_enter
//     csr.io.trap_exit  := io.input.csr_ctrl.trap_exit
//     csr.io.pc_in      := io.input.pc
//     csr.io.cause_in   := io.input.rf_read_data_1

//   // ----------------------- 用于握手的部分 ----------------------- //
//   val willValid = lsu_end || (~rw_valid)
//   io.output.valid := io.input.valid && willValid    // io.input.valid为数据有效信号，是寄存器信号
//   io.input.ready := willValid
//   // ----------------------- 数据传输部分 ----------------------- //
//   io.output.pc          := io.input.pc
//   io.output.pc_next     := Mux(io.input.csr_ctrl.trap_enter, csr.io.mtvec,
//                            Mux(io.input.csr_ctrl.trap_exit, csr.io.mepc,
//                                io.input.pc_next))

//   io.output.mem_data_out:= Mux(io.input.csr_ctrl.csr_cmd =/= U"3'd0", csr.io.csr_rdata, dataProcess.io.rdataReal)           // 借用mem_data_out来输出读出的值
//   io.output.alu_data_out:= io.input.alu_result
//   io.output.rf_ctrl     := io.input.rf_ctrl    

// }

