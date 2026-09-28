# BACKLOG.md — Rodi 후속/기술부채 (에이전트 공유)

> Codex는 Claude의 개인 메모리를 볼 수 없다. **두 에이전트가 공유해야 할 후속 항목은 여기에** 둔다.
> 한 줄씩 누적하고, 착수 시 `docs/handoff/HANDOFF.md`로 옮겨 작업한다.
>
> **운영 원칙 (2026-09-28 전수 감사 후)**: 선제적인 구조 정리는 여기서 끝낸다. 새 구조 작업은 기능 개발이나
> QA에서 구체적인 문제가 확인됐을 때만 만든다. 항목은 **누가 다음 행동을 해야 하는지**로 나눈다.
> 상태가 바뀌면 해당 절로 옮기고, 완료는 맨 아래 "완료 (이력)"에 한두 줄로 남긴다. 긴 경위는 git 이력에 있다.

## 개발 대기

### 테스트 공백 (2026-09-28 감사)
- [ ] **테스트가 없는 ViewModel** — `PracticeSkipReasonViewModel`(미방문 사유를 서버에 제출)과
  `PermissionSettingsViewModel`. 전자는 서버 쓰기가 있는 흐름이라 우선한다.
  재검증: ViewModel 이름으로 `*Test.kt`를 검색해 참조가 0건인 것을 찾는다.
