# SpinalHDL工程示例
=======================

此工程根据根据 [SpinalTemplateSbt](https://github.com/SpinalHDL/SpinalTemplateSbt) 和 [chisel-playground](https://github.com/OSCPU/chisel-playground) 修改而成，使用 mill 作为构建工具，因为它比 sbt 速度更快。

文件内容：

* `.gitignore` - 帮助 Git 忽略垃圾文件，如生成的文件、构建产品和临时文件。
* `build.sc` - 指示 mill 构建 SpinalHDL 项目
* `Makefile` - 构建规则
* `playground` - SpinalHDL 的工程文件，"playground"为 SpinalHDL 的工程名，可以在Makefile中修改，不过要注意scala文件中的第一行要与该名称一致
* `playground/src` - 存放要生成verilog的scala文件
* `playground/test` - 存放 SpinalHDL 仿真文件
* `playground/formal` - 存放 SpinalHDL 形式化验证文件
* `playground/vsrc` - 存放 SpinalHDL 使用黑盒例化的 verilog 文件，本意用于实现 varilator 的 DPI-C 功能
* `playground/Config.scala` - 工程配置文件
* `csrc` - verilator 所用的 .c 和 .cpp 文件
* `include` - verilator 所用的头文件
* `constr` - 使用 nvboard 仿真时所用的引脚配置
  
## 使用教程

首先要安装以下所需环境：
* `verilator`
* `gtkwave`
* `mill` 
* `Java JDK` - SpinalHDL 官方推荐安装 JDK 17 (LTS)
* `nvoard` - 如有需要再安装

要运行此设计中的所有测试（建议用于测试驱动开发）：
```bash
make test
```

生成 verilog ：
```bash
make verilog
```

生成 verilog 并且使用 verilator 进行仿真
```bash
make sim
```

生成 verilog 并且使用 verilator 进行仿真，并产生波形
```bash
make wave
```

生成 verilog 并且使用 verilator 进行仿真，并使用 nvboard 查看现象
```bash
make run
```
