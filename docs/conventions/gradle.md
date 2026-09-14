# Gradle · Convention Plugin

> Rodi가 의도적으로 유지하는 **프로젝트 고유 규칙**이다. 프로젝트 무관 규범은 전역 스킬
> `/android-code-standard`에 있고, 여기 규칙이 그것과 충돌하면 **이 문서가 우선**한다.
> 수치는 적지 않는다 — 재검증 명령으로 대체한다(→ `README.md`). 시점별 조사 수치는 `../audits/`.
>
> **이 파일은 ADR 0001 기준으로 다시 썼다(2026-09-06).** 그 이전 조사 결과는
> 통째로 무효다 — feature 6개가 Compose·Hilt를 각자 선언하던 구조가 사라졌다. 구조 변경이
> 문서를 무효화하는 전형적인 사례라, Rodi는 이후 ADR에 "무효화하는 규범 항목" 칸을 두기로 했다
> (`../adr/TEMPLATE.md`).

## 의존성은 출처를 하나만 갖는다

이게 이 문서의 유일한 원칙이고 나머지는 그 적용이다.

**왜**: 같은 의존성이 두 경로로 들어오면 버전을 올릴 때 어디를 고쳐야 하는지 알 수 없고,
한쪽만 고쳐도 빌드가 통과해 어긋난 채로 남는다. 실제로 Rodi에서 Compose BOM이 Convention
Plugin과 모듈 양쪽에 선언돼 있었고, 한쪽만 고쳐도 아무 신호가 없었다.

## feature 모듈은 기능 전용 Convention Plugin 하나만 적용

```kotlin
plugins {
    id("dororong.rodi.android.feature")
}
```

이 플러그인이 `library.compose` + `hilt`를 합성하고 화면 아키텍처 의존성을 주입한다:
`:core:ui`, `activity-compose`, `lifecycle-compose` 번들, `hilt-compose` 번들, Hilt compiler,
debug `ui-tooling`.

모듈 `build.gradle.kts`에는 **그 모듈 고유 의존성만** 남긴다 — Kakao SDK, play-services-location,
aboutlibraries 같은 것들, `project(...)` 의존성, 테스트 의존성, `android { namespace }`.

**정본**: `feature/auth/build.gradle.kts`,
`build-logic/src/main/kotlin/AndroidFeatureConventionPlugin.kt`

## Compose 본체는 `:core:ui`가 `api`로 재노출하는 것이 유일한 출처

```kotlin
// core/ui/build.gradle.kts
api(platform(libs.androidx.compose.bom))
api(libs.bundles.compose)
```

**왜**: 디자인 시스템 모듈의 공개 API가 이미 Compose 타입을 노출하므로 `api`가 의미상 맞다.
Compose 버전을 조정하는 지점이 한 곳으로 줄어든다.

**트레이드오프**: 모듈의 `build.gradle.kts`만 봐서는 Compose가 어디서 오는지 안 보인다.
전체 구성을 이해하려면 `build-logic`과 `:core:ui`를 함께 읽어야 한다. Rodi는 이 비용을
받아들이기로 했고 ADR 0001에 명시했다.

**정본**: `core/ui/build.gradle.kts` — 앵커 `api(libs.bundles.compose)`

## androidTest용 Compose BOM은 Compose Convention Plugin이 주입

```kotlin
add("androidTestImplementation", platform(libs.findLibrary("androidx-compose-bom").get()))
```

**왜**: `androidTestImplementation`은 `api`로 노출된 BOM 제약을 **자동으로 상속하지 않는다.**
`:core:ui`가 `api(platform(bom))`을 해도 계측 테스트 쪽 버전은 정렬되지 않는다. 그래서 BOM은
main과 androidTest에 각각 필요한데, **Compose library 모듈**(`:core:ui`와 feature 6개)이 공통으로
지나가는 `AndroidLibraryComposeConventionPlugin`에 한 번만 두면 그 범위 안에서는 출처가 하나다.

