#!/usr/bin/env bash
# 규범 자동 점검 — docs/PROJECT.md 컨벤션과 android-code-standard 스킬 규칙 중
# **기계로 판정 가능한 것만** 본다. 리뷰를 대체하지 않는다.
#
# 두 등급:
#   BLOCK — 명확한 invariant. false positive가 낮고 예외가 거의 없으며 현재 위반 0건.
#           새 위반이 들어오면 CI를 실패시킨다.
#   WARN  — 옳은 방향이지만 기존 부채가 있는 규칙. 건수만 보고하고 실패시키지 않는다.
#           부채를 다 갚으면 BLOCK으로 승격한다(docs/BACKLOG.md "코드 관용구 정합성").
#   INFO  — heuristic·구조적 관찰. 강제할 근거는 없지만 리뷰 때 볼 가치가 있다.
#           실패시키지 않고, 승격을 전제하지도 않는다.
#
# 규범의 *이유*와 *예외 정책*은 여기 있지 않다 — docs/conventions/ 와 docs/adr/ 에 있다.
# 이 스크립트는 판정자일 뿐이다.
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
cd "$(dirname "$0")/../.." || { echo "리포 루트로 이동하지 못했습니다."; exit 2; }

command -v rg >/dev/null || { echo "ripgrep(rg)이 필요합니다."; exit 2; }

# 검증된 버전: 14.1.0(CI) / 15.2.0(로컬). 13 미만은 글롭 동작이 달라 결과를 신뢰할 수 없다.
RG_MAJOR=$(rg --version | head -1 | sed -E 's/[^0-9]*([0-9]+).*/\1/')
if [ "${RG_MAJOR:-0}" -lt 13 ]; then
  echo "ripgrep ${RG_MAJOR}.x는 지원하지 않습니다 (최소 13, 검증된 버전 14/15)."
  echo "글롭 동작이 달라 검사 결과를 신뢰할 수 없습니다. 중단합니다."
  exit 2
fi
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

# regex가 민감한 BLOCK 규칙이 "알려진 위반"을 실제로 잡는지 확인한다.
# 0건이 성공을 뜻하는 검사에서, 검증기가 고장 나면 전부 통과로 보인다.
# fixture는 임시 디렉터리에 만든다 — 리포에 두면 본 검사가 그걸 위반으로 잡는다.
positive_control() {
  local d rc=0
  d=$(mktemp -d) || { echo "임시 디렉터리를 만들지 못했습니다."; exit 2; }
  cat > "$d/Violation.kt" <<'FIXTURE'
val a = Icons.Default.Add
val b = MaterialTheme.colorScheme.primary
val c = vm.state.collectAsState()
val d = TextStyle(fontSize = 1.sp)
val e = Color(0xFF123456)
class FixtureMapper
val errorMessage = error.message

try {
  Unit
} catch (error: CancellationException) {
  val ignored = true
}
FIXTURE
  cat > "$d/Lookalike.kt" <<'FIXTURE'
val ok1 = NotificationCompat.BigTextStyle().bigText(msg)
val ok2 = vm.state.collectAsStateWithLifecycle()
val ok3 = error.userMessage("fallback")
val ok4 = intent.message

try {
  Unit
} catch (error: CancellationException) {
  throw error
}
FIXTURE

  # 위반 fixture에서 반드시 잡혀야 하는 패턴
  for pat in 'Icons\.' 'MaterialTheme\.colorScheme' 'collectAsState\(\)' '\bTextStyle\(' \
             'Color\(0xFF' '(class|object) \w+Mapper'; do
    rg -q "$pat" "$d/Violation.kt" 2>/dev/null || { echo "  탐지 실패: $pat"; rc=1; }
  done
  # 유사어 fixture에서 잡히면 안 되는 패턴
  for pat in '\bTextStyle\(' 'collectAsState\(\)'; do
    rg -q "$pat" "$d/Lookalike.kt" 2>/dev/null && { echo "  오탐: $pat"; rc=1; }
  done

  rg -n '\.message\b' "$d/Violation.kt" 2>/dev/null \
    | grep -v 'intent\.message' >/dev/null 2>&1 \
    || { echo '  탐지 실패: ViewModel 예외 message 원문 노출'; rc=1; }
  if rg -n '\.message\b' "$d/Lookalike.kt" 2>/dev/null \
    | grep -v 'intent\.message' >/dev/null 2>&1; then
    echo '  오탐: ViewModel 예외 message 원문 노출'
    rc=1
  fi

  local violation_catch_line lookalike_catch_line
  violation_catch_line="$(rg -n 'catch \([^)]*CancellationException\)' "$d/Violation.kt" 2>/dev/null | head -1 | cut -d: -f1)"
  if [ -z "$violation_catch_line" ]; then
    echo '  탐지 실패: 취소 재전파가 catch 첫 문장이 아님'
    rc=1
  else
    case "$(sed -n "$((violation_catch_line + 1))p" "$d/Violation.kt")" in
      *throw*) echo '  탐지 실패: 취소 재전파가 catch 첫 문장이 아님'; rc=1;;
    esac
  fi
  lookalike_catch_line="$(rg -n 'catch \([^)]*CancellationException\)' "$d/Lookalike.kt" 2>/dev/null | head -1 | cut -d: -f1)"
  if [ -z "$lookalike_catch_line" ]; then
    echo '  탐지 실패: 취소 재전파 lookalike'
    rc=1
  else
    case "$(sed -n "$((lookalike_catch_line + 1))p" "$d/Lookalike.kt")" in
      *throw*) ;;
      *) echo '  오탐: 취소 재전파 lookalike'; rc=1;;
    esac
  fi

  rm -rf "$d"
  if [ "$rc" -ne 0 ]; then
    echo "검증기 자체가 고장났습니다 — 알려진 위반을 못 잡거나 정상 코드를 잡습니다."
    echo "이 상태의 '통과'는 의미가 없으므로 중단합니다."
    exit 2
  fi
}
positive_control

