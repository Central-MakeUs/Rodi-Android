# HANDOFF — core:data 네트워크/로컬DB/DataStore 공통 뼈대 구축

> Claude(기획)와 Codex(구현)가 주고받는 **단일 활성 작업 채널**.
> 완료되면 `docs/handoff/archive/<날짜>-<작업>.md`로 옮기고 이 파일은 다음 작업으로 비운다.

Status: DONE
Branch: feat/network-db-skeleton

## Context (왜)
`core:data`는 현재 `EntryPreferences`(DataStore Preferences), `NaviPreference`(SharedPreferences),
`KakaoDirectionsClient`(수동 `HttpURLConnection` REST 호출), `SampleCourses`(인메모리 더미)만
있고 공용 네트워크 클라이언트나 로컬 DB가 없다. 실제 서버 API/스키마는 아직 정해지지 않았지만,
`docs/BACKLOG.md`에 등록된 후속 항목대로 서버 연동 이전에 Retrofit/OkHttp + Room + (기존
DataStore 패턴과 일관된) 공통 인프라를 미리 뼈대로 갖춰 둔다. 이번 작업은 **인프라 추가만** 하고
기존 화면·리포지토리에는 연결하지 않는다.

## Spec (무엇을·어떻게)

### 1. 버전 카탈로그 (`gradle/libs.versions.toml`)
`[versions]`에 추가:
```toml
retrofit = "2.11.0"
okhttp = "4.12.0"
room = "2.6.1"
kotlinxSerialization = "1.7.3"
retrofitKotlinxSerializationConverter = "1.0.0"
```
`[libraries]`에 추가:
```toml
retrofit-core = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-converter-kotlinx-serialization = { group = "com.squareup.retrofit2", name = "converter-kotlinx-serialization", version.ref = "retrofitKotlinxSerializationConverter" }
okhttp-bom = { group = "com.squareup.okhttp3", name = "okhttp-bom", version.ref = "okhttp" }
okhttp-core = { group = "com.squareup.okhttp3", name = "okhttp" }
okhttp-logging-interceptor = { group = "com.squareup.okhttp3", name = "logging-interceptor" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
```
`[plugins]`에 추가:
```toml
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```
버전 충돌로 위 값이 해석 안 되면(예: `converter-kotlinx-serialization` 1.0.0이 retrofit 2.11.0과
호환 안 될 경우) 같은 major 범위 내 최신 안정 patch로 조정 가능 — 단, 그 경우 `docs/BACKLOG.md`나
Codex Result의 Open questions에 조정 사실을 남길 것.

### 2. `core/data/build.gradle.kts`
- `plugins {}`에 `alias(libs.plugins.kotlin.serialization)` 추가 (KSP는 이미 `android.hilt`
  컨벤션 플러그인에서 적용됨 — 별도 추가 불필요).
- `dependencies {}`에 추가:
  ```kotlin
  implementation(platform(libs.okhttp.bom))
  implementation(libs.okhttp.core)
  implementation(libs.okhttp.logging.interceptor)
  implementation(libs.retrofit.core)
  implementation(libs.retrofit.converter.kotlinx.serialization)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)
  ```
- 기존 `android {}` 블록(`buildConfigField`)은 그대로 둔다. `BuildConfig.DEBUG`는 AGP가 자동
  생성하므로 별도 설정 불필요.

### 3. 에러 매핑 공통 규약 — `core/data/network/`
새 패키지 `com.dororong.rodi.core.data.network`에 3개 파일:

**`DataError.kt`** — Retrofit/OkHttp 타입에 의존하지 않는, 상위 레이어(리포지토리/뷰모델)가
다루기 쉬운 에러 표현:
```kotlin
package com.dororong.rodi.core.data.network

sealed interface DataError {
    enum class Network : DataError {
        NO_INTERNET,
        TIMEOUT,
        SERVER,   // 5xx
        CLIENT,   // 4xx
        UNKNOWN,
    }
}
```

**`NetworkResult.kt`** — Retrofit 호출 결과를 감싸는 공용 래퍼:
```kotlin
package com.dororong.rodi.core.data.network

sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class Failure(val error: DataError) : NetworkResult<Nothing>
}
```

**`SafeApiCall.kt`** — Retrofit suspend 호출을 감싸 예외를 `DataError`로 매핑하는 공용 함수.
향후 실제 API 리포지토리 구현체가 이 함수로 모든 호출을 감싸는 것을 전제로 한다:
```kotlin
package com.dororong.rodi.core.data.network

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

suspend fun <T> safeApiCall(block: suspend () -> T): NetworkResult<T> =
    try {
        NetworkResult.Success(block())
    } catch (e: SocketTimeoutException) {
        NetworkResult.Failure(DataError.Network.TIMEOUT)
    } catch (e: IOException) {
        NetworkResult.Failure(DataError.Network.NO_INTERNET)
    } catch (e: HttpException) {
        val error = when (e.code()) {
            in 500..599 -> DataError.Network.SERVER
            in 400..499 -> DataError.Network.CLIENT
            else -> DataError.Network.UNKNOWN
        }
        NetworkResult.Failure(error)
    } catch (e: Exception) {
        NetworkResult.Failure(DataError.Network.UNKNOWN)
    }
```

