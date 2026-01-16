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
    val ifuAxi4     = slave(Axi4ReadOnly(AxiConfig.axiConfig))
    val lsuAxi4     = slave(Axi4(AxiConfig.axiConfig))
    val clintAxi4   = master(Axi4(AxiConfig.axiConfig))
    val externalAxi = master(Axi4(AxiConfig.axiConfig))
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
      when(io.ifuAxi4.r.fire) { arbiterState := ArbiterState.Idle }
      .otherwise( arbiterState := ArbiterState.IfuUsing)
    }
    is (ArbiterState.LsuUsing){
      when(io.lsuAxi4.r.fire) { arbiterState := ArbiterState.Idle }
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
  object CrossState extends SpinalEnum {              // 定义状态机枚举
    val Idle, Clint, External = newElement()
  }
  val crossState = Reg(CrossState()) init(CrossState.Idle) 
  switch(crossState) {      // 长记性了，读有效+读地址在范围内才可以判断在读取系统时钟，少用或
    is(CrossState.Idle) {
      when(busAxi4.ar.valid) { 
        when(busAxi4.ar.addr >= U"32'h02000000" && busAxi4.ar.addr <= U"32'h0200ffff") { 
          crossState := CrossState.Clint 
          } otherwise { 
            crossState := CrossState.External 
          }
      } elsewhen(busAxi4.aw.valid) { 
        when(busAxi4.aw.addr >= U"32'h02000000" && busAxi4.aw.addr <= U"32'h0200ffff") {
           crossState := CrossState.Clint 
        } otherwise { 
          crossState := CrossState.External 
        }
      } otherwise {
        crossState := CrossState.Idle
      } 
    }
    is(CrossState.Clint, CrossState.External) {
      when(busAxi4.r.fire || busAxi4.b.fire) { crossState := CrossState.Idle }
      .otherwise { crossState := crossState }
    }
  }

  // ---------------------------------------------- 系统时钟的总线 ---------------------------------------------- //
  // ------------------------------- 读地址 ------------------------------- //
  io.clintAxi4.ar.valid := (crossState === CrossState.Clint) && busAxi4.ar.valid   
  io.clintAxi4.ar.payload := busAxi4.ar.payload

  io.externalAxi.ar.valid := (crossState === CrossState.External) && busAxi4.ar.valid
  io.externalAxi.ar.payload := busAxi4.ar

  busAxi4.ar.ready := (crossState === CrossState.Clint && io.clintAxi4.ar.ready) ||
                      (crossState === CrossState.External && io.externalAxi.ar.ready)
  // ------------------------------- 读数据 ------------------------------- //
  busAxi4.r.valid  := (crossState === CrossState.Clint && io.clintAxi4.r.valid) ||
                      (crossState === CrossState.External && io.externalAxi.r.valid)
  busAxi4.r.payload := Mux(crossState === CrossState.Clint, io.clintAxi4.r.payload, io.externalAxi.r.payload)
  io.clintAxi4.r.ready := (crossState === CrossState.Clint) && busAxi4.r.ready

  io.externalAxi.r.ready := (crossState === CrossState.External) && busAxi4.r.ready
  // ------------------------------- 写地址 ------------------------------- //
  io.clintAxi4.aw.valid := (crossState === CrossState.Clint && busAxi4.aw.valid)
  io.clintAxi4.aw.payload := busAxi4.aw.payload

  io.externalAxi.aw.valid := (crossState === CrossState.External && busAxi4.aw.valid)
  io.externalAxi.aw.payload := busAxi4.aw.payload

  busAxi4.aw.ready := (crossState === CrossState.Clint && io.clintAxi4.aw.ready) ||
                      (crossState === CrossState.External && io.externalAxi.aw.ready)
  // ------------------------------- 写数据 ------------------------------- //
  io.clintAxi4.w.valid := (crossState === CrossState.Clint && busAxi4.w.valid)
  io.clintAxi4.w.payload := busAxi4.w.payload

  io.externalAxi.w.valid := (crossState === CrossState.External && busAxi4.w.valid)
  io.externalAxi.w.payload := busAxi4.w.payload

  busAxi4.w.ready := (crossState === CrossState.Clint && io.clintAxi4.w.ready) ||
                     (crossState === CrossState.External && io.externalAxi.w.ready)
  // ------------------------------- 写响应 ------------------------------- //
  busAxi4.b.valid := (crossState === CrossState.Clint && io.clintAxi4.b.valid) ||
                      (crossState === CrossState.External && io.externalAxi.b.valid)
  busAxi4.b.payload := Mux(crossState === CrossState.Clint, io.clintAxi4.b.payload, io.externalAxi.b.payload)
  io.clintAxi4.b.ready := (crossState === CrossState.Clint && busAxi4.b.ready)

  io.externalAxi.b.ready := (crossState === CrossState.External && busAxi4.b.ready)
}
