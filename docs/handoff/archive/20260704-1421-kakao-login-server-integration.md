# HANDOFF — 카카오 로그인 서버 검증 연동

> Claude(기획)와 Codex(구현)가 주고받는 **단일 활성 작업 채널**.
> 완료되면 `docs/handoff/archive/<날짜>-<작업>.md`로 옮기고 이 파일은 다음 작업으로 비운다.

Status: IMPL_DONE
Branch: feature/kakao-login-prep

## Context (왜)
직전 작업(`archive/20260703-2321-kakao-login-prep.md`)에서 카카오 SDK 로그인 화면과
`AuthRepositoryImpl`의 placeholder(서버 호출 없이 성공 처리)까지 준비해뒀다. 백엔드 API가
확정되어(Notion "카카오 로그인 API 연동 가이드") 이제 실제로 서버에 카카오 액세스 토큰을
검증받고, 서버가 발급한 세션(accessToken/refreshToken)을 기기에 안전하게 저장해 다음 실행 시
로그인 화면을 건너뛰도록 만든다.

### 백엔드 API 스펙 요약 (Notion 원문)
- Base: 운영 `https://api.stillstar.store`, prefix `/api/v1` → 최종 base URL
  `https://api.stillstar.store/api/v1/`
- 공통 응답 envelope: `{ "isSuccess": boolean, "code": string, "message": string, "data": {...} }`
- **로그인/가입**: `POST /api/v1/auth/oauth/{provider}` (`provider`="kakao")
  - Request: `{ "credential": "<카카오 access token>" }`
  - Response 200 `data`: `{ "accessToken": "...", "refreshToken": "...", "isNewMember": boolean }`
  - 에러: `400 COMMON_400`(credential 비어있음) / `400 AUTH_400_1`(미지원 provider) /
    `401 AUTH_401_5`(카카오 토큰 검증 실패)
- accessToken 만료 30분, refreshToken 만료 14일. **이번 작업 범위는 로그인 검증까지이며,
  토큰 재발급(`/auth/token/refresh`)·로그아웃(`/auth/logout`)·자동 갱신은 Out of scope**
  (아래 및 BACKLOG.md 참고 — 보호 API가 아직 하나도 없어서 지금 만들면 검증 불가능한 코드가 됨).
- 클라이언트는 카카오 토큰을 카카오 API에 직접 쓰지 않고 서버로 `credential`만 전달한다
  (이미 직전 작업에서 accessToken 방식으로 맞춰뒀음 — 이번에 바꿀 것 없음).

## Spec (무엇을·어떻게)

### 1. 버전 카탈로그 — 암호화 저장소
`gradle/libs.versions.toml`에 추가:
```
# 버전 섹션에 추가
securityCrypto = "1.1.0"
```
```
# alias 섹션에 추가 (kakao-user 근처 또는 AndroidX 섹션)
androidx-security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "securityCrypto" }
```

### 2. `core:data/build.gradle.kts`
- `implementation(libs.androidx.security.crypto)` 추가.
- 이 모듈에 테스트가 하나도 없었다(`build.gradle.kts`에 testImplementation 없음). 아래 4개를
  다른 모듈(`feature/entry/build.gradle.kts` 참고)과 동일하게 추가:
  ```kotlin
  testImplementation(libs.junit.jupiter)
  testRuntimeOnly(libs.junit.platform.launcher)
  testImplementation(libs.mockk)
  testImplementation(libs.kotlinx.coroutines.test)
  ```
  (turbine은 이번 테스트에 안 씀 — 추가하지 않아도 됨)

### 3. `core:domain` — 인증 에러 타입
`core/domain/src/main/kotlin/com/dororong/rodi/core/domain/AuthException.kt` 신규:
```kotlin
package com.dororong.rodi.core.domain

sealed class AuthException(message: String) : Exception(message) {
    class InvalidRequest(message: String) : AuthException(message)
    class InvalidCredential(message: String) : AuthException(message)
    class Network(message: String) : AuthException(message)
    class Unknown(message: String) : AuthException(message)
}
```

