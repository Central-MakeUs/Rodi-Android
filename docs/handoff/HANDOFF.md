# HANDOFF — HomeScreen.kt 컴포넌트 분리 + MVI Contract(Intent/State/Effect) 도입

> Claude(기획)와 Codex(구현)가 주고받는 **단일 활성 작업 채널**.
> 완료되면 `docs/handoff/archive/<날짜>-<작업>.md`로 옮기고 이 파일은 다음 작업으로 비운다.

Status: READY_FOR_IMPL
Branch: refactor/home-mvi-contract

## Context (왜)
`feature/home` 모듈의 `HomeScreen.kt`가 2018줄짜리 단일 파일에 최상위 화면 오케스트레이션 +
30개 이상의 private Composable + 포맷팅 유틸 함수가 전부 들어있다. `HomeViewModel`도 `UiState`
하나와 개별 public 함수(`onCourseClick`, `onDismissDetail` 등)로만 구성돼 있어 Intent/Effect
구분이 없다. 다음 예정 작업(Hilt 도입, Repository/UseCase 계층화)을 진행하려면 먼저 화면의
책임 경계(Contract)가 명확해야 어디에 DI를 꽂을지 판단할 수 있다. 이번 작업은 **동작 변경 없이**
구조만 정리한다.

## Spec (무엇을·어떻게)

### 1. 파일 분리 — `feature/home/src/main/java/com/dororong/rodi/feature/home/`

기존 `HomeScreen.kt`(2018줄) 안의 함수들을 아래 표대로 새 파일로 옮긴다. **각 함수의 내부
로직은 한 글자도 바꾸지 않는다** — 파일 위치와 필요한 import만 옮긴다. `private` 접근제한자는
같은 패키지 내에서는 그대로 유지 가능하므로 유지한다(패키지가 바뀌지 않으므로 문제없음).

| 새 파일 | 옮길 함수/타입 (기존 HomeScreen.kt 라인 기준) |
|---|---|
| `HomeScreen.kt` (기존 파일 유지) | `HomeScreen` 최상위 컴포저블만 남김. 상수(`DEFAULT_ZOOM`, `HOME_PREFS`, `KEY_HAS_LOADED_MAP`, `SEOUL`, `hasLoadedMapInSession`, `MapScreenState` enum)와 `hasLoadedMapBefore()`/`markMapLoaded()` 확장함수는 `HomeMapState.kt`로 이동 |
| `HomeMapState.kt` (신규) | `MapScreenState` enum, `DEFAULT_ZOOM`/`SEOUL`/`HOME_PREFS`/`KEY_HAS_LOADED_MAP` 상수, `hasLoadedMapInSession` 최상위 var, `Context.hasLoadedMapBefore()`, `Context.markMapLoaded()` |
| `components/MapStatusScreens.kt` (신규) | `MapLoadingScreen`, `MapNetworkErrorScreen`, `RodiLoadingIndicator`, `RodiNetworkSnackbar`, `SnackbarAlertIcon`, `Modifier.consumeTouches()` + 각각의 `@Preview` |
| `components/MapControlButtons.kt` (신규) | `DistanceFilterBar`, `MyLocationButton`, `SettingsButton` + 각 `@Preview` |
| `components/SettingsTermsScreen.kt` (신규) | `SettingsTermsScreen` |
| `components/CourseListContent.kt` (신규) | `CourseListContent`, `CourseEmptyContent`, `CourseCard` + `CourseListContentPreview`, `CourseCardPreview` |
| `components/CourseDetailContent.kt` (신규) | `CourseDetailContent`, `StableMeasuredDetailSheet` + `CourseDetailContentPreview` |
| `components/ParkingDetailContent.kt` (신규) | `ParkingDetailContent`, `ParkingMetaRow`, `ParkingCapacityRow`, `ParkingHoursRows`, `ParkingFeeSection`, `ParkingInfoRow`, `DashedInfoDivider`, `ParkingFeeInfo`(data class), `ParkingDetail?.parkingTypeDisplay()`, `ParkingDetail?.operatingSummary()`, `String?.toDisplayHours()`, `ParkingDetail?.toParkingFeeInfo()`, `String?.extractFeeNumber()`, `formatFee()` |
| `components/DetailCommonRows.kt` (신규) | `RatingRegionRow`, `ExpandableAddressCard`, `TagRow`, `DifficultyTag`, `PracticeTagChip`, `SummaryBox`, `VerticalStepList` + `VerticalStepListPreview` |
| `HomeFormatters.kt` (신규) | `distanceText()`, `String.stripCityPrefix()`, `String.stripCityAndDistrict()`, `Waypoint.displayStepAddress()`, `String.toRoadNameWithNumberOrShortName()`, `String.shortenRoadAddress()`, `String.shortenCommaRoad()`, `String.shortenJibunAddress()` |
| `components/BottomSheetPreviewWrapper.kt` (신규) | `BottomSheetPreviewWrapper` (Preview 전용 wrapper, `MyLocationButtonPreview`/`DistanceFilterBarPreview`/`MapLoadingScreenPreview`/`MapNetworkErrorScreenPreview`는 각자 옮겨간 파일에 남기고, `BottomSheetPreviewWrapper`를 쓰는 Preview들만 이 파일 또는 해당 컴포넌트 파일에 유지) |

