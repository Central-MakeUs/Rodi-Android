# HANDOFF — Entry flow restore and guest access

> Claude(기획)와 Codex(구현)가 주고받는 **단일 활성 작업 채널**.
> 완료되면 `docs/handoff/archive/<날짜>-<작업>.md`로 옮기고 이 파일은 다음 작업으로 비운다.

Status: IMPL_DONE            <!-- PLANNING | READY_FOR_IMPL | IMPL_DONE | IN_REVIEW | DONE | BLOCKED -->
Branch: develop

## Context (왜)
앱 진입 플로우 진행 중 앱이 종료되더라도 사용자는 기존에 진행하던 화면과 선택/입력 상태로 돌아와야 한다.
또한 한 번 둘러보기를 선택한 사용자는 이후에도 둘러보기 권한을 유지해야 하며, 로그인 시 로컬에 저장된 온보딩 draft를 서버에 함께 보내야 한다.

## Spec (무엇을·어떻게)
- 앱 진입 플로우의 현재 단계는 로컬에 저장하고 앱 재실행 시 해당 단계로 복원한다.
- 약관/운전 주의사항 체크 상태도 앱 재실행 후 그대로 복원한다.
- 온보딩 닉네임, 경력, 최근 운전, 도로주행 경험, 혼자 운전/주차, 선호 상황, 차종, 목표 입력은 선택/입력 즉시 draft로 저장한다.
- 온보딩 화면 복원 시 기존 선택/입력값이 모두 채워진 상태여야 한다.
- 조건부 문항에서 숨겨진 응답은 stale 값으로 복원하지 않는다.
- 둘러보기 선택 시 guest access를 로컬에 저장하고, 이후 앱 실행에서는 로그인 화면을 다시 띄우지 않는다.
- guest access 사용자는 entry flow 미완료 시 entry로, 완료 시 home으로 진입한다.
- 카카오 로그인 시 로컬 온보딩 draft가 있으면 `onboardingProfile` payload로 함께 전송한다.
- 완료 시 기존처럼 온보딩 profile 저장 후 entry completed를 저장하고, 완료된 사용자는 진입 플로우를 건너뛴다.

