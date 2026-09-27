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
    val reqIn    = slave  Stream(DcacheReqData())
    val rspOut   = master Flow(DcacheRspData())
    val readHit  = out Bool()       // 本拍这次读真的命中(load 用它判"当拍完成")
    val writeAccept = out Bool()    // dcache接收了写数据，正在执行写
    val writeBusy= out Bool()       // 正在后台写
    val readErr   = out Bool()
    val writeErr   = out Bool() 
    val miss     = if (config.enableSimDebug) out Bool() else null   // 缺失脉冲(供 perf 计数)
    val missDone = if (config.enableSimDebug) out Bool() else null   // 缺失完成脉冲
    val axi4     = master(Axi4(AxiConfig.axiConfig))
  }

  def inDcache(addr: UInt): Bool =
    (addr >= U(0x30000000L, 32 bits) && addr < U(0x40000000L, 32 bits)) ||   // flash
    (addr >= U(0x80000000L, 32 bits) && addr < U(0xc0000000L, 32 bits))      // psram + sdram

  // ================================ AXI 控制器(读写共用, 单拍) ================================ //
  val axi4Ctrler  = ysyx_23060082_Axi4_Ctrler()
  io.axi4 <> axi4Ctrler.io.axi4

  // ================================ 存储阵列: 每项 1 个字 ================================ //
  val tag      = io.reqIn.addr(31 downto 4)
  val index    = io.reqIn.addr(3 downto 2)
  val dataMem  = Reg(Vec(UInt(32 bits), 4))    // 数据
  val tagMem   = Reg(Vec(UInt(28 bits), 4))    // 标签
  val validReg = Reg(Bits(4 bits)) init(0)     // 每项 1 位有效位

  val cacheable = inDcache(io.reqIn.addr)
  val hit       = validReg(index) && (tagMem(index) === tag) && cacheable

  // ================================ 状态机 ================================ //
  object DcacheState extends SpinalEnum {
    val Idle, ReadMiss, Write = newElement()
  }
  val state = Reg(DcacheState()) init(DcacheState.Idle)

  val reqRead  = io.reqIn.valid && io.reqIn.read
  val reqWrite = io.reqIn.valid && io.reqIn.write
  val readHit  = (state === DcacheState.Idle) && reqRead && hit

  when(state === DcacheState.Idle) {
    when(reqRead && !hit) { state := DcacheState.ReadMiss }   // 读缺失
    .elsewhen(reqWrite)   { state := DcacheState.Write    }   // 写: 命中也要写内存
    .otherwise            { state := DcacheState.Idle     }
  } elsewhen(state === DcacheState.ReadMiss) {
    when(axi4Ctrler.io.readEnd)  { state := DcacheState.Idle }
    .otherwise                   { state := DcacheState.ReadMiss }
  } elsewhen(state === DcacheState.Write) {
    when(axi4Ctrler.io.writeEnd) { state := DcacheState.Idle }
    .otherwise                   { state := DcacheState.Write }
  }

  // 只在 Idle 接受请求(忙时下游会等; LSU 也必须等 reqIn.fire 才离开 Idle)
  io.reqIn.ready := (state === DcacheState.Idle)

  val readMissDone = (state === DcacheState.ReadMiss) && axi4Ctrler.io.readEnd
  val writeDone    = (state === DcacheState.Write)    && axi4Ctrler.io.writeEnd
  if (config.enableSimDebug) {
    io.miss     := (state === DcacheState.Idle) && reqRead && cacheable && !hit
    io.missDone := readMissDone
  }

  // ================================ 响应 ================================ //
  val writeAccept = (state === DcacheState.Idle) && reqWrite      // 请求被接受
  io.rspOut.valid := readHit || readMissDone || writeAccept       // 不等到写完
  io.writeAccept  := writeAccept                                  //
  io.writeBusy    := (state === DcacheState.Write)

  val readDataReg = RegNextWhen(axi4Ctrler.io.readData, readMissDone)
  io.rspOut.readData := Mux(readHit, dataMem(index),
                        Mux(readMissDone, axi4Ctrler.io.readData, readDataReg))
  io.readHit         := readHit

  // ================================ AXI ================================ //
  axi4Ctrler.io.readReq   := (state === DcacheState.Idle) && reqRead && !hit   // 只持续一拍
  axi4Ctrler.io.readAddr  := io.reqIn.addr
  axi4Ctrler.io.writeReq  := (state === DcacheState.Idle) && reqWrite
  axi4Ctrler.io.writeAddr := io.reqIn.addr
  axi4Ctrler.io.writeData := io.reqIn.writeData
  axi4Ctrler.io.writeMask := io.reqIn.writeMask
  axi4Ctrler.io.size      := io.reqIn.size

  val readErr  = axi4Ctrler.io.axi4.r.fire && (axi4Ctrler.io.axi4.r.payload.resp =/= B"2'b00")
  val writeErr = axi4Ctrler.io.axi4.b.fire && (axi4Ctrler.io.axi4.b.payload.resp =/= B"2'b00")
  io.readErr  := readErr
  io.writeErr := writeErr
  // ================================ 写穿更新 / 缺失填回 ================================ //
  // store 命中: 按字节使能改 cache 里那一个字(与发给内存的 data/mask 完全一致)
  val storeData = UInt(32 bits)
  storeData := dataMem(index)                                  // 默认保持
  for (b <- 0 until 4) {
    when(io.reqIn.writeMask(b)) {
      storeData(b * 8 + 7 downto b * 8) := io.reqIn.writeData(b * 8 + 7 downto b * 8)
    }
  }

  when(readMissDone && cacheable && !readErr) {               // 读缺失填回(只有可缓存地址才占 cache)
    dataMem(index)  := axi4Ctrler.io.readData
    tagMem(index)   := tag
    validReg(index) := True
  } elsewhen((state === DcacheState.Idle) && reqWrite && hit) {
    dataMem(index) := storeData                                // store 命中: 同步更新, valid 不动
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