이동 후 각 파일의 `import`는 실제 사용하는 것만 남긴다(미사용 import 경고 없게).
`HomeScreen.kt`는 최상위 `HomeScreen()` 컴포저블 + 그 함수가 직접 참조하는 것들만 남고,
나머지는 위 표의 파일에서 `import`해서 쓴다.

### 2. MVI Contract 도입 — `HomeContract.kt` (신규)

`feature/home/src/main/java/com/dororong/rodi/feature/home/HomeContract.kt` 파일을 만들고
아래 3가지를 정의한다.

```kotlin
package com.dororong.rodi.feature.home

import com.dororong.rodi.core.data.navi.NaviApp
import com.dororong.rodi.core.domain.Course

sealed interface HomeIntent {
    data class OnCourseClick(val id: Int) : HomeIntent
    data object OnDismissDetail : HomeIntent
    data class OnDistanceFilterChange(val km: Int?) : HomeIntent
    data class OnLocationUpdate(val lat: Double, val lng: Double) : HomeIntent

    /**
     * 경로 안내 버튼 클릭. 설치 여부/저장된 선호 앱 조회는 Context가 필요해 Composable이
     * 미리 계산해서 함께 넘긴다 — ViewModel은 순수 분기 로직만 담당한다.
     */
    data class OnNavigateClick(
        val course: Course,
        val savedApp: NaviApp?,
        val kakaoMapInstalled: Boolean,
        val kakaoNaviInstalled: Boolean,
    ) : HomeIntent

    /** NaviPickerSheet(SELECT 모드)에서 앱을 골랐을 때. */
    data class OnNaviAppSelected(val app: NaviApp, val course: Course, val always: Boolean) : HomeIntent

    /** NaviPickerSheet(INSTALL 모드)에서 앱을 골랐을 때(설치 페이지로 이동). */
    data class OnInstallNaviAppSelected(val app: NaviApp) : HomeIntent
}

sealed interface HomeEffect {
    data class LaunchKakaoMap(val course: Course) : HomeEffect
    data class LaunchKakaoNavi(val course: Course) : HomeEffect
    data class ShowNaviPicker(val course: Course) : HomeEffect
    data class ShowInstallNaviPicker(val course: Course) : HomeEffect
    data class OpenNaviInstallPage(val app: NaviApp) : HomeEffect
    data class SaveNaviPreference(val app: NaviApp) : HomeEffect
}
```

`HomeViewModel.UiState`는 이름을 바꾸지 않고 그대로 둔다(이미 State 역할을 하고 있고, 이름
변경은 이번 스코프 밖 — 아래 Out of scope 참고).

### 3. `HomeViewModel.kt` 수정

- Effect 채널 추가:
  ```kotlin
  private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
  val effect: Flow<HomeEffect> = _effect.receiveAsFlow()
  ```
- 기존 public 함수(`onCourseClick`, `onDismissDetail`, `onDistanceFilterChange`,
  `onLocationUpdate`)는 `private`로 가시성을 낮추고, **새로 `fun onIntent(intent: HomeIntent)`를
  유일한 public 진입점으로 추가**한다. 내부에서 `when (intent)`로 분기해 각 private 함수를 호출한다.
