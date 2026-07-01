#!/usr/bin/env bash
# 기획→구현→검토를 한 번에 돌리되, 각 단계 끝에서 사람이 [y/N]로 승인한다.
# 사용: scripts/relay.sh "<요구사항>"
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; cd "$ROOT"
INTENT="${1:-${INTENT:-}}"
[ -z "$INTENT" ] && { echo "usage: scripts/relay.sh \"<요구사항>\""; exit 1; }

gate() {
  echo
  read -r -p "▶ [$1] 승인하고 다음 단계로 진행? [y/N] " ans
  [[ "$ans" == [yY] ]] || { echo "중단합니다."; exit 0; }
}

scripts/plan.sh "$INTENT"
gate "기획 — HANDOFF.md 확인"

scripts/impl.sh
gate "구현 — git diff / Codex Result 확인"

scripts/review.sh

echo
VERDICT=$(grep 'Verdict:' docs/handoff/HANDOFF.md 2>/dev/null | grep -o 'APPROVE\|NEEDS_CHANGES' | head -1)
if [[ "$VERDICT" == "APPROVE" ]]; then
  read -r -p "▶ [APPROVE] 커밋·아카이브·PR 자동 처리? [y/N] " ans
  [[ "$ans" == [yY] ]] && scripts/review.sh --auto || \
    echo "  수동 처리: git add -A && git commit -m '...' && git push && gh pr create --base develop"
else
  echo "▶ NEEDS_CHANGES — HANDOFF.md Claude Review 확인 후: RESUME=1 make impl"
fi
