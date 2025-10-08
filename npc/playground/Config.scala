package playground

import spinal.core._
import spinal.core.sim._

object Config {
  val build_dir = sys.env.getOrElse("BUILD_DIR", "./build")    // verilog文件生成位置
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
  def failWithHelp(message: String): Nothing = {
    println(s"❌ $message")
    println("❌ Usage: SPINAL_TOPNAME=<module> make run")
    sys.exit(1)
  }
  val moduleSpec = sys.env.getOrElse("SPINAL_TOPNAME", failWithHelp("SPINAL_TOP environment variable not set!"))

  val topModule = if (moduleSpec.contains(".")) {
    // 如果是完整类名，使用反射
    try {
      val clazz = Class.forName(moduleSpec)
      val constructor = clazz.getConstructor()
      constructor.newInstance().asInstanceOf[Component]
    } catch {
      case e: Exception =>
        failWithHelp(s"Cannot load class: $moduleSpec (${e.getMessage})")
    }
  } else{
    failWithHelp("Please use full name like: playground.TopLevel")
  }
  Config.spinal.generateVerilog(topModule)
  // Config.spinal.generateVerilog(CPU())
}

// object SpinalToVhdl extends App {
//   Config.spinal.generateVhdl(Top())
// }

