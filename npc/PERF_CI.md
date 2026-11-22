# NPC CI 面积优化记录

用于记录为通过 CI 面积检查而进行的部件裁减和内部结构优化。每次修改后运行 microbench(test)，记录周期、IPC 和性能计数器；再在同一份关闭仿真调试的 RTL 上依次运行本地和 CI 两套 STA，记录面积、频率及相应的真实时间。格式沿用 `PERF.md`，在汇总表中逐次追加记录。

CI Nangate45 面积预算目前按 `HANDOVER-CI.md` 中的 **16,500 µm²** 记录。本地面积用于跟踪变化；CI 流程以 **fix-fanout 后**的面积作为预算判断依据。这里的 CI 数据来自本机保留的 CI 工具流程，GitHub Actions 的实际测试结果另行记录。

## 使用方法

```sh
# 在工程根目录运行 microbench(test)，取得周期、指令、IPC 和 PERF 计数器
CCACHE_DISABLE=1 make -C npc perf git_commit=

# 本地 STA：生成关闭调试模块的综合用 RTL，并测量面积、频率
make -C npc sta git_commit=

# CI STA：随后使用同一份 RTL，具体命令见本次条目末尾
```

## 记录表

commit 使用 NPC 仓库的硬件版本；对应主仓库提交、测量条件及原始计数器见各次条目。保留 `PERF.md` 的性能统计列，删除指令类别占比，增加 STA-CI 的频率、真实时间和面积。

| commit | 说明 | 仿真周期数 | 指令数 | IPC | STA频率(MHz) | STA真实时间(ms) | STA面积(µm²) | STA-CI频率(MHz) | STA-CI真实时间(ms) | STA-CI面积(µm²) | 取指总周期(cyc) | 取指avg(cyc) | LSU访存总周期(cyc) | LSU mem rd total | LSU mem rd avg | LSU mem wr total | LSU mem wr avg | LSU dev avg(rd/wr) | EXU calc | 取指命中率% | 缺失avg(cyc) |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 6f2c190 | 移除 dcache，LSU 直连 AXI，请求仅发一拍 [nangate45] | 12599015 | 556479 | 0.0442 | 837.126 | 15.05 | 22158.066 | 658.165 | 19.14 | 25382.252 | 14718016 | 20.44 | 8883983 | 7799307 | 104.88 | 1084676 | 18.95 | 4.79/4.51 | 7798604 | 92.84 | 58.26 |
| e023c80+LSU | 合并 LSU 地址锁存，移除写数据/掩码锁存；修正首拍旁路 [nangate45，未提交] | 12599015 | 556479 | 0.0442 | 897.506 | 14.04 | 21485.884 | 602.618 | 20.91 | 25014.108 | 14718016 | 20.44 | 8883983 | 7799307 | 104.88 | 1084676 | 18.95 | 4.79/4.51 | 7798604 | 92.84 | 58.26 |
| e023c80+LSU+共享移位 | LSU 优化后启用反转共用右移的桶形移位器 [nangate45，未提交] | 12599015 | 556479 | 0.0442 | 965.664 | 13.05 | 21642.824 | 648.993 | 19.41 | 24866.478 | 14718016 | 20.44 | 8883983 | 7799307 | 104.88 | 1084676 | 18.95 | 4.79/4.51 | 7798604 | 92.84 | 58.26 |
| e023c80+LSU+共享移位+x0 | x0 常量化，15 项存储与静态写译码 [nangate45，未提交] | 12599015 | 556479 | 0.0442 | 894.678 | 14.08 | 21588.294 | 660.173 | 19.08 | 24461.626 | 14718016 | 20.44 | 8883983 | 7799307 | 104.88 | 1084676 | 18.95 | 4.79/4.51 | 7798604 | 92.84 | 58.26 |

## 指标说明

