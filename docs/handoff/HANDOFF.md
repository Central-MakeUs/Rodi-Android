# HANDOFF — Login tooltip and bottom CTA

> Claude(기획)와 Codex(구현)가 주고받는 **단일 활성 작업 채널**.
> 완료되면 `docs/handoff/archive/<날짜>-<작업>.md`로 옮기고 이 파일은 다음 작업으로 비운다.

Status: IMPL_DONE            <!-- PLANNING | READY_FOR_IMPL | IMPL_DONE | IN_REVIEW | DONE | BLOCKED -->
Branch: develop

## Context (왜)
Figma 로그인 화면 기준으로 Android 로그인 화면의 하단 CTA와 최근 로그인 툴팁을 반영한다.

## Spec (무엇을·어떻게)
- 첫 진입 상태에서는 상단 우측 `둘러보기`를 표시하고, 하단에는 카카오 로그인 버튼만 배치한다.
- 최근 카카오 로그인 이력이 있으면 `둘러보기`를 숨기고 카카오 버튼 위에 `최근에 로그인했어요!` 툴팁을 표시한다.
- iOS 전용 Apple 로그인 버튼은 Android 화면에서 구현하지 않는다.
- 참고 repo `dnd-14th-2-android`의 툴팁 꼬리/본문 구조를 ROUTI 토큰 기반 Compose 컴포넌트로 축소 구현한다.
- Figma SVG asset 기준으로 RODI 로고와 카카오 아이콘을 Android vector drawable로 반영한다.

## Files to touch
- `app/src/main/java/com/dororong/rodi/ui/RodiApp.kt`
- `core/data/src/main/java/com/dororong/rodi/core/data/auth/AuthTokenStore.kt`
- `core/ui/src/main/java/com/dororong/rodi/core/ui/components/RodiTooltip.kt`
- `feature/auth/src/main/java/com/dororong/rodi/feature/auth/KakaoLoginButton.kt`
- `feature/auth/src/main/java/com/dororong/rodi/feature/auth/LoginScreen.kt`
- `feature/auth/src/main/res/drawable/ic_kakao.xml`
- `feature/auth/src/main/res/drawable/ic_rodi_logo.xml`
- `docs/handoff/HANDOFF.md`

## Acceptance criteria
- [x] 첫 진입 로그인 화면에 `둘러보기`가 표시된다.
- [x] 최근 카카오 로그인 이력이 있는 로그인 화면에만 툴팁이 표시된다.
- [x] Android 로그인 화면에는 Apple 로그인 버튼이 없다.
- [x] 카카오 버튼은 하단 정렬 기준으로 표시된다.
- [x] RODI 로고와 카카오 아이콘은 Figma 벡터 asset을 사용한다.
- [x] debug build가 성공한다.

## Verification
```
./gradlew :feature:auth:testDebugUnitTest
./gradlew :core:data:testDebugUnitTest
./gradlew assembleDebug
```

## Out of scope
- Apple 로그인
- 로그아웃 플로우 신설
- Figma 원본 수정

---
## Codex Result   <!-- Codex가 구현 후 채움 → Status=IMPL_DONE (또는 막히면 BLOCKED) -->
- Changed files: `app/src/main/java/com/dororong/rodi/ui/RodiApp.kt`, `core/data/src/main/java/com/dororong/rodi/core/data/auth/AuthTokenStore.kt`, `core/ui/src/main/java/com/dororong/rodi/core/ui/components/RodiTooltip.kt`, `feature/auth/src/main/java/com/dororong/rodi/feature/auth/KakaoLoginButton.kt`, `feature/auth/src/main/java/com/dororong/rodi/feature/auth/LoginScreen.kt`, `feature/auth/src/main/res/drawable/ic_kakao.xml`, `feature/auth/src/main/res/drawable/ic_rodi_logo.xml`, `docs/handoff/HANDOFF.md`
- Build/test: `./gradlew :feature:auth:testDebugUnitTest` GREEN; `./gradlew :core:data:testDebugUnitTest` GREEN; `./gradlew assembleDebug` GREEN
- Open questions: none

---
## Claude Review
- Blocking:
- Nits:
- Verdict:   <!-- APPROVE | NEEDS_CHANGES -->
---
