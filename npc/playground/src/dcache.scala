package playground

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi._

/* ****************************************************************
   D-Cache: 直接映射 + 写穿(write-through) + 写不分配, 2 行 × 16B(每行 4 个字)

   为什么改成 2 行 × 16B(来自 exp/dcache-dse 分支的 trace 与 DSE, 见 PERF.md):
   - 数据访问空间局部性极好: 88.5% 的 16B 行 4 个字全用到(平均 3.74 个字)
   - 单拍读要 115.81 拍, 其中约 87 拍是"与写穿 store 抢同一条 AXI 口"的排队, 设备延迟只约 29 拍
   - 每多一拍突发代价 10.09 拍(实测) => 用 4 拍突发一次取回整行, 每字代价大幅摊薄
   - 2 行 × 16B 的存储 = 2×(128+27+1) = 312 触发器(旧版 4 项 × 1 字 = 244) => 增量约 +400~500 um2

   结构照抄 icache: Stream 请求 / Flow 响应; dcache 独占 LSU 的 AXI 口。
   - 读命中: 请求拍组合返回"行内被选中的那个字"
   - 读缺失: 进 ReadMiss, 逐拍把突发的数据直接写进 dataMem 的那一行(不额外占 128 位寄存器),
             完成后置 valid/tag, 并返回请求的那个字(它可能早于最后一拍到达, 所以单独记下来)
     · 只有 psram/sdram(0x8000_0000~0xbfff_ffff)才突发: flash 挂 APB, 突发会被拆成单拍
     · 不可缓存地址(设备/SRAM 等)仍按单拍读, 且 size 用请求本身的 size
   - 写: 写穿 + 写不分配; 命中时按字节使能只改"行内那一个字", 其余 3 个字保持
         (行变大后这一步是正确性关键: 否则行内会残留旧值)
**************************************************************** */
case class DcacheParams(
  lines:     Int = 2,       // 行数(直接映射)
  lineBytes: Int = 16       // 行大小(字节) => 一次缺失突发 lineBytes/4 拍
) {
  val lineBits  = log2Up(lineBytes)
  val indexBits = log2Up(lines)
  val tagBits   = 32 - lineBits - indexBits
  val words     = lineBytes / 4
  val wordBits  = scala.math.max(log2Up(words), 1)   // log2Up(1)=0, 兜到 1
  val dataBits  = lineBytes * 8
}

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