- `OnNavigateClick` 처리 로직 (기존 `HomeScreen.kt`의 `navigate()` 로컬 함수 안에 있던
  분기를 그대로 옮겨온다):
  ```kotlin
  private fun onNavigateClick(intent: HomeIntent.OnNavigateClick) {
      viewModelScope.launch {
          when {
              intent.savedApp == NaviApp.KAKAOMAP && intent.kakaoMapInstalled ->
                  _effect.send(HomeEffect.LaunchKakaoMap(intent.course))
              intent.savedApp == NaviApp.KAKAONAVI && intent.kakaoNaviInstalled ->
                  _effect.send(HomeEffect.LaunchKakaoNavi(intent.course))
              intent.kakaoMapInstalled && intent.kakaoNaviInstalled ->
                  _effect.send(HomeEffect.ShowNaviPicker(intent.course))
              intent.kakaoMapInstalled -> _effect.send(HomeEffect.LaunchKakaoMap(intent.course))
              intent.kakaoNaviInstalled -> _effect.send(HomeEffect.LaunchKakaoNavi(intent.course))
              else -> _effect.send(HomeEffect.ShowInstallNaviPicker(intent.course))
          }
      }
  }
  ```
- `OnNaviAppSelected` 처리: `always`가 true면 `HomeEffect.SaveNaviPreference(app)`을 먼저
  send하고, `app`에 따라 `LaunchKakaoMap`/`LaunchKakaoNavi` Effect를 send (기존
  `HomeScreen.kt`의 `naviCourse?.let { ... onSelect = { app, always -> ... } }` 블록 로직 이관).
- `OnInstallNaviAppSelected` 처리: `app`에 따라 `HomeEffect.OpenNaviInstallPage(app)` send
  (기존 `installNaviCourse?.let { ... onSelect = { app, _ -> ... } }` 블록 로직 이관).

### 4. `HomeScreen.kt` 호출부 수정

- 기존에 `vm.onCourseClick(id)`, `vm.onDismissDetail()`, `vm.onDistanceFilterChange(km)`,
  `vm.onLocationUpdate(lat, lng)`로 직접 호출하던 부분을 전부
  `vm.onIntent(HomeIntent.OnCourseClick(id))` 형태로 교체한다.
- 기존 `navigate: () -> Unit` 로컬 람다(카카오맵/내비 설치 확인 + 분기)는 그대로 두되,
  마지막에 직접 `KakaoMapLauncher.launch(...)`/`naviCourse = selectedCourse`처럼 실행하던
  대신, `vm.onIntent(HomeIntent.OnNavigateClick(course, NaviPreference.getAlways(context), kakaoMapInstalled, kakaoNaviInstalled))`
  를 호출하도록 바꾼다. 즉 **설치 여부 확인(Context 필요)까지는 Composable에 남기고, 그
  다음 "무엇을 할지 결정"은 ViewModel로 넘긴다.**
- `naviCourse`/`installNaviCourse` 로컬 `remember` state는 유지한다(Effect를 받아서 다이얼로그
  표시 여부를 이 로컬 state로 제어하는 것은 정상 패턴). 대신 이 state를 채우는 주체가
  "설치 여부를 직접 계산하는 로컬 함수"에서 "ViewModel이 보낸 Effect"로 바뀐다.
- `HomeScreen()` 함수 안, 다른 `LaunchedEffect`들 옆에 아래를 추가:
  ```kotlin
  LaunchedEffect(Unit) {
      vm.effect.collect { effect ->
          when (effect) {
              is HomeEffect.LaunchKakaoMap -> KakaoMapLauncher.launch(context, effect.course)
              is HomeEffect.LaunchKakaoNavi -> KakaoNaviLauncher.launch(context, effect.course)
              is HomeEffect.ShowNaviPicker -> naviCourse = effect.course
              is HomeEffect.ShowInstallNaviPicker -> installNaviCourse = effect.course
              is HomeEffect.OpenNaviInstallPage -> when (effect.app) {
                  NaviApp.KAKAOMAP -> KakaoMapLauncher.openInstallPage(context)
                  NaviApp.KAKAONAVI -> KakaoNaviLauncher.openInstallPage(context)
              }
              is HomeEffect.SaveNaviPreference -> NaviPreference.setAlways(context, effect.app)
          }
      }
  }
  ```
