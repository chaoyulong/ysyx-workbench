#!/usr/bin/env bash
# ============================================================================
# 生成 ysyxSoC 本地补丁（留档用）
#
# 【为什么需要】
#   ysyxSoC 的远端是【官方仓库】OSCPU/ysyxSoC，本地改动推不上去。
#   一旦重新 clone，这些改动就会丢失 —— 尤其是 sdram.v 里那个
#   "命令优先级" 修复，丢了之后 "SDRAM 突发第二拍全 0" 的 bug 会神秘复现。
#   所以把本地改动固化成 patch 放在 npc 仓库里（npc 的远端是自己仓库，推得上去）。
#
# 【用法】
#   tools/make-ysyxsoc-patch.sh [ysyxSoC路径]      # 默认 ../ysyxSoC
#   BASE=v1.0 tools/make-ysyxsoc-patch.sh          # 换基线（默认 origin/ysyx6）
#
# 【应用方法】（在纯净的 ysyxSoC 克隆里）
#   cd ysyxSoC
#   git checkout <patch 头里记录的基线 commit>
#   git apply ../npc/ysyxSoC-local.patch
#
# 【为什么不带 firtool 二进制】
#   patch/firtool/ 下有 firtool(28MB) + om-linker(8.5MB)，共 36MB。
#   git diff 对二进制只会输出 "Binary files differ"，打不出内容，
#   带上反而让 patch 无法应用，所以有意排除，需要时另行获取。
# ============================================================================
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NPC_HOME="$(cd "$HERE/.." && pwd)"
YSYXSOC="${1:-$(cd "$NPC_HOME/.." && pwd)/ysyxSoC}"
OUT="$NPC_HOME/ysyxSoC-local.patch"
BASE="${BASE:-origin/ysyx6}"

[ -d "$YSYXSOC/.git" ] || { echo "错误: 找不到 ysyxSoC 仓库: $YSYXSOC" >&2; exit 1; }

cd "$YSYXSOC"
BASE_HASH="$(git rev-parse "$BASE")"
HEAD_HASH="$(git rev-parse --short HEAD)"

{
  echo "# ============================================================================"
  echo "# ysyxSoC 本地补丁"
  echo "#"
  echo "#   基线 commit    : $BASE_HASH  ($BASE)"
  echo "#   生成时的 HEAD  : $HEAD_HASH"
  echo "#   生成时间       : $(date '+%Y-%m-%d %H:%M:%S')"
  echo "#   生成脚本       : npc/tools/make-ysyxsoc-patch.sh"
  echo "#"
  echo "#   应用方法(在纯净的 ysyxSoC 克隆里):"
  echo "#     cd ysyxSoC"
  echo "#     git checkout $BASE_HASH"
  echo "#     git apply ../npc/ysyxSoC-local.patch"
  echo "#"
  echo "#   有意排除:"
  echo "#     - rocket-chip/   子模块, 只是指针脏标记(-dirty), 无实际改动"
  echo "#     - patch/firtool/ firtool(28MB)+om-linker(8.5MB), 二进制无法用 diff 表达"
  echo "#"
  echo "#   主要内容:"
  echo "#     perip/sdram/sdram.v      SDRAM 芯片模型: 突发优先级修复(命令 > 突发结束),"
  echo "#                              命令/状态具名化(CMD_*/state_t), cs 守卫统一,"
  echo "#                              data_count/delay_count 语义闭合"
  echo "#     perip/amba/apb_delayer.v APB 访存延迟校准"
  echo "#     perip/amba/axi4_delayer.v AXI 访存延迟校准(读/写通道)"
  echo "#     perip/sdram/core_sdram_axi4/, sdram_top_*.v  SDRAM 控制器改动"
  echo "#     src/SoC.scala 等         4 片 SDRAM 芯片模型实例化(chip id, DIP-C 存储)"
  echo "# ============================================================================"
  echo ""
  git diff "$BASE" -- . ':(exclude)rocket-chip' ':(exclude)patch'
} > "$OUT"

echo "已生成 $OUT"
echo "  基线      : $BASE_HASH"
echo "  文件/行数 : $(grep -c '^diff --git' "$OUT") 个文件, $(wc -l < "$OUT") 行, $(du -h "$OUT" | cut -f1)"
