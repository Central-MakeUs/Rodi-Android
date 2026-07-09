# HANDOFF — Splash screen update

> Claude(기획)와 Codex(구현)가 주고받는 **단일 활성 작업 채널**.
> 완료되면 `docs/handoff/archive/<날짜>-<작업>.md`로 옮기고 이 파일은 다음 작업으로 비운다.

Status: IMPL_DONE            <!-- PLANNING | READY_FOR_IMPL | IMPL_DONE | IN_REVIEW | DONE | BLOCKED -->
Branch: develop

## Context (왜)
앱 시작 시 Android 기본 스플래시 아이콘 대신 Rodi 브랜드 스플래시를 보여줘야 한다.

## Spec (무엇을·어떻게)
- Android 12+ 시스템 기본 스플래시 아이콘은 보이지 않게 한다.
- 앱 초기 로딩 화면은 흰 배경 중앙에 Figma 기준 RODI 워드마크와 `운전연습의 시작, 로디` 문구만 보여준다.
- 로그인 버튼, 최근 로그인 팝오버 등 로그인 화면의 다른 요소는 스플래시에 포함하지 않는다.
- 커밋은 만들지 않는다.

## Files to touch
- `app/src/main/java/com/dororong/rodi/ui/RodiApp.kt`
- `app/src/main/res/drawable/ic_rodi_wordmark.xml`
- `app/src/main/res/drawable/ic_splash_transparent.xml`
- `app/src/main/res/values-v31/themes.xml`
- `docs/handoff/HANDOFF.md`

## Acceptance criteria
- [x] 기본 시스템 스플래시 아이콘이 표시되지 않는다.
- [x] 앱 스플래시는 RODI 워드마크와 `운전연습의 시작, 로디` 문구만 표시한다.
- [x] 앱 debug build가 성공한다.

## Verification
```
./gradlew assembleDebug
```

## Out of scope
- 로그인 화면 레이아웃 변경
- 앱 아이콘 변경
- 커밋 생성

---
## Codex Result
- Changed files: `app/src/main/java/com/dororong/rodi/ui/RodiApp.kt`, `app/src/main/res/drawable/ic_rodi_wordmark.xml`, `app/src/main/res/drawable/ic_splash_transparent.xml`, `app/src/main/res/values-v31/themes.xml`, `docs/handoff/HANDOFF.md`
- Build/test: `./gradlew assembleDebug` GREEN
- Open questions: none

---
## Claude Review
- Blocking:
- Nits:
- Verdict:   <!-- APPROVE | NEEDS_CHANGES -->
---
