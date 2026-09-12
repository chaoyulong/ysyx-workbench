error id: 7F401511BCDF33D84ABC6D8C837724D4
file://<WORKSPACE>/playground/src/Xbar.scala
### java.util.NoSuchElementException: head of empty String

occurred in the presentation compiler.



action parameters:
offset: 553
uri: file://<WORKSPACE>/playground/src/Xbar.scala
text:
```scala
package playground

import spinal.core._
import spinal.lib._       // 使用spinal的模块库
import spinal.lib.bus.amba4.axi._

case class ysyx_23060082_AXI4Xbar() extends Component {
  val io = new Bundle {
    val ifuAXI4     = slave(Axi4ReadOnly(AxiConfig.axiConfig))
    val ifuAXI4Req  = in Bool()
    val lsuAXI4     = slave(Axi4(AxiConfig.axiConfig))
    val lsuAXI4Req  = in Bool()
    val clintAxi    = master(Axi4(AxiConfig.axiConfig))
    val externalAxi = master(Axi4(AxiConfig.axiConfig))
  }

  // val axi4Bus  = master(Axi4(AxiConfig.axiConfig))
  @@axi4Bus  = master(Axi4(AxiConfig.axiConfig))
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

  val hitClint = (axi4Bus.ar.addr >= U"32'h02000000") && (axi4Bus.ar.addr <= U"32'h0200ffff") ||    // 在Clint范围内
                 (axi4Bus.aw.addr >= U"32'h02000000") && (axi4Bus.aw.addr <= U"32'h0200ffff")
  val toExt   = !hitClint   // 通往外部

  switch(crossState) {
    is(CrossState.Idle) {
      when(axi4Bus.ar.valid || axi4Bus.aw.valid) { 
        when(hitClint) { crossState := CrossState.Clint } 
        .otherwise { crossState := CrossState.External }
      }
      .otherwise( crossState := CrossState.Idle)
    }
    is (CrossState.Clint, CrossState.External){
      when(axi4Bus.r.fire || axi4Bus.b.fire) { crossState := CrossState.Idle }
      .otherwise( crossState := crossState)
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


  // io.externalAxi <> axi4Bus
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

```


presentation compiler configuration:
Scala version: 2.13.14
Classpath:
<WORKSPACE>/.bloop/out/playground/bloop-bsp-clients-classes/classes-Metals-EP3AXH17R0i3blXeRHUCWA== [exists ], <HOME>/.cache/bloop/semanticdb/com.sourcegraph.semanticdb-javac.0.11.1/semanticdb-javac-0.11.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-core_2.13/1.12.3/spinalhdl-core_2.13-1.12.3.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-lib_2.13/1.12.3/spinalhdl-lib_2.13-1.12.3.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.14/scala-library-2.13.14.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-idsl-plugin_2.13/1.12.3/spinalhdl-idsl-plugin_2.13-1.12.3.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-sim_2.13/1.12.3/spinalhdl-sim_2.13-1.12.3.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scalactic/scalactic_2.13/3.2.10/scalactic_2.13-3.2.10.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-reflect/2.13.14/scala-reflect-2.13.14.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/scopt/scopt_2.13/4.1.0/scopt_2.13-4.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/sourcecode_2.13/0.3.0/sourcecode_2.13-0.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-compiler/2.13.14/scala-compiler-2.13.14.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-idsl-payload_2.13/1.12.3/spinalhdl-idsl-payload_2.13-1.12.3.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/net/openhft/affinity/3.23.2/affinity-3.23.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.5/slf4j-simple-2.0.5.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/oshi/oshi-core/6.4.0/oshi-core-6.4.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/io/github/java-diff-utils/java-diff-utils/4.12/java-diff-utils-4.12.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/jline/jline/3.25.1/jline-3.25.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/net/java/dev/jna/jna/5.14.0/jna-5.14.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.5/slf4j-api-2.0.5.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/net/java/dev/jna/jna-platform/5.12.1/jna-platform-5.12.1.jar [exists ], <WORKSPACE>/compile-resources [missing ]
Options:
-Yrangepos -Xplugin-require:semanticdb




#### Error stacktrace:

```
scala.collection.StringOps$.head$extension(StringOps.scala:1124)
	scala.meta.internal.metals.ClassfileComparator.compare(ClassfileComparator.scala:30)
	scala.meta.internal.metals.ClassfileComparator.compare(ClassfileComparator.scala:3)
	java.base/java.util.PriorityQueue.siftUpUsingComparator(PriorityQueue.java:660)
	java.base/java.util.PriorityQueue.siftUp(PriorityQueue.java:637)
	java.base/java.util.PriorityQueue.offer(PriorityQueue.java:330)
	java.base/java.util.PriorityQueue.add(PriorityQueue.java:311)
	scala.meta.internal.metals.ClasspathSearch.$anonfun$search$3(ClasspathSearch.scala:32)
	scala.meta.internal.metals.ClasspathSearch.$anonfun$search$3$adapted(ClasspathSearch.scala:26)
	scala.collection.IterableOnceOps.foreach(IterableOnce.scala:630)
	scala.collection.IterableOnceOps.foreach$(IterableOnce.scala:628)
	scala.collection.AbstractIterator.foreach(Iterator.scala:1313)
	scala.meta.internal.metals.ClasspathSearch.search(ClasspathSearch.scala:26)
	scala.meta.internal.metals.WorkspaceSymbolProvider.search(WorkspaceSymbolProvider.scala:107)
	scala.meta.internal.metals.MetalsSymbolSearch.search$1(MetalsSymbolSearch.scala:114)
	scala.meta.internal.metals.MetalsSymbolSearch.search(MetalsSymbolSearch.scala:118)
	scala.meta.internal.pc.AutoImportsProvider.autoImports(AutoImportsProvider.scala:48)
	scala.meta.internal.pc.ScalaPresentationCompiler.$anonfun$autoImports$1(ScalaPresentationCompiler.scala:399)
	scala.meta.internal.pc.CompilerAccess.withSharedCompiler(CompilerAccess.scala:148)
	scala.meta.internal.pc.CompilerAccess.$anonfun$withInterruptableCompiler$1(CompilerAccess.scala:92)
	scala.meta.internal.pc.CompilerAccess.$anonfun$onCompilerJobQueue$1(CompilerAccess.scala:209)
	scala.meta.internal.pc.CompilerJobQueue$Job.run(CompilerJobQueue.scala:152)
	java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136)
	java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
	java.base/java.lang.Thread.run(Thread.java:840)
```
#### Short summary: 

java.util.NoSuchElementException: head of empty String