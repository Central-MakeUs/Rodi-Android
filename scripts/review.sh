#!/usr/bin/env bash
# Claude(검토자)가 작업 트리 diff를 HANDOFF 스펙과 대조해 리뷰한다.
# 사용: scripts/review.sh [--auto] [--no-pr]
#   --auto   : APPROVE 시 커밋·아카이브·PR 자동 생성 (relay.sh 전용)
#   --no-pr  : --auto와 함께 쓰면 커밋·아카이브까지만 하고 push/PR 생성은 생략
#              (사용자가 직접 확인해야 하는 작업용 — 로컬 커밋으로만 남긴다)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; cd "$ROOT"
AUTO="${1:-}"
NO_PR="${2:-}"

HANDOFF="docs/handoff/HANDOFF.md"

claude -p --permission-mode acceptEdits "너는 Rodi 프로젝트의 검토자다. CLAUDE.md의 검토 워크플로를 따른다.
현재 작업 트리의 git diff를 $HANDOFF 의 Spec/Acceptance criteria와 대조하라.
docs/PROJECT.md 컨벤션 위반을 점검하라: 토큰 하드코딩, Material 아이콘 사용, 불필요한 주석,
스코프 이탈, 시크릿(local.properties 키) 노출.
$HANDOFF 의 Claude Review 섹션에 Blocking / Nits 를 구분해 기록하고
Verdict 를 APPROVE 또는 NEEDS_CHANGES 로 적어라."

# Verdict 파싱 (마지막으로 기록된 값만 인정 — 재검토 시 과거 값 잔존 방지)
VERDICT=$(grep 'Verdict:' "$HANDOFF" 2>/dev/null | grep -o 'APPROVE\|NEEDS_CHANGES' | tail -1)
VERDICT="${VERDICT:-NEEDS_CHANGES}"

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
# HANDOFF에서 메타데이터 추출 (없어도 스크립트가 죽지 않도록 || true)
INTENT=$(grep '^# HANDOFF' "$HANDOFF" | sed 's/# HANDOFF[[:space:]]*—[[:space:]]*//' | head -1) || true
BRANCH=$(grep '^Branch:' "$HANDOFF" | sed 's/Branch:[[:space:]]*//' | head -1) || true
TIMESTAMP=$(date '+%Y%m%d-%H%M')

if [[ "$AUTO" == "--auto" ]]; then
  echo ""
  echo "▶ APPROVE — 자동 후처리 시작"

  echo "  ▶ 빌드 검증 (assembleDebug)"
  if ! ./gradlew assembleDebug -q; then
    echo "  ✗ 빌드 실패 — 자동 커밋을 중단합니다. 원인을 확인하세요."
    exit 1
  fi
  echo "  ✓ 빌드 성공"

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
  # HANDOFF.md는 다음 사이클을 위해 원본 브릿지 템플릿 골격으로 초기화
  cat > "$HANDOFF" << 'TMPL'
# HANDOFF — <작업 제목>

> Claude(기획)와 Codex(구현)가 주고받는 **단일 활성 작업 채널**.
> 완료되면 `docs/handoff/archive/<날짜>-<작업>.md`로 옮기고 이 파일은 다음 작업으로 비운다.

Status: PLANNING            <!-- PLANNING | READY_FOR_IMPL | IMPL_DONE | IN_REVIEW | DONE | BLOCKED -->
Branch: <feature/xxx>

## Context (왜)
<이 작업이 필요한 배경 한두 줄>

## Spec (무엇을·어떻게)
<구현할 내용. 구체적으로. 모호하면 Codex는 추측하지 말 것>

## Files to touch
<예상 수정 파일 경로>

## Acceptance criteria
- [ ] <검수 기준 1>
- [ ] <검수 기준 2>

## Verification
```
./gradlew assembleDebug
```

## Out of scope
<이번에 건드리지 않을 것>

---
## Codex Result   <!-- Codex가 구현 후 채움 → Status=IMPL_DONE (또는 막히면 BLOCKED) -->
- Changed files:
- Build/test:
- Open questions:

---
## Claude Review  <!-- Claude가 검토 후 채움 -->
- Blocking:
- Nits:
- Verdict:   <!-- APPROVE | NEEDS_CHANGES -->
TMPL
  git add "$ARCHIVE_DIR/$ARCHIVE_NAME" "$HANDOFF"
  git commit -m "chore(handoff): $TIMESTAMP 아카이브"
  echo "  ✓ 아카이브: $ARCHIVE_DIR/$ARCHIVE_NAME"

  # 5) 브랜치 push + PR 생성 (gh CLI 필요) — --no-pr 지정 시 생략
  if [[ "$NO_PR" == "--no-pr" ]]; then
    echo "  ℹ --no-pr 지정 — push/PR 생성 생략. 로컬 브랜치 $(git branch --show-current)에 커밋만 남았습니다."
  elif command -v gh &>/dev/null; then
    # 실제 체크아웃된 브랜치를 쓴다 — HANDOFF.md의 Branch: 필드는 이전 사이클의 잔여값일 수
    # 있어 신뢰하지 않는다(실제로 엉뚱한 원격 브랜치에 push된 사고가 있었음).
    CURRENT_BRANCH="$(git branch --show-current)"
    if [[ -n "$BRANCH" && "$BRANCH" != "$CURRENT_BRANCH" && "$BRANCH" != *"<"* ]]; then
      echo "  ⚠ HANDOFF.md의 Branch(\"$BRANCH\")가 실제 브랜치(\"$CURRENT_BRANCH\")와 다름 — 실제 브랜치로 push합니다."
    fi
    git push -u origin "$CURRENT_BRANCH"
    if gh pr create \
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
      --base develop 2>/dev/null; then
      echo "  ✓ PR 생성 완료"
    else
      echo "  ℹ PR 생성 스킵 (이미 존재하거나 오류)"
    fi
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
