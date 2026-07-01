# HANDOFF — :app 코드 이관 Phase 1 (model→core:domain, data/navi-pref/directions→core:data, ui/theme→core:ui)

> Claude(기획)와 Codex(구현)가 주고받는 **단일 활성 작업 채널**.
> 완료되면 `docs/handoff/archive/<날짜>-<작업>.md`로 옮기고 이 파일은 다음 작업으로 비운다.

Status: DONE
Branch: feature/multimodule-scaffold

## Context (왜)
`:app` 코드를 `core:*`/`feature:*` 모듈로 옮기는 실제 마이그레이션을 시작한다. 전체 이관을 한
번에 하면 리소스 분할(entry/home 화면 각각의 drawable)과 신규 `:feature:entry` 모듈 신설까지
얽혀 스펙이 비대해지므로 **2단계로 나눈다**:

- **Phase 1 (이번 작업)**: 다른 패키지에 의존하지 않는 하위 계층부터 — `model`→`:core:domain`,
  `data`(EntryPreferences/SampleCourses)+`navi`의 `NaviPreference`+`directions`→`:core:data`,
  `ui/theme`→`:core:ui`. 리소스 이동은 폰트 4개뿐이라 충돌 위험이 낮다.
- **Phase 2 (후속, BACKLOG 기록 예정)**: `entry`/`home`/`map`/`navi`의 런처/`location`을
  `:feature:entry`(신규)·`:feature:home`으로 이관. drawable 리소스 분할과 `:feature:entry` 신설
  이 필요해 별도 스펙으로 진행.

이번 스펙은 **패키지 이동 + import 경로 수정만** 한다. 로직·동작은 1바이트도 바꾸지 않는다
(순수 `git mv` + 패키지 선언/import 문 치환).

## Spec (무엇을·어떻게)

