# HANDOFF — Current Task

Status: DONE
Task: 네트워크 로딩 화면 Skeleton 적용
Branch: codex/skeleton-shimmer-setup
Base: origin/develop (55e3654d) + 4e4a7f93
Risk: MEDIUM

## Context

공용 `compose-shimmer` 의존성이 준비된 상태에서, 네트워크 읽기 요청을 기다리는 화면의 중앙 원형 스피너를 레이아웃 형태를 보존하는 skeleton으로 교체한다. 디자인 원본은 제공되지 않았으므로 현재 화면의 실제 레이아웃·테마 토큰을 skeleton의 근거로 사용한다.

## Goal

마이페이지 프로필 카드와 그 외 주요 네트워크 읽기 화면에서 데이터가 도착하기 전에도 최종 콘텐츠 구조를 예측할 수 있게 한다.

## Spec

- `core:ui`에 `RodiSkeleton` 공용 Composable을 추가한다. `RodiTheme.colors`와 `compose-shimmer`만 사용하며 Preview를 제공한다.
- 마이페이지의 초기 `GetMyPageUseCase` 로딩은 TopBar, 프로필 카드, 저장한 장소 행의 skeleton을 표시한다. 기존 성공·오류·갱신 동작은 보존한다.
- 운전 목표 편집의 초기 `GetMyPageUseCase` 로딩은 제목·입력 영역 skeleton을 표시하고, 로딩 중 포커스를 요청하지 않는다.
- 저장한 장소의 첫 페이지 로딩은 TopBar 아래 저장 장소 행 skeleton을 표시한다. 다음 페이지 로딩의 하단 spinner는 유지한다.
- 홈의 빈 첫 검색 로딩은 바텀시트 내 장소 행 skeleton을 표시하고, 장소 상세 요청의 기존 `PlaceDetailLoading`은 상세 바텀시트 형태 skeleton으로 바꾼다.
- 지도 SDK 초기화, 경로 탐색, 로그인/저장/북마크 같은 명령형 요청의 진행 UI는 변경하지 않는다.

## Acceptance

- 지정한 네트워크 읽기 로딩 상태에서 중앙 원형 스피너 대신 화면별 skeleton이 렌더된다.
- loaded, empty, error, retry, pagination, back navigation과 클릭 동작의 기존 상태 전이는 유지된다.
- 색상은 `RodiTheme.colors`만 사용하고, 새 공용 Composable은 Preview를 가진다.
- 변경은 공용 skeleton·지정 화면·design assumptions·현재 HANDOFF로 제한되며 `./gradlew :core:ui:assembleDebug`, `./gradlew :feature:mypage:assembleDebug`, `./gradlew :feature:home:assembleDebug`, `./gradlew assembleDebug`, `./gradlew test`가 통과한다.

## Expected Files

- `core/ui/src/main/java/com/dororong/rodi/core/ui/components/RodiSkeleton.kt`
- `feature/mypage/src/main/java/com/dororong/rodi/feature/mypage/MyPageScreen.kt`
- `feature/mypage/src/main/java/com/dororong/rodi/feature/mypage/drivinggoal/DrivingGoalScreen.kt`
- `feature/mypage/src/main/java/com/dororong/rodi/feature/mypage/savedcourses/SavedCoursesScreen.kt`
- `feature/home/src/main/java/com/dororong/rodi/feature/home/HomeScreen.kt`
- `feature/home/src/main/java/com/dororong/rodi/feature/home/detail/components/PlaceDetailLoading.kt`
- `docs/DESIGN_ASSUMPTIONS.md`
- `docs/handoff/HANDOFF.md`

## Verification

- `./gradlew :core:ui:assembleDebug`
- `./gradlew :feature:mypage:assembleDebug`
- `./gradlew :feature:home:assembleDebug`
- `./gradlew assembleDebug`
- `./gradlew test`
- 실제 기기 또는 Compose Preview에서 초기 로딩·성공·오류·다음 페이지 로딩 상태를 확인한다.

## Out of Scope

- 지도 초기화·경로 탐색·로그인·저장·북마크 요청의 UI 변경
- 서버 API, ViewModel state 모델, navigation 변경
- 제공되지 않은 Figma 디자인을 추정한 색상·타이포·에셋 변경

## Implementation Result

- Changed files: `RodiSkeleton` 공용 shimmer surface, MyPage·DrivingGoal·SavedCourses·Home 목록·PlaceDetail의 초기 읽기 로딩 skeleton, design assumptions를 추가했다.
- 상태 보존: MyPage 오류/성공 분기와 SavedCourses 다음 페이지 spinner를 유지했고, DrivingGoal은 로딩 중 자동 포커스와 저장을 막는다. Home의 지도 초기화와 명령형 요청 UI는 변경하지 않았다.
- Verification: `./gradlew :core:ui:assembleDebug :feature:mypage:assembleDebug :feature:home:assembleDebug` 성공. `./gradlew assembleDebug test` 성공.
- Device check: emulator에서 debug APK 설치와 홈 화면 실행은 확인했다. 인증·응답이 즉시 완료되어 각 초기 네트워크 로딩 state를 기기에서 안정적으로 고정할 수는 없었으며, 해당 state는 Compose Preview와 코드 경로로 확인이 남아 있다.
- Open questions: 확정된 로딩 상태 Figma가 제공되면 `docs/DESIGN_ASSUMPTIONS.md`의 D-001을 대조해 밀도·모양을 조정한다.

