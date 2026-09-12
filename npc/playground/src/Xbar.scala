package playground

import spinal.core._
import spinal.lib._       // 使用spinal的模块库
import spinal.lib.bus.amba4.axi._

// --------------------------------- 地址映射配置 --------------------------------- //
// clint 地址可能会修改, 统一在此定义, 修改时只需改动这里
object AddressMap {
  val CLINT_BASE = BigInt("02000000", 16)   // clint 基地址
  val CLINT_SIZE = BigInt("00010000", 16)   // clint 地址空间大小 64KB
  val CLINT_END  = CLINT_BASE + CLINT_SIZE - 1

  // clint 内部寄存器偏移
  val CLINT_MTIME  = BigInt("00000000", 16)   // mtime 低位 (0x02000000)
  val CLINT_MTIMEH = BigInt("00000004", 16)   // mtime 高位 (0x02000004)

  def isClint(addr: UInt) = addr >= U(CLINT_BASE, 32 bits) && addr <= U(CLINT_END, 32 bits)
}

// --------------------------------- AXI4Xbar --------------------------------- //
case class ysyx_23060082_AXI4Xbar() extends Component {
  val io = new Bundle {
    val ifuAxi4     = slave(Axi4ReadOnly(AxiConfig.axiConfig))
    val lsuAxi4     = slave(Axi4(AxiConfig.axiConfig))
    val clintAxi4   = master(Axi4(AxiConfig.axiConfig))
    val externalAxi4= master(Axi4(AxiConfig.axiConfig))
  }

  val busAxi4  = Axi4(AxiConfig.axiConfig)

// --------------------------------------------------------- Arbiter ------------------------------------------------------ //
  object ArbiterState extends SpinalEnum {              // 定义状态机枚举
    val Idle, IfuUsing, LsuUsing = newElement()
  }
  val arbiterState = Reg(ArbiterState()) init(ArbiterState.Idle)   // 创建一个状态机

  switch(arbiterState) {
    is(ArbiterState.Idle) {
      when(io.ifuAxi4.ar.valid) { arbiterState := ArbiterState.IfuUsing }
      .elsewhen (io.lsuAxi4.ar.valid) { arbiterState := ArbiterState.LsuUsing }
      .otherwise( arbiterState := ArbiterState.Idle)
    }
    is (ArbiterState.IfuUsing){
      when(io.ifuAxi4.r.fire && io.ifuAxi4.r.last) { arbiterState := ArbiterState.Idle }  // 握手成功并且是最后一个数据才算完成
      .otherwise( arbiterState := ArbiterState.IfuUsing)
    }
    is (ArbiterState.LsuUsing){
      when(io.lsuAxi4.r.fire && io.lsuAxi4.r.last) { arbiterState := ArbiterState.Idle }
      .otherwise( arbiterState := ArbiterState.LsuUsing)
    }
  }

  busAxi4.ar.payload  := Mux(arbiterState === ArbiterState.IfuUsing, io.ifuAxi4.ar.payload, io.lsuAxi4.ar.payload)
  busAxi4.ar.valid    := (arbiterState === ArbiterState.IfuUsing && io.ifuAxi4.ar.valid) ||
                         (arbiterState === ArbiterState.LsuUsing && io.lsuAxi4.ar.valid)
  io.ifuAxi4.ar.ready := (arbiterState === ArbiterState.IfuUsing && busAxi4.ar.ready) 
  io.lsuAxi4.ar.ready := (arbiterState === ArbiterState.LsuUsing && busAxi4.ar.ready) 

  io.ifuAxi4.r.payload := busAxi4.r.payload
  io.lsuAxi4.r.payload := busAxi4.r.payload
  io.ifuAxi4.r.valid := (arbiterState === ArbiterState.IfuUsing && busAxi4.r.valid)
  io.lsuAxi4.r.valid := (arbiterState === ArbiterState.LsuUsing && busAxi4.r.valid)
  busAxi4.r.ready    := (arbiterState === ArbiterState.IfuUsing && io.ifuAxi4.r.ready) ||
                        (arbiterState === ArbiterState.LsuUsing && io.lsuAxi4.r.ready)

  busAxi4.aw <> io.lsuAxi4.aw   // 写通道直连
  busAxi4.w <> io.lsuAxi4.w
  busAxi4.b <> io.lsuAxi4.b

// --------------------------------------------------------- crossbar ------------------------------------------------------ //
  // 读/写各自独立的状态机: 读事务(ar/r)与写事务(aw/w/b)可以并行, 互不阻塞(符合AXI语义)
  object CrossState extends SpinalEnum {              // 定义状态机枚举
    val Idle, Clint, External = newElement()
  }
  // 读状态机: 路由 ar/r 通道, 由 r.fire 结束事务
  val readState = Reg(CrossState()) init(CrossState.Idle)
  switch(readState) {
    is(CrossState.Idle) {
      when(busAxi4.ar.valid) {
        when(AddressMap.isClint(busAxi4.ar.addr)) { readState := CrossState.Clint }
        .otherwise { readState := CrossState.External }
      } otherwise {
        readState := CrossState.Idle
      }
    }
    is(CrossState.Clint, CrossState.External) {
      when(busAxi4.r.fire && busAxi4.r.last) { readState := CrossState.Idle }
      .otherwise { readState := readState }
    }
  }

  // 写状态机: 路由 aw/w/b 通道, 由 b.fire 结束事务
  val writeState = Reg(CrossState()) init(CrossState.Idle)
  switch(writeState) {
    is(CrossState.Idle) {
      when(busAxi4.aw.valid) {
        when(AddressMap.isClint(busAxi4.aw.addr)) { writeState := CrossState.Clint }
        .otherwise { writeState := CrossState.External }
      } otherwise {
        writeState := CrossState.Idle
      }
    }
    is(CrossState.Clint, CrossState.External) {
      when(busAxi4.b.fire) { writeState := CrossState.Idle }
      .otherwise { writeState := writeState }
    }
  }

  // ---------------------------------------------- 读通道路由 (readState) ---------------------------------------------- //
  // ------------------------------- 读地址 ------------------------------- //
  io.clintAxi4.ar.valid := (readState === CrossState.Clint) && busAxi4.ar.valid   
  io.clintAxi4.ar.payload := busAxi4.ar.payload

  io.externalAxi4.ar.valid := (readState === CrossState.External) && busAxi4.ar.valid
  io.externalAxi4.ar.payload := busAxi4.ar

  busAxi4.ar.ready := (readState === CrossState.Clint && io.clintAxi4.ar.ready) ||
                      (readState === CrossState.External && io.externalAxi4.ar.ready)
  // ------------------------------- 读数据 ------------------------------- //
  busAxi4.r.valid  := (readState === CrossState.Clint && io.clintAxi4.r.valid) ||
                      (readState === CrossState.External && io.externalAxi4.r.valid)
  busAxi4.r.payload := Mux(readState === CrossState.Clint, io.clintAxi4.r.payload, io.externalAxi4.r.payload)
  io.clintAxi4.r.ready := (readState === CrossState.Clint) && busAxi4.r.ready

  io.externalAxi4.r.ready := (readState === CrossState.External) && busAxi4.r.ready
  // ---------------------------------------------- 写通道路由 (writeState) ---------------------------------------------- //
  // ------------------------------- 写地址 ------------------------------- //
  io.clintAxi4.aw.valid := (writeState === CrossState.Clint && busAxi4.aw.valid)
  io.clintAxi4.aw.payload := busAxi4.aw.payload

  io.externalAxi4.aw.valid := (writeState === CrossState.External && busAxi4.aw.valid)
  io.externalAxi4.aw.payload := busAxi4.aw.payload

  busAxi4.aw.ready := (writeState === CrossState.Clint && io.clintAxi4.aw.ready) ||
                      (writeState === CrossState.External && io.externalAxi4.aw.ready)
  // ------------------------------- 写数据 ------------------------------- //
  io.clintAxi4.w.valid := (writeState === CrossState.Clint && busAxi4.w.valid)
  io.clintAxi4.w.payload := busAxi4.w.payload

  io.externalAxi4.w.valid := (writeState === CrossState.External && busAxi4.w.valid)
  io.externalAxi4.w.payload := busAxi4.w.payload

  busAxi4.w.ready := (writeState === CrossState.Clint && io.clintAxi4.w.ready) ||
                     (writeState === CrossState.External && io.externalAxi4.w.ready)
  // ------------------------------- 写响应 ------------------------------- //
  busAxi4.b.valid := (writeState === CrossState.Clint && io.clintAxi4.b.valid) ||
                      (writeState === CrossState.External && io.externalAxi4.b.valid)
  busAxi4.b.payload := Mux(writeState === CrossState.Clint, io.clintAxi4.b.payload, io.externalAxi4.b.payload)
  io.clintAxi4.b.ready := (writeState === CrossState.Clint && busAxi4.b.ready)

  io.externalAxi4.b.ready := (writeState === CrossState.External && busAxi4.b.ready)
}
