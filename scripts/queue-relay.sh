#!/usr/bin/env bash
# 큐 파일(docs/QUEUE.md)의 항목을 순서대로 하나씩 처리한다.
# 항목마다: base_branch 기준으로 브랜치 준비(없으면 새로 생성) → scripts/auto-relay.sh 실행 → 다음 항목.
# 사람 개입 없이 밤새 돌리는 걸 전제로 한다 — 실패한 항목은 건너뛰고 계속 진행한다.
#
# 사용: nohup scripts/queue-relay.sh > docs/handoff/queue-run.log 2>&1 &
#   MAX_ITEMS=3 scripts/queue-relay.sh   # 처리할 최대 항목 수 (기본 3)
set -uo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; cd "$ROOT"

QUEUE_FILE="docs/QUEUE.md"
MAX_ITEMS="${MAX_ITEMS:-3}"
LOG_DIR="docs/handoff/queue-logs"
mkdir -p "$LOG_DIR"

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"; }

[ -f "$QUEUE_FILE" ] || { log "✗ $QUEUE_FILE 이 없습니다."; exit 1; }

log "════════════════════════════════════════"
log "큐 실행 시작 (최대 $MAX_ITEMS 건)"
log "════════════════════════════════════════"

git fetch origin --quiet

processed=0
succeeded=()
failed=()

# QUEUE.md 의 데이터 라인만 추출 (# 주석, 빈 줄, > 안내문 제외).
# macOS 기본 /bin/bash(3.2)는 mapfile/readarray가 없어 임시 파일로 우회한다.
# 루프 내부 명령에는 </dev/null 을 붙여 이 큐 파일 읽기용 stdin과 충돌하지 않게 한다.
QUEUE_TMP="$(mktemp)"
trap 'rm -f "$QUEUE_TMP"' EXIT
grep -E '^[^#>[:space:]].*\|.*\|' "$QUEUE_FILE" > "$QUEUE_TMP"

while IFS='|' read -r branch intent skip_plan base_branch; do
  branch="$(echo "$branch" | xargs)"
  intent="$(echo "$intent" | xargs)"
  skip_plan="$(echo "$skip_plan" | xargs)"
  base_branch="$(echo "${base_branch:-develop}" | xargs)"
  [[ -z "$base_branch" ]] && base_branch="develop"

  if (( processed >= MAX_ITEMS )); then
    log "▶ 최대 처리 건수($MAX_ITEMS) 도달 — 나머지는 다음 실행으로 넘김"
    break
  fi

  log ""
  log "──────────────────────────────────────"
  log "[$((processed + 1))/$MAX_ITEMS] $branch (base: $base_branch) — $intent"
  log "──────────────────────────────────────"

  ITEM_LOG="$LOG_DIR/$(date '+%Y%m%d-%H%M%S')-$(basename "$branch").log"

  CHECKOUT_RESULT=0
  if git show-ref --verify --quiet "refs/heads/$branch"; then
    log "  기존 브랜치 사용: $branch"
    git checkout "$branch" </dev/null >>"$ITEM_LOG" 2>&1 || CHECKOUT_RESULT=$?
  elif [[ "$base_branch" == "develop" ]]; then
    log "  새 브랜치 생성: $branch (from origin/develop)"
    git fetch origin --quiet </dev/null
    git checkout -b "$branch" origin/develop </dev/null >>"$ITEM_LOG" 2>&1 || CHECKOUT_RESULT=$?
  else
    if ! git show-ref --verify --quiet "refs/heads/$base_branch"; then
      log "  ✗ base_branch '$base_branch' 가 로컬에 없음 — 이 항목 건너뜀"
      failed+=("$branch(base 없음)")
      processed=$((processed + 1))
      continue
    fi
    log "  새 브랜치 생성: $branch (from local $base_branch — 아직 develop에 안 머지된 의존 브랜치)"
    git checkout -b "$branch" "$base_branch" </dev/null >>"$ITEM_LOG" 2>&1 || CHECKOUT_RESULT=$?
  fi

  # 체크아웃/브랜치 생성이 실패하면(dirty 워킹트리, 이름 충돌 등) 엉뚱한 브랜치 위에서
  # auto-relay.sh를 돌리는 사고를 막기 위해 실제 체크아웃된 브랜치를 재확인하고 다르면 건너뛴다.
  ACTUAL_BRANCH="$(git branch --show-current)"
  if [[ "$CHECKOUT_RESULT" -ne 0 || "$ACTUAL_BRANCH" != "$branch" ]]; then
    log "  ✗ 브랜치 전환 실패 (기대: $branch, 실제: $ACTUAL_BRANCH) — 이 항목 건너뜀. 로그: $ITEM_LOG"
    failed+=("$branch(체크아웃 실패)")
    processed=$((processed + 1))
    continue
  fi

  # macOS 기본 bash(3.2)는 set -u 상태에서 빈 배열 "${arr[@]}" 확장 시 "unbound variable"로
  # 죽는다 — 배열 대신 skip_plan 값으로 분기해 호출한다.
  if [[ "$skip_plan" == "yes" ]]; then
    RELAY_RESULT=0
    scripts/auto-relay.sh "$intent" --skip-plan --max-retries 2 </dev/null >>"$ITEM_LOG" 2>&1 || RELAY_RESULT=$?
  else
    RELAY_RESULT=0
    scripts/auto-relay.sh "$intent" --max-retries 2 </dev/null >>"$ITEM_LOG" 2>&1 || RELAY_RESULT=$?
  fi

  if [[ "$RELAY_RESULT" -eq 0 ]]; then
    log "  ✓ 성공 — 로그: $ITEM_LOG"
    succeeded+=("$branch")
  else
    log "  ✗ 실패/BLOCKED — 로그: $ITEM_LOG"
    failed+=("$branch")
  fi

  processed=$((processed + 1))
done < "$QUEUE_TMP"

log ""
log "════════════════════════════════════════"
log "큐 실행 종료 — 처리 $processed 건"
log "  성공: ${succeeded[*]:-없음}"
log "  실패/BLOCKED: ${failed[*]:-없음}"
log "════════════════════════════════════════"
