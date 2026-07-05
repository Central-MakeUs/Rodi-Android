#!/usr/bin/env bash
# 실기기에서 Baseline Profile 적용/미적용 상태의 완전 첫 실행(fresh install) 콜드 스타트를 비교 측정한다.
# 사용법:
#   ./benchmark/measure-cold-start.sh [APK_PATH] [-s SERIAL] [-n ITERATIONS]
# APK_PATH를 주면 기존 앱을 지우고 새로 설치한 뒤 측정한다. 생략하면 이미 설치된 앱으로 측정한다.
set -euo pipefail

PKG="com.dororong.rodi"
ACTIVITY=".MainActivity"
ITERATIONS=3
APK_PATH=""
SERIAL="$(adb devices | awk 'NR==2{print $1}')"

while [[ $# -gt 0 ]]; do
  case "$1" in
    -s) SERIAL="$2"; shift 2 ;;
    -n) ITERATIONS="$2"; shift 2 ;;
    *) APK_PATH="$1"; shift ;;
  esac
done

if [[ -z "$SERIAL" ]]; then
  echo "✗ 연결된 기기가 없습니다 (adb devices)"; exit 1
fi
adb() { command adb -s "$SERIAL" "$@"; }

echo "▶ 대상 기기: $SERIAL / 패키지: $PKG"

if [[ -n "$APK_PATH" ]]; then
  echo "▶ 기존 앱 삭제 후 새로 설치: $APK_PATH"
  adb uninstall "$PKG" 2>/dev/null || true
  adb install "$APK_PATH"
fi

measure() {
  local label="$1"
  local -a times=()
  for ((i = 1; i <= ITERATIONS; i++)); do
    adb shell pm clear "$PKG" > /dev/null
    adb shell am force-stop "$PKG"
    local total
    total=$(adb shell am start -W -n "$PKG/$ACTIVITY" | grep TotalTime | grep -o '[0-9]*')
    times+=("$total")
  done
  local sum=0
  for t in "${times[@]}"; do sum=$((sum + t)); done
  local avg=$((sum / ITERATIONS))
  echo "  [$label] ${times[*]} ms  →  평균 ${avg}ms" >&2
  echo "$avg"
}

echo "▶ Baseline Profile 미적용(verify-only) 상태로 초기화"
adb shell cmd package compile --reset "$PKG" > /dev/null
NONE_AVG=$(measure "미적용")

echo "▶ Baseline Profile 적용(speed-profile) 상태로 컴파일"
adb shell cmd package compile -m speed-profile -f "$PKG" > /dev/null
PROFILE_AVG=$(measure "적용")

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
if [[ "$NONE_AVG" -gt 0 ]]; then
  IMPROVEMENT=$(awk "BEGIN { printf \"%.1f\", ($NONE_AVG - $PROFILE_AVG) / $NONE_AVG * 100 }")
  echo "▶ 미적용 평균 ${NONE_AVG}ms → 적용 평균 ${PROFILE_AVG}ms  (${IMPROVEMENT}% 개선)"
fi
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "▶ 측정 종료 — 기기를 profile 적용 상태로 복원해둠"