### 4. 공통 OkHttp/Retrofit 인스턴스 — `core/data/di/NetworkModule.kt`
```kotlin
package com.dororong.rodi.core.data.di

import com.dororong.rodi.core.data.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

// 실제 서버 도메인 확정 전 placeholder. 서버 연동 시 교체.
private const val BASE_URL = "https://api.rodi.app/"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
```
아직 어떤 `<T>` API 인터페이스도 정의하지 않으므로 이 `Retrofit` 인스턴스는 다른 코드에서
주입받지 않는다 (컴파일만 되면 됨, 사용처는 실제 API 정의 시점에 추가).

### 5. Room Database 스켈레톤 — `core/data/db/`
새 패키지 `com.dororong.rodi.core.data.db`에 3개 파일. `SchemaPlaceholderEntity`/`Dao`는
**실제 엔티티가 추가되면 통째로 삭제**하고 `RodiDatabase.entities`를 교체하는 것을 전제로 한
자리표시자임을 KDoc으로 명시한다.

**`SchemaPlaceholderEntity.kt`**:
```kotlin
package com.dororong.rodi.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schema_placeholder")
data class SchemaPlaceholderEntity(
    @PrimaryKey val id: Int,
)
```

**`SchemaPlaceholderDao.kt`**:
```kotlin
package com.dororong.rodi.core.data.db

import androidx.room.Dao
import androidx.room.Query

@Dao
interface SchemaPlaceholderDao {
    @Query("SELECT COUNT(*) FROM schema_placeholder")
    suspend fun count(): Int
}
```

**`RodiDatabase.kt`**:
```kotlin
package com.dororong.rodi.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * 로컬 DB 스켈레톤. [SchemaPlaceholderEntity]는 실제 엔티티가 생기기 전까지의 자리표시자로,
 * 첫 실제 엔티티 추가 시 제거하고 entities 목록을 교체한다.
 */
@Database(entities = [SchemaPlaceholderEntity::class], version = 1, exportSchema = false)
abstract class RodiDatabase : RoomDatabase() {
    abstract fun schemaPlaceholderDao(): SchemaPlaceholderDao
}
```
`exportSchema = false`로 두어 스키마 JSON 내보내기 설정(`ksp { arg("room.schemaLocation", ...) }`)을
지금 단계에서는 추가하지 않는다.

### 6. Room DI 모듈 — `core/data/di/DatabaseModule.kt`
```kotlin
package com.dororong.rodi.core.data.di

import android.content.Context
import androidx.room.Room
import com.dororong.rodi.core.data.db.RodiDatabase
import com.dororong.rodi.core.data.db.SchemaPlaceholderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "rodi.db"

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRodiDatabase(@ApplicationContext context: Context): RodiDatabase =
        Room.databaseBuilder(context, RodiDatabase::class.java, DATABASE_NAME).build()

    @Provides
    fun provideSchemaPlaceholderDao(database: RodiDatabase): SchemaPlaceholderDao =
        database.schemaPlaceholderDao()
}
```

### 7. DataStore
기존 `EntryPreferences`(`context.entryDataStore by preferencesDataStore(name = "entry")`) 패턴이
이미 확립돼 있고, 요구사항은 "기존 패턴과 일관된 설정"이지 새 DataStore 인스턴스 추가가 아니다.
**새 파일 추가 없음.** `EntryPreferences.kt`/`EntryRepository.kt`는 건드리지 않는다. (참고용으로
Codex Result에 "기존 DataStore 패턴 확인, 변경 없음"만 남기면 됨)

## Files to touch
- `gradle/libs.versions.toml` (수정)
- `core/data/build.gradle.kts` (수정)
- `core/data/src/main/java/com/dororong/rodi/core/data/network/DataError.kt` (신규)
- `core/data/src/main/java/com/dororong/rodi/core/data/network/NetworkResult.kt` (신규)
- `core/data/src/main/java/com/dororong/rodi/core/data/network/SafeApiCall.kt` (신규)
- `core/data/src/main/java/com/dororong/rodi/core/data/di/NetworkModule.kt` (신규)
- `core/data/src/main/java/com/dororong/rodi/core/data/db/SchemaPlaceholderEntity.kt` (신규)
- `core/data/src/main/java/com/dororong/rodi/core/data/db/SchemaPlaceholderDao.kt` (신규)
- `core/data/src/main/java/com/dororong/rodi/core/data/db/RodiDatabase.kt` (신규)
- `core/data/src/main/java/com/dororong/rodi/core/data/di/DatabaseModule.kt` (신규)

