# cachesim —— icache 功能模拟器（独立构建）
#
# 本程序完全独立于 NPC 主构建流程，只依赖 libc。
# 编译产物 cachesim 会被 .gitignore 忽略（不入库）。

CC      ?= gcc
CFLAGS  ?= -O2 -Wall -Wextra
BIN     := cachesim

all: $(BIN)

$(BIN): cachesim.c
	$(CC) $(CFLAGS) -o $@ $<

clean:
	rm -f $(BIN)
	rm -rf traces/*.log

.PHONY: all clean
