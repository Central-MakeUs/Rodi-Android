# HANDOFF — Rodi 1.1.0-alpha04 출시 필수 수정

Status: IMPL_DONE
Branch: release/1.1.0-alpha04
PR: #48 (`develop` ← `release/1.1.0-alpha04`)

## Context

alpha04는 홈 지도 검색·클러스터·초기 위치, 온보딩 정책과 인증 복구, 마이페이지·저장 장소 API, 상세 시트 동작 및 릴리스 워크플로를 출시 가능한 상태로 통합한다. 서버와 합의된 7개 운전 기간 enum을 앱 선택지·점수·전송 계약에 반영했으며 실제 서버 배포 확인만 출시 게이트로 남아 있다.

## Files

- 앱 진입·인증: `app`, `feature/auth`, `feature/entry`
- 지도·장소 상세: `feature/home`, `core/data` 장소 API·캐시
- 마이페이지·저장 장소: `feature/mypage`, `core/domain`, `core/data`
- 출시·검증 기록: `.github/workflows/release.yml`, `docs/PROJECT.md`, `docs/design/visual-qa.md`, `docs/handoff/HANDOFF.md`

## Acceptance

- 인증·온보딩·마이페이지·장소 API와 화면이 연결되고 인증 API의 재발급·취소 계약이 유지된다.
- 사용자 지도 제스처와 프로그램 카메라 이동을 구분하며, 서버 고유 장소 수와 클러스터 수가 일치한다.
- 상세·empty 시트의 하강 동작과 선택 해제가 스펙대로 동작한다.
- 앱 버전은 `versionCode=5`, `versionName=1.1.0-alpha04`이며 최종 결합 트리 빌드가 통과한다.
- 앱과 서버가 합의된 7개 `drivingPeriod` wire enum을 동일하게 사용하며 실제 서버 배포를 확인한 뒤 출시한다.

## Verification

- 도메인·데이터·feature 단위 테스트와 `lint`, `assembleDebug`, `assembleRelease`, `bundleRelease`를 최종 결합 트리에서 실행한다.
- 실기기에서 지도 제스처·클러스터·상세 시트·온보딩·계정 복구·마이페이지와 저장 장소를 확인한다.
- 주차장 요금 안내는 Preview 구조 대조와 별도로 실제 기기 캡처 상태를 기록한다.

## Out of scope

- 구형 운전 기간 값을 새 wire value로 추정 변환하는 호환 로직
- 확정되지 않은 Navigator 이미지 제작
- 이번 PR과 무관한 기존 worktree·stash 정리

## Claude Review

### Blocking

- 클라이언트 구현 차단 사항은 없다. 서버 배포본과 OpenAPI가 합의된 7개 enum을 제공하는지 출시 전에 확인한다.

### Nits

- 주차장 요금 안내의 Preview 구조 대조는 완료했지만 실제 기기 상세 캡처는 남아 있다.
- 위치 지연·거절, 온보딩 Figma 대조, 계정 복구와 시트 제스처의 최종 실기기 QA가 남아 있다.

### Verdict

- 클라이언트 구현과 자동 검증은 완료됐으며 서버 배포 확인과 남은 실기기 QA가 충족되면 출시 가능하다.

## Spec

- 홈 지도는 사용자의 미세한 이동·확대·축소에도 재검색 버튼을 표시하고, 서버 검색 성공 전까지 유지한다.
- 축척은 현위치 버튼 왼쪽에 배치한다. 초기 위치 확정 전에는 기본 서울 좌표의 장소를 노출하지 않는다.
- 지도 좌표·클러스터·장소 상세은 서버 데이터만 사용하며 기존 샘플 캐시를 제거한다.
- 온보딩 분석 문장과 레벨별 추천은 최신 Notion 정책으로 단일화한다.
- 로그인 응답의 서버 닉네임과 탈퇴 유예 계정 복구 흐름을 연결한다.
- 마이페이지 조회·운전 목표 수정·저장한 장소 목록 API를 실제 화면에 연결한다.
- 기존 empty 시트 하강 동작을 보존하고 버전을 `1.1.0-alpha04`/code 5로 변경한다.
- 운전 기간은 서버와 합의된 `UNDER_1_MONTH`, `MONTHS_1_2`, `MONTHS_3_5`, `MONTHS_6_11`, `YEARS_1_2`, `YEARS_3_9`, `OVER_10_YEARS`만 사용한다.

## Alpha04 implementation

