package playground

import spinal.core._
import spinal.core.sim._

object Config {
  def spinal = SpinalConfig(
    targetDirectory = sys.env.getOrElse("BUILD_DIR", "./build"),    // 输出文件默认位置为./build
    defaultConfigForClockDomains = ClockDomainConfig(
      resetActiveLevel = HIGH
    ),
    onlyStdLogicVectorAtTopLevelIo = false
  )

  def sim = SimConfig.
    withConfig(spinal).
    withFstWave.
    workspacePath("./build/simulations")
}

object SpinalToVerilog extends App {
  Config.spinal.generateVerilog(TopLevel())
}

object SpinalToVhdl extends App {
  Config.spinal.generateVhdl(TopLevel())
}