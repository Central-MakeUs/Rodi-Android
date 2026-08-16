# HANDOFF — 운전 세션·Android 16 ProgressStyle 스파이크

Status: IMPL_DONE
Branch: codex/spike-driving-live-updates

## Context

현재 홈의 `연습하러 가기`는 카카오맵·카카오내비만 실행하며 운전 세션, 백그라운드 위치 추적,
도착 판정, Service, 알림 구현은 없다. 실제 제품 기능으로 채택하기 전에 Android 위치 Foreground
Service와 Android 16 ProgressStyle의 동작·커스텀 가능 범위를 확인하는 스파이크를 만든다.

## Spec

- 사용자가 홈에서 명시적으로 내비 실행을 선택한 경우에만 location Foreground Service를 시작한다.
- 현재 운전 세션 하나를 DataStore에 저장하고 `START_NOT_STICKY`로 자동 재시작하지 않는다.
- 정확도 50m 이내 위치가 목적지 100m 안에 연속 2회 들어오면 도착으로 판정한다.
- 운전 중에는 동일 ID의 ongoing 알림을 유지하고 Android 16 이상에서 ProgressStyle을 사용한다.
  하위 버전과 제조사별 표준 렌더링 환경은 표준 ongoing 알림으로 동작한다.
- 도착 상태를 먼저 저장한 후 위치 추적을 중단하고 같은 ID를 일반 도착 알림으로 전환한다.
- 앱이 화면에 있거나 사용자가 도착 알림을 눌러 복귀하면 Home의 상태 기반 다이얼로그를 표시한다.
- 권한·알림 채널이 비활성화된 경우 서비스와 외부 내비 실행을 보류하고 홈 스낵바로 안내한다.
- Dialog·Activity 강제 실행, FullScreenIntent, 오버레이, WakeLock, RemoteViews는 사용하지 않는다.

## Files

- 운전 도메인·저장: `core/domain`, `core/data`
- Service·알림·Manifest: `app`
- 시작·도착 UI 연결과 기존 위치 소스 공유: `feature/home`
- 구현·검증 기록: `docs/handoff/HANDOFF.md`

## Acceptance

- 운전 시작 시 location FGS와 지속 알림이 시작되고 앱이 백그라운드여도 위치 수신이 유지된다.
- API 36 이상은 ProgressStyle을 사용하고, API 35 이하는 표준 ongoing 알림으로 동작한다.
- 도착 판정·저장·알림 전환·서비스 종료가 한 번만 실행된다.
- 백그라운드 도착은 화면을 강제로 열지 않으며 알림 클릭 후 Home 다이얼로그로 연결된다.
- 수동 종료는 위치 callback, coroutine, FGS, ongoing 알림과 임시 세션을 멱등하게 정리한다.
- 알림 또는 위치 권한 거부 시 크래시나 보이지 않는 위치 추적이 발생하지 않는다.

## Verification

- 도착 정책, 거리 누적, 저장 상태 전환, Home 상태, 버전별 알림 정책을 단위 테스트한다.
- `git diff --check`
- `./gradlew :core:domain:test :core:data:testDebugUnitTest :feature:home:testDebugUnitTest :app:testDebugUnitTest`
- `./gradlew lint assembleDebug`
- API 36.1 에뮬레이터에서 FGS·알림·백그라운드·화면 꺼짐·도착·권한 거부를 확인한다.

## Out of scope

- 서버 운전 기록, Room 누적 기록, 정식 운전 기록 화면과 실디자인
- Android 15 AVD 신규 설치
- 시스템/OEM이 결정하는 ProgressStyle의 확장 카드 표현 보장

## Codex Result