## Review

- 독립 검토 기준: 최신 `origin/develop`(55e3654d), 현재 HEAD(4e4a7f93), staged/unstaged/untracked 최종 작업 트리. `git diff --check` PASS. `origin/develop...HEAD`의 catalog·`:core:ui` 의존성·archived handoff는 Base에 명시된 선행 shimmer 준비 작업(4e4a7f93)이며, 현재 작업 트리 변경은 Expected Files 여덟 개와 일치한다.
- Acceptance 1 — 미충족: Home 목록·상세, SavedCourses, DrivingGoal은 지정된 초기 읽기 로딩에서 `RodiSkeleton`을 렌더한다. 그러나 MyPage 초기 로딩은 `MyPageTopBar`의 실제 텍스트·설정 아이콘을 렌더해, Spec의 TopBar skeleton을 제공하지 않는다.
- Acceptance 2 — 미충족: `feature/mypage/src/main/java/com/dororong/rodi/feature/mypage/MyPageScreen.kt:75,111`이 초기 로딩 중 `MyPageTopBar(onSettingsClick)`를 노출한다. 기준 트리의 초기 상태는 상호작용 없는 전체 화면 spinner였으나, 변경 후에는 설정 아이콘을 눌러 화면을 이탈할 수 있어 클릭 상태 전이가 바뀐다. **P1 / 수정 필요:** 초기 MyPage 로딩에는 설정 callback을 연결하지 않은 non-interactive TopBar skeleton을 렌더하고, 기존 TopBar는 profile을 받은 성공 상태에만 유지한다.
- Acceptance 3 — 충족: `RodiSkeleton.kt:19-28`은 shimmer modifier와 `RodiTheme.colors.gray100`만 사용하며, `RodiTheme`으로 감싼 360dp Preview가 있다. `docs/DESIGN_ASSUMPTIONS.md`의 D-001은 디자인 원본 부재, 기존 레이아웃·토큰 기반 가정, Medium 신뢰도 및 Figma 대조 후속 조치를 명시한다.
- Acceptance 4 — 충족(검증 명령): 현재 최종 트리에서 `./gradlew :core:ui:assembleDebug :feature:mypage:assembleDebug :feature:home:assembleDebug`, `./gradlew assembleDebug`, `./gradlew test` 모두 BUILD SUCCESSFUL.
- 추가 검토: 확정된 loading-state 디자인 원본·안정적으로 고정한 기기 캡처는 없어 D-001의 시각 가정은 아직 Medium confidence다. 이는 현재 P1 수정 후 실제 디자인이 제공될 때 확인할 비차단 시각 QA 항목이다.

## Review Triage

- P1 수용: 초기 로딩에서 실제 `MyPageTopBar`의 설정 클릭을 노출한 것은 기준 상태와 달라진다. TopBar도 skeleton으로 표시한다.

## Revision Plan

- `MyPageScreen.kt`: `MyPageLoadingContent`에서 callback을 제거하고, 제목·설정 아이콘 위치를 재현한 non-interactive skeleton TopBar를 추가한다. `RodiSkeleton`만 사용하며, 성공 상태의 `MyPageTopBar`는 변경하지 않는다.
- Verification: `./gradlew :feature:mypage:assembleDebug`와 `./gradlew test`를 다시 실행하고, `git diff --check`로 최종 patch를 확인한다.

## Revision Result

- `MyPageLoadingContent`에서 실제 `MyPageTopBar`와 settings callback을 제거하고, 동일한 56dp 영역에 제목·설정 아이콘 위치를 나타내는 non-interactive skeleton을 배치했다.
- Verification: `./gradlew :feature:mypage:assembleDebug test` 성공, `git diff --check` 성공.

## Final Review

- 승인: P1 해결 확인. `MyPageLoadingContent`는 `RodiSkeleton` 두 개로 구성한 56dp non-interactive TopBar skeleton만 렌더하며, settings callback·`clickable`·실제 `MyPageTopBar`를 포함하지 않는다. 실제 TopBar와 설정 이동은 profile이 로드된 `MyPageContent`에서만 유지된다.
- Acceptance 1 — 충족: MyPage TopBar·프로필·저장 장소 영역을 포함해 지정된 초기 읽기 상태가 skeleton을 렌더하고, Home 목록·상세, SavedCourses, DrivingGoal의 skeleton 분기도 이전 Review 근거와 동일하게 유지된다.
- Acceptance 2 — 충족: 초기 MyPage 로딩 중 새 화면 이동 경로가 제거됐고, error/retry·pagination spinner·back handling·성공 상태의 클릭 동작은 수정 전 분기와 동일하다.
- Acceptance 3 — 충족: `RodiSkeleton`의 색상·Preview 및 D-001 디자인 가정 기록을 재확인했다.
- Acceptance 4 — 충족: 최종 `git diff --check` PASS. 변경 범위는 Expected Files와 일치하며, 수정본에서 `./gradlew :feature:mypage:assembleDebug`와 `./gradlew test`가 BUILD SUCCESSFUL이다. 앞선 독립 검토에서 `:core:ui:assembleDebug`, `:feature:home:assembleDebug`, `assembleDebug`도 성공했다.
