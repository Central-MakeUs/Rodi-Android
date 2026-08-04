# HANDOFF — Current Task

Status: DONE
Task: 검색 서버 로딩 스켈레톤 적용
Branch: feat/search
Base: origin/develop (1588af7b), merged at acda6eef
Risk: MEDIUM

## Context

- 검색 화면은 최근 검색어 조회 중 빈 영역을, 연관검색 조회 중 중앙 원형 스피너를 표시한다.
- 두 요청 모두 네트워크 응답을 기다리는 동안 결과 행의 구조를 미리 보여 줄 수 있다.
- 다음 페이지 장소 추가 조회는 이미 행 단위 원형 스피너를 사용하며, 기존 결과의 스크롤 흐름을 유지해야 한다.

## Goal

검색 화면의 초기 네트워크 로딩을 실제 결과 행 구조에 맞는 shimmer 스켈레톤으로 대체한다.

## Spec

- `core:ui`에 `RodiTheme.colors.gray100` 기반의 재사용 가능한 `RodiSkeleton`을 추가하고 compose-shimmer 의존성을 version catalog로 관리한다.
- 최근 검색어 최초 조회 중에는 최근 검색어 제목과 3개 행의 아이콘·텍스트·삭제 영역 스켈레톤을 표시한다.
- 입력어의 연관검색 최초 조회 중에는 지역/장소 결과 행과 동일한 높이의 아이콘·텍스트 스켈레톤 6개를 표시한다.
- 기존 검색 입력, 오류·빈 결과 화면, 최근 검색어 삭제 동작은 변경하지 않는다.
- 다음 페이지 장소 조회의 행 단위 원형 스피너는 유지한다.
- 시각 설계 가정은 `docs/DESIGN_ASSUMPTIONS.md`에 기록한다.

## Acceptance

- 최근 검색어와 연관검색이 로딩일 때 빈 화면 또는 중앙 스피너 대신 shimmer 스켈레톤이 표시된다.
- 스켈레톤 행은 실제 `SearchRow`와 같은 61dp 높이, 좌우 여백, 구분선을 사용한다.
- 다음 페이지 로딩은 기존과 동일하게 행 단위 원형 스피너를 표시한다.
- `:core:ui:assembleDebug`, `:feature:home:testDebugUnitTest`, `:app:assembleDebug`, `git diff --check`가 성공한다.

## Expected Files

- gradle/libs.versions.toml
- core/ui/build.gradle.kts
- core/ui/src/main/java/com/dororong/rodi/core/ui/components/RodiSkeleton.kt
- feature/home/src/main/java/com/dororong/rodi/feature/home/SearchScreen.kt
- docs/DESIGN_ASSUMPTIONS.md
- docs/handoff/HANDOFF.md

## Verification

- ./gradlew :core:ui:assembleDebug :feature:home:testDebugUnitTest :app:assembleDebug
- git diff --check
- 검색 화면에서 최근 검색어 및 입력 연관검색 로딩 상태의 Preview 또는 기기 확인

## Out of Scope

- 장소 상세·홈·마이페이지 등 검색 외 화면의 스켈레톤 적용
- 검색 API, ViewModel 상태 전이, 페이지네이션 정책 변경

## Implementation Result

- Changed files: `gradle/libs.versions.toml`, `core/ui/build.gradle.kts`, `core/ui/.../components/RodiSkeleton.kt`, `feature/home/.../SearchScreen.kt`, `docs/DESIGN_ASSUMPTIONS.md`.
- `compose-shimmer` 1.4.0과 `RodiSkeleton`을 추가하고, 최근 검색어는 헤더·삭제 영역·3개 행, 연관검색은 6개 행의 아이콘·텍스트·구분선 스켈레톤으로 교체했다.
- 다음 페이지 장소 조회는 기존 `SearchLoadingContent`를 그대로 사용한다.
- 영향을 받는 흐름: 빈 검색어의 최근 검색어 조회 → 입력 후 연관검색 조회 → 추가 페이지 장소 조회. 정방향과 역방향 상태 전이를 바꾸지 않고 UI 분기만 교체했다.
- 검증: `./gradlew --no-daemon :core:ui:assembleDebug :feature:home:testDebugUnitTest :app:assembleDebug --console=plain` BUILD SUCCESSFUL.
- 정적 검증: `git diff --check` GREEN.
- Open questions: 없음.

## Review

- 최신 `origin/develop`(`1588af7b`) 기준의 working tree diff를 확인했다. 변경은 Expected Files와 이전 검색 작업 HANDOFF의 아카이브 처리로 한정되며, `git diff --check`는 성공했다.
- Acceptance 1 — 충족. 빈 query의 `isRecentSearchesLoading` 분기는 `RecentSearchSkeletonList`로, 입력 query의 `SearchResultState.Loading` 분기는 `SearchSuggestionSkeletonList`로 전환된다. 각각 최근 검색어 header·삭제 영역·3개 행, 연관검색 6개 행의 아이콘·텍스트 skeleton을 표시한다. empty/error(`Empty`, `RegionEmpty`, `Idle`)와 검색 입력, 삭제 click 분기는 그대로다.
- Acceptance 2 — 충족. `SearchRowSkeleton`은 실제 `SearchRow`와 동일하게 61dp 높이, 16dp 수평 여백, 20dp 아이콘 영역, 12dp 요소 간격, `gray100` 구분선을 사용한다. `RodiSkeleton`은 `RodiTheme.colors.gray100`과 shimmer를 적용하며 core:ui Preview 및 최근/연관검색 loading Preview가 있다.
- Acceptance 3 — 충족. `SearchSuggestionList`의 `isNextPageLoading` 분기는 수정되지 않았고, 기존 `SearchLoadingContent`의 62dp 행 단위 `CircularProgressIndicator`를 계속 사용한다. ViewModel/API/페이지네이션 정책의 변경도 없다.
- 독립 검증: `./gradlew --no-daemon :core:ui:assembleDebug :feature:home:testDebugUnitTest :app:assembleDebug --console=plain` BUILD SUCCESSFUL, `git diff --check` 성공.
- Finding: 없음.

## Review Triage

## Revision Plan

## Revision Result

## Final Review

- 승인. 모든 Acceptance를 충족했고 blocking finding이 없다. 최근 검색어·연관검색의 초기 서버 로딩만 행 구조 shimmer로 교체되며, 다음 페이지 spinner와 기존 입력·빈/오류·삭제 흐름은 보존된다.