- **仿真周期数 / 指令数 / IPC**：取 `make perf` 输出的 `total execution cycle / inst / instructions per cycle`。
- **STA / STA-CI 频率与面积**：分别取本地流程和 CI 流程的报告。STA-CI 面积采用修复扇出后的结果，频率采用时序报告 `core_clock` 最差 setup 路径对应的 `Freq(MHz)`。
- **两列真实时间(ms)**：分别按 `仿真周期数 ÷ (对应频率(MHz) × 1000)` 换算，是按 STA 可达频率估算的硬件执行时间；主机运行 Verilator 的耗时另行说明。这里明确 MHz 到毫秒的换算因子。
- **取指总周期 / avg**：沿用 `PERF.md` 中 PerfReg 的统计口径。同拍请求和响应时，计数器可能读到前次请求的时间戳，avg 更接近请求间隔，不能直接当作纯取指延迟。
- **LSU访存总周期**：`LSU mem rd total + LSU mem wr total`；外设读写单独记录，`LSU dev avg` 按读/写顺序填写。
- **EXU calc**：沿用仿真原始输出的计算周期统计。当前计数器会计入流水级等待期间的有效信号保持，不能直接当作退休的计算类指令数。
- **取指命中率 / 缺失avg**：取 icache 访问和缺失计数器输出；各项原始次数、总周期在条目中保留。
- 汇总表不记录 `calc% / mem% / branch% / jump% / csr% / sys%` 等指令类别占比。

## 测量约定

- 先提交并推送待测代码，再测量，记录明确的提交号。运行 microbench(test) 补齐性能统计；仿真启用调试计数器，综合关闭调试模块，避免计数器面积进入 STA。
- 本地流程：`/home/cyl/tools/yosys-sta`；CI 流程：工程内 `tools/yosys-sta-ci`，对应保留的 `OSCPU/yosys-sta` 的 `ci` 分支副本。
- 使用 `make -C npc sta` 生成 `npc/build/sta/ysyx_23060082.v`，顶层为 `ysyx_23060082`，`SPINAL_SIM_DEBUG=0`，`PARTFORM=ysyxsoc`。
- 两套流程使用同一个 RTL 文件，工艺为 `nangate45`，目标频率为 **500 MHz**（周期 **2 ns**），时钟端口为 `clock`。
- 本地沿用 `npc/constr/npc.sdc`；CI 沿用工具自带的 `scripts/default.sdc`，复现 CI 的默认约束。本地约束额外设置输入转换时间 `0.1 ns`，两套约束并不完全相同。
- 本地面积取 `synth_stat.txt`；CI 面积取 `synth_stat_fixed.txt`，并与 `yosys-fixed.log` 中的 `Chip area` 核对。CI 的 `synth_stat.txt` 是修复扇出前的中间结果。
- STA 按顺序手动执行。保留 `npc/Makefile` 的现有规则。
- 记录实际使用的 Yosys 二进制版本和两套流程仓库的提交；若 GitHub runner 更新二进制或流程，重新建立对比基线。
- 面积变化只在相同流程内比较。历史记录没有重新做严格 A/B 时，标明历史参照，避免把全部差值归因于单一修改。

## 2026-11-20：移除 LSU 中的 dcache

### 对应代码与改动

测量前已推送到 GitHub：

| 仓库 | 分支 | 待测提交 |
| --- | --- | --- |
| `chaoyulong/ysyx-workbench` | `master` | `dfc3811ea37e9a670af3344a4ac39e08550394de` |
| `chaoyulong/npc` | `main` | `6f2c1902e64975915c32863331b8b0a53bcd80f3` |

本次结构变化：

1. LSU 不再实例化 dcache，直接连接 AXI 控制器，保留访存的数据整理、异常和响应处理。
2. AXI 控制器定义移到 `LSU.scala`，原文件中的重复定义已注释。
3. 访存请求只在 `LsuState.Idle` 且没有异常时发送一拍；等待响应期间由控制器保持通道 `valid`，防止重复 AXI 请求。
4. 仿真端注释 dcache 命中率统计，并增加本地 NVBoard 结束提示。这些 C/C++ 变化不参与 CPU 综合。

测量配置：保留 icache（默认 8 行 × 16 字节）、16 个通用寄存器；`enableMul=false`、`enableDiv=false`、`enableInterrupt=false`、`enableSimDebug=false`；复位 PC 为 `0x30000000`。生成 RTL 中 `PerfReg`、`ItraceReg`、`MtraceReg`、`ysyx_23060082_Dcache` 的出现次数均为 0。

### 性能计数器实测

运行 `CCACHE_DISABLE=1 make -C npc perf git_commit=`，microbench 的 test 规模十项测试全部通过，输出 `MicroBench PASS`，最终 `HIT GOOD TRAP at pc = 0xa00056b4`。

