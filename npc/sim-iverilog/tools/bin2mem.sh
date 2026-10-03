#!/bin/sh
#=====================================================================
# bin2mem.sh —— 把 .bin 转成 $readmemh 能读的十六进制文本
#
# 用法:  bin2mem.sh <in.bin> <out.hex>
#
# 为什么需要它：
#   - CI 的 sim-iverilog / sim-iverilog-netlist 只收 .bin，而 $readmemh 读不了 .bin
#   - 输出格式 = 每行一个字节（与 objcopy -O verilog 的格式一致），
#     配合 TB 里的字节数组 `reg [7:0] psram[...]` 使用
#   - 镜像链接在 0x80000000，而 $readmemh 从下标 0 开始
#     ⇒ TB 把下标 0 直接当作 0x80000000（不做地址重定位）
#=====================================================================
set -e

if [ $# -ne 2 ]; then
  echo "用法: $0 <in.bin> <out.hex>" >&2
  exit 1
fi

in=$1
out=$2

[ -f "$in" ] || { echo "bin2mem: 找不到输入文件 $in" >&2; exit 1; }

# od命令用于输出文件内容
# -An 不打印地址列
# -v : 不把重复行折叠成*,bss段里全0，不加-v会丢数据
# -tx1 ：每字节一个字段、十六进制小写，默认一行16字节
# tr -s ' ' '\n' ：把连续空格压成一个换行
# grep -v '^$' ：去掉od行首那个空格造成的空行
od -An -v -tx1 "$in" | tr -s ' ' '\n' | grep -v '^$' > "$out"

printf 'bin2mem: %s (%s 字节) -> %s (%s 字节数据行)\n' \
       "$in" "$(stat -c%s "$in")" "$out" "$(wc -l < "$out")"
