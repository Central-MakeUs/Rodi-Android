# HANDOFF — :app 코드 이관 Phase 2 (entry→feature:entry, home/map/navi런처/location→feature:home)

> Claude(기획)와 Codex(구현)가 주고받는 **단일 활성 작업 채널**.
> 완료되면 `docs/handoff/archive/<날짜>-<작업>.md`로 옮기고 이 파일은 다음 작업으로 비운다.

Status: DONE
Branch: feature/multimodule-scaffold

## Context (왜)
Phase 1(model/data/directions/ui-theme→core:*)에 이어, 나머지 `:app` 코드를 이관한다.
`entry`는 신규 `:feature:entry`로, `home`+`map`+`navi`의 두 런처(`KakaoMapLauncher`/
`KakaoNaviLauncher`)+`location`은 기존 빈 `:feature:home`으로 옮긴다.

**중요한 발견 — 코드를 직접 읽어 확인**: `home/HomeScreen.kt`가 `entry` 패키지의
`TermsWebView`/`TermsDocument`/`TermsDocuments`를 가져다 쓴다(홈 화면에서도 약관을 보여주는 바텀시트가
있음). `entry`와 `home`을 완전히 독립된 형제 feature 모듈로 두려면(feature↔feature 직접 의존 금지),
이 3개 파일은 `:feature:entry`가 아니라 **`:core:ui`(신규 `terms` 하위 패키지)로 옮긴다**.
`TermsWebViewScreen.kt`도 같이 옮기지만 — 확인해보니 **어디서도 호출되지 않는 죽은 코드**다. 이번엔
동작을 안 바꾸는 게 원칙이라 그대로 옮기고, 삭제는 BACKLOG로 남긴다.

또한 `ic_chevron_left`/`ic_chevron_right` 아이콘은 `entry`와 `home` 양쪽에서 쓰여서(공용) 개별
feature 모듈이 아니라 `:core:ui`의 drawable로 옮긴다. 그 외 drawable은 각 feature 전용이라 정확히
나뉜다(아래 4번 참고).

## Spec (무엇을·어떻게)

### 1. `:feature:entry` 신규 모듈 생성
- `settings.gradle.kts`에 `include(":feature:entry")` 추가
- `feature/entry/build.gradle.kts` 신규:
  ```kotlin
  plugins {
      id("dororong.rodi.android.library.compose")
  }

  android {
      namespace = "com.dororong.rodi.feature.entry"
  }

  dependencies {
      implementation(project(":core:data"))
      implementation(project(":core:ui"))
      implementation(platform(libs.androidx.compose.bom))
      implementation(libs.androidx.activity.compose)
      implementation(libs.androidx.compose.material3)
      implementation(libs.androidx.compose.ui)
      implementation(libs.androidx.compose.ui.graphics)
      implementation(libs.androidx.compose.ui.tooling.preview)
      implementation(libs.androidx.lifecycle.viewmodel.compose)
      debugImplementation(libs.androidx.compose.ui.tooling)
  }
  ```

### 2. `git mv` — `entry` → `:feature:entry` (6개 파일, package `com.dororong.rodi.feature.entry`)
- `entry/DrivingPrecautionsContent.kt` → `feature/entry/src/main/java/com/dororong/rodi/feature/entry/DrivingPrecautionsContent.kt`
  - `package com.dororong.rodi.entry` → `package com.dororong.rodi.feature.entry`
  - `import com.dororong.rodi.R` → `import com.dororong.rodi.feature.entry.R`
- `entry/EntryComponents.kt` → `feature/entry/.../EntryComponents.kt`
  - `package com.dororong.rodi.entry` → `package com.dororong.rodi.feature.entry`
  - `import com.dororong.rodi.R` → `import com.dororong.rodi.feature.entry.R` (그대로 `ic_check` 용)
  - **추가** `import com.dororong.rodi.core.ui.R as CoreUiR`
  - 본문의 `painterResource(R.drawable.ic_chevron_right)` **2곳** → `painterResource(CoreUiR.drawable.ic_chevron_right)`
    (`ic_check` 쪽 `R.drawable.ic_check`는 그대로 둠 — feature:entry 자체 리소스)
