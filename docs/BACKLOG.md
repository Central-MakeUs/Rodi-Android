# BACKLOG.md — Rodi 후속/기술부채 (에이전트 공유)

> Codex는 Claude의 개인 메모리를 볼 수 없다. **두 에이전트가 공유해야 할 후속 항목은 여기에** 둔다.
> 한 줄씩 누적하고, 착수 시 `docs/handoff/HANDOFF.md`로 옮겨 작업한다.

## 열린 항목
- [ ] **후기 등록 성공 후 코스 상세 목록·요약에 노출되지 않음 (백엔드 확인 필요)** — `placeId 106`
  (영덕 해안도로 코스)에 `POST /places/{placeId}/reviews`가 200으로 성공한 뒤에도
  `GET /places/{placeId}/reviews/summary`·`?level=ALL`·`GET /places/{placeId}/reviews?size=1`이
  전부 200을 반환하지만 방금 만든 후기가 응답에 없다. 클라이언트 재조회 배선(`CourseReviewViewModel.refresh()`)은
  3라운드에 걸쳐 정상 동작을 확인했다 — `ReviewLevelFilter.Mine`이 `level` 쿼리를 생략하는데,
  서버가 이를 "내 레벨 코호트"로 해석하는지 "필터 없음"으로 해석하는지 확인 필요. 개발 서버
  `placeId 106`에 테스트 계정 후기 2건("행", "그드팥지")이 남아 정리 필요.
- [ ] **손수 만든 다이얼로그 3개를 `RodiAlertDialog`로 이관** — 후기 등록 플로우 작업에서
  `core/ui/components/dialog/RodiDialog.kt`(`RodiDialog` + `RodiAlertDialog`)를 새로 만들었다.
  같은 구조를 이미 복사해 쓰고 있는 `core/ui/.../AccountRecoveryDialog.kt`,
  `feature/home/.../reviewactions/ReviewReportScreen.kt`의 `BlockMemberDialog`·`ReportSubmittedDialog`를
  이관하고, 거기 있는 사설 `DialogButton`(116×42)을 제거한다. 당시엔 diff를 작게 유지하려고 미뤘다.
- [ ] **`Throwable.userMessage()` `core:common` 승격** — `HomeViewModel.kt:689`,
  `SearchViewModel.kt`, `MyPageViewModel.kt`에 같은 파일-private 확장이 복사돼 있다(3곳).
  `core:common`으로 올리고 복사본을 제거한다. 세 번째 복사본은 마이페이지가 역직렬화 예외
  원문을 화면에 그대로 노출하던 걸 막으면서 생겼다 — 규칙이 코드가 아니라 관습으로만 있으니
  같은 것이 계속 복제된다.

- [ ] **재가입 가능 시각(`rejoinableAt`) 서버 필드 요청됨 (백엔드 대기)** — 탈퇴 정책은
  유예 3일(복구 가능) → 이후 총 10일까지 재가입 불가 → 그 뒤 재가입 가능, 3구간이다.
  가운데 구간(탈퇴+3일 ~ +10일)에서 서버는 `MEMBER_409_1`을 주는데 본문이 `code`/`message`뿐이라
  **안내에 쓸 기준 날짜가 오지 않는다.** 그래서 MY-06-R "0월 0일 이후 재가입 가능해요." 다이얼로그를
  구현할 수 없다. 주의: `recoverableUntil`은 탈퇴+3일이라 이 문구에 쓰면 7일 어긋난다.
  서버가 `rejoinableAt`을 `200 WITHDRAWAL_PENDING`과 `MEMBER_409_1` 양쪽에 실어주면
  앱이 정책 상수를 하드코딩하지 않아도 된다(`ApiEnvelope`에 `data` 필드가 이미 있다).
  그때까지 이 구간은 디자인의 "재가입 가능 날짜를 불러오지 못했어요." 토스트 + 새로고침으로 폴백.
  요청은 넣어둔 상태(2026-08-12).
- [ ] **미방문 사유 제출 API 연동** — RV-01의 "안 했어요" → 미방문 사유 화면은 만들어뒀지만
  **서버에 제출 API가 없어 제출이 스텁**이다(`feature/home/.../review/notvisited/`).
  사유 5종도 서버 계약이 없어 클라이언트 enum(`NotVisitedReason`)으로 두었다.
  API가 나오면 enum을 `core:domain`으로 승격하고 UseCase를 배선한다.
