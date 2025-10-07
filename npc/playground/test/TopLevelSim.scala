package playground

import spinal.core._
import spinal.core.sim._

object SpinalSim extends App {
  Config.sim.compile(Adder()).doSim { dut =>
    dut.clockDomain.forkStimulus(10)
    
    println("=== Adder 完整功能测试 ===")
    
    // 定义测试用例
    val testCases = Seq(
      // (rs1, rs2, sub_add, 期望结果, 测试描述)
      (0x00000010L, 0x00000020L, 0, 0x00000030L, "小正数加法"),
      (0x7FFFFFFFL, 0x00000001L, 0, 0x80000000L, "最大正数加1（溢出）"),
      (0x80000000L, 0xFFFFFFFFL, 0, 0x7FFFFFFFL, "最小负数加-1（溢出）"),
      (0x00000020L, 0x00000010L, 1, 0x00000010L, "正数减法"),
      (0x00000010L, 0x00000020L, 1, 0xFFFFFFF0L, "小减大（负数结果）"),
      (0x00000000L, 0x00000000L, 0, 0x00000000L, "零加零"),
      (0xFFFFFFFFL, 0x00000001L, 0, 0x00000000L, "-1 + 1 = 0（进位）"),
      (0x12345678L, 0x87654321L, 0, 0x99999999L, "大数加法"),
      (0x12345678L, 0x12345678L, 1, 0x00000000L, "相同数相减"),
      (0x80000000L, 0x00000001L, 1, 0x7FFFFFFFL, "最小负数减1")
    )

    
    var passed = 0
    var total = 0
    
    for ((rs1, rs2, sub_add, expected, description) <- testCases) {
      dut.io.rs1 #= rs1
      dut.io.rs2 #= rs2
      dut.io.sub_add #= sub_add
      dut.clockDomain.waitRisingEdge()
      
      val result = dut.io.result.toLong & 0xFFFFFFFFL
      val carry = dut.io.carry.toBoolean
      val zero = dut.io.zero.toBoolean
      val overflow = dut.io.overflow.toBoolean
      
      // 计算期望的标志位
      val expectedZero = (expected == 0)
      val expectedCarry = if (sub_add == 0) {
        // 加法进位：检查是否超过32位
        (rs1.toLong + rs2.toLong) > 0xFFFFFFFFL
      } else {
        // 减法进位：检查是否没有借位
        rs1 >= rs2
      }
      
      val success = result == expected
      
      if (success) {
        passed += 1
        println(f"✓ $description: 0x$rs1%08X ${if(sub_add==0)"+"else"-"} 0x$rs2%08X = 0x$result%08X")
      } else {
        println(f"✗ $description: 0x$rs1%08X ${if(sub_add==0)"+"else"-"} 0x$rs2%08X")
        println(f"  期望: 0x$expected%08X, 实际: 0x$result%08X")
      }
      println(f"  标志: carry=$carry, zero=$zero, overflow=$overflow")  
      
      total += 1
      dut.clockDomain.waitRisingEdge() // 额外等待一个周期
    }
    
    println(f"\n测试完成: $passed/$total 通过")
    assert(passed == total, s"有 ${total - passed} 个测试失败")
  }
}
