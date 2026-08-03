# HANDOFF — Current Task

Status: DONE
Task: 마이페이지 프로필 카드 도장 임팩트 애니메이션
Branch: feat/mypage-profile-card-stamp-impact
Base: origin/develop (55e3654d)
Risk: LOW

## Context

사용자가 제공한 `ProfileCardStampImpact (1).tsx`의 진입 모션을 Android Compose 마이페이지 프로필 카드에 적용한다. 현재 카드는 `feature/mypage`의 `ProfileCard.kt`가 직접 렌더하며, 도장 이미지는 이미 카드 콘텐츠 뒤쪽 레이어에 있다.

## Goal

기존 프로필 카드 UI와 데이터 동작을 변경하지 않고 카드 진입과 도장 임팩트 모션을 제공한다.

## Spec

- `ProfileCard`가 composition에 진입하면 카드 전체를 16dp 아래·투명 상태에서 340ms cubic ease-out으로 원위치·불투명 상태까지 애니메이션한다.
- 카드 진입과 동시에 900ms 모션을 실행한다. 최초 540ms는 1배를 유지하고, 다음 72ms에 1.012배, 마지막 288ms에 1배로 복귀한다.
- 카드 뒤 우측 상단의 기존 도장 이미지는 카드보다 200ms 늦게 시작한다. 1.8배·-18도·투명 상태에서 700ms 동안 0.97배·3도·alpha 0.28, 1.02배·-2도·alpha 0.16, 1배·0도·alpha 0.20 순으로 정착한다.
- 기존 레이아웃, 색상, 타이포, 이미지, 클릭 동작과 API/ViewModel 흐름은 변경하지 않는다.

## Acceptance

- 마이페이지 프로필 카드가 화면 진입 시 위 명세의 카드 모션을 한 번 재생한다.
- 기존 도장 이미지가 콘텐츠 뒤 레이어를 유지하면서 지정된 지연·스케일·회전·투명도 모션을 한 번 재생한다.
- 애니메이션 종료 후 카드와 도장의 시각 결과가 변경 전과 동일하다.
- 변경 범위는 `ProfileCard.kt`와 현재 HANDOFF 기록으로 제한되고 `./gradlew :feature:mypage:assembleDebug`, `./gradlew assembleDebug`, `./gradlew test`가 통과한다.

## Expected Files

- `feature/mypage/src/main/java/com/dororong/rodi/feature/mypage/components/ProfileCard.kt`
- `docs/handoff/archive/20260802-mypage-profile-card-stamp-impact.md`

## Verification

- `./gradlew :feature:mypage:assembleDebug`
- `./gradlew assembleDebug`
- `./gradlew test`
- Android Studio Compose Preview 또는 설치된 앱에서 마이페이지 진입·복귀 시 카드와 도장 모션을 눈으로 확인한다.

## Out of Scope

- 프로필 카드의 레이아웃·색상·타이포·에셋 변경
- 마이페이지 데이터, ViewModel, API, 내비게이션 변경
- 다른 화면의 애니메이션 추가

## Implementation Result

- Changed files: `feature/mypage/src/main/java/com/dororong/rodi/feature/mypage/components/ProfileCard.kt`, `docs/handoff/archive/20260802-mypage-profile-card-stamp-impact.md`
- `ProfileCard`의 기존 레이아웃과 콘텐츠를 유지한 채 outer layer에 카드 진입·미세 반동을, 기존 도장 `Image` layer에 지연된 스케일·회전·alpha 임팩트를 적용했다.
- Preview에서는 최종 정지 상태를 제공해 기존 프로필 카드 Preview가 투명한 초기 프레임으로 보이지 않게 했다.
- Verification: `./gradlew :feature:mypage:assembleDebug` PASS, `./gradlew assembleDebug` PASS, `./gradlew test` PASS.
- Open questions: 연결된 Android 기기가 없어 마이페이지의 정방향 진입 및 뒤로 복귀 시 실제 모션을 캡처하지 못했다. `adb devices`에 연결 기기가 없었다.

## Review

- 독립 검토 기준: 최신 `origin/develop`(55e3654d)과 현재 최종 작업 트리. `origin/develop...HEAD` 및 `origin/develop..HEAD`는 모두 빈 diff이며, 미커밋 작업 트리 diff는 Expected Files의 두 파일로만 제한된다.
- Acceptance 1 — 충족: `ProfileCard.kt:95-106`의 `LaunchedEffect(isInPreview)`가 composition 진입 때 한 번 실행되며, 카드 alpha와 16dp translationY를 340ms cubic ease-out으로, scale을 540ms 유지·72ms 확대·288ms 복귀(총 900ms)로 처리한다.
- Acceptance 2 — 충족: 기존 도장 `Image`는 `Column`보다 먼저 배치되어 콘텐츠 뒤 레이어를 유지한다. `ProfileCard.kt:108-125`는 200ms 지연 후 700ms 동안 scale/rotation/alpha를 각각 0.97/3도/0.28, 1.02/-2도/0.16, 1/0도/0.20으로 완료한다.
- Acceptance 3 — 충족: 모든 `Animatable`의 최종값은 기존 카드의 alpha 1·translationY 0·scale 1 및 도장의 alpha 0.2·scale 1·rotation 0으로 원상 복귀한다. 레이아웃·데이터·클릭 흐름 변경은 없다.
- Acceptance 4 — 충족: `git diff --check` PASS, 변경 파일은 Expected Files와 일치. 현재 작업 트리에서 `./gradlew :feature:mypage:assembleDebug`, `./gradlew assembleDebug`, `./gradlew test` 모두 BUILD SUCCESSFUL.
- 발견사항: blocking finding 없음. `adb devices`에 연결 기기가 없어 실제 마이페이지 진입·복귀 모션의 눈검증은 수행하지 못했다. 정적 코드·빌드 검토로는 명세를 충족하며, 기기 사용 가능 시 확인할 비차단 수동 QA 항목으로 남긴다.

## Review Triage

## Revision Plan

## Revision Result

## Final Review

- 승인: 모든 Acceptance를 충족했고, `origin/develop` 대비 최종 작업 트리에는 `ProfileCard.kt`와 현재 HANDOFF 기록만 변경되었다. 모션 타이밍·최종 정지 상태·기존 도장 레이어·기존 상호작용 보존을 코드로 확인했으며 정적/빌드 검증도 통과했다.