- 总周期：**12,599,015**；退休指令数：**556,479**；IPC：**0.0442**。
- 本地 STA 频率换算时间：`12599015 / (837.126 × 1000)` = **15.05 ms**。
- CI STA 频率换算时间：`12599015 / (658.165 × 1000)` = **19.14 ms**。
- 主机仿真耗时：`host time spent = 11362861 us`，约 **11.36 s**，与表中硬件执行时间分别记录。

| 计数器 | 次数 cnt | 总周期 total | 平均周期 avg |
| --- | ---: | ---: | ---: |
| IFU fetch | 720,131 | 14,718,016 | 20.44 |
| Icache miss | 51,591 | 3,005,873 | 58.26 |
| LSU mem rd | 74,362 | 7,799,307 | 104.88 |
| LSU mem wr | 57,247 | 1,084,676 | 18.95 |
| LSU dev rd | 657 | 3,147 | 4.79 |
| LSU dev wr | 621 | 2,801 | 4.51 |
| EXU calc | 7,798,604 | 7,798,604 | 1.00 |

Icache access：**720,132** 次，缺失 **51,592** 次，命中率 **92.84%**。缺失延迟计数器完成 **51,591** 次，保留原始输出中“发生缺失”和“完成计时”相差 1 次的结果。LSU 内存读写总周期为 **8,883,983**。dcache 已移除，不再记录其命中率。

性能补测时 NPC HEAD 为 `e023c80`，其硬件和仿真源码与待测提交 `6f2c190` 相同，期间只有记录文档变化；因此复用上一轮同硬件版本的双 STA 结果。

测试镜像：`am-kernels/benchmarks/microbench/build/microbench-riscv32e-ysyxsoc.bin`，参数为 `test`，SHA-256 为 `dfc1c9033632e31fc42dc22e4af3e2f15ef41de15788b7e9101ea1f8d7b0ba0a`。

### 工具与输入

| 项目 | 本次值 |
| --- | --- |
| Yosys（二套流程均使用本机同一二进制） | `0.69+117`，git `e8db64c60-dirty`，Release，Clang 21.1.8 |
| Yosys 路径 | `/home/cyl/tools/oss-cad-suite/bin/yosys` |
| oss-cad-suite 的 `VERSION` | `20260922` |
| 本地 yosys-sta 提交 | `72495ad5619a9d5053c3a2748db1b295d5df3fb4`，运行前工作树干净 |
| CI yosys-sta 提交 | `df02806ccef21f546f8038f97cffb217231ff6eb`，运行前工作树干净 |
| iEDA（两套工具相同，取自报告） | GitVersion `aa25008b3778a76ae1975c839d4cc459f7f7b3b7` |
| 工艺库 | `pdk/nangate45/lib/Nangate45_typ.lib` |
| 共用 RTL | `npc/build/sta/ysyx_23060082.v` |
| RTL SHA-256 | `7e3cb68f8329d00316d83fc93f9517b716d25ced5d4a9a394fb7356300ab875a` |

这里对齐的是保留的 CI 脚本和默认流程；尚未核对本次 GitHub runner 的 Yosys 二进制是否与本机完全一致。

### 实测结果

两条 `make sta` 均正常退出。面积为标准单元面积总和，单位为 µm²。

| 流程 | 面积（µm²） | 单元数 | DFF_X1 数 | core_clock 最差 setup slack（ns） | 报告估算频率（MHz） | core_clock setup / hold TNS（ns） |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| 本地流程，综合后直接 STA | **22,158.066** | 10,073 | 2,919 | +0.805 | 837.126 | 0.000 / 0.000 |
| CI 流程，fix-fanout 后 STA | **25,382.252** | 12,306 | 2,919 | +0.481 | 658.165 | 0.000 / 0.000 |

CI 修复扇出前面积为 **25,292.344 µm²**，修复后增加 **89.908 µm²**。本地和 CI 的映射脚本也有差异，两套最终面积之差不能全部归因于插入 buffer。

目前 500 MHz 时序余量为正，CI 面积仍超出 16,500 µm² 预算 **8,882.252 µm²**；还需在当前 CI 面积基础上减少约 **34.99%**。此结果不表示整套 CI 已通过。

### 历史参照

以下改动前面积取自 `HANDOVER-CI.md` 的既有测量，本次未重新生成改动前 RTL 做严格 A/B：

| 流程 | 历史面积（µm²） | 本次面积（µm²） | 较历史减少（µm²） | 减少比例 |
| --- | ---: | ---: | ---: | ---: |
| 本地流程 | 24,208.930 | 22,158.066 | 2,050.864 | 8.47% |
| CI 流程 | 27,546.960 | 25,382.252 | 2,164.708 | 7.86% |