- Changed files: `core/domain/**/driving/*`, `core/domain/**/DrivingNavigation*`,
  `core/data/**/DrivingSession*`, `core/data/**/DrivingNavigation*`,
  `app/**/spike/driving/*`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/dororong/rodi/ui/MainScreen.kt`,
  `feature/home/**/Home*`, `feature/home/**/DrivingArrivalDialog.kt`,
  `feature/home/**/CurrentLocation.kt`, `feature/home/**/Kakao*Launcher.kt`, 관련 단위 테스트
- Build/test: `git diff --check` GREEN, 관련 모듈 단위 테스트 GREEN,
  `./gradlew lint assembleDebug` GREEN, API 36.1 AVD의 location FGS·ProgressStyle 요청·화면 꺼짐
  도착·동일 ID 알림 전환·알림 클릭 다이얼로그·수동 종료·알림 권한 거부 GREEN
- Open questions: API 35 이하 실제 알림 렌더링은 AVD 부재로 직접 확인하지 못했으며,
  ProgressStyle이 확장 카드로 보일지 또는 표준 알림으로 보일지는 시스템·OEM 결정 범위다.

## Follow-up Result

- Changed files: `core/data/**/PlaceApi.kt`, `core/data/**/PlaceRepositoryImpl.kt`,
  `core/data/**/PlaceRepositoryImplTest.kt`
- Build/test: `./gradlew :core:data:testDebugUnitTest` GREEN, `git diff --check` GREEN,
  `./gradlew assembleDebug` GREEN
- Open questions: 서버는 `GET /places`와 `GET /places/coordinates`의 선택적 Bearer 토큰을 해석해
  회원별 필터를 적용해야 한다. 필터 저장 API와 UI 연결은 이번 변경 범위에 포함하지 않았다.

## Follow-up Result — 지도 가시 viewport

- Changed files: `feature/home/.../HomeScreen.kt`, `BottomSheetViewportPolicy.kt`와 단위 테스트
- Build/test: `./gradlew :feature:home:testDebugUnitTest` GREEN, `git diff --check` GREEN,
  `./gradlew assembleDebug` GREEN
- Open questions: 바텀시트 부분·전체 전환 후 지도 이동 또는 재검색에서 서버 요청의 viewport 파라미터가
  시트 상단 기준으로 변경되는지 실기기 네트워크 로그로 확인이 필요하다.

## Follow-up Result — 홈 정렬 필터 저장

- Changed files: `core/domain` MemberRepository·UpdateFilterTagsUseCase, `core/data` MemberApi·DTO·Repository와
  테스트, `feature/home` Contract·ViewModel·테스트
- Build/test: `./gradlew :core:domain:test :core:data:testDebugUnitTest :feature:home:testDebugUnitTest` GREEN,
  `git diff --check` GREEN, `./gradlew assembleDebug` GREEN
- Open questions: 현재 Android 홈에는 필터 선택 UI가 없으므로, 해당 UI에서
  `HomeIntent.OnFilterTagsSaved(filterTags, query)`를 발생시키는 연결이 필요하다.

## Follow-up Result — Android 16 진행 알림 디자인 스파이크

- Changed files: `app/src/main/java/com/dororong/rodi/spike/driving/DrivingNotificationFactory.kt`,
  `docs/DESIGN_ASSUMPTIONS.md`, 관련 `HomeViewModelTest.kt` 시그니처 정합성 수정
- Build/test: `git diff --check` GREEN,
  `./gradlew :core:domain:test :core:data:testDebugUnitTest :feature:home:testDebugUnitTest :app:testDebugUnitTest lint assembleDebug` GREEN
- Open questions: Figma의 차량 원본 asset을 저장소 리소스로 export하면 현재 임시 런처 아이콘을 tracker/start/end 아이콘으로 교체할 수 있다. API 35 실제 렌더링과 OEM별 ProgressStyle 표현은 실기기 확인이 필요하다.

## Follow-up Result — Live Update 승격 제외 (이후 재검토됨)

- Changed files: `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/dororong/rodi/spike/driving/DrivingNotificationFactory.kt`, `app/src/main/java/com/dororong/rodi/spike/driving/DrivingTrackingService.kt`, `app/src/test/java/com/dororong/rodi/spike/driving/DrivingNotificationStylePolicyTest.kt`, `docs/DESIGN_ASSUMPTIONS.md`
- Build/test: 당시 스파이크 범위에서는 Android 16은 `ProgressStyle`, Android 15 이하는 표준 알림만 사용하고
  Live Update 승격 요청과 `POST_PROMOTED_NOTIFICATIONS` 선언을 제거했다. 이후 실기기 검증을 위해 아래
  `promoted ongoing` 후속 섹션에서 공식 승격 요청을 다시 추가했다.
- Open questions: API 36 제조사 SystemUI가 ProgressStyle을 확장 카드로 표현하지 않는 경우에도 동일 알림의 표준 ongoing 표현을 사용한다.

## Follow-up Result — Figma ProgressStyle 방향 반영

- Input: Figma `3681:23511`, `3681:23512`, `3680:23416`
- Changed files: `app/src/main/java/com/dororong/rodi/spike/driving/DrivingNotificationFactory.kt`,
  `docs/DESIGN_ASSUMPTIONS.md`
- Behavior: Rodi 아이콘, 시안 문구, chronometer 헤더, primary600/gray300 진행 구간을 표준 알림 필드에 매핑한다.
  API 36 ProgressStyle에서는 시스템이 제공하지 않는 카드 배경·패딩·출발/도착 텍스트 위치와 확장 상태를 강제하지 않는다.
- Open questions: 제조사 SystemUI가 ProgressStyle을 어떤 높이와 배경으로 표현하는지는 실기기에서 확인해야 한다.

## Follow-up Result — Android 16 promoted ongoing·삼성 실기기 확인

- Changed files: `app/src/main/AndroidManifest.xml`,
  `app/src/main/java/com/dororong/rodi/spike/driving/DrivingNotificationFactory.kt`,
  `app/src/test/java/com/dororong/rodi/spike/driving/DrivingNotificationStylePolicyTest.kt`
- Behavior: API 36 이상은 표준 `ProgressStyle`과 `setRequestPromotedOngoing(true)`를 함께 사용하고,
  `POST_PROMOTED_NOTIFICATIONS`를 선언한다. API 35 이하는 기존 표준 ongoing 알림으로 유지한다.
  `RemoteViews`, 삼성 비공개 메타데이터, FullScreenIntent는 추가하지 않는다.
- Device verification: `SM-M446K` 실기기(Android 16, One UI 8.0)에 APK를 설치하고 실제 `연습하러 가기`로
  location FGS를 시작했다. `dumpsys activity services`에서 `DrivingTrackingService`와 location 타입을 확인했고,
  `dumpsys notification`에서 동일 ID `4210`, `ProgressStyle`, `requestPromotedOngoing=true`, low 중요도 채널,
  무음·ongoing·chronometer를 확인했다. 그러나 시스템이 부여하는 `PROMOTED_ONGOING` 플래그는 없었고,
  세로 알림창에서는 표준 ongoing 알림으로 표시됐다. 일부 확장/가로 표면에서는 ProgressStyle 진행 카드가 렌더링됐다.
- KoRailTalk comparison: 설치된 `com.korail.talk`(targetSdk 36) APK에는 공식
  `POST_PROMOTED_NOTIFICATIONS` 선언이나 확인 가능한 삼성 ongoing 메타데이터가 없었다. 따라서 첨부된 카드는
  표준 Android 알림을 삼성 One UI가 지원 앱·허용 목록 기준으로 Now Bar/Live Notification에 표현한 것으로
  추정하며, 정적 APK만으로 코레일톡의 정확한 내부 구현을 단정하지 않는다.
- Open questions: 실제 Live Update/Now Bar 승격, 상태바 chip, 기본 확장 여부와 표현은 Android SystemUI·삼성
  펌웨어·사용자 설정이 결정한다. 기기의 `설정 > 잠금화면 및 AOD > Now bar > 실시간 알림`에서 Rodi가
  목록에 나타나지 않는다면 앱 코드만으로 코레일톡과 같은 표면을 강제할 수 없다.
- Verification: `./gradlew clean :core:domain:test :core:data:testDebugUnitTest :feature:home:testDebugUnitTest :app:testDebugUnitTest`,
  `./gradlew lint assembleDebug`, `git diff --check`를 통과했고, API 36 실기기 설치 및 location FGS·알림을 확인했다.

## Follow-up Result — 승격 허용 경로 조사

- Android 36 SDK의 공식 사용자 설정 action은 `android.settings.APP_NOTIFICATION_PROMOTION_SETTINGS`이며,
  표준 API 36 에뮬레이터에서는 Rodi별 승격 설정 화면이 열렸다. SM-M446K One UI 8.0에서는 동일 action이
  해석되지 않아 OS가 제공하는 승격 설정 화면이 없다.
- SM-M446K 개발자 옵션 전체 목록에서 `Live Notifications for all apps` 항목을 찾지 못했고, Rodi 앱 알림
  설정에도 `Live notifications` 토글이 노출되지 않는다. `POST_PROMOTED_NOTIFICATIONS` AppOp를 테스트 기기에서
  임시 `allow`로 바꿔도 서비스 외부 시작이 차단되는 환경이라 승격 효과를 검증할 수 없었으며, 검증 후 `default`로 원복했다.
- Samsung 공식 안내는 Now Bar가 지원 앱 목록과 모델·국가·소프트웨어 조건에 따라 동작한다고 설명한다.
  공개된 일반 앱용 Now Bar allowlist 신청 API나 manifest 선언은 확인되지 않았다. 삼성 개발자 1:1 지원 또는
  파트너 문의 시 패키지 `com.dororong.rodi`, targetSdk 36, Android 16 `ProgressStyle`·promoted ongoing 구현,
  실기기 모델 `SM-M446K`, 운전 중 사용자 시작·종료가 명확한 위치 FGS라는 정보를 함께 제출해야 한다.
- 비공개 삼성 메타데이터·OEM extras·MediaSession 위장·root/AppOps 우회는 일반 배포 기능으로 채택하지 않는다.
  One UI 7 allowlist용 비공개 방식은 One UI 8/Android 16 공식 Live Updates와 별개이며, allowlist를 코드로 만들 수 없다.

## Follow-up Result — 후기 인증 정책 적용

- Input: Notion `💭 후기 인증 정책` (`3b15686b-1943-80b9-9b99-cf38ede7e022`)
- Behavior: 코스 경로 폴리라인 150m 안에서 정확도 50m 이하의 유효 표본만 이어 붙여 인정거리를 계산한다.
  코스 총거리의 40%를 인증 필요거리로 사용하고 5km를 상한으로 둔다. 코스 밖 이동·GPS 점프·낮은 정확도
  표본은 제외하며, GPS 점프가 다음 표본의 시작점이 되지 않도록 연결도 초기화한다. 기준에 도달하면
  세션을 ARRIVED로 원자 전환하고 위치 수집을 중단한다.
- Notification: `코스로 이동 중` → `코스 연습 중` → `운전연습 완료` 상태를 같은 ID로 갱신한다.
  진행률 분모는 인증 필요거리이며, 알림 권한이 없으면 권한 안내에서 `경로만 보기`로 측정 없이 내비만
  실행할 수 있다. 측정하지 않은 경로는 10분 전 재진입에서 완료 팝업을 띄우지 않는다.
- Review: 도착 상태를 확인한 뒤 기존 RV-01 흐름을 열고, `다녀왔어요`를 선택하면 인정거리를 서버의
  방문 인증 요청에 전달한다. `안 했어요`와 수동 종료는 방문 인증을 만들지 않는다.
  수동 종료 시에는 DataStore 세션과 주행 내비게이션 메타데이터도 함께 정리하고, 도착 완료 세션만
  후기 확인 전까지 유지한다.
- Changed files: `core/domain/**/driving/*`, `core/data/**/DrivingSession*`,
  `app/**/spike/driving/*`, `feature/home/**/Home*`, `feature/home/**/Driving*`,
  `feature/settings/**/PermissionSettingsScreen.kt`, 관련 단위 테스트
- Verification: `git diff --check`, `./gradlew :core:domain:test :core:data:testDebugUnitTest`,
  `./gradlew :feature:home:testDebugUnitTest :app:testDebugUnitTest` GREEN. 실기기 GPS 주행과 서버 방문
  인증 연동, API 35 알림 렌더링은 별도 확인이 필요하다.

## Follow-up Result — 측정 종료와 알림 아이콘 정합성

- Behavior: 측정 종료 요청에 세션 상태가 메모리에 없더라도 DataStore 세션, Foreground Service,
  동일 notification ID를 모두 정리한다. 다른 코스 선택 팝업에서 `측정 종료`를 누르면 대기 중인
  새 코스를 자동으로 다시 시작하지 않는다.
- Notification: 적응형 앱 아이콘의 큰 투명 여백 때문에 작게 보이던 작은 아이콘을 전용 단색 Rodi 벡터로
  교체했다. 시스템이 정하는 알림 아이콘 최대 크기 안에서 브랜드 마크가 더 크게 보인다.
- Changed files: `app/src/main/java/com/dororong/rodi/spike/driving/DrivingTrackingService.kt`,
  `app/src/main/java/com/dororong/rodi/spike/driving/DrivingNotificationFactory.kt`,
  `app/src/main/res/drawable/ic_notification_rodi.xml`, `feature/home/.../HomeScreen.kt`
- Verification: `./gradlew :app:assembleDebug` GREEN. 실제 SM-M446K(Android 16) 설치는 완료했으며,
  운전 시작·종료와 제조사별 아이콘 렌더링은 실기기에서 추가 확인이 필요하다.
