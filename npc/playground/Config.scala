package playground

import spinal.core._
import spinal.core.sim._
import scala.reflect.runtime.universe

// 配置 CPU 的参数
case class CpuConfig(
  resetPc:          Long,             // 上电后的初始PC
  enableMul:        Boolean = false,  // 乘法器
  enableDiv:        Boolean = false,  // 触发器
  enableInterrupt:  Boolean = false   // 中断
  // 以后继续加选项
)

object CpuConfig {
  val npc = CpuConfig(
    resetPc = 0x80000000L
  )

  val ysyxSoc = CpuConfig(
    resetPc = 0x30000000L
  )
}


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
  // 读取环境变量
  val topName   = sys.env.getOrElse("SPINAL_TOPNAME", "NPC_TOP")
  val resetPcStr = sys.env.getOrElse("RESET_PC", "0x80000000")

  val resetPc = BigInt(resetPcStr.replace("0x", ""), 16)

  Config.spinal.generateVerilog{
    val top = topName match {
      case "NPC_TOP" => NPC_TOP(CpuConfig.npc)
      case "ysyx_23060082" => ysyx_23060082(CpuConfig.ysyxSoc)
      case _ => throw new Exception(s"Unknown TOP_NAME: $topName")
    }
    top
  }
  // val report = SpinalVerilog{
  // val top = topName match {
  //   case "NPC_TOP" => NPC_TOP(CpuConfig(resetPc))
  //   case "ysyx_23060082" => ysyx_23060082(CpuConfig(resetPc))
  //   case _ => throw new Exception(s"Unknown TOP_NAME: $topName")
  // }
  // top
  // }
  // report.printPruned()
}

// object SpinalToVerilog extends App {
//   val fullName = sys.env.getOrElse("SPINAL_TOPNAME", {
//     System.err.println("[Error] Missing env var SPINAL_TOPNAME")
//     sys.exit(1)
//   })

//   println(s"[Info] Generating Verilog for: $fullName")

//   val mirror = universe.runtimeMirror(getClass.getClassLoader)
//   try {
//     val module      = mirror.reflectModule(mirror.staticModule(fullName)).instance
//     val applyMethod = module.getClass.getMethods
//       .find(_.getName == "apply")
//       .getOrElse {
//         System.err.println(s"[Error] $fullName has no apply() method")
//         sys.exit(1)
//       }
//     Config.spinal.generateVerilog(applyMethod.invoke(module).asInstanceOf[Component])
//     println(s"[Info] Done.")
//   } catch {
//     case _: ScalaReflectionException =>
//       System.err.println(s"[Error] Module not found: $fullName")
//       sys.exit(1)
//     case e: Exception =>
//       System.err.println(s"[Error] Failed to generate HDL for '$fullName': ${e.getMessage}")
//       sys.exit(1)
//   }
// }

// object SpinalToVhdl extends App {
//   Config.spinal.generateVhdl(Top())
// }