### 1. `:core:domain` — `model` 패키지 전체 이동
- `app/src/main/java/com/dororong/rodi/model/Course.kt`
  → `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/Course.kt` (`git mv`)
  - 1번째 줄 `package com.dororong.rodi.model` → `package com.dororong.rodi.core.domain`
  - 파일 내용(코드) 그 외 변경 없음
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/.gitkeep` 삭제
- `app/src/main/java/com/dororong/rodi/model/` 디렉터리 삭제(비게 됨)

### 2. `:core:data` — `data`/`directions`/`navi.NaviPreference` 이동
- `app/src/main/java/com/dororong/rodi/data/EntryPreferences.kt`
  → `core/data/src/main/java/com/dororong/rodi/core/data/EntryPreferences.kt`
  - `package com.dororong.rodi.data` → `package com.dororong.rodi.core.data`
- `app/src/main/java/com/dororong/rodi/data/SampleCourses.kt`
  → `core/data/src/main/java/com/dororong/rodi/core/data/SampleCourses.kt`
  - `package com.dororong.rodi.data` → `package com.dororong.rodi.core.data`
  - `import com.dororong.rodi.model.*` 10줄 → `import com.dororong.rodi.core.domain.*` 로 치환
    (Course, CourseFeatures, OperatingHours, ParkingDetail, RodiItem, RodiItemType, RouteDetail,
    RoutePoint, Waypoint, WaypointType)
- `app/src/main/java/com/dororong/rodi/data/` 디렉터리 삭제(비게 됨)
- `app/src/main/java/com/dororong/rodi/navi/NaviPreference.kt`
  → `core/data/src/main/java/com/dororong/rodi/core/data/navi/NaviPreference.kt`
  - `package com.dororong.rodi.navi` → `package com.dororong.rodi.core.data.navi`
  - 내용(코드) 변경 없음 (이 파일은 다른 rodi 패키지를 import하지 않음)
- `app/src/main/java/com/dororong/rodi/directions/KakaoDirectionsClient.kt`
  → `core/data/src/main/java/com/dororong/rodi/core/data/directions/KakaoDirectionsClient.kt`
  - `package com.dororong.rodi.directions` → `package com.dororong.rodi.core.data.directions`
  - `import com.dororong.rodi.model.Course` → `import com.dororong.rodi.core.domain.Course`
  - `import com.dororong.rodi.model.CoursePoint` → `import com.dororong.rodi.core.domain.CoursePoint`
  - `import com.dororong.rodi.BuildConfig` → `import com.dororong.rodi.core.data.BuildConfig`
    (아래 5번에서 core:data 모듈에 자체 BuildConfig 필드를 만든다)
- `app/src/main/java/com/dororong/rodi/directions/` 디렉터리 삭제(비게 됨)
- `core/data/src/main/java/com/dororong/rodi/core/data/.gitkeep` 삭제

### 3. `:core:ui` — `ui/theme` 패키지 전체 이동
- `app/src/main/java/com/dororong/rodi/ui/theme/{Color,RodiColors,RodiDimens,RodiTheme,RodiTypography}.kt`
  → `core/ui/src/main/java/com/dororong/rodi/core/ui/theme/{동일 파일명}` (5개 파일, `git mv`)
  - 각 파일 1번째 줄 `package com.dororong.rodi.ui.theme` → `package com.dororong.rodi.core.ui.theme`
  - `RodiTypography.kt`의 `import com.dororong.rodi.R` → `import com.dororong.rodi.core.ui.R`
    (폰트 리소스가 아래 4번에서 `:core:ui`로 함께 이동하므로 R 클래스도 `:core:ui`의 것을 참조)
  - 그 외 내용 변경 없음
- `core/ui/src/main/java/com/dororong/rodi/core/ui/.gitkeep` 삭제

### 4. 폰트 리소스 이동
- `app/src/main/res/font/{pretendard_bold,pretendard_medium,pretendard_regular,pretendard_semibold}.ttf`
  → `core/ui/src/main/res/font/{동일 파일명}` (`git mv`, 4개 파일)
- `app/src/main/res/font/` 디렉터리 삭제(비게 됨)

### 5. `core/data/build.gradle.kts` — BuildConfig 필드 + 의존성 추가
`KakaoDirectionsClient`가 `:app`의 `BuildConfig`를 더 이상 참조할 수 없으므로(역방향 의존 금지),
`:core:data`가 `local.properties`에서 직접 읽어 자체 `BuildConfig` 필드를 만든다
(`app/build.gradle.kts`의 기존 패턴과 동일):

```kotlin
import java.util.Properties

plugins {
    id("dororong.rodi.android.library")
}

val localProperties = Properties().apply {
    val localProps = rootProject.file("local.properties")
    if (localProps.exists()) localProps.inputStream().use { load(it) }
}
val kakaoRestApiKey: String = localProperties.getProperty("KAKAO_REST_API_KEY", "")

android {
    namespace = "com.dororong.rodi.core.data"
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        buildConfigField("String", "KAKAO_REST_API_KEY", "\"$kakaoRestApiKey\"")
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kakao.maps)
}
```

### 6. `core/ui/build.gradle.kts` — Compose 의존성 추가
```kotlin
plugins {
    id("dororong.rodi.android.library.compose")
}

android {
    namespace = "com.dororong.rodi.core.ui"
}

