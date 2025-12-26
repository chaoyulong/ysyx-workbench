package playground

import spinal.core._
import spinal.lib._       // 使用spinal的模块库
import spinal.lib.bus.amba4.axi._

  // 一个空的axi总线示例，可能会有什么用先留着
  // val axi4Empty = Axi4(AxiConfig.axiConfig)
  // axi4Empty.ar.valid := False
  // axi4Empty.ar.ready := False
  // axi4Empty.ar.addr  := U(0)
  // axi4Empty.ar.id    := U(0)
  // axi4Empty.ar.len   := U(0)
  // axi4Empty.ar.size  := U(0)
  // axi4Empty.ar.burst := B(0)

  // axi4Empty.r.valid  := False
  // axi4Empty.r.ready  := False
  // axi4Empty.r.data   := B(0)
  // axi4Empty.r.resp   := B(0)
  // axi4Empty.r.last   := False
  
  // axi4Empty.aw.valid := False
  // axi4Empty.aw.ready := False
  // axi4Empty.aw.addr  := U(0)
  // axi4Empty.aw.id    := U(0)
  // axi4Empty.aw.len   := U(0)
  // axi4Empty.aw.size  := U(0)
  // axi4Empty.aw.burst := B(0)

  // axi4Empty.w.valid := False
  // axi4Empty.w.ready := False
  // axi4Empty.w.data  := B(0)
  // // axi4Empty.w.id    := U(0)
  // axi4Empty.w.strb  := B(0)
  // axi4Empty.w.last  := False

  // axi4Empty.b.valid  := False
  // axi4Empty.b.ready  := False
  // axi4Empty.b.resp   := B(0)
  // axi4Empty.b.id   := U(0)