`core/domain/src/main/kotlin/com/dororong/rodi/core/domain/AuthRepository.kt` 수정
(기존 `suspend fun loginWithKakao(kakaoAccessToken: String)`를 아래로 교체 — 반환값 추가):
```kotlin
package com.dororong.rodi.core.domain

interface AuthRepository {
    /** @return isNewMember(이번 로그인으로 신규 가입됐는지) */
    suspend fun loginWithKakao(kakaoAccessToken: String): Boolean
}
```

`core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/LoginWithKakaoUseCase.kt`
수정(반환 타입만 `Result<Unit>` → `Result<Boolean>`):
```kotlin
package com.dororong.rodi.core.domain.usecase

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.AuthRepository
import javax.inject.Inject

class LoginWithKakaoUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(kakaoAccessToken: String): Result<Boolean> =
        runSuspendCatching { authRepository.loginWithKakao(kakaoAccessToken) }
}
```

### 4. `core:data` — 응답 envelope + 에러 매핑
`core/data/src/main/java/com/dororong/rodi/core/data/network/ApiEnvelope.kt` 신규:
```kotlin
package com.dororong.rodi.core.data.network

import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelope<T>(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val data: T? = null,
)
```

`core/data/src/main/java/com/dororong/rodi/core/data/network/AuthErrorMapper.kt` 신규
(HttpException의 에러 바디에서 `code`/`message`를 읽어 도메인 예외로 매핑):
```kotlin
package com.dororong.rodi.core.data.network

import com.dororong.rodi.core.domain.AuthException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

@Serializable
private data class AuthErrorBody(val code: String = "", val message: String = "")

fun Throwable.toAuthException(json: Json): AuthException = when (this) {
    is CancellationException -> throw this
    is HttpException -> {
        val body = response()?.errorBody()?.string()
        val parsed = body?.let { runCatching { json.decodeFromString<AuthErrorBody>(it) }.getOrNull() }
        val message = parsed?.message?.ifBlank { null } ?: "로그인 요청이 거부되었습니다."
        when (parsed?.code) {
            "AUTH_401_5" -> AuthException.InvalidCredential(message)
            "COMMON_400", "AUTH_400_1" -> AuthException.InvalidRequest(message)
            else -> AuthException.Unknown(message)
        }
    }
    is IOException -> AuthException.Network("네트워크 연결을 확인해주세요.")
    else -> AuthException.Unknown(message ?: "알 수 없는 오류가 발생했습니다.")
}
```

### 5. `core:data` — 로그인 API
`core/data/src/main/java/com/dororong/rodi/core/data/auth/AuthApi.kt` 신규:
```kotlin
package com.dororong.rodi.core.data.auth

import com.dororong.rodi.core.data.network.ApiEnvelope
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApi {
    @POST("auth/oauth/{provider}")
    suspend fun oauthLogin(
        @Path("provider") provider: String,
        @Body request: OAuthLoginRequest,
    ): ApiEnvelope<AuthTokenResponse>
}

@Serializable
data class OAuthLoginRequest(val credential: String)

@Serializable
data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val isNewMember: Boolean,
)
```

### 6. `core:data` — 세션 저장소(암호화)
`core/data/src/main/java/com/dororong/rodi/core/data/auth/AuthTokenStore.kt` 신규:
```kotlin
package com.dororong.rodi.core.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로그인 세션(액세스/리프레시 토큰)을 EncryptedSharedPreferences에 저장한다.
 * DataStore를 쓰지 않는 이유: 토큰은 평문 보관 금지 대상이라 Android Keystore 기반
 * 암호화가 필요하다(백엔드 문서의 "안전한 저장소" 요구사항).
 */
@Singleton
class AuthTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "auth_secure_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    val accessToken: String? get() = prefs.getString(KEY_ACCESS_TOKEN, null)
    val refreshToken: String? get() = prefs.getString(KEY_REFRESH_TOKEN, null)
    val isLoggedIn: Boolean get() = refreshToken != null

    fun save(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
```
`AuthTokenStore`는 Hilt 싱글톤이므로 Compose 쪽에서는 `AuthTokenStoreEntryPoint`를 통해
애플리케이션 컴포넌트에서 가져온다. 내부가 EncryptedSharedPreferences라 저장 데이터는 같은 파일을
보지만, Keystore 초기화 비용과 인스턴스 수명 관리를 위해 직접 생성하지 않는다.

