package playground

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi._

/* ****************************************************************
   D-Cache: 直接映射 + 写穿(write-through) + 写不分配, 4 项 × 1 个字(4B)

   结构照抄 icache: Stream 请求 / Flow 响应; dcache 独占 LSU 的 AXI 口,
   里面复用 ysyx_23060082_Axi4_Ctrler(单拍读写), 所以【不需要任何 AXI mux】。

   - 每项只有 1 个字 => 不需要突发, 也不需要多字选择
   - 只缓存 flash(0x3000_0000)与 PSRAM/SDRAM(0x8000_0000~0xbfff_ffff);
     SRAM/MROM/设备一律不介入(设备访问也不占 cache)
   - 读命中: 请求拍组合返回;
   - 读缺失: 进 ReadMiss, 等控制器读完, 填回 cache 并返回
   - 写: 一律进 Write(写穿: 命中也要写内存), 命中时【同步更新 cache 里那一个字】,
         写不分配(缺失时不占 cache, 只写内存) —— 这样 cache 永远与内存一致, 不会读到旧值
   - 面积: 4×32 数据 + 4×28 tag + 4 valid, 实测约 +1.7k um2
**************************************************************** */
case class DcacheReqData() extends Bundle {
  val write     = Bool()
  val writeData = UInt(32 bits)
  val writeMask = UInt(4 bits)
  val read      = Bool()
  val size      = UInt(3 bits)
  val addr      = UInt(32 bits)
}

case class DcacheRspData() extends Bundle {
  val readData = UInt(32 bits)
}

