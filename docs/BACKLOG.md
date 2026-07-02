# BACKLOG.md — Rodi 후속/기술부채 (에이전트 공유)

> Codex는 Claude의 개인 메모리를 볼 수 없다. **두 에이전트가 공유해야 할 후속 항목은 여기에** 둔다.
> 한 줄씩 누적하고, 착수 시 `docs/handoff/HANDOFF.md`로 옮겨 작업한다.

## 열린 항목
- [ ] **Repository 인터페이스 domain 이동 검토** — 현재 `RouteResult`(Kakao `LatLng` 포함)와 `NaviApp`이
  `core:data` 타입이라 Hilt 도입 작업에서는 Repository 인터페이스/구현체를 함께 `core:data`에 둠.
  domain purity를 위해 vendor 타입 분리 후 `core:domain` 이동 여부를 별도 작업으로 검토.
- [ ] **Kotlin 2.2.10 → 2.4.0 / AGP 버전 업그레이드** — Google Maven 기준 Kotlin 최신 안정은 2.4.0,
  AGP는 현재 프로젝트(9.2.1)가 이미 공개 릴리스 노트보다 앞서 있음. 컴파일러 호환성(compose
  compiler, KSP 등) 검증이 필요해 Java 21 통일 작업(2026-07-01)에서 범위 밖으로 뺌.
- [ ] **Kakao Map/Navi SDK 버전 업그레이드 검토** — `kakaoMap`(2.11.9)/`kakaoSdk`(2.20.6) 최신 여부
  미확인. 지도·내비 핵심 기능 회귀 위험이 있어 별도 검증 후 진행.
- [ ] **시스템 바 화면별 동적 컬러** — 현재 `MainActivity`가 `SystemBarStyle.light`로 상·하단 아이콘을
  항상 검정 고정. entry/약관 등 **어두운 배경 화면에서는 흰 아이콘**이 필요. 공식·비deprecated 방식
  (`enableEdgeToEdge` + 화면별 `SystemBarStyle` 전환 또는 `WindowInsetsControllerCompat.isAppearanceLightStatusBars`)
  으로 화면에 따라 동적 전환. ← 파이프라인 첫 실작업 후보.

- [ ] **네트워크/로컬DB/DataStore 뼈대 구축** — `feat/hilt-di`(Repository 계층) 완료 후,
  실제 서버 연동 전 단계로 Retrofit/OkHttp(or Ktor) + Room + DataStore 공통 설정을
  `core:data`에 뼈대만 구축(모듈 의존성, 공통 클라이언트/DB 인스턴스, 에러 매핑). 실제 API/스키마는
  서버 연동 시점에 채운다.
- [ ] **단위 테스트 + 테스트 자동화/CI 검증** — 현재 기능(Home/Entry) 대상 단위 테스트 작성,
  앞으로 로직 작성 시 테스트코드를 자동 생성/검증하는 스킬 또는 컨벤션 마련, CI(GitHub Actions)에
  테스트 게이트 추가.
- [ ] **Nav3 도입 + 하드코딩 축소** — Navigation 3(`androidx.navigation3`)로 전환하고
  `kotlinx.serialization`으로 라우트를 타입-세이프하게 정의해 문자열 하드코딩 제거.
- [ ] **커스텀 스낵바 도입** — `/Users/uihyeon/StudioProjects/dnd-14th-2-android`의
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
