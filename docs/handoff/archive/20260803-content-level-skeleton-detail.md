# HANDOFF — Current Task

Status: DONE
Task: 콘텐츠 구조 기반 Skeleton 세부화
Branch: codex/skeleton-shimmer-setup
Base: origin/develop (55e3654d) + 4934601a
Risk: MEDIUM

## Context

초기 skeleton은 읽기 요청의 빈 화면을 막았지만, 일부 화면이 최종 콘텐츠의 내부 정보 구조 대신 하나의 큰 placeholder로 표시된다. 디자인 원본은 없으므로 완성 화면의 Compose 레이아웃을 직접 근거로 세부 skeleton을 맞춘다.

## Goal

로딩 중인 데이터 영역만 skeleton으로 표시하고, 각 영역이 로드 후 카드·목록·상세 정보의 이미지·텍스트·태그·CTA 구조를 미리 전달하게 한다.

## Spec

- MyPage의 고정 TopBar는 실제 텍스트·아이콘 형태를 유지하되, 로딩 중 설정 이동은 계속 막는다. 프로필 카드는 캐릭터 이미지, 닉네임·레벨, 연습 유형 태그, 운전 목표 행을 독립 skeleton으로 표시한다.
- MyPage의 저장한 장소 행은 카운트 텍스트 영역만 skeleton으로 표시하고, 정적 chevron 형태와 행 높이는 유지한다.
- 운전 목표의 로딩 입력 영역은 입력 박스와 내부 텍스트·글자 수 위치를 따로 표시한다.
- 저장한 장소 목록과 홈 장소 목록은 제목·보조 정보·태그·설명 컨테이너를 실제 행 구조대로 표시한다.
- 장소 상세 로딩은 헤더 정보·보조 정보·태그·설명·하단 액션 영역을 실제 bottom sheet 구조에 맞춰 표시한다.
- 성공·오류·페이지네이션·back 동작 및 데이터·ViewModel 모델은 변경하지 않는다.

## Acceptance

- 이전에 추가한 모든 초기 읽기 skeleton이 단일 카드/행 block이 아닌 완성 콘텐츠의 주요 시각 요소로 나뉘어 렌더된다.
- 고정 chrome과 command UI를 skeleton으로 바꾸지 않으며, MyPage 로딩 중 settings 이동은 여전히 불가능하다.
- 색·타이포·아이콘은 기존 `RodiTheme`과 리소스만 사용하며, 대표 loading Preview가 유지 또는 추가된다.
- 변경은 아래 Expected Files와 design assumptions로 제한되고, `./gradlew :core:ui:assembleDebug`, `./gradlew :feature:mypage:assembleDebug`, `./gradlew :feature:home:assembleDebug`, `./gradlew test`가 통과한다.

## Expected Files

- `docs/DESIGN_ASSUMPTIONS.md`
- `docs/handoff/HANDOFF.md`
- `feature/mypage/src/main/java/com/dororong/rodi/feature/mypage/MyPageScreen.kt`
- `feature/mypage/src/main/java/com/dororong/rodi/feature/mypage/drivinggoal/DrivingGoalScreen.kt`
- `feature/mypage/src/main/java/com/dororong/rodi/feature/mypage/savedcourses/SavedCoursesScreen.kt`
- `feature/home/src/main/java/com/dororong/rodi/feature/home/HomeScreen.kt`
- `feature/home/src/main/java/com/dororong/rodi/feature/home/detail/components/PlaceDetailLoading.kt`

## Verification

- `./gradlew :core:ui:assembleDebug`
- `./gradlew :feature:mypage:assembleDebug`
- `./gradlew :feature:home:assembleDebug`
- `./gradlew test`
- Compose Preview와 emulator에서 MyPage·Home 진입 및 뒤로 이동을 확인한다. 네트워크 로딩 state는 Preview에서 확인한다.

## Out of Scope

- `RodiSkeleton` API, 디자인 토큰, 서버 API, ViewModel/Contract, navigation 변경
- 지도 초기화·경로 탐색·로그인·저장·북마크 요청 UI 변경
- 확정 디자인 원본 없이 새로운 색상·타이포·에셋을 도입하는 작업

## Implementation Result

