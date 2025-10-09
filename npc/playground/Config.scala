package playground

import spinal.core._
import spinal.core.sim._
import scala.reflect.runtime.universe

// ============================================
// 顶层名称解析
// ============================================
object TopNameResolver {

  /** 获取顶层模块名称（从环境变量 SPINAL_TOPNAME 读取） */
  def getTopName(): String = {
    sys.env.get("SPINAL_TOPNAME") match {
      case Some(name) if name.nonEmpty =>
        println(s"[Config] Using top module name: $name")
        name
      case _ =>
        System.err.println("[Error] Environment variable SPINAL_TOPNAME is not set!")
        System.err.println("Usage example:")
        System.err.println("  SPINAL_TOPNAME=CPU BUILD_DIR=./build mill -i playground.runMain playground.SpinalGenerateMain")
        sys.exit(1)
        ""
    }
  }

  /** 根据模块名动态实例化顶层组件 */
  def createTop(name: String): Component = {
    name match {
      case "CPU" => CPU(CPUConfig()) // 你的CPU定义，带参数的版本
      // 这里可以按需扩展更多模块
      // case "ALU" => ALU()
      // case "CoreTop" => CoreTop()
      case other =>
        System.err.println(s"[Error] Unknown SPINAL_TOPNAME '$other'. Please check your module name.")
        sys.exit(1)
        null
    }
  }
}

object Config {
  val build_dir = sys.env.getOrElse("BUILD_DIR", ".")    // verilog文件生成位置
  // val build_dir = sys.env.getOrElse("$BUILD_DIR", ".")    // verilog文件生成位置
  val sim_dir = sys.env.getOrElse("SPINAL_SIM_DIR", "./build/simulations")  // 仿真文件生成位置

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
  val fullName = sys.env.getOrElse("SPINAL_TOPNAME", "CPU")
  println(s"[SpinalToVerilog] Generating Verilog for top module: $fullName")

  val mirror = universe.runtimeMirror(getClass.getClassLoader)
  try {
    val moduleSymbol:String = mirror.staticModule(fullName) // companion object
    val module:String = mirror.reflectModule(moduleSymbol).instance

    // 找到 companion 的 apply 方法（可能有参数或无参数）
    val applyMethod = module.getClass.getMethods.find(_.getName == "apply")
      .getOrElse(throw new NoSuchMethodException(s"No apply() in companion object of $fullName"))

    // **重点**：在 generateVerilog 的 by-name 块里实例化 component
    Config.spinal.generateVerilog {
      // 如果 apply 无参数，直接调用
      if (applyMethod.getParameterCount == 0) {
        applyMethod.invoke(module).asInstanceOf[Component]
      } 
      else {
        // // 如果 apply 有参数，尝试从环境变量 SPINAL_ARGS 取 CSV 参数（简单示例）
        // val rawArgs = sys.env.get("SPINAL_ARGS").map(_.split(",")).getOrElse(Array.empty[String])
        // if (rawArgs.length < applyMethod.getParameterCount)
        //   throw new IllegalArgumentException(s"Need ${applyMethod.getParameterCount} SPINAL_ARGS, got ${rawArgs.length}")

        // val params: Array[AnyRef] = applyMethod.getParameterTypes.zipWithIndex.map {
        //   case (pt, i) =>
        //     val s = rawArgs(i)
        //     // 支持简单的几种类型（按需扩展）
        //     if (pt == classOf[java.lang.String]) s
        //     else if (pt == java.lang.Integer.TYPE) java.lang.Integer.valueOf(s)
        //     else if (pt == java.lang.Long.TYPE) java.lang.Long.valueOf(s)
        //     else if (pt == java.lang.Boolean.TYPE) java.lang.Boolean.valueOf(s)
        //     else throw new IllegalArgumentException(s"Unsupported param type: $pt for top $fullName. Consider providing a zero-arg wrapper object.")
        // }.asInstanceOf[Array[AnyRef]]

        // applyMethod.invoke(module, params: _*).asInstanceOf[Component]
        ALU()
      }
    }
  } catch {
    case e: Throwable =>
      System.err.println(s"Error while generating Verilog for '$fullName': ${e.getMessage}")
      e.printStackTrace()
      sys.exit(1)
  }
  // Config.spinal.generateVerilog(CPU())
}

// object SpinalToVhdl extends App {
//   Config.spinal.generateVhdl(Top())
// }

