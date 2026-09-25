error id: file://<WORKSPACE>/playground/Config.scala:
file://<WORKSPACE>/playground/Config.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -spinal/core/TopNpc#
	 -spinal/core/sim/TopNpc#
	 -TopNpc#
	 -scala/Predef.TopNpc#
offset: 1360
uri: file://<WORKSPACE>/playground/Config.scala
text:
```scala
package playground

import spinal.core._
import spinal.core.sim._
import scala.reflect.runtime.universe

object Config {
  val build_dir:String = sys.env.getOrElse("BUILD_DIR", ".")    // verilog文件生成位置
  val sim_dir:String = sys.env.getOrElse("SPINAL_SIM_DIR", "./simulations")  // 仿真文件生成位置

  def spinal = SpinalConfig(
    targetDirectory = build_dir,
    defaultConfigForClockDomains = ClockDomainConfig(
      resetActiveLevel = HIGH
    ),                                      // 设置将用作所有新 ``ClockDomain``时钟域默认值的配置。
    onlyStdLogicVectorAtTopLevelIo = false, // 将所有无符号/有符号顶级 io 更改为 std_logic_vector类型。
    anonymSignalPrefix = "_zz",             // 未命名信号的前缀,默认为"_zz"
    genLineComments = true,                 // 添加注释
    headerWithDate = true,                  // 添加时间信息
    headerWithRepoHash = true,              // 添加git hash(默认为true)
    withTimescale = true                    // 添加时间刻度(默认为true)
  )

  def sim = SimConfig.
    withConfig(spinal).
    withFstWave.
    workspacePath(sim_dir)
}

object SpinalToVerilog extends App {

  // 1. 读取环境变量
  val topName   = sys.env.getOrElse("SPINAL_TOPNAME", "NPC_TOP")
  val resetPcStr = sys.env.getOrElse("RESET_PC", "0x80000000")

  val resetPc = BigInt(resetPcStr.replace("0x", ""), 16)

  // 2. 根据 TOP_NAME / 或者你也可以用 PLATFORM 决定用哪个 Top
  val top = topName match {
    case "NPC_TOP" =>
      new TopN@@pc(CpuConfig(resetPc))
    case "ysyxsocfull" =>
      new TopSoC(CpuConfig(resetPc))
    case _ =>
      throw new Exception(s"Unknown TOP_NAME: $topName")
  }

  top.setName(topName)

  // 3. 指定生成目录
  val spinalConfig = Config.spinal.copy(
    targetDirectory = buildDir
  )

  spinalConfig.generateVerilog(top)
}

// object SpinalToVerilog extends App {

//   // === 颜色 ANSI 转义码 ===
//   val RED    = "\u001b[31m"  
//   val YELLOW = "\u001b[33m"  
//   val BLUE   = "\u001b[34m" 
//   val RESET  = "\u001b[0m"     // 重置

//   // === 获取顶层模块名 ===
//   val fullName = sys.env.get("SPINAL_TOPNAME").getOrElse {
//     System.err.println(
//       s"${RED}[Error] Missing environment variable: SPINAL_TOPNAME.${RESET}\n" +
//       s"${YELLOW}[Info] Example:\n" +
//       "  SPINAL_TOPNAME=playground.CPU mill -i playground.runMain playground.SpinalToVerilog"
//     )
//     sys.exit(1)
//     ""
//   }
//   println(s"${BLUE}[Info] Generating Verilog for top module: $fullName${RESET}")

//   val mirror = universe.runtimeMirror(getClass.getClassLoader)
//   try {
//     // === 反射查找顶层模块 ===
//     val moduleSymbol = mirror.staticModule(fullName)
//     val module = mirror.reflectModule(moduleSymbol).instance

//     // === 找到 apply 方法 ===
//     val applyMethod = module.getClass.getMethods.find(_.getName == "apply")
//       .getOrElse {
//         System.err.println(s"${RED}[Error] No apply() method in companion object of '$fullName'.${RESET}")
//         sys.exit(1)
//         null
//       }

//     // === 解析参数 ===
//     val args = sys.env.get("SPINAL_ARGS").map(_.split(",")).getOrElse(Array.empty[String])
//     val params: Array[AnyRef] =
//       if (applyMethod.getParameterCount == 0) Array.empty
//       else {
//         if (args.length < applyMethod.getParameterCount) {
//           System.err.println(
//             s"${RED}[Error] Need ${applyMethod.getParameterCount} SPINAL_ARGS, got ${args.length}.${RESET}\n" +
//             s"${YELLOW}[Info] Example: SPINAL_ARGS=4,true,1024"
//           )
//           sys.exit(1)
//         }

//         applyMethod.getParameterTypes.zipWithIndex.map {
//           case (pt, i) =>
//             val s = args(i)
//             if (pt == classOf[java.lang.String]) s
//             else if (pt == java.lang.Integer.TYPE) java.lang.Integer.valueOf(s)
//             else if (pt == java.lang.Long.TYPE) java.lang.Long.valueOf(s)
//             else if (pt == java.lang.Boolean.TYPE) java.lang.Boolean.valueOf(s)
//             else throw new IllegalArgumentException(
//               s"${RED}[Error] Unsupported param type: $pt for top $fullName${RESET}"
//             )
//         }.asInstanceOf[Array[AnyRef]]
//       }

//     // === 生成 Verilog ===
//     Config.spinal.generateVerilog(
//       applyMethod.invoke(module, params: _*).asInstanceOf[Component]
//     )

//     println(s"${BLUE}[Info] Verilog generation completed successfully for $fullName.${RESET}")

//   } catch {
//     case e: Throwable =>
//       System.err.println(s"${RED}[Error] Failed to generate HDL for '$fullName': ${e.getMessage}${RESET}")
//       e.printStackTrace()
//       sys.exit(1)
//   }
// }


// object SpinalToVhdl extends App {
//   Config.spinal.generateVhdl(Top())
// }


```


#### Short summary: 

empty definition using pc, found symbol in pc: 