- 지도 제스처가 시작되면 변화량과 무관하게 재검색 상태를 유지하고, 사용자가 누른 재검색의 서버 갱신이 성공한 경우에만 해제한다. 현위치·초기 위치·클러스터 이동은 이 상태를 만들지 않는다.
- 축척을 `BOTTOM | RIGHT`, 우측 60dp에 배치했다. 위치 준비 전에는 서울 기본 좌표로 검색하거나 마커를 노출하지 않고, 위치 실패 후 사용자가 직접 이동해 재검색하는 흐름은 유지했다.
- 샘플 장소 코드와 테스트를 제거하고 기존 음수 ID 캐시를 삭제한다. 좌표 캐시는 고유 서버 ID 응답으로 트랜잭션 교체하며 클러스터 수 역시 고유 장소 ID를 센다.
- 온보딩 계산 결과를 `score + level`로 모델링하고, 점수 대신 계산된 `level`만 전송한다. 분석 문장과 온보딩·마이페이지 추천은 공통 도메인 정책으로 통합했다.
- 로그인 응답의 상태·토큰·신규 회원 여부·서버 닉네임을 보존하고, 빈 닉네임을 오류로 처리한다. 탈퇴 유예 복구 확인·취소·실패 및 복구 후 기존 회원 이동을 연결했다.
- 둘러보기 온보딩 완료본은 `pending`으로 보존하되 신규 회원 로그인이 확인된 뒤에만 서버 전송을 허용한다. 로그인 직후 전송 실패는 로그인 성공을 취소하지 않으며 앱 시작·포그라운드 복귀 때 재시도한다. 기존 회원 로그인에는 둘러보기 답변을 덮어쓰지 않는다.
- 클러스터는 고정 전국 격자 대신 SDK viewport와 지도 패딩으로 계산한 실제 노출 영역 안의 고유 서버 장소 ID만 한 번씩 포함한다. 56dp 반경의 화면 좌표 클러스터가 연쇄적으로 과대 확장되지 않게 제한했고, 선택 시 부모의 전체 ID 집합을 유지한 채 모든 좌표가 들어오는 bounds로 이동한다.
- 마이페이지 조회와 목표 부분 수정, 코스·주차장 통합 저장 장소 커서 목록을 화면에 연결했다. nullable 계약, 첫 페이지 총 개수, 타입·ID 중복 제거와 페이지별 재시도를 반영했다.
- empty/error 시트의 아래 방향 숨김 동작을 보존했고 `versionCode=5`, `versionName=1.1.0-alpha04`를 적용했다.
- 릴리스 워크플로를 Java 21로 맞추고 키스토어·태그 버전·APK/AAB 서명 검증, 테스트·lint, 수동 실행 산출물 업로드를 추가했다.

## API audit (2026-07-23)

- 서버 OpenAPI의 인증·회원·온보딩·장소 11개 경로를 Retrofit 선언과 대조했으며 미연결 엔드포인트는 없다.
- `GET/PATCH /api/v1/members/me`, `GET /api/v1/places/bookmarks`, 로그인 닉네임, `WITHDRAWAL_PENDING` 복구 경로가 연결됐다.
- 서버와 합의된 `drivingPeriod` 계약은 `UNDER_1_MONTH`, `MONTHS_1_2`, `MONTHS_3_5`, `MONTHS_6_11`, `YEARS_1_2`, `YEARS_3_9`, `OVER_10_YEARS`다. 클라이언트는 이 값만 전송하며 구형 enum 호환 변환은 하지 않는다.

## Codex Result — alpha04

- Changed files: `.github/workflows/release.yml`; `app/build.gradle.kts`, `app/src/main/java/com/dororong/rodi/ui/{MainScreen,RodiApp,RodiAppViewModel}.kt`와 app tests; `core/domain`의 auth/member/onboarding/place 모델·저장소·use case·tests; `core/data`의 auth/member/place API·DTO·mapper·repository·DataStore·Room cache·tests; `core/ui/.../AccountRecoveryDialog.kt`; `feature/auth`, `feature/entry`, `feature/home`, `feature/mypage` 화면·ViewModel·Contract·tests; `docs/PROJECT.md`, `docs/handoff/HANDOFF.md`; 삭제 `core/data/.../SamplePlaces.kt`, `SamplePlacesTest.kt`
- Build/test: `git diff --check` GREEN; `./gradlew :feature:home:testDebugUnitTest` GREEN; 최종 결합 트리 `./gradlew test lint assembleDebug assembleRelease bundleRelease` GREEN (778 tasks); debug APK emulator install·cold launch·프로세스 유지 GREEN; 실기기 지도 이동·확대/축소·클러스터 동작 사용자 확인 GREEN
- Open questions: 서버 배포본과 OpenAPI의 새 7개 `drivingPeriod` enum 확인, Navigator 이미지 확정, GitHub `KEYSTORE_BASE64` secret 교체 후 workflow 수동 실행, 위치 지연/거절·온보딩 Figma 대조·계정 복구·empty/normal 시트 실기기 QA가 남아 있다.

## Follow-up — 기존 회원 재실행 진입 게이트

- 서버가 기존 회원으로 확인한 로그인과 계정 복구 성공 시 로컬 `entry_completed`를 저장한다.
- 신규 회원은 완료 상태를 기록하지 않고 계정 복구 결과의 신규 여부도 화면 전환에 전달해 미완료 온보딩으로 계속 진입한다.
- 로그인 화면과 홈 내부 보호 동작 로그인이 공유하는 domain use case에서 처리해 모든 인증 진입점에 같은 정책을 적용한다.
- Changed files: `core/domain/.../LoginWithKakaoUseCase.kt`, `RestoreWithKakaoUseCase.kt`, `feature/auth/.../LoginViewModel.kt`, 관련 tests, `docs/handoff/HANDOFF.md`
- Build/test: `./gradlew :core:domain:test :feature:auth:testDebugUnitTest :app:testDebugUnitTest assembleDebug` GREEN
- Open questions: 기존 회원 로그인 후 프로세스 종료·재실행 시 홈 직행을 실기기에서 최종 확인한다.

