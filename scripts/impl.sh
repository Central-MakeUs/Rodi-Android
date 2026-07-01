#!/usr/bin/env bash
# Codex(구현자)가 HANDOFF.md 스펙을 구현하고 빌드한다.
# 사용: scripts/impl.sh           (새 구현)
#       RESUME=1 scripts/impl.sh  (직전 Codex 세션 맥락 이어서 — 리뷰 반영 재구현 등)
# 자율 실행: --sandbox workspace-write (워크스페이스 쓰기/명령 실행 허용).
#   더 강한 자율이 필요하면 --dangerously-bypass-approvals-and-sandbox 로 교체(주의).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; cd "$ROOT"

PROMPT="AGENTS.md의 절차를 따른다. docs/PROJECT.md와 docs/handoff/HANDOFF.md를 읽고 Spec을 정확히 구현하라.
스코프를 넓히지 말고, 모호하면 추측 말고 Open questions에 적고 BLOCKED로 멈춰라.
./gradlew assembleDebug 가 green이 될 때까지 수정하라.
끝나면 HANDOFF.md의 Codex Result 섹션(Changed files / Build·test / Open questions)을 채우고
Status를 IMPL_DONE(막혔으면 BLOCKED)으로 바꿔라. 커밋은 하지 마라."

if [ "${RESUME:-}" = "1" ]; then
  codex exec resume --last --sandbox workspace-write "$PROMPT"
else
  codex exec --sandbox workspace-write "$PROMPT"
fi

echo "▶ 구현 완료 — git diff 와 HANDOFF.md 의 Codex Result 를 확인하세요."
