# HANDOFF — Onboarding conditional question update

> Claude(기획)와 Codex(구현)가 주고받는 **단일 활성 작업 채널**.
> 완료되면 `docs/handoff/archive/<날짜>-<작업>.md`로 옮기고 이 파일은 다음 작업으로 비운다.

Status: IMPL_DONE            <!-- PLANNING | READY_FOR_IMPL | IMPL_DONE | IN_REVIEW | DONE | BLOCKED -->
Branch: develop

## Context (왜)
온보딩 설문 Figma가 갱신되어 경력 문항의 점진 노출 규칙과 선호 문항의 목표 입력 UX를 맞춰야 한다.

## Spec (무엇을·어떻게)
- 경력 화면은 처음에 "면허 취득 후 실제 운전한 기간" 문항만 노출한다.
- `2~10년`, `10년 이상` 선택 시 즉시 다음 버튼을 활성화한다.
- 그 외 운전 기간 선택 시 "가장 최근 운전"과 "면허 취득후 도로주행 경험" 문항을 함께 노출한다.
- 도로주행 경험은 복수 선택이며, `혼자 연습`이 포함되지 않으면 다음 버튼을 활성화한다.
- 도로주행 경험에 `혼자 연습`이 포함되면 "혼자 운전 범위"와 "혼자 주차 수준" 문항을 함께 노출하고, 둘 다 선택해야 다음 버튼을 활성화한다.
- 선호 화면은 모든 문항을 함께 노출한다.
- 선호 화면의 다음 버튼은 "더 연습해보고 싶은 상황"을 하나라도 선택하면 활성화한다.
- 차종과 목표 입력은 선택 입력이다.
- 목표 입력은 포커스 시 테두리 활성 상태를 표시하고, 30자까지 입력 가능하며 초과 문자는 입력되지 않는다.

## Files to touch
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/OnboardingProfile.kt`
- `core/data/src/main/java/com/dororong/rodi/core/data/OnboardingPreferences.kt`
- `feature/entry/src/main/java/com/dororong/rodi/feature/entry/EntryViewModel.kt`
- `feature/entry/src/main/java/com/dororong/rodi/feature/entry/EntryFlow.kt`
- `feature/entry/src/main/java/com/dororong/rodi/feature/entry/CareerContent.kt`
- `feature/entry/src/main/java/com/dororong/rodi/feature/entry/PreferenceContent.kt`
- `feature/entry/src/test/java/com/dororong/rodi/feature/entry/EntryViewModelTest.kt`
- `docs/handoff/HANDOFF.md`

## Acceptance criteria
- [x] 장기 경력 선택 시 상세 문항 없이 다음 버튼이 활성화된다.
- [x] 짧은 경력 선택 시 최근 운전/도로주행 경험 문항이 함께 노출된다.
- [x] 도로주행 경험에 혼자 연습이 없으면 다음 버튼이 활성화된다.
- [x] 도로주행 경험에 혼자 연습이 포함되면 추가 2문항을 모두 선택해야 다음 버튼이 활성화된다.
- [x] 목표 입력은 30자에서 잘리고 카운터가 표시된다.
- [x] 앱 debug build가 성공한다.

## Verification
```
./gradlew :feature:entry:testDebugUnitTest
./gradlew assembleDebug
```

## Out of scope
- 서버 전송 스펙 변경
- 온보딩 이후 추천 알고리즘 변경

---
## Codex Result   <!-- Codex가 구현 후 채움 → Status=IMPL_DONE (또는 막히면 BLOCKED) -->
- Changed files: `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/OnboardingProfile.kt`, `core/data/src/main/java/com/dororong/rodi/core/data/OnboardingPreferences.kt`, `feature/entry/src/main/java/com/dororong/rodi/feature/entry/EntryViewModel.kt`, `feature/entry/src/main/java/com/dororong/rodi/feature/entry/EntryFlow.kt`, `feature/entry/src/main/java/com/dororong/rodi/feature/entry/CareerContent.kt`, `feature/entry/src/main/java/com/dororong/rodi/feature/entry/PreferenceContent.kt`, `feature/entry/src/test/java/com/dororong/rodi/feature/entry/EntryViewModelTest.kt`, `docs/handoff/HANDOFF.md`
- Build/test: `./gradlew :feature:entry:testDebugUnitTest` GREEN; `git diff --check` GREEN; `./gradlew assembleDebug` GREEN
- Open questions: none

---
## Claude Review  <!-- Claude가 검토 후 채움 -->
- Blocking:
- Nits:
- Verdict:   <!-- APPROVE | NEEDS_CHANGES -->
---
