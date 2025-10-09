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
  // === 获取顶层模块名 ===
  val fullName = sys.env.get("SPINAL_TOPNAME").getOrElse {
    System.err.println(
      "[Error] Missing environment variable: SPINAL_TOPNAME.\n" +
      "Example: SPINAL_TOPNAME=playground.CPU mill -i playground.runMain playground.SpinalToVerilog "
    )
    sys.exit(1)
    ""
  }

  val mirror = universe.runtimeMirror(getClass.getClassLoader)
  try {
    val moduleSymbol = mirror.staticModule(fullName)
    val module = mirror.reflectModule(moduleSymbol).instance

    // 找到 apply 方法
    val applyMethod = module.getClass.getMethods.find(_.getName == "apply")
      .getOrElse(throw new NoSuchMethodException(s"No apply() in companion object of $fullName"))

    // 解析参数
    val args = sys.env.get("SPINAL_ARGS").map(_.split(",")).getOrElse(Array.empty[String])
    val params: Array[AnyRef] =
      if (applyMethod.getParameterCount == 0) Array.empty
      else {
        if (args.length < applyMethod.getParameterCount)
          throw new IllegalArgumentException(
            s"[Error] Need ${applyMethod.getParameterCount} SPINAL_ARGS, got ${args.length}"
          )

        applyMethod.getParameterTypes.zipWithIndex.map {
          case (pt, i) =>
            val s = args(i)
            if (pt == classOf[java.lang.String]) s
            else if (pt == java.lang.Integer.TYPE) java.lang.Integer.valueOf(s)
            else if (pt == java.lang.Long.TYPE) java.lang.Long.valueOf(s)
            else if (pt == java.lang.Boolean.TYPE) java.lang.Boolean.valueOf(s)
            else throw new IllegalArgumentException(s"Unsupported param type: $pt for top $fullName")
        }.asInstanceOf[Array[AnyRef]]
      }

      Config.spinal.generateVerilog (
        applyMethod.invoke(module, params: _*).asInstanceOf[Component]
      )
  } catch {
    case e: Throwable =>
      System.err.println(s"[Error] Failed to generate HDL for '$fullName': ${e.getMessage}")
      e.printStackTrace()
      sys.exit(1)
  }
}


// object SpinalToVhdl extends App {
//   Config.spinal.generateVhdl(Top())
// }