case class ysyx_23060082_AXI4Xbar() extends Component {
  val io = new Bundle {
    val ifuAXI4     = slave(Axi4ReadOnly(AxiConfig.axiConfig))
    val ifuAXI4Req  = in Bool()
    val lsuAXI4     = slave(Axi4(AxiConfig.axiConfig))
    val lsuAXI4Req  = in Bool()
    val clintAxi    = master(Axi4(AxiConfig.axiConfig))
    val externalAxi = master(Axi4(AxiConfig.axiConfig))
  }

  val axi4Bus  = Axi4(AxiConfig.axiConfig)

// --------------------------------------------------------- Arbiter ------------------------------------------------------ //
  object ArbiterState extends SpinalEnum {              // 定义状态机枚举
    val Idle, IfuUsing, LsuUsing = newElement()
  }
  val arbiterState = Reg(ArbiterState()) init(ArbiterState.Idle)   // 创建一个状态机
  // val axi4Bus = io.externalAxi

  switch(arbiterState) {
    is(ArbiterState.Idle) {
      when(io.ifuAXI4.ar.valid) { arbiterState := ArbiterState.IfuUsing }
      .elsewhen (io.lsuAXI4.ar.valid) { arbiterState := ArbiterState.LsuUsing }
      .otherwise( arbiterState := ArbiterState.Idle)
    }
    is (ArbiterState.IfuUsing){
      when(io.ifuAXI4.r.fire) { arbiterState := ArbiterState.Idle }
      .otherwise( arbiterState := ArbiterState.IfuUsing)
    }
    is (ArbiterState.LsuUsing){
      when(io.lsuAXI4.r.fire) { arbiterState := ArbiterState.Idle }
      .otherwise( arbiterState := ArbiterState.LsuUsing)
    }
  }

  axi4Bus.ar.payload  := Mux(arbiterState === ArbiterState.IfuUsing, io.ifuAXI4.ar.payload, io.lsuAXI4.ar.payload)
  axi4Bus.ar.valid    := (arbiterState === ArbiterState.IfuUsing && io.ifuAXI4.ar.valid) ||
                         (arbiterState === ArbiterState.LsuUsing && io.lsuAXI4.ar.valid)
  io.ifuAXI4.ar.ready := (arbiterState === ArbiterState.IfuUsing && axi4Bus.ar.ready) 
  io.lsuAXI4.ar.ready := (arbiterState === ArbiterState.LsuUsing && axi4Bus.ar.ready) 

  io.ifuAXI4.r.payload := axi4Bus.r.payload
  io.lsuAXI4.r.payload := axi4Bus.r.payload
  io.ifuAXI4.r.valid := (arbiterState === ArbiterState.IfuUsing && axi4Bus.r.valid)
  io.lsuAXI4.r.valid := (arbiterState === ArbiterState.LsuUsing && axi4Bus.r.valid)
  axi4Bus.r.ready    := (arbiterState === ArbiterState.IfuUsing && io.ifuAXI4.r.ready) ||
                        (arbiterState === ArbiterState.LsuUsing && io.lsuAXI4.r.ready)

  axi4Bus.aw <> io.lsuAXI4.aw   // 写通道直连
  axi4Bus.w <> io.lsuAXI4.w
  axi4Bus.b <> io.lsuAXI4.b

// --------------------------------------------------------- crossbar ------------------------------------------------------ //
  object CrossState extends SpinalEnum {              // 定义状态机枚举
    val Idle, Clint, External = newElement()
  }
  val crossState = Reg(CrossState()) init(CrossState.Idle) 
  switch(crossState) {      // 长记性了，读有效+读地址在范围内才可以判断在读取系统时钟，少用或
    is(CrossState.Idle) {
      when(axi4Bus.ar.valid) { 
        when(axi4Bus.ar.addr >= U"32'h02000000" && axi4Bus.ar.addr <= U"32'h0200ffff") { 
          crossState := CrossState.Clint 
          } otherwise { 
            crossState := CrossState.External 
          }
      } elsewhen(axi4Bus.aw.valid) { 
        when(axi4Bus.aw.addr >= U"32'h02000000" && axi4Bus.aw.addr <= U"32'h0200ffff") {
           crossState := CrossState.Clint 
        } otherwise { 
          crossState := CrossState.External 
        }
      } otherwise {
        crossState := CrossState.Idle
      } 
    }
    is(CrossState.Clint, CrossState.External) {
      when(axi4Bus.r.fire || axi4Bus.b.fire) { crossState := CrossState.Idle }
      .otherwise { crossState := crossState }
    }
  }

  // ---------------------------------------------- 系统时钟的总线 ---------------------------------------------- //
  // ------------------------------- 读地址 ------------------------------- //
  io.clintAxi.ar.valid := (crossState === CrossState.Clint) && axi4Bus.ar.valid   
  io.clintAxi.ar.payload := axi4Bus.ar.payload

  io.externalAxi.ar.valid := (crossState === CrossState.External) && axi4Bus.ar.valid
  io.externalAxi.ar.payload := axi4Bus.ar

  axi4Bus.ar.ready := (crossState === CrossState.Clint && io.clintAxi.ar.ready) ||
                      (crossState === CrossState.External && io.externalAxi.ar.ready)
  // ------------------------------- 读数据 ------------------------------- //
  axi4Bus.r.valid  := (crossState === CrossState.Clint && io.clintAxi.r.valid) ||
                      (crossState === CrossState.External && io.externalAxi.r.valid)
  axi4Bus.r.payload := Mux(crossState === CrossState.Clint, io.clintAxi.r.payload, io.externalAxi.r.payload)
  io.clintAxi.r.ready := (crossState === CrossState.Clint) && axi4Bus.r.ready

  io.externalAxi.r.ready := (crossState === CrossState.External) && axi4Bus.r.ready
  // ------------------------------- 写地址 ------------------------------- //
  io.clintAxi.aw.valid := (crossState === CrossState.Clint && axi4Bus.aw.valid)
  io.clintAxi.aw.payload := axi4Bus.aw.payload

  io.externalAxi.aw.valid := (crossState === CrossState.External && axi4Bus.aw.valid)
  io.externalAxi.aw.payload := axi4Bus.aw.payload

  axi4Bus.aw.ready := (crossState === CrossState.Clint && io.clintAxi.aw.ready) ||
                      (crossState === CrossState.External && io.externalAxi.aw.ready)
  // ------------------------------- 写数据 ------------------------------- //
  io.clintAxi.w.valid := (crossState === CrossState.Clint && axi4Bus.w.valid)
  io.clintAxi.w.payload := axi4Bus.w.payload

  io.externalAxi.w.valid := (crossState === CrossState.External && axi4Bus.w.valid)
  io.externalAxi.w.payload := axi4Bus.w.payload

  axi4Bus.w.ready := (crossState === CrossState.Clint && io.clintAxi.w.ready) ||
                     (crossState === CrossState.External && io.externalAxi.w.ready)
  // ------------------------------- 写响应 ------------------------------- //
  axi4Bus.b.valid := (crossState === CrossState.Clint && io.clintAxi.b.valid) ||
                      (crossState === CrossState.External && io.externalAxi.b.valid)
  axi4Bus.b.payload := Mux(crossState === CrossState.Clint, io.clintAxi.b.payload, io.externalAxi.b.payload)
  io.clintAxi.b.ready := (crossState === CrossState.Clint && axi4Bus.b.ready)

  io.externalAxi.b.ready := (crossState === CrossState.External && axi4Bus.b.ready)


}
