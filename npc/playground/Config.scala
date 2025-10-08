package playground

import spinal.core._
import spinal.core.sim._
import scala.reflect.runtime.universe

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
  // 从环境变量读取顶层模块名
  val topName = sys.env.getOrElse("SPINAL_TOPNAME", "CPU")
  val fullName = s"playground.$topName"   // 包名 + 类名
  println(s"[SpinalToVerilog] Generating Verilog for top module: $fullName")

  // 获取运行时反射镜像
  val mirror = universe.runtimeMirror(getClass.getClassLoader)

  try {
    // 获取伴生对象（object CPU）
    val moduleSymbol = mirror.staticModule(fullName)
    val module = mirror.reflectModule(moduleSymbol).instance

    // 调用 apply() 生成实例（case class 有自动 apply）
    val applyMethod = module.getClass.getMethod("apply")
    val component = applyMethod.invoke(module).asInstanceOf[Component]

    // 使用你的配置生成 Verilog
    Config.spinal.generateVerilog(component)
  } catch {
    case e: scala.ScalaReflectionException =>
      Console.err.println(s"Error: Cannot find module '$fullName'")
      e.printStackTrace()
    case e: Throwable =>
      Console.err.println(s"Error while generating Verilog for '$fullName': ${e.getMessage}")
      e.printStackTrace()
  }
  // Config.spinal.generateVerilog(CPU())
}

// object SpinalToVhdl extends App {
//   Config.spinal.generateVhdl(Top())
// }

