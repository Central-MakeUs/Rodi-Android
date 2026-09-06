#!/usr/bin/env bash
# 규범 자동 점검 — docs/PROJECT.md 컨벤션과 android-code-standard 스킬 규칙 중
# **기계로 판정 가능한 것만** 본다. 리뷰를 대체하지 않는다.
#
# 두 등급:
#   BLOCK — 현재 위반 0건인 규칙. 새 위반이 들어오면 CI를 실패시킨다.
#   WARN  — 이미 부채가 있는 규칙. 건수만 보고하고 실패시키지 않는다.
#           부채를 갚으면 BLOCK으로 승격한다(docs/BACKLOG.md "코드 관용구 정합성").
#
# 작성 규칙 — 과거에 전부 한 번씩 당한 것들이라 지킬 것:
#   * 글롭은 `-g '*.kt'`처럼 **베이스네임**만 쓴다. `-g '**/src/test/**/*.kt'` 같은 경로 글롭은
#     ripgrep 버전에 따라 조용히 0건을 낸다(로컬 15.2.0 통과 → CI 14.1.0에서 전부 0건).
#     디렉터리 한정은 `grep`으로 **출력 경로를 거른다**.
#   * `build/`는 .gitignore에 있어 rg가 알아서 건너뛴다. 따로 제외하지 않는다.
#   * rg에는 **항상 경로 `.`를 준다.** 경로가 없으면 rg가 stdin을 읽으려 해서, 실행 환경에
#     따라 멈추거나(파이프) 0건을 낸다(리다이렉트). 백그라운드 실행에서 실제로 멈췄다.
#   * `xargs`를 쓰지 않는다. 빈 입력이면 인자 없이 실행돼 저장소 전체를 훑고(CI에서 603건 오탐),
#     막으려고 붙이는 `-r`은 BSD xargs(macOS)에 없다. `while read` + `grep -q`로 대체한다.
#   * PCRE2(`-P`)를 쓰지 않는다. 빌드에 안 들어간 환경에서 에러가 0건으로 삼켜진다.
#   * 0건은 "깨끗함"과 "고장"이 구분되지 않는다. 그래서 아래 sanity check가 있다.

set -uo pipefail
cd "$(dirname "$0")/../.."

command -v rg >/dev/null || { echo "ripgrep(rg)이 필요합니다."; exit 2; }
echo "$(rg --version | head -1)  |  $(rg --files -g '*.kt' . | wc -l | tr -d ' ') kt files"
echo

# 점검 자체가 고장 났는지 먼저 확인한다. 이게 없으면 글롭이 깨져도 "전부 통과"로 보인다.
sanity() {
  local kt vm
  kt=$(rg --files -g '*.kt' . 2>/dev/null | wc -l | tr -d ' ')
  vm=$(rg -l -g '*.kt' 'class HomeViewModel' . 2>/dev/null | wc -l | tr -d ' ')
  if [ "$kt" -lt 100 ] || [ "$vm" -lt 1 ]; then
    echo "환경 이상 — rg가 알려진 파일을 못 찾습니다 (kt=$kt 기대 ≥100, HomeViewModel=$vm 기대 ≥1)."
    echo "글롭이나 rg 버전 문제일 수 있어 점검 결과를 신뢰할 수 없습니다. 중단합니다."
    exit 2
  fi
}
sanity

block_fail=0
warn_total=0

# $1 등급  $2 이름  $3 셸 표현식(줄 단위 출력, 위반이 있으면 출력)
check() {
  local grade="$1" name="$2" expr="$3" out count
  out="$(eval "$expr" 2>/dev/null)"
  count=$([ -z "$out" ] && echo 0 || printf '%s\n' "$out" | wc -l | tr -d ' ')
  if [ "$count" -eq 0 ]; then
    printf '  \033[32m✓\033[0m %s\n' "$name"
  elif [ "$grade" = BLOCK ]; then
    printf '  \033[31m✗ %s — %s건\033[0m\n' "$name" "$count"
    printf '%s\n' "$out" | head -20 | sed 's/^/      /'
    block_fail=1
  else
    printf '  \033[33m! %s — %s건 (기존 부채)\033[0m\n' "$name" "$count"
    warn_total=$((warn_total + count))
  fi
}

