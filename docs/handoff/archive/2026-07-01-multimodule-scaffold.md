# HANDOFF — 멀티모듈 스캐폴드 (build-logic + core/feature 빈 구조)

> Claude(기획)와 Codex(구현)가 주고받는 **단일 활성 작업 채널**.
> 완료되면 `docs/handoff/archive/<날짜>-<작업>.md`로 옮기고 이 파일은 다음 작업으로 비운다.

Status: DONE
Branch: feature/multimodule-scaffold

## Context (왜)
`docs/ARCHITECTURE_TARGET.md`에 멀티모듈 목표 구조가 문서로만 존재한다. 실제 코드 이관(마이그레이션)
전에, `UiHyeon-Kim/android-template-large` 템플릿을 참고해 **빈 모듈 골격**(build-logic convention
plugin + core/feature 디렉터리)을 먼저 깃허브에 반영해 둔다. 이번 작업은 **구조만** 만든다 — 기존
`:app`의 실제 코드를 새 모듈로 옮기는 마이그레이션은 하지 않는다(별도 후속 작업).

빈 디렉터리는 git이 추적하지 않으므로, 각 신규 패키지 디렉터리에 `.gitkeep`을 두어 구조가
그대로 커밋·푸시되도록 한다.

## Spec (무엇을·어떻게)

### 1. build-logic (Convention Plugin 4종)
`docs/ARCHITECTURE_TARGET.md`의 6종 중 **Hilt/feature 전용 플러그인은 이번 범위에서 제외**한다
(프로젝트에 아직 Hilt가 도입되지 않았고, 실사용처 없는 DI 배선을 미리 넣지 않기 위함).
이번엔 `application` / `library` / `library.compose` / `jvm.library` 4종만 만든다.
plugin ID prefix는 `dororong.rodi.*` (ARCHITECTURE_TARGET.md 표기 그대로), group은
`com.dororong.rodi.buildlogic`.

`build-logic/settings.gradle.kts`:
```kotlin
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
    versionCatalogs {
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}
rootProject.name = "build-logic"
```

`build-logic/build.gradle.kts`:
```kotlin
plugins { `kotlin-dsl` }

group = "com.dororong.rodi.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "dororong.rodi.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "dororong.rodi.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "dororong.rodi.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("jvmLibrary") {
            id = "dororong.rodi.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
```

`build-logic/src/main/kotlin/KotlinAndroid.kt` (AGP 9.x — `CommonExtension` 타입 인수 없이,
`compileOptions`는 프로퍼티):
```kotlin
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion

internal fun configureJavaKotlin(commonExtension: CommonExtension) {
    commonExtension.compileOptions.apply {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
```
※ `:app`이 현재 Java 11 (`app/build.gradle.kts`의 `compileOptions`)이므로 신규 모듈도 11로 맞춘다.
버전 통일(예: 21 승격)은 이번 스코프 밖.

`build-logic/src/main/kotlin/AndroidApplicationConventionPlugin.kt`:
```kotlin
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.plugin.compose")
            }
            extensions.configure<ApplicationExtension> {
                compileSdk = 36
                defaultConfig { minSdk = 30; targetSdk = 36 }
                configureJavaKotlin(this)
                buildFeatures { compose = true }
            }
        }
    }
}
```

`build-logic/src/main/kotlin/AndroidLibraryConventionPlugin.kt`:
```kotlin
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            extensions.configure<LibraryExtension> {
                compileSdk = 36
                defaultConfig.minSdk = 30
                configureJavaKotlin(this)
            }
        }
    }
}
```

`build-logic/src/main/kotlin/AndroidLibraryComposeConventionPlugin.kt`:
```kotlin
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.plugin.compose")
            }
            extensions.configure<LibraryExtension> {
                compileSdk = 36
                defaultConfig.minSdk = 30
                configureJavaKotlin(this)
                buildFeatures { compose = true }
            }
        }
    }
}
```

`build-logic/src/main/kotlin/JvmLibraryConventionPlugin.kt`:
```kotlin
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }
        }
    }
}
```

### 2. gradle/libs.versions.toml — build-logic classpath 항목 추가
`[libraries]`에 추가 (버전은 기존 `agp`, `kotlin` ref 재사용):
```toml
android-gradlePlugin = { group = "com.android.tools.build", name = "gradle", version.ref = "agp" }
compose-gradlePlugin = { group = "org.jetbrains.kotlin", name = "compose-compiler-gradle-plugin", version.ref = "kotlin" }
kotlin-gradlePlugin = { group = "org.jetbrains.kotlin", name = "kotlin-gradle-plugin", version.ref = "kotlin" }
```
기존 버전/의존성은 건드리지 않는다.