## Acceptance criteria
- [ ] `./gradlew :core:data:assembleDebug` 성공 (KSP로 Room 코드 생성 + Hilt 컴포넌트 생성 포함).
- [ ] `NetworkModule`/`DatabaseModule`이 기존 `DataModule`(`@Binds` 전용)과 별도 파일로 존재하고,
      Hilt `SingletonComponent`에 정상 설치됨 (중복 `@Provides`/충돌 없음).
- [ ] `RodiDatabase`/`SchemaPlaceholderEntity`/`SchemaPlaceholderDao`가 자리표시자임을 밝히는 KDoc이
      있음 — 이후 실제 엔티티 교체 시 삭제 대상임을 코드만 보고 알 수 있어야 함.
- [ ] `safeApiCall`/`NetworkResult`/`DataError`는 `retrofit2`/`okhttp3` 타입을 시그니처에 노출하지
      않음 (구현부에서만 `HttpException`/`IOException` catch).
- [ ] 새로 추가한 `Retrofit`/`OkHttpClient`/`RodiDatabase` 인스턴스는 기존 코드(`CourseRepository`,
      `EntryRepository`, `NaviPreferenceRepository`, `KakaoDirectionsClient`, `SampleCourses`) 어디에서도
      주입/사용하지 않음 — 순수 추가 인프라.
- [ ] `EntryPreferences.kt`, `NaviPreference.kt`, `KakaoDirectionsClient.kt`, `SampleCourses.kt`,
      `DataModule.kt`에 diff 없음.
- [ ] 하드코딩 색/타이포 등 UI 관련 컨벤션 위반 없음 (이번 작업은 UI 코드 없음).

## Verification
```
./gradlew :core:data:assembleDebug
./gradlew :app:assembleDebug
```
(전체 앱이 여전히 정상 빌드되는지 — 새 인프라가 아직 미사용이라도 컴파일 그래프에는 포함되므로
`:app` 빌드까지 통과해야 한다.)

## Out of scope
- 실제 서버 API 인터페이스(`@GET`/`@POST` 등) 정의 — 서버 스펙 확정 후 별도 작업.
- Room 실제 엔티티/DAO/마이그레이션 — 첫 실제 기능(예: 코스 즐겨찾기 로컬 캐시) 도입 시 별도 작업.
- `SampleCourses`/`KakaoDirectionsClient`를 새 네트워크 스택으로 교체 — 이번 스코프 아님, 계속
  동작해야 함.
- 기존 리포지토리(`CourseRepository`, `EntryRepository`, `NaviPreferenceRepository`)에 `safeApiCall`/
  `RodiDatabase` 연결.
- 인증/토큰 헤더, 재시도(retry) 정책, 캐싱 전략 — 실제 API 요구사항 확정 후.
- Room 스키마 export(`room.schemaLocation`) 설정 — 실제 엔티티 확정 후 필요 시 추가.

---
## Codex Result   <!-- Codex가 구현 후 채움 → Status=IMPL_DONE (또는 막히면 BLOCKED) -->
- Changed files: gradle/libs.versions.toml, core/data/build.gradle.kts, core/data/src/main/java/com/dororong/rodi/core/data/network/DataError.kt, core/data/src/main/java/com/dororong/rodi/core/data/network/NetworkResult.kt, core/data/src/main/java/com/dororong/rodi/core/data/network/SafeApiCall.kt, core/data/src/main/java/com/dororong/rodi/core/data/di/NetworkModule.kt, core/data/src/main/java/com/dororong/rodi/core/data/db/SchemaPlaceholderEntity.kt, core/data/src/main/java/com/dororong/rodi/core/data/db/SchemaPlaceholderDao.kt, core/data/src/main/java/com/dororong/rodi/core/data/db/RodiDatabase.kt, core/data/src/main/java/com/dororong/rodi/core/data/di/DatabaseModule.kt, docs/handoff/HANDOFF.md
- Build/test: ./gradlew :core:data:assembleDebug GREEN; ./gradlew assembleDebug GREEN
- Open questions: none. Dependency adjustments for build compatibility: com.squareup.retrofit2:converter-kotlinx-serialization 1.0.0 -> 2.11.0 because 1.0.0 artifact was not resolvable; Room 2.6.1 -> 2.8.4 because 2.6.1 generated incompatible suspend DAO Java stubs with the current Kotlin/KSP toolchain. Existing DataStore pattern confirmed; no DataStore changes. Claude Review blocking item was already absent on inspection; NetworkModule placeholder comment added.

