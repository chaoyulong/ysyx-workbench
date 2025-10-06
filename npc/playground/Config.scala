package playground

import spinal.core._
import spinal.core.sim._

object Config {
  val build_dir = sys.env.getOrElse("BUILD_DIR", "./build")    // verilog文件生成位置
  val sim_dir = sys.env.getOrElse("SPINAL_SIM_DIR", "./build/simulations")  // 仿真文件生成位置

  def spinal = SpinalConfig(
    targetDirectory = build_dir,
    defaultConfigForClockDomains = ClockDomainConfig(
      resetActiveLevel = HIGH
    ),
    onlyStdLogicVectorAtTopLevelIo = false
  )
  .withDebugComments()      // 源代码注释
  .withDebugUnnamed()       // 未命名信号调试
  .withLineNumbers()        // 行号信息

  def sim = SimConfig.
    withConfig(spinal).
    withFstWave.
    workspacePath(sim_dir)
}

object SpinalToVerilog extends App {
  Config.spinal.generateVerilog(RegFile())
}

object SpinalToVhdl extends App {
  Config.spinal.generateVhdl(RegFile())
}