## Follow-up — PR #48 리뷰 반영

Status: IMPL_DONE

- 신규 회원은 로컬 진입 완료 여부와 무관하게 온보딩으로 이동하고, 기존 회원·서버 신규 여부 누락만 각각 홈·로컬 상태로 분기한다.
- 홈 계정 복구 credential은 ViewModel private 필드에만 유지하고 UI 상태에는 복구 대기 여부만 노출한다.
- 마이페이지 최초 조회 소유자를 화면 lifecycle로 단일화하고, 기존 콘텐츠를 유지한 백그라운드 갱신 실패는 공용 스낵바로 표시한다.
- 인증 날짜 파싱 오류 문구를 로그인·복구 공용으로 정리하고, 장소 캐시는 첫 페이지 크기를 지키며 서버 cursor를 첫 페이지 캐시로 재생하지 않는다.
- `PROJECT.md` 모듈 맵에 실제 `:feature:mypage` 책임을 추가했다.
- 제외: 온보딩 pending은 repository가 `Submitted`·`AlreadyCompleted`에서 이미 해제하므로 use case 중복 처리를 넣지 않았다. 영문 레벨 표기는 온보딩·마이페이지 공통 현행 정책이라 유지했다. 서버 복구 성공 후 로컬 저장 실패의 보상 정책과 HANDOFF 템플릿 변경은 별도 결정 없이 자동 반영하지 않았다.
- Changed files: `app/.../RodiApp.kt`, route test; `core/data` auth mapper·cached place repository와 tests; `feature/home` Contract·Screen·ViewModel·test; `feature/mypage` Screen·ViewModel·test; `docs/PROJECT.md`, `docs/handoff/HANDOFF.md`
- Build/test: `git diff --check` GREEN; `./gradlew :app:testDebugUnitTest :core:data:testDebugUnitTest :feature:home:testDebugUnitTest :feature:mypage:testDebugUnitTest` GREEN; `./gradlew assembleDebug` GREEN
- Open questions: none

## Follow-up — 주차장·경로 상세 시트 단방향 드래그

Status: IMPL_DONE

- empty/error·주차장 상세·경로 상세·상세 로딩 화면이 공용 `DismissibleSheetHandle`을 사용한다. 핸들에서 아래 방향으로 12dp 이상 드래그하면 콜백을 한 번만 호출하고, 위 방향 드래그는 소비하되 시트를 확장하지 않는다.
- 상세 핸들 드래그는 진입 위치와 관계없이 `Navigation`으로 돌아가며 상세·경로 요청을 취소하고 선택 장소·경로·진입 위치와 상세/경로/북마크 진행 상태를 초기화한다.
- 드래그 직전 선택된 주차장 마커를 서버 좌표 타입까지 확인해 기본 상태로 복원한다. 상세 응답을 기다리는 중에도 같은 동작을 수행한다.
- X 버튼과 시스템 뒤로가기는 기존 `OnDismissDetail` 경로를 유지해 목록에서 진입한 경우 `PartialList`로 돌아간다. 주차장 내부 스크롤 영역은 핸들과 분리했다.
- Changed files: `feature/home/.../components/DismissibleSheetHandle.kt`, `HomeContract.kt`, `HomeScreen.kt`, `HomeViewModel.kt`, `detail/components/{CourseDetailContent,ParkingDetailContent,PlaceDetailLoading}.kt`, `list/components/PlaceEmptyContent.kt`, `HomeViewModelTest.kt`, `docs/handoff/HANDOFF.md`
- Build/test: `git diff --check` GREEN; `./gradlew :feature:home:testDebugUnitTest assembleDebug` GREEN
- Open questions: 지도·목록에서 각각 상세을 열었을 때의 핸들 하강, 위 방향 드래그, 주차장 콘텐츠 스크롤, 상세 로딩 중 닫기를 실기기에서 최종 확인한다.

## Follow-up — 주차장 중복 마커·요금 안내 복구

Status: IMPL_DONE

