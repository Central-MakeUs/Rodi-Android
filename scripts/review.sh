#!/usr/bin/env bash
# Claude(검토자)가 작업 트리 diff를 HANDOFF 스펙과 대조해 리뷰한다.
# 사용: scripts/review.sh [--auto]
#   --auto : APPROVE 시 커밋·아카이브·PR 자동 생성 (relay.sh 전용)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; cd "$ROOT"
AUTO="${1:-}"

HANDOFF="docs/handoff/HANDOFF.md"

claude -p --permission-mode acceptEdits "너는 Rodi 프로젝트의 검토자다. CLAUDE.md의 검토 워크플로를 따른다.
현재 작업 트리의 git diff를 $HANDOFF 의 Spec/Acceptance criteria와 대조하라.
docs/PROJECT.md 컨벤션 위반을 점검하라: 토큰 하드코딩, Material 아이콘 사용, 불필요한 주석,
스코프 이탈, 시크릿(local.properties 키) 노출.
$HANDOFF 의 Claude Review 섹션에 Blocking / Nits 를 구분해 기록하고
Verdict 를 APPROVE 또는 NEEDS_CHANGES 로 적어라."

# Verdict 파싱
if grep -q 'Verdict:.*APPROVE' "$HANDOFF" 2>/dev/null; then
  VERDICT="APPROVE"
else
  VERDICT="NEEDS_CHANGES"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "▶ Verdict: $VERDICT"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [[ "$VERDICT" == "NEEDS_CHANGES" ]]; then
  echo "  HANDOFF.md Claude Review 를 확인하세요."
  echo "  수정 후: RESUME=1 make impl"
  exit 0
fi

# ── APPROVE 후처리 ────────────────────────────────────────────
# HANDOFF에서 메타데이터 추출
INTENT=$(grep '^# HANDOFF' "$HANDOFF" | sed 's/# HANDOFF[[:space:]]*—[[:space:]]*//' | head -1)
BRANCH=$(grep '^Branch:' "$HANDOFF" | sed 's/Branch:[[:space:]]*//' | head -1)
TIMESTAMP=$(date '+%Y%m%d-%H%M')

if [[ "$AUTO" == "--auto" ]]; then
  echo ""
  echo "▶ APPROVE — 자동 후처리 시작"

  # 1) 구현 파일 스테이징 (HANDOFF 제외)
  git add -A
  git restore --staged "$HANDOFF" 2>/dev/null || true

  # 2) HANDOFF에 Status=DONE 기록 후 스테이징
  sed -i '' 's/^Status:.*/Status: DONE/' "$HANDOFF" 2>/dev/null || \
    sed -i 's/^Status:.*/Status: DONE/' "$HANDOFF"
  git add "$HANDOFF"

  # 3) 커밋 (INTENT를 커밋 메시지로)
  COMMIT_MSG="${INTENT:-chore: Codex 구현 완료}"
  git commit -m "$COMMIT_MSG"
  echo "  ✓ 커밋: $COMMIT_MSG"

  # 4) HANDOFF 아카이브
  ARCHIVE_DIR="docs/handoff/archive"
  mkdir -p "$ARCHIVE_DIR"
  ARCHIVE_NAME="${TIMESTAMP}-$(basename "$HANDOFF")"
  cp "$HANDOFF" "$ARCHIVE_DIR/$ARCHIVE_NAME"
  # HANDOFF.md는 다음 사이클을 위해 초기화
  cat > "$HANDOFF" << 'TMPL'
# HANDOFF

> 다음 사이클을 위해 비워둠. `make plan "요구사항"` 으로 채운다.

Status: EMPTY
TMPL
  git add "$ARCHIVE_DIR/$ARCHIVE_NAME" "$HANDOFF"
  git commit -m "chore(handoff): $TIMESTAMP 아카이브"
  echo "  ✓ 아카이브: $ARCHIVE_DIR/$ARCHIVE_NAME"

  # 5) 브랜치 push + PR 생성 (gh CLI 필요)
  if command -v gh &>/dev/null; then
    CURRENT_BRANCH="${BRANCH:-$(git branch --show-current)}"
    git push -u origin "$CURRENT_BRANCH" 2>/dev/null || git push
    gh pr create \
      --title "${INTENT:-Codex 구현}" \
      --body "$(cat <<EOF
## 구현 내용
${INTENT:-HANDOFF 참조}

## 검토
- Claude 검토 완료 (Verdict: APPROVE)
- HANDOFF 아카이브: $ARCHIVE_DIR/$ARCHIVE_NAME

🤖 Generated with Claude Code + Codex pipeline
EOF
)" \
      --base develop 2>/dev/null || echo "  ℹ PR 생성 스킵 (이미 존재하거나 오류)"
    echo "  ✓ PR 생성 완료"
  else
    echo "  ℹ gh CLI 미설치 — PR은 수동으로 생성하세요."
    echo "  push: git push -u origin \$(git branch --show-current)"
  fi
else
  echo ""
  echo "▶ APPROVE — 수동 후처리 안내"
  echo "  1) git add -A && git commit -m '${INTENT:-구현 완료}'"
  echo "  2) HANDOFF 아카이브: cp $HANDOFF docs/handoff/archive/${TIMESTAMP}-HANDOFF.md"
  echo "  3) git push && gh pr create --base develop"
  echo ""
  echo "  자동 처리: make review-auto  (또는 relay.sh 사용)"
fi