- [ ] **연습 방문 감지를 서버/지오펜싱 기반으로 교체** — 현재 RV-01 트리거는 "내비 실행 시각을
  로컬에 저장(`PracticeSessionPreference`) → 앱 재진입 시 10분 경과 판정" 휴리스틱이다.
  내비를 띄우고 실제로는 안 갔거나, 앱을 아예 안 열면 감지되지 않는다.
  서버 방문 인증 API 또는 Geofencing+WorkManager가 준비되면 교체한다.
  (목록 API에 `isVerifiedVisit`가 생기면 후기 카드의 방문인증 칩도 함께.)
- [ ] **장소 상세 조회 실패 시 에러 피드백 부재** — `HomeViewModel.openPlace()`가 `getPlaceDetailUseCase` 실패 시 조용히 리스트/지도로 되돌아가므로 `HomeEffect.ShowSnackbar(error.userMessage())`를 보내도록 보강 필요.
- [ ] **보호 API 토큰 갱신 로직 중앙화(OkHttp Authenticator)** — `Authorization` 헤더가 필요한 보호
  API가 이미 다수인데 `NetworkModule`엔 `Authenticator`가 없고, `OnboardingRepositoryImpl`/
  `MemberRepositoryImpl`/`PlaceRepositoryImpl`(+`ReviewRepositoryImpl`) 각각이 401을 잡아
  `authRepository.reissueToken()`을 호출하는 `authenticatedRequest` 헬퍼를 **거의 동일하게 복사**해
  들고 있다. 남은 문제는 **중복뿐**이다.
  **refreshToken 재사용으로 전 세션이 폐기되는(`AUTH_401_4`) 레이스는 이미 막혀 있다** — 2026-08-08
  확인: `AuthRepositoryImpl`이 `@Singleton`이고 `reissueToken()`이 인스턴스 `refreshMutex`로 감싼 뒤
  "요청 시점 refreshToken ≠ 현재 저장된 refreshToken이면 재발급하지 않고 return"하는 single-flight
  가드를 이미 구현하고 있다. 따라서 이 항목은 데이터 손실 위험이 아니라 **리팩터링 우선순위**다.
  중앙화 시: `OkHttpClient`에 `Authenticator`를 추가하고 각 Repository의 중복 헬퍼를 제거한다.
  순환 의존(OkHttpClient → AuthApi → Retrofit → 같은 OkHttpClient) 방지를 위해 재발급 전용
  Retrofit/OkHttpClient 인스턴스를 따로 구성해야 하고, 위 single-flight 가드는 그대로 유지해야 한다.
- [ ] **`androidx.baselineprofile` Gradle 플러그인 stable로 교체** — stable(1.4.1)이 AGP 9.2.1을
  지원하지 않아 `1.5.0-alpha07`로 임시 고정(`feature/baseline-profile` 작업, `gradle/libs.versions.toml`의
  `baselineProfilePlugin`). 빌드 툴체인에만 영향(런타임 코드 무관)이지만 alpha 의존이므로 stable
  릴리스가 나오면 버전 교체.
