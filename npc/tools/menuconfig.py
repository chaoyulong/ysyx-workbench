#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# menuconfig: 终端图形配置界面, 切换 include/config.h 的功能开关
# 用法: make menuconfig  (或直接 python3 tools/menuconfig.py)
# 按键: ↑/↓ 或 j/k 移动, 空格 切换, s 保存, r 重置(重新读取), q 退出(不保存)

import os
import re
import sys
import curses

CONFIG_H = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "include", "config.h")

# 匹配 config.h 里的一行宏定义: 行首缩进 + 可选的 "//" 注释前缀 + #define + 宏名 + 其余部分
# 用命名组, 并让 load_state / save_state **共用同一个正则**, 免得两处的规则各自漂移
#   indent  : 行首缩进                      comment : "//" 或 None(未注释 = 宏打开)
#   gap     : "//" 与 "#define" 之间的空白   name    : 宏名
#   rest    : 值及其后的内容(含行尾注释, 原样保留)
DEFINE_RE = re.compile(
    r"^(?P<indent>\s*)(?:(?P<comment>//)(?P<gap>\s*))?#define\s+(?P<name>\w+)(?P<rest>\s*.*)$")

# 选项定义: (宏名, 描述, 依赖的宏)
# 依赖: 父开关关闭时子项不可开启(灰显)
OPTIONS = [
    ("CONFIG_TRACE",  "Trace 总开关(配合 log_enable 按指令范围写日志)", None),
    ("CONFIG_ITRACE", "指令 trace(每条指令打印到 log + ringbuf)", "CONFIG_TRACE"),
    ("CONFIG_MTRACE", "访存 trace(数据访存记录, 内存/设备分类)", "CONFIG_TRACE"),
    ("CONFIG_MTRACE_PC", "mtrace 打印访存指令 pc", "CONFIG_MTRACE"),
    ("CONFIG_FTRACE", "函数 trace(记录函数调用, 需要 -f <elf>)", "CONFIG_TRACE"),
    ("CONFIG_FTRACE_FILTER_INTERNAL", "ftrace 过滤 __ 开头的内部函数", "CONFIG_FTRACE"),
    ("CONFIG_WATCHPOINT", "监视点(sdb 的 w/d/info w 命令)", None),
    ("CONFIG_DIFFTEST", "差分测试(与 NEMU 逐指令比对, 需 -d; 无 -d 自动关)", None),
    ("CONFIG_LOG", "日志写文件(配合 -l <file> 参数)", None),
]


def disp_width(s):
    """显示宽度: 中文/全角字符占 2 列, 其余 1 列(终端对齐用)"""
    return sum(2 if ord(ch) > 0x2E80 else 1 for ch in s)


def pad(s, width):
    """按显示宽度左对齐填充到 width 列"""
    return s + " " * max(0, width - disp_width(s))


def load_state():
    """读取 config.h 中每个宏的开关状态(on/off)"""
    state = {}
    try:
        with open(CONFIG_H) as f:
            for line in f:
                m = DEFINE_RE.match(line)
                if m:
                    state[m["name"]] = (m["comment"] is None)  # 未注释 => on
    except FileNotFoundError:
        pass
    return state


def save_state(state):
    """把开关状态写回 config.h

    两条原则:
      1) **只改写状态真的发生变化的那一行**, 其余行原样保留 —— 否则 menuconfig 会把
         config.h 的注释/对齐/空行顺手"规范化"掉(例如 '//  #define X 1' 里那个用来对齐的
         双空格会被吃掉), 每次保存都产生一堆与本次操作无关的 diff;
      2) 菜单里有、但 config.h 里**根本没有**的宏, 追加到最后一个 #endif 之前 ——
         否则开关在界面上能点、文件里却什么都不会生成, 属静默失效(最难查的一类问题)。
    """
    with open(CONFIG_H) as f:
        lines = f.read().split("\n")

    out, seen = [], set()
    for line in lines:
        m = DEFINE_RE.match(line)
        if m:
            name = m["name"]
            seen.add(name)
            # 状态变了才重写; 写法统一为: 保留行首缩进, 关闭时用 "// " 前缀
            if name in state and bool(state[name]) != (m["comment"] is None):
                prefix = "" if state[name] else "// "
                out.append(f"{m['indent']}{prefix}#define {name}{m['rest']}")
                continue
        out.append(line)

    # 补齐 config.h 里缺失的宏(菜单里注册过、但文件里没有这一行)
    missing = [n for (n, _, _) in OPTIONS if n in state and n not in seen]
    if missing:
        block = ["", "// ---- 以下宏由 menuconfig 补齐: config.h 里原本没有这些行 ----"]
        block += [(f"#define {n} 1" if state[n] else f"// #define {n} 1") for n in missing]
        pos = len(out)
        for i in range(len(out) - 1, -1, -1):   # 插到最后一个 #endif 之前(仍在包含卫哨内)
            if out[i].strip() == "#endif":
                pos = i
                break
        out[pos:pos] = block

    with open(CONFIG_H, "w") as f:
        f.write("\n".join(out))