### 7. `core:data` — `AuthRepositoryImpl` 실제 구현으로 교체
`core/data/src/main/java/com/dororong/rodi/core/data/AuthRepositoryImpl.kt` 전체 교체:
```kotlin
package com.dororong.rodi.core.data

import com.dororong.rodi.core.data.auth.AuthApi
import com.dororong.rodi.core.data.auth.AuthTokenStore
import com.dororong.rodi.core.data.auth.OAuthLoginRequest
import com.dororong.rodi.core.data.network.toAuthException
import com.dororong.rodi.core.domain.AuthException
import com.dororong.rodi.core.domain.AuthRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: AuthTokenStore,
    private val json: Json,
) : AuthRepository {
    override suspend fun loginWithKakao(kakaoAccessToken: String): Boolean {
        val envelope = runCatching { authApi.oauthLogin("kakao", OAuthLoginRequest(kakaoAccessToken)) }
            .getOrElse { throw it.toAuthException(json) }
        val body = envelope.data ?: throw AuthException.Unknown("응답에 로그인 정보가 없습니다.")
        tokenStore.save(body.accessToken, body.refreshToken)
        // isNewMember: 온보딩(닉네임 설정) 화면이 생기면 이 값으로 분기(BACKLOG 참고). 현재는 미사용.
        return body.isNewMember
    }
}
```
`DataModule.kt`의 기존 `bindAuthRepository` 바인딩은 그대로 둔다(이미 있음, 변경 불필요).

### 8. `core:data` — `NetworkModule` 갱신
`core/data/src/main/java/com/dororong/rodi/core/data/di/NetworkModule.kt`:
- `BASE_URL` 값과 주석 교체:
  ```kotlin
  // 로그인/토큰 API 서버. Notion "카카오 로그인 API 연동 가이드" 기준.
  private const val BASE_URL = "https://api.stillstar.store/api/v1/"
  ```
- `AuthApi` provider 추가:
  ```kotlin
  @Provides
  @Singleton
  fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)
  ```
  (import `com.dororong.rodi.core.data.auth.AuthApi` 추가)

### 9. `:feature:auth` — ViewModel 에러 메시지 연동
`feature/auth/src/main/java/com/dororong/rodi/feature/auth/LoginViewModel.kt`의
`onKakaoLoginResult`를 아래로 교체(반환 타입이 `Result<Boolean>`으로 바뀌었고, 서버 에러
메시지를 스낵바에 그대로 노출):
```kotlin
fun onKakaoLoginResult(accessToken: String) {
    _uiState.update { LoginUiState.LoggingIn }
    viewModelScope.launch {
        loginWithKakaoUseCase(accessToken)
            .onSuccess { _effect.send(LoginEffect.NavigateNext) }
            .onFailure { error ->
                val message = (error as? AuthException)?.message
                    ?: "로그인에 실패했습니다. 잠시 후 다시 시도해주세요."
                _effect.send(LoginEffect.ShowSnackbar(message))
                _uiState.update { LoginUiState.Idle }
            }
    }
}
```
(import `com.dororong.rodi.core.domain.AuthException` 추가. `feature:auth`는 이미
`core:domain`에 의존하므로 build.gradle.kts 변경 불필요)

### 10. 앱 라우팅 — 세션 있으면 로그인 화면 스킵
`app/src/main/java/com/dororong/rodi/ui/RodiApp.kt` 수정:
```kotlin
val tokenStore = remember {
    EntryPointAccessors.fromApplication(
        context.applicationContext,
        AuthTokenStoreEntryPoint::class.java,
    ).authTokenStore()
}
```
를 `prefs`/`backStack` 선언 근처에 추가하고, 백스택 최초 진입 로직을 아래로 교체:
```kotlin
LaunchedEffect(completedValue) {
    if (backStack.isEmpty()) {
        val destination = if (tokenStore.isLoggedIn) {
            if (completedValue) HomeRoute else EntryRoute
        } else {
            LoginRoute
        }
        backStack.add(destination)
    }
}
```
(import `com.dororong.rodi.core.data.auth.AuthTokenStore` 추가. `LoginRoute`의
`onNavigateNext` 콜백 내용은 기존 그대로 — 이미 `if (completedValue) HomeRoute else
EntryRoute`로 되어 있음, 변경 불필요)

