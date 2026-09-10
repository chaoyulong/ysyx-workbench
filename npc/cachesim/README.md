# cachesim —— icache 功能模拟器

用于 **cache 设计空间探索（DSE）**：回放取指 PC 序列，统计不同 cache 参数下的缺失次数。

## 为什么快

对于给定的访存地址序列，**缺失次数与访存内容无关**，只与地址有关。所以：

- **不模拟数据**，只维护元数据（valid / tag / 替换信息）
- **不执行指令**，直接回放 PC 序列（序列里已包含每次分支的真实方向）
- 因此不需要 NPC、不需要 Verilator、不需要 NEMU

比 RTL 仿真相差几千倍，可以在数分钟内扫完几十组参数组合。

## 编译运行

```bash
make                        # 生成 ./cachesim
./cachesim traces/shift.log --lines 16 --block 4
```

`cachesim` 完全独立（只依赖 libc），可以拷到任何地方跑。

## 参数

| 参数 | 说明 | 默认 |
|---|---|---|
| `--lines N` | cache 总块数（对应 RTL 的 `IcacheParams.lines`） | 16 |
| `--block B` | 块大小(字节)（对应 `lineBytes`） | 4 |
| `--ways W` | 相联度，1 = 直接映射 | 1 |
| `--repl P` | 替换算法 `lru` / `fifo` / `rand` | lru |
| `--misscost C` | 平均缺失代价(cyc)，给出则计算 TMT | — |
| `--from ADDR` | 只统计 `pc >= ADDR` 的访问（排除 boot 阶段） | 0 |
| `--skip N` | 跳过前 N 行 | 0 |
| `--max N` | 最多处理 N 行 | 不限 |

输出一行 `RESULT:` 便于脚本扫描参数组合：

```
RESULT: lines=64  ways=1  block=4  repl=lru  access=20000  miss=40  hit= 99.80%
```

## PC 序列怎么来

`cachesim` 不认识任何平台，只要能拿到 PC 序列即可。三种来源：

### ① NPC 仿真的 itrace（最省事）

1. 打开 `include/config.h` 里的 `CONFIG_TRACE` 和 `CONFIG_ITRACE`
2. 仿真时加 `-l <文件>`（AM 平台的 `ysyxsoc.mk` 已自动传 `-l build/ysyxsoc-log.txt`）
3. 跑完之后把日志拷到 `traces/`

日志格式是 `0x00000000: <反汇编>`，`cachesim` 会取冒号前的 PC，并从反汇编里识别 `fence.i`（用于清空 cache）。

### ② NEMU 生成的 itrace（文档推荐）

比 NPC 快得多。需要让 NEMU 能跑 `riscv32e-ysyxsoc` 的镜像。

### ③ 自己造（只适合验证 cachesim 本身）

任何一行一个 PC 的文本文件都能当输入。

## trace 压缩（可选）

日志很大时可以先压，再用管道喂给 cachesim：

```bash
bzip2 traces/shift.log
bzcat traces/shift.log.bz2 | ./cachesim - --lines 16
```

## 与 RTL 对拍（性能 DiffTest）

`cachesim` 可以作为**性能测试的 REF**：同一段程序，NPC 的 PERF 计数器报告的 icache 缺失次数，应该与 cachesim 回放同一份 itrace 得到的结果**完全一致**。不一致说明 RTL 有性能 bug（功能测试和形式化验证都发现不了这类问题）。

NPC 侧的缺失次数由 `make perf` 输出里的 `Icache access` 行给出。

> 注意：对拍要保证两边参数一致（`lines` / `block`），并且 `fence.i` 的处理方式一致。