### 3. 루트 settings.gradle.kts
- `pluginManagement { includeBuild("build-logic") ... }` 추가 (기존 repositories 블록은 유지).
- `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` 추가.
- 아래 5개 모듈 include 추가 (`:app`은 유지):
```kotlin
include(":core:common")
include(":core:data")
include(":core:domain")
include(":core:ui")
include(":feature:home")
```

### 4. 신규 모듈 (5개) — 빈 골격 + `.gitkeep`
패키지명은 `com.dororong.rodi.<모듈 경로>` (예: `com.dororong.rodi.core.domain`).
순수 Kotlin 모듈(`core:common`, `core:domain`)은 `src/main/kotlin`, Android 모듈(`core:data`,
`core:ui`, `feature:home`)은 기존 `:app` 관례를 따라 `src/main/java`를 소스 루트로 쓴다.
AGP가 `namespace`로 매니페스트를 자동 생성하므로 `AndroidManifest.xml`은 만들지 않는다.

- `core/common/build.gradle.kts`:
  ```kotlin
  plugins { id("dororong.rodi.jvm.library") }
  ```
  `core/common/src/main/kotlin/com/dororong/rodi/core/common/.gitkeep`

- `core/domain/build.gradle.kts`:
  ```kotlin
  plugins { id("dororong.rodi.jvm.library") }
  ```
  `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/.gitkeep`

- `core/data/build.gradle.kts`:
  ```kotlin
  plugins { id("dororong.rodi.android.library") }
  android { namespace = "com.dororong.rodi.core.data" }
  dependencies { implementation(project(":core:domain")) }
  ```
  `core/data/src/main/java/com/dororong/rodi/core/data/.gitkeep`

- `core/ui/build.gradle.kts`:
  ```kotlin
  plugins { id("dororong.rodi.android.library.compose") }
  android { namespace = "com.dororong.rodi.core.ui" }
  ```
  `core/ui/src/main/java/com/dororong/rodi/core/ui/.gitkeep`

- `feature/home/build.gradle.kts`:
  ```kotlin
  plugins { id("dororong.rodi.android.library.compose") }
  android { namespace = "com.dororong.rodi.feature.home" }
  dependencies {
      implementation(project(":core:ui"))
      implementation(project(":core:domain"))
  }
  ```
  `feature/home/src/main/java/com/dororong/rodi/feature/home/.gitkeep`

### 5. `:app`은 건드리지 않는다
기존 `app/build.gradle.kts`, `app/src/**`는 이번 스코프에서 수정하지 않는다. 신규 모듈들은
아직 `:app`에서 `implementation(project(":core:..."))`으로 참조되지 않는 **미연결 빈 모듈**이다
(연결·코드 이관은 후속 작업).

### 6. `docs/ARCHITECTURE_TARGET.md` 갱신
"Convention Plugins" 섹션을 실제 구현(4종, hilt/feature 제외)에 맞게 갱신하고, hilt/feature
플러그인은 "Hilt 도입 시 추가 예정"으로 각주 처리한다. Module Structure 표는 그대로 유지.

## Files to touch
- `build-logic/settings.gradle.kts` (신규)
- `build-logic/build.gradle.kts` (신규)
- `build-logic/src/main/kotlin/KotlinAndroid.kt` (신규)
- `build-logic/src/main/kotlin/AndroidApplicationConventionPlugin.kt` (신규)
- `build-logic/src/main/kotlin/AndroidLibraryConventionPlugin.kt` (신규)
- `build-logic/src/main/kotlin/AndroidLibraryComposeConventionPlugin.kt` (신규)
- `build-logic/src/main/kotlin/JvmLibraryConventionPlugin.kt` (신규)
- `gradle/libs.versions.toml` (라이브러리 3종 추가)
- `settings.gradle.kts` (includeBuild + include 5개 + TYPESAFE_PROJECT_ACCESSORS)
- `core/common/build.gradle.kts`, `core/common/src/main/kotlin/.../.gitkeep` (신규)
- `core/domain/build.gradle.kts`, `core/domain/src/main/kotlin/.../.gitkeep` (신규)
- `core/data/build.gradle.kts`, `core/data/src/main/java/.../.gitkeep` (신규)
- `core/ui/build.gradle.kts`, `core/ui/src/main/java/.../.gitkeep` (신규)
- `feature/home/build.gradle.kts`, `feature/home/src/main/java/.../.gitkeep` (신규)
- `docs/ARCHITECTURE_TARGET.md` (Convention Plugins 섹션 갱신)

