package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库
import spinal.lib.bus.amba4.axi._

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

// case class ysyx_23060082_LSU() extends Component {
//   val io = new Bundle {
//     val input     = slave  Flow(Exu2Lsu_data())
//     val output    = master Stream(Lsu2Wbu_data()) 
//   }
//   val state = Reg(LsuState()) init(LsuState.Idle)   // 创建一个状态机
//   val memAddr    = io.input.alu_result    // alu的输出结果就是访存地址
//   val needMem = io.input.valid && (io.input.rf_ctrl.mem2reg || io.input.mem_ctrl.mem_wr) // 需要访问内存

//   val dataProcess = ysyx_23060082_DataProcess()   // 数据处理
//   val mem_rw = Mem_RW()
//   val rdEnd = (state === LsuState.WaitMem) && mem_rw.io.rw_end && io.input.rf_ctrl.mem2reg  // 读内存结束，需要更新数据
//   val rdata_reg = RegNextWhen(mem_rw.io.rdata, rdEnd) init(0)  // 是读内存指令并且已读完

//     dataProcess.io.addrOp := memAddr(1 downto 0) ## io.input.mem_ctrl.mem_op      // 合并 addr + MemOp 生成 5 位索引
//     dataProcess.io.wdata  := io.input.rf_read_data_2 // 写数据为寄存器2的数据
//     dataProcess.io.rdata  := Mux(state === LsuState.WaitMem && mem_rw.io.rw_end, mem_rw.io.rdata, rdata_reg)

//     mem_rw.io.valid := needMem && (state === LsuState.Idle)
//     mem_rw.io.wen   := io.input.mem_ctrl.mem_wr
//     mem_rw.io.addr  := U(memAddr(31 downto 2) ## U"00")   // 真实地址要对齐
//     mem_rw.io.wdata := dataProcess.io.wdataReal
//     mem_rw.io.wmask := dataProcess.io.wmask
//   // ------------------------------------- 状态机 ------------------------------------- // 
//   switch(state) {
//     is(LsuState.Idle) {
//       when(needMem) {state := LsuState.WaitMem}      
//       .otherwise{state := state} 
//     }
//     is(LsuState.WaitMem) {
//       when(mem_rw.io.rw_end) {
//         when(io.output.fire){state := LsuState.Idle}     // 若已经握手成功，则返回到Idle状态
//         .otherwise{state := LsuState.Done}
//       }
//       .otherwise{state := state}
//     }
//     is(LsuState.Done) {
//       when(io.output.fire) {state := LsuState.Idle}   
//       .otherwise{state := state}     
//     }
//   }
//   // ----------------------------------- csr寄存器 ----------------------------------- // 
//   val csr = ysyx_23060082_CSR()
//     csr.io.csr_addr   := io.input.imm
//     csr.io.csr_wdata  := io.input.rf_read_data_1
//     csr.io.csr_cmd    := io.input.csr_ctrl.csr_cmd
//     csr.io.trap_enter := io.input.csr_ctrl.trap_enter
//     csr.io.trap_exit  := io.input.csr_ctrl.trap_exit
//     csr.io.pc_in      := io.input.pc
//     csr.io.cause_in   := io.input.rf_read_data_1

//   // --------------------------------- 用于握手的部分 --------------------------------- //
//   // willValid的意义就是当前周期就可以完成任务
//   val willValid = (state === LsuState.WaitMem && mem_rw.io.rw_end) ||       // 需要访存并且访存成功
//                   (state === LsuState.Done) ||
//                   (state === LsuState.Idle && io.input.valid && !needMem)
//   io.output.valid := io.input.valid && willValid  
//   // ---------------------------------- 数据传输部分 ---------------------------------- //
//   io.output.pc          := io.input.pc
//   io.output.pc_next     := Mux(io.input.csr_ctrl.trap_enter, csr.io.mtvec,
//                            Mux(io.input.csr_ctrl.trap_exit, csr.io.mepc,
//                                io.input.pc_next))

//   io.output.mem_data_out:= Mux(io.input.csr_ctrl.csr_cmd =/= U"3'd0", csr.io.csr_rdata, dataProcess.io.rdataReal)           // 借用mem_data_out来输出读出的值
//   io.output.alu_data_out:= io.input.alu_result
//   io.output.rf_ctrl     := io.input.rf_ctrl    
// }

