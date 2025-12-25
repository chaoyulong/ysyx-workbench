package playground

import spinal.core._
import spinal.lib._       // 使用spinal的模块库
import spinal.lib.bus.amba4.axi._

case class ysyx_23060082_AXI4Adapter() extends Component {
  val io = new Bundle {
    val ifuAXI4     = slave(Axi4ReadOnly(AxiConfig.axiConfig))
    val ifuAXI4Req  = in Bool()
    val lsuAXI4     = slave(Axi4(AxiConfig.axiConfig))
    val lsuAXI4Req  = in Bool()
    val clintAxi    = master(Axi4(AxiConfig.axiConfig))
    val externalAxi = master(Axi4(AxiConfig.axiConfig))
  }

  object XbarState extends SpinalEnum {              // 定义状态机枚举
    val Idle, IfuUsing, LsuUsing = newElement()
  }
  val state = Reg(XbarState()) init(XbarState.Idle)   // 创建一个状态机

  val axi4Bus  = master(Axi4(AxiConfig.axiConfig))
  axi4Bus.aw <> io.lsuAXI4.aw   // 写通道直连
  axi4Bus.w <> io.lsuAXI4.w
  axi4Bus.b <> io.lsuAXI4.b

  val arMasterValid = RegNext(io.ifuAXI4.ar.valid || io.lsuAXI4.ar.valid)

  switch(state) {
    is(XbarState.Idle) {
      when(io.ifuAXI4.ar.valid) { state := XbarState.IfuUsing }
      .elsewhen (io.lsuAXI4.ar.valid) { state := XbarState.LsuUsing }
      .otherwise( state := XbarState.Idle)
    }
    is (XbarState.IfuUsing){
      when(io.ifuAXI4.r.fire) { state := XbarState.Idle }
      .otherwise( state := XbarState.IfuUsing)
    }
    is (XbarState.LsuUsing){
      when(io.lsuAXI4.r.fire) { state := XbarState.Idle }
      .otherwise( state := XbarState.LsuUsing)
    }
  }

  val axi4ReadOnlyEnpty = master(Axi4ReadOnly(AxiConfig.axiConfig))   // 创建一个空的axi总线
  axi4ReadOnlyEnpty.ar.valid = (state === XbarState.IfuUsing && io.ifuAXI4.ar.valid) ||
                               (state === XbarState.LsuUsing && io.lsuAXI4.ar.valid)

  // axi4Bus.ar.payload <> Mux(state === XbarState.IfuUsing, io.ifuAXI4.ar.payload, io.lsuAXI4.ar.payload)
  // axi4Bus.ar.valid := (state === XbarState.IfuUsing && io.ifuAXI4.ar.valid) ||
  //                     (state === XbarState.LsuUsing && io.lsuAXI4.ar.valid)
  // io.ifuAXI4.ar.ready := (state === XbarState.IfuUsing && io.ifuAXI4.ar.ready) 
  // io.lsuAXI4.ar.ready := (state === XbarState.LsuUsing && io.lsuAXI4.ar.ready) 

  // axi4Bus.r.payload <> Mux(state === XbarState.IfuUsing, io.ifuAXI4.r.payload, io.lsuAXI4.r.payload)
  // io.ifuAXI4.r.valid := (state === XbarState.IfuUsing && axi4Bus.r.valid)
  // io.lsuAXI4.r.valid := (state === XbarState.LsuUsing && axi4Bus.r.valid)
  // io.axi4Bus.r.ready :=
}

// case class ysyx_23060082_AXI4Adapter() extends Component {
//   val io = new Bundle {
//     val ifuAXI4      = slave(Axi4ReadOnly(AxiConfig.axiConfig))
//     val lsuAXI4      = slave(Axi4(AxiConfig.axiConfig))
//     val clintAxi     = master(Axi4(AxiConfig.axiConfig))
//     val externalAxi  = master(Axi4(AxiConfig.axiConfig))
//   }

//   // ===============================
//   // IFU ReadOnly → Full AXI
//   // ===============================
//   val ifuAdapt = new Component {
//     val io = new Bundle {
//       val ifu = slave(Axi4ReadOnly(AxiConfig.axiConfig))
//       val axi = master(Axi4(AxiConfig.axiConfig))
//     }

//     io.axi.ar <> io.ifu.ar
//     io.axi.r  <> io.ifu.r

