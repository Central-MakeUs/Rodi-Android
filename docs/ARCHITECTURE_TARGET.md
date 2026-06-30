# ARCHITECTURE_TARGET.md — 멀티모듈 북극성 (목표/미구현)

> **현재는 단일 `:app` 모듈이다.** 이 문서는 출시 후 진행할 멀티모듈 클린아키텍처 마이그레이션의
> **목표 스펙**이다. 분리가 진행되면 이 내용이 `PROJECT.md`/`CLAUDE.md`로 순차 승격된다.

## Module Structure
- `:app` — Application(@HiltAndroidApp), MainActivity, NavHost assembler
- `:core:domain` — UseCase, Model, Repository interface (pure Kotlin)
- `:core:data` — Repository impl, DTO, Mapper, Hilt @Module
- `:core:ui` — Shared Compose components, RodiTheme (api-exported)
- `:core:common` — Extension functions, utilities (pure Kotlin)
- `:feature:home` — Sample: MVI ViewModel + Screen + Contract
- `:build-logic` — Convention Plugins

## Convention Plugins
build-logic에 등록할 ID 6종: `dororong.rodi.android.application`, `dororong.rodi.android.library`,
`dororong.rodi.android.library.compose`, `dororong.rodi.jvm.library`, `dororong.rodi.android.hilt`,
`dororong.rodi.android.feature`. (hilt/ksp/serialization은 `libs.plugins.*` alias로 추가)

| 모듈 | Convention Plugin | 추가 alias |
|------|------|------|
| `:app` | `dororong.rodi.android.application` | — |
| `:core:data` | `dororong.rodi.android.library` | hilt, ksp, kotlin.serialization |
| `:core:ui` | `dororong.rodi.android.library.compose` | hilt, ksp |
| `:feature:*` | `dororong.rodi.android.library.compose` | hilt, ksp |
| `:core:domain` | `dororong.rodi.jvm.library` | kotlin.serialization |
| `:core:common` | `dororong.rodi.jvm.library` | — |

## Add a feature module
1. `mkdir -p feature/<name>/src/main/kotlin/com/dororong/rodi/feature/<name>`
2. `feature/<name>/build.gradle.kts` — `dororong.rodi.android.library.compose` + `libs.plugins.hilt`, `libs.plugins.ksp`
3. `settings.gradle.kts`에 `include(":feature:<name>")`
4. `app/build.gradle.kts`에 `implementation(project(":feature:<name>"))`
5. `app/MainActivity.kt` NavHost에 composable 등록

## 관련 스킬
- 아키텍처/멀티모듈 셋업: `/android_ca_multimodule`(Profile A), `/agp9_module_setup`
- 의존성 버전 관리: `/version_control_wisdom`
