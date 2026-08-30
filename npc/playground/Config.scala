package playground

import spinal.core._
import spinal.core.sim._
import scala.reflect.runtime.universe

// 配置 CPU 的参数(两平台统一, 直接 CpuConfig() 使用; 综合/STA 时显式 enableSimDebug = false)
case class CpuConfig(
  resetPc:          Long = 0x30000000L,  // 上电后的初始PC(两平台统一)
  enableMul:        Boolean = false,  // 乘法器
  enableDiv:        Boolean = false,  // 触发器
  enableInterrupt:  Boolean = false,  // 中断
  enableSimDebug:   Boolean = true    // 仿真专用调试信号(指令退休追踪/mtrace), 综合时关闭
  // 以后继续加选项
)


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

  Config.spinal.generateVerilog{
    val top = topName match {
      case "NPC_TOP" => NPC_TOP(CpuConfig())
      case "ysyx_23060082" => ysyx_23060082(CpuConfig())
      case _ => throw new Exception(s"Unknown TOP_NAME: $topName")
    }
    top
  }
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

