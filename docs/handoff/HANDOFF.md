# HANDOFF — <작업 제목>

> Claude(기획)와 Codex(구현)가 주고받는 **단일 활성 작업 채널**.
> 완료되면 `docs/handoff/archive/<날짜>-<작업>.md`로 옮기고 이 파일은 다음 작업으로 비운다.

Status: IMPL_DONE            <!-- PLANNING | READY_FOR_IMPL | IMPL_DONE | IN_REVIEW | DONE | BLOCKED -->
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
- Changed files: feature/entry/src/main/java/com/dororong/rodi/feature/entry/EntryComponents.kt, feature/entry/src/main/java/com/dororong/rodi/feature/entry/EntryFlow.kt, feature/entry/src/main/java/com/dororong/rodi/feature/entry/EntryViewModel.kt, feature/entry/src/main/java/com/dororong/rodi/feature/entry/LocationPermissionContent.kt, feature/entry/src/main/java/com/dororong/rodi/feature/entry/TermsAgreementContent.kt, feature/entry/src/main/java/com/dororong/rodi/feature/entry/NicknameContent.kt, feature/entry/src/main/java/com/dororong/rodi/feature/entry/CareerContent.kt, feature/entry/src/main/java/com/dororong/rodi/feature/entry/PreferenceContent.kt, feature/entry/src/test/java/com/dororong/rodi/feature/entry/EntryViewModelTest.kt
- Build/test: `./gradlew :feature:entry:testDebugUnitTest` GREEN; `./gradlew assembleDebug` GREEN
- Open questions: none

---
## Claude Review  <!-- Claude가 검토 후 채움 -->
- Blocking:
- Nits:
- Verdict:   <!-- APPROVE | NEEDS_CHANGES -->
