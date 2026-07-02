#!/usr/bin/env bash
# 사람 개입 없이 기획→구현→검토를 반복한다. NEEDS_CHANGES면 자동으로 재구현을 재시도하고,
# APPROVE면 review.sh --auto가 커밋·아카이브·PR까지 마친다.
# 큐 러너(scripts/queue-relay.sh)에서 호출되는 걸 전제로 하며, 단독 실행도 가능하다.
#
# 사용: scripts/auto-relay.sh "<요구사항>" [--skip-plan] [--max-retries N] [--no-pr]
#   --skip-plan     : docs/handoff/HANDOFF.md 에 이미 READY_FOR_IMPL 스펙이 있을 때
#                     (기획 단계를 건너뛰고 바로 구현부터)
#   --max-retries N : NEEDS_CHANGES 시 RESUME=1 재구현을 몇 번까지 시도할지 (기본 2)
#   --no-pr         : APPROVE 시 커밋·아카이브까지만 하고 push/PR은 생략
#                     (사용자가 직접 확인해야 하는 작업용)
set -uo pipefail  # 개별 단계 실패로 전체 큐가 죽지 않도록 -e는 뺀다 — 실패는 명시적으로 처리
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; cd "$ROOT"

INTENT="${1:-}"
[ -z "$INTENT" ] && { echo "usage: scripts/auto-relay.sh \"<요구사항>\" [--skip-plan] [--max-retries N] [--no-pr]"; exit 1; }
shift

SKIP_PLAN=0
MAX_RETRIES=2
NO_PR=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-plan) SKIP_PLAN=1; shift ;;
    --max-retries) MAX_RETRIES="${2:-2}"; shift 2 ;;
    --no-pr) NO_PR=1; shift ;;
    *) shift ;;
  esac
done

HANDOFF="docs/handoff/HANDOFF.md"
EMPTY_MARKER="# HANDOFF — <작업 제목>"
PLACEHOLDER_BRANCH="Branch: <feature/xxx>"

log() { echo "[$(date '+%H:%M:%S')] $*"; }

log "▶ 시작: $INTENT (skip_plan=$SKIP_PLAN, max_retries=$MAX_RETRIES)"

if [[ "$SKIP_PLAN" -eq 0 ]]; then
  log "  기획 중..."
  if ! scripts/plan.sh "$INTENT"; then
    log "  ✗ 기획 실패 — 이 작업 건너뜀"
    exit 1
  fi
  # plan.sh(헤드리스 claude -p)가 실제로 스펙을 못 쓰고 질문만 남긴 채 템플릿을 그대로
  # 둔 경우(예: 참고 경로가 샌드박스 밖이라 읽지 못함) — 이 상태로 impl/review를 진행하면
  # "빈 템플릿"을 성공 판정과 혼동하는 사고가 난다. 여기서 미리 걸러낸다.
  if grep -qF "$EMPTY_MARKER" "$HANDOFF" 2>/dev/null || grep -qF "$PLACEHOLDER_BRANCH" "$HANDOFF" 2>/dev/null; then
    log "  ✗ 기획 실패 — HANDOFF.md가 여전히 빈 템플릿(스펙 작성 안 됨). 이 작업 건너뜀."
    log "     (헤드리스 세션이 참고 경로 접근 등으로 막혔을 수 있음 — 로그 확인 필요)"
    exit 1
  fi
fi

attempt=0
while (( attempt <= MAX_RETRIES )); do
  HEAD_BEFORE="$(git rev-parse HEAD)"

  if (( attempt == 0 )); then
    log "  구현 중... (시도 1)"
    scripts/impl.sh
  else
    log "  재구현 중... (시도 $((attempt + 1))/$((MAX_RETRIES + 1)))"
    RESUME=1 scripts/impl.sh
  fi

  log "  검토 중..."
  if [[ "$NO_PR" -eq 1 ]]; then
    scripts/review.sh --auto --no-pr
  else
    scripts/review.sh --auto
  fi

  # review.sh --auto 가 APPROVE 처리에 성공하면 HANDOFF.md 를 빈 템플릿으로 되돌리고 커밋을
  # 남긴다. 템플릿 문자열만 보면 "애초에 한 번도 안 건드려진 상태"와 구분이 안 되므로
  # (실제로 이 때문에 헛APPROVE 사고가 있었음), HEAD가 실제로 전진했는지도 함께 확인한다.
  HEAD_AFTER="$(git rev-parse HEAD)"
  if grep -qF "$EMPTY_MARKER" "$HANDOFF" 2>/dev/null && [[ "$HEAD_AFTER" != "$HEAD_BEFORE" ]]; then
    if [[ "$NO_PR" -eq 1 ]]; then
      log "  ✓ 완료: $INTENT (로컬 커밋만, PR 생략)"
    else
      log "  ✓ 완료: $INTENT (PR 생성됨)"
    fi
    exit 0
  fi

  attempt=$((attempt + 1))
  log "  ⚠ NEEDS_CHANGES 또는 BLOCKED (재시도 $attempt/$MAX_RETRIES)"
done

log "  ✗ 재시도 소진 — BLOCKED 상태로 남김. HANDOFF.md 확인 필요: $INTENT"
exit 1