历史 DFF 数为 3,198，本次为 2,919，少 279 个。后续优化以本次双流程实测值作为新的跟踪基线。

### 功能验证

- 修复请求握手后，hello 仿真打印 `HIT GOOD TRAP at pc = 0xa0000338`：10,586 条指令、902,117 周期。
- 用户已手动确认本地 RT-Thread 可以运行。
- 已补测 microbench(test) 并记录上面的完整性能计数器；本次未重新执行完整 GitHub CI。

### 复现命令和报告位置

在工程根目录执行以下命令，先测 microbench，再依次运行本地和 CI STA：

```sh
cd /home/cyl/Desktop/ysyx-workbench
CCACHE_DISABLE=1 make -C npc perf git_commit=
make -C npc sta git_commit=

make -C /home/cyl/Desktop/ysyx-workbench/tools/yosys-sta-ci sta \
  DESIGN=ysyx_23060082 PDK=nangate45 \
  CLK_FREQ_MHZ=500 CLK_PORT_NAME=clock \
  RTL_FILES=/home/cyl/Desktop/ysyx-workbench/npc/build/sta/ysyx_23060082.v \
  O=/home/cyl/Desktop/ysyx-workbench/npc/build/sta-ci
```

`git_commit=` 用于禁止根 Makefile 的自动追踪提交；本次 STA 使用的代码提交号见上表。生成的 RTL 带有生成时间，重新生成后应重新记录 SHA-256。

| 内容 | 文件（相对工程根目录） |
| --- | --- |
| 本地面积 | `npc/build/sta/ysyx_23060082-500MHz/synth_stat.txt` |
| 本地时序 | `npc/build/sta/ysyx_23060082-500MHz/ysyx_23060082.rpt` |
| CI 修复前面积 | `npc/build/sta-ci/ysyx_23060082-500MHz/synth_stat.txt` |
| CI 最终面积 | `npc/build/sta-ci/ysyx_23060082-500MHz/synth_stat_fixed.txt` |
| CI 最终面积核对日志 | `npc/build/sta-ci/ysyx_23060082-500MHz/yosys-fixed.log` |
| CI 时序 | `npc/build/sta-ci/ysyx_23060082-500MHz/ysyx_23060082.rpt` |

本次完整执行日志：`/tmp/perf-ci-20261120-microbench.log`、`/tmp/perf-ci-20261120-local-sta.log`、`/tmp/perf-ci-20261120-ci-sta.log`。`build/` 和 `/tmp` 中的报告是本地产物，后续 clean 或重新综合可能覆盖；关键结果和待测提交保存在本记录中。


## 2026-11-20：LSU 寄存器裁减与共享移位器 A/B

本轮先保留原 ALU 移位表达式，测量用户裁减 LSU 寄存器后的结果；再启用 `EXU.scala` 中原先注释的共享桶形移位器，重新测试两套 STA 和 microbench。两版均基于 NPC HEAD `e023c80e63151ad08704ca12cb7cc4c3828f93d9` 的工作区修改；下方给出源码、RTL 快照，汇总表中的 `+LSU` 和 `+LSU+共享移位` 为本地测量标签，不是 Git 提交号。本轮未提交。

### 改动与功能前提

- LSU 将读、写地址锁存合并，删除写数据、写掩码的重复锁存，总共减少 **68 位寄存器**（32 + 32 + 4）。实测 DFF_X1 从 **2,919** 降到 **2,851**。`dcache.scala` 的整体注释为用户改动，dcache 原已不实例化。
- 检查中发现共享地址的旁路只判断 `readReq`：当写请求首拍 AWVALID 和 AWREADY 同时有效时，会发出上一笔地址。建立本轮基线前已改为 `Mux(io.readReq || io.writeReq, io.addr, addrReg)`，并删除遗留未连接的 `writeAddr` 输入。
- 删除 WDATA/WSTRB 锁存依赖上层 LSU 级间寄存器保持输入。当前流水线在访存响应返回、LSU 指令完成前不会接收下一条输入，所以数据、掩码和 size 可以直接使用；该控制器目前只服务一个未完成事务。
- ALU 将三种移位统一为反转输入 → 共用五级右移器 → 必要时反转输出，保持组合逻辑，没有增加流水级或指令周期。

### 同条件实测