**`app`은 이 범위 밖이다.** 이 플러그인은 `com.android.library`를 적용하므로 application 모듈에는
걸리지 않는다. `app`이 BOM을 직접 선언하는 건 **중복이 아니라 별도 공급 경로**이므로 지우면 안 된다.
app까지 Convention Plugin으로 들이는 건 별도 작업이다 → `../BACKLOG.md`

**정본**: `build-logic/src/main/kotlin/AndroidLibraryComposeConventionPlugin.kt` —
앵커 `androidTestImplementation`

**재검증** (모듈에서 BOM을 다시 선언한 곳):
```bash
rg -n '(implementation|androidTestImplementation)\(platform\(libs\.androidx\.compose\.bom\)\)' \
  -g 'build.gradle.kts' core feature app
```
> `core/ui`의 `api(platform(...))`은 **유일 출처 그 자체**라 걸리면 안 된다 — 그래서 `api`를
> 패턴에서 뺐다. 걸리는 건 아직 Convention Plugin 밖에 있는 `app`뿐이다 → `../BACKLOG.md`.
> (건수는 여기 적지 않는다 — 명령을 돌리거나 `../audits/`를 본다.)

## 커버리지(Kover)는 `dororong.rodi.kover` 하나가 설정한다

```kotlin
// AndroidLibraryConventionPlugin / AndroidLibraryComposeConventionPlugin / JvmLibraryConventionPlugin
apply("dororong.rodi.kover")
```

**왜**: 제외 규칙(Hilt·Room 생성 코드, `@Preview`)이 모듈마다 달라지면 모듈 간 수치를 비교할 수 없다.
library·jvm Convention Plugin이 자동으로 적용하므로 새 모듈도 빠짐없이 측정된다.

**`app`은 예외다.** Convention Plugin 밖에 있어서 `app/build.gradle.kts`에 `id("dororong.rodi.kover")`
한 줄을 직접 둔다. 설정 자체는 여전히 build-logic에만 있다. 루트 `build.gradle.kts`는 같은 플러그인을 적용하고
`kover(project(...))`로 모듈을 모아 통합 리포트를 만든다. **새 모듈을 추가하면 루트의 `kover(...)` 목록에도 넣는다.**

**정본**: `build-logic/src/main/kotlin/KoverConventionPlugin.kt`

**재검증** (Kover를 모듈에서 직접 설정한 곳 — 루트와 `app`의 id 한 줄 외에는 없어야 한다):
```bash
rg -n 'kover' -g 'build.gradle.kts' .
```

## 버전 카탈로그 접근 방식

```kotlin
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
libs.findLibrary("androidx-activity-compose").get()
libs.findBundle("lifecycle-compose").get()
```

**정본**: `build-logic/src/main/kotlin/AndroidFeatureConventionPlugin.kt`

## namespace는 기본 패키지 + 모듈 경로

하이픈이 있는 모듈명은 유효한 패키지 세그먼트로 나눈다.

```kotlin
android { namespace = "com.dororong.rodi.feature.course.registration" }
```
<!-- 모듈 디렉터리는 feature/course-registration, namespace는 ...feature.course.registration -->

**왜**: 소스 패키지·모듈 좌표·생성 코드 namespace의 관계가 예측 가능해진다.

**정본**: `feature/course-registration/build.gradle.kts`

## bundle은 "항상 함께 쓰는 것"에만

UI/runtime(`compose`, `lifecycle-compose`, `hilt-compose`, `navigation3`, `coil`),
data runtime(`room-runtime`, `network-runtime`, `kakao-navigation`),
test(`unit-test`, `flow-test`, `roborazzi-test`, `android-test`)로 나뉜다.
BOM·compiler·debug 전용은 bundle에서 뺀다.

**테스트 의존성은 Convention Plugin으로 올리지 않았다** — 모듈마다 조합이 다르다.
`roborazzi-test`는 `feature:home`과 `core:ui`만, `flow-test`도 일부만 쓴다. 억지로 공통화하면
안 쓰는 모듈까지 무거워진다.

**정본**: `gradle/libs.versions.toml`의 `[bundles]`
