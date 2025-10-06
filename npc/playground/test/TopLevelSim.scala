package playground

import spinal.core._
import spinal.core.sim._

object SpinalSim extends App {
  Config.sim.compile(Shifter()).doSim { dut =>
    // Fork a process to generate the reset and the clock on the dut
    dut.clockDomain.forkStimulus(period = 10)

    for (idx <- 0 to 50) {
      dut.io.din.randomize()
      dut.io.shamt.randomize()
      dut.io.a_l #= false
      dut.io.l_r #= true

      dut.clockDomain.waitRisingEdge()
    }
    for (idx <- 0 to 50) {
      dut.io.din.randomize()
      dut.io.shamt.randomize()
      dut.io.a_l #= true
      dut.io.l_r #= true

      dut.clockDomain.waitRisingEdge()
    }
    for (idx <- 0 to 50) {
      dut.io.din.randomize()
      dut.io.shamt.randomize()
      dut.io.a_l #= false
      dut.io.l_r #= false

      dut.clockDomain.waitRisingEdge()
    }
    for (idx <- 0 to 50) {
      dut.io.din.randomize()
      dut.io.shamt.randomize()
      dut.io.a_l #= true
      dut.io.l_r #= false

      dut.clockDomain.waitRisingEdge()
    }
  }
}