---
## Claude Review  <!-- Claude가 검토 후 채움 -->
- Blocking: 없음.
  - (이전 리뷰 기록에 `core/domain/.../CourseRepository.kt`에 스코프 밖 문자(`ㅈ`)가 있다는 blocking
    항목이 남아 있었으나, 이번 세션에서 해당 파일을 다시 읽어 확인한 결과 현재 내용은
    `interface CourseRepository { fun getCourses(): List<Course>; suspend fun getRoute(...): RouteResult }`
    로 정상 Kotlin이며 `git diff`/`git status`에도 이 파일이 전혀 잡히지 않는다(무수정). 이전 기록은
    현재 작업트리 상태와 맞지 않는 stale한 내용이라 판단해 폐기한다.
- Nits: 없음.
  - (이전 기록의 "`NetworkModule.kt`에 placeholder 주석 누락" 지적도 stale — 현재
    `NetworkModule.kt:17`에 `// 실제 서버 도메인 확정 전 placeholder. 서버 연동 시 교체.` 주석이
    정확히 존재함을 확인.)
- 검증: 스펙/Acceptance 항목 전부 diff와 대조 확인 완료.
  - `gradle/libs.versions.toml`: versions/libraries/plugins 블록 모두 스펙 값과 일치(단, Codex Result에
    기록된 대로 `retrofitKotlinxSerializationConverter` 1.0.0→2.11.0, `room` 2.6.1→2.8.4로 조정됨 —
    스펙의 예외 조항("같은 major 범위 내 최신 안정 patch로 조정 가능, 조정 사실 기록")을 따랐고
    Open questions에 사유도 기록돼 있어 문제 없음).
  - `core/data/build.gradle.kts`: `kotlin.serialization` 플러그인 추가, Retrofit/OkHttp/Room/kotlinx-serialization
    의존성 및 `ksp(libs.androidx.room.compiler)` 추가가 스펙과 일치. `buildFeatures.buildConfig = true`가
    이미 있어 `NetworkModule`의 `BuildConfig.DEBUG` 참조가 유효함을 확인. 시크릿(`KAKAO_REST_API_KEY`)
    블록은 그대로 유지, 노출 없음.
  - `network/DataError.kt`, `network/NetworkResult.kt`, `network/SafeApiCall.kt`: 스펙 코드와 정확히 일치.
    `retrofit2`/`okhttp3` 타입이 시그니처(공개 API)에 노출되지 않고 `SafeApiCall.kt` 구현부에서만
    `HttpException`/`IOException`을 catch — Acceptance 기준 충족.
  - `di/NetworkModule.kt`, `di/DatabaseModule.kt`: 스펙 코드와 정확히 일치, 기존 `di/DataModule.kt`
    (`@Binds` 전용)와 별도 파일로 존재, `SingletonComponent`에 정상 설치. 세 모듈 간 `@Provides`/`@Binds`
    타입 중복 없음.
  - `db/SchemaPlaceholderEntity.kt`, `db/SchemaPlaceholderDao.kt`, `db/RodiDatabase.kt`: 스펙과 일치, 각각
    "실제 엔티티/DAO 추가 시 삭제" 자리표시자임을 밝히는 KDoc 포함 — Acceptance 충족.
  - 스코프 확인: `EntryPreferences.kt`/`NaviPreference.kt`/`KakaoDirectionsClient.kt`/`SampleCourses.kt`/
    `DataModule.kt`는 `git status`/`git diff`에 전혀 나타나지 않음 — diff 없음 확인. 새 `Retrofit`/
    `OkHttpClient`/`RodiDatabase` 인스턴스는 기존 리포지토리 어디에서도 주입/참조되지 않음(신규 DI
    모듈 파일들에서만 정의).
  - 컨벤션 위반 없음: 토큰 하드코딩·Material 아이콘·불필요한 주석·시크릿 노출 없음(이번 작업은 UI
    코드 없는 순수 인프라 추가).
  - `docs/BACKLOG.md`: 해당 후속 항목이 제거됨 — 이번 작업으로 완료 처리한 것으로 보이며 스펙과
    합치.
  - 빌드 실행: 이 세션에서 `./gradlew :core:data:assembleDebug` bash 실행 승인을 받지 못해 직접
    구동 검증은 못함. 다만 정적 검토상 모든 코드가 스펙과 100% 일치하고 문법 오류·미해결 참조가
    없어 컴파일 리스크는 낮음. 커밋 전 `./gradlew :core:data:assembleDebug`와
    `./gradlew :app:assembleDebug` 실행 확인을 권장.
- Verdict: APPROVE (빌드 실행 검증은 커밋 전 별도로 수행 권장)
