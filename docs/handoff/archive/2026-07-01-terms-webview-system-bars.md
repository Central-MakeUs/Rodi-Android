# HANDOFF — 약관 WebView 시스템 바 아이콘 동적 전환

> Claude(기획)와 Codex(구현)가 주고받는 **단일 활성 작업 채널**.
> 완료되면 `docs/handoff/archive/<날짜>-<작업>.md`로 옮기고 이 파일은 다음 작업으로 비운다.

Status: IMPL_DONE
Branch: fix/entry-terms-webview-system-bars

## Context (왜)
`TermsWebView.kt`가 화면 진입 시 `window.statusBarColor`를 직접 대입해 상태바를 검게 칠하는데, 이 API는 deprecated이고 edge-to-edge 환경에서 신뢰할 수 없다. 게다가 내비게이션 바 아이콘 색은 전혀 건드리지 않아서, 검은 배경 웹뷰 위에서 내비게이션 바 아이콘이 라이트 테마 기준(어두운 색)으로 남아 시인성이 떨어진다.

## Spec (무엇을·어떻게)
`TermsWebView.kt`의 `DisposableEffect(view)` 블록(현재 49~67번 줄)을 아래와 같이 바꾼다.

1. `window.statusBarColor` 직접 대입 로직을 **완전히 제거**한다. (`Color.BLACK` 대입, `previousStatusBarColor` 저장/복원 모두 삭제)
2. `WindowCompat.getInsetsController(window, view)`로 얻은 `controller`를 통해서만 아이콘 색을 제어한다:
   - 진입 시: `controller.isAppearanceLightStatusBars = false`, `controller.isAppearanceLightNavigationBars = false` (둘 다 아이콘을 흰색으로 — `isAppearanceLight*` = false가 밝은/흰색 아이콘을 의미)
   - 이탈 시(`onDispose`): 진입 전 저장해둔 `previousLightStatusBars`, `previousLightNavigationBars` 값으로 각각 원복한다.
3. `import android.graphics.Color`가 더 이상 쓰이지 않으면 제거한다.
4. 배경색 처리(`Box`의 `ComposeColor.Black`, `statusBarsPadding()`)는 이번 스코프에서 건드리지 않는다 — 아이콘 색 전환만 다룬다.

## Files to touch
- `app/src/main/java/com/dororong/rodi/entry/TermsWebView.kt`

## Acceptance criteria
- [ ] `window.statusBarColor` 직접 대입 코드가 삭제됨 (deprecated API 미사용)
- [ ] 진입 시 `isAppearanceLightStatusBars = false` **및** `isAppearanceLightNavigationBars = false`가 함께 설정됨
- [ ] `onDispose`에서 상태바·내비게이션 바 아이콘 appearance가 각각 진입 전 값으로 정확히 원복됨
- [ ] 미사용 import(`android.graphics.Color` 등) 정리됨
- [ ] `RodiTheme` 토큰/Material 아이콘 등 기존 컨벤션 위반 없음
- [ ] `./gradlew assembleDebug` 성공

## Verification
```
./gradlew assembleDebug
```
에뮬레이터에서 EntryFlow → 약관 웹뷰 진입 시 상태바·내비게이션 바 아이콘이 흰색으로 바뀌고, 뒤로가기로 이탈 시 원래(어두운) 아이콘 색으로 복원되는지 스크린샷으로 확인.

## Out of scope
- 웹뷰 배경색/패딩 레이아웃 변경
- EntryFlow의 다른 단계(위치권한, 운전 주의사항) 시스템 바 처리
- 상태바 배경 색상 자체(스크림) 커스터마이징 — 이번엔 아이콘 appearance만

---
## Codex Result   <!-- Codex가 구현 후 채움 → Status=IMPL_DONE (또는 막히면 BLOCKED) -->
- Changed files: app/src/main/java/com/dororong/rodi/entry/TermsWebView.kt, docs/handoff/HANDOFF.md
- Build/test: ./gradlew assembleDebug GREEN
- Open questions: none

---
## Claude Review  <!-- Claude가 검토 후 채움 -->
- Blocking: 없음
- Nits: 없음
- Verdict: APPROVE