//     io.axi.aw.valid := False
//     io.axi.w.valid  := False
//     io.axi.b.ready  := True

//     io.axi.aw.payload.assignDontCare()
//     io.axi.w.payload.assignDontCare()
//   }

//   ifuAdapt.io.ifu <> io.ifuAXI4

//   // ===============================
//   // Crossbar
//   // ===============================
//   val xbar = Axi4CrossbarFactory()

//   // Slaves
//   xbar.addSlave(io.clintAxi,SizeMapping(0x02000000L, 8 Byte))
//   xbar.addSlave(io.externalAxi,SizeMapping(0x00000000, 4 GB))

//   // Masters (order = priority)
//   xbar.addConnection(ifuAdapt.io.axi,List(io.clintAxi, io.externalAxi))
//   xbar.addConnection(io.lsuAXI4,List(io.clintAxi, io.externalAxi))

//   xbar.build()
// }

// case class ysyx_23060082_AXI4Adapter() extends Component {
//   val io = new Bundle {
//     val ifuAXI4      = slave(Axi4ReadOnly(AxiConfig.axiConfig))
//     val lsuAXI4      = slave(Axi4(AxiConfig.axiConfig))
//     val clintAxi     = master(Axi4(AxiConfig.axiConfig))
//     val externalAxi  = master(Axi4(AxiConfig.axiConfig))
//   }

//   // ========================
//   // 地址命中 CLINT
//   // ========================
//   def hitClint(addr: UInt): Bool = {
//     // CLINT 8B: 0x0200_0000 ~ 0x0200_0007
//     (addr === U(0x02000000L)) || (addr === U(0x02000004L))
//   }

//   // ========================
//   // AR通道：IFU > LSU 优先仲裁
//   // ========================
//   val arValid = Bool()
//   val arAddr  = UInt(AxiConfig.axiConfig.addressWidth bits)
//   val arId    = UInt(AxiConfig.axiConfig.idWidth bits)
//   val arLen   = UInt(8 bits)
//   val arSize  = UInt(3 bits)
//   val arBurst = UInt(2 bits)

//   val arMasterValid = io.ifuAXI4.ar.valid || io.lsuAXI4.ar.valid
//   arValid := arMasterValid

//   // 优先 IFU
//   when(io.ifuAXI4.ar.valid){
//     arAddr  := io.ifuAXI4.ar.addr
//     arId    := io.ifuAXI4.ar.id
//     arLen   := io.ifuAXI4.ar.len
//     arSize  := io.ifuAXI4.ar.size
//     arBurst := io.ifuAXI4.ar.burst
//   }otherwise{
//     arAddr  := io.lsuAXI4.ar.addr
//     arId    := io.lsuAXI4.ar.id
//     arLen   := io.lsuAXI4.ar.len
//     arSize  := io.lsuAXI4.ar.size
//     arBurst := io.lsuAXI4.ar.burst
//   }

//   // 判断目标 slave
//   val toClint = hitClint(arAddr)
//   val toExt   = !toClint

//   // AR 分发
//   io.clintAxi.ar.valid := arValid && toClint
//   io.clintAxi.ar.addr  := arAddr
//   io.clintAxi.ar.id    := arId
//   io.clintAxi.ar.len   := arLen
//   io.clintAxi.ar.size  := arSize
//   io.clintAxi.ar.burst := arBurst
//   io.clintAxi.ar.prot  := (if(io.ifuAXI4.ar.valid) io.ifuAXI4.ar.prot else io.lsuAXI4.ar.prot)
//   io.clintAxi.ar.lock  := (if(io.ifuAXI4.ar.valid) io.ifuAXI4.ar.lock else io.lsuAXI4.ar.lock)
//   io.clintAxi.ar.cache := (if(io.ifuAXI4.ar.valid) io.ifuAXI4.ar.cache else io.lsuAXI4.ar.cache)
//   io.clintAxi.ar.qos   := (if(io.ifuAXI4.ar.valid) io.ifuAXI4.ar.qos else io.lsuAXI4.ar.qos)
//   io.clintAxi.ar.region:= (if(io.ifuAXI4.ar.valid) io.ifuAXI4.ar.region else io.lsuAXI4.ar.region)
//   io.clintAxi.ar.user  := (if(io.ifuAXI4.ar.valid) io.ifuAXI4.ar.user else io.lsuAXI4.ar.user)
//   io.clintAxi.r.ready  := if(toClint) (if(io.ifuAXI4.ar.valid) io.ifuAXI4.r.ready else io.lsuAXI4.r.ready) else False

