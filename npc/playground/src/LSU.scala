package playground

import spinal.core._
import spinal.lib._    // 使用spinal的模块库
import spinal.lib.bus.amba4.axi._

case class Lsu2Wbu_data() extends Bundle {
  val pc            = UInt(32 bits)
  val pcNext       = UInt(32 bits)
  val mem_data_out  = UInt(32 bits)
  val alu_data_out  = UInt(32 bits) 
  val rfCtrl       = RfCtrl()      // 其中的mem2reg信号会作为读内存信号被用到
  val fenceI       = Bool()        // fence.i(直通, WBU 据此通知 IFU 失效 icache)
}


case class ysyx_23060082_LSU(config: CpuConfig = CpuConfig()) extends Component {
  val io = new Bundle {
    val input     = slave  Flow(Exu2Lsu_data())
    val output    = master Stream(Lsu2Wbu_data()) 
    val axi4 = master(Axi4(AxiConfig.axiConfig))
  }
  // ================================ 输入信号整理 ================================ //
  object LsuState extends SpinalEnum {
    val Idle, WaitMem, Done = newElement()          // lsu等待读写完成的状态机
  }
  val state = Reg(LsuState()) init(LsuState.Idle)   // 创建一个状态机
  val memAddr   = io.input.aluResult                // alu的输出结果就是访存地址
  val needRead  = io.input.valid && io.input.rfCtrl.mem2reg  // 需要读内存
  val needWrite = io.input.valid && io.input.memCtrl.memWr   // 需要写内存
  val needMem   = needRead || needWrite                       // 需要访问内存

  // ================================ 访存通路 ================================ //
  val dataProcess = ysyx_23060082_DataProcess()   // 数据处理
  val axi4Ctrler  = ysyx_23060082_Axi4_Ctrler()   // AXI总线控制
  // ---- 数据处理连接 ----
  dataProcess.io.addrOp := memAddr(1 downto 0) ## io.input.memCtrl.memOp    // 合并 addr + MemOp 生成 5 位索引
  dataProcess.io.wdata  := io.input.rfReadData2                               // 写数据为寄存器2的数据
  // ---- AXI控制器连接 ----
  io.axi4 <> axi4Ctrler.io.axi4
  axi4Ctrler.io.readReq  := needRead  && (state === LsuState.Idle)
  axi4Ctrler.io.writeReq := needWrite && (state === LsuState.Idle)
  axi4Ctrler.io.size     := (False ## io.input.memCtrl.memOp(1 downto 0)).asUInt
  axi4Ctrler.io.readAddr := memAddr
  axi4Ctrler.io.writeAddr:= memAddr
  axi4Ctrler.io.writeData:= dataProcess.io.wdataReal  // 处理后的数据
  axi4Ctrler.io.writeMask:= dataProcess.io.wmask
  // ---- 访存结束信号 ----
  val rdEnd = (state === LsuState.WaitMem) && axi4Ctrler.io.readEnd && io.input.rfCtrl.mem2reg  // 读内存结束, 需要更新数据
  val wrEnd = (state === LsuState.WaitMem) && axi4Ctrler.io.writeEnd && io.input.memCtrl.memWr
  val rdataReg = RegNextWhen(axi4Ctrler.io.readData, rdEnd) init(0)  // 是读内存指令并且已读完
  dataProcess.io.rdata  := Mux(rdEnd, axi4Ctrler.io.readData, rdataReg) // 快一周期读完

  // ================================ lsu状态机 ================================ //
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
  // ================================ CSR寄存器 ================================ //
  val csr = ysyx_23060082_CSR()
  csr.io.csrAddr    := io.input.csrAddr
  csr.io.csrWdata   := io.input.rfReadData1
  csr.io.csrCmd     := io.input.csrCtrl.csrCmd
  csr.io.trapEnter  := io.input.csrCtrl.trapEnter
  csr.io.trapExit   := io.input.csrCtrl.trapExit
  csr.io.pcIn       := io.input.pc
  csr.io.causeIn    := Mux(io.input.csrCtrl.illegal, U(2),
                        Mux(io.input.csrCtrl.ebreak,  U(3), io.input.rfReadData1))
  csr.io.instrRetire := io.output.fire    // 指令传出LSU即计数(比写回提前1拍, 总数正确)

  // ================================ 用于握手的部分 ================================ //
  // willValid的意义就是当前周期就可以完成任务
  val willValid = (rdEnd || wrEnd) ||                       // 需要访存并且访存成功
                  (state === LsuState.Done) ||
                  (state === LsuState.Idle && io.input.valid && !needMem)
  io.output.valid := io.input.valid && willValid  

  // ================================ 数据传输部分 ================================ //
  io.output.pc          := io.input.pc
  io.output.pcNext     := Mux(io.input.csrCtrl.trapEnter, csr.io.mtvec,
                           Mux(io.input.csrCtrl.trapExit, csr.io.mepc,
                               io.input.pcNext))

  io.output.mem_data_out:= Mux(io.input.csrCtrl.csrCmd =/= U"3'd0", csr.io.csrRdata, dataProcess.io.rdataReal)           // 借用mem_data_out来输出读出的值
  io.output.alu_data_out:= io.input.aluResult
  io.output.rfCtrl     := io.input.rfCtrl    
  io.output.fenceI     := io.input.fenceI

  // ==================== 仿真专用: LSU 访存性能统计(仅仿真, 4 组: mem/dev × 读/写) ====================
  // 内存范围(两平台统一): flash 0x30000000-0x3fffffff + psram 0x80000000-0x9fffffff + sdram 0xa0000000-0xbfffffff
  if (config.enableSimDebug) {
    def isDevAddr(addr: UInt) =
      !((addr >= U("32'h30000000") && addr < U("32'h40000000")) ||
        (addr >= U("32'h80000000") && addr < U("32'hc0000000")))
    val arAddr = io.axi4.ar.addr
    val awAddr = io.axi4.aw.addr
    val perf = PerfReg()
    perf.io.valid := True
    perf.io.evt   := B"8'b0"
    perf.io.req(0) := io.axi4.ar.fire && !isDevAddr(arAddr)   // mem 读请求
    perf.io.req(1) := io.axi4.aw.fire && !isDevAddr(awAddr)   // mem 写请求
    perf.io.req(2) := io.axi4.ar.fire &&  isDevAddr(arAddr)   // dev 读请求
    perf.io.req(3) := io.axi4.aw.fire &&  isDevAddr(awAddr)   // dev 写请求
    // 请求拍锁存类别, 响应拍按类别配对(否则 r/b.fire 无法区分是哪个请求的响应)
    val pendingIsDev = Reg(Bool()) init(False)
    pendingIsDev := Mux(io.axi4.ar.fire, isDevAddr(arAddr),
                    Mux(io.axi4.aw.fire, isDevAddr(awAddr), pendingIsDev))
    perf.io.rsp(0) := io.axi4.r.fire && !pendingIsDev   // mem 读响应
    perf.io.rsp(1) := io.axi4.b.fire && !pendingIsDev   // mem 写响应
    perf.io.rsp(2) := io.axi4.r.fire &&  pendingIsDev   // dev 读响应
    perf.io.rsp(3) := io.axi4.b.fire &&  pendingIsDev   // dev 写响应
  }
}


// ================================ 数据处理单元 ================================ //
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
// ================================ 读操作 ================================ //
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
// ================================ 写操作 ================================ //
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
case class ysyx_23060082_Axi4_Ctrler() extends Component {
  val io = new Bundle {
    val readReq   = in Bool()
    val writeReq  = in Bool()
    val size      = in UInt(3 bits)
    val readAddr  = in UInt(32 bits)
    val writeAddr = in UInt(32 bits)
    val writeData = in UInt(32 bits)
    val writeMask = in UInt(4 bits)
    val readEnd   = out Bool()
    val readData  = out UInt(32 bits)
    val writeEnd  = out Bool()
    val axi4 = master(Axi4(AxiConfig.axiConfig))
  }
  // ================================ 读操作 ================================ //
  io.axi4.ar.valid.setAsReg() init(False)
  io.axi4.ar.addr .setAsReg()

  // 加不加突发，这些数值都会是常量，不需要寄存器锁存
  io.axi4.ar.id   := U"4'b0"
  io.axi4.ar.len  := U"8'b0"          // 突发长度1  
  io.axi4.ar.size := io.size  
  io.axi4.ar.burst:= B"2'b01"         // 突发类型INCR
  // ================================ 读地址 ================================ //
  when(io.readReq) {
    io.axi4.ar.valid := True
  } elsewhen(io.axi4.ar.fire) {
    io.axi4.ar.valid := False
  } otherwise {
    io.axi4.ar.valid := io.axi4.ar.valid
  }

  when(io.readReq) {
    io.axi4.ar.addr := io.readAddr
  } otherwise {
    io.axi4.ar.addr := io.axi4.ar.addr 
  }

  // ================================ 读数据 ================================ //
  io.axi4.r.ready := io.axi4.r.valid
  io.readEnd := io.axi4.r.fire && io.axi4.r.last   // 突发结束(r.last)才算读完
  io.readData := io.axi4.r.data.asUInt

  // 读响应错误检查: 从机返回非 OKAY 时仿真报错
  when(io.axi4.r.fire && io.axi4.r.resp =/= Axi4.resp.OKAY) {
    report(Seq("[LSU] read resp error! resp =", io.axi4.r.resp, "addr =", io.axi4.ar.addr))
  }

  // ================================ 写操作 ================================ //
  io.axi4.aw.valid.setAsReg() init(False)
  io.axi4.aw.addr .setAsReg()

  io.axi4.w.valid .setAsReg() init(False)
  io.axi4.w.data  .setAsReg()
  io.axi4.w.strb  .setAsReg()
  // io.axi4.w.last  .setAsReg()
  io.axi4.w.last := True  

  io.axi4.aw.id   := U"4'b0"
  io.axi4.aw.len  := U"8'b0"          // 突发长度1  
  io.axi4.aw.size := io.size       
  io.axi4.aw.burst:= B"2'b01"         // 突发类型INCR
  // ================================ 写地址 ================================ //
  when(io.writeReq) {
    io.axi4.aw.valid := True
  } elsewhen(io.axi4.aw.fire) {
    io.axi4.aw.valid := False
  } otherwise {
    io.axi4.aw.valid := io.axi4.aw.valid
  }

  when(io.writeReq) {
    io.axi4.aw.addr := io.writeAddr
  } otherwise {
    io.axi4.aw.addr := io.axi4.aw.addr 
  }
  // ================================ 写数据 ================================ //
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
  } otherwise {
    io.axi4.w.data := io.axi4.w.data
    io.axi4.w.strb := io.axi4.w.strb
  }
  // ================================ 写响应 ================================ //
  io.axi4.b.ready := io.axi4.b.valid
  io.writeEnd := io.axi4.b.fire

  // 写响应错误检查: 从机返回非 OKAY 时仿真报错
  when(io.axi4.b.fire && io.axi4.b.resp =/= Axi4.resp.OKAY) {
    report(Seq("[LSU] write resp error! resp =", io.axi4.b.resp, ", addr =", io.axi4.aw.addr))
  }

}