echo "== BLOCK — 새 위반은 CI 실패 =="

check BLOCK "Material 아이콘 금지 (Icons.*)" \
  "rg -n -g '*.kt' 'Icons\.' ."

check BLOCK "MaterialTheme.colorScheme 직접 참조 금지" \
  "rg -n -g '*.kt' 'MaterialTheme\.colorScheme' ."

check BLOCK "TextStyle 직접 생성 금지 (테마 밖)" \
  "rg -n -g '*.kt' '\bTextStyle\(' . | grep -v '/theme/'"

check BLOCK "collectAsState() 대신 collectAsStateWithLifecycle()" \
  "rg -n -g '*.kt' 'collectAsState\(\)' ."

check BLOCK "runSuspendCatching은 UseCase에서만" \
  "rg -l -g '*.kt' 'runSuspendCatching' . | grep -v -E 'UseCase|RunSuspendCatching'"

check BLOCK "매퍼는 최상위 확장 함수 (클래스/객체 금지)" \
  "rg -n -g '*.kt' '(class|object) \w+Mapper' . | grep -v -E '/src/(test|androidTest)/'"

check BLOCK "Dispatchers.setMain 후 resetMain 누락" \
  "rg -l -g '*.kt' 'Dispatchers\.setMain' . | grep '/src/test/' \
   | while read -r f; do grep -q 'Dispatchers\.resetMain' \"\$f\" || echo \"\$f\"; done"

check BLOCK "core/feature 모듈에서 Compose BOM 재선언" \
  "rg -n -g 'build.gradle.kts' '(implementation|androidTestImplementation)\(platform\(libs\.androidx\.compose\.bom\)\)' core feature"

echo
echo "== WARN — 기존 부채 (docs/BACKLOG.md '코드 관용구 정합성') =="

check WARN "하드코딩 색 (Color(0xFF...))" \
  "rg -n -g '*.kt' 'Color\(0xFF' . | grep -v '/theme/'"

check WARN "ViewModel 선언명 ≠ 파일명" \
  "rg -n -g '*.kt' -o -r '\$1' --no-heading 'class (\w+ViewModel)\b' . \
   | grep -v -E '/src/(test|androidTest)/' \
   | while IFS=: read -r f _ vm; do [ \"\$(basename \"\$f\" .kt)\" != \"\$vm\" ] && echo \"\$vm ← \$f\"; done"

# 취소 재전파가 catch의 첫 문장이 아닌 곳. PCRE2 없이 다음 줄을 직접 본다.
check WARN "취소 재전파가 catch 첫 문장이 아님" \
  "rg -n -g '*.kt' 'catch \([^)]*CancellationException\)' . \
   | grep -v -E '/src/(test|androidTest)/' \
   | while IFS=: read -r f n _; do \
       case \"\$(sed -n \"\$((n+1))p\" \"\$f\")\" in *throw*) ;; *) echo \"\$f:\$n\";; esac; \
     done"

check WARN "Effect를 SharedFlow로 전달" \
  "rg -l -g '*.kt' 'MutableSharedFlow' . | grep 'ViewModel\.kt$' | grep -v '/src/test/'"

check WARN "component(단수) 패키지" \
  "find app core feature -type d -name component -not -path '*/build/*'"

check WARN "app이 Compose BOM 직접 선언" \
  "rg -n -g 'build.gradle.kts' 'platform\(libs\.androidx\.compose\.bom\)' app"

echo
if [ "$block_fail" -ne 0 ]; then
  echo "BLOCK 규칙 위반이 있습니다. 위 목록을 고치거나, 예외라면 PR에 근거를 적으세요."
  exit 1
fi
echo "BLOCK 통과. WARN 합계 ${warn_total}건 — 기존 부채이므로 실패시키지 않습니다."