### 11. 테스트
`docs/TESTING.md` 컨벤션(JUnit5 + MockK, 백틱 서술형).

- `core/data/src/test/java/com/dororong/rodi/core/data/network/AuthErrorMapperTest.kt` 신규
  (순수 JVM 테스트 — Android 프레임워크 불필요, `retrofit2.HttpException`을
  `Response.error(...)`로 직접 구성):
  ```kotlin
  package com.dororong.rodi.core.data.network

  import com.dororong.rodi.core.domain.AuthException
  import kotlinx.serialization.json.Json
  import okhttp3.MediaType.Companion.toMediaType
  import okhttp3.ResponseBody.Companion.toResponseBody
  import org.junit.jupiter.api.Assertions.assertEquals
  import org.junit.jupiter.api.Assertions.assertTrue
  import org.junit.jupiter.api.Test
  import retrofit2.HttpException
  import retrofit2.Response
  import java.io.IOException

  class AuthErrorMapperTest {
      private val json = Json { ignoreUnknownKeys = true }

      private fun httpException(code: Int, body: String) =
          HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

      @Test
      fun `maps AUTH_401_5 to InvalidCredential`() {
          val exception = httpException(
              401,
              """{"isSuccess":false,"code":"AUTH_401_5","message":"카카오 토큰이 유효하지 않습니다."}""",
          )

          val result = exception.toAuthException(json)

          assertTrue(result is AuthException.InvalidCredential)
          assertEquals("카카오 토큰이 유효하지 않습니다.", result.message)
      }

      @Test
      fun `maps COMMON_400 to InvalidRequest`() {
          val exception = httpException(400, """{"isSuccess":false,"code":"COMMON_400","message":"입력값이 올바르지 않습니다."}""")

          val result = exception.toAuthException(json)

          assertTrue(result is AuthException.InvalidRequest)
      }

      @Test
      fun `maps unknown error code to Unknown`() {
          val exception = httpException(500, """{"isSuccess":false,"code":"COMMON_500","message":"서버 오류"}""")

          val result = exception.toAuthException(json)

          assertTrue(result is AuthException.Unknown)
      }

      @Test
      fun `maps IOException to Network`() {
          val result = IOException("연결 실패").toAuthException(json)

          assertTrue(result is AuthException.Network)
      }
  }
  ```

- `core/domain/src/test/kotlin/com/dororong/rodi/core/domain/usecase/LoginWithKakaoUseCaseTest.kt`
  수정: repository mock이 `Boolean`을 반환/던지도록 갱신(현재는 `Unit` 기준으로 작성돼 있음).
  성공 케이스는 `coEvery { repository.loginWithKakao(...) } returns true`로,
  실패 케이스는 `coEvery { ... } throws AuthException.InvalidCredential("boom")`으로.

- `feature/auth/src/test/java/com/dororong/rodi/feature/auth/LoginViewModelTest.kt` 수정:
  - 성공 케이스: `coEvery { useCase("access-token") } returns Result.success(true)` (또는
    `false`) — 어느 쪽이든 `NavigateNext`만 emit되면 됨(현재는 분기 없음).
  - 실패 케이스: `coEvery { useCase("access-token") } returns
    Result.failure(AuthException.InvalidCredential("카카오 인증에 실패했습니다."))`로 바꾸고,
    `ShowSnackbar("카카오 인증에 실패했습니다.")`가 emit되는지 검증(서버 메시지가 그대로
    전달되는 것 확인 — 하드코딩된 일반 메시지가 아님).

### 12. 문서
- `docs/PROJECT.md`의 `:core:data` 모듈 맵 행에 `AuthApi`/`AuthTokenStore` 추가:
  `| \`:core:data\` | \`EntryPreferences\`(DataStore), \`SampleCourses\`,
  \`KakaoDirectionsClient\`(REST), \`NaviPreference\`, \`AuthApi\`/\`AuthTokenStore\`(로그인,
  EncryptedSharedPreferences) |`