- `entry/EntryFlow.kt` → `feature/entry/.../EntryFlow.kt`
  - `package com.dororong.rodi.entry` → `package com.dororong.rodi.feature.entry` (import 변경 없음 — 이 파일은 rodi import가 없음)
- `entry/EntryViewModel.kt` → `feature/entry/.../EntryViewModel.kt`
  - `package com.dororong.rodi.entry` → `package com.dororong.rodi.feature.entry` (import 변경 없음 — `core.data.EntryPreferences`는 Phase 1에서 이미 correct)
- `entry/LocationPermissionContent.kt` → `feature/entry/.../LocationPermissionContent.kt`
  - `package com.dororong.rodi.entry` → `package com.dororong.rodi.feature.entry`
  - `import com.dororong.rodi.R` → `import com.dororong.rodi.feature.entry.R`
- `entry/TermsAgreementContent.kt` → `feature/entry/.../TermsAgreementContent.kt`
  - `package com.dororong.rodi.entry` → `package com.dororong.rodi.feature.entry`
  - **추가** `import com.dororong.rodi.core.ui.terms.TermsDocuments`
    (이 파일은 지금까지 `TermsDocuments`와 같은 패키지라 import 없이 썼음 — `TermsDocuments`가
    `:core:ui`로 이동하므로 이제 명시적 import 필요)

### 3. `git mv` — 공용 약관 화면 3개 → `:core:ui`(`terms` 하위 패키지)
- `entry/TermsWebView.kt` → `core/ui/src/main/java/com/dororong/rodi/core/ui/terms/TermsWebView.kt`
  - `package com.dororong.rodi.entry` → `package com.dororong.rodi.core.ui.terms`
  - `import com.dororong.rodi.core.ui.theme.RodiTheme` 그대로(이미 Phase 1에서 correct)
- `entry/TermsWebViewScreen.kt` → `core/ui/.../terms/TermsWebViewScreen.kt`
  - `package com.dororong.rodi.entry` → `package com.dororong.rodi.core.ui.terms`
  - `import com.dororong.rodi.R` → `import com.dororong.rodi.core.ui.R` (`ic_chevron_left`, 4번에서 core:ui로 이동)
- `entry/TermsDocuments.kt` → `core/ui/.../terms/TermsDocuments.kt`
  - `package com.dororong.rodi.entry` → `package com.dororong.rodi.core.ui.terms` (import 없음)
- `core/ui/build.gradle.kts`에 `implementation(libs.androidx.core.ktx)` 추가
  (`TermsWebView.kt`가 `androidx.core.view.WindowCompat` 사용 — 지금까지 `:app`의 core-ktx로 컴파일됐던 것)

### 4. `git mv` — `home`/`map`/`navi`(런처만)/`location` → `:feature:home`
`:feature:home`은 이미 `core:ui`/`core:domain` 의존을 가진 빈 모듈이다. 아래처럼 채운다
(package `com.dororong.rodi.feature.home` + 하위 패키지 `map`/`navi`/`location`):

