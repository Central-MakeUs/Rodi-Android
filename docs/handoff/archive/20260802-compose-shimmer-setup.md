# HANDOFF — Current Task

Status: DONE
Task: 공용 Compose shimmer 의존성 준비
Branch: codex/skeleton-shimmer-setup
Base: origin/develop (55e3654d)
Risk: LOW

## Context

향후 화면별 스켈레톤 UI 구현 전에 Compose용 shimmer 효과를 공용 UI 모듈에서 사용할 수 있게 준비한다. `com.valentinilk.shimmer:compose-shimmer:1.4.0`은 Maven Central의 안정판이며, 현재 앱의 Android Compose 사용 범위와 맞는 가벼운 modifier 기반 API를 제공한다.

## Goal

향후 모든 feature가 `core:ui`를 통해 shimmer를 사용할 수 있도록 version catalog와 공용 UI 모듈 의존성을 추가한다.

## Spec

- Version catalog에 `composeShimmer` 1.4.0과 `compose-shimmer` 라이브러리 alias를 추가한다.
- `:core:ui`의 main implementation dependency로 해당 alias를 추가한다.
- 스켈레톤 Composable, 화면별 로딩 상태, 디자인 토큰 및 기존 UI는 이번 작업에서 변경하지 않는다.

## Acceptance

- `core:ui`가 version catalog alias를 통해 `com.valentinilk.shimmer:compose-shimmer:1.4.0`을 의존한다.
- 기존 feature에 직접 의존성을 추가하지 않고, 스켈레톤 구현을 위한 공용 기반만 제공한다.
- `./gradlew :core:ui:assembleDebug`와 `./gradlew assembleDebug`가 통과한다.

## Expected Files

- `gradle/libs.versions.toml`
- `core/ui/build.gradle.kts`
- `docs/handoff/HANDOFF.md`

## Verification

- `./gradlew :core:ui:assembleDebug`
- `./gradlew assembleDebug`

## Out of Scope

- 개별 화면 또는 공용 Skeleton Composable 구현
- 로딩 상태, ViewModel, API 변경
- `valentinilk/compose-shimmer` 외의 라이브러리 추가

## Implementation Result

- Changed files: `gradle/libs.versions.toml`, `core/ui/build.gradle.kts`, `docs/handoff/HANDOFF.md`
- `composeShimmer` 1.4.0 version과 `compose-shimmer` alias를 catalog에 등록하고, `:core:ui`의 main implementation dependency로 추가했다.
- Verification: `./gradlew :core:ui:assembleDebug` PASS, `./gradlew assembleDebug` PASS, `git diff --check` PASS.
- Open questions: 실제 Skeleton Composable의 색상·모양·애니메이션 속도는 화면별 디자인을 확정하는 다음 작업에서 정의한다.

## Review

- 독립 검토 기준: 최신 `origin/develop`(55e3654d)과 현재 최종 작업 트리. `origin/develop...HEAD` 및 `origin/develop..HEAD`는 빈 diff이며, 미커밋 작업 트리 변경은 Expected Files의 세 파일로만 제한된다.
- Acceptance 1 — 충족: `gradle/libs.versions.toml`에 `composeShimmer = "1.4.0"`과 `com.valentinilk.shimmer:compose-shimmer` alias가 있고, `core/ui/build.gradle.kts`의 main `implementation(libs.compose.shimmer)`가 이를 사용한다. `:core:ui:dependencyInsight --configuration debugRuntimeClasspath`가 요청 좌표와 Android runtime variant의 1.4.0 해석을 확인했다.
- Acceptance 2 — 충족: feature 모듈과 앱 소스는 변경하지 않았고, 의존성은 `:core:ui`에만 추가했다. Skeleton Composable, 로딩 상태, ViewModel, API, 디자인 토큰을 변경한 diff는 없다.
- Acceptance 3 — 충족: 현재 최종 트리에서 `./gradlew :core:ui:assembleDebug`와 `./gradlew assembleDebug`가 모두 BUILD SUCCESSFUL이다.
- 발견사항: 없음. `git diff --check` PASS, blocking 또는 non-blocking finding 없음.

## Review Triage

## Revision Plan

## Revision Result

## Final Review

- 승인: version catalog를 통한 공용 `:core:ui` 의존성만 추가해 스펙·Expected Files·Out of Scope를 모두 지켰고, 실제 의존성 해석과 요구 빌드가 통과했다.
