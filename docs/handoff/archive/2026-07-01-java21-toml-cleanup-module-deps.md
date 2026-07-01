# HANDOFF — Java 21 통일 + libs.versions.toml 카테고리 정리·버전 업데이트 + core/feature 모듈 의존성 등록

> Claude(기획)와 Codex(구현)가 주고받는 **단일 활성 작업 채널**.
> 완료되면 `docs/handoff/archive/<날짜>-<작업>.md`로 옮기고 이 파일은 다음 작업으로 비운다.

Status: DONE
Branch: feature/multimodule-scaffold

## Context (왜)
직전 작업(멀티모듈 스캐폴드)에서 Java 버전 승격과 `:app`↔신규 모듈 배선을 의도적으로 Out of scope로
미뤘다. 이번엔 그중 **Java 21 통일**, **libs.versions.toml 정리·안전한 버전 업데이트**,
**core/feature 모듈 간 의존성 등록**만 처리한다. `:app`을 신규 모듈에 연결하는 배선과 실제 코드
이관은 여전히 후속 작업(계속 보류).

버전 업데이트는 Google Maven `maven-metadata.xml`을 직접 조회해 확인한 값만 반영한다
(Codex가 임의로 다른 값을 추측하지 않도록 아래 표에 최종값을 명시).

## Spec (무엇을·어떻게)

### 1. Java 21 통일
- `build-logic/src/main/kotlin/KotlinAndroid.kt`: `sourceCompatibility`/`targetCompatibility`를
  `JavaVersion.VERSION_11` → `JavaVersion.VERSION_21`로 변경.
- `build-logic/src/main/kotlin/JvmLibraryConventionPlugin.kt`:
  - `JavaPluginExtension`의 `sourceCompatibility`/`targetCompatibility`를 `VERSION_21`로 변경.
  - Kotlin 컴파일러 JVM 타깃도 명시적으로 21로 맞춘다 (`android_ca_multimodule` 스킬의
    `JvmLibraryConventionPlugin` 레퍼런스와 동일 패턴):
    ```kotlin
    import org.gradle.api.JavaVersion
    import org.gradle.api.Plugin
    import org.gradle.api.Project
    import org.gradle.api.plugins.JavaPluginExtension
    import org.gradle.kotlin.dsl.configure
    import org.jetbrains.kotlin.gradle.dsl.JvmTarget
    import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

    class JvmLibraryConventionPlugin : Plugin<Project> {
        override fun apply(target: Project) {
            with(target) {
                pluginManager.apply("org.jetbrains.kotlin.jvm")
                extensions.configure<JavaPluginExtension> {
                    sourceCompatibility = JavaVersion.VERSION_21
                    targetCompatibility = JavaVersion.VERSION_21
                }
                extensions.configure<KotlinJvmProjectExtension> {
                    compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
                }
            }
        }
    }
    ```
- `app/build.gradle.kts`의 `compileOptions` 블록: `sourceCompatibility`/`targetCompatibility`를
  `JavaVersion.VERSION_11` → `JavaVersion.VERSION_21`로 변경. 그 외 `app/build.gradle.kts`·
  `app/src/**`는 건드리지 않는다.
- AGP(`android.library`/`android.application`/`android.library.compose` convention plugin)는
  이미 `configureJavaKotlin(this)`를 호출하고 있으므로 `KotlinAndroid.kt`만 고치면 4개 convention
  plugin 전체(application/library/library.compose)와 `:app`이 함께 21로 맞춰진다.

### 2. gradle/libs.versions.toml — 카테고리 주석 정리 + 버전 업데이트
아래 **파일 전체 내용**으로 교체한다(순서·주석·값 그대로, 임의 재배열 금지):