- 같은 좌표에 서로 다른 서버 장소 ID가 개별 마커로 겹쳐 있을 때, 주차장 선택 시 선택 대상 외의 동일 좌표 라벨을 숨긴다. 선택 해제 시 숨긴 라벨을 다시 노출한다.
- Kakao Map 라벨 스타일은 기존 `changeStyles` 대신 라벨을 숨긴 상태에서 `setStyles`와 `invalidate(false)`로 교체한 뒤 다시 노출해 기본 사각 마커가 선택 핀과 함께 남지 않게 했다.
- Figma `1982:38025`의 요금 안내 4행 구조를 모든 주차장에 공통 적용한다. 무료 주차장도 초기무료·기본요금·추가요금·할증기준시간 행을 유지하며 기본요금만 `무료`로 표시한다.
- 유료·무료 표시 정책을 단위 테스트로 고정하고 375dp 유료·무료 Preview를 각각 추가했다.
- Changed files: `feature/home/.../map/BrowseMapRenderer.kt`, `feature/home/.../detail/components/ParkingDetailContent.kt`, `feature/home/.../detail/components/ParkingFeeDisplayTest.kt`, `docs/design/visual-qa.md`, `docs/handoff/HANDOFF.md`
- Build/test: `git diff --check` GREEN; `./gradlew :feature:home:compileDebugKotlin :feature:home:testDebugUnitTest` GREEN; `./gradlew assembleDebug` GREEN; debug APK emulator install·홈/목록 진입 GREEN
- Open questions: 에뮬레이터 저장 세션의 refresh token 오류로 상세 화면 캡처가 막혔다. 구미의 동일 좌표 주차장 선택 전후 단일 마커와 유·무료 상세의 Figma 픽셀 대조를 실기기에서 최종 확인한다.

## Follow-up — PR #48 추가 리뷰 반영

Status: IMPL_DONE

- 앱 시작 시 비로그인 사용자는 보류 온보딩 동기화를 호출하지 않고, 일반 동기화 실패는 앱 준비 상태를 막지 않으며 코루틴 취소는 전파하는 계약을 테스트로 고정했다.
- 계정 복구 서버 성공과 로컬 상태 동기화를 분리했다. 로컬 쓰기 하나가 실패해도 복구 성공을 유지하고 나머지 로컬 갱신을 계속 시도하며 취소는 전파한다.
- 신규·기존 회원의 로컬 진입 완료 반대 조합, 상세 요청 Job의 실제 취소를 테스트에 추가했다.
- 온보딩 레벨 표시는 현행 영문 UI 정책을 유지하되 enum 이름 파생 대신 명시적 매핑으로 고정했다.
- 주차장 요금 안내의 실기기 캡처 미완료 상태를 `Device capture pending`으로 정정하고 alpha04 HANDOFF에 필수 Context·Files·Acceptance·Verification·Out of scope·Claude Review 섹션을 추가했다.
- 별도 코드 변경 없음: 복구 credential은 이미 ViewModel private 필드에만 보관한다. 온보딩 pending 해제는 `OnboardingRepositoryImpl.submit()`의 성공·기완료 분기에서 수행하고 데이터 테스트가 보장한다.
- Changed files: `app/.../RodiAppViewModel.kt`, `RodiAppViewModelTest.kt`, `RodiAppRouteTest.kt`; `core/domain/.../RestoreWithKakaoUseCase.kt`, `AccountAuthUseCasesTest.kt`; `feature/entry/.../OnboardingAnalysisDialog.kt`; `feature/home/.../HomeViewModelTest.kt`; `docs/design/visual-qa.md`, `docs/handoff/HANDOFF.md`
- Build/test: `git diff --check` GREEN; `./gradlew :app:testDebugUnitTest :core:domain:test :feature:entry:testDebugUnitTest :feature:home:testDebugUnitTest` GREEN; `./gradlew assembleDebug` GREEN
- Open questions: none

## Follow-up — 운전 기간 서버 enum 갱신

Status: IMPL_DONE

- 운전 기간 도메인 enum과 서버 wire value를 `UNDER_1_MONTH`, `MONTHS_1_2`, `MONTHS_3_5`, `MONTHS_6_11`, `YEARS_1_2`, `YEARS_3_9`, `OVER_10_YEARS`로 통일했다.
- 선택 UI는 `1개월 미만`, `1~2개월`, `3~5개월`, `6~11개월`, `1~2년`, `3~9년`, `10년 이상` 순서로 표시한다.
- `UNDER_1_MONTH`·`MONTHS_1_2`·`MONTHS_3_5`는 0점, `MONTHS_6_11`·`YEARS_1_2`는 1점, `YEARS_3_9`·`OVER_10_YEARS`는 즉시 Navigator로 판정한다.
- 구형 로컬 enum 값을 새 서버 값으로 추정하는 호환 변환은 추가하지 않았다.
- Changed files: `core/domain/.../OnboardingProfile.kt`, `OnboardingLevel.kt`와 tests; `core/data/.../OnboardingMapper.kt`, repository·mapper tests; `feature/entry/.../CareerContent.kt`, `EntryViewModelTest.kt`; `docs/handoff/HANDOFF.md`
- Build/test: `git diff --check` GREEN; `./gradlew :core:domain:test :core:data:testDebugUnitTest :feature:entry:testDebugUnitTest` GREEN; `./gradlew assembleDebug` GREEN
- Open questions: 서버 배포본과 OpenAPI가 새 7개 enum을 실제 제공하는지 출시 전에 확인한다.

## Follow-up — 주차장 공공데이터 출처 고지

Status: IMPL_DONE