- `home/HomeScreen.kt` → `feature/home/src/main/java/com/dororong/rodi/feature/home/HomeScreen.kt`
  - `package com.dororong.rodi.home` → `package com.dororong.rodi.feature.home`
  - `import com.dororong.rodi.R` → `import com.dororong.rodi.feature.home.R`
  - **추가** `import com.dororong.rodi.core.ui.R as CoreUiR`
  - `import com.dororong.rodi.entry.TermsDocument` → `import com.dororong.rodi.core.ui.terms.TermsDocument`
  - `import com.dororong.rodi.entry.TermsDocuments` → `import com.dororong.rodi.core.ui.terms.TermsDocuments`
  - `import com.dororong.rodi.entry.TermsWebView` → `import com.dororong.rodi.core.ui.terms.TermsWebView`
  - `import com.dororong.rodi.location.awaitCurrentLocation` → `import com.dororong.rodi.feature.home.location.awaitCurrentLocation`
  - `import com.dororong.rodi.location.hasLocationPermission` → `import com.dororong.rodi.feature.home.location.hasLocationPermission`
  - `import com.dororong.rodi.map.rememberMapViewWithLifecycle` → `import com.dororong.rodi.feature.home.map.rememberMapViewWithLifecycle`
  - `import com.dororong.rodi.map.renderCourse` → `import com.dororong.rodi.feature.home.map.renderCourse`
  - `import com.dororong.rodi.map.renderCourseChips` → `import com.dororong.rodi.feature.home.map.renderCourseChips`
  - `import com.dororong.rodi.navi.KakaoMapLauncher` → `import com.dororong.rodi.feature.home.navi.KakaoMapLauncher`
  - `import com.dororong.rodi.navi.KakaoNaviLauncher` → `import com.dororong.rodi.feature.home.navi.KakaoNaviLauncher`
  - (`core.domain.*`, `core.data.*`, `core.data.navi.*`, `core.ui.theme.RodiTheme` import는 Phase 1에서 이미 correct, 변경 없음)
  - 본문 `painterResource(R.drawable.ic_chevron_left)` **2곳**, `painterResource(R.drawable.ic_chevron_right)` **1곳**
    → 각각 `painterResource(CoreUiR.drawable.ic_chevron_left)` / `painterResource(CoreUiR.drawable.ic_chevron_right)`
    (`ic_chevron_down` 등 나머지 `R.drawable.*`는 그대로 — feature:home 자체 리소스)
- `home/HomeViewModel.kt` → `feature/home/.../HomeViewModel.kt`
  - `package com.dororong.rodi.home` → `package com.dororong.rodi.feature.home` (그 외 import 변경 없음)
- `home/NaviPickerSheet.kt` → `feature/home/.../NaviPickerSheet.kt`
  - `package com.dororong.rodi.home` → `package com.dororong.rodi.feature.home`
  - `import com.dororong.rodi.R` → `import com.dororong.rodi.feature.home.R`
  - (`core.data.navi.NaviApp`, `core.ui.theme.RodiTheme`는 Phase 1에서 이미 correct)
- `map/CourseRouteRenderer.kt` → `feature/home/src/main/java/com/dororong/rodi/feature/home/map/CourseRouteRenderer.kt`
  - `package com.dororong.rodi.map` → `package com.dororong.rodi.feature.home.map`
  - `import com.dororong.rodi.R` → `import com.dororong.rodi.feature.home.R`
  - (`core.domain.Course`는 Phase 1에서 이미 correct)
- `map/MapViewLifecycle.kt` → `feature/home/.../map/MapViewLifecycle.kt`
  - `package com.dororong.rodi.map` → `package com.dororong.rodi.feature.home.map` (import 변경 없음)
- `navi/KakaoMapLauncher.kt` → `feature/home/src/main/java/com/dororong/rodi/feature/home/navi/KakaoMapLauncher.kt`
  - `package com.dororong.rodi.navi` → `package com.dororong.rodi.feature.home.navi` (`core.domain.*` import는 이미 correct)
- `navi/KakaoNaviLauncher.kt` → `feature/home/.../navi/KakaoNaviLauncher.kt`
  - `package com.dororong.rodi.navi` → `package com.dororong.rodi.feature.home.navi` (`core.domain.Course` import는 이미 correct)
- `location/CurrentLocation.kt` → `feature/home/src/main/java/com/dororong/rodi/feature/home/location/CurrentLocation.kt`
  - `package com.dororong.rodi.location` → `package com.dororong.rodi.feature.home.location` (import 변경 없음)

이동 후 빈 디렉터리 삭제: `app/.../home/`, `app/.../map/`, `app/.../navi/`, `app/.../location/`,
`app/.../entry/`.