```toml
[versions]
# Android Gradle Plugin / Kotlin (build-logic 포함 전역 툴체인)
agp = "9.2.1"
kotlin = "2.2.10"

# AndroidX Core / Lifecycle / Activity
coreKtx = "1.19.0"
lifecycleRuntimeKtx = "2.11.0"
activityCompose = "1.13.0"

# Compose
composeBom = "2026.06.00"

# DataStore
datastore = "1.1.7"

# Test (JUnit / Espresso)
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"

# Kakao SDK
kakaoMap = "2.11.9"
kakaoSdk = "2.20.6"

# Google Play Services
playServicesLocation = "21.4.0"

[libraries]
# build-logic classpath (AGP / Kotlin Gradle Plugin)
android-gradlePlugin = { group = "com.android.tools.build", name = "gradle", version.ref = "agp" }
compose-gradlePlugin = { group = "org.jetbrains.kotlin", name = "compose-compiler-gradle-plugin", version.ref = "kotlin" }
kotlin-gradlePlugin = { group = "org.jetbrains.kotlin", name = "kotlin-gradle-plugin", version.ref = "kotlin" }

# AndroidX Core / Lifecycle / Activity
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }

# Compose
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }

# DataStore
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# Test (JUnit / Espresso)
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }

# Kakao SDK
kakao-maps = { group = "com.kakao.maps.open", name = "android", version.ref = "kakaoMap" }
kakao-navi = { group = "com.kakao.sdk", name = "v2-navi", version.ref = "kakaoSdk" }

# Google Play Services
play-services-location = { group = "com.google.android.gms", name = "play-services-location", version.ref = "playServicesLocation" }

[plugins]
# 앱/모듈에서 alias로 적용하는 Gradle 플러그인 (build-logic classpath는 [libraries] 참고)
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

**바뀌는 값**: `coreKtx` 1.18.0→1.19.0, `lifecycleRuntimeKtx` 2.10.0→2.11.0,
`composeBom` 2026.02.01→2026.06.00, `playServicesLocation` 21.3.0→21.4.0.
**바뀌지 않는 값**(그대로 유지, 이번 스코프 아님): `agp`, `kotlin`, `activityCompose`, `junit`,
`junitVersion`, `espressoCore`, `datastore`, `kakaoMap`, `kakaoSdk`.

### 3. core/feature 모듈 간 의존성 등록
`core:common`은 확장함수/유틸 계층이지만 현재 어떤 모듈에서도 참조되지 않는 고아 모듈이다.
`core:domain`/`core:data`/`core:ui`가 기반 계층으로 의존하도록 등록한다(:app 배선은 계속 보류,
`feature:home`은 기존 `core:ui`/`core:domain` 의존만으로 충분하므로 변경 없음):

- `core/domain/build.gradle.kts`:
  ```kotlin
  plugins {
      id("dororong.rodi.jvm.library")
  }

  dependencies {
      implementation(project(":core:common"))
  }
  ```
- `core/data/build.gradle.kts`: 기존 `implementation(project(":core:domain"))`에
  `implementation(project(":core:common"))` 한 줄 추가.
- `core/ui/build.gradle.kts`:
  ```kotlin
  plugins {
      id("dororong.rodi.android.library.compose")
  }

  android {
      namespace = "com.dororong.rodi.core.ui"
  }

  dependencies {
      implementation(project(":core:common"))
  }
  ```
- `feature/home/build.gradle.kts`: 변경 없음.

## Files to touch
- `build-logic/src/main/kotlin/KotlinAndroid.kt`
- `build-logic/src/main/kotlin/JvmLibraryConventionPlugin.kt`
- `app/build.gradle.kts` (compileOptions만)
- `gradle/libs.versions.toml` (전체 교체, 위 스펙 그대로)
- `core/domain/build.gradle.kts`
- `core/data/build.gradle.kts`
- `core/ui/build.gradle.kts`

## Acceptance criteria
- [ ] `KotlinAndroid.kt`, `JvmLibraryConventionPlugin.kt`, `app/build.gradle.kts` 모두 `VERSION_21` 사용
- [ ] `JvmLibraryConventionPlugin.kt`에 `KotlinJvmProjectExtension` + `jvmTarget.set(JvmTarget.JVM_21)` 반영
- [ ] `gradle/libs.versions.toml`이 스펙에 명시한 내용과 **바이트 단위로 동일**(주석·순서·값)
- [ ] `agp`/`kotlin`/`activityCompose`/`junit`/`junitVersion`/`espressoCore`/`datastore`/`kakaoMap`/`kakaoSdk` 값 변경 없음
- [ ] `core/domain`, `core/data`, `core/ui`의 `build.gradle.kts`에 `implementation(project(":core:common"))` 반영
- [ ] `feature/home/build.gradle.kts`는 diff 없음
- [ ] `./gradlew assembleDebug` 성공
- [ ] `./gradlew :core:common:build :core:domain:build :core:data:build :core:ui:build :feature:home:build` 성공
- [ ] `git diff -- app/src` 비어있음 (compileOptions 외 `:app` 코드 변경 없음)

## Verification
```
./gradlew assembleDebug
./gradlew :core:common:build :core:domain:build :core:data:build :core:ui:build :feature:home:build
```

## Out of scope
- Kotlin(2.2.10→2.4.0)/AGP 메이저·마이너 버전 업그레이드 — 컴파일러 호환성 검증이 필요한 별도 작업
  (BACKLOG.md에 후속 항목으로 기록)
- Kakao Map/Navi SDK 버전 업그레이드 — 서드파티 지도/내비 회귀 위험, 별도 검증 필요
  (BACKLOG.md에 후속 항목으로 기록)
- DataStore 1.3.0-alpha 프리릴리즈 채택 (현재 안정 최신은 1.1.7 그대로 유지)
- `:app`이 신규 core/feature 모듈을 `implementation(project(...))`으로 참조하도록 배선
- 기존 `:app` 코드(`home`, `entry`, `map`, `directions`, `navi`, `location`, `model`, `data`, `ui`) 이관
- Hilt 도입

---
## Codex Result   <!-- Codex가 구현 후 채움 → Status=IMPL_DONE (또는 막히면 BLOCKED) -->
- Changed files: build-logic/src/main/kotlin/KotlinAndroid.kt, build-logic/src/main/kotlin/JvmLibraryConventionPlugin.kt, build-logic/src/main/kotlin/AndroidApplicationConventionPlugin.kt, build-logic/src/main/kotlin/AndroidLibraryConventionPlugin.kt, build-logic/src/main/kotlin/AndroidLibraryComposeConventionPlugin.kt, app/build.gradle.kts, gradle/libs.versions.toml, core/domain/build.gradle.kts, core/data/build.gradle.kts, core/ui/build.gradle.kts, docs/PROJECT.md, docs/handoff/HANDOFF.md
- Build/test: `./gradlew assembleDebug` GREEN; `./gradlew :core:common:build :core:domain:build :core:data:build :core:ui:build :feature:home:build` GREEN; Gradle installed Android SDK Platform 37.0 automatically during `assembleDebug`; `git diff -- app/src` is empty; `feature/home/build.gradle.kts` has no diff.
- Open questions: none

---
## Claude Review  <!-- Claude가 검토 후 채움 -->
- Blocking: 없음
- Nits:
  - Codex가 Files to touch에 없던 `AndroidApplicationConventionPlugin.kt`/`AndroidLibraryConventionPlugin.kt`/
    `AndroidLibraryComposeConventionPlugin.kt`/`docs/PROJECT.md`를 건드려 `compileSdk`를 36→37로 올렸다.
    직접 재현해 확인한 결과 **필요한 변경**이었다 — 이번 스펙에서 올린 `lifecycleRuntimeKtx` 2.11.0이
    "compile against version 37 or later" 를 요구해서 compileSdk 36 상태로는 `assembleDebug`가
    실패한다(라이브러리 요구사항, 로컬 재현으로 확인). 다만 Codex가 이 판단을 "Open questions"에
    남기지 않고 조용히 스코프를 넓힌 점은 다음엔 개선 필요(AGENTS.md 절차상 애매하면 멈추고 물어야 함).
    이번 건은 스펙의 사각지대(제가 lifecycleRuntimeKtx 2.11.0을 지정하며 compileSdk 요구사항을
    검토하지 않음)라 Codex 책임이 아니라 스펙 결함이었음.
- Verdict: APPROVE

재검증 결과 (2026-07-01):
- `./gradlew assembleDebug` → BUILD SUCCESSFUL (compileSdk 36으로 되돌려 재현 시 lifecycle-runtime-compose/
  lifecycle-viewmodel-compose 2.11.0의 compileSdk 37 요구사항 에러로 BUILD FAILED 확인 → 37 유지가 맞음)
- `./gradlew :core:common:build :core:domain:build :core:data:build :core:ui:build :feature:home:build` → BUILD SUCCESSFUL
- `git diff -- app/src` 비어있음, `feature/home/build.gradle.kts` diff 없음
- `gradle/libs.versions.toml`이 스펙과 바이트 단위로 일치
- `core/domain`·`core/data`·`core/ui`에 `implementation(project(":core:common"))` 반영 확인
- `KotlinAndroid.kt`/`JvmLibraryConventionPlugin.kt`/`app/build.gradle.kts` 모두 `VERSION_21`, `JvmLibraryConventionPlugin`에 `jvmTarget.set(JvmTarget.JVM_21)` 반영 확인