dependencies {
    implementation(project(":core:common"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
}
```

### 7. `app/build.gradle.kts` 수정
- `dependencies` 블록에 추가:
  ```kotlin
  implementation(project(":core:domain"))
  implementation(project(":core:data"))
  implementation(project(":core:ui"))
  ```
- `implementation(libs.androidx.datastore.preferences)` 줄 **삭제** — DataStore 사용처
  (`EntryPreferences`)가 `:core:data`로 옮겨져 `:app`에서 더 이상 직접 쓰지 않음
  (`:core:data`가 자체 `implementation` 의존성으로 가짐, `:app`엔 전이되지 않음 — 의도된 캡슐화)
- `kakaoRestApiKey` 로컬 변수 선언과 `buildConfigField("String", "KAKAO_REST_API_KEY", ...)` 줄
  **삭제** — 5번에서 `:core:data`가 자체 필드를 갖게 되어 `:app`에서는 더 이상 안 씀
  (`KAKAO_NATIVE_APP_KEY` 관련 줄은 그대로 유지 — `RodiApplication`이 계속 씀)

### 8. `:app` 나머지 파일의 import 경로 수정 (로직 변경 없음, import 문만)
아래 표대로 **정확히** 치환한다. 각 파일에서 해당 import만 바뀌고 그 외 코드는 동일해야 한다.

| 파일 | 바꿀 import |
|---|---|
| `home/HomeScreen.kt` | `com.dororong.rodi.data.SampleCourses` → `com.dororong.rodi.core.data.SampleCourses`; `com.dororong.rodi.directions.KakaoDirectionsClient.RouteResult` → `com.dororong.rodi.core.data.directions.KakaoDirectionsClient.RouteResult`; `com.dororong.rodi.model.{Course,Difficulty,ParkingDetail,PracticeTag,Waypoint}` → `com.dororong.rodi.core.domain.{동일}`; `com.dororong.rodi.navi.{KakaoMapLauncher,KakaoNaviLauncher,NaviApp}` 중 `NaviApp`만 → `com.dororong.rodi.core.data.navi.NaviApp` (`KakaoMapLauncher`/`KakaoNaviLauncher`는 그대로 `com.dororong.rodi.navi`, 이번에 안 옮김); `com.dororong.rodi.navi.NaviPreference` → `com.dororong.rodi.core.data.navi.NaviPreference`; `com.dororong.rodi.ui.theme.RodiTheme` → `com.dororong.rodi.core.ui.theme.RodiTheme` |
| `home/HomeViewModel.kt` | `com.dororong.rodi.data.SampleCourses` → `com.dororong.rodi.core.data.SampleCourses`; `com.dororong.rodi.directions.KakaoDirectionsClient`(+`.RouteResult`) → `com.dororong.rodi.core.data.directions.KakaoDirectionsClient`(+`.RouteResult`); `com.dororong.rodi.model.Course` → `com.dororong.rodi.core.domain.Course` |
| `home/NaviPickerSheet.kt` | `com.dororong.rodi.navi.NaviApp` → `com.dororong.rodi.core.data.navi.NaviApp`; `com.dororong.rodi.ui.theme.RodiTheme` → `com.dororong.rodi.core.ui.theme.RodiTheme` |
| `entry/EntryViewModel.kt` | `com.dororong.rodi.data.EntryPreferences` → `com.dororong.rodi.core.data.EntryPreferences` |
| `entry/DrivingPrecautionsContent.kt` | `com.dororong.rodi.ui.theme.{RodiRadius,RodiSpacing,RodiTheme}` → `com.dororong.rodi.core.ui.theme.{동일}` |
| `entry/EntryComponents.kt` | `com.dororong.rodi.ui.theme.{RodiRadius,RodiSpacing,RodiTheme}` → `com.dororong.rodi.core.ui.theme.{동일}` |
| `entry/LocationPermissionContent.kt` | `com.dororong.rodi.ui.theme.RodiTheme` → `com.dororong.rodi.core.ui.theme.RodiTheme` |
| `entry/TermsAgreementContent.kt` | `com.dororong.rodi.ui.theme.{RodiRadius,RodiSpacing,RodiTheme}` → `com.dororong.rodi.core.ui.theme.{동일}` |
| `entry/TermsWebView.kt` | `com.dororong.rodi.ui.theme.RodiTheme` → `com.dororong.rodi.core.ui.theme.RodiTheme` |
| `map/CourseRouteRenderer.kt` | `com.dororong.rodi.model.Course` → `com.dororong.rodi.core.domain.Course` (이 파일의 `R.drawable.*` 참조는 `:app` 소유 리소스라 그대로 유지) |
| `navi/KakaoMapLauncher.kt` | `com.dororong.rodi.model.{Course,CoursePoint}` → `com.dororong.rodi.core.domain.{동일}` |
| `navi/KakaoNaviLauncher.kt` | `com.dororong.rodi.model.Course` → `com.dororong.rodi.core.domain.Course` |
| `MainActivity.kt` | `com.dororong.rodi.ui.theme.RodiTheme` → `com.dororong.rodi.core.ui.theme.RodiTheme` |
| `ui/AppRoot.kt` | `com.dororong.rodi.data.EntryPreferences` → `com.dororong.rodi.core.data.EntryPreferences`; `com.dororong.rodi.ui.theme.RodiTheme` → `com.dororong.rodi.core.ui.theme.RodiTheme` |

### 9. `docs/PROJECT.md` 패키지 맵 갱신
"패키지 맵" 표에서 `model`/`data`/`directions` 행을 삭제하고, `navi`/`ui` 행 설명을 아래로 교체,
표 아래에 "모듈 맵" 표를 새로 추가한다:

```markdown
| `navi` | 외부 내비 런처(카카오맵·카카오내비). 선호 저장(`NaviPreference`)은 `:core:data`로 이동 |
| `ui` | `AppRoot`(게이트→홈 분기). 테마 토큰(`RodiTheme`)은 `:core:ui`로 이동 |
```

새 섹션(패키지 맵 표 바로 아래):
```markdown
## 모듈 맵
| 모듈 | 역할 |
|---|---|
| `:core:domain` | 도메인 모델(`Course` 등) |
| `:core:data` | `EntryPreferences`(DataStore), `SampleCourses`, `KakaoDirectionsClient`(REST), `NaviPreference` |
| `:core:ui` | `RodiTheme` 토큰(colors/typography/spacing/radius) |
| `:core:common` | 확장함수/유틸 (아직 비어있음) |
| `:feature:home` | (아직 비어있음, Phase 2 예정) |
```

## Files to touch
- `git mv`: `model/Course.kt`, `data/EntryPreferences.kt`, `data/SampleCourses.kt`,
  `navi/NaviPreference.kt`, `directions/KakaoDirectionsClient.kt`, `ui/theme/*.kt`(5개),
  `res/font/*.ttf`(4개) — 각각 위 스펙의 목적지 경로로
- 삭제: `core/domain/.../.gitkeep`, `core/data/.../.gitkeep`, `core/ui/.../.gitkeep`,
  이제 빈 `app/.../model/`, `app/.../data/`, `app/.../directions/`, `app/src/main/res/font/` 디렉터리
- `core/domain/build.gradle.kts` — 변경 없음(이미 `core:common` 의존, 그대로 유지)
- `core/data/build.gradle.kts`, `core/ui/build.gradle.kts` — 5, 6번 스펙대로 교체
- `app/build.gradle.kts` — 7번 스펙대로 수정
- import 경로만 수정: `home/HomeScreen.kt`, `home/HomeViewModel.kt`, `home/NaviPickerSheet.kt`,
  `entry/EntryViewModel.kt`, `entry/DrivingPrecautionsContent.kt`, `entry/EntryComponents.kt`,
  `entry/LocationPermissionContent.kt`, `entry/TermsAgreementContent.kt`, `entry/TermsWebView.kt`,
  `map/CourseRouteRenderer.kt`, `navi/KakaoMapLauncher.kt`, `navi/KakaoNaviLauncher.kt`,
  `MainActivity.kt`, `ui/AppRoot.kt`
- `docs/PROJECT.md`

## Acceptance criteria
- [ ] `git mv`로 이동해 각 파일의 git history가 보존됨(`git log --follow`로 확인 가능)
- [ ] 이동한 파일들은 **패키지 선언/식별된 import 외 diff 없음** (로직·포맷팅 변경 없음)
- [ ] `app/src/main/java/com/dororong/rodi/{model,data,directions}/` 디렉터리가 더 이상 존재하지 않음
- [ ] `app/src/main/res/font/` 디렉터리가 더 이상 존재하지 않음
- [ ] `core/domain`·`core/data`·`core/ui`의 `.gitkeep`이 삭제됨(실제 소스가 생겼으므로)
- [ ] `./gradlew assembleDebug` 성공
- [ ] `./gradlew :core:common:build :core:domain:build :core:data:build :core:ui:build :feature:home:build` 성공
- [ ] `docs/PROJECT.md` 패키지 맵·모듈 맵이 스펙대로 갱신됨
- [ ] `entry/*`, `home/*`, `map/*`, `navi/KakaoMapLauncher.kt`, `navi/KakaoNaviLauncher.kt`,
      `location/*`, `ui/AppRoot.kt`, `MainActivity.kt`, `RodiApplication.kt`는 여전히 `:app`에 있음
      (Phase 2 범위 아님)

## Verification
```
./gradlew assembleDebug
./gradlew :core:common:build :core:domain:build :core:data:build :core:ui:build :feature:home:build
git status --short app/src/main/java/com/dororong/rodi/model app/src/main/java/com/dororong/rodi/data app/src/main/java/com/dororong/rodi/directions
```

## Out of scope
- Phase 2: `entry`/`home`/`map`/`navi` 런처/`location`을 `:feature:entry`(신규)·`:feature:home`으로
  이관, drawable 리소스 분할 (BACKLOG.md에 후속 항목으로 기록 예정)
- `:app`이 `:core:ui`의 컴포넌트를 실제로 재사용하도록 리팩터링(지금은 그대로 이동만)
- Hilt 도입, Repository 인터페이스/UseCase 계층화(현재는 객체·클래스 그대로 이동만)
- `SampleCourses.kt`를 실제 API 연동으로 교체 (현재도 목데이터, 이번엔 위치만 이동)
- Kotlin/AGP/Kakao SDK 버전 업그레이드 (이미 BACKLOG.md에 별도 기록됨)

---
## Codex Result   <!-- Codex가 구현 후 채움 → Status=IMPL_DONE (또는 막히면 BLOCKED) -->
- Changed files: app/build.gradle.kts, app/src/main/java/com/dororong/rodi/MainActivity.kt, app/src/main/java/com/dororong/rodi/entry/DrivingPrecautionsContent.kt, app/src/main/java/com/dororong/rodi/entry/EntryComponents.kt, app/src/main/java/com/dororong/rodi/entry/EntryViewModel.kt, app/src/main/java/com/dororong/rodi/entry/LocationPermissionContent.kt, app/src/main/java/com/dororong/rodi/entry/TermsAgreementContent.kt, app/src/main/java/com/dororong/rodi/entry/TermsWebView.kt, app/src/main/java/com/dororong/rodi/home/HomeScreen.kt, app/src/main/java/com/dororong/rodi/home/HomeViewModel.kt, app/src/main/java/com/dororong/rodi/home/NaviPickerSheet.kt, app/src/main/java/com/dororong/rodi/map/CourseRouteRenderer.kt, app/src/main/java/com/dororong/rodi/navi/KakaoMapLauncher.kt, app/src/main/java/com/dororong/rodi/navi/KakaoNaviLauncher.kt, app/src/main/java/com/dororong/rodi/ui/AppRoot.kt, core/domain/src/main/kotlin/com/dororong/rodi/core/domain/Course.kt, core/data/build.gradle.kts, core/data/src/main/java/com/dororong/rodi/core/data/EntryPreferences.kt, core/data/src/main/java/com/dororong/rodi/core/data/SampleCourses.kt, core/data/src/main/java/com/dororong/rodi/core/data/directions/KakaoDirectionsClient.kt, core/data/src/main/java/com/dororong/rodi/core/data/navi/NaviPreference.kt, core/ui/build.gradle.kts, core/ui/src/main/java/com/dororong/rodi/core/ui/theme/Color.kt, core/ui/src/main/java/com/dororong/rodi/core/ui/theme/RodiColors.kt, core/ui/src/main/java/com/dororong/rodi/core/ui/theme/RodiDimens.kt, core/ui/src/main/java/com/dororong/rodi/core/ui/theme/RodiTheme.kt, core/ui/src/main/java/com/dororong/rodi/core/ui/theme/RodiTypography.kt, core/ui/src/main/res/font/pretendard_bold.ttf, core/ui/src/main/res/font/pretendard_medium.ttf, core/ui/src/main/res/font/pretendard_regular.ttf, core/ui/src/main/res/font/pretendard_semibold.ttf, docs/PROJECT.md, docs/handoff/HANDOFF.md
- Build/test: `./gradlew assembleDebug` GREEN; `./gradlew :core:common:build :core:domain:build :core:data:build :core:ui:build :feature:home:build` GREEN; `app/src/main/java/com/dororong/rodi/{model,data,directions}/`, `app/src/main/res/font/`, `app/src/main/java/com/dororong/rodi/ui/theme/` directories removed; `core/domain`, `core/data`, `core/ui` `.gitkeep` removed.
- Open questions: none

---
## Claude Review  <!-- Claude가 검토 후 채움 -->
- Blocking: 없음
- Nits:
  - `core/domain/.../Course.kt`의 KDoc이 여전히 옛 경로를 참조: `[com.dororong.rodi.data.SampleCourses]`
    → `[com.dororong.rodi.core.data.SampleCourses]`로 갱신 필요(깨진 문서 링크, 컴파일에는 영향 없음).
  - `core/ui/.../RodiTypography.kt` 13번째 줄 주석 `app/src/main/res/font/ 에 4 weight 배치 필요`가
    옛 경로 — `core/ui/src/main/res/font/`로 갱신 필요. 스펙에 명시한 "import 문만" 범위 밖이라
    Codex 잘못은 아니고, 다음 정리 때 같이 고치면 됨.
- Verdict: APPROVE

재검증 결과 (2026-07-01):
- 이동한 13개 파일(Course.kt, EntryPreferences.kt, SampleCourses.kt, NaviPreference.kt,
  KakaoDirectionsClient.kt, ui/theme 5개, 폰트 4개) 모두 `git diff -M`으로 확인 — 패키지 선언/
  스펙에 명시한 import 외 diff 없음
- import만 수정한 14개 파일(`home/HomeScreen.kt` 등) 모두 스펙의 표와 정확히 일치, 그 외 코드 변경 없음
- `app/build.gradle.kts`: `core:domain`/`core:data`/`core:ui` 의존성 추가, `datastore.preferences`·
  `kakaoRestApiKey`/`KAKAO_REST_API_KEY` buildConfigField 삭제 — 스펙과 일치
- `core/data/build.gradle.kts`·`core/ui/build.gradle.kts` 스펙 코드 블록과 동일
- `docs/PROJECT.md` 패키지 맵·모듈 맵 갱신 스펙과 일치
- `app/.../{model,data,directions}/`, `app/src/main/res/font/` 디렉터리 삭제 확인, `.gitkeep` 3개 삭제 확인
- `./gradlew clean` 후 `./gradlew assembleDebug` → BUILD SUCCESSFUL (독립적으로 클린 리빌드해 재검증)
- `./gradlew :core:common:build :core:domain:build :core:data:build :core:ui:build :feature:home:build` → BUILD SUCCESSFUL
- `entry/*`, `home/*`, `map/*`, `navi/KakaoMapLauncher.kt`, `navi/KakaoNaviLauncher.kt`, `location/*`,
  `ui/AppRoot.kt`, `MainActivity.kt`, `RodiApplication.kt` 여전히 `:app`에 위치 확인(Phase 2 범위 아님)
