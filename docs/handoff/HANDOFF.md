# HANDOFF — core:domain UseCase 계층 도입 (runSuspendCatching + GetCoursesUseCase/GetRouteUseCase)

> Claude(기획)와 Codex(구현)가 주고받는 **단일 활성 작업 채널**.
> 완료되면 `docs/handoff/archive/<날짜>-<작업>.md`로 옮기고 이 파일은 다음 작업으로 비운다.

Status: READY_FOR_IMPL
Branch: feat/domain-usecases (base: feat/hilt-di)

## Context (왜)

`feat/hilt-di` 브랜치에서 Hilt DI + Repository 계층(`CourseRepository` 등)을 도입했다. 이번 작업은
그 위에 **UseCase 계층**을 얹어 `HomeViewModel`이 Repository를 직접 주입받지 않고 UseCase를 통해서만
접근하도록 리팩터링한다. 동시에 `runCatching`이 `CancellationException`을 삼켜버리는 문제를 막는
`runSuspendCatching` 유틸을 추가해 앞으로 모든 suspend 계층에서 재사용한다.

**중요 — 브랜치 기준**: 이 작업은 `main`이 아니라 **`feat/hilt-di` 브랜치 위에서** 시작해야 한다.
`feat/hilt-di`에만 `core:data`의 `CourseRepository`/`DataModule`/Hilt 설정이 존재한다
(`main`/`develop`의 `HomeViewModel`은 아직 `KakaoDirectionsClient`/`SampleCourses`를 직접 호출하는
Hilt 이전 상태다). 이 브랜치(`feat/domain-usecases`)는 이미 `feat/hilt-di`를 base로 생성됐다 —
`git log --oneline -3`으로 `Hilt 도입 + Repository 계층 분리 (Home/Entry)` 커밋이 조상에 있는지
먼저 확인하고 시작할 것. 없다면 절대 추측으로 진행하지 말고 BLOCKED로 보고한다.

**아키텍처 제약 — Repository 인터페이스는 core:data가 아니라 core:domain으로 옮겨야 한다.**
`feat/hilt-di`에서 `CourseRepository` 인터페이스는 `core:data`에 정의돼 있다. 그런데 `core:data`는
이미 `core:domain`에 의존한다(`Course` 모델). 만약 UseCase를 `core:domain`에 두고 `core:data`의
`CourseRepository` 인터페이스를 그대로 참조하면 `core:domain → core:data → core:domain` 순환 의존이
생겨 Gradle 빌드가 깨진다. 따라서 **`CourseRepository` 인터페이스 자체를 `core:domain`으로 이동**하고,
`core:data`에는 구현체(`CourseRepositoryImpl`)만 남긴다 — 표준 클린 아키텍처 방향(도메인이 포트를
정의하고, 데이터가 구현한다).

이 이동에는 부수 문제가 하나 있다: `CourseRepository.getRoute()`가 반환하는
`KakaoDirectionsClient.RouteResult`는 카카오맵 SDK 타입(`com.kakao.vectormap.LatLng`)을 필드로 갖는데,
`core:domain`은 `dororong.rodi.jvm.library`(순수 Kotlin/JVM, Android·카카오맵 의존성 없음) 모듈이라
이 타입을 참조할 수 없다. 그래서 **도메인 전용 `RouteResult`/`GeoPoint`를 새로 정의**하고,
`core:data`의 `CourseRepositoryImpl`이 카카오 SDK 타입 → 도메인 타입으로 변환해서 반환하게 한다.
`feature:home`(이미 카카오맵 SDK에 의존)에서는 지도에 그릴 때만 `GeoPoint → LatLng`로 다시 변환한다.
영향 범위는 작다 — `HomeScreen.kt`에서 `route.points`/`route.snappedPoints`를 지도 렌더링 함수에
넘기는 두 곳뿐이고, `CourseDetailContent.kt`의 Preview들은 `points = emptyList()`만 쓰므로 그대로
컴파일된다.

`NaviPreferenceRepository`/`EntryRepository`는 이번 스코프에 포함하지 않는다(아래 Out of scope 참고).

## Spec (무엇을·어떻게)

### 1. `runSuspendCatching` 유틸 — `core:common`