- 설정 메뉴의 `오픈소스 라이센스`와 별도로 `데이터 출처` 항목을 추가했다.
- 데이터 출처 화면에 공공데이터포털 `전국주차장정보표준데이터`, 제공기관 안내, 2026년 7월 23일 기준일과 실제 운영 정보 차이 가능성을 고지한다.
- 시스템 뒤로가기와 상단 뒤로가기는 기존 설정 내부 목적지 규칙에 따라 설정 메뉴로 돌아간다.
- Changed files: `feature/settings/.../SettingsScreen.kt`, `datasource/DataSourceScreen.kt`, `DataSourceScreenTest.kt`, `docs/handoff/HANDOFF.md`
- Build/test: `git diff --check` GREEN; `./gradlew :feature:settings:testDebugUnitTest assembleDebug` GREEN
- Open questions: 실제 기기에서 설정 → 데이터 출처 진입·복귀와 긴 문구 스크롤을 최종 확인한다.

## Follow-up — PR #48 최종 리뷰 반영

Status: IMPL_DONE

- 보류 온보딩 재시도의 실제 `viewModelScope.launch` 진입점이 반환한 Job에서 `CancellationException`이 취소 완료 원인으로 전파되는지 테스트한다.
- 단일 화면인 `DataSourceScreen`과 고지문 테스트를 `feature.settings` 루트 패키지로 이동해 저장소 구조 규칙을 따른다.
- Changed files: `app/.../RodiAppViewModel.kt`, `RodiAppViewModelTest.kt`; `feature/settings/.../DataSourceScreen.kt`, `DataSourceScreenTest.kt`, `SettingsScreen.kt`; `docs/handoff/HANDOFF.md`
- Build/test: `git diff --check` GREEN; `./gradlew :app:testDebugUnitTest :feature:settings:testDebugUnitTest assembleRelease` GREEN
- Open questions: none

## Follow-up — Navigator 레벨 이미지 적용

Status: IMPL_DONE

- 온보딩 분석 결과에는 투명 배경 Navigator 일러스트를, 마이페이지 프로필 카드에는 배경 포함 Navigator 일러스트를 적용했다.
- 기존 레벨별 자산 크기(온보딩 300×300, 프로필 270×270)와 동일한 원본을 사용해 기존 레이아웃 규칙을 유지한다.
- Changed files: `core/ui/.../illust_level_navigator.png`, `feature/entry/.../OnboardingAnalysisDialog.kt`, `feature/mypage/.../illust_profile_navigator.png`, `feature/mypage/.../ProfileCard.kt`, `docs/handoff/HANDOFF.md`
- Build/test: `git diff --check` GREEN; `./gradlew assembleDebug` GREEN
- Open questions: none

## Follow-up — 로그아웃·탈퇴 시 온보딩 로컬 정보 초기화

Status: IMPL_DONE

- 로그아웃과 탈퇴의 서버 요청·세션 토큰 삭제가 성공한 뒤, 온보딩 프로필·보류 동기화 상태와 진입 게이트 진행 상태·둘러보기 권한을 모두 초기화한다.
- 서버 요청이 실패하거나 취소되면 기존 로컬 정보는 유지한다.
- Changed files: `core/domain` 온보딩 정리 use case·로그아웃·탈퇴 use case와 tests; `core/data` entry/onboarding DataStore·repository와 tests; `docs/handoff/HANDOFF.md`
- Build/test: `./gradlew :core:domain:test :core:data:testDebugUnitTest :feature:settings:testDebugUnitTest` GREEN; `./gradlew assembleDebug` GREEN
- Open questions: 실기기에서 로그아웃·탈퇴 서버 성공 및 세션 종료 후 온보딩 재진입 상태가 초기화되는지, 서버 요청 실패·취소 시 로컬 상태가 유지되는지 확인이 필요하다.

## Follow-up — 둘러보기·게스트 신규 회원 엔트리 분리

Status: IMPL_DONE

- 엔트리 모드를 일반 로그인, 둘러보기, 게스트 신규 회원 가입으로 영속화했다. 기존 둘러보기 사용자는 저장된 모드가 없어도 게스트 권한으로 식별한다.
- 둘러보기는 약관 → 운전 자격 및 주의 사항 → 위치 권한 → 홈만 진행하며 닉네임과 온보딩 설문을 건너뛴다. 이전 버전에서 설문 단계에 남은 게스트 진행 상태는 주의 사항으로 보정한다.
- 홈 로그인 다이얼로그에서 신규 회원이면 기존 게스트 온보딩 답변을 초기화하고 닉네임 → 기존 온보딩 → 분석 완료 다이얼로그를 진행한다. 분석 완료 확인 시 다이얼로그를 유지한 채 완료 상태를 저장한 뒤 홈으로 이동하며 약관·주의 사항·위치 권한을 다시 노출하지 않는다.
- 홈 로그인 다이얼로그의 기존 회원은 온보딩으로 보내지 않고 로그인 전에 요청한 상세·북마크·마이페이지 동작을 기존처럼 재개한다. 최초 로그인 화면의 신규·기존 회원 분기는 유지한다.
- Changed files: `app/.../{MainScreen,RodiApp}.kt`; `core/domain` entry mode·repository·guest/login use case와 tests; `core/data` entry DataStore·repository와 test; `feature/entry` Contract·Flow·ViewModel·NicknameContent와 tests; `feature/home` Contract·Screen·ViewModel과 test; `docs/handoff/HANDOFF.md`
- Build/test: `git diff --check` GREEN; `./gradlew :core:domain:test :core:data:testDebugUnitTest :feature:entry:testDebugUnitTest :feature:home:testDebugUnitTest :app:testDebugUnitTest assembleDebug` GREEN
- Open questions: none