### 5. `feature/home/build.gradle.kts` 교체
```kotlin
plugins {
    id("dororong.rodi.android.library.compose")
}

android {
    namespace = "com.dororong.rodi.feature.home"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kakao.maps)
    implementation(libs.kakao.navi)
    implementation(libs.play.services.location)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
```

### 6. drawable 리소스 이동 (`git mv`)
- `:core:ui`(공용, entry+home 양쪽에서 씀) — `app/src/main/res/drawable/` → `core/ui/src/main/res/drawable/`:
  `ic_chevron_left.xml`, `ic_chevron_right.xml`
- `:feature:entry`(entry 전용) — → `feature/entry/src/main/res/drawable/`:
  `ic_check.xml`, `illust_driving_precautions.xml`, `illust_location_permission.xml`
- `:feature:home`(home/map 전용) — → `feature/home/src/main/res/drawable/`:
  `ic_chevron_down.xml`, `ic_crosshair.xml`, `ic_settings.xml`, `ic_star.xml`, `ic_x.xml`,
  `illust_network_disconntected.xml`, `img_navi_kakaomap.png`, `img_navi_kakaonavi.png`,
  `ic_pin_arrival.xml`, `ic_pin_park.xml`, `ic_pin_start.xml`
- `:app`에 남는 것(건드리지 않음): `ic_launcher_background.xml`, `ic_launcher_foreground.xml`
  (mipmap 어댑티브 아이콘 전용)

### 7. `:app` 나머지 파일 수정
- `ui/AppRoot.kt`:
  - `import com.dororong.rodi.entry.EntryFlow` → `import com.dororong.rodi.feature.entry.EntryFlow`
  - `import com.dororong.rodi.home.HomeScreen` → `import com.dororong.rodi.feature.home.HomeScreen`
- `MainActivity.kt`, `RodiApplication.kt`: 변경 없음
- `settings.gradle.kts`: `include(":feature:entry")` 추가(1번), 기존 `include(":feature:home")`는 유지
- `app/build.gradle.kts`:
  - `dependencies`에 추가: `implementation(project(":feature:entry"))`, `implementation(project(":feature:home"))`
  - `implementation(project(":core:domain"))` 줄 **삭제** — `Course` 등 도메인 모델을 쓰던 코드
    (home/map/navi)가 전부 `:feature:home`으로 이관되어 `:app`에서 더 이상 직접 안 씀
  - `implementation(libs.play.services.location)` 줄 **삭제** — 유일한 사용처(`location/CurrentLocation.kt`)가
    `:feature:home`으로 이관됨
  - 그 외 의존성(`material3`, `ui.graphics`, `ui.tooling.preview`, `lifecycle.viewmodel.compose` 등)은
    이번 이관과 직접 연결되지 않은 정리라 **건드리지 않는다**(Out of scope, BACKLOG 후보)

### 8. `docs/PROJECT.md` 갱신
- "구조" 줄(10번째 줄) 교체:
  ```markdown
  - 구조: 멀티모듈(`:core:domain/data/ui/common` + `:feature:entry/home`). 목표 전체 구조는
    `docs/ARCHITECTURE_TARGET.md` 참고
  ```
- "패키지 맵" 섹션(`app/src/main/java/com/dororong/rodi/` 하위) 전체를 아래로 교체
  (`entry`/`home`/`map`/`navi`/`location`이 전부 이관되어 `:app`엔 진입점만 남음):
  ```markdown
  ## `:app`에 남은 것
  `MainActivity`(엔트리 포인트), `RodiApplication`(Kakao SDK 초기화), `ui/AppRoot`(게이트→홈 라우팅).
  화면·기능 코드는 전부 `core:*`/`feature:*`로 이관 완료.
  ```