`core/common/src/main/kotlin/com/dororong/rodi/core/common/RunSuspendCatching.kt` 신규 작성:

```kotlin
package com.dororong.rodi.core.common

import kotlinx.coroutines.CancellationException

suspend inline fun <T> runSuspendCatching(crossinline block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
```

- `kotlinx.coroutines.CancellationException`을 반드시 먼저 catch해서 rethrow한다(구조적 동시성 보존).
- `core/common/build.gradle.kts`에 `kotlinx-coroutines-core` 의존성이 없으니 추가해야 한다.
  `gradle/libs.versions.toml`의 `[libraries]` 섹션에 다음 alias를 추가:
  ```
  kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
  ```
  (`coroutines` version ref는 이미 존재함, 값 `1.10.2`.) 그리고 `core/common/build.gradle.kts`에
  `implementation(libs.kotlinx.coroutines.core)` 추가. (android 변형이 아니라 core 변형이어야
  순수 JVM 모듈에서 문제없이 쓸 수 있다.)

### 2. 도메인 라우트 모델 — `core:domain`

`core/domain/src/main/kotlin/com/dororong/rodi/core/domain/RouteResult.kt` 신규 작성:

```kotlin
package com.dororong.rodi.core.domain

data class GeoPoint(val lat: Double, val lng: Double)

data class RouteResult(
    val points: List<GeoPoint>,
    val isRealRoute: Boolean,
    val totalDistanceMeters: Int = 0,
    val snappedPoints: List<GeoPoint> = emptyList(),
)
```

### 3. `CourseRepository` 인터페이스를 core:domain으로 이동

`core/domain/src/main/kotlin/com/dororong/rodi/core/domain/CourseRepository.kt` 신규 작성:

```kotlin
package com.dororong.rodi.core.domain

interface CourseRepository {
    fun getCourses(): List<Course>
    suspend fun getRoute(course: Course): RouteResult
}
```

`core:domain`은 `javax.inject.Inject`를 UseCase 생성자에 쓸 것이므로(Hilt가 그래프를 구성할 때
필요), `gradle/libs.versions.toml`에 alias 추가:
```
javax-inject = { group = "javax.inject", name = "javax.inject", version = "1" }
```
`core/domain/build.gradle.kts`에 `implementation(libs.javax.inject)` 추가.

### 4. `core:data`의 `CourseRepository.kt`를 impl 전용으로 수정

`core/data/src/main/java/com/dororong/rodi/core/data/CourseRepository.kt`에서 인터페이스 정의를
지우고 impl만 남긴다. 카카오 SDK `LatLng`(`.latitude`/`.longitude` 프로퍼티 보유) → 도메인
`GeoPoint` 변환을 여기서 수행:

```kotlin
package com.dororong.rodi.core.data

import com.dororong.rodi.core.data.directions.KakaoDirectionsClient
import com.dororong.rodi.core.domain.Course
import com.dororong.rodi.core.domain.CourseRepository
import com.dororong.rodi.core.domain.GeoPoint
import com.dororong.rodi.core.domain.RouteResult
import javax.inject.Inject

class CourseRepositoryImpl @Inject constructor() : CourseRepository {
    override fun getCourses(): List<Course> = SampleCourses.RODI_COURSES

    override suspend fun getRoute(course: Course): RouteResult {
        val raw = KakaoDirectionsClient.getRoute(course)
        return RouteResult(
            points = raw.points.map { GeoPoint(it.latitude, it.longitude) },
            isRealRoute = raw.isRealRoute,
            totalDistanceMeters = raw.totalDistanceMeters,
            snappedPoints = raw.snappedPoints.map { GeoPoint(it.latitude, it.longitude) },
        )
    }
}
```

`core/data/src/main/java/com/dororong/rodi/core/data/di/DataModule.kt`의
`bindCourseRepository(impl: CourseRepositoryImpl): CourseRepository` 바인딩은 그대로 두되, import를
`com.dororong.rodi.core.domain.CourseRepository`로 바꾼다.

### 5. UseCase 2개 — `core:domain`

`core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/GetCoursesUseCase.kt`:

```kotlin
package com.dororong.rodi.core.domain.usecase

import com.dororong.rodi.core.domain.Course
import com.dororong.rodi.core.domain.CourseRepository
import javax.inject.Inject

class GetCoursesUseCase @Inject constructor(
    private val courseRepository: CourseRepository,
) {
    operator fun invoke(): List<Course> = courseRepository.getCourses()
}
```

`core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/GetRouteUseCase.kt`:

```kotlin
package com.dororong.rodi.core.domain.usecase

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.Course
import com.dororong.rodi.core.domain.CourseRepository
import com.dororong.rodi.core.domain.RouteResult
import javax.inject.Inject

class GetRouteUseCase @Inject constructor(
    private val courseRepository: CourseRepository,
) {
    suspend operator fun invoke(course: Course): Result<RouteResult> =
        runSuspendCatching { courseRepository.getRoute(course) }
}
```

(`GetCoursesUseCase`는 정적 샘플 리스트를 읽는 동기 호출이라 실패할 일이 없어 `Result`로 감싸지
않는다. `GetRouteUseCase`는 네트워크 I/O + 코루틴 취소 가능성이 있어 `runSuspendCatching`을 쓴다.)

### 6. `HomeViewModel` — Repository 대신 UseCase 주입

`feature/home/src/main/java/com/dororong/rodi/feature/home/HomeViewModel.kt`:
- 생성자에서 `courseRepository: CourseRepository` 파라미터를 제거하고
  `getCoursesUseCase: GetCoursesUseCase`, `getRouteUseCase: GetRouteUseCase`로 교체한다.
  `naviPreferenceRepository`는 그대로 유지한다(스코프 아님).
- `RouteResult` import를 `com.dororong.rodi.core.data.directions.KakaoDirectionsClient.RouteResult`
  → `com.dororong.rodi.core.domain.RouteResult`로 변경.
- `UiState(courses = courseRepository.getCourses())` → `UiState(courses = getCoursesUseCase())`.
- `onCourseClick`의 라우팅 로직을 `Result` 언랩에 맞게 수정:
  ```kotlin
  viewModelScope.launch {
      getRouteUseCase(course)
          .onSuccess { result ->
              _state.update {
                  it.copy(
                      routeByCourse = it.routeByCourse + (id to result),
                      routingCourseIds = it.routingCourseIds - id,
                  )
              }
          }
          .onFailure {
              _state.update { it.copy(routingCourseIds = it.routingCourseIds - id) }
          }
  }
  ```
  (실패 시 라우팅 상태만 해제하고 `routeByCourse`는 갱신하지 않는다 — 현재
  `KakaoDirectionsClient.getRoute`는 내부적으로 직선 폴백을 이미 처리해서 사실상 실패하지 않지만,
  UseCase 계약상 실패 케이스를 무시하지 않는다.)

### 7. `RouteResult` 참조 갱신 — `feature:home`

아래 두 파일의 import를 `com.dororong.rodi.core.data.directions.KakaoDirectionsClient.RouteResult`
→ `com.dororong.rodi.core.domain.RouteResult`로 바꾼다. 로직 변경은 없다(둘 다
`isRealRoute`/`totalDistanceMeters`와 Preview용 `RouteResult(points = emptyList(), ...)` 생성자
호출만 사용하므로 필드 시그니처가 동일하게 유지됨):
- `feature/home/src/main/java/com/dororong/rodi/feature/home/HomeFormatters.kt`
- `feature/home/src/main/java/com/dororong/rodi/feature/home/components/sheet/CourseDetailContent.kt`

### 8. `GeoPoint → LatLng` 변환 — `feature:home/HomeScreen.kt`

`route.points`/`route.snappedPoints`는 이제 `List<GeoPoint>`다. 카카오맵 렌더 함수
(`renderCourse`, `fitCourseToScreen`)는 `List<LatLng>`를 받으므로, 호출부 2곳에서 변환한다:

- 약 283번째 줄:
  ```kotlin
  map.renderCourse(
      context,
      course,
      route.points.map { LatLng.from(it.lat, it.lng) },
      route.snappedPoints.map { LatLng.from(it.lat, it.lng) },
  )
  ```
- 약 311번째 줄:
  ```kotlin
  map.fitCourseToScreen(route.points.map { LatLng.from(it.lat, it.lng) })
  ```

`LatLng`는 이미 이 파일에서 import돼 있다(카메라 포커스 등에 사용 중).

