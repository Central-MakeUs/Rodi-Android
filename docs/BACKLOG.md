# BACKLOG.md — Rodi 후속/기술부채 (에이전트 공유)

> Codex는 Claude의 개인 메모리를 볼 수 없다. **두 에이전트가 공유해야 할 후속 항목은 여기에** 둔다.
> 한 줄씩 누적하고, 착수 시 `docs/handoff/HANDOFF.md`로 옮겨 작업한다.

## 열린 항목
- [ ] **Kotlin 2.2.10 → 2.4.0 / AGP 버전 업그레이드** — Google Maven 기준 Kotlin 최신 안정은 2.4.0,
  AGP는 현재 프로젝트(9.2.1)가 이미 공개 릴리스 노트보다 앞서 있음. 컴파일러 호환성(compose
  compiler, KSP 등) 검증이 필요해 Java 21 통일 작업(2026-07-01)에서 범위 밖으로 뺌.
- [ ] **Kakao Map/Navi SDK 버전 업그레이드 검토** — `kakaoMap`(2.11.9)/`kakaoSdk`(2.20.6) 최신 여부
  미확인. 지도·내비 핵심 기능 회귀 위험이 있어 별도 검증 후 진행.
- [ ] **Nav3 도입 + 하드코딩 축소** — Navigation 3(`androidx.navigation3`)로 전환하고
  `kotlinx.serialization`으로 라우트를 타입-세이프하게 정의해 문자열 하드코딩 제거.
- [ ] **EntryRepository/NaviPreferenceRepository UseCase 래핑** — `feat/domain-usecases`(PR #15)에서
  `CourseRepository`만 UseCase로 감쌌다. 같은 패턴으로 `EntryRepository`(isCompleted/setCompleted),
  `NaviPreferenceRepository`(getAlways/setAlways)도 UseCase로 감싸 `EntryViewModel`/`HomeViewModel`이
  Repository 대신 UseCase를 주입받도록 정리(원래 도메인 UseCase 작업의 Out of scope 항목).
- [ ] **커스텀 스낵바 도입 — PR #18 진행 중** — `/Users/uihyeon/StudioProjects/dnd-14th-2-android`의
  `designsystem/components/snackbar/`(PickleSnackbar/SnackbarHost/SnackbarState)를 참고해
  `core:ui`에 Rodi판 Snackbar를 만든다. 단순 이식이 아니라 Rodi 토큰으로 재설계하되, 다음
  요소들은 구조적으로 참고할 가치가 있음:
  - `ArrayDeque` 기반 큐잉으로 여러 스낵바 순차 표시 (`show()`/`showImmediately()`)
  - Icon 타입을 sealed interface로(Success/Error/None/Custom) — Material 아이콘 금지 컨벤션과
    맞물려 Figma 아이콘 리소스로 대체
  - 위치(`SnackbarPosition`: BelowStatusBar/BelowTopAppBar/AboveSystemNavigation/
    AboveBottomContents/Custom)와 지속시간(`SnackbarDuration`: TOAST_SHORT~SNACKBAR_INDEFINITE)을
    독립된 enum/sealed로 분리해 조합 가능하게
  - `AnimatedVisibility` + `AnimatedContent`(fade + slideInVertically/slideOutVertically, 300ms)
  - `toastSuccess()`/`toastError()` 같은 호출부 편의 헬퍼 함수
- [ ] **테마 시스템 고도화** — 마찬가지로 `dnd-14th-2-android`의 `designsystem/theme/`
  (Theme.kt/Color.kt/Typography.kt/Dimensions.kt)를 참고해 `RodiTheme`을 확장.
  **목표: 이 참고 프로젝트와 같거나 더 나은 완성도로, Rodi가 앞으로의 모든 프로젝트에서 기준이
  되는 디자인 시스템/테마 구조를 갖추는 것.** 참고 프로젝트 구조:
  - `PickleTheme.colors` / `.semantic` / `.typography` 3개의 `CompositionLocal`을
    `ReadOnlyComposable`로 노출하는 패턴 (색상 토큰과 "의미 있는" 색상 매핑을 분리)
  - `SemanticColors`: 카카오/구글 로그인 브랜드색, 도메인 상태색(예: guilty/innocent) 등
    화면 의미 단위로 색을 매핑 — Rodi라면 코스/주차장/경로 상태 등에 적용 가능
  - `Dimensions.kt` 단일 객체로 버튼/입력필드/아이콘/앱바/보더라디우스 등 수치 상수 중앙화
    (현재 `RodiTheme.spacing`/`radius`와 통합 또는 대체 검토)
  - 컴포넌트 네이밍은 프로젝트 프리픽스 통일(`Pickle*` → Rodi라면 `Rodi*`), `components/<종류>/model/`
    하위에 Type/Size 등 sealed 모델 분리
  - 디자인시스템 Button 작업(`feat/design-system-buttons`)과 결과물 정합성 확인.

## 완료 (이력)
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