- [ ] **이름보다 좁게 검증하던 테스트의 assertion 보강** — #185에서 이름을 실제 검증 범위로 좁힌 19개 중
  사용자 흐름에 닿는 것만 보강한다. 인증 헤더 주장 7건은 `AuthHeaderInterceptorTest`·`TokenAuthenticatorTest`가
  이미 검증하므로 대상이 아니다.
  - `HomeViewModelTest`: GPS로 확인되지 **않은** 도착이면 인정 거리를 보내지 않는다
  - `MyPostsViewModelTest`: 연습 기록이 **없으면** 연습 기록 버튼이 비활성이다
  - `EntryViewModelTest`: `setAllTermsChecked`가 약관 외 항목을 바꾸지 않는다(미리 false로 두고 확인)
  - `SearchViewModelTest`: 장소 추천은 최근 검색어 등록이 끝난 **뒤** 이동한다
  - `CourseRegistrationTutorialContentTest`: 연속 스와이프 뒤 이전 페이지로 되돌아가지 않는다(#180에서 수정 코드를 빼도 통과)

## 외부 대기 (서버·제조사)

- [ ] **실시간 업데이트가 Samsung 기기에서 안 뜬다** (2026-09-17 내부 테스트에서 발견) — 코드만으로는 해결되지 않는다.
  표시 조건은 셋이다: ① 알림 형태(코드상 충족), ② Android 16 이상, ③ 사용자·제조사 허용.
  **Samsung One UI 8은 서드파티 앱을 기본 차단하고 삼성 허용 목록에 있는 앱만 표시한다.**
  - 2026-09-18 실측(SM-M446K, One UI 8.0, API 36.0): 개발자 옵션 "모든 앱의 실시간 알림"을 켜면 표시된다.
  - `adb shell dumpsys notification --noredact`의 `AppSettings: <패키지> ... allowOngoingActivity=1`이 삼성의 앱별 허용값이다.
    카카오T·카카오내비·네이버지도는 1이고 Rodi는 값이 없다. `Notification Policies SCPM Version`으로 보아 서버에서 내려오는 목록이다.
  - `canPostPromotedNotifications()`는 이 기기에서 false라 판단 근거가 못 된다.
  - 삼성 전용 확장(`com.samsung.android.support.ongoing_activity` 메타데이터)도 허용 목록에 들어야 동작한다.
  - 승격되지 않는 기기에서도 일반 진행 알림으로 갱신되는 것은 유지한다.
  - 다음 행동: 삼성 제휴 신청 여부(기획 판단, 아래 "기획·디자인 대기")
  - 근거: developer.android.com/develop/ui/views/notifications/live-update
- [ ] **닉네임 수정** — `PATCH /members/me`는 `drivingGoal`만 받는다(2026-09-28 Swagger 확인).
  서버가 닉네임 수정을 지원하면 마이페이지에 붙인다.
- [ ] **개발 서버 `placeId 106`의 테스트 후기 2건 정리** — 2026-08 QA에서 테스트 계정으로 남긴 후기("행", "그드팥지").

## 기획·디자인 대기

- [ ] **권한 설정 화면 문구** (2026-09-16 실기기 확인) — "위치"와 "주행 상태 알림" 행이 허용/미허용만 보여주고,
  거부하면 무엇이 안 되는지 알려주지 않는다. "실시간 업데이트"만 설명이 있다. 세 행의 문구를 함께 다시 쓴다.
  배경: One UI 8에서 "실시간 업데이트"를 켤 시스템 스위치가 없어, 지금은 Android 16 이상에서 앱 내 토글을 노출한다.
- [ ] **Samsung 실시간 업데이트 제휴 신청 여부** — 위 "외부 대기" 항목의 선행 판단.
- [ ] **후기 요약 `topDifficulty`·`levelReviewCount` 노출 여부** — 서버가 두 필드를 내려준다.
  `topDifficulty`는 클라이언트가 `difficultyCounts`로 같은 규칙(동률이면 어려운 쪽)을 이미 계산하고,
  `levelReviewCount`는 UI에서 쓰지 않는다. 화면에 새로 보여줄 것이 있을 때만 연결한다.
- [ ] **`NicknameGenerator` 단어 목록 검수** — 형용사구·동물 각 10개가 임시 값이다(`core/common/.../NicknameGenerator.kt`).
  로그인 회원은 서버 닉네임을 쓰므로 게스트 폴백에만 쓰인다.
- [ ] **후기 "좋아요" 안내 문구** — 후기 수정 안내가 좋아요 초기화를 언급하지만 앱과 서버에 좋아요 기능이 없다.
  기능을 만들지, 문구를 뺄지 정한다.
- [ ] **설정 `데이터 출처` 항목 존치** — 최신 디자인에는 없지만 공공데이터 출처 표기 의무 가능성이 있어 유지 중이다.
- [ ] **크래시 모니터링 도입 여부** — 크래시 리포팅 SDK가 없다. 지금은 Clarity 세션 기록과 Play Console vitals뿐이다.

## QA 대기

- [ ] **순환 코스 마커 겹침** — 출발·도착이 겹치면 지금은 X 앵커(0.25/0.75)로 좌우로 벌린다
  (`feature/home/.../map/CourseRouteRenderer.kt`). 원래 항목의 "앵커 Y 재평가"는 재현 조건이 적혀 있지 않았다.
  순환 코스를 기기에서 열어 겹침이 여전히 문제인지 확인한 뒤 개발 항목으로 올리거나 닫는다.
- [ ] **재가입 대기(`WITHDRAWAL_LOCKED`) 다이얼로그 기기 확인** — #177에서 구현했지만 해당 상태의 계정이 없어
  기기에서 보지 못했다. 다이얼로그는 공용 팝업 규격이라 시안보다 높이가 약 40dp 작다.

## 보류

- [ ] **`RodiAlertDialog`로 수제 다이얼로그 이관** — `AccountRecoveryDialog`, `BlockMemberDialog`,
  `ReportSubmittedDialog`, 계정 설정 확인 다이얼로그가 같은 구조를 따로 구현하고 사설 `DialogButton`(116×42)을 쓴다.
  버튼 규격이 공용 팝업과 달라 시안 확인이 먼저다. 해당 화면을 고칠 일이 생길 때 함께 한다.
- [ ] **Baseline Profile 재생성** — 커밋된 `baseline-prof.txt`가 현재 코드와 크게 다르다(2026-09-28 에뮬레이터
  생성본과 비교해 약 2,600줄 추가·2,200줄 삭제). 다음 릴리스 전에 실기기로 다시 생성한다.
- [ ] **시트 드래그 잼 감시 (FrameTimingMetric)** — `:benchmark` 모듈은 이제 정상 동작한다(#184).
  하지만 코스 상세까지 가려면 카카오 로그인이 필요해 벤치마크가 화면에 도달하지 못한다.
  디버그 전용 진입점(deep link나 테스트 토큰 주입)이 먼저 필요하고, 그 자체가 보안 판단 대상이다.
  그때까지는 `adb shell dumpsys gfxinfo com.dororong.rodi`의 janky frame 비율을 수정 전후로 비교한다.
  주의: Compose UI Test는 드래그의 상태 전환만 검증하고 프레임 잼은 측정하지 않는다.
- [ ] **의존성 업그레이드 묶음** — 한 번에 하나씩, 별도 작업으로 공식 호환성과 실제 동작을 확인한다.
  - `androidx.baselineprofile` 플러그인: 지금 `1.5.0-alpha07`로 정상 동작한다. stable 여부와 AGP 9.2.1 호환을 먼저 확인한다.
  - Kotlin `2.2.10`: Compose 컴파일러·KSP 호환 검증이 필요하다.
  - Kakao Map `2.11.9`·Kakao SDK `2.20.6`: 지도·내비·로그인 회귀 QA가 필요하다.
  - 재검증: `gradle/libs.versions.toml`
- [ ] **`PracticeSituation`(온보딩 선호) ↔ `PracticeTag`(코스 특징) 통합** — 라벨이 상당수 겹치지만 같지 않다.
  코스 추천 매칭을 설계할 때 함께 정한다.
- [ ] **운전 알림 색의 다크 모드** — `DrivingNotificationFactory`는 Compose 밖이라 `LightRodiColors`를 직접 쓴다.
  앱에 다크 테마가 생기면 테마 브릿지를 검토한다.
- [ ] **`RodiAppViewModel`의 `ReissueAuthTokenUseCase(authRepository)` 직접 생성** — `conventions/structure.md`에
  적힌 legacy debt다. 같은 ViewModel의 `observeSessionExpiration()` 구독은 문서화된 제한 예외라 대상이 아니다.
- [ ] **이전 세션 workflow가 새 로그인 뒤 첫 요청의 세션을 고정할 가능성** — ADR 0002 작업에서 남긴 의문이다.
  재현 경로가 확인되지 않았다. 증상이 보고되면 다시 본다.

## 코드 관용구 정합성 (보류)

> 규칙과 코드가 어긋난 채 남은 지점이다. `check-conventions.sh`의 WARN이 이 절을 가리킨다.

- [ ] **`app`이 Compose BOM을 직접 선언** (check-conventions WARN) — `app`은 `AndroidApplicationConventionPlugin`을
  쓰지만(#182) 이 플러그인은 Compose 의존성을 넣지 않는다. application 플러그인에 Compose BOM과 JUnit Platform까지
  맡길지는 플러그인 책임을 넓히는 결정이라 하지 않았다. 지우면 안 된다 — app의 BOM은 중복이 아니라 별도 공급 경로다
  (`conventions/gradle.md`).

## 위험 수용

- [x] **이전 계정의 보류 온보딩 답변이 새 계정으로 제출될 수 있음** (2026-09-25) —
  A의 온보딩 제출이 실패해 보류로 남은 상태에서 A의 세션이 만료되거나 로그아웃 정리가 실패하고,
  같은 기기에서 게스트 이력 없는 새 회원 B가 가입하면 `LoginWithKakaoUseCase`가 A의 답변을 B 계정에 제출한다
  (클라이언트 경로 재현, 서버 수락 여부는 미확인).
  수용 이유: 세 조건이 동시에 겹쳐야 하고 기기 공유가 드물다. 클라이언트는 회원 식별자가 없어
  "같은 사용자의 만료 후 재로그인"과 다른 계정을 구분하지 못한다.
  다시 볼 때: 새 회원 로그인에서 보류 초안을 버리는 클라이언트 완화책이 가장 작다. 먼저 Entry 온보딩이
  새 회원에게 설문을 다시 받는지 확인한다.
- [x] **로그아웃 중 연습 세션 정리가 실패하면 운전 추적이 남음** (2026-09-25) —
  `AuthSessionCoordinator`의 연습 세션 정리는 best-effort라, 로그아웃 순간 DataStore 쓰기가 실패하면 추적과 알림이 남는다.
  수용 이유: 드물고, 남더라도 알림의 "운전 종료"·도착·알림 끄기로 서비스가 스스로 끝난다(`START_NOT_STICKY`).
  다시 볼 때: 로그아웃 뒤 추적 알림이 남는 제보나 로그가 실제로 확인되면.

## 폐기

- **테마 시스템 고도화** (2026-09-28) — `RodiTheme`이 이미 colors·semantic·typography·dimens를 갖췄다.
  다른 프로젝트의 테마 구조를 따라가는 목표라 현재 방향과 맞지 않는다.
- **후기 테스트의 성공·실패·취소 경로 검토** (2026-09-28) — 범위가 정해지지 않은 항목이다.
  구체적인 누락은 "테스트 공백"에 따로 적는다.
- **이름 없는 `@Preview`에 이름 부여** (2026-09-28) — 요구하는 규칙이 없다(`conventions/preview.md`).
- **Compose UI Test 제스처·드롭다운·리플 커버리지** (2026-09-28) — 드롭다운(`RodiPopupMenuTest`)과
  드래그(`CourseDetailSheetInteractionTest`)는 커버됐다. 리플은 Robolectric이 `RippleDrawable`을 그리지 않아
  semantics 테스트로 검증할 수 없고(#181에서 확인), 기기 확인 대상이다.
  마이페이지·설정 화면의 UI 테스트 부재는 구체적인 회귀가 생길 때 그 화면에 추가한다.

## 완료 (이력)

### 2026-09-28 전수 감사 사이클
- [x] Compose Foundation을 `libs.bundles.compose`로 옮겨 `:core:ui` 단일 출처(ADR 0001)에 맞춤 (#186)
- [x] 차단·계정 확인 다이얼로그와 `RodiPopupMenu`에 `LocalInspectionMode` 분기 (#187) — 프리뷰가 실제 창을 띄우던 곳만 고쳤다.
  `ReportSubmittedDialog`, `NaviPickerSheet`, `FilterBottomSheet`는 분기가 없지만 프리뷰가 내용 Composable을 직접 그린다.
- [x] 벤치마크 앱·테스트 APK 빌드를 PR CI에서 확인 (#188)
- [x] 테스트 함수명 774개를 한국어 설명형으로 통일하고 `conventions/testing.md`에 규칙 기록 (#185)
- [x] 벤치마크 실행 variant의 Clarity·서명과 벤치마크 대상 패키지 수정, 쓰지 않는 `benchmark` 빌드 타입 제거 (#183, #184)
- [x] `app`에 `AndroidApplicationConventionPlugin` 적용 (#182)
- [x] 필터 시트·계정 확인 다이얼로그 버튼의 리플이 모서리 밖으로 번지던 문제 (#181)
- [x] `CourseRegistrationTutorialContentTest`를 Robolectric으로 옮겨 CI에서 실행 (#180)
- [x] 삭제된 코스를 저장 목록에서 숨기고 상세 실패를 "삭제된 코스예요."로 표시 (#179)
- [x] 재가입 대기 계정(`WITHDRAWAL_LOCKED` + `reRegisterableAt`)에 재가입 가능 날짜 안내 (#177) — 서버는 요청한
  `rejoinableAt` 대신 이 형태로 내려준다. 날짜는 서버 값만 쓰고 정책 날짜를 재계산하지 않는다.
- [x] 로그인 후 온보딩 분기를 `isNewMember`가 아니라 서버 `isOnboarded`로 판단 (#176)
- [x] 서버가 보내지 않는 응답 필드 제거(`isVerified`×2, `congestionCounts`, `regionKey`) (#175)
- [x] 응답 DTO 기본값이 누락 필드를 가리던 문제 — 필수 필드는 파싱 실패로 처리 (#174, `0d7b42bd`).
  남은 기본값은 Kakao 외부 응답의 선택 필드와 Request 1건이다.
  재검증: `rg -n ': String = ""' -g '*Response*.kt' core/data/src/main`

### 인증·세션
- [x] 보호 API 인증 중앙화와 session 경합 방어 — `AuthHeaderInterceptor`/`TokenAuthenticator`를 인증 전용 client에 연결,
  새 login/logout 이후 stale refresh commit과 old-request retry 방어 (#166). 남은 `authenticatedRequest` helper는
  로그인 확인·오류 mapping 용도다. `audits/2026-09-17-auth-header.md`는 중앙화 이전 스냅샷이다.
- [x] 로그아웃·탈퇴·삭제의 로컬 정리를 `AuthSessionCoordinator`의 세션 소유권 확인 commit으로 모음 (ADR 0002)
- [x] 운전 추적 종료를 화면 대신 연습 세션이 소유 (#170)
- [x] 로그아웃 API(`POST /auth/logout`) 연동

### 코드 관용구 정합성 (2026-09-06 조사, 2026-09-18까지 정리)
- [x] 상태 property를 `_uiState`/`uiState`로 통일 (check-conventions BLOCK)
- [x] Effect 전달·소비를 Channel + `effect` + `CollectEffect`로 통일 (BLOCK). 신규 output 정책은 `conventions/mvi.md`
- [x] Intent 자식 이름을 이벤트형으로 통일 (BLOCK)
- [x] Contract를 화면마다 ViewModel 옆 `XxxContract.kt`로 이동 (BLOCK)
- [x] 컴포넌트의 색 리터럴 4건을 `semantic` 토큰으로 이관 (BLOCK)
- [x] 다른 파일에 내장된 ViewModel 2건 분리, `SearchScreen` 패키지 이동, `component`→`components`, 테스트 파일당 클래스 하나
- [x] `ReviewWriteViewModel`이 취소를 실패로 표시하던 문제 (2026-09-07)
- [x] ViewModel의 예외 원문 노출 차단 — 공통 `userMessage(fallback)`으로 변환 (2026-09-07)
- [x] DTO enum의 알 수 없는 값을 임의 정상값으로 대체하던 2건 제거 (2026-09-18)
- [x] `useJUnitPlatform()`을 library convention으로 이동, `app` JVM 테스트 JUnit5 이전 (2026-09-17)
- [x] 죽은 코드 제거: `safeApiCall`/`NetworkResult`/`DataError`, `CourseRepository`의 샘플 경로와 `SampleCourses`(2026-09-15)

### 테스트·CI
- [x] Roborazzi 스크린샷 테스트 도입(2026-08-24)과 `verifyRoborazziDebug` CI 게이트(2026-09-15).
  `CourseDetailSheet` 접힘·저장됨·펼침 스크린샷 포함
- [x] Kover 커버리지 리포트(2026-09-15, 임계값 없음). 첫 측정은 `audits/2026-09-15-coverage.md`
- [x] androidTest Compose UI 테스트를 Robolectric(`src/test`)으로 이전해 CI에서 실행 (2026-09-15)
- [x] `MockResponseRegistry.withMocks` 계측 테스트 픽스처 (2026-08-24)
- [x] GitHub Actions 외부 액션을 커밋 SHA로 고정하고 `ci.yml`에 `permissions: contents: read` (2026-09-17).
  자동 갱신은 붙이지 않았다 — 액션을 올릴 때 태그가 가리키는 SHA를 다시 확인한다.
  재검증: `rg -n 'uses: [^@]+@v[0-9]' .github/workflows`

### 기능·버그
- [x] 홈 필터 저장 중 시트가 화면에서만 사라지던 버그 (2026-09-17, `FilterBottomSheetDismissTest`).
  주의: `ModalBottomSheetProperties(shouldDismissOnBackPress = !isSaving)`로 막으면 안 된다. Material3 1.4.0은
  창을 만들 때의 값으로만 뒤로가기 콜백을 등록해, 저장이 끝난 뒤에도 뒤로가기로 닫히지 않는다.
- [x] 코스 상세 시트 드래그 버벅임 — 운전 추적 서비스가 계속 돌던 버그(#81) 수정 후 재현되지 않음 (2026-09-01)
- [x] 연습 방문 감지 교체 — 내비 실행 시각 휴리스틱(`PracticeSessionPreference`)을 연습 추적 API(#75)와
  운전 추적 서비스의 GPS 도착 판정(`RadiusArrivalPolicy`)으로 교체
- [x] 후기 요약 스키마 변경 대응(`totalCount` → `levelReviewCount`/`totalReviewCount`, 2026-08-13)과
  `ReviewRepositoryImpl` 예외 원문 fallback 제거 (2026-09-01)
- [x] `DrivingTrackingService` 시작/종료 명령 직렬화, 도착 알림 탭 라우팅 (#113)
- [x] 미방문 사유 제출 API, 연습기록·내 후기·차단 목록 API, 레벨 진행률, 레벨업 다이얼로그 연동 (2026-08)
- [x] 주차장도 연습 목록에 담기 (2026-08-13), 장소 상세 조회 실패 스낵바, 차단목록 빈 상태
- [x] `Throwable.userMessage()`를 `core:common`으로 승격
- [x] Nav3 도입 — app은 Navigation3 `NavDisplay`와 typed route를 쓴다

### 초기 구축 (2026-07 ~ 08)
- [x] 코스 등록 ViewModel이 Repository 대신 UseCase를 거치도록 연결 (`conventions/structure.md`, 2026-09-15)
- [x] 온보딩 서버 API 연동과 점수 배점, 레거시 `OAuthOnboardingProfileRequest` 제거 (2026-08-08)
- [x] Pretendard ExtraBold 적용, 커스텀 스낵바(PR #18), 시스템 바 화면별 동적 컬러(PR #9)
- [x] EntryRepository/NaviPreferenceRepository UseCase 래핑, Repository 인터페이스 domain 이동(PR #15)
- [x] Routi → Rodi 브랜드 식별자 정리(PR #8), 단위 테스트와 CI 게이트(PR #17), 네트워크·DB 공통 뼈대(PR #16)
