#!/usr/bin/env bash
# Claude(기획자)가 요구를 분석해 docs/handoff/HANDOFF.md에 스펙을 작성한다.
# 사용: scripts/plan.sh "<요구사항>"   또는   INTENT="..." scripts/plan.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; cd "$ROOT"
INTENT="${1:-${INTENT:-}}"
[ -z "$INTENT" ] && { echo "usage: scripts/plan.sh \"<요구사항>\""; exit 1; }

claude -p --permission-mode acceptEdits "너는 Rodi 프로젝트의 기획자다. CLAUDE.md의 기획 워크플로를 따른다.
아래 요구를 분석해 docs/handoff/HANDOFF.md를 템플릿대로 채워라
(Context / Spec / Files to touch / Acceptance criteria / Verification / Out of scope),
Status는 READY_FOR_IMPL로, Branch도 지정. 옛 플랜이 아니라 실제 코드를 읽어 현재 동작 기준으로 작성.
모호함 없이 Codex가 추측 없이 구현할 만큼 구체적으로.

요구: $INTENT"

echo "▶ 기획 완료 — docs/handoff/HANDOFF.md 를 확인하세요."