## Follow-up — 최근 카카오 로그인 툴팁 복구

Status: IMPL_DONE

- Figma `1946:18595`와 로그인 화면을 대조했다. 최근 카카오 로그인 상태에서 둘러보기 숨김과 `최근에 로그인했어요!` 툴팁 표시는 기존 UI를 그대로 사용한다.
- 최근 로그인 provider를 세션 토큰과 분리해 저장한다. 로그아웃·세션 종료로 토큰을 삭제해도 카카오 로그인 이력은 유지되며 앱 재실행 후에도 최근 로그인 화면으로 진입한다.
- 기존 설치 사용자가 업데이트 후 처음 로그아웃할 때는 삭제 직전 토큰의 provider를 최근 로그인 이력으로 보존한다.
- Changed files: `core/data/.../AuthTokenDataStore.kt`, `AuthTokenStore.kt`, `AuthRepositoryImpl.kt`와 tests; `app/.../RodiAppViewModel.kt`와 test; `docs/handoff/HANDOFF.md`
- Build/test: `git diff --check` GREEN; `./gradlew :core:data:testDebugUnitTest :app:testDebugUnitTest assembleDebug` GREEN
- Open questions: none

## Follow-up — PR #49 리뷰 반영

Status: IMPL_DONE

- 로그아웃·탈퇴 뒤 온보딩 로컬 상태 정리는 한 저장소가 실패해도 나머지 저장소를 계속 정리한다. 실패한 대상은 `OnboardingDataCleanupException`으로 구분하고 코루틴 취소는 그대로 전파한다.
- 둘러보기에서 신규 회원으로 전환할 때에는 기존 약관·주의사항 진행 키를 모두 제거한 뒤 닉네임 단계부터 시작한다. 위치 권한 처리와 둘러보기 권한은 유지한다.
- 게스트 신규 회원 로그인 후에는 게스트 작업 재개 effect가 추가로 발생하지 않는 회귀 테스트를 고정했다.
- Changed files: `core/domain` 온보딩 정리 use case·로그아웃·탈퇴 tests; `core/data/.../EntryPreferences.kt`; `feature/home/.../HomeViewModelTest.kt`; `docs/handoff/HANDOFF.md`
- Build/test: `git diff --check` GREEN; `./gradlew :core:domain:test :core:data:testDebugUnitTest :feature:home:testDebugUnitTest` GREEN; `./gradlew assembleDebug` GREEN
- Open questions: 로그아웃·탈퇴의 서버 성공·실패·취소와 게스트 신규 회원 전환은 실기기에서 최종 확인이 필요하다.

## Follow-up — main 기준 CodeRabbit 앱 리뷰

Status: IMPL_DONE

- 로그인 성공 후 앱 루트가 인증 세션을 다시 조회하고, 세션 조회 실패 뒤에도 다음 갱신으로 정상 상태를 복구한다.
- 앱 루트 백스택을 Navigation 3의 저장 가능한 `NavBackStack`으로 바꿔 프로세스 재생성 시에도 route 타입과 복원 경로를 유지한다.
- Changed files: `app/.../RodiApp.kt`, `RodiAppViewModel.kt`, `RodiAppViewModelTest.kt`; `docs/handoff/HANDOFF.md`
- Build/test: `git diff --check` GREEN; `./gradlew :app:testDebugUnitTest :app:assembleDebug` GREEN
- Open questions: CodeRabbit free CLI의 `core` 전체 리뷰는 150파일 제한을 초과했고, `core/data` 경량 리뷰는 결과 없이 장시간 대기해 이번 커밋에 포함하지 않았다.

## Follow-up — 1.1.1 버전 반영

Status: IMPL_DONE

- 앱 버전을 `versionName=1.1.1`, `versionCode=7`로 최종 반영했다.
- Changed files: `app/build.gradle.kts`, `docs/PROJECT.md`, `docs/handoff/HANDOFF.md`
- Build/test: `./gradlew assembleDebug` GREEN
- Open questions: none

## Follow-up — 클러스터 바텀 네비 안전 여백

Status: IMPL_DONE

- Navigation 상태에서는 실제 측정한 바텀 네비 높이를 지도 하단 패딩으로 사용한다.
- 클러스터 bounds 이동의 고정 여백은 64dp로 늘리고, 이보다 바텀 네비 높이와 16dp 안전 여백이 크면 그 값을 사용한다. SDK가 전체 MapView 기준으로 bounds를 계산해도 선택 마커 좌표가 네비 아래로 내려가지 않는다.
- SDK가 이미 padding을 반영해 반환한 viewport를 앱에서 다시 차감하지 않도록 수정했다. 부분 목록·상세 시트의 기존 패딩과 선택 클러스터 ID 제한은 유지한다.
- 앱 버전은 `versionName=1.1.1`, `versionCode=7`이다.
- Changed files: `app/build.gradle.kts`, `feature/home/.../HomeScreen.kt`, `feature/home/.../map/{MapViewport.kt,MapClustererTest.kt}`, `docs/{PROJECT.md,handoff/HANDOFF.md}`
- Build/test: `git diff --check` GREEN; `./gradlew :feature:home:testDebugUnitTest assembleDebug` GREEN
- Open questions: 실제 서버 클러스터에서 긴 이름·주차장 마커가 바텀 네비 위에 표시되는지 실기기 확인이 필요하다.