- Changed files: MyPage 프로필 카드·저장 장소 행, 운전 목표 입력, 저장 장소 목록, Home 장소 목록·상세 skeleton을 콘텐츠 단위 placeholder로 세분화했고 D-002 가정을 기록했다.
- 상태 보존: MyPage의 고정 TopBar는 실제 형태이지만 callback을 연결하지 않아 로딩 중 settings 이동은 계속 불가능하다. 성공·오류·페이지네이션·back 분기는 변경하지 않았다.
- Verification: `./gradlew :core:ui:assembleDebug :feature:mypage:assembleDebug :feature:home:assembleDebug`, `./gradlew :app:assembleDebug`, `./gradlew test`, `git diff --check` 모두 성공했다.
- Visual QA: 에뮬레이터에 최신 debug APK를 설치하고 MyPage의 loaded 화면을 캡처해 ProfileCard의 실제 이미지·텍스트·태그·목표 행을 skeleton 대응 기준으로 대조했다. MyPage 진입 후 back으로 Home 복귀도 확인했다. 네트워크 응답이 즉시 완료되어 초기 loading state 자체의 기기 캡처는 재현하지 못했다.
- Open questions: loading-state Figma가 제공되면 D-001, D-002의 Medium/High 가정을 실제 프레임과 재대조한다.

## Review

- 독립 검토 기준: 최신 `origin/develop`(55e3654d), 현재 HEAD(4934601a), staged/unstaged/untracked 최종 작업 트리. `origin/develop...HEAD`에는 선행 shimmer·초기 skeleton 작업이 포함되며, 이번 작업 트리 diff는 Expected Files 일곱 개와 일치한다. `git diff --check` PASS.
- Acceptance 1 — 충족: MyPage 프로필은 캐릭터·닉네임/레벨·연습 태그·목표 행으로, DrivingGoal은 입력 테두리·입력 텍스트·글자 수로 세분화됐다. SavedCourses와 Home은 각각 실제 `SavedCourseRow`/course·parking `PlaceCard`의 제목·보조 정보·태그·설명 컨테이너 순서를 재현하며, PlaceDetail은 헤더·거리·태그·설명·하단 액션을 독립 placeholder로 표시한다.
- Acceptance 2 — 충족: `MyPageLoadingContent`의 TopBar는 기존 텍스트와 settings drawable만 렌더하고 `onSettingsClick` 또는 `clickable`을 포함하지 않아 로딩 중 설정 이동이 불가능하다. 성공·오류·페이지네이션·back·명령형 UI의 기존 분기와 ViewModel/Contract 변경은 없다.
- PlaceDetail 높이 검토 — 충족: 240dp 고정 컨테이너에서 24dp handle 뒤 weighted content의 유효 높이는 192dp다. 자식 높이는 header 24 + 10 + 18 + 8 + 20 + 8 + 37 + 10 + divider 1 + 8 + action 44 = 188dp이므로 4dp 여유가 남아 overflow하지 않는다.
- Acceptance 3 — 충족: 모든 추가 색·타이포·아이콘은 `RodiTheme`과 기존 drawable을 사용한다. loading Preview는 기존 `RodiSkeleton`, SavedCourses, PlaceDetail preview로 유지되며 D-002는 실제 Compose 레이아웃을 근거로 가정·신뢰도·후속 대조를 기록한다.
- Acceptance 4 — 충족: 현재 최종 트리에서 `./gradlew :core:ui:assembleDebug :feature:mypage:assembleDebug :feature:home:assembleDebug`와 `./gradlew test`가 모두 BUILD SUCCESSFUL.
- 발견사항: blocking/non-blocking finding 없음. 초기 loading state의 기기 캡처는 네트워크 응답이 즉시 완료되어 확보하지 못했으나, DESIGN_ASSUMPTIONS에 명시된 비차단 시각 QA 잔여 항목이다.

## Review Triage

## Revision Plan

## Revision Result

## Final Review

- 승인: 모든 Acceptance를 충족했고, 고정 chrome·상태 전이·변경 범위를 보존하면서 콘텐츠 구조 기반 skeleton 밀도를 높였다. 코드 구조·고정 높이 제약·최종 빌드 및 테스트 증거에 blocking finding이 없다.
