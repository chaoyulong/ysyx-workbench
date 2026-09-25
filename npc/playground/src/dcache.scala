package playground

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi._

// valid ready pc 三个信号
case class DcacheReqData() extends Bundle {
  val write     = Bool()
  val writeData = UInt(32 bits)
  val writeMask = UInt(4 bits)
  val read      = Bool()
  val size      = UInt(3 bits)
  val addr      = UInt(32 bits)
}

// valid rdata 两个信号
case class DcacheRspData() extends Bundle {
  val readData = UInt(32 bits)
}

// 由于剩余面积不够，并且dcache的收益不高，所以只能实现一个极其微小的dcache
// 目前是4*1Byte，而且只有写没有读，所以fence.i不需要在这里起作用
case class ysyx_23060082_Dcache() extends Component {
  val io = new Bundle {
    val reqIn  = slave  Stream(DcacheReqData())
    val rspOut = master Flow(DcacheRspData())
    val fenceI = in Bool()    // fence.i: 清空有效位(后续取指缺失重读)
    val miss   = out Bool()   // 缺失拍脉冲(每次缺失一次), 供 IFU 的性能计数器统计命中率; STA 时无人使用会被剪掉
    val missDone = out Bool() // 缺失完成拍脉冲, 与 miss 配对可测出平均缺失代价(即 TMT 里的那一项)

    val axi4   = master(Axi4(AxiConfig.axiConfig))
  }

  def inDcache(addr: UInt): Bool = {
    // (addr >= U(0x0f000000L, 32 bits) && addr < U(0x10000000L, 32 bits)) ||    // sram
    (addr >= U(0x30000000L, 32 bits) && addr < U(0x40000000L, 32 bits)) ||    // flash
    (addr >= U(0x80000000L, 32 bits) && addr < U(0xc0000000L, 32 bits))       // psram + sdram
  }

  // ================================ axi交给dcache控制，因为1行只有1个字,所以不需要突发 ================================ //
  val axi4Ctrler  = ysyx_23060082_Axi4_Ctrler() 
  io.axi4 <> axi4Ctrler.io.axi4

  // ================================ 存储阵列 (寄存器) ================================ //
  val tag      = io.reqIn.addr(31 downto 4)
  val index    = io.reqIn.addr(3 downto 2)
  val dataMem  = Reg(Vec(UInt(32 bits), 4))   // 数据
  val tagMem   = Reg(Vec(UInt(28 bits), 4))   // 地址
  val validReg = Reg(Bits(4 bits)) init(0)    // 每块1位有效位

  // 命中判断，当前索引位有效并且tag相等
  val cacheable = inDcache(io.reqIn.addr)     // 位于cache的有效范围内
  val hit = validReg(index) && (tagMem(index) === tag) && cacheable

  // ================================ 状态机 ================================ //
  object DcacheState extends SpinalEnum {
    val Idle, ReadMiss, Write= newElement()
  }
  val state = Reg(DcacheState()) init(DcacheState.Idle)

  val reqRead  = io.reqIn.valid && io.reqIn.read
  val reqWrite = io.reqIn.valid && io.reqIn.write
  val readHit  = (state === DcacheState.Idle) && reqRead && hit

  when(state === DcacheState.Idle) {
    when(reqRead && !hit) { state := DcacheState.ReadMiss }   // 有读请求但是没有命中
    .elsewhen(reqWrite)   { state := DcacheState.Write    }   // 有写请求，就算写是命中的，重新写入
    .otherwise { state := DcacheState.Idle }
  } elsewhen(state === DcacheState.ReadMiss) {
    when(axi4Ctrler.io.readEnd) { state := DcacheState.Idle } // 访存完成, 返回
    .otherwise { state := DcacheState.ReadMiss }
  } elsewhen(state === DcacheState.Write) {
    when(axi4Ctrler.io.writeEnd) { state := DcacheState.Idle }
    .otherwise { state := DcacheState.Write }
  }

  // ================================  ================================ //
  // 请求握手: Idle时接受(命中同拍组合返回rspOut, 读缺失进入ReadMiss，或写内存进入Write)
  io.reqIn.ready := (state === DcacheState.Idle)

  val readMissDone = (state === DcacheState.ReadMiss) && axi4Ctrler.io.readEnd
  val writeDone    = (state === DcacheState.Write)    && axi4Ctrler.io.writeEnd

  io.miss     := (state === DcacheState.Idle) && reqRead && !hit
  io.missDone := readMissDone

  // ================================  ================================ //
  val readDataReg = RegNextWhen(axi4Ctrler.io.readData, readMissDone)
  io.rspOut.readData := Mux(readHit, dataMem(index), 
                        Mux(readMissDone, axi4Ctrler.io.readData, readDataReg))   // 命中返回cache,未命中返回读完的数据

  io.rspOut.valid    := readHit || readMissDone || writeDone      // 读命中，或者读缺失但是完成，或者写完

  // ============ AXI ============ //
  axi4Ctrler.io.readReq   := (state === DcacheState.Idle) && reqRead && !hit
  axi4Ctrler.io.readAddr  := io.reqIn.addr
  axi4Ctrler.io.writeReq  := (state === DcacheState.Idle) && reqWrite
  axi4Ctrler.io.writeAddr := io.reqIn.addr
  axi4Ctrler.io.writeData := io.reqIn.writeData
  axi4Ctrler.io.writeMask := io.reqIn.writeMask
  axi4Ctrler.io.size      := io.reqIn.size

  // 写入内存的同时也要写入cache
  val storeData = UInt(32 bits)
  storeData := dataMem(index)                                  // 默认保持, 再按字节使能改
  for (b <- 0 until 4) {
    when(io.reqIn.writeMask(b)) {
      storeData(b * 8 + 7 downto b * 8) := io.reqIn.writeData(b * 8 + 7 downto b * 8)
    }
  }

  when(readMissDone && cacheable) {                           // 读缺失: 写回数据 + tag + valid
    dataMem(index)  := axi4Ctrler.io.readData
    tagMem(index)   := tag
    validReg(index) := True
  } elsewhen((state === DcacheState.Idle) && reqWrite && hit) {
    dataMem(index)     := storeData                           // 同步更新写入数据
  }
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
  // io.axi4.ar.valid.setAsReg() init(False)
  // io.axi4.ar.addr .setAsReg()

  // 加不加突发，这些数值都会是常量，不需要寄存器锁存
  io.axi4.ar.id   := U"4'b0"
  io.axi4.ar.len  := U"8'b0"          // 突发长度1  
  io.axi4.ar.size := io.size  
  io.axi4.ar.burst:= B"2'b01"         // 突发类型INCR
  // ================================ 读地址 ================================ //
  // when(io.readReq) {
  //   io.axi4.ar.valid := True
  // } elsewhen(io.axi4.ar.fire) {
  //   io.axi4.ar.valid := False
  // } otherwise {
  //   io.axi4.ar.valid := io.axi4.ar.valid
  // }

  // when(io.readReq) {
  //   io.axi4.ar.addr := io.readAddr
  // } otherwise {
  //   io.axi4.ar.addr := io.axi4.ar.addr 
  // }

  val arValidReg = RegInit(False)
  val arAddrReg  = RegNextWhen(io.readAddr, io.readReq)
  val arValidOut = io.readReq || arValidReg    // 提前一周期发出arvalid信号
  io.axi4.ar.valid := arValidOut
  io.axi4.ar.addr  := Mux(io.readReq, io.readAddr, arAddrReg)

  when(arValidReg) {
    when(io.axi4.ar.fire) {
      arValidReg := False
    } otherwise {
      arValidReg := True
    }
  } otherwise {
    when(io.readReq && !io.axi4.ar.fire) {
      arValidReg := True
    } otherwise {
      arValidReg := False
    }
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