case class ysyx_23060082_Dcache(config: CpuConfig = CpuConfig(), param: DcacheParams = DcacheParams()) extends Component {
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
  // 只对 psram/sdram 突发: flash 走 APB(不支持突发, 会被 AXI4Fragmenter 拆成单拍, 反而更慢)
  def burstable(addr: UInt): Bool =
    addr >= U(0x80000000L, 32 bits) && addr < U(0xc0000000L, 32 bits)

  // ================================ AXI 控制器(读写共用; 读支持突发) ================================ //
  val axi4Ctrler  = ysyx_23060082_Axi4_Ctrler()
  io.axi4 <> axi4Ctrler.io.axi4

  // ================================ 存储阵列: 每行 param.words 个字 ================================ //
  val tag      = io.reqIn.addr(31 downto param.lineBits + param.indexBits)
  val index    = io.reqIn.addr(param.lineBits + param.indexBits - 1 downto param.lineBits)
  val wordSel  = io.reqIn.addr(param.lineBits - 1 downto 2)            // 行内第几个字
  val dataMem  = Reg(Vec(UInt(param.dataBits bits), param.lines))      // 数据(整行)
  val tagMem   = Reg(Vec(UInt(param.tagBits bits), param.lines))       // 标签
  val validReg = Reg(Bits(param.lines bits)) init(0)                   // 每行 1 位有效位

  val cacheable = inDcache(io.reqIn.addr)
  val hit       = validReg(index) && (tagMem(index) === tag) && cacheable
  // 这次读缺失是否要突发填整行(单字行时退化为原来的单拍读)
  val burstFill = if (param.words > 1) cacheable && burstable(io.reqIn.addr) else False

  // 命中的字: 行内每个字先取出来, 再用 wordSel 选(动态切片不能直接做, 需要 mux 树)
  val hitWordVec = Vec(UInt(32 bits), param.words)
  for (i <- 0 until param.words) hitWordVec(i) := dataMem(index)(i*32 + 31 downto i*32)
  val hitWord = hitWordVec(wordSel)

  // ================================ 状态机 ================================ //
  object DcacheState extends SpinalEnum {
    val Idle, ReadMiss, Write = newElement()
  }
  val state = Reg(DcacheState()) init(DcacheState.Idle)

  val reqRead  = io.reqIn.valid && io.reqIn.read
  val reqWrite = io.reqIn.valid && io.reqIn.write
  val readHit  = (state === DcacheState.Idle) && reqRead && hit
  val enterReadMiss = (state === DcacheState.Idle) && reqRead && !hit

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
  if (config.enableSimDebug) {
    io.miss     := (state === DcacheState.Idle) && reqRead && cacheable && !hit
    io.missDone := readMissDone
  }

  // ================================ 突发读: 逐拍收数据 ================================ //
  // (必须定义在"响应"之前: Scala 的 val 是先用后声明会拿到 null)
  val beatFire = axi4Ctrler.io.readBeat                           // 本拍收到一个 R beat
  val beatCnt  = Reg(UInt(param.wordBits bits)) init(0)           // 进缺失后已收到几个 beat
  when(enterReadMiss) { beatCnt := 0 }
  .elsewhen(beatFire) { beatCnt := beatCnt + 1 }

  // 逐拍直接把 beat 写进该行(省掉 128 位装配寄存器); 因为 readEnd 那拍 dataMem 还没更新,
  // 所以"请求的那个字"要单独记下来 —— 突发时它在 beat == wordSel 那拍到达, 单拍读就是那唯一一拍
  val missWordReg = Reg(UInt(32 bits))
  when(beatFire) {
    when(!burstFill || (beatCnt === wordSel)) { missWordReg := axi4Ctrler.io.readData }
    when(burstFill) {
      for (w <- 0 until param.words) {
        when(beatCnt === U(w, param.wordBits bits)) {
          dataMem(index)(w*32 + 31 downto w*32) := axi4Ctrler.io.readData
        }
      }
    }
  }

  // ================================ 响应 ================================ //
  val writeAccept = (state === DcacheState.Idle) && reqWrite      // 请求被接受
  io.rspOut.valid := readHit || readMissDone || writeAccept       // 不等到写完
  io.writeAccept  := writeAccept                                  //
  io.writeBusy    := (state === DcacheState.Write)
  io.rspOut.readData := Mux(readHit, hitWord, missWordReg)        // 写入那一拍 readData 无意义
  io.readHit         := readHit

  // ================================ AXI ================================ //
  axi4Ctrler.io.readReq   := enterReadMiss                      // 只持续一拍
  axi4Ctrler.io.readAddr  := Mux(burstFill,                     // 突发必须从"行首"开始
                             (io.reqIn.addr(31 downto param.lineBits) ## U(0, param.lineBits bits)).asUInt,
                             io.reqIn.addr)
  axi4Ctrler.io.readLen   := Mux(burstFill, U(param.words - 1, 8 bits), U(0, 8 bits))
  axi4Ctrler.io.readSize  := Mux(burstFill, Axi4Define.size.BYTE_4, io.reqIn.size)
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
  // store 命中: 只改"行内那一个字", 其余字保持(与发给内存的 data/mask 完全一致)
  val lineOld = dataMem(index)
  val lineNew = UInt(param.dataBits bits)
  for (w <- 0 until param.words) {
    val m = UInt(32 bits)
    m := lineOld(w*32 + 31 downto w*32)                 // 默认保持该字
    when(wordSel === U(w, param.wordBits bits)) {
      for (b <- 0 until 4) {
        when(io.reqIn.writeMask(b)) {
          m(b*8 + 7 downto b*8) := io.reqIn.writeData(b*8 + 7 downto b*8)
        }
      }
    }
    lineNew(w*32 + 31 downto w*32) := m
  }

  when(readMissDone && cacheable && !readErr) {         // 读缺失: 数据已逐拍写入, 这里只置 tag/valid
    tagMem(index)   := tag
    validReg(index) := True
  } elsewhen((state === DcacheState.Idle) && reqWrite && hit) {
    dataMem(index) := lineNew                           // store 命中: 同步更新, valid 不动
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
    val readLen   = in UInt(8 bits)      // 读突发长度-1(0 = 单拍); dcache 整行填充时给 words-1
    val readSize  = in UInt(3 bits)      // 读的 size(突发固定 BYTE_4; 单拍用请求的 size)
    val readBeat  = out Bool()           // 本拍收到一个 R beat(供 dcache 逐拍拼整行)
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
  io.axi4.ar.len  := io.readLen       // 单拍给 0; 整行填充给 words-1
  io.axi4.ar.size := io.readSize      // 突发固定 BYTE_4
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
  io.readBeat := io.axi4.r.fire                    // 本拍收到一个 beat(单拍读也是一拍)
  io.readEnd  := io.axi4.r.fire && io.axi4.r.last  // 突发结束(r.last)才算读完
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