## Files to touch
- `gradle/libs.versions.toml` (alias 2개 추가: `kotlinx-coroutines-core`, `javax-inject`)
- `core/common/build.gradle.kts` (coroutines-core 의존성 추가)
- `core/common/src/main/kotlin/com/dororong/rodi/core/common/RunSuspendCatching.kt` (신규)
- `core/domain/build.gradle.kts` (javax.inject 의존성 추가)
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/RouteResult.kt` (신규 — `GeoPoint`, `RouteResult`)
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/CourseRepository.kt` (신규 — 인터페이스만)
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/GetCoursesUseCase.kt` (신규)
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/GetRouteUseCase.kt` (신규)
- `core/data/src/main/java/com/dororong/rodi/core/data/CourseRepository.kt` (인터페이스 제거, impl만 남기고 도메인 타입 매핑 추가)
- `core/data/src/main/java/com/dororong/rodi/core/data/di/DataModule.kt` (import 경로만 변경)
- `feature/home/src/main/java/com/dororong/rodi/feature/home/HomeViewModel.kt` (UseCase 주입, Result 언랩)
- `feature/home/src/main/java/com/dororong/rodi/feature/home/HomeFormatters.kt` (import 변경)
- `feature/home/src/main/java/com/dororong/rodi/feature/home/components/sheet/CourseDetailContent.kt` (import 변경)
- `feature/home/src/main/java/com/dororong/rodi/feature/home/HomeScreen.kt` (GeoPoint→LatLng 변환 2곳)

## Acceptance criteria
- [ ] `runSuspendCatching`이 `core:common`에 존재하고, `CancellationException`을 catch 후 반드시
      rethrow한다(다른 `Throwable`만 `Result.failure`로 감쌈).
- [ ] `CourseRepository` 인터페이스가 `core:domain`에 있고, `core:data`엔 `CourseRepositoryImpl`만
      남아 그 인터페이스를 구현한다.
- [ ] `core:domain`이 `core:data`에 의존하지 않는다(순환 의존 없음) —
      `core/domain/build.gradle.kts`의 `dependencies` 블록에 `project(":core:data")`가 없어야 한다.
- [ ] `GetCoursesUseCase`, `GetRouteUseCase`가 `core:domain`에 있고 각각 `CourseRepository`를
      생성자 주입받는다. `GetRouteUseCase`는 `runSuspendCatching`으로 감싸 `Result<RouteResult>`를
      반환한다.
- [ ] `HomeViewModel`이 `CourseRepository`를 더 이상 직접 주입받지 않고, 대신 `GetCoursesUseCase`/
      `GetRouteUseCase`를 주입받는다. `naviPreferenceRepository`는 기존대로 유지.
- [ ] 지도 화면에서 코스 선택 시 실제 경로선이 이전과 동일하게 그려진다(카카오 SDK `LatLng`
      변환이 `HomeScreen.kt`에서 정상 동작).
- [ ] `./gradlew assembleDebug`, `./gradlew lint` 통과.
- [ ] `core:domain`, `core:common` 모듈에 Android/카카오맵 SDK 의존성이 없다(순수 JVM 유지).

## Verification
```
./gradlew :core:domain:compileDebugKotlin || ./gradlew :core:domain:compileKotlin
./gradlew assembleDebug
./gradlew lint
```
에뮬레이터에서 홈 화면 진입 → 코스 카드 선택 → 실제 경로선이 지도에 그려지는지, 상세 시트의
주행거리 텍스트가 정상 표시되는지 육안 확인.

## Out of scope
- `EntryRepository`, `NaviPreferenceRepository`를 UseCase로 감싸는 작업 (요청 예시에 없음 —
  필요해지면 `docs/BACKLOG.md`에 후속 항목으로 추가할 것).
- `KakaoDirectionsClient`의 내부 `runCatching` → `runSuspendCatching` 교체 (여긴 `withContext` 내부의
  블로킹 HTTP 호출이라 취소 시맨틱이 다르게 걸림 — 별도 검토 필요, 이번 스코프 아님).
- `EntryViewModel` 등 다른 ViewModel 리팩터링.
- 서버 연동/실제 API 교체 (여전히 `SampleCourses` 하드코딩 사용).

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
