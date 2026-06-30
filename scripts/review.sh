#!/usr/bin/env bash
# Claude(검토자)가 작업 트리 diff를 HANDOFF 스펙과 대조해 리뷰한다.
# 사용: scripts/review.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; cd "$ROOT"

claude -p --permission-mode acceptEdits "너는 Rodi 프로젝트의 검토자다. CLAUDE.md의 검토 워크플로를 따른다.
현재 작업 트리의 git diff를 docs/handoff/HANDOFF.md의 Spec/Acceptance criteria와 대조하라.
docs/PROJECT.md 컨벤션 위반을 점검하라: 토큰 하드코딩, Material 아이콘 사용, 불필요한 주석,
스코프 이탈, 시크릿(local.properties 키) 노출.
HANDOFF.md의 Claude Review 섹션에 Blocking / Nits 를 구분해 기록하고
Verdict 를 APPROVE 또는 NEEDS_CHANGES 로 적어라."

echo "▶ 검토 완료 — HANDOFF.md 의 Claude Review 를 확인하세요."
echo "  NEEDS_CHANGES면: RESUME=1 scripts/impl.sh 로 재구현."
