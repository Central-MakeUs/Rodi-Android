# HANDOFF — Current Task

Status: DONE
Task: 프로필 카드 Loading Skeleton 가시성 보정
Branch: codex/skeleton-shimmer-setup
Base: origin/develop (55e3654d) + 4934601a
Risk: LOW

## Context

현재 프로필 카드 loading 표면은 `primary20`(F4F4FF)이고 내부 skeleton은 `gray100`(F5F5F5)이라 색상 대비가 거의 없다. 그 결과 내부 이미지·텍스트·태그 placeholder가 카드 전체 색상 변경처럼 보인다.

## Goal

프로필 카드의 고정 배경은 실제 카드 chrome으로 유지하고, 로딩 데이터만 분리된 skeleton으로 분명히 보이게 한다.

## Spec

- `MyPageProfileCardLoadingContent`의 카드 배경을 white로 유지하고 기존 primary50 border는 보존한다.
- 캐릭터·닉네임·레벨·태그·운전 목표 skeleton의 크기와 간격은 유지하되, outer card 자체에는 shimmer를 적용하지 않는다.
- loading Preview를 추가해 콘텐츠 단위 skeleton 상태를 직접 확인할 수 있게 한다.
- MyPage 외 skeleton, navigation, ViewModel, 데이터 모델은 변경하지 않는다.

## Acceptance

- 카드 바탕은 non-loading chrome이며, 내부 placeholder가 눈에 구별되는 개별 로딩 요소로 보인다.
- 카드 높이·border·padding 및 로딩 중 settings 비활성 동작은 유지된다.
- 변경 파일은 `MyPageScreen.kt`, `DESIGN_ASSUMPTIONS.md`, `HANDOFF.md`로 제한되고 `./gradlew :feature:mypage:assembleDebug`가 통과한다.

## Expected Files

- `docs/DESIGN_ASSUMPTIONS.md`
- `docs/handoff/HANDOFF.md`
- `feature/mypage/src/main/java/com/dororong/rodi/feature/mypage/MyPageScreen.kt`

## Verification

- `./gradlew :feature:mypage:assembleDebug`
- `git diff --check`
- MyPage loading Preview를 확인한다.

## Out of Scope

- Home·저장 장소·운전 목표 skeleton 수정
- 공용 `RodiSkeleton` API·테마 토큰·서버 상태·navigation 변경

## Implementation Result

- Changed files: 프로필 카드 loading chrome을 `primary20`에서 white로 바꿔 내부 `gray100` skeleton과 분리했고, 해당 loading Preview를 추가했다. D-002에 색 대비 근거를 기록했다.
- 상태 보존: 카드 높이(227dp), primary50 border, padding, MyPage loading 중 settings 비활성은 바꾸지 않았다.
- Verification: `./gradlew :feature:mypage:assembleDebug`, `git diff --check` 성공.
- Visual QA: 최신 APK를 에뮬레이터에 설치했다. 네트워크 응답과 앱 전환이 매우 빨라 초기 loading state를 안정적으로 캡처하지 못했으므로 새 `MyPageLoadingPreview`가 해당 state의 재현 수단이다.

## Review

- 독립 검토 기준: `MyPageProfileCardLoadingContent`와 `RodiSkeleton`의 실제 Compose modifier를 확인했다. 현재 작업 트리에 함께 남아 있는 Home·저장 장소·운전 목표 변경은 `20260803-content-level-skeleton-detail.md`로 아카이브된 직전 작업의 보존분이며, 이번 보정의 판정 범위에는 포함하지 않았다.
- Acceptance 1 — 충족. outer card는 `white` 배경과 `primary50` border만 사용해 shimmer가 없고, 내부 placeholder만 `RodiSkeleton`의 `gray100` + shimmer를 사용한다. 따라서 기존 `primary20`(F4F4FF)과 `gray100`(F5F5F5)의 낮은 대비가 해소되어 개별 로딩 요소가 구별된다.
- Acceptance 2 — 충족. 227dp 높이, 16dp 가로·상단 여백, `primary50` border와 내부 placeholder의 크기·간격이 유지된다. loading top bar의 설정 아이콘에는 click handler가 없어 로딩 중 설정 이동이 비활성으로 보존된다.
- Acceptance 3 — 충족. 이번 작업의 변경 파일은 allowlist에 속하며 `MyPageLoadingPreview`가 추가되어 해당 상태를 재현할 수 있다. 독립 실행한 `./gradlew :feature:mypage:assembleDebug`와 `git diff --check`는 모두 성공했다. 실제 loading state는 응답 전환이 빨라 안정적인 캡처를 확보하지 못한 제약이 있으며, Preview가 현재 검토 가능한 재현 수단이다.
- Finding: 없음.

## Review Triage

## Revision Plan

## Revision Result

## Final Review

- 승인. 모든 Acceptance를 충족했고 blocking finding이 없다. white non-shimmer card chrome과 `gray100` shimmer 내부 요소의 분리, `primary50` border·227dp layout·settings 비활성 보존, loading Preview 및 모듈 빌드 증거를 확인했다.
