# BACKLOG.md — Rodi 후속/기술부채 (에이전트 공유)

> Codex는 Claude의 개인 메모리를 볼 수 없다. **두 에이전트가 공유해야 할 후속 항목은 여기에** 둔다.
> 한 줄씩 누적하고, 착수 시 `docs/handoff/HANDOFF.md`로 옮겨 작업한다.

## 열린 항목

### 코스 상세 시트, 손가락으로 천천히 위로 드래그 시 버벅임 (2026-08-17 발견, 미해결)
- 증상: 코스 상세 바텀시트 핸들을 잡고 천천히 위로 올릴 때 프레임이 튐(빠르게 튕기거나
  뒤로가기로 접을 때는 정상). 손가락 추적 드래그 전환(#77) 이후 눈에 띄기 시작한 것으로 보임.
- `Modifier.sheetHeight()`가 `sheetState.offset`을 높이 제약으로 써서 매 프레임 서브트리를
  재측정하던 옛 버그(→ `3bafacc2`에서 이미 수정, offset은 현재 배치 단계에서만 적용)는
  아니라고 코드로 확인함 — `CourseDetailSheet.kt`에 `layout{}`/`Constraints` 사용 없음.
- 운전 추적 서비스가 백그라운드에서 종료되지 않고 계속 돌던 버그(#81)가 원인일 가능성이
  있어 먼저 고쳤으나, 실기기 재검증 전이라 실제로 해결됐는지 미확인.
- 다음 시도 시 필요한 것: 실기기에서 `adb shell dumpsys gfxinfo com.dororong.rodi reset` →
  느리게 드래그 재현 → `dumpsys gfxinfo com.dororong.rodi`로 janky frame 비율 확인. 코드
  추론만으로 원인을 단정하지 말 것(이미 한 번 잘못된 진단 — 이미 고쳐진 옛 코드를 보고
  같은 원인이라 재판단한 사례 있음).

### ★ 최우선 — UI 회귀 안전망 (2026-08-14 추가, 2026-08-24 1차 도입 완료)
> 배경: 이 리포는 단위 테스트 574개(2026-08-24 기준) 대비 계측(androidTest)이 사실상 없었고
> 스크린샷 테스트는 0개였다. 마커 겹침·시트 드래그 잼·리플 클리핑·드롭다운처럼 실제로 터지는
> 버그는 전부 **단위 테스트가 볼 수 없는 영역**이었다. `./gradlew test` 통과가 "검증됨"의
> 근거로 계속 오용됐고, 같은 QA 라운드에서 회귀가 반복됐다.

- [x] **Roborazzi 스크린샷 테스트 도입** — `test/ui-regression-safety-net` 브랜치에서 완료
  (2026-08-24). `core:ui`(`RodiButton`/`RodiSelectableChip`/`RodiSnackbar`)와
  `feature:home`(`LevelReviewSection` 빈 상태/요약)에 Roborazzi 1.68.0 + Robolectric 4.16.1로
  스크린샷 5장 커밋. AGP 9.2.1/Kotlin 2.2.10 호환성 확인 후 `./gradlew test` 전체 통과 유지한
  채로 도입 완료. `CourseDetailSheet`(접힘/펼침)는 이번엔 다루지 않음 — 후속으로 남김.
- [x] **`MockResponseRegistry`를 계측 테스트 픽스처로 승격** — `withMocks(responses, block)`
  suspend 헬퍼 추가 완료(2026-08-24, 상태 복원 포함). 아직 실제 androidTest에서 쓰인 곳은 없음 —
  진입점만 마련된 상태.
- [ ] **`CourseDetailSheet` 접힘/펼침 Roborazzi 스크린샷 추가** (2026-08-24 후속) — 위 1차
  도입에서 `core:ui`/`LevelReviewSection`만 다뤘고, 처음에 최우선으로 지목했던 코스 상세 시트
  자체는 아직 없다.
- [ ] **Compose UI Test — 제스처/드롭다운/리플 클리핑 커버리지** (2026-08-24 후속, 리뷰에서
  발견) — 1차 도입에서 `feature:auth`/`feature:entry`/`core:ui`에 추가한 androidTest 3개는
  전부 클릭/토글 기반 상태 전환 검증이다(`LoginContentTest`/`TermsAgreementContentTest`/
  `CoreUiComponentsTest` 참고 — HANDOFF는 로컬 전용이라 원문은 리뷰 시점 세션에만 있음).
  정작 이 백로그가 처음에 지목했던 실제 회귀 유형 — 드래그/스와이프, 드롭다운·팝업 열림-닫힘,
  리플 클리핑 — 은 스크린샷 diff와 이번 androidTest 어느 쪽으로도 아직 안 잡힌다. `feature:mypage`/
  `feature:settings`도 여전히 androidTest 0개.
- [ ] **`docs/TESTING.md`에 Roborazzi 예외 명시** (2026-08-24 후속, 리뷰에서 발견) — `TESTING.md`는
  `src/test`에 JUnit5만 쓰라고 명시하는데, 새로 추가한 Roborazzi 테스트는 Robolectric 생태계
  제약으로 JUnit4(`AndroidJUnit4` 러너)를 쓴다(`junit-vintage-engine`으로 JUnit5 플랫폼에 연결).
  불가피한 예외지만 문서에 왜 그런지 한 줄이 없어 다음에 헷갈릴 수 있다.
- [ ] **시트 드래그 잼 회귀 감시 (FrameTimingMetric)** — `:benchmark` 모듈에 Macrobenchmark와
  uiautomator가 이미 붙어 있으므로(`StartupBenchmark.kt` 참고) 테스트만 추가하면 된다.
  **선결 과제: 로그인 우회 수단이 없다.** 코스 상세까지 가려면 카카오 로그인 → 위치 → 목록
  선택을 거쳐야 해서 벤치마크가 안정적으로 화면에 도달하지 못한다. 디버그 빌드 전용 진입점
  (예: 특정 화면으로 바로 가는 deep link, 또는 테스트용 토큰 주입)이 먼저 필요하다 — 이 진입점
  자체가 보안·스펙 판단이 필요해 2026-08-24 배치에서도 그대로 남겼다.
  그때까지는 수동으로 `adb shell dumpsys gfxinfo com.dororong.rodi`의 janky frame 비율을
  수정 전/후 비교하는 방식으로 대체한다.

- [x] **후기 등록 성공 후 코스 상세 목록·요약에 노출되지 않음 (백엔드 확인 필요)** — `placeId 106`
  (영덕 해안도로 코스)에 `POST /places/{placeId}/reviews`가 200으로 성공한 뒤에도
  `GET /places/{placeId}/reviews/summary`·`?level=ALL`·`GET /places/{placeId}/reviews?size=1`이
  전부 200을 반환하지만 방금 만든 후기가 응답에 없다. 클라이언트 재조회 배선(`CourseReviewViewModel.refresh()`)은
  3라운드에 걸쳐 정상 동작을 확인했다 — `ReviewLevelFilter.Mine`이 `level` 쿼리를 생략하는데,
  서버가 이를 "내 레벨 코호트"로 해석하는지 "필터 없음"으로 해석하는지 확인 필요. 개발 서버
  `placeId 106`에 테스트 계정 후기 2건("행", "그드팥지")이 남아 정리 필요.

  **2026-08-12 기기 검증 — 백엔드 이슈가 맞다.** 한때 이 항목을 "클라이언트 타임스탬프 파싱
  버그"로 재진단했으나 틀렸다. 에뮬레이터에서 같은 계정으로 확인한 결과:
  - `GET /members/me/reviews` → 그 후기 2건이 **정상 렌더링**된다(내 게시글, 26.08.10 "ㄱㄷ팥ㅈ" /
    26.08.09 "행"). 즉 서버에 후기가 실재하고 클라이언트 파싱도 정상이다.
  - 같은 시점에 `/places/106/reviews*`는 200 + 0건 → 코스 상세는 빈 상태.
  - `HomeScreen`에 후기 조회 실패 스낵바를 붙여둔 상태에서 **스낵바가 뜨지 않았다.** 매퍼 예외가
    아니라 서버가 실제로 빈 응답을 준다는 뜻이다.

  타임스탬프 파싱 버그(`1193e8bf`)는 별개로 실재했고 고쳐졌다 — 그게 막고 있던 건 내 게시글·
  차단목록·연습기록이지 이 항목이 아니었다.
  **2026-08-12 재검증 — 원인을 찾았다. 서버 스키마 자체가 바뀌었고 클라이언트가 못 쫓아갔다.**
  코스 상세를 열 때 스낵바에 원문 예외가 그대로 떴다: `Field 'totalCount' is required for type
  with serial name '...ReviewSummaryResponse'`. Swagger를 다시 받아 대조하니
  `ReviewSummaryResponse`가 통째로 바뀌어 있었다 — `totalCount`가 없어지고
  `levelReviewCount`·`totalReviewCount`·`topDifficulty`(신규)로 갈렸다. 클라이언트 DTO
  (`core/data/.../model/review/ReviewResponses.kt`)는 옛 스키마 그대로라 역직렬화가 항상
  실패한다. `/places/106/reviews*`가 200에 0건처럼 보인 건 실제로 빈 응답이 아니라
  **파싱이 매번 터져서 조회 자체가 실패**했기 때문이다("빈 상태"와 "실패"가 UI에서 구분이
  안 됐을 뿐, 스낵바가 뜬 지금은 원인이 보인다).

  범위가 크다 — 새 필드 3개 반영, `ReviewSummaryResponse`/도메인 모델/매퍼/`CourseReviewViewModel`/
  UI(`topDifficulty` 노출 여부 등) 전부 손대야 해서 이번엔 고치지 않고 여기 남긴다. 최신 Swagger
  원문(`GET /places/{placeId}/reviews/summary` description): "난이도 분포와 최다 난이도
  (topDifficulty)는 **선택한 레벨** 기준, 추천/비추천 수는 **전체 레벨 합산**이라 모수가
  levelReviewCount·totalReviewCount로 나뉜다. 동률이면 더 어려운 난이도를 고르고, 후기가 없으면
  topDifficulty 키 자체가 빠진다."
  같은 else 분기 문제(`ReviewRepositoryImpl.toReviewException`가 `message ?: "..."`로 예외 원문을
  그대로 실어 보냄)도 `AuthErrorMapper`(`876f3142`)·`PracticeRepositoryImpl`과 같은 패턴이라
  이 작업과 함께 고치는 게 맞다.

  **2026-08-13 부분 해결.** 지난 QA 라운드에서 "totalCount 오류 토스트"를 크래시만 막고 넘어갔다가
  (기본값 0L만 채움), 이번에 Swagger를 다시 대조해 진짜 원인을 잡았다. `ReviewSummaryResponse`를
  `levelReviewCount`/`totalReviewCount`에 맞추고 도메인 `totalCount`를 `totalReviewCount`에서
  옮기도록 매퍼를 고쳤다 — 이제 파싱은 항상 성공하고 "전체보기" 링크도 실제 후기 수를 반영한다.
  **남은 범위**: `topDifficulty`(서버가 동률까지 계산해 내려주는 신규 필드)는 매핑하지 않았다 —
  클라이언트가 `difficultyCounts`로 이미 같은 규칙을 계산 중이라 당장 필요하지 않았다. `levelReviewCount`도
  아직 UI에서 안 쓴다. `ReviewRepositoryImpl.toReviewException`의 원문 노출(`else` 분기) 정리도 남아있다.
  `placeId 106`의 테스트 후기 2건 정리는 여전히 미확인.

  **남은 작업**
  - [ ] `topDifficulty` 서버 필드 매핑·노출 여부 결정
  - [ ] `levelReviewCount` UI 사용 여부 검토
  - [ ] `ReviewRepositoryImpl.toReviewException`의 예외 원문 fallback 제거
  - [ ] 관련 후기 테스트의 성공·실패·취소 경로 검토 및 정리
- [x] 주차장도 연습 목록에 담을지 기획 확인 필요 — 2026-08-13. Swagger 원문("코스·주차장 모두
  가능")을 재확인해 코스만 등록하던 클라이언트 분기를 제거했다(`HomeViewModel.launchPractice`).
- [ ] 순환 코스 마커 앵커 Y 값 재평가 — 출발·도착 겹침 조건에서 앵커 Y 기준을 동일 조건으로 비교하고, 검증 결과를 반영한다.
- [ ] **남은 Dialog/Sheet 프리뷰에 `LocalInspectionMode` 분기 적용 및 이름 없는 `@Preview`에 이름 부여**

- [x] **차단목록 빈 상태 문구 부재** — `BlockedMembersEmpty()`로 반영 완료(`b0ebd754`, QA
  라운드). Figma("차단한 사람 없을 때", node 3659:67282)와 문구·스타일 일치 확인(2026-08-13).

- [ ] **손수 만든 다이얼로그 3개를 `RodiAlertDialog`로 이관** — 후기 등록 플로우 작업에서
  `core/ui/components/dialog/RodiDialog.kt`(`RodiDialog` + `RodiAlertDialog`)를 새로 만들었다.
  같은 구조를 이미 복사해 쓰고 있는 `core/ui/.../AccountRecoveryDialog.kt`,
  `feature/home/.../reviewactions/ReviewReportScreen.kt`의 `BlockMemberDialog`·`ReportSubmittedDialog`를
  이관하고, 거기 있는 사설 `DialogButton`(116×42)을 제거한다. 당시엔 diff를 작게 유지하려고 미뤘다.
- [x] **`Throwable.userMessage()` `core:common` 승격** — `core/common/.../UserMessage.kt`로
  올리고 `HomeViewModel`·`SearchViewModel`의 동일 복사본을 제거했다. `ReviewWriteViewModel`은
  도메인 예외 분기가 있어 `reviewErrorMessage()`로 이름을 바꾸고 else만 공용 함수에 위임한다.
  `MyPageViewModel`의 `userMessage(fallback)`은 화면별 대체 문구를 받는 다른 계약이라 남겼다.
  원문 누출 차단은 `AuthErrorMapper`가 맡는다(`876f3142`) — 리포지토리가 모든 예외를
  `AuthException`으로 감싸므로 화면 단 필터로는 못 막는다.

- [ ] **재가입 가능 시각(`rejoinableAt`) 서버 필드 요청됨 (백엔드 대기)** — 탈퇴 정책은
  유예 3일(복구 가능) → 이후 총 10일까지 재가입 불가 → 그 뒤 재가입 가능, 3구간이다.
  가운데 구간(탈퇴+3일 ~ +10일)에서 서버는 `MEMBER_409_1`을 주는데 본문이 `code`/`message`뿐이라
  **안내에 쓸 기준 날짜가 오지 않는다.** 그래서 MY-06-R "0월 0일 이후 재가입 가능해요." 다이얼로그를
  구현할 수 없다. 주의: `recoverableUntil`은 탈퇴+3일이라 이 문구에 쓰면 7일 어긋난다.
  서버가 `rejoinableAt`을 `200 WITHDRAWAL_PENDING`과 `MEMBER_409_1` 양쪽에 실어주면
  앱이 정책 상수를 하드코딩하지 않아도 된다(`ApiEnvelope`에 `data` 필드가 이미 있다).
  그때까지 이 구간은 디자인의 "재가입 가능 날짜를 불러오지 못했어요." 토스트 + 새로고침으로 폴백.
  요청은 넣어둔 상태(2026-08-12).

  **2026-08-13 재확인 — 여전히 대기 중.** 현재 로그인 응답 Swagger엔 `rejoinableAt`이 없고
  `withdrawalRequestedAt`/`recoverableUntil`만 있다(`SocialLoginResponse.kt`). 로컬은
  `recoverableUntil`까지는 이미 받고 있지만 화면에 날짜를 표시하는 곳은 없다 — 연결 누락이
  아니라 애초에 서버 필드가 없어서 못 붙인 상태. 백엔드가 "`recoverableUntil`을 재가입 기준으로
  쓴다"고 확정하면 새 필드 없이도 바로 연결 가능하니, 필드 추가 대신 그 방향으로 정리될 수도 있다.
- [x] **미방문 사유 제출 API 연동** — 완료 확인(2026-08-13). `POST /practices/{practiceId}/skip-reason`이
  최신 Swagger에 있고 `PracticeApi.submitSkipReason` → `PracticeRepositoryImpl` →
  `SubmitSkipReasonUseCase` → `PracticeSkipReasonViewModel.submit()`까지 전부 실제 API를
  호출하도록 배선돼 있다(스텁 아님). 이 항목을 작성한 시점 이후 API가 나와서 바로 연동된 것으로
  보인다.
- [ ] **연습 방문 감지를 서버/지오펜싱 기반으로 교체** — 현재 RV-01 트리거는 "내비 실행 시각을
  로컬에 저장(`PracticeSessionPreference`) → 앱 재진입 시 10분 경과 판정" 휴리스틱이다.
  내비를 띄우고 실제로는 안 갔거나, 앱을 아예 안 열면 감지되지 않는다.
  서버 방문 인증 API 또는 Geofencing+WorkManager가 준비되면 교체한다.
  (목록 API에 `isVerifiedVisit`가 생기면 후기 카드의 방문인증 칩도 함께.)
- [x] **장소 상세 조회 실패 시 에러 피드백 부재** — 재확인(2026-08-14) 결과 이미 해결돼 있다.
  `HomeViewModel.openPlace()`의 `onFailure`가 `_effect.send(HomeEffect.ShowSnackbar(error.userMessage()))`를
  호출 중.
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
- [ ] **`CourseRepository`/`SampleCourses`/`GetCoursesUseCase` 죽은 코드 정리 필요 (2026-08-14 발견)** —
  `CourseRepositoryImpl.getCourses()`가 서버 대신 하드코딩된 `SampleCourses.RODI_COURSES`를
  그대로 반환한다. 하지만 `GetCoursesUseCase`를 호출하는 화면이 하나도 없다 — 실제 코스 목록은
  `PlaceApi` 기반 검색/상세 경로로 이미 대체됐고, 이쪽은 초기 PoC 잔재로 보인다. 릴리스 빌드에
  섞여 나가진 않지만(호출부가 없어 도달 불가) 죽은 코드라 헷갈릴 수 있다 — 완전히 제거하거나,
  아직 쓸 곳이 있다면 실제 API로 교체할 것.
- [ ] **`DrivingTrackingService` 시작/종료 명령 직렬화 (2026-08-16 CodeRabbit 발견)** — `ACTION_START`가
  비동기로 `startDrivingSession()`을 저장하는 도중 `ACTION_STOP`이 먼저 처리되면, 저장되지 않은
  세션을 `clear()`가 지우지 못하고 뒤늦게 완료된 `startDrivingSession()`이 이미 종료된 세션을
  ACTIVE로 남길 수 있다. `Mutex`나 단일 명령 처리 코루틴으로 시작·종료를 직렬화해야 함.
- [ ] **운전 도착 알림 탭 시 도착 흐름 미연결 (2026-08-16 CodeRabbit 발견)** — `DrivingNotificationFactory`가
  `ACTION_OPEN_ARRIVAL`을 붙인 PendingIntent를 만들지만 `MainActivity`/라우팅 어디서도 이 action을
  소비하지 않는다. 알림을 탭해도 도착 흐름으로 이동하지 않음 — 처리 경로를 연결하거나 미사용
  action을 제거할 것.
- [ ] **운전 알림 색상의 Compose 외부 테마 브릿지 검토 (2026-08-16 CodeRabbit 발견)** — `DrivingNotificationFactory`가
  `RodiTheme.colors`(CompositionLocal)를 쓸 수 없는 비-Compose 컨텍스트라 `LightRodiColors`를 직접
  참조 중. 다크 모드 알림 색상이 필요해지면 전용 브릿지(예: Application 시작 시 현재 테마를
  구독해 정적 필드에 반영)를 검토할 것.

## 마이페이지 개편 후속
- [x] **연습기록 조회 API 연동** — `GET /members/me/practices`를 마이페이지 섹션·전체보기 화면에 커서 페이징으로 연결했다.
- [x] **내 후기 목록 API 연동** — `GET /members/me/reviews`를 내 게시글 화면에 커서 페이징으로 연결했다.
- [x] **차단 목록 조회 API 연동** — `GET /members/me/blocks`를 차단목록 화면에 커서 페이징으로 연결했다.
- [x] **레벨 진행률(누적 주행거리) 필드 연동** — `MyPageResponse.levelProgress`를 프로필 카드 진행바와 거리 텍스트에 연결했다.
- [x] **레벨업 감지 트리거 연결** — 완료 확인(2026-08-13). `POST /practices/{practiceId}/visits`
  응답의 `levelUp`/`newLevel`을 `HomeViewModel.recordPracticeVisit()`이 `state.levelUp`으로
  넘기고, `HomeScreen.kt`가 이 값으로 `LevelUpDialog`를 띄운다. 실제 승급은 서버가 누적 거리
  기준으로 `levelUp: true`를 내려줄 때만 발생한다(GPS 인증 거리는 Phase A라 항상 생략).
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
