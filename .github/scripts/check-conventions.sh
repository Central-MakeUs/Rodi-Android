#!/usr/bin/env bash
# 규범 자동 점검 — docs/PROJECT.md 컨벤션과 android-code-standard 스킬 규칙 중
# **기계로 검증 가능한 것만** 본다. 리뷰를 대체하지 않는다.
#
# 두 등급으로 나눈다:
#   BLOCK — 현재 위반 0건인 규칙. 새 위반이 들어오면 CI를 실패시킨다.
#   WARN  — 이미 부채가 있는 규칙. 건수만 보고하고 실패시키지 않는다.
#           부채를 다 갚으면 BLOCK으로 승격한다(docs/BACKLOG.md "코드 관용구 정합성").
#
# 규칙의 *왜*는 ~/.claude/skills/android-code-standard/ 에 있다.

set -uo pipefail
cd "$(dirname "$0")/../.."

command -v rg >/dev/null || { echo "ripgrep(rg)이 필요합니다."; exit 2; }

EX=(--glob '!**/build/**')
MAIN=("${EX[@]}" --glob '!**/src/test/**' --glob '!**/src/androidTest/**')

block_fail=0
warn_total=0

# $1 등급  $2 이름  $3.. 명령
check() {
  local grade="$1" name="$2"; shift 2
  local out count
  out="$("$@" 2>/dev/null)"
  count=$([ -z "$out" ] && echo 0 || printf '%s\n' "$out" | wc -l | tr -d ' ')
  if [ "$count" -eq 0 ]; then
    printf '  \033[32m✓\033[0m %s\n' "$name"
    return
  fi
  if [ "$grade" = BLOCK ]; then
    printf '  \033[31m✗ %s — %s건\033[0m\n' "$name" "$count"
    printf '%s\n' "$out" | sed 's/^/      /'
    block_fail=1
  else
    printf '  \033[33m! %s — %s건 (기존 부채)\033[0m\n' "$name" "$count"
    warn_total=$((warn_total + count))
  fi
}

echo "== BLOCK — 새 위반은 CI 실패 =="

check BLOCK "Material 아이콘 금지 (Icons.*)" \
  rg -n 'Icons\.' --glob '**/*.kt' "${EX[@]}"

check BLOCK "MaterialTheme.colorScheme 직접 참조 금지" \
  rg -n 'MaterialTheme\.colorScheme' --glob '**/*.kt' "${EX[@]}"

check BLOCK "TextStyle 직접 생성 금지 (테마 밖)" \
  rg -n '\bTextStyle\(' --glob '**/*.kt' "${EX[@]}" --glob '!**/theme/**'

check BLOCK "collectAsState() 대신 collectAsStateWithLifecycle()" \
  rg -n 'collectAsState\(\)' --glob '**/*.kt' "${EX[@]}"

check BLOCK "runSuspendCatching은 UseCase에서만" \
  bash -c "rg -ln 'runSuspendCatching' --glob '**/*.kt' --glob '!**/build/**' | rg -v 'UseCase|RunSuspendCatching'"

check BLOCK "매퍼는 최상위 확장 함수 (클래스/객체 금지)" \
  rg -n '(class|object) \w+Mapper' --glob '**/*.kt' "${MAIN[@]}"

check BLOCK "Dispatchers.setMain 후 resetMain 누락" \
  bash -c "rg -l 'Dispatchers\.setMain' --glob '**/src/test/**/*.kt' --glob '!**/build/**' | xargs rg --files-without-match 'Dispatchers\.resetMain' 2>/dev/null"

check BLOCK "core/feature 모듈에서 Compose BOM 재선언" \
  rg -n '(implementation|androidTestImplementation)\(platform\(libs\.androidx\.compose\.bom\)\)' \
     --glob 'core/**/build.gradle.kts' --glob 'feature/**/build.gradle.kts' "${EX[@]}"

echo
echo "== WARN — 기존 부채 (docs/BACKLOG.md '코드 관용구 정합성') =="

check WARN "하드코딩 색 (Color(0xFF...))" \
  rg -n 'Color\(0xFF' --glob '**/*.kt' "${EX[@]}" --glob '!**/theme/**'

check WARN "ViewModel 선언명 ≠ 파일명" \
  bash -c "rg -n 'class (\w+ViewModel)\b' -r '\$1' -o --no-heading --glob '**/*.kt' --glob '!**/build/**' --glob '!**/src/test/**' --glob '!**/src/androidTest/**' | while IFS=: read -r f _ vm; do [ \"\$(basename \"\$f\" .kt)\" != \"\$vm\" ] && echo \"\$vm ← \$f\"; done"

check WARN "취소 재전파가 catch 첫 문장이 아님" \
  rg -U -P -n 'catch \([^)]*CancellationException\)\s*\{\s*\n(?!\s*throw)' \
     --glob '**/*.kt' "${MAIN[@]}"

check WARN "Effect를 SharedFlow로 전달" \
  rg -n 'MutableSharedFlow' --glob '**/*ViewModel.kt' "${MAIN[@]}"

check WARN "component(단수) 패키지" \
  bash -c "find app core feature -type d -name component -not -path '*/build/*'"

check WARN "app이 Compose BOM 직접 선언" \
  rg -n 'platform\(libs\.androidx\.compose\.bom\)' --glob 'app/build.gradle.kts' "${EX[@]}"

echo
if [ "$block_fail" -ne 0 ]; then
  echo "BLOCK 규칙 위반이 있습니다. 위 목록을 고치거나, 예외라면 PR에 근거를 적으세요."
  exit 1
fi
echo "BLOCK 통과. WARN 합계 ${warn_total}건 — 기존 부채이므로 실패시키지 않습니다."