case class ysyx_23060082_LSU() extends Component {
  val io = new Bundle {
    val input     = slave  Flow(Exu2Lsu_data())
    val output    = master Stream(Lsu2Wbu_data()) 
    val axi4 = master(Axi4(AxiConfig.axiConfig))
    val axiBusReq = out Bool()     // 占用总线请求
  }
  val state = Reg(LsuState()) init(LsuState.Idle)   // 创建一个状态机
  val memAddr    = io.input.aluResult    // alu的输出结果就是访存地址
  val needRead  = io.input.valid && io.input.rf_ctrl.mem2reg  // 需要读内存
  val needWrite = io.input.valid && io.input.mem_ctrl.memWr   // 需要写内存
  val needMem   = needRead || needWrite                       // 需要访问内存
  // ------------------------------------- 内存控制器 ------------------------------------- // 
  val dataProcess = ysyx_23060082_DataProcess()   // 数据处理
  val axiCtrl = ysyx_23060082_AXI_Ctrl()          // AXI总线控制

  val rdEnd = (state === LsuState.WaitMem) && axiCtrl.io.readEnd && io.input.rf_ctrl.mem2reg  // 读内存结束，需要更新数据
  val wrEnd = (state === LsuState.WaitMem) && axiCtrl.io.writeEnd && io.input.mem_ctrl.memWr
  val rdataReg = RegNextWhen(axiCtrl.io.readData, rdEnd) init(0)  // 是读内存指令并且已读完
  dataProcess.io.addrOp := memAddr(1 downto 0) ## io.input.mem_ctrl.memOp      // 合并 addr + MemOp 生成 5 位索引
  dataProcess.io.wdata  := io.input.rfReadData2 // 写数据为寄存器2的数据
  dataProcess.io.rdata  := Mux(state === LsuState.WaitMem && rdEnd, axiCtrl.io.readData, rdataReg)

  io.axi4 <> axiCtrl.io.axi4
  axiCtrl.io.readReq  := needRead  && (state === LsuState.Idle)
  axiCtrl.io.writeReq := needWrite && (state === LsuState.Idle)
  axiCtrl.io.memOp    := io.input.mem_ctrl.memOp
  axiCtrl.io.readAddr := memAddr
  axiCtrl.io.writeAddr:= memAddr
  axiCtrl.io.writeData:= dataProcess.io.wdataReal // 处理后的数据
  axiCtrl.io.writeMask:= dataProcess.io.wmask

  when(axiCtrl.io.readReq || axiCtrl.io.writeReq) {    // 发出请求信号
    io.axiBusReq := True
  } elsewhen(axiCtrl.io.readEnd) {
    io.axiBusReq := False
  } otherwise {
    io.axiBusReq := io.axiBusReq
  }
  // ------------------------------------- 状态机 ------------------------------------- // 
  switch(state) {
    is(LsuState.Idle) {
      when(needMem) {state := LsuState.WaitMem}      
      .otherwise{state := state} 
    }
    is(LsuState.WaitMem) {
      when(rdEnd || wrEnd) {
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
    csr.io.csr_wdata  := io.input.rfReadData1
    csr.io.csr_cmd    := io.input.csr_ctrl.csr_cmd
    csr.io.trap_enter := io.input.csr_ctrl.trap_enter
    csr.io.trap_exit  := io.input.csr_ctrl.trap_exit
    csr.io.pc_in      := io.input.pc
    csr.io.cause_in   := io.input.rfReadData1

  // --------------------------------- 用于握手的部分 --------------------------------- //
  // willValid的意义就是当前周期就可以完成任务
  val willValid = (state === LsuState.WaitMem && rdEnd || wrEnd) ||       // 需要访存并且访存成功
                  (state === LsuState.Done) ||
                  (state === LsuState.Idle && io.input.valid && !needMem)
  io.output.valid := io.input.valid && willValid  
  // ---------------------------------- 数据传输部分 ---------------------------------- //
  io.output.pc          := io.input.pc
  io.output.pc_next     := Mux(io.input.csr_ctrl.trap_enter, csr.io.mtvec,
                           Mux(io.input.csr_ctrl.trap_exit, csr.io.mepc,
                               io.input.pc_next))

  io.output.mem_data_out:= Mux(io.input.csr_ctrl.csr_cmd =/= U"3'd0", csr.io.csr_rdata, dataProcess.io.rdataReal)           // 借用mem_data_out来输出读出的值
  io.output.alu_data_out:= io.input.aluResult
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


/* ****************************************************************
  axi总线控制器
**************************************************************** */
case class ysyx_23060082_AXI_Ctrl() extends Component {
  val io = new Bundle {
    val readReq   = in Bool()
    val writeReq  = in Bool()
    val memOp     = in UInt(3 bits)
    val readAddr  = in UInt(32 bits)
    val writeAddr = in UInt(32 bits)
    val writeData = in UInt(32 bits)
    val writeMask = in UInt(4 bits)
    val readEnd   = out Bool()
    val readData  = out UInt(32 bits)
    val writeEnd  = out Bool()
    val axi4 = master(Axi4(AxiConfig.axiConfig))
  }

  // ------------------------------- 读操作 ------------------------------- //
  io.axi4.ar.valid.setAsReg() init(False)
  io.axi4.ar.addr .setAsReg() init(0)
  io.axi4.ar.id   .setAsReg() init(0)
  io.axi4.ar.len  .setAsReg() init(0)
  io.axi4.ar.size .setAsReg() init(0)
  io.axi4.ar.burst.setAsReg() init(0)
  // ---------------- 读地址 ---------------- //
  when(io.readReq) {
    io.axi4.ar.valid := True
  } elsewhen(io.axi4.ar.fire) {
    io.axi4.ar.valid := False
  } otherwise {
    io.axi4.ar.valid := io.axi4.ar.valid
  }

  when(io.readReq) {
    io.axi4.ar.addr := io.readAddr
    io.axi4.ar.id   := U"4'b0"
    io.axi4.ar.len  := U"8'b0"          // 突发长度1  
    io.axi4.ar.size := io.memOp       
    io.axi4.ar.burst:= B"2'b01"         // 突发类型INCR
  } otherwise {
    io.axi4.ar.addr := io.axi4.ar.addr 
    io.axi4.ar.id   := io.axi4.ar.id 
    io.axi4.ar.len  := io.axi4.ar.len   // 突发长度1  
    io.axi4.ar.size := io.axi4.ar.size  
    io.axi4.ar.burst:= io.axi4.ar.burst // 突发类型INCR
  }
  // ---------------- 读数据 ---------------- //
  io.axi4.r.ready := io.axi4.r.valid
  io.readEnd := io.axi4.r.fire
  io.readData := io.axi4.r.data.asUInt

  // ------------------------------- 写操作 ------------------------------- //
  io.axi4.aw.valid.setAsReg() init(False)
  io.axi4.aw.addr .setAsReg() init(0)
  io.axi4.aw.id   .setAsReg() init(0)
  io.axi4.aw.len  .setAsReg() init(0)
  io.axi4.aw.size .setAsReg() init(0)
  io.axi4.aw.burst.setAsReg() init(0)

  io.axi4.w.valid .setAsReg() init(False)
  io.axi4.w.data  .setAsReg() init(0)
  io.axi4.w.strb  .setAsReg() init(0)
  io.axi4.w.last  .setAsReg() init(False)
  // ---------------- 写地址 ---------------- //
  when(io.writeReq) {
    io.axi4.aw.valid := True
  } elsewhen(io.axi4.aw.fire) {
    io.axi4.aw.valid := False
  } otherwise {
    io.axi4.aw.valid := io.axi4.aw.valid
  }

  when(io.writeReq) {
    io.axi4.aw.addr := io.writeAddr
    io.axi4.aw.id   := U"4'b0"
    io.axi4.aw.len  := U"8'b0"          // 突发长度1  
    io.axi4.aw.size := io.memOp       
    io.axi4.aw.burst:= B"2'b01"         // 突发类型INCR
  } otherwise {
    io.axi4.aw.addr := io.axi4.aw.addr 
    io.axi4.aw.id   := io.axi4.aw.id 
    io.axi4.aw.len  := io.axi4.aw.len   // 突发长度1  
    io.axi4.aw.size := io.axi4.aw.size  
    io.axi4.aw.burst:= io.axi4.aw.burst // 突发类型INCR
  }
  // ---------------- 写数据 ---------------- //
  when(io.writeReq) {
    io.axi4.w.valid := True
  } elsewhen(io.axi4.w.fire) {
    io.axi4.w.valid := False
  } otherwise {
    io.axi4.w.valid := io.axi4.w.valid
  }

  when(io.writeReq) {
    io.axi4.w.data := io.writeData.asBits
    io.axi4.w.strb := io.writeMask.asBits
    io.axi4.w.last := True  
  } otherwise {
    io.axi4.w.data := io.axi4.w.data
    io.axi4.w.strb := io.axi4.w.strb
    io.axi4.w.last := io.axi4.w.last
  }
  // ---------------- 写响应 ---------------- //
  io.axi4.b.ready := io.axi4.b.valid
  io.writeEnd := io.axi4.b.fire
}