- "모듈 맵" 표의 `:feature:home` 행, `:feature:entry` 행 추가/교체:
  ```markdown
  | `:feature:entry` | 진입 게이트(위치권한·약관·운전 주의사항), `EntryFlow` + 단계별 Content |
  | `:feature:home` | 홈 화면(지도+코스 바텀시트), 지도 렌더(`map`), 외부 내비 런처(`navi`), 현재 위치(`location`) |
  ```
  `:core:ui` 행 설명 뒤에 " · 공용 약관 WebView(`terms.TermsWebView`)" 추가

## Files to touch
- 신규: `feature/entry/build.gradle.kts`, `feature/entry/src/main/java/com/dororong/rodi/feature/entry/*.kt`(6개),
  `feature/entry/src/main/res/drawable/*`(3개)
- `git mv` + 패키지/특정 import 수정: `entry/TermsWebView.kt`, `entry/TermsWebViewScreen.kt`,
  `entry/TermsDocuments.kt` → `core/ui/.../terms/`
- `git mv` + 패키지/import 수정: `home/*.kt`(3개), `map/*.kt`(2개), `navi/KakaoMapLauncher.kt`,
  `navi/KakaoNaviLauncher.kt`, `location/CurrentLocation.kt` → `feature/home/...`
- `git mv`: drawable 17개(위 6번대로 core:ui/feature:entry/feature:home 3곳으로 분산)
- `core/ui/build.gradle.kts` — `libs.androidx.core.ktx` 추가
- `feature/home/build.gradle.kts` — 5번 스펙대로 교체
- `settings.gradle.kts` — `include(":feature:entry")` 추가
- `app/build.gradle.kts` — 7번 스펙대로 수정
- `app/src/main/java/com/dororong/rodi/ui/AppRoot.kt` — import 2줄만
- 삭제: `app/.../entry/`, `app/.../home/`, `app/.../map/`, `app/.../navi/`, `app/.../location/` 디렉터리
- `docs/PROJECT.md`
- `docs/BACKLOG.md` — `TermsWebViewScreen` 죽은 코드 정리 항목 추가(Claude가 plan 단계에서 직접 추가)

## Acceptance criteria
- [ ] `git mv`로 이동해 각 파일 git history 보존
- [ ] 이동한 파일은 스펙에 명시한 패키지/import/call-site 외 diff 없음 (로직 변경 없음)
- [ ] `app/src/main/java/com/dororong/rodi/{entry,home,map,navi,location}/` 디렉터리 존재하지 않음
- [ ] `app/src/main/res/drawable/`에 `ic_launcher_background.xml`/`ic_launcher_foreground.xml`만 남음
      (mipmap 관련 제외 — 이관 대상 17개 drawable은 모두 이동)
- [ ] `./gradlew assembleDebug` 성공
- [ ] `./gradlew :core:common:build :core:domain:build :core:data:build :core:ui:build :feature:entry:build :feature:home:build` 성공
- [ ] `docs/PROJECT.md` 갱신 스펙대로 반영
- [ ] `MainActivity.kt`, `RodiApplication.kt`, `ui/AppRoot.kt`는 여전히 `:app`에 있음

## Verification
```
./gradlew assembleDebug
./gradlew :core:common:build :core:domain:build :core:data:build :core:ui:build :feature:entry:build :feature:home:build
git status --short app/src/main/java/com/dororong/rodi/entry app/src/main/java/com/dororong/rodi/home app/src/main/java/com/dororong/rodi/map app/src/main/java/com/dororong/rodi/navi app/src/main/java/com/dororong/rodi/location
```

## Out of scope
- `TermsWebViewScreen` 죽은 코드 삭제(BACKLOG 기록, 이번엔 동작 보존 우선)
- `:app`의 이번 이관과 무관한 미사용 의존성 정리(`material3`/`ui.graphics`/`ui.tooling.preview`/
  `lifecycle.viewmodel.compose` 등 — 이번 phase에서 이동한 코드 때문에 죽은 게 아니라 검증이 더 필요)
