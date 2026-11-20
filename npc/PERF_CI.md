# NPC CI 面积优化记录

用于记录为通过 CI 面积检查而进行的部件裁减和内部结构优化。每次涉及硬件结构的修改，都在同一份关闭仿真调试的 RTL 上依次运行本地和 CI 两套 STA，记录代码提交、工具版本、面积、时序及功能验证结果，按日期追加条目。

CI Nangate45 面积预算目前按 `HANDOVER-CI.md` 中的 **16,500 µm²** 记录。本地面积用于跟踪变化；CI 流程以 **fix-fanout 后**的面积作为预算判断依据。这里的 CI 数据来自本机保留的 CI 工具流程，GitHub Actions 的实际测试结果另行记录。

## 测量约定

- 先提交并推送待测代码，再测量，记录明确的提交号。
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
- 本次记录未重新执行完整 GitHub CI，也未重新测量 microbench 性能；移除数据缓存后的性能变化需后续实测。

### 复现命令和报告位置

在工程根目录执行以下命令，先完成本地流程，再运行 CI 流程：

```sh
cd /home/cyl/Desktop/ysyx-workbench
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

本次完整执行日志：`/tmp/perf-ci-20261120-local-sta.log`、`/tmp/perf-ci-20261120-ci-sta.log`。`build/` 和 `/tmp` 中的报告是本地产物，后续 clean 或重新综合可能覆盖；关键结果和待测提交保存在本记录中。