两套流程均为 Nangate45、目标 500 MHz；每一版的本地和 CI 流程使用相同 RTL，关闭仿真调试。工具版本和本地/CI SDC 差异沿用上一条记录，本轮核对两套脚本提交号没有变化。这里的 CI 是本机保留的 CI 脚本流程。

| 版本 | 本地面积(µm²) | 本地频率(MHz) | 本地 setup slack(ns) | CI 最终面积(µm²) | CI 频率(MHz) | CI setup slack(ns) | DFF_X1 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 原基线（沿用 16:18～16:19 报告） | 22158.066 | 837.126 | +0.805 | 25382.252 | 658.165 | +0.481 | 2919 |
| 仅 LSU 优化（原移位器） | 21485.884 | 897.506 | +0.886 | 25014.108 | 602.618 | +0.341 | 2851 |
| LSU 优化 + 共享移位器 | 21642.824 | 965.664 | +0.964 | 24866.478 | 648.993 | +0.459 | 2851 |

| 单独改动 | 本地面积变化 | CI 最终面积变化 | 本地频率变化 | CI 频率变化 |
| --- | ---: | ---: | ---: | ---: |
| LSU 裁减（相对原基线） | -672.182 µm²（-3.03%） | -368.144 µm²（-1.45%） | +60.380 MHz（+7.21%） | -55.547 MHz（-8.44%） |
| 共享移位器（相对仅 LSU） | +156.940 µm²（+0.73%） | -147.630 µm²（-0.59%） | +68.158 MHz（+7.59%） | +46.375 MHz（+7.70%） |

共享移位器在 CI 面积上有小幅收益，频率也回升；本地流程面积略增。两套脚本使用不同的 ABC 映射策略，对完整核心重新映射，不能把门级面积或关键路径变化完全解释为移位器自身的变化，也不能将源码“共用”直接等同于最终面积一定下降。

本轮最终本地/CI 单元数分别为 **9,726 / 12,195**，仅 LSU 版本为 **9,751 / 12,445**。本地关键端点从 `ifu.icache.axi4Ctrler.io_readAddr_28__reg_p:D` 变为 `lsu.csr.minstreth_29__reg_p:D`；CI 从 `lsu.csr.mcycleh_$_SDFF_PP0__Q_19:D` 变为 `ifu.state_$_DFF_P__Q_3:D`。报告频率是当前模型下完整核心的估算值，未做布局布线。最终 CI 面积 **24,866.478 µm²**，距 16,500 µm² 预算还差 **8,366.478 µm²**。

### 性能计数器与验证

两版 microbench(test) 都完整通过 10 个子项，均为 **12,599,015 周期 / 556,479 指令 / IPC 0.0442**，下面全部性能计数器一致：

| 计数器 | cnt | total(cyc) | avg(cyc) |
| --- | ---: | ---: | ---: |
| IFU fetch | 720131 | 14718016 | 20.44 |
| Icache miss | 51591 | 3005873 | 58.26 |
| LSU mem rd | 74362 | 7799307 | 104.88 |
| LSU mem wr | 57247 | 1084676 | 18.95 |
| LSU dev rd | 657 | 3147 | 4.79 |
| LSU dev wr | 621 | 2801 | 4.51 |
| EXU calc | 7798604 | 7798604 | 1.00 |

Icache access **720132**、miss **51592**、hit **92.84%**。计数口径沿用前条说明。按各版对应 STA 频率换算的硬件执行时间已填入汇总表；两版程序周期相同，时间变化完全来自报告频率。主机仿真耗时分别约 **11.07 s / 11.41 s**，不作为硬件性能指标。测试镜像与前条一致（32608 字节，`mainargs=test`）。

- 新旧完整 ALU 的组合逻辑做 Yosys SAT 形式等价验证：比较 `aluResult`、`less`、`zero`，证明任意两个 32 位操作数、全部 16 个控制码下输出一致，结果 `SUCCESS`。
- 共享移位器版本 `cpu-tests ALL=shift` 通过。
- LSU AXI 控制器定向测试通过：AWREADY/WREADY 独立延迟 0～4 拍的 25 种组合，ARREADY 延迟 0～4 拍；检查首拍地址、请求脉冲后的地址保持、WDATA/WSTRB、每通道只握手一次、OKAY/错误响应。测试期间写数据/掩码/size 按上层约定保持到事务完成。
- 两版双 STA 均正常退出；未运行整套 GitHub CI。本轮未修改 `npc/Makefile`。

