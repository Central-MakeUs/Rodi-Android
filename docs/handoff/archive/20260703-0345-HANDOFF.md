# HANDOFF — EntryRepository/NaviPreferenceRepository를 core:domain UseCase로 감싸기

> Claude(기획)와 Codex(구현)가 주고받는 **단일 활성 작업 채널**.
> 완료되면 `docs/handoff/archive/<날짜>-<작업>.md`로 옮기고 이 파일은 다음 작업으로 비운다.

Status: DONE
Branch: feat/entry-navi-usecases (base: main)

## Context (왜)

`feat/domain-usecases`(PR #15, 아카이브: `docs/handoff/archive/20260702-1034-HANDOFF.md`)에서
`CourseRepository` 인터페이스를 `core:domain`으로 옮기고 `GetCoursesUseCase`/`GetRouteUseCase`로 감싸
`HomeViewModel`이 Repository를 직접 주입받지 않게 했다. 그 HANDOFF의 Out of scope에 명시된 대로
`EntryRepository`/`NaviPreferenceRepository`는 그때 손대지 않았다. 이번 작업은 그 후속으로 동일 패턴을
두 Repository에 적용한다.

현재 상태(코드 확인 완료):
- `core/data/src/main/java/com/dororong/rodi/core/data/EntryRepository.kt`에 `EntryRepository` 인터페이스
  + `EntryRepositoryImpl`이 함께 정의돼 있다(`core:data`에 인터페이스·구현체 동거).
- `core/data/src/main/java/com/dororong/rodi/core/data/navi/NaviPreferenceRepository.kt`에
  `NaviPreferenceRepository` 인터페이스 + `NaviPreferenceRepositoryImpl`이 동거.
- `core/data/src/main/java/com/dororong/rodi/core/data/navi/NaviPreference.kt`에 `NaviApp` enum(순수
  Kotlin, Android 의존 없음)과 `NaviPreference` object(Android `Context`/`SharedPreferences` 직접 사용,
  실제 저장 로직)가 함께 있다.
- `core/data/.../di/DataModule.kt`가 `@Binds`로 `EntryRepositoryImpl → EntryRepository`,
  `NaviPreferenceRepositoryImpl → NaviPreferenceRepository`를 바인딩 중(`CourseRepository`와 동일한 자리).
- `EntryViewModel`(`feature/entry`)이 `EntryRepository`를 직접 생성자 주입받아 `isCompleted`는 안 쓰고
  `complete()`에서 `entryRepository.setCompleted()`만 호출한다. `isCompleted` Flow는 `EntryViewModel`이
  아니라 `app/src/main/java/com/dororong/rodi/ui/AppRoot.kt`가 **Hilt를 거치지 않고**
  `remember { EntryPreferences(context) }`로 직접 만들어 쓴다 — `EntryRepository`/DI 경로 밖이라 이번
  스코프의 UseCase 적용 대상이 아니다(아래 Out of scope 참고). 다만 사용자가 요청한
  `GetEntryCompletedUseCase`는 계약대로 만들어 둔다(현재 소비자가 없어도 `EntryRepository.isCompleted`를
  감싸는 대칭 UseCase로 존재해야 하며, 향후 `AppRoot`를 Hilt 경로로 옮길 때 바로 쓸 수 있다).
- `HomeViewModel`(`feature/home`)이 `NaviPreferenceRepository`를 직접 생성자 주입받아
  `onNavigateClick`/`onNaviAppSelected`에서 `getAlways()`/`setAlways()`를 호출한다.
- `NaviApp`은 `feature/home`의 `HomeContract.kt`/`HomeScreen.kt`/`NaviPickerSheet.kt`에서
  `com.dororong.rodi.core.data.navi.NaviApp`으로 import돼 UI 코드(피커 시트, effect 타입)에 직접 쓰인다.
- `feature/entry/build.gradle.kts`는 `core:data`만 의존하고 `core:domain`은 의존하지 않는다(`core:domain`
  의존 자체가 이번에 처음 추가됨). `feature/home/build.gradle.kts`는 이미 `core:domain`/`core:data` 둘 다
  의존 중(`core:data`는 `SampleCourses` 때문에 이후에도 계속 필요 — 건드리지 않는다).
- `core/domain/build.gradle.kts`는 이미 `implementation(libs.javax.inject)`를 갖고 있다(PR #15에서 추가).
- `EntryViewModelTest`/`HomeViewModelTest`가 각각 `mockk<EntryRepository>()`/
  `mockk<NaviPreferenceRepository>()`를 직접 생성자에 넘기고 있어, 이번 리팩터링으로 생성자 시그니처가
  바뀌면 두 테스트도 함께 고쳐야 컴파일된다(수정 없이 두면 `./gradlew test`가 컴파일 단계에서 깨짐).

## Spec (무엇을·어떻게)

### 1. `EntryRepository` 인터페이스를 `core:domain`으로 이동

`core/domain/src/main/kotlin/com/dororong/rodi/core/domain/EntryRepository.kt` 신규:
```kotlin
package com.dororong.rodi.core.domain

import kotlinx.coroutines.flow.Flow

interface EntryRepository {
    val isCompleted: Flow<Boolean?>
    suspend fun setCompleted()
}
```
`core:domain`엔 아직 `kotlinx-coroutines-core` 의존성이 없다(`javax.inject`만 있음). `Flow`를 쓰려면
`core/domain/build.gradle.kts`에 `implementation(libs.kotlinx.coroutines.core)` 추가 필요
(alias는 `gradle/libs.versions.toml`에 이미 존재 — PR #15 때 `core:common`용으로 추가됨, 재사용만 하면 됨).

`core/data/src/main/java/com/dororong/rodi/core/data/EntryRepository.kt`는 인터페이스 정의를 지우고
impl만 남긴다:
```kotlin
package com.dororong.rodi.core.data

import android.content.Context
import com.dororong.rodi.core.domain.EntryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class EntryRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
) : EntryRepository {
    private val prefs = EntryPreferences(context)

    override val isCompleted: Flow<Boolean?> = prefs.isCompleted

    override suspend fun setCompleted() = prefs.setCompleted()
}
```
(`EntryPreferences.kt`의 실제 DataStore 저장 로직은 그대로 둔다 — 이번 스코프 아님.)

### 2. `NaviApp` enum을 `core:domain`으로 이동, `NaviPreference` object는 `core:data`에 유지

`core/domain/src/main/kotlin/com/dororong/rodi/core/domain/NaviApp.kt` 신규:
```kotlin
package com.dororong.rodi.core.domain

enum class NaviApp(val key: String, val label: String) {
    KAKAOMAP("kakaomap", "카카오맵"),
    KAKAONAVI("kakaonavi", "카카오내비"),
}
```

`core/data/src/main/java/com/dororong/rodi/core/data/navi/NaviPreference.kt`에서 `NaviApp` enum 정의를
지우고 `com.dororong.rodi.core.domain.NaviApp`을 import해서 그대로 쓴다(object 이름/저장 로직/
`Context`/`SharedPreferences` 사용은 변경 없음):
```kotlin
package com.dororong.rodi.core.data.navi

import android.content.Context
import com.dororong.rodi.core.domain.NaviApp

object NaviPreference {
    private const val PREFS_NAME = "rodi_navi"
    private const val KEY_ALWAYS = "navi_always_app"

    fun getAlways(context: Context): NaviApp? {
        val key = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ALWAYS, null) ?: return null
        return NaviApp.entries.firstOrNull { it.key == key }
    }

    fun setAlways(context: Context, app: NaviApp) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ALWAYS, app.key)
            .apply()
    }
}
```

### 3. `NaviPreferenceRepository` 인터페이스를 `core:domain`으로 이동

`core/domain/src/main/kotlin/com/dororong/rodi/core/domain/NaviPreferenceRepository.kt` 신규:
```kotlin
package com.dororong.rodi.core.domain

interface NaviPreferenceRepository {
    fun getAlways(): NaviApp?
    fun setAlways(app: NaviApp)
}
```

`core/data/src/main/java/com/dororong/rodi/core/data/navi/NaviPreferenceRepository.kt`는 인터페이스를
지우고 impl만 남긴다:
```kotlin
package com.dororong.rodi.core.data.navi

import android.content.Context
import com.dororong.rodi.core.domain.NaviApp
import com.dororong.rodi.core.domain.NaviPreferenceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NaviPreferenceRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : NaviPreferenceRepository {
    override fun getAlways(): NaviApp? = NaviPreference.getAlways(context)
    override fun setAlways(app: NaviApp) = NaviPreference.setAlways(context, app)
}
```

### 4. UseCase 4개 — `core:domain`

`core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/GetEntryCompletedUseCase.kt`:
```kotlin
package com.dororong.rodi.core.domain.usecase

import com.dororong.rodi.core.domain.EntryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEntryCompletedUseCase @Inject constructor(
    private val entryRepository: EntryRepository,
) {
    operator fun invoke(): Flow<Boolean?> = entryRepository.isCompleted
}
```

`core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/SetEntryCompletedUseCase.kt`:
```kotlin
package com.dororong.rodi.core.domain.usecase

import com.dororong.rodi.core.domain.EntryRepository
import javax.inject.Inject

class SetEntryCompletedUseCase @Inject constructor(
    private val entryRepository: EntryRepository,
) {
    suspend operator fun invoke() = entryRepository.setCompleted()
}
```

`core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/GetNaviAlwaysUseCase.kt`:
```kotlin
package com.dororong.rodi.core.domain.usecase

import com.dororong.rodi.core.domain.NaviApp
import com.dororong.rodi.core.domain.NaviPreferenceRepository
import javax.inject.Inject

class GetNaviAlwaysUseCase @Inject constructor(
    private val naviPreferenceRepository: NaviPreferenceRepository,
) {
    operator fun invoke(): NaviApp? = naviPreferenceRepository.getAlways()
}
```

`core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/SetNaviAlwaysUseCase.kt`:
```kotlin
package com.dororong.rodi.core.domain.usecase

import com.dororong.rodi.core.domain.NaviApp
import com.dororong.rodi.core.domain.NaviPreferenceRepository
import javax.inject.Inject

class SetNaviAlwaysUseCase @Inject constructor(
    private val naviPreferenceRepository: NaviPreferenceRepository,
) {
    operator fun invoke(app: NaviApp) = naviPreferenceRepository.setAlways(app)
}
```
(`GetCoursesUseCase`/`GetNaviAlwaysUseCase`처럼 실패할 일 없는 동기 위임은 `Result`로 감싸지 않는다 —
PR #15와 동일 기준. `runSuspendCatching`은 네트워크/IO가 있는 `GetRouteUseCase`에만 쓰인 패턴이고,
DataStore/SharedPreferences 위임은 대상이 아니다.)

### 5. `DataModule` — import만 `core:domain`으로 변경

`core/data/src/main/java/com/dororong/rodi/core/data/di/DataModule.kt`:
```kotlin
package com.dororong.rodi.core.data.di

import com.dororong.rodi.core.data.CourseRepositoryImpl
import com.dororong.rodi.core.data.EntryRepositoryImpl
import com.dororong.rodi.core.data.navi.NaviPreferenceRepositoryImpl
import com.dororong.rodi.core.domain.CourseRepository
import com.dororong.rodi.core.domain.EntryRepository
import com.dororong.rodi.core.domain.NaviPreferenceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindCourseRepository(impl: CourseRepositoryImpl): CourseRepository

    @Binds
    abstract fun bindNaviPreferenceRepository(impl: NaviPreferenceRepositoryImpl): NaviPreferenceRepository

    @Binds
    abstract fun bindEntryRepository(impl: EntryRepositoryImpl): EntryRepository
}
```

### 6. `EntryViewModel` — Repository 대신 UseCase 주입

`feature/entry/src/main/java/com/dororong/rodi/feature/entry/EntryViewModel.kt`:
- `entryRepository: EntryRepository` 생성자 파라미터를 `setEntryCompletedUseCase: SetEntryCompletedUseCase`
  로 교체(현재 `EntryViewModel`은 `isCompleted`를 안 쓰므로 `GetEntryCompletedUseCase`는 주입 대상 아님).
- import를 `com.dororong.rodi.core.data.EntryRepository` → `com.dororong.rodi.core.domain.usecase.SetEntryCompletedUseCase`로 변경.
- `complete()` 내부 `entryRepository.setCompleted()` → `setEntryCompletedUseCase()`로 변경.
- `feature/entry/build.gradle.kts`에 `implementation(project(":core:domain"))` 추가. 이 리팩터링 후
  `EntryViewModel.kt`가 더 이상 `core:data` 심볼을 참조하지 않으므로
  `implementation(project(":core:data"))`는 제거한다(다른 파일에서 `core:data` 참조 없음 — 확인 완료).

### 7. `HomeViewModel` — Repository 대신 UseCase 주입

`feature/home/src/main/java/com/dororong/rodi/feature/home/HomeViewModel.kt`:
- `naviPreferenceRepository: NaviPreferenceRepository` 파라미터를
  `getNaviAlwaysUseCase: GetNaviAlwaysUseCase`, `setNaviAlwaysUseCase: SetNaviAlwaysUseCase` 2개로 교체.
- import 변경: `com.dororong.rodi.core.data.navi.NaviApp` → `com.dororong.rodi.core.domain.NaviApp`,
  `com.dororong.rodi.core.data.navi.NaviPreferenceRepository` 제거,
  `com.dororong.rodi.core.domain.usecase.GetNaviAlwaysUseCase`/`SetNaviAlwaysUseCase` 추가.
- `onNavigateClick`의 `naviPreferenceRepository.getAlways()` → `getNaviAlwaysUseCase()`.
- `onNaviAppSelected`의 `naviPreferenceRepository.setAlways(intent.app)` → `setNaviAlwaysUseCase(intent.app)`.
- `feature/home/build.gradle.kts`는 그대로 둔다(`core:domain`/`core:data` 둘 다 이미 의존, `core:data`는
  `SampleCourses` 때문에 계속 필요).

### 8. `NaviApp` import 갱신 — `feature:home` UI 파일 3곳

아래 파일의 `import com.dororong.rodi.core.data.navi.NaviApp`을
`import com.dororong.rodi.core.domain.NaviApp`으로 바꾼다(로직 변경 없음, import만):
- `feature/home/src/main/java/com/dororong/rodi/feature/home/HomeContract.kt`
- `feature/home/src/main/java/com/dororong/rodi/feature/home/HomeScreen.kt`
- `feature/home/src/main/java/com/dororong/rodi/feature/home/components/NaviPickerSheet.kt`

### 9. 기존 테스트 갱신(컴파일 유지 목적, 새 케이스 추가는 선택)

`feature/entry/src/test/java/com/dororong/rodi/feature/entry/EntryViewModelTest.kt`:
- `import com.dororong.rodi.core.data.EntryRepository` →
  `import com.dororong.rodi.core.domain.usecase.SetEntryCompletedUseCase`.
- `testEntryRepository(): EntryRepository = mockk()` →
  `testSetEntryCompletedUseCase(): SetEntryCompletedUseCase = mockk()`로 이름/타입 변경, 모든
  `EntryViewModel(testEntryRepository())` 호출부를 `EntryViewModel(testSetEntryCompletedUseCase())`로 변경.
- `` `complete stores entry completion and invokes callback` `` 테스트: `coEvery { entryRepository.setCompleted() } returns Unit` → `coEvery { setEntryCompletedUseCase() } returns Unit`,
  `coVerify(exactly = 1) { entryRepository.setCompleted() }` → `coVerify(exactly = 1) { setEntryCompletedUseCase() }`
  (mockk로 `operator fun invoke()`를 검증할 땐 `useCase()` 호출 문법 그대로 `coEvery`/`coVerify`에 쓰면 된다).

`feature/home/src/test/java/com/dororong/rodi/feature/home/HomeViewModelTest.kt`:
- import 변경: `com.dororong.rodi.core.data.navi.NaviApp` → `com.dororong.rodi.core.domain.NaviApp`,
  `com.dororong.rodi.core.data.navi.NaviPreferenceRepository` 제거,
  `com.dororong.rodi.core.domain.usecase.GetNaviAlwaysUseCase`/`SetNaviAlwaysUseCase` 추가.
- `createViewModel`의 `naviPreferenceRepository: NaviPreferenceRepository = mockk()` 파라미터를
  `getNaviAlwaysUseCase: GetNaviAlwaysUseCase = mockk()`, `setNaviAlwaysUseCase: SetNaviAlwaysUseCase = mockk()`
  2개로 교체하고, `HomeViewModel(...)` 생성 호출에 두 인자를 그대로 전달.
- 각 테스트에서 `mockk<NaviPreferenceRepository>()` + `every { naviPreferenceRepository.getAlways() } returns ...`
  형태를 `mockk<GetNaviAlwaysUseCase>()` + `every { getNaviAlwaysUseCase() } returns ...`로 바꾼다
  (`onNavigateClick` 관련 3개 테스트). `setAlways` 검증 1곳(`onNaviAppSelected stores always preference...`)은
  `mockk<SetNaviAlwaysUseCase>()` + `every { setNaviAlwaysUseCase(NaviApp.KAKAONAVI) } returns Unit`,
  `verify(exactly = 1) { setNaviAlwaysUseCase(NaviApp.KAKAONAVI) }`로 바꾼다. `getRouteUseCase`/
  `getCoursesUseCase` 관련 로직·픽스처(`testCourse`/`testWaypoints`/`testRouteResult`)는 변경 없음.

## Files to touch
- `gradle/libs.versions.toml` — 변경 없음(기존 `kotlinx-coroutines-core`/`javax-inject` alias 재사용)
- `core/domain/build.gradle.kts` (`kotlinx.coroutines.core` 의존성 추가)
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/EntryRepository.kt` (신규)
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/NaviApp.kt` (신규)
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/NaviPreferenceRepository.kt` (신규)
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/GetEntryCompletedUseCase.kt` (신규)
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/SetEntryCompletedUseCase.kt` (신규)
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/GetNaviAlwaysUseCase.kt` (신규)
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/SetNaviAlwaysUseCase.kt` (신규)
- `core/data/src/main/java/com/dororong/rodi/core/data/EntryRepository.kt` (인터페이스 제거, impl만)
- `core/data/src/main/java/com/dororong/rodi/core/data/navi/NaviPreference.kt` (enum 제거, domain import)
- `core/data/src/main/java/com/dororong/rodi/core/data/navi/NaviPreferenceRepository.kt` (인터페이스 제거, impl만)
- `core/data/src/main/java/com/dororong/rodi/core/data/di/DataModule.kt` (import 경로만 변경)
- `feature/entry/build.gradle.kts` (`core:domain` 추가, `core:data` 제거)
- `feature/entry/src/main/java/com/dororong/rodi/feature/entry/EntryViewModel.kt` (UseCase 주입)
- `feature/entry/src/test/java/com/dororong/rodi/feature/entry/EntryViewModelTest.kt` (mock 대상 교체)
- `feature/home/src/main/java/com/dororong/rodi/feature/home/HomeViewModel.kt` (UseCase 주입)
- `feature/home/src/main/java/com/dororong/rodi/feature/home/HomeContract.kt` (import 변경)
- `feature/home/src/main/java/com/dororong/rodi/feature/home/HomeScreen.kt` (import 변경)
- `feature/home/src/main/java/com/dororong/rodi/feature/home/components/NaviPickerSheet.kt` (import 변경)
- `feature/home/src/test/java/com/dororong/rodi/feature/home/HomeViewModelTest.kt` (mock 대상 교체)

## Acceptance criteria
- [ ] `EntryRepository`/`NaviPreferenceRepository` 인터페이스가 `core:domain`에 있고, `core:data`엔
      `EntryRepositoryImpl`/`NaviPreferenceRepositoryImpl`만 남아 각각 그 인터페이스를 구현한다.
- [ ] `NaviApp` enum이 `core:domain`에 있고, `core:data`의 `NaviPreference` object는 그 타입을 import해서
      기존과 동일한 `SharedPreferences` 저장 로직을 유지한다(키/값 포맷 변경 없음).
- [ ] `core:domain`이 `core:data`에 의존하지 않는다(순환 의존 없음) —
      `core/domain/build.gradle.kts`의 `dependencies` 블록에 `project(":core:data")`가 없어야 한다.
- [ ] `GetEntryCompletedUseCase`(`Flow<Boolean?>` 반환), `SetEntryCompletedUseCase`,
      `GetNaviAlwaysUseCase`(`NaviApp?` 반환), `SetNaviAlwaysUseCase`(`NaviApp` 파라미터) 4개가
      `core:domain`에 있고 각각 해당 Repository를 생성자 주입받는다.
- [ ] `EntryViewModel`이 `EntryRepository`를 더 이상 직접 주입받지 않고 `SetEntryCompletedUseCase`를
      주입받는다. `complete()`가 그 UseCase를 통해서만 완료 상태를 저장한다.
- [ ] `HomeViewModel`이 `NaviPreferenceRepository`를 더 이상 직접 주입받지 않고
      `GetNaviAlwaysUseCase`/`SetNaviAlwaysUseCase`를 주입받는다. `getCoursesUseCase`/`getRouteUseCase`
      관련 동작은 변경 없이 유지.
- [ ] `feature/entry`가 더 이상 `core:data`에 의존하지 않는다(`build.gradle.kts`에서
      `project(":core:data")` 제거 확인).
- [ ] `EntryViewModelTest`/`HomeViewModelTest`가 새 UseCase 생성자 시그니처에 맞게 갱신되고, 기존에
      검증하던 케이스(진입 단계 전이, `complete` 호출/콜백, 코스 클릭 라우팅, `onNavigateClick` 3분기,
      `onNaviAppSelected` always 저장)를 동일하게 커버한다.
- [ ] `./gradlew test`가 4개 모듈(`core:domain`, `core:common`, `feature:home`, `feature:entry`) 전부
      GREEN — 특히 `EntryViewModelTest`/`HomeViewModelTest`가 새 시그니처로 컴파일·통과한다.
- [ ] `./gradlew assembleDebug`, `./gradlew lint` GREEN(회귀 없음).
- [ ] `core:domain` 모듈에 Android SDK 의존성이 새로 추가되지 않는다(순수 JVM 유지 —
      `kotlinx-coroutines-core`는 이미 `core:common`이 쓰는 순수 코틀린 아티팩트라 이 제약을 어기지 않음).
- [ ] 앱 실행 시 첫 진입 게이트 완료 후 홈으로 전환되는 동작, 내비게이션 앱 선택/"항상 이 앱으로" 저장
      동작이 리팩터링 전과 동일하게 작동한다(에뮬레이터 육안 확인).

## Verification
```
./gradlew :core:domain:compileKotlin
./gradlew test --stacktrace
./gradlew assembleDebug
./gradlew lint
```
에뮬레이터에서: (1) 앱 최초 실행 → 위치권한/약관/주의사항 게이트 통과 → 홈 화면으로 자동 전환되는지,
(2) 홈에서 코스 선택 후 내비게이션 시작 → 카카오맵/카카오내비 둘 다 설치된 상태에서 피커가 뜨고
"항상 이 앱으로 열기" 체크 후 선택 → 이후 같은 코스에서 다시 내비게이션 시작 시 피커 없이 저장된 앱으로
바로 실행되는지 육안 확인.

## Out of scope
- `app/src/main/java/com/dororong/rodi/ui/AppRoot.kt`가 `EntryPreferences`를 Hilt 없이 직접 생성해 쓰는
  부분 — `EntryRepository`/DI 경로 밖이라 이번 리팩터링 대상이 아니다. `GetEntryCompletedUseCase`를 만들되
  이 UseCase를 `AppRoot`에 연결하는 것은 별도 스코프(필요해지면 `docs/BACKLOG.md`에 추가).
- `EntryPreferences`(DataStore)/`NaviPreference`(SharedPreferences)의 실제 저장 로직 변경 — 이번 작업은
  순수 리팩터링(인터페이스 이동 + UseCase 래핑)이며 저장 방식·키·포맷은 그대로 둔다.
- `EntryViewModelTest`/`HomeViewModelTest`에 새 테스트 케이스 추가(예: `GetEntryCompletedUseCase`,
  `GetNaviAlwaysUseCase`에 대한 신규 `*UseCaseTest.kt`) — 기존 두 ViewModel 테스트가 새 시그니처로
  컴파일·통과하도록 갱신하는 것까지만 스코프. UseCase 자체의 단독 단위테스트(`GetCoursesUseCaseTest`/
  `GetRouteUseCaseTest`처럼 mockk로 위임 검증)가 필요하면 후속으로 `docs/BACKLOG.md`에 추가할 것.
- `core:data`(`EntryRepositoryImpl`, `NaviPreferenceRepositoryImpl`)의 단위테스트 — Android
  Context/DataStore/SharedPreferences 의존이라 Robolectric/계측테스트가 필요한 별도 스코프
  (`20260702-1223-HANDOFF.md`의 기존 Out of scope와 동일 사유).
- 새 화면/기능 추가, 디자인 변경 — 없음.

---
## Codex Result   <!-- Codex가 구현 후 채움 → Status=IMPL_DONE (또는 막히면 BLOCKED) -->
- Changed files: core/domain/build.gradle.kts, core/domain/src/main/kotlin/com/dororong/rodi/core/domain/EntryRepository.kt, core/domain/src/main/kotlin/com/dororong/rodi/core/domain/NaviApp.kt, core/domain/src/main/kotlin/com/dororong/rodi/core/domain/NaviPreferenceRepository.kt, core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/GetEntryCompletedUseCase.kt, core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/SetEntryCompletedUseCase.kt, core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/GetNaviAlwaysUseCase.kt, core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/SetNaviAlwaysUseCase.kt, core/data/src/main/java/com/dororong/rodi/core/data/EntryRepository.kt, core/data/src/main/java/com/dororong/rodi/core/data/navi/NaviPreference.kt, core/data/src/main/java/com/dororong/rodi/core/data/navi/NaviPreferenceRepository.kt, core/data/src/main/java/com/dororong/rodi/core/data/di/DataModule.kt, feature/entry/build.gradle.kts, feature/entry/src/main/java/com/dororong/rodi/feature/entry/EntryViewModel.kt, feature/entry/src/test/java/com/dororong/rodi/feature/entry/EntryViewModelTest.kt, feature/home/src/main/java/com/dororong/rodi/feature/home/HomeViewModel.kt, feature/home/src/main/java/com/dororong/rodi/feature/home/HomeContract.kt, feature/home/src/main/java/com/dororong/rodi/feature/home/HomeScreen.kt, feature/home/src/main/java/com/dororong/rodi/feature/home/components/NaviPickerSheet.kt, feature/home/src/test/java/com/dororong/rodi/feature/home/HomeViewModelTest.kt, docs/handoff/HANDOFF.md
- Build/test: ./gradlew :core:domain:compileKotlin GREEN; ./gradlew test --stacktrace GREEN; ./gradlew assembleDebug GREEN; ./gradlew lint GREEN; adb install -r app/build/outputs/apk/debug/app-debug.apk GREEN; adb shell am start -n com.dororong.rodi/.MainActivity GREEN. Screenshot capture after launch was not completed because adb daemon restart was denied by the sandbox.
- Open questions: none

---
## Claude Review  <!-- Claude가 검토 후 채움 -->
- Blocking: 없음
- Nits:
  - 이번 세션에서 `./gradlew` 실행이 샌드박스 승인 게이트에 막혀 빌드/테스트를 직접 재실행하지 못했다.
    Codex Result에 기록된 `compileKotlin`/`test`/`assembleDebug`/`lint` GREEN 보고에 의존해 검토함
    (정적 diff 검토 결과는 스펙과 완전히 일치하므로 리스크 낮음). 다음 검토 시 여유가 되면 직접
    빌드 재확인 권장.
  - 에뮬레이터 육안 확인(진입 게이트→홈 전환, 내비 피커 "항상 이 앱으로" 저장/재사용)은 Codex도
    스크린샷 캡처를 완료하지 못했다고 명시함(sandbox에서 adb 데몬 재시작 거부). 병합 전 실제 확인 권장.
  - 스펙에 언급됐던 `feature/entry/build.gradle.kts`의 `core:data` 제거·`core:domain` 추가가 정확히
    반영됨. `feature/home/build.gradle.kts`도 스펙대로 변경 없음.
- 검토 상세:
  1. `core/domain`의 신규 파일 7개(`EntryRepository.kt`, `NaviApp.kt`, `NaviPreferenceRepository.kt`,
     `GetEntryCompletedUseCase.kt`, `SetEntryCompletedUseCase.kt`, `GetNaviAlwaysUseCase.kt`,
     `SetNaviAlwaysUseCase.kt`)가 HANDOFF Spec의 코드 블록과 바이트 단위로 일치.
  2. `core/data`의 `EntryRepository.kt`/`NaviPreference.kt`/`NaviPreferenceRepository.kt`에서 인터페이스·
     enum 정의가 제거되고 domain import로 대체됨(저장 로직·`SharedPreferences` 키/포맷 불변 확인).
  3. `DataModule.kt`는 import 경로만 `core:domain`으로 교체, `@Binds` 바인딩 3개 그대로 유지.
  4. `EntryViewModel`/`HomeViewModel`이 Repository 대신 UseCase(`SetEntryCompletedUseCase`,
     `GetNaviAlwaysUseCase`/`SetNaviAlwaysUseCase`)를 주입받도록 변경, `complete()`/`onNavigateClick()`/
     `onNaviAppSelected()` 내부 호출부도 스펙대로 교체됨.
  5. `HomeContract.kt`/`HomeScreen.kt`/`NaviPickerSheet.kt`의 `NaviApp` import가
     `core.domain.NaviApp`으로 갱신됨(로직 변경 없음).
  6. `core/domain/build.gradle.kts`에 `kotlinx.coroutines.core` 의존성만 추가, `core:data` 의존 없음
     — 순환 의존 없음 확인(grep으로 재확인).
  7. 두 ViewModel 테스트가 새 UseCase mock 시그니처로 갱신되고 기존 커버리지(단계 전이, complete 콜백,
     onNavigateClick 3분기, onNaviAppSelected 저장) 유지.
  8. PROJECT.md 컨벤션 위반 없음: 토큰 하드코딩 없음, `Icons.*` 사용 없음(grep 확인), 불필요한 주석
     추가 없음, `local.properties`/카카오 키 노출 없음, 스코프 이탈 없음(Files to touch와 실제 변경
     파일 목록 일치).
- Verdict: APPROVE