- [ ] **닉네임 마이페이지 수정 기능** — 온보딩에서 서버가 배정한 닉네임(로그인 응답 `nickname`,
  게스트는 로컬 `NicknameGenerator` 폴백)은 이번 스코프에서 수정 UI가 없다(사용자 확인: "닉네임
  수정은 나중에 마이페이지에서"). 마이페이지 화면 작업 시 함께 고려.
- [ ] **`NicknameGenerator` 단어 리스트 PM 검수** — 형용사구/동물 각 10개씩 임시로 채워 넣었다
  (`core/common/.../NicknameGenerator.kt`). 로그인 계정은 서버 닉네임을 쓰므로 이 목록은 게스트
  전용 폴백에만 쓰인다. 실제 서비스에 쓸 최종 리스트는 PM 검수 필요.
- [ ] **`PracticeSituation`(온보딩 선호 상황) ↔ `PracticeTag`(Course 특징) 통합 검토** — 두 enum이
  라벨 상당수 겹치지만(유턴/좌우회전/주차/차선변경/교차로/회전교차로/고속진입/직선주행 등) 완전히
  같지 않아 이번엔 별도 enum으로 분리했다. 코스 추천 매칭 로직을 설계할 때 두 개념을 어떻게
  연결할지(혹은 통합할지) 재검토할 것.
- [ ] **Kotlin 2.2.10 → 2.4.0 / AGP 버전 업그레이드** — Google Maven 기준 Kotlin 최신 안정은 2.4.0,
  AGP는 현재 프로젝트(9.2.1)가 이미 공개 릴리스 노트보다 앞서 있음. 컴파일러 호환성(compose
  compiler, KSP 등) 검증이 필요해 Java 21 통일 작업(2026-07-01)에서 범위 밖으로 뺌.
- [ ] **Kakao Map/Navi SDK 버전 업그레이드 검토** — `kakaoMap`(2.11.9)/`kakaoSdk`(2.20.6) 최신 여부
  미확인. 지도·내비 핵심 기능 회귀 위험이 있어 별도 검증 후 진행.
- [ ] **Nav3 도입 + 하드코딩 축소** — Navigation 3(`androidx.navigation3`)로 전환하고
  `kotlinx.serialization`으로 라우트를 타입-세이프하게 정의해 문자열 하드코딩 제거.
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

## 마이페이지 개편 후속
- [x] **연습기록 조회 API 연동** — `GET /members/me/practices`를 마이페이지 섹션·전체보기 화면에 커서 페이징으로 연결했다.
- [x] **내 후기 목록 API 연동** — `GET /members/me/reviews`를 내 게시글 화면에 커서 페이징으로 연결했다.
- [x] **차단 목록 조회 API 연동** — `GET /members/me/blocks`를 차단목록 화면에 커서 페이징으로 연결했다.
- [x] **레벨 진행률(누적 주행거리) 필드 연동** — `MyPageResponse.levelProgress`를 프로필 카드 진행바와 거리 텍스트에 연결했다.
- [ ] **레벨업 감지 트리거 연결** — 레벨업 팝업 UI만 구현되어 호출부가 없다.
- [ ] **후기 "좋아요" 기능 유무 확인** — 후기 수정 안내 문구가 좋아요 초기화를 언급하지만 현재 앱에는 좋아요 기능이 없다.
- [ ] **설정 `데이터 출처` 항목 존치 여부** — 최신 디자인에는 빠졌으나 공공데이터 출처 표기 의무 가능성이 있어 유지했다.

## 완료 (이력)
- [x] **온보딩 서버 API 연동 + 점수 배점** — `OnboardingApi.submit()`이 `/members/me/onboarding`에
  실제 연동됐고(`OnboardingRepositoryImpl`), 요청 페이로드가 최신 서버 스펙(2026-08-08 확인,
  OpenAPI)과 필드·enum 값까지 정확히 일치함(`OnboardingMapper.toApiValue()` 전수 대조 완료).
  점수 계산도 `core:domain`의 `OnboardingProfile.calculateAssessment()`로 이미 구현되어 있고,
  스펙대로 점수는 서버에 보내지 않고 클라이언트가 변환한 `level`만 전송한다. 닉네임은 로그인
  응답의 `nickname`(서버 값)을 그대로 저장하고, `NicknameGenerator`는 게스트(로그인 없는 둘러보기)
  전용 로컬 폴백으로만 쓰여 원래 설계대로 동작 중.
- [x] **레거시 `OAuthOnboardingProfileRequest` 죽은 코드 제거** — 온보딩 데이터를 로그인 요청에
  같이 보내던 구 설계의 잔재(`AuthMapper.toOAuthRequest()` 포함, 호출부 없음)를 삭제.
  `fix/typography-and-onboarding-cleanup` 브랜치, 2026-08-08.
- [x] **Pretendard ExtraBold 폰트 적용** — ttf는 이미 확보돼 있었으나 `RodiTypography.price2`가
  여전히 `FontWeight.Bold`로 남아있던 것을 `FontWeight.ExtraBold`로 교체하고 `RodiFontFamily`에
  등록. `fix/typography-and-onboarding-cleanup` 브랜치, 2026-08-08.
- [x] **로그아웃 API(`POST /auth/logout`) 연동** — `LogoutUseCase`/`AuthRepositoryImpl`이
  `AccountSettingsViewModel`에 연결되어 동작 중.
- [x] **`isNewMember` 기반 온보딩 분기** — `LoginWithKakaoUseCase`가 `isNewMember`로 온보딩 진입
  여부를 분기하고 `LoginContract`/`LoginViewModel`/`LoginScreen`이 이를 소비.
- [x] **EntryRepository/NaviPreferenceRepository UseCase 래핑** (커밋 `290fdd4f`) —
  `EntryViewModel`/`HomeViewModel`이 Repository 대신 UseCase(`GetEntryProgressUseCase` 등)를 주입받음.
- [x] **커스텀 스낵바 도입 (PR #18)** — `core:ui/components/snackbar/`에 `RodiSnackbar`/
  `RodiSnackbarHost`/`RodiSnackbarHostState`/`RodiSnackbarData` 구현·병합 완료(`ArrayDeque` 큐잉,
  `RodiSnackbarDuration`, `AnimatedVisibility` 전환 포함). 참고 프로젝트의 `SnackbarPosition` enum·
  `toastSuccess()/toastError()` 헬퍼는 이식하지 않았음(필요해지면 별도 항목으로 재검토).
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