def enabled(state, macro):
    """宏是否开启(含依赖: 父关闭则子视为关)"""
    if macro is None:
        return True
    if not state.get(macro, False):
        return False
    return True


def visible_range(sel, n, avail):
    """算出可视区显示哪几项: 返回 (top, count)。

    窗口矮的时候不能直接截断(会把选中项和后面的选项都藏起来, 让人以为"没这个选项"),
    所以这里滚动: 保证 sel 一定落在 [top, top+count) 里, 且尽量少滚动。
    """
    if avail <= 0 or n <= 0:
        return 0, 0
    avail = min(avail, n)
    top = 0 if sel < avail else sel - avail + 1
    top = max(0, min(top, n - avail))
    return top, avail


def draw_menu(stdscr, state, sel, msg):
    h, w = stdscr.getmaxyx()
    stdscr.erase()
    title = " === NPC 功能配置 (include/config.h) ==="
    stdscr.addstr(0, 0, title[:w - 1], curses.A_BOLD | curses.A_UNDERLINE)
    stdscr.addstr(1, 0, "按键: ↑/↓ 或 j/k 移动  空格 切换   s 保存   r 重置   q 退出(不保存)"[:w - 1])

    # ---- 列表: 窗口矮就滚动, 而不是截断(选中项永远可见) ----
    list_row0 = 3
    n = len(OPTIONS)
    top, count = visible_range(sel, n, max(0, h - 3 - list_row0))
    if top > 0 or top + count < n:      # 有被藏起来的项 -> 在第 2 行给个提示
        hint = f"↕ 共 {n} 项, 正显示第 {top + 1}~{top + count} 项（窗口只有 {h} 行, 拉高可全部显示）"
        stdscr.addstr(2, 0, hint[:w - 1], curses.A_DIM)
    for i in range(top, top + count):
        name, desc, dep = OPTIONS[i]
        on = state.get(name, False)
        dep_on = enabled(state, dep) if dep else True
        if not dep_on:
            on = False  # 父关 => 子不可开
        box = "[*]" if on else "[ ]"
        attr = curses.A_NORMAL
        if i == sel:
            attr |= curses.A_REVERSE
        if not dep_on:
            attr |= curses.A_DIM  # 灰显(依赖未满足)
        line = f"  {box} {pad(desc, 42)} ({name})"
        stdscr.addstr(list_row0 + (i - top), 0, line[:w - 1], attr)

    row = list_row0 + count + 1
    if row < h - 1:
        status = msg or "就绪"
        stdscr.addstr(row, 0, f"  {status}"[:w - 1], curses.A_BOLD)

    stdscr.refresh()


def main(stdscr):
    curses.curs_set(0)  # 隐藏光标
    state = load_state()
    # 依赖清理: 父开关关闭时, 子项强制关闭(保持配置一致)
    for name, _, dep in OPTIONS:
        if dep and not enabled(state, dep):
            state[name] = False
    sel = 0
    msg = ""
    while True:
        draw_menu(stdscr, state, sel, msg)
        key = stdscr.getch()
        msg = ""
        if key in (ord("q"), ord("Q")):
            break
        elif key in (ord("s"), ord("S")):
            save_state(state)
            msg = "已保存到 include/config.h"
        elif key in (ord("r"), ord("R")):
            state = load_state()
            for name, _, dep in OPTIONS:
                if dep and not enabled(state, dep):
                    state[name] = False
            msg = "已重新读取 config.h"
        elif key in (curses.KEY_UP, ord("k")):
            sel = (sel - 1) % len(OPTIONS)
        elif key in (curses.KEY_DOWN, ord("j")):
            sel = (sel + 1) % len(OPTIONS)
        elif key == ord(" "):
            name, _, dep = OPTIONS[sel]
            dep_on = enabled(state, dep) if dep else True
            if not dep_on:
                msg = f"{name} 依赖 {dep}, 请先开启父开关"
            else:
                state[name] = not state.get(name, False)
                # 关闭父开关时顺带关闭子项
                if not state.get(name, False):
                    for sub, _, subdep in OPTIONS:
                        if subdep == name:
                            state[sub] = False


if __name__ == "__main__":
    if not sys.stdin.isatty():
        print("menuconfig 需要终端(tty)环境, 请直接运行: make menuconfig")
        sys.exit(1)
    curses.wrapper(main)
