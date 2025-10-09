package playground

import spinal.core._
import spinal.core.sim._
import scala.reflect.runtime.universe

// ============================================
// 顶层名称解析模块（支持反射加载）
// ============================================
object TopNameResolver {

  /** 从环境变量中获取顶层模块名称 */
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

  /** 通过反射动态创建 Spinal 顶层组件实例 */
  def createTop(name: String): Component = {
    try {
      // 完整类名（假设都在 playground 包内）
      val fullName = if (name.contains(".")) name else s"playground.$name"

      // 通过 Scala 反射机制加载模块
      val mirror = ru.runtimeMirror(getClass.getClassLoader)
      val moduleSymbol = mirror.staticModule(fullName)
      val moduleMirror = mirror.reflectModule(moduleSymbol)
      val obj = moduleMirror.instance

      // 如果是 object，尝试调用 apply()，若是 case class 则直接实例化
      obj match {
        case clazz: Component => clazz
        case _ =>
          // 若是 object + apply() 结构，则尝试调用
          val applyMethod = obj.getClass.getMethod("apply")
          val result = applyMethod.invoke(obj)
          result.asInstanceOf[Component]
      }
    } catch {
      case e: ClassNotFoundException =>
        System.err.println(s"[Error] Cannot find class or object for top '$name'. Expected '$name' or 'playground.$name'")
        sys.exit(1)
        null
      case e: Exception =>
        System.err.println(s"[Error] Failed to instantiate top module '$name': ${e.getMessage}")
        e.printStackTrace()
        sys.exit(1)
        null
    }
  }
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
  val topName = TopNameResolver.getTopName()
  val top = TopNameResolver.createTop(topName)
  Config.spinal.generateVerilog(top)
  // Config.spinal.generateVerilog(CPU())
}

// object SpinalToVhdl extends App {
//   Config.spinal.generateVhdl(Top())
// }