- Hilt 도입, Repository 인터페이스/UseCase 계층화
- `:feature:entry`/`:feature:home`이 서로 의존하지 않는 것을 강제하는 lint/아키텍처 테스트 추가
- Kotlin/AGP/Kakao SDK 버전 업그레이드 (이미 BACKLOG에 기록됨)

---
## Codex Result   <!-- Codex가 구현 후 채움 → Status=IMPL_DONE (또는 막히면 BLOCKED) -->
- Changed files: settings.gradle.kts, app/build.gradle.kts, app/src/main/java/com/dororong/rodi/ui/AppRoot.kt, app/src/main/java/com/dororong/rodi/{entry,home,map,navi,location}/ moved to feature/core modules, app/src/main/res/drawable/* moved to core/feature modules, core/ui/build.gradle.kts, core/ui/src/main/java/com/dororong/rodi/core/ui/terms/*, core/ui/src/main/res/drawable/{ic_chevron_left.xml,ic_chevron_right.xml}, feature/entry/build.gradle.kts, feature/entry/src/main/java/com/dororong/rodi/feature/entry/*, feature/entry/src/main/res/drawable/*, feature/home/build.gradle.kts, feature/home/src/main/java/com/dororong/rodi/feature/home/**, feature/home/src/main/res/drawable/*, docs/PROJECT.md, docs/handoff/HANDOFF.md
- Build/test: `./gradlew assembleDebug` GREEN; `./gradlew :core:common:build :core:domain:build :core:data:build :core:ui:build :feature:entry:build :feature:home:build` GREEN; `app/src/main/res/drawable/` contains only launcher drawables; old `app/src/main/java/com/dororong/rodi/{entry,home,map,navi,location}/` directories removed.
- Open questions: none

---
## Claude Review  <!-- Claude가 검토 후 채움 -->
- Blocking: 없음
- Nits: 없음
- Verdict: APPROVE

재검증 결과 (2026-07-01):
- 33개 파일/리소스 이동 전부 `git diff --cached -M`(old/new 경로 함께 지정해 rename 페어링 확인)으로
  검사 — 스펙에 명시한 패키지·import·call-site 외 diff 없음. `EntryComponents.kt`/`HomeScreen.kt`의
  `CoreUiR` 별칭 도입과 `ic_chevron_left/right` 3곳 call-site 치환도 스펙과 정확히 일치
  (`ic_check`/`ic_chevron_down` 등 나머지는 그대로 자기 모듈 R 유지 확인)
- `TermsAgreementContent.kt`의 신규 `import com.dororong.rodi.core.ui.terms.TermsDocuments` 반영 확인
- drawable 17개 분배 확인: `core/ui`(공용 2개), `feature/entry`(3개), `feature/home`(11개),
  `app`엔 launcher 아이콘 2개만 남음
- `feature/entry/build.gradle.kts`(신규), `feature/home/build.gradle.kts`, `core/ui/build.gradle.kts`
  (core-ktx 추가) 모두 스펙 코드 블록과 동일
- `app/build.gradle.kts`: `core:domain`/`play-services-location` 제거, `feature:entry`/`feature:home`
  추가 — 스펙과 일치, 그 외 의존성(material3 등)은 손대지 않음 확인
- `docs/PROJECT.md`, `docs/BACKLOG.md` 갱신 스펙과 일치(BACKLOG는 plan 단계에 이미 넣은 내용 그대로,
  Codex가 추가로 건드리지 않음)
- `./gradlew clean` 후 `assembleDebug` → BUILD SUCCESSFUL(독립 재검증)
- `./gradlew :core:common:build :core:domain:build :core:data:build :core:ui:build :feature:entry:build :feature:home:build` → BUILD SUCCESSFUL
- `app/.../{entry,home,map,navi,location}/` 디렉터리 모두 삭제 확인, `feature:entry`에 6개 파일 정확히 존재
- `TermsWebViewScreen`은 스펙대로 동작 보존한 채 그대로 이동(여전히 미호출 — 삭제는 BACKLOG로 남김)
