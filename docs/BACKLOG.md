# BACKLOG.md — Rodi 후속/기술부채 (에이전트 공유)

> Codex는 Claude의 개인 메모리를 볼 수 없다. **두 에이전트가 공유해야 할 후속 항목은 여기에** 둔다.
> 한 줄씩 누적하고, 착수 시 `docs/handoff/HANDOFF.md`로 옮겨 작업한다.

## 열린 항목
- [ ] **Kotlin 2.2.10 → 2.4.0 / AGP 버전 업그레이드** — Google Maven 기준 Kotlin 최신 안정은 2.4.0,
  AGP는 현재 프로젝트(9.2.1)가 이미 공개 릴리스 노트보다 앞서 있음. 컴파일러 호환성(compose
  compiler, KSP 등) 검증이 필요해 Java 21 통일 작업(2026-07-01)에서 범위 밖으로 뺌.
- [ ] **Kakao Map/Navi SDK 버전 업그레이드 검토** — `kakaoMap`(2.11.9)/`kakaoSdk`(2.20.6) 최신 여부
  미확인. 지도·내비 핵심 기능 회귀 위험이 있어 별도 검증 후 진행.
- [ ] **테마 시스템 고도화 (부분 완료, 잔여 작업)** — `core:ui/theme/`에 `RodiSemanticColors`
  (pin/tag 등 일부 도메인 색상)와 `RodiDimens`(spacing/radius)까지는 도입됨. 참고 프로젝트
  (`/Users/uihyeon/StudioProjects/dnd-14th-2-android`의 `designsystem/theme/`) 대비 남은 격차:
  - `RodiSemanticColors`가 현재 pin/tag 일부만 커버 — 코스/주차장/경로 상태, 소셜 로그인
    브랜드색 등 화면 의미 단위 매핑으로 확장 여지
  - `RodiDimens`는 spacing/radius만 있음 — 버튼/입력필드/아이콘/앱바 등 컴포넌트별 수치까지
    중앙화할지 검토
  - `components/<종류>/model/` 하위에 Type/Size 등 sealed 모델 분리 패턴은 아직 미도입
  - 디자인시스템 Button 작업(`feat/design-system-buttons`)과 결과물 정합성 재확인 필요

## 완료 (이력)
- [x] Pretendard ExtraBold 폰트 파일 확보 — `core/ui/src/main/res/font/pretendard_extrabold.ttf`
  추가, `RodiFontFamily`에 `FontWeight.ExtraBold` 매핑, `price2`를 `FontWeight.ExtraBold`로 교체.
- [x] Nav3 도입 (PR #20) — `AppRoot` 최상위 라우팅을 Navigation 3(`NavDisplay`)로 교체
  (`app/.../ui/RodiApp.kt`).
- [x] EntryRepository/NaviPreferenceRepository UseCase 래핑 (PR #19) — `GetEntryCompletedUseCase`/
  `SetEntryCompletedUseCase`/`GetNaviAlwaysUseCase`/`SetNaviAlwaysUseCase` 추가, `EntryViewModel`/
  `HomeViewModel`이 Repository 대신 UseCase를 주입받도록 정리.
- [x] 커스텀 스낵바 도입 (PR #18) — `core:ui/components/snackbar/`에 `RodiSnackbar`/`RodiSnackbarHost`/
  `RodiSnackbarHostState`/`RodiSnackbarData` 추가. `ArrayDeque` 큐잉·`RodiSnackbarDuration` 분리는
  참고 프로젝트대로 반영했으나, Icon은 sealed interface 대신 `Painter?`로, 위치(`SnackbarPosition`)는
  별도 enum 없이 단순화해 구현.
- [x] Routi → Rodi 브랜드 식별자 정리 (PR #8)
- [x] 단위 테스트 + 테스트 자동화/CI 검증 (PR #17) — JUnit5 + MockK로 UseCase/ViewModel 핵심
  로직 테스트 작성, GitHub Actions에 테스트 게이트 추가. `docs/TESTING.md`에 컨벤션 정리.
- [x] 네트워크/로컬DB/DataStore 공통 뼈대 구축 (PR #16) — Retrofit/OkHttp + Room + 에러 매핑
  공용 규약(`DataError`/`NetworkResult`/`safeApiCall`) 추가. 실제 API/스키마는 아직 없음(뼈대만).
- [x] Repository 인터페이스 domain 이동 (PR #15) — `CourseRepository`가 `core:domain`으로 이동,
  Kakao `LatLng` 의존 없는 도메인 전용 `RouteResult`/`GeoPoint` 도입 완료.
- [x] 시스템 바 화면별 동적 컬러 (PR #9, `ec36a8d fix(entry): 약관 WebView 시스템 바 아이콘 동적 전환`) —
  앱 내 유일한 어두운 배경 화면인 `TermsWebView`가 진입 시 `WindowInsetsControllerCompat
  .isAppearanceLightStatusBars`/`.isAppearanceLightNavigationBars`를 `false`로 전환하고 이탈 시
  이전 값으로 복원. 나머지 화면은 전부 흰 배경이라 `MainActivity`의 기본 라이트 스타일이 맞음.
  이 항목이 "완료 처리" 커밋만 남긴 채 미머지 상태로 로컬에 방치돼 계속 노출됐던 것 —
  `feat/system-bar-dynamic-color` 브랜치/워크트리 삭제로 정리.