block_fail=0
warn_total=0

# $1 등급  $2 이름  $3 셸 표현식(줄 단위 출력, 위반이 있으면 출력)
# rg는 매치 없음이 1, **실제 오류가 2**다. 출력만 보면 오류가 "0건 통과"로 둔갑한다.
# PIPESTATUS를 되받아 파이프 안 어느 단계든 2 이상이면 검증기 고장으로 중단한다.
check() {
  local grade="$1" name="$2" expr="$3" raw out count rcs rc
  raw="$(eval "$expr"'; printf "\n__RC:%s" "${PIPESTATUS[*]}"' 2>/dev/null)"
  rcs="${raw##*__RC:}"
  out="${raw%$'\n'__RC:*}"
  out="$(printf '%s' "$out")"   # 마커 앞 개행이 남아 카운트가 1씩 늘어난다
  for rc in $rcs; do
    if [ "${rc:-0}" -ge 2 ]; then
      printf '  \033[31m! %s — 검사 명령이 오류로 종료했습니다 (exit %s)\033[0m\n' "$name" "$rc"
      echo "    0건과 구분되지 않으므로 통과로 처리하지 않습니다."
      exit 2
    fi
  done
  count=$([ -z "$out" ] && echo 0 || printf '%s\n' "$out" | wc -l | tr -d ' ')
  if [ "$count" -eq 0 ]; then
    printf '  \033[32m✓\033[0m %s\n' "$name"
  elif [ "$grade" = BLOCK ]; then
    printf '  \033[31m✗ %s — %s건\033[0m\n' "$name" "$count"
    printf '%s\n' "$out" | head -20 | sed 's/^/      /'
    block_fail=1
  elif [ "$grade" = WARN ]; then
    printf '  \033[33m! %s — %s건 (기존 부채)\033[0m\n' "$name" "$count"
    warn_total=$((warn_total + count))
  else
    printf '  \033[36mi %s — %s건\033[0m\n' "$name" "$count"
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
  "rg -n -g '*.kt' 'collectAsState\s*\(' ."

check BLOCK "runSuspendCatching은 UseCase에서만" \
  "rg -l -g '*.kt' 'runSuspendCatching' . | grep -v -E 'UseCase|RunSuspendCatching'"

check BLOCK "매퍼는 최상위 확장 함수 (클래스/객체 금지)" \
  "rg -n -g '*.kt' '(class|object) \w+Mapper' . | grep -v -E '/src/(test|androidTest)/'"

check BLOCK "Dispatchers.setMain 후 resetMain 누락" \
  "rg -l -g '*.kt' 'Dispatchers\.setMain' . | grep '/src/test/' \
   | while read -r f; do grep -q 'Dispatchers\.resetMain' \"\$f\" || echo \"\$f\"; done"

check BLOCK "core/feature 모듈에서 Compose BOM 재선언" \
  "rg -n -g 'build.gradle.kts' '(implementation|androidTestImplementation)\(platform\(libs\.androidx\.compose\.bom\)\)' core feature"

check BLOCK "취소 재전파가 catch 첫 문장이 아님" \
  "rg -n -g '*.kt' 'catch \([^)]*CancellationException\)' . \
   | grep -v -E '/src/(test|androidTest)/' \
   | while IFS=: read -r f n _; do \
       case \"\$(sed -n \"\$((n+1))p\" \"\$f\")\" in *throw*) ;; *) echo \"\$f:\$n\";; esac \
     done"

check BLOCK "예외 원문(error.message)을 화면에 그대로 노출" \
  "rg -n -g '*ViewModel.kt' '\\.message\\b' . | grep -v '/src/test/' | grep -v 'intent\\.message'"

echo
echo "== WARN — 기존 부채 (docs/BACKLOG.md '코드 관용구 정합성') =="

check WARN "하드코딩 색 (Color(0xFF...))" \
  "rg -n -g '*.kt' 'Color\(0xFF' . | grep -v '/theme/'"

check WARN "ViewModel 선언명 ≠ 파일명" \
  "rg -n -g '*.kt' -o -r '\$1' --no-heading 'class (\w+ViewModel)\b' . \
   | grep -v -E '/src/(test|androidTest)/' \
   | while IFS=: read -r f _ vm; do [ \"\$(basename \"\$f\" .kt)\" != \"\$vm\" ] && echo \"\$vm ← \$f\"; done"

check WARN "Effect를 SharedFlow로 전달" \
  "rg -l -g '*.kt' 'MutableSharedFlow' . | grep 'ViewModel\.kt$' | grep -v '/src/test/'"

check WARN "component(단수) 패키지" \
  "find app core feature -type d -name component -not -path '*/build/*'"

check WARN "app이 Compose BOM 직접 선언" \
  "rg -n -g 'build.gradle.kts' 'platform\(libs\.androidx\.compose\.bom\)' app"

echo
echo "== INFO — 강제하지 않음. 리뷰 때 볼 값 =="

# 컨벤션을 어느 쪽으로 정할지 아직 미결이라 규칙으로 강제하지 않는다 → docs/conventions/structure.md
check INFO "Contract를 ViewModel 파일에 내장한 화면" \
  "rg -l -g '*ViewModel.kt' 'data class \w+UiState' ."

check INFO "상태 프로퍼티를 _state로 쓰는 ViewModel (_uiState로 통일 예정)" \
  "rg -l -g '*ViewModel.kt' 'private val _state\b' ."

check INFO "authenticatedRequest 헬퍼를 자체 보유한 Repository" \
  "rg -l -g '*.kt' 'authenticatedRequest' . | grep -v '/src/test/'"

check INFO "JUnit4를 쓰는 JVM 테스트 (Roborazzi는 정상)" \
  "rg -l -g '*.kt' 'org\.junit\.Test' . | grep '/src/test/'"

echo
if [ "$block_fail" -ne 0 ]; then
  echo "BLOCK 규칙 위반이 있습니다. 위 목록을 고치거나, 예외라면 PR에 근거를 적으세요."
  exit 1
fi
echo "BLOCK 통과. WARN 합계 ${warn_total}건 — 기존 부채이므로 실패시키지 않습니다."
