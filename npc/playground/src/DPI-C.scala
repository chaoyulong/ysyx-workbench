package playground

import spinal.core._

case class GetInstr() extends BlackBox{
  val io = new Bundle{
    val pc_o = in UInt(32 bits)
    val instr = in UInt(32 bits)
  }
  noIoPrefix()
  addRTLPath(s"${sys.env("NPC_HOME")}/playground/vsrc/dpi-c.v")   
}

case class NpcMemRW() extends BlackBox{
  val io=new Bundle{
    val clock = in Bool()
    val reset = in Bool()
    val valid = in Bool()
    val wen   = in Bool()
    val waddr  = in UInt(32 bits)
    val wdata = in UInt(32 bits)
    val wmask = in UInt(4 bits)
    val raddr  = in UInt(32 bits)
    val rdata = out UInt(32 bits)
  }
  noIoPrefix()
  mapClockDomain(clock = io.clock,reset = io.reset)
  addRTLPath(s"${sys.env("NPC_HOME")}/playground/vsrc/dpi-c.v")   
}

// 仿真专用: itrace 指令退休追踪黑盒 (寄存器写在 dpi-c.v, 仅 enableSimDebug 时实例化)
case class ItraceReg() extends BlackBox{
  val io = new Bundle{
    val clock = in Bool()
    val reset = in Bool()
    val valid = in Bool()
    val pc    = in UInt(32 bits)
    val pcNext= in UInt(32 bits)
    val instr = in UInt(32 bits)
    val difftestSkip = in Bool()
  }
  noIoPrefix()
  mapClockDomain(clock = io.clock, reset = io.reset)
  addRTLPath(s"${sys.env("NPC_HOME")}/playground/vsrc/dpi-c.v")
}

// 仿真专用: mtrace 访存踪迹黑盒 (记录每次数据访存的 wen/isDev/addr/wdata + 计数器)
// isDev: 1=设备访问(串口/键盘/RTC/VGA), 0=内存访存
case class MtraceReg() extends BlackBox{
  val io = new Bundle{
    val clock = in Bool()
    val reset = in Bool()
    val valid = in Bool()
    val wen   = in Bool()
    val isDev = in Bool()
    val addr  = in UInt(32 bits)
    val wdata = in UInt(32 bits)
  }
  noIoPrefix()
  mapClockDomain(clock = io.clock, reset = io.reset)
  addRTLPath(s"${sys.env("NPC_HOME")}/playground/vsrc/dpi-c.v")
}

// 仿真专用: PerfReg 性能计数器黑盒 (4组延迟 + 8事件计数, 各模块 enableSimDebug 时实例化)
case class PerfReg() extends BlackBox{
  val io = new Bundle{
    val clock = in Bool()
    val reset = in Bool()
    val valid = in Bool()
    val req   = in Bits(4 bits)
    val rsp   = in Bits(4 bits)
    val evt   = in Bits(8 bits)
  }
  noIoPrefix()
  mapClockDomain(clock = io.clock, reset = io.reset)
  addRTLPath(s"${sys.env("NPC_HOME")}/playground/vsrc/dpi-c.v")
}

// 仿真专用: LSU 数据访存 trace 黑盒(dcache【请求侧】, 只有 enableSimDebug 时实例化)
// 与 MtraceReg 的区别: 那个截的是 dcache 之后的 AXI 事务(只有缺失流), 这个记 CPU 发出的
// 每一次数据访存(含命中) —— 给 dcache 做 DSE 用的输入序列。STA 时黑盒整体不生成(零面积)。
case class LsuTrace() extends BlackBox{
  val io = new Bundle{
    val clock = in Bool()
    val reset = in Bool()
    val valid = in Bool()
    val wen   = in Bool()
    val addr  = in UInt(32 bits)
    val size  = in UInt(3 bits)      // dcache 请求的 size = (False ## memOp[1:0]), 3 位
  }
  noIoPrefix()
  mapClockDomain(clock = io.clock, reset = io.reset)
  addRTLPath(s"${sys.env("NPC_HOME")}/playground/vsrc/dpi-c.v")
}