case class ysyx_23060082_Dcache(config: CpuConfig = CpuConfig()) extends Component {
  val io = new Bundle {
    val reqIn       = slave  Stream(DcacheReqData())
    val rspOut      = master Flow(DcacheRspData())
    val readHit     = out Bool()        // 本拍这次读真的命中(load 用它判"当拍完成")
    val writeAccept = out Bool()        // dcache接收了写数据，正在执行写
    val writeBusy   = out Bool()        // 正在后台写
    val readErr     = out Bool()
    val writeErr    = out Bool()
    val axi4        = master(Axi4(AxiConfig.axiConfig))

    val miss        = if (config.enableSimDebug) out Bool() else null   // 缺失脉冲(供 perf 计数)
    val missDone    = if (config.enableSimDebug) out Bool() else null   // 缺失完成脉冲
    
  }

  def inDcache(addr: UInt): Bool =
    (addr >= U(0x30000000L, 32 bits) && addr < U(0x40000000L, 32 bits)) ||   // flash
    (addr >= U(0x80000000L, 32 bits) && addr < U(0xc0000000L, 32 bits))      // psram + sdram

  // ================================ AXI 控制器(读写共用, 单拍) ================================ //
  val axi4Ctrler  = ysyx_23060082_Axi4_Ctrler()
  io.axi4 <> axi4Ctrler.io.axi4

  // ================================ 存储阵列: 每项 1 个字 ================================ //
  val tag       = io.reqIn.addr(31 downto 4)
  val index     = io.reqIn.addr(3 downto 2)
  val dataMem   = Reg(Vec(UInt(32 bits), 4))      // 数据
  val tagMem    = Reg(Vec(UInt(28 bits), 4))      // 标签
  val validReg  = Reg(Bits(4 bits)) init(0)       // 每项 1 位有效位
  val cacheable = inDcache(io.reqIn.addr)
  val hit       = validReg(index) && (tagMem(index) === tag) && cacheable

  // ================================ 状态机 ================================ //
  object DcacheState extends SpinalEnum {
    val Idle, ReadMiss, Write = newElement()
  }
  val state = Reg(DcacheState()) init(DcacheState.Idle)

  val bPending  = RegInit(False)          // 记录还有挂在后台的写事务
  val writeAddr = Reg(UInt(30 bits))      // 写地址
  val writeCacheable = Reg(Bool())        // 是否是内存地址，如果不是就不能挂后台

  val reqRead  = io.reqIn.valid && io.reqIn.read
  val reqWrite = io.reqIn.valid && io.reqIn.write

  val readHit  = (state === DcacheState.Idle) && reqRead && hit             // 读地址命中cache

  val sameAddr  = io.reqIn.addr(31 downto 2) === writeAddr                  // 判断是否是同一地址，后台写的话，就不能再读同一地址，需要等待写完
  val needWait  = bPending && (reqWrite || (reqRead && sameAddr && !hit))   // 后台有写事务时，再次的写请求，或对相同地址的读，需要等待之前的写完成

  val writeAccept = (state === DcacheState.Idle) && reqWrite && !needWait   // 写请求被接受，这个是在valid信号为1的同时就能判断出来的
  when(writeAccept) {
    writeAddr       := io.reqIn.addr(31 downto 2)
    writeCacheable  := cacheable
  }

  val writeSent     = !axi4Ctrler.io.axi4.aw.valid && !axi4Ctrler.io.axi4.w.valid   // valid不为高，说明发送完成
  val writePostDone = (state === DcacheState.Write) && writeSent && writeCacheable  // 写通道发送完成，并且访问的不是确实是存储，此时就可以挂后台

  when(axi4Ctrler.io.axi4.b.fire) {   // b信号返回，后台事务完成
    bPending := False
  } elsewhen(writePostDone) {         // 开始挂起
    bPending := True
  } otherwise {
    bPending := bPending
  }

  // 读的时候由于会阻塞，不会产生写信号，但是在后台写的时候下一条指令可能会产生读信号
  when(state === DcacheState.Idle) {
    when(reqRead && !hit && !needWait)  { state := DcacheState.ReadMiss }           // 读缺失，没有命中但是不需要阻塞等待，直接向控制器发出读信号
    .elsewhen(reqWrite && !needWait)    { state := DcacheState.Write    }           // 请求写，并且不需要阻塞等待
    .otherwise                            { state := DcacheState.Idle     }
  } elsewhen(state === DcacheState.ReadMiss) {                                      // 读只能等到结束
    when(axi4Ctrler.io.readEnd)           { state := DcacheState.Idle     }
    .otherwise                            { state := DcacheState.ReadMiss }
  } elsewhen(state === DcacheState.Write) {
    when(axi4Ctrler.io.writeEnd || writePostDone) { state := DcacheState.Idle  }    // 读取完成，或者可以挂后台，就返回idle状态
    .otherwise                                    { state := DcacheState.Write }
  }

  val readMissDone = (state === DcacheState.ReadMiss) && axi4Ctrler.io.readEnd      // 读事务完成

  // ================================ 响应 ================================ //
  io.reqIn.ready  := (state === DcacheState.Idle) && !needWait                      // 如果空闲且不用阻塞，就说明可以接受信号
  io.rspOut.valid := readHit || readMissDone || writeAccept                         // 读命中，或者读缺失但完成，或者请求被接受
  io.writeBusy    := (state === DcacheState.Write) || bPending                      // fence.i要等真正写入进存储
  io.writeAccept  := writeAccept

  val readDataReg     = RegNextWhen(axi4Ctrler.io.readData, readMissDone)
  io.rspOut.readData := Mux(readHit, dataMem(index),
                        Mux(readMissDone, axi4Ctrler.io.readData, readDataReg))
  io.readHit         := readHit

  // ================================ AXI ================================ //
  axi4Ctrler.io.readReq   := (state === DcacheState.Idle) && !needWait && reqRead  && !hit  // 请求信号只持续一拍
  axi4Ctrler.io.readAddr  := io.reqIn.addr
  axi4Ctrler.io.writeReq  := (state === DcacheState.Idle) && !needWait && reqWrite
  axi4Ctrler.io.writeAddr := io.reqIn.addr
  axi4Ctrler.io.writeData := io.reqIn.writeData
  axi4Ctrler.io.writeMask := io.reqIn.writeMask
  axi4Ctrler.io.size      := io.reqIn.size

  io.readErr  := axi4Ctrler.io.axi4.r.fire && (axi4Ctrler.io.axi4.r.payload.resp =/= B"2'b00")
  io.writeErr := axi4Ctrler.io.axi4.b.fire && (axi4Ctrler.io.axi4.b.payload.resp =/= B"2'b00")
  // ================================ 写穿更新 / 缺失填回 ================================ //
  // store 命中: 按字节使能改 cache 里那一个字(与发给内存的 data/mask 完全一致)
  val writeMaskFull = (io.reqIn.writeMask(3) #* 8) ## (io.reqIn.writeMask(2) #* 8) ##
                      (io.reqIn.writeMask(1) #* 8) ## (io.reqIn.writeMask(0) #* 8)

  val storeData = ((dataMem(index).asBits & ~writeMaskFull) | (io.reqIn.writeData.asBits & writeMaskFull)).asUInt

  when(readMissDone && cacheable && !io.readErr) {                   // 读缺失填回(只有可缓存地址才占 cache)
    dataMem(index)  := axi4Ctrler.io.readData
    tagMem(index)   := tag
    validReg(index) := True
  } elsewhen((state === DcacheState.Idle) && reqWrite && hit) {   // 如果是写入的地址正好命中，就更新cache
    dataMem(index)  := storeData                                
  }

  // ================================ 仿真信号 ================================ //
  if (config.enableSimDebug) {
    io.miss     := (state === DcacheState.Idle) && reqRead && cacheable && !hit
    io.missDone := readMissDone
  }
}

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
  // 加不加突发，这些数值都会是常量，不需要寄存器锁存
  io.axi4.ar.id   := U"4'b0"
  io.axi4.ar.len  := U"8'b0"          // 突发长度1  
  io.axi4.ar.size := io.size  
  io.axi4.ar.burst:= B"2'b01"         // 突发类型INCR
  // ================================ 读地址 ================================ //
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
  // when(io.axi4.r.fire && io.axi4.r.resp =/= Axi4.resp.OKAY) {
  //   report(Seq("[LSU] read resp error! resp =", io.axi4.r.resp, "addr =", io.axi4.ar.addr))
  // }

  // ================================ 写操作 ================================ //
  io.axi4.aw.id   := U"4'b0"
  io.axi4.aw.len  := U"8'b0"          // 突发长度1  
  io.axi4.aw.size := io.size       
  io.axi4.aw.burst:= B"2'b01"         // 突发类型INCR

  io.axi4.w.last  := True             // lsu没有加突发
  // ================================ 写地址 ================================ //
  val awValidReg = RegInit(False)
  val awAddrReg  = RegNextWhen(io.writeAddr, io.writeReq)
  val awValidOut = io.writeReq || awValidReg    // 提前一周期发出valid信号
  io.axi4.aw.valid := awValidOut
  io.axi4.aw.addr  := Mux(io.writeReq, io.writeAddr, awAddrReg)

  when(awValidReg) {
    when(io.axi4.aw.fire) {
      awValidReg := False
    } otherwise {
      awValidReg := True
    }
  } otherwise {
    when(io.writeReq && !io.axi4.aw.fire) {
      awValidReg := True
    } otherwise {
      awValidReg := False
    }
  }
  // ================================ 写数据 ================================ //
  val wValidReg = RegInit(False)
  val wDataReg  = RegNextWhen(io.writeData.asBits, io.writeReq)
  val wStrbReg  = RegNextWhen(io.writeMask.asBits, io.writeReq)
  val wValidOut = io.writeReq || wValidReg    // 提前一周期发出valid信号
  io.axi4.w.valid := wValidOut
  io.axi4.w.data  := Mux(io.writeReq, io.writeData.asBits, wDataReg)
  io.axi4.w.strb  := Mux(io.writeReq, io.writeMask.asBits, wStrbReg)

  when(wValidReg) {
    when(io.axi4.w.fire) {
      wValidReg := False
    } otherwise {
      wValidReg := True
    }
  } otherwise {
    when(io.writeReq && !io.axi4.w.fire) {
      wValidReg := True
    } otherwise {
      wValidReg := False
    }
  }
  // ================================ 写响应 ================================ //
  io.axi4.b.ready := io.axi4.b.valid
  io.writeEnd := io.axi4.b.fire

  // 写响应错误检查: 从机返回非 OKAY 时仿真报错
  // when(io.axi4.b.fire && io.axi4.b.resp =/= Axi4.resp.OKAY) {
  //   report(Seq("[LSU] write resp error! resp =", io.axi4.b.resp, ", addr =", io.axi4.aw.addr))
  // }
}