- `docs/BACKLOG.md` 열린 항목에 아래 3개를 한 줄씩 추가(Claude가 이미 추가해둠 — Codex는 이
  파일을 건드릴 필요 없음):
  1. 보호 API 자동 토큰 갱신(OkHttp Authenticator)
  2. 로그아웃 API 연동
  3. `isNewMember` 기반 온보딩 분기

## Files to touch
- `gradle/libs.versions.toml`
- `core/data/build.gradle.kts`
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/AuthException.kt` (신규)
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/AuthRepository.kt`
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/LoginWithKakaoUseCase.kt`
- `core/domain/src/test/kotlin/com/dororong/rodi/core/domain/usecase/LoginWithKakaoUseCaseTest.kt`
- `core/data/src/main/java/com/dororong/rodi/core/data/network/ApiEnvelope.kt` (신규)
- `core/data/src/main/java/com/dororong/rodi/core/data/network/AuthErrorMapper.kt` (신규)
- `core/data/src/test/java/com/dororong/rodi/core/data/network/AuthErrorMapperTest.kt` (신규)
- `core/data/src/main/java/com/dororong/rodi/core/data/auth/AuthApi.kt` (신규)
- `core/data/src/main/java/com/dororong/rodi/core/data/auth/AuthTokenStore.kt` (신규)
- `core/data/src/main/java/com/dororong/rodi/core/data/AuthRepositoryImpl.kt`
- `core/data/src/main/java/com/dororong/rodi/core/data/di/NetworkModule.kt`
- `feature/auth/src/main/java/com/dororong/rodi/feature/auth/LoginViewModel.kt`
- `feature/auth/src/test/java/com/dororong/rodi/feature/auth/LoginViewModelTest.kt`
- `app/src/main/java/com/dororong/rodi/ui/RodiApp.kt`
- `docs/PROJECT.md`

## Acceptance criteria
- [ ] 카카오 로그인 버튼으로 실제 카카오 액세스 토큰을 받으면 `POST
      /api/v1/auth/oauth/kakao`가 호출되고, 성공 시 서버 accessToken/refreshToken이
      `AuthTokenStore`(EncryptedSharedPreferences)에 저장된다.
- [ ] 로그인 성공 후 다음 화면(둘러보기와 동일한 목적지)으로 진행한다.
- [ ] 서버가 400/401 에러(`COMMON_400`/`AUTH_400_1`/`AUTH_401_5`)를 내려주면 그 `message`가
      스낵바에 그대로 노출된다(하드코딩 문구로 뭉개지 않음).
- [ ] 네트워크 자체가 끊긴 경우(`IOException`)에도 스낵바로 실패를 알린다.
- [ ] 앱을 재시작했을 때 `AuthTokenStore.isLoggedIn`이 true면 로그인 화면 없이 바로 다음
      화면(EntryRoute/HomeRoute)으로 진입한다.
- [ ] `core:data`에 테스트 인프라(JUnit5/MockK)가 새로 갖춰지고 `AuthErrorMapperTest` 4개
      케이스가 통과한다.
- [ ] `LoginWithKakaoUseCaseTest`/`LoginViewModelTest`가 `Boolean` 반환 기준으로 갱신되어
      통과한다.
- [ ] `./gradlew assembleDebug`, `./gradlew test` 모두 GREEN.

## Verification
```
./gradlew assembleDebug
./gradlew test
```
에뮬레이터에 설치해 카카오 로그인 버튼 클릭 → (카카오 계정으로 실제 로그인 가능하면) 서버
응답 후 다음 화면 진입까지 확인. 실제 카카오 계정 로그인이 불가능한 환경이면 최소한
"둘러보기 없이 로그인 버튼만 눌렀을 때 카카오 로그인 화면/브라우저로 전환되는 것"까지 확인하고,
서버 호출 자체는 코드 리뷰 + 단위 테스트로 검증.

## Out of scope
- `POST /auth/token/refresh` 연동, 401 자동 재시도(OkHttp Authenticator) — 보호 API가
  하나도 없어 지금 만들면 검증 불가. BACKLOG에 설계 유의사항과 함께 등록해둠(동시 재발급 호출 시
  refreshToken 재사용 탐지로 전 세션이 폐기되는 서버 정책이 있어 single-flight 락이 필수).
- `POST /auth/logout` 연동 — 로그아웃 트리거를 놓을 화면(프로필/설정)이 아직 없음.
- `isNewMember`에 따른 온보딩 분기 — 온보딩 화면이 아직 없음. 값은 반환하되 미사용.
- 로컬 백엔드(`localhost:8080`) 지원 — 운영 서버로 고정(사용자 결정).
- 보호 API 호출용 `Authorization` 헤더 자동 첨부(인터셉터) — 아직 우리 보호 API가 없어서
  같이 만들면 죽은 코드. 보호 API 첫 등장 시 refresh 연동과 함께.

---
## Codex Result   <!-- Codex가 구현 후 채움 → Status=IMPL_DONE (또는 막히면 BLOCKED) -->
- Changed files: gradle/libs.versions.toml, core/data/build.gradle.kts, core/domain/src/main/kotlin/com/dororong/rodi/core/domain/AuthException.kt, core/domain/src/main/kotlin/com/dororong/rodi/core/domain/AuthRepository.kt, core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/LoginWithKakaoUseCase.kt, core/domain/src/test/kotlin/com/dororong/rodi/core/domain/usecase/LoginWithKakaoUseCaseTest.kt, core/data/src/main/java/com/dororong/rodi/core/data/network/ApiEnvelope.kt, core/data/src/main/java/com/dororong/rodi/core/data/network/AuthErrorMapper.kt, core/data/src/test/java/com/dororong/rodi/core/data/network/AuthErrorMapperTest.kt, core/data/src/main/java/com/dororong/rodi/core/data/auth/AuthApi.kt, core/data/src/main/java/com/dororong/rodi/core/data/auth/AuthTokenStore.kt, core/data/src/main/java/com/dororong/rodi/core/data/AuthRepositoryImpl.kt, core/data/src/main/java/com/dororong/rodi/core/data/di/NetworkModule.kt, feature/auth/src/main/java/com/dororong/rodi/feature/auth/LoginViewModel.kt, feature/auth/src/test/java/com/dororong/rodi/feature/auth/LoginViewModelTest.kt, app/src/main/java/com/dororong/rodi/ui/RodiApp.kt, docs/PROJECT.md, docs/handoff/HANDOFF.md
- Build/test: ./gradlew test GREEN, ./gradlew assembleDebug GREEN
- Open questions: none

---
## Claude Review  <!-- Claude가 검토 후 채움 -->
- Blocking: 없음
- Nits: `docs/PROJECT.md`의 `:feature:auth` 행이 "서버 연동 전"으로 남아 있어 Claude가 직접
  "서버 로그인 API 연동 완료(재발급/로그아웃은 미연동)"으로 수정함(스펙에 명시 안 된 문서
  정확성 보정, Codex 재작업 불필요).
- Verdict: APPROVE — diff가 스펙과 거의 1:1로 일치(envelope/에러 매핑/EncryptedSharedPreferences/
  라우팅 게이팅 전부 지시대로). `./gradlew assembleDebug`/`test` GREEN 재확인 완료.
  에뮬레이터 시각 검증은 공유 에뮬레이터에 로그인 흐름과 무관한 다른 앱(`com.cmc.routi`)이
  계속 포그라운드를 가로채는 환경 이슈로 카카오 버튼 탭 이후 전환까지는 못 봤음(신규 설치 시
  세션 없으면 로그인 화면이 정상적으로 뜨는 것은 확인). 실제 카카오 계정 로그인 왕복은
  이 환경에 로그인된 카카오 계정이 없어 어차피 완전한 시각 검증이 불가능한 상태였고, HANDOFF의
  Verification 섹션에 명시한 대로 코드 리뷰 + 단위 테스트(AuthErrorMapperTest 4종,
  LoginWithKakaoUseCaseTest, LoginViewModelTest)로 대체 검증함.
---