- `NaviPickerSheet`의 `onSelect` 콜백들도 `vm.onIntent(HomeIntent.OnNaviAppSelected(app, course, always))`,
  `vm.onIntent(HomeIntent.OnInstallNaviAppSelected(app))` 호출로 교체.

## Files to touch
- `feature/home/src/main/java/com/dororong/rodi/feature/home/HomeScreen.kt` (대폭 축소)
- `feature/home/src/main/java/com/dororong/rodi/feature/home/HomeViewModel.kt`
- `feature/home/src/main/java/com/dororong/rodi/feature/home/HomeContract.kt` (신규)
- `feature/home/src/main/java/com/dororong/rodi/feature/home/HomeMapState.kt` (신규)
- `feature/home/src/main/java/com/dororong/rodi/feature/home/HomeFormatters.kt` (신규)
- `feature/home/src/main/java/com/dororong/rodi/feature/home/components/*.kt` (신규 다수, 위 표 참고)

## Acceptance criteria
- [ ] `HomeScreen.kt`가 최상위 `HomeScreen()` 컴포저블 위주로 대폭 줄어든다 (대략 300~450줄
      수준 — 지도/시트 오케스트레이션 + Effect 수집 로직만 남음)
- [ ] 위 표에 나열된 모든 함수/타입이 지정된 새 파일로 이동했고, 원본 로직은 한 글자도
      바뀌지 않았다 (순수 이동)
- [ ] `HomeContract.kt`에 `HomeIntent`/`HomeEffect` sealed interface가 정의돼 있다
- [ ] `HomeViewModel`의 public 진입점이 `onIntent(HomeIntent)` 하나뿐이다 (기존
      `onCourseClick` 등은 `private`)
- [ ] `HomeScreen.kt`의 모든 `vm.xxx(...)` 직접 호출이 `vm.onIntent(HomeIntent.Xxx(...))`로
      교체됐다
- [ ] 카카오맵/카카오내비 실행, 설치 페이지 이동, 선호 앱 저장이 전부 `HomeEffect`를 거쳐서
      실행된다 (Composable이 `vm.effect`를 collect하는 지점 한 곳에서만 처리)
- [ ] 지도 카메라 정렬, 코스/주차장 상세 시트, 거리 필터, 위치 권한 플로우 등 **기존 동작이
      전혀 바뀌지 않는다** (이번 작업은 순수 구조 개선 — 버그 수정이나 UX 변경 없음)
- [ ] `./gradlew assembleDebug` 성공
- [ ] `./gradlew :feature:home:lint` 경고 없음 (미사용 import 등)

## Verification
```
./gradlew assembleDebug
./gradlew :feature:home:lint
```
빌드 성공 후, 에뮬레이터에서 다음을 수동으로 확인(Claude가 검토 단계에서 수행):
- 코스 리스트 → 코스 클릭 → 상세 시트 → 경로 안내 버튼 → (카카오맵/내비 둘 다 설치 시) 선택
  다이얼로그 → 특정 앱 선택 시 실제 실행되는지
- 주차장 코스 클릭 → 상세 시트 → 경로 안내 버튼도 동일하게 동작하는지
- 거리 필터(3/5/10km) 정상 동작
- 설정 → 약관 화면 진입/복귀

## Out of scope
- `HomeViewModel.UiState`의 이름 변경(`HomeState`로 리네이밍 등) — 이번엔 하지 않는다
- Hilt 도입, Repository/UseCase 계층 분리 — 다음 작업으로 별도 진행
- 카카오맵/카카오내비 설치 확인·`NaviPreference` 자체를 인터페이스로 추상화하는 것 — Context
  의존성 완전 제거는 Hilt 도입 시점에 다시 다룬다
- 지도 카메라 정렬/바텀시트 관련 버그 수정 — 이미 별도 브랜치(`fix/map-camera-and-sheet-ux`,
  PR 진행 중)에서 처리했으므로 이 작업에서 다시 건드리지 않는다
- 신규 기능 추가, UI 디자인 변경 없음

---
## Codex Result   <!-- Codex가 구현 후 채움 → Status=IMPL_DONE (또는 막히면 BLOCKED) -->
- Changed files:
- Build/test:
- Open questions:

---
## Claude Review  <!-- Claude가 검토 후 채움 -->
- Blocking:
- Nits:
- Verdict:   <!-- APPROVE | NEEDS_CHANGES -->