### 输入与报告保存

测量基于主仓库 HEAD `f4d1f252963cd3df1b411912beff866f08fc1817` 和上述 NPC HEAD 的未提交工作区。旧报告、仅 LSU 报告、共享移位器报告、源码快照、等价检查和 AXI 测试均保存在：

`npc/build/compare-lsu-shifter-20261120/`

| 输入版本 | RTL SHA-256 |
| --- | --- |
| 仅 LSU 优化 | `1dec7f401ff9c2ed376c0693e4766c6dcb974d007ce5164e31bd49c39810f5bb` |
| LSU 优化 + 共享移位器 | `2833be2f48454a0cd450ab14ce5dfc4c04aaa80766b64ecb967c7f74611c7c7a` |

目录中 `lsu-local/`、`lsu-ci/`、`shared-local/`、`shared-ci/` 分别保存独立报告；`previous-local/` 和 `previous-ci/` 是原基线报告备份。`metrics.json` 保存结构化指标，`LSU.scala`、`EXU-original.scala`、`EXU-shared.scala` 保存 A/B 源码。`build/` 是忽略的本地测量产物。

本轮命令仍为前条的 `make -C npc sta git_commit=`、手动调用 CI `make sta` 和 `CCACHE_DISABLE=1 make -C npc perf git_commit=`；在切换移位器前后各执行一遍。新增验证命令：

```sh
CCACHE_DISABLE=1 make -C am-kernels/tests/cpu-tests \
  ARCH=riscv32e-ysyxsoc ALL=shift sim sdb=n git_commit=
yosys -s npc/build/compare-lsu-shifter-20261120/alu-equivalence.ys
iverilog -g2012 -s tb \
  -o npc/build/compare-lsu-shifter-20261120/axi-test.vvp \
  npc/build/compare-lsu-shifter-20261120/lsu-local/ysyx_23060082.v \
  npc/build/compare-lsu-shifter-20261120/axi-test.sv
vvp npc/build/compare-lsu-shifter-20261120/axi-test.vvp
```

完整日志为 `/tmp/perf-ci-20261120-lsu-{local-sta,ci-sta,microbench}.log`、`/tmp/perf-ci-20261120-shifter-{local-sta,ci-sta,microbench,cpu-shift}.log` 和 `/tmp/perf-ci-20261120-alu-equivalence.log`，已复制到上述对比目录。


## 2026-11-20：x0 改为常量，寄存器堆仅保存 x1～x15

本次测量用户修改后的工作区，未提交 Git。NPC HEAD 仍为 `e023c80e63151ad08704ca12cb7cc4c3828f93d9`，对照版本为上一条的“LSU 优化 + 共享移位器”。除寄存器堆及配套仿真读取外，LSU、共享桶形移位器等沿用上一版本；测量前后保存的源码内容一致。没有修改 `npc/Makefile`。

### 改动与网表核对

- `RegFile.scala` 将 16 项寄存器改成 15 项 `rfStorage`，仅存储 x1～x15；16 项读取视图中的 x0 直接连接常量零。
- 每个实际寄存器使用静态写地址比较，写 x0 时没有任何存储写入。
- 新综合 RTL 中 `rf_0` 为 wire，`assign rf_0 = 32'h0`，仅有 `rfStorage_0`～`rfStorage_14` 共 15 个 32 位寄存器。
- 两套网表的寄存器堆 DFF 均从 **512 降为 480**，x0 的 DFF 从 **32 降为 0**；全核 DFF_X1 均从 **2851 降为 2819**。因此这次确实删除了 32 位实际存储。

### 双 STA 对比

沿用同一份关闭调试的 RTL、Nangate45 工艺和 500 MHz 目标。本地约束、CI 默认约束以及两套工具版本均沿用上一条，二套 `make sta` 均正常退出；综合检查均为 0 problems。

| 指标 | 修改前：本地 | 修改后：本地 | 修改前：CI | 修改后：CI |
| --- | ---: | ---: | ---: | ---: |
| 最终面积（µm²） | 21642.824 | 21588.294 | 24866.478 | 24461.626 |
| 面积变化（µm² / %） | — | -54.530 / -0.25% | — | -404.852 / -1.63% |
| 报告估算频率（MHz） | 965.664 | 894.678 | 648.993 | 660.173 |
| 频率变化 | — | -7.35% | — | +1.72% |
| 最差 setup slack（ns） | +0.964 | +0.882 | +0.459 | +0.485 |
| 标准单元数 | 9726 | 9981 | 12195 | 12047 |
| DFF_X1 | 2851 | 2819 | 2851 | 2819 |
| 寄存器堆 DFF | 512 | 480 | 512 | 480 |
| microbench 估算硬件时间（ms） | 13.05 | 14.08 | 19.41 | 19.08 |