## Follow-up — PR #51 리뷰 반영

Status: IMPL_DONE

- 앱 루트 상태 수집은 영구 오류에서 최대 세 번만 재시도하고, 재시도마다 1초씩 증가하는 대기 시간을 적용한다. 취소 예외는 기존처럼 즉시 전파한다.
- 로그인 직후 인증 세션 갱신은 `MutableStateFlow.update`로 원자적으로 증가시킨다.
- 클러스터 bounds 이동은 지도 viewport의 바텀 네비 패딩과 분리된 64dp 카메라 여백만 사용한다.
- Changed files: `app/.../RodiAppViewModel.kt`, `RodiAppViewModelTest.kt`; `feature/home/.../HomeScreen.kt`; `docs/handoff/HANDOFF.md`
- Build/test: `git diff --check` GREEN; `./gradlew :app:testDebugUnitTest :feature:home:testDebugUnitTest assembleDebug` GREEN
- Open questions: none

## Previous alpha03 record

Status: IMPL_DONE
PR: #47 (`release/1.1.0-alpha03` ← `fix/home-1.1`)

## Context

alpha03 홈 지도에서 현 위치보다 기본 viewport 조회가 먼저 실행되는 경합, 동일 좌표 샘플/서버 마커 중복, 홈·마이 전환 시 바텀 내비 재구성, 주차장 상세 시트의 고정 높이와 토글 재중앙 정렬을 수정한다.
PR #47의 주소 축약과 부분 목록 지도 패딩을 유지하고, 별도 워크트리에 남아 있던 온보딩 분석 자산과 완료 상태 수정을 최신 `origin/develop` 기준으로 통합한다.

## Implemented

- 현 위치는 권한 허용 후 최대 5초간 기다리고, 성공 시 목표 카메라 이동이 끝난 뒤 최초 조회한다.
- 카메라 자동 검색은 목표 좌표·줌·세대·이동 사유를 보관하며, viewport 중심이 목표점에서 가로·세로 span의 5% 이내일 때만 소비한다.
- 새 자동 이동이 이전 pending 검색을 대체하고, 사용자 제스처는 pending 검색을 취소한다. ViewModel의 요청 세대와 Job 취소가 최신 조회 결과만 반영한다.
- 위치 거절·타임아웃은 기본 viewport로 폴백한다. 늦은 위치는 사용자가 지도를 움직이거나 별도 viewport를 선택하지 않았을 때만 자동 이동·재조회한다.
- KakaoMap 최소 줌을 6으로 고정하고 회전·회전 줌·기울기 제스처를 비활성화했다.
- 동일 타입·동일 좌표에 서버 양수 ID가 있으면 샘플 음수 ID만 제외한다. 서로 다른 좌표와 서버끼리의 동일 좌표는 유지한다.
- 좌표·목록 캐시 단계에서 같은 정규화를 적용해 전국/지역 클러스터, 개별 마커, 목록이 동일한 데이터를 사용한다.
- `MainScreen`이 하나의 `movableContentOf` 바텀 내비를 소유하고 홈에서는 시트 아래, 마이에서는 상위 레이어로 이동 배치한다.
- 주차장 상세은 `heightIn(max = 400.dp)`와 내부 스크롤을 사용한다. 장소별 최초 유효 높이만 지도 패딩으로 고정하고 토글 높이 변화는 지도 중심에 반영하지 않는다.
- `versionCode 4`, `versionName 1.1.0-alpha03`을 반영했다.
- 온보딩 분석 GIF를 680×600 고해상도 자산으로 교체하고 기존 GIF 재생 방식을 유지한 채 180dp 폭으로 표시한다.
- 온보딩 분석 제출 성공 시 `PRECAUTIONS` 진행 상태를 결과 노출 전에 저장한다. 결과 확인이나 앱 재시작 후 설문이 다시 노출·제출되지 않으며 주의사항 단계에서는 이전 설문으로 돌아가지 않는다.
- 최신 `origin/develop`을 PR 브랜치에 병합해 마이페이지·약관 등 이후 반영 사항이 PR 최종 트리에서 빠지지 않도록 했다.
- 온보딩은 투명 배경 `illust_level_*`, 마이페이지는 배경 포함 `illust_profile_*` 자산을 각각 사용하는 것을 확인했다.

## Recovery audit