## Acceptance criteria
- [ ] `./gradlew :core:common:build :core:domain:build :core:data:build :core:ui:build :feature:home:build` 모두 성공
- [ ] `./gradlew assembleDebug` (기존 `:app`) 그대로 성공 — 스캐폴드가 기존 앱 빌드에 영향 없음
- [ ] `git add -A -n` (dry-run) 또는 `git status`로 `.gitkeep` 덕분에 5개 모듈의 빈 패키지 디렉터리가
      추적 대상에 포함되는 것을 확인
- [ ] `:app`의 기존 파일은 diff에 나타나지 않음 (namespace/코드 변경 없음)
- [ ] `settings.gradle.kts`에 `:core:common` `:core:data` `:core:domain` `:core:ui` `:feature:home`
      5개 모듈 include 확인
- [ ] `docs/ARCHITECTURE_TARGET.md`의 Convention Plugins 섹션이 실제 구현(4종)과 일치

## Verification
```
./gradlew :core:common:build :core:domain:build :core:data:build :core:ui:build :feature:home:build
./gradlew assembleDebug
```

## Out of scope
- 기존 `:app` 코드(`home`, `entry`, `map`, `directions`, `navi`, `location`, `model`, `data`, `ui`)를
  신규 모듈로 이관하는 실제 마이그레이션
- `:app`이 신규 모듈을 `implementation(project(...))`으로 참조하도록 배선
- Hilt 도입 및 `dororong.rodi.android.hilt` / `dororong.rodi.android.feature` convention plugin
- Java/Kotlin 버전 21 승격, Compose BOM 등 기존 버전 업그레이드
- `:app`을 convention plugin(`dororong.rodi.android.application`)으로 전환

---
## Codex Result   <!-- Codex가 구현 후 채움 → Status=IMPL_DONE (또는 막히면 BLOCKED) -->
- Changed files: build-logic/settings.gradle.kts, build-logic/build.gradle.kts, build-logic/src/main/kotlin/KotlinAndroid.kt, build-logic/src/main/kotlin/AndroidApplicationConventionPlugin.kt, build-logic/src/main/kotlin/AndroidLibraryConventionPlugin.kt, build-logic/src/main/kotlin/AndroidLibraryComposeConventionPlugin.kt, build-logic/src/main/kotlin/JvmLibraryConventionPlugin.kt, gradle/libs.versions.toml, settings.gradle.kts, core/common/build.gradle.kts, core/common/src/main/kotlin/com/dororong/rodi/core/common/.gitkeep, core/domain/build.gradle.kts, core/domain/src/main/kotlin/com/dororong/rodi/core/domain/.gitkeep, core/data/build.gradle.kts, core/data/src/main/java/com/dororong/rodi/core/data/.gitkeep, core/ui/build.gradle.kts, core/ui/src/main/java/com/dororong/rodi/core/ui/.gitkeep, feature/home/build.gradle.kts, feature/home/src/main/java/com/dororong/rodi/feature/home/.gitkeep, docs/ARCHITECTURE_TARGET.md, docs/handoff/HANDOFF.md
- Build/test: `./gradlew :core:common:build :core:domain:build :core:data:build :core:ui:build :feature:home:build` GREEN; `./gradlew assembleDebug` GREEN; `git add -A -n` confirmed `.gitkeep` files are tracked candidates.
- Open questions: none

---
## Claude Review  <!-- Claude가 검토 후 채움 -->
- Blocking: 없음
- Nits:
  - `settings.gradle.kts`에 추가한 `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")`는 이번 스캐폴드
    코드에서 실제로 쓰이진 않음(`project(":core:...")` 문자열 참조만 사용). 문제 없음, 다음 모듈
    연결 작업 때 `projects.core.domain` 타입세이프 접근자로 전환 검토 가능.
- Verdict: APPROVE

재검증 결과 (2026-07-01):
- `./gradlew :core:common:build :core:domain:build :core:data:build :core:ui:build :feature:home:build` → BUILD SUCCESSFUL
- `./gradlew assembleDebug` → BUILD SUCCESSFUL, `git diff --stat -- app/` 비어있음(：app 무변경 확인)
- `git add -A -n build-logic core feature` → 신규 17개 파일만 추가 대상, `.gradle/`·`build/` 빌드 산출물은 `.gitignore`로 정상 제외