CI 修复扇出前面积 **24371.718 µm²**，修复后 **24461.626 µm²**，增加 **89.908 µm²**。当前仍比 16500 µm² 预算高 **7961.626 µm²**。

Nangate45 的 DFF_X1 面积为 4.522 µm²，32 个触发器直接对应 144.704 µm²。最终全核面积变化还包含静态写译码、读选择和 ABC 重映射的影响，所以本地总面积减少量小于 144.704 µm²，而 CI 减少量大于该值。

本地关键端点由 `lsu.csr.minstreth_29__reg_p:D` 变为 `ifu.stopFetch_reg_p:D`；CI 由 `ifu.state_$_DFF_P__Q_3:D` 变为 `ifu.pcFetch_$_SDFFE_PP1P__Q_1:D`。两套报告的 core_clock setup / hold TNS 均为 **0.000 / 0.000 ns**。两套流程的频率变化方向不同，保留各自实测值；这些是当前时序模型下的估算值。

### 性能计数器与功能验证

`CCACHE_DISABLE=1 make -C npc perf git_commit=` 正常结束，microbench(test) 输出 **MicroBench PASS** 与 **HIT GOOD TRAP at pc = 0xa00056b4**。总周期 **12,599,015**、指令 **556,479**、IPC **0.0442**，与修改前完全一致，各项性能计数器也一致：

| 计数器 | cnt | total(cyc) | avg(cyc) |
| --- | ---: | ---: | ---: |
| IFU fetch | 720131 | 14718016 | 20.44 |
| Icache miss | 51591 | 3005873 | 58.26 |
| LSU mem rd | 74362 | 7799307 | 104.88 |
| LSU mem wr | 57247 | 1084676 | 18.95 |
| LSU dev rd | 657 | 3147 | 4.79 |
| LSU dev wr | 621 | 2801 | 4.51 |
| EXU calc | 7798604 | 7798604 | 1.00 |

Icache access **720132**、miss **51592**、hit **92.84%**；LSU 内存读写总周期 **8883983**。主机仿真耗时 **11686833 us**（约 **11.69 s**）。按两套 STA 频率估算的硬件执行时间分别为 **14.08 ms / 19.08 ms**，已记入汇总表。镜像 SHA-256 为 `dfc1c9033632e31fc42dc22e4af3e2f15ef41de15788b7e9101ea1f8d7b0ba0a`。未运行整套 GitHub CI。

当前 `regfile.c` 将 x0 直接返回零，其他寄存器仍通过 `&rfStorage_0` 的指针偏移读取。当前生成布局下 microbench 已通过，但跨独立 C++ 成员的数组式访问仍依赖生成布局；建议后续按字段显式映射读取。这一仿真端问题不影响本次 STA 面积。

### 输入、工具与保存位置

- RTL SHA-256：`ea8aaeeea24aa0ed517f24095485381bab6fc6c6c3de70229e05478190badfce`。
- Yosys：`Yosys 0.69+117 (git sha1 e8db64c60-dirty, Release, Clang /usr/bin/clang++ 21.1.8)`。
- 本地工具提交：`72495ad5619a9d5053c3a2748db1b295d5df3fb4`；CI 工具提交：`df02806ccef21f546f8038f97cffb217231ff6eb`。
- 对比目录：`npc/build/compare-x0-20261120/`，含源码快照、综合 RTL、`metrics.json`、修改前后两套报告与完整日志。
- 本地及 CI 当前报告仍位于 `npc/build/sta/ysyx_23060082-500MHz/` 和 `npc/build/sta-ci/ysyx_23060082-500MHz/`。

复现命令沿用本文件“使用方法”和首条记录中的 CI 手动命令。本次依次运行本地、CI 两套 STA，并补测 microbench，均传入 `git_commit=` 禁止自动提交。完整日志为 `/tmp/perf-ci-20261120-x0-local-sta.log`、`/tmp/perf-ci-20261120-x0-ci-sta.log`、`/tmp/perf-ci-20261120-x0-microbench.log`，已复制到上述对比目录。