- 유지: 현재 GIF 방식과 고해상도 GIF 자산, 온보딩 완료 진행 상태 보존, 최신 develop의 레벨 이미지·약관 자산.
- 제외: Media3/WebM 재생안과 WebM preview 이미지는 현재 GIF 유지 요청과 충돌해 적용하지 않았다. 원본 `codex/terms-backstack-reset` 워크트리 변경은 삭제하지 않았다.
- 제외: 오래된 코스 상세 간격 변경은 이후 닫기 버튼·상세 시트 디자인 수정과 겹쳐 적용하지 않았다.
- 제외: 홈 stash 2건과 `feat/home-1.1-fix`의 미병합 변경은 현재 홈 구조 및 PR #41–46에서 대체되었으므로 통째 적용하지 않았다. stash와 워크트리는 보존했다.
- 제외: detached 워크트리의 `RodiApplication`·마이페이지 배경 변경은 최신 develop에 동등하거나 이후 구현이 존재해 적용하지 않았다. 해당 워크트리도 보존했다.

## Manual QA

- signed debug APK 설치 성공: `app/build/outputs/apk/debug/app-debug.apk`
- 현 위치 마커와 주변 결과가 같은 viewport에 표시되는 것을 확인했다.
- 지역 클러스터 클릭 후 줌 14 개별 주차장 마커 전환을 확인했다.
- 홈 → 마이 → 홈 양방향에서 바텀 내비가 한 개만 표시되고 선택 상태가 전환되는 것을 확인했다.
- 주차장 상세이 바텀 내비 위를 덮고, 영업시간 토글 전후 지도 마커 위치가 유지되며 긴 내용은 400dp 안에서 스크롤되는 것을 확인했다.
- 온보딩·마이페이지 레벨 이미지 파일과 코드 매핑을 대조해 투명/배경 포함 자산이 화면별로 분리된 것을 확인했다.
- 기기에서 별도 위치 지연·권한 거절, 전국 줌 6 클러스터, 30% 재검색 경계, 짧은 주차장 wrap은 자동화/코드 검증까지만 수행했다.
- 고해상도 GIF와 분석 완료 후 뒤로가기 흐름은 단위 테스트·빌드까지만 검증했으며 기기 수동 QA가 추가로 필요하다.

## Codex Result

- Changed files: `app/build.gradle.kts`, `app/src/main/java/com/dororong/rodi/ui/MainScreen.kt`, `core/data` 캐시 repository·샘플 정규화·tests, `feature/home` HomeScreen·location·map pending search·parking layout state·tests, `feature/entry` 분석 GIF·dialog·flow·ViewModel·주의사항 content·tests, `docs/handoff/HANDOFF.md`, 이전 HANDOFF archive
- Build/test: `git diff --check` GREEN; `./gradlew :core:domain:test :core:data:testDebugUnitTest :feature:entry:testDebugUnitTest :feature:home:testDebugUnitTest lint assembleDebug assembleRelease` GREEN; debug APK emulator install GREEN
- Open questions: 위치 지연·권한 거절과 전국/30% 경계, 고해상도 GIF 및 분석 완료 후 뒤로가기 흐름은 실제 기기 수동 QA가 추가로 필요하다.

## Follow-up — 빈 상태 시트 단방향 드래그

Status: IMPL_DONE

- 빈 결과·초기 오류 시트는 Material 양방향 swipe를 사용하지 않는다.
- 핸들 행을 아래로 12dp 이상 드래그할 때만 시트를 숨기며, 위 방향 드래그는 소비하고 전체 화면 전환도 차단한다.
- Changed files: `feature/home/src/main/java/com/dororong/rodi/feature/home/HomeScreen.kt`, `feature/home/src/main/java/com/dororong/rodi/feature/home/list/components/PlaceEmptyContent.kt`
- Build/test: `git diff --check` GREEN; `./gradlew :feature:home:testDebugUnitTest assembleDebug` GREEN; debug APK emulator install GREEN
- Open questions: none

## Follow-up — Play Store 출시 감시

Status: BLOCKED

- 공개 Play Store 페이지의 버전·업데이트 날짜 스크래핑을 제거하고 Google Play Developer API의 `production` 트랙을 조회한다.
- 기본 브랜치의 `app/build.gradle.kts`에 있는 `versionCode`가 마지막 Discord 알림 코드보다 클 때만 감시한다.
- 대상 코드가 production 트랙에서 `RELEASE_LIFECYCLE_STATE_PUBLISHED`가 되면 Discord를 한 번 전송하고 상태 파일에 알림 코드를 기록한다.
- GitHub Secret `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`에는 Play Console에서 앱 조회 권한을 부여한 서비스 계정 JSON 전체를 저장해야 한다.
- Changed files: `.github/workflows/playstore-watch.yml`, `.github/playstore-watch/check_playstore_update.py`, `playstore-state.json`, `test_check_playstore_update.py`, `docs/handoff/HANDOFF.md`
- Build/test: `python3 .github/playstore-watch/test_check_playstore_update.py` GREEN; `python3 -m py_compile .github/playstore-watch/check_playstore_update.py .github/playstore-watch/test_check_playstore_update.py` GREEN
- Open questions: Play Console 서비스 계정과 GitHub Secret 설정은 저장소 밖 권한이 필요하다.