//   io.externalAxi.ar.valid := arValid && toExt
//   io.externalAxi.ar.addr  := arAddr
//   io.externalAxi.ar.id    := arId
//   io.externalAxi.ar.len   := arLen
//   io.externalAxi.ar.size  := arSize
//   io.externalAxi.ar.burst := arBurst
//   io.externalAxi.ar.prot  := (if(io.ifuAXI4.ar.valid) io.ifuAXI4.ar.prot else io.lsuAXI4.ar.prot)
//   io.externalAxi.ar.lock  := (if(io.ifuAXI4.ar.valid) io.ifuAXI4.ar.lock else io.lsuAXI4.ar.lock)
//   io.externalAxi.ar.cache := (if(io.ifuAXI4.ar.valid) io.ifuAXI4.ar.cache else io.lsuAXI4.ar.cache)
//   io.externalAxi.ar.qos   := (if(io.ifuAXI4.ar.valid) io.ifuAXI4.ar.qos else io.lsuAXI4.ar.qos)
//   io.externalAxi.ar.region:= (if(io.ifuAXI4.ar.valid) io.ifuAXI4.ar.region else io.lsuAXI4.ar.region)
//   io.externalAxi.ar.user  := (if(io.ifuAXI4.ar.valid) io.ifuAXI4.ar.user else io.lsuAXI4.ar.user)
//   io.externalAxi.r.ready  := if(toExt) (if(io.ifuAXI4.ar.valid) io.ifuAXI4.r.ready else io.lsuAXI4.r.ready) else False

//   // ========================
//   // R通道直接 mux 回 master
//   // ========================
//   io.ifuAXI4.r <> io.clintAxi.r
//   io.lsuAXI4.r <> io.externalAxi.r

//   // ========================
//   // AW / W / B 通道（只 LSU 会写）
//   // ========================
//   val awAddr = io.lsuAXI4.aw.addr
//   val toClintW = hitClint(awAddr)
//   val toExtW   = !toClintW

//   io.clintAxi.aw.valid := io.lsuAXI4.aw.valid && toClintW
//   io.clintAxi.aw.addr  := awAddr
//   io.clintAxi.aw.id    := io.lsuAXI4.aw.id
//   io.clintAxi.aw.len   := io.lsuAXI4.aw.len
//   io.clintAxi.aw.size  := io.lsuAXI4.aw.size
//   io.clintAxi.aw.burst := io.lsuAXI4.aw.burst
//   io.clintAxi.aw.prot  := io.lsuAXI4.aw.prot
//   io.clintAxi.aw.lock  := io.lsuAXI4.aw.lock
//   io.clintAxi.aw.cache := io.lsuAXI4.aw.cache
//   io.clintAxi.aw.qos   := io.lsuAXI4.aw.qos
//   io.clintAxi.aw.region:= io.lsuAXI4.aw.region
//   io.clintAxi.aw.user  := io.lsuAXI4.aw.user
//   io.clintAxi.w        <> io.lsuAXI4.w
//   io.clintAxi.b.ready  := io.lsuAXI4.b.ready

//   io.externalAxi.aw.valid := io.lsuAXI4.aw.valid && toExtW
//   io.externalAxi.aw.addr  := awAddr
//   io.externalAxi.aw.id    := io.lsuAXI4.aw.id
//   io.externalAxi.aw.len   := io.lsuAXI4.aw.len
//   io.externalAxi.aw.size  := io.lsuAXI4.aw.size
//   io.externalAxi.aw.burst := io.lsuAXI4.aw.burst
//   io.externalAxi.aw.prot  := io.lsuAXI4.aw.prot
//   io.externalAxi.aw.lock  := io.lsuAXI4.aw.lock
//   io.externalAxi.aw.cache := io.lsuAXI4.aw.cache
//   io.externalAxi.aw.qos   := io.lsuAXI4.aw.qos
//   io.externalAxi.aw.region:= io.lsuAXI4.aw.region
//   io.externalAxi.aw.user  := io.lsuAXI4.aw.user
//   io.externalAxi.w        <> io.lsuAXI4.w
//   io.externalAxi.b.ready  := io.lsuAXI4.b.ready
// }
