package playground

import spinal.core._
import spinal.core.sim._

object SpinalSim extends App {
  Config.sim.compile(ALU()).doSim { dut =>
    dut.clockDomain.forkStimulus(10)
    
    println("=== ALU 完整功能测试 ===")
    
    def runTest(rs1: Long, rs2: Long, alu_ctr: Int, expected: Long, description: String): Boolean = {
      dut.io.rs1 #= (rs1 & 0xFFFFFFFFL).toInt
      dut.io.rs2 #= (rs2 & 0xFFFFFFFFL).toInt
      dut.io.alu_ctr #= alu_ctr
      
      dut.clockDomain.waitRisingEdge()
      dut.clockDomain.waitRisingEdge() // 额外等待一个周期确保稳定
      
      val result = dut.io.alu_out.toLong & 0xFFFFFFFFL
      val success = result == expected
      
      if (!success) {
        println(f"✗ $description")
        println(f"  输入: rs1=0x$rs1%08X, rs2=0x$rs2%08X, ctr=0x$alu_ctr%X")
        println(f"  期望: 0x$expected%08X, 实际: 0x$result%08X")
        println(f"  标志: less=${dut.io.less.toBoolean}, zero=${dut.io.zero.toBoolean}")
      }
      
      success
    }
    
    var passed = 0
    
    // 测试 ADD/SUB
    passed += (if (runTest(10, 20, 0x0, 30, "ADD: 正数加法")) 1 else 0)
    passed += (if (runTest(0x7FFFFFFF, 1, 0x0, 0x80000000L, "ADD: 正数溢出")) 1 else 0)
    passed += (if (runTest(0x80000000L, 0xFFFFFFFFL, 0x0, 0x7FFFFFFF, "ADD: 负数加-1")) 1 else 0)
    passed += (if (runTest(50, 30, 0x8, 20, "SUB: 正数减法")) 1 else 0)
    passed += (if (runTest(10, 20, 0x8, 0xFFFFFFF6L, "SUB: 结果为负")) 1 else 0)
    
    // 测试移位
    passed += (if (runTest(0xF, 4, 0x1, 0xF0, "SLL: 左移")) 1 else 0)
    passed += (if (runTest(0xFFFFFFF0L, 4, 0x5, 0x0FFFFFFF, "SRL: 逻辑右移")) 1 else 0)
    passed += (if (runTest(0x80000000L, 4, 0xD, 0xF8000000L, "SRA: 算术右移")) 1 else 0)
    
    // 测试比较
    passed += (if (runTest(10, 20, 0x2, 1, "SLT: 10 < 20")) 1 else 0)
    passed += (if (runTest(20, 10, 0x2, 0, "SLT: 20 > 10")) 1 else 0)
    passed += (if (runTest(0x80000000L, 0x7FFFFFFF, 0x2, 1, "SLT: 有符号比较")) 1 else 0)
    passed += (if (runTest(0x80000000L, 0x7FFFFFFF, 0xA, 0, "SLTU: 无符号比较")) 1 else 0)
    
    // 测试逻辑运算
    passed += (if (runTest(0x12345678, 0x87654321, 0x4, 0x95511559L, "XOR")) 1 else 0)
    passed += (if (runTest(0x12345678, 0x87654321, 0x6, 0x97755779L, "OR")) 1 else 0)
    passed += (if (runTest(0x12345678, 0x87654321, 0x7, 0x02244220, "AND")) 1 else 0)
    
    // 测试 LUI
    passed += (if (runTest(0x12345678, 0x0000ABCD, 0x3, 0x0000ABCD, "LUI")) 1 else 0)
    
    val total = 15
    println(f"\n测试完成: $passed/$total 通过")
    
    if (passed == total) {
      println("🎉 所有测试通过! ALU 功能正常")
    } else {
      println(s"❌ 有 ${total - passed} 个测试失败，请检查 ALU 实现")
    }
  }
}