## Files to touch
- `app/src/main/java/com/dororong/rodi/ui/RodiApp.kt`
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/AuthRepository.kt`
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/EntryProgress.kt`
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/EntryRepository.kt`
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/OnboardingProfile.kt`
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/OnboardingRepository.kt`
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/GetEntryProgressUseCase.kt`
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/GetGuestAccessUseCase.kt`
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/GetOnboardingProfileUseCase.kt`
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/GrantGuestAccessUseCase.kt`
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/LoginWithKakaoUseCase.kt`
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/SaveEntryProgressUseCase.kt`
- `core/domain/src/test/kotlin/com/dororong/rodi/core/domain/usecase/LoginWithKakaoUseCaseTest.kt`
- `core/data/src/main/java/com/dororong/rodi/core/data/AuthRepositoryImpl.kt`
- `core/data/src/main/java/com/dororong/rodi/core/data/EntryPreferences.kt`
- `core/data/src/main/java/com/dororong/rodi/core/data/EntryRepositoryImpl.kt`
- `core/data/src/main/java/com/dororong/rodi/core/data/OnboardingPreferences.kt`
- `core/data/src/main/java/com/dororong/rodi/core/data/OnboardingRepositoryImpl.kt`
- `core/data/src/main/java/com/dororong/rodi/core/data/auth/AuthApi.kt`
- `core/data/src/test/java/com/dororong/rodi/core/data/AuthRepositoryImplTest.kt`
- `feature/auth/src/main/java/com/dororong/rodi/feature/auth/LoginViewModel.kt`
- `feature/auth/src/test/java/com/dororong/rodi/feature/auth/LoginViewModelTest.kt`
- `feature/entry/src/main/java/com/dororong/rodi/feature/entry/EntryFlow.kt`
- `feature/entry/src/main/java/com/dororong/rodi/feature/entry/EntryViewModel.kt`
- `feature/entry/src/test/java/com/dororong/rodi/feature/entry/EntryViewModelTest.kt`
- `docs/handoff/HANDOFF.md`

## Acceptance criteria
- [x] 앱 종료 후 재실행하면 진입 플로우의 마지막 진행 단계로 복원된다.
- [x] 약관/주의사항 체크 상태가 복원된다.
- [x] 온보딩 선택/입력값이 draft로 저장되고 복원된다.
- [x] 조건부 문항에서 숨겨진 응답은 stale 값으로 복원되지 않는다.
- [x] 둘러보기 선택 후 앱을 다시 실행해도 로그인 화면이 다시 뜨지 않는다.
- [x] 둘러보기 권한은 entry completed 저장 시 지워지지 않는다.
- [x] 카카오 로그인 요청에 로컬 온보딩 draft가 함께 포함된다.
- [x] 앱 debug build가 성공한다.

## Verification
```
git diff --check
./gradlew :core:domain:test
./gradlew :core:data:testDebugUnitTest
./gradlew :feature:auth:testDebugUnitTest
./gradlew :feature:entry:testDebugUnitTest
./gradlew assembleDebug
```

## Out of scope
- 완료된 entry flow 재진입/초기화 UX
- 앱 종료 직전 DataStore write가 완료되기 전 강제 종료되는 극단 케이스 보장
- 서버가 요구하는 최종 onboarding payload 필드명/enum 값 변경 대응

---
## Codex Result   <!-- Codex가 구현 후 채움 → Status=IMPL_DONE (또는 막히면 BLOCKED) -->
- Changed files: `app/src/main/java/com/dororong/rodi/ui/RodiApp.kt`, `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/AuthRepository.kt`, `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/EntryProgress.kt`, `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/EntryRepository.kt`, `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/OnboardingProfile.kt`, `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/OnboardingRepository.kt`, `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/GetEntryProgressUseCase.kt`, `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/GetGuestAccessUseCase.kt`, `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/GetOnboardingProfileUseCase.kt`, `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/GrantGuestAccessUseCase.kt`, `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/LoginWithKakaoUseCase.kt`, `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/SaveEntryProgressUseCase.kt`, `core/domain/src/test/kotlin/com/dororong/rodi/core/domain/usecase/LoginWithKakaoUseCaseTest.kt`, `core/data/src/main/java/com/dororong/rodi/core/data/AuthRepositoryImpl.kt`, `core/data/src/main/java/com/dororong/rodi/core/data/EntryPreferences.kt`, `core/data/src/main/java/com/dororong/rodi/core/data/EntryRepositoryImpl.kt`, `core/data/src/main/java/com/dororong/rodi/core/data/OnboardingPreferences.kt`, `core/data/src/main/java/com/dororong/rodi/core/data/OnboardingRepositoryImpl.kt`, `core/data/src/main/java/com/dororong/rodi/core/data/auth/AuthApi.kt`, `core/data/src/test/java/com/dororong/rodi/core/data/AuthRepositoryImplTest.kt`, `feature/auth/src/main/java/com/dororong/rodi/feature/auth/LoginViewModel.kt`, `feature/auth/src/test/java/com/dororong/rodi/feature/auth/LoginViewModelTest.kt`, `feature/entry/src/main/java/com/dororong/rodi/feature/entry/EntryFlow.kt`, `feature/entry/src/main/java/com/dororong/rodi/feature/entry/EntryViewModel.kt`, `feature/entry/src/test/java/com/dororong/rodi/feature/entry/EntryViewModelTest.kt`, `docs/handoff/HANDOFF.md`
- Build/test: `git diff --check` GREEN; `./gradlew :core:domain:test` GREEN; `./gradlew :core:data:testDebugUnitTest` GREEN; `./gradlew :feature:auth:testDebugUnitTest` GREEN; `./gradlew :feature:entry:testDebugUnitTest` GREEN; `./gradlew assembleDebug` GREEN
- Open questions: 서버 계약은 `onboardingProfile` 필드와 enum `name` 값 기준으로 구현했다. 백엔드 최종 스펙이 다르면 필드명/값 매핑만 맞추면 된다.

---
## Claude Review  <!-- Claude가 검토 후 채움 -->
- Blocking:
- Nits:
- Verdict:   <!-- APPROVE | NEEDS_CHANGES -->
---
