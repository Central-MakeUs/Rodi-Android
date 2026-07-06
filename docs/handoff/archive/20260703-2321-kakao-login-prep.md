# HANDOFF — 카카오 로그인 준비(임시 로그인 화면 + SDK 로직)

> Claude(기획)와 Codex(구현)가 주고받는 **단일 활성 작업 채널**.
> 완료되면 `docs/handoff/archive/<날짜>-<작업>.md`로 옮기고 이 파일은 다음 작업으로 비운다.

Status: IMPL_DONE
Branch: feature/kakao-login-prep

## Context (왜)
서버 연동 전, 카카오 로그인 화면과 SDK 로직을 미리 준비해둔다. 서버 API 스펙이 아직 없으므로
이번 작업은 "임시" 로그인 화면 UI + 카카오 SDK 인증 로직까지만 만들고, 서버로 토큰을 보내는
지점은 `AuthRepositoryImpl` 안에 자리만 잡아둔다(이미 `NetworkModule.kt`의 `BASE_URL` placeholder와
동일한 패턴). 카카오 로그인 로직은 `/Users/uihyeon/StudioProjects/dnd-14th-2-android`의
`presentation/src/main/java/com/smtm/pickle/presentation/common/auth/KakaoLoginManager.kt`를
참고 원천으로 삼는다(패키지/네이밍은 Rodi 컨벤션으로 교체).

## Spec (무엇을·어떻게)

### 1. 새 모듈 `:feature:auth`
`docs/ARCHITECTURE_TARGET.md`의 "Add a feature module" 절차를 따른다.
- `feature/auth/build.gradle.kts`는 `feature/entry/build.gradle.kts`를 그대로 복사하고
  namespace만 `com.dororong.rodi.feature.auth`로 바꾼 뒤, `implementation(libs.kakao.user)` 한 줄을
  추가한다(다른 의존성은 entry와 동일: hilt, compose, activity-compose, hilt-navigation-compose,
  테스트 4종 — junit-jupiter/mockk/coroutines-test/turbine).
- `settings.gradle.kts`에 `include(":feature:auth")` 추가.
- `app/build.gradle.kts`의 `dependencies` 블록에 `implementation(project(":feature:auth"))` 추가
  (`feature:entry`/`feature:home` 옆에).

### 2. 버전 카탈로그
`gradle/libs.versions.toml`의 `kakao-navi` 라인 바로 아래에 카카오 로그인 SDK alias 추가:
```
kakao-user = { group = "com.kakao.sdk", name = "v2-user", version.ref = "kakaoSdk" }
```
(버전은 기존 `kakaoSdk = "2.20.6"` 재사용, `kakao-navi`와 동일 버전 참조)

### 3. `core:domain` — 로그인 시드(서버 연동 지점)
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/AuthRepository.kt` 신규:
  ```kotlin
  package com.dororong.rodi.core.domain

  interface AuthRepository {
      suspend fun loginWithKakao(kakaoAccessToken: String)
  }
  ```
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/LoginWithKakaoUseCase.kt` 신규,
  `GetRouteUseCase.kt`와 동일 패턴(`runSuspendCatching`으로 감싸 `Result<Unit>` 반환):
  ```kotlin
  class LoginWithKakaoUseCase @Inject constructor(
      private val authRepository: AuthRepository,
  ) {
      suspend operator fun invoke(kakaoAccessToken: String): Result<Unit> =
          runSuspendCatching { authRepository.loginWithKakao(kakaoAccessToken) }
  }
  ```

### 4. `core:data` — placeholder 구현
- `core/data/src/main/java/com/dororong/rodi/core/data/AuthRepositoryImpl.kt` 신규:
  ```kotlin
  package com.dororong.rodi.core.data

  import com.dororong.rodi.core.domain.AuthRepository
  import javax.inject.Inject

  // 로그인 API 확정 전 placeholder(NetworkModule.BASE_URL과 동일 사유).
  // 현재는 카카오 액세스 토큰 획득 성공을 곧 로그인 성공으로 간주한다.
  // 서버 연동 시 이 안에서 Retrofit 호출로 토큰을 전달하고 응답(세션/회원 상태 등)을 반영하도록 교체.
  class AuthRepositoryImpl @Inject constructor() : AuthRepository {
      override suspend fun loginWithKakao(kakaoAccessToken: String) {
      }
  }
  ```
- `core/data/src/main/java/com/dororong/rodi/core/data/di/DataModule.kt`에 바인딩 추가:
  ```kotlin
  @Binds
  abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
  ```

### 5. `:feature:auth` — 카카오 SDK 로그인 매니저
`KakaoLoginManager`는 Activity 컨텍스트가 필요하다(`loginWithKakaoTalk`가 액티비티 결과를 받아야
함). Hilt `ActivityComponent` + `@EntryPoint` 패턴을 참고 프로젝트 그대로 이식한다.

`feature/auth/src/main/java/com/dororong/rodi/feature/auth/KakaoLoginManager.kt`:
```kotlin
package com.dororong.rodi.feature.auth

import android.content.Context
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@EntryPoint
@InstallIn(ActivityComponent::class)
interface KakaoLoginManagerEntryPoint {
    fun kakaoLoginManager(): KakaoLoginManager
}

@ActivityScoped
class KakaoLoginManager @Inject constructor(
    @ActivityContext private val context: Context,
) {
    fun login(onSuccess: (accessToken: String) -> Unit, onFailure: (message: String) -> Unit) {
        val resultHandler: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            when {
                token?.accessToken != null -> onSuccess(token.accessToken)
                else -> onFailure("로그인에 실패했습니다. 잠시 후 다시 시도해주세요.")
            }
        }

        if (!UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            UserApiClient.instance.loginWithKakaoAccount(context, callback = resultHandler)
            return
        }

        UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
            when {
                token?.accessToken != null -> onSuccess(token.accessToken)
                error is ClientError && error.reason == ClientErrorCause.Cancelled ->
                    onFailure("로그인이 취소되었습니다.")
                error != null ->
                    UserApiClient.instance.loginWithKakaoAccount(context, callback = resultHandler)
                else -> onFailure("로그인에 실패했습니다. 잠시 후 다시 시도해주세요.")
            }
        }
    }
}
```
참고 프로젝트는 `idToken`을 쓰지만(OIDC 별도 설정 필요), Rodi 카카오 앱의 OIDC 활성화 여부가
불확실하므로 이번 구현은 항상 존재하는 `accessToken`을 사용한다. 서버가 idToken을 요구하면
이 지점만 바꾸면 된다 — **Open Questions에 이 결정을 기록**.

### 6. `:feature:auth` — ViewModel
`feature/auth/src/main/java/com/dororong/rodi/feature/auth/LoginViewModel.kt`:
- `LoginUiState`: `sealed interface { data object Idle; data object LoggingIn }`
- `LoginEffect`: `sealed interface { data object NavigateNext; data class ShowSnackbar(val message: String) }`
- `uiState: StateFlow<LoginUiState>`, `effect: Flow<LoginEffect>` — `feature/home/HomeViewModel.kt`의
  `Channel<HomeEffect>(Channel.BUFFERED)` + `receiveAsFlow()` 패턴을 그대로 따른다(entry 모듈의
  `SharedFlow` 방식이 아니라 home 모듈 방식으로 통일).
- `fun onKakaoLoginResult(accessToken: String)`: `uiState = LoggingIn` → `LoginWithKakaoUseCase(accessToken)`
  → 성공 시 `NavigateNext` emit, 실패 시 `ShowSnackbar("로그인에 실패했습니다. 잠시 후 다시 시도해주세요.")`
  emit 후 `uiState = Idle`.
- `fun onKakaoLoginFailed(message: String)`: `ShowSnackbar(message)` emit(카카오 SDK 단계에서 이미
  실패/취소된 경우, 유스케이스 호출 없이 바로).
- `fun onSkipClick()`: 유스케이스 호출 없이 바로 `NavigateNext` emit.

### 7. `:feature:auth` — 화면
`feature/auth/src/main/java/com/dororong/rodi/feature/auth/LoginScreen.kt`:
- `LoginScreen(onNavigateNext: () -> Unit, viewModel: LoginViewModel = hiltViewModel())`
  - `LocalActivity.current`로 액티비티를 얻어 `EntryPointAccessors.fromActivity(it,
    KakaoLoginManagerEntryPoint::class.java).kakaoLoginManager()`를 `remember(activity)`로 생성.
  - `remember { RodiSnackbarHostState() }` 생성.
  - `LaunchedEffect(viewModel) { viewModel.effect.collect { ... } }`로 `HomeScreen.kt`와 동일하게
    effect 구독(`repeatOnLifecycle` 쓰지 않음 — Home 컨벤션 그대로).
    - `NavigateNext` → `onNavigateNext()` 호출
    - `ShowSnackbar` → `snackbarHostState.show(RodiSnackbarData(message = effect.message))`
  - `LoginContent(uiState = ..., onKakaoLoginClick = { kakaoLoginManager?.login(onSuccess =
    viewModel::onKakaoLoginResult, onFailure = viewModel::onKakaoLoginFailed) }, onSkipClick =
    viewModel::onSkipClick)`
  - `RodiSnackbarHost(snackbarHostState)`
- `LoginContent(modifier, uiState, onKakaoLoginClick, onSkipClick)` — `Box.fillMaxSize()` +
  `background(RodiTheme.colors.white)` + `statusBarsPadding()` + `navigationBarsPadding()`:
  - 우상단: `TextButton`(또는 `Text` + `clickable`) "둘러보기", `Modifier.align(Alignment.TopEnd)
    .padding(RodiSpacing.md)`, `RodiTheme.typography.body2Medium`, `RodiTheme.colors.gray500`,
    `onClick = onSkipClick`.
  - 정중앙: 앱 이름 텍스트 `"Rodi"`, `Modifier.align(Alignment.Center)`,
    `RodiTheme.typography.heading1`, `RodiTheme.colors.primary600`.
  - 하단: `KakaoLoginButton(onClick = onKakaoLoginClick, enabled = uiState !=
    LoginUiState.LoggingIn, modifier = Modifier.align(Alignment.BottomCenter)
    .padding(horizontal = RodiSpacing.md, vertical = 40.dp))`.
- `@Preview(showBackground = true, widthDp = 360, heightDp = 760)`로 `LoginContent` Idle 상태 최소
  1개 작성(기존 `LocationPermissionContent` Preview 패턴 참고).

### 8. `:feature:auth` — 카카오 로그인 버튼
`feature/auth/src/main/java/com/dororong/rodi/feature/auth/KakaoLoginButton.kt`:
- `Button` 48dp 높이, `RodiRadius.md` 모양(= `RodiButtonDefaults.Height`/`shape()` 재사용 가능).
- 카카오 브랜드 가이드 고정 색상(`#FEE500` 배경 / `#191919` 텍스트·아이콘)은 `RodiTheme` 토큰이
  아니므로 파일 내 `private val`로 하드코딩하고, 아래 주석을 정확히 남긴다:
  `// 카카오 브랜드 가이드 고정 색상 — RodiTheme 토큰 대상 아님. SemanticColors 도입(BACKLOG) 시 이관 검토.`
- 아이콘(`ic_kakao`) + 텍스트("카카오로 시작하기", `RodiTheme.typography.button1`) 좌우 배치.
- `enabled = false`일 때 대체 텍스트("로그인 중...")까지는 요구하지 않음 — `enabled` 하나로 중복
  클릭만 막으면 충분(임시 화면이므로 로딩 스피너 등 폴리싱은 Out of scope).

### 9. 카카오 아이콘 리소스
`/Users/uihyeon/StudioProjects/dnd-14th-2-android/presentation/src/main/res/drawable/icon_kakao.xml`을
`feature/auth/src/main/res/drawable/ic_kakao.xml`로 복사(파일명만 Rodi 컨벤션 `ic_` 접두사로 변경,
내용은 그대로).

### 10. 앱 라우팅 연결
- `app/src/main/java/com/dororong/rodi/ui/RodiRoute.kt`에 `@Serializable data object LoginRoute` 추가.
- `app/src/main/java/com/dororong/rodi/ui/RodiApp.kt`:
  - 백스택 최초 진입점을 `LoginRoute`로 바꾼다: `LaunchedEffect(completedValue) { if
    (backStack.isEmpty()) backStack.add(LoginRoute) }` (기존 `if (completedValue) HomeRoute else
    EntryRoute` 분기는 `LoginRoute` 진입 후 다음 목적지 결정 로직으로 이동).
  - `entryProvider`에 `LoginRoute` 케이스 추가:
    ```kotlin
    LoginRoute -> NavEntry(key) {
        LoginScreen(
            onNavigateNext = {
                backStack.clear()
                backStack.add(if (completedValue) HomeRoute else EntryRoute)
            },
        )
    }
    ```
  - 카카오 로그인 성공/둘러보기 모두 동일하게 `onNavigateNext`를 호출한다(서버가 없어 로그인
    여부를 구분해 저장할 데이터가 아직 없음 — 둘 다 같은 다음 화면으로 진행).

### 11. AndroidManifest — 카카오 로그인 리다이렉트
`app/src/main/AndroidManifest.xml`의 `<application>` 안, `MainActivity` 옆에 카카오 계정(웹) 로그인
리다이렉트를 받는 액티비티를 추가한다(`manifestPlaceholders["KAKAO_NATIVE_APP_KEY"]`는
`app/build.gradle.kts`에 이미 설정돼 있음 — 이번에 실제로 처음 소비됨):
```xml
<activity
    android:name="com.kakao.sdk.auth.AuthCodeHandlerActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="kakao${KAKAO_NATIVE_APP_KEY}" android:host="oauth" />
    </intent-filter>
</activity>
```

### 12. 문서
`docs/PROJECT.md`의 "모듈 맵" 표에 한 행 추가:
`| `:feature:auth` | 카카오 로그인(임시 화면 + SDK 로직, 서버 연동 전) |`

### 13. 테스트
`docs/TESTING.md` 컨벤션(JUnit5 + MockK + Turbine, 백틱 서술형 함수명) 그대로.
- `core/domain/src/test/kotlin/com/dororong/rodi/core/domain/usecase/LoginWithKakaoUseCaseTest.kt`
  — `GetRouteUseCaseTest.kt` 템플릿 참고. 성공/실패(repository가 던진 예외를 `Result.failure`로
  감싸는지) 케이스.
- `feature/auth/src/test/java/com/dororong/rodi/feature/auth/LoginViewModelTest.kt` —
  `feature/entry/.../EntryViewModelTest.kt` 템플릿 참고. `onKakaoLoginResult` 성공 시
  `NavigateNext` emit, 실패 시 `ShowSnackbar` emit + `uiState`가 다시 `Idle`로 복귀하는지,
  `onSkipClick` 호출 시 유스케이스를 호출하지 않고 바로 `NavigateNext`가 emit되는지 Turbine으로
  검증.

## Files to touch
- `gradle/libs.versions.toml`
- `settings.gradle.kts`
- `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/dororong/rodi/ui/RodiRoute.kt`, `RodiApp.kt`
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/AuthRepository.kt` (신규)
- `core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/LoginWithKakaoUseCase.kt` (신규)
- `core/domain/src/test/kotlin/com/dororong/rodi/core/domain/usecase/LoginWithKakaoUseCaseTest.kt` (신규)
- `core/data/src/main/java/com/dororong/rodi/core/data/AuthRepositoryImpl.kt` (신규)
- `core/data/src/main/java/com/dororong/rodi/core/data/di/DataModule.kt`
- `feature/auth/build.gradle.kts` (신규 모듈)
- `feature/auth/src/main/java/com/dororong/rodi/feature/auth/KakaoLoginManager.kt` (신규)
- `feature/auth/src/main/java/com/dororong/rodi/feature/auth/LoginViewModel.kt` (신규)
- `feature/auth/src/main/java/com/dororong/rodi/feature/auth/LoginScreen.kt` (신규)
- `feature/auth/src/main/java/com/dororong/rodi/feature/auth/KakaoLoginButton.kt` (신규)
- `feature/auth/src/main/res/drawable/ic_kakao.xml` (신규)
- `feature/auth/src/test/java/com/dororong/rodi/feature/auth/LoginViewModelTest.kt` (신규)
- `docs/PROJECT.md` (모듈 맵 행 추가)

## Acceptance criteria
- [ ] 앱 최초 실행 시 로그인 화면이 뜬다: 중앙 "Rodi" 텍스트, 하단 카카오 로그인 버튼, 우상단
      "둘러보기" 버튼.
- [ ] "둘러보기" 클릭 시 기존 진입 흐름(위치권한/약관/주의사항 또는 이미 완료했다면 홈)으로 즉시
      진행한다.
- [ ] 카카오 로그인 버튼 클릭 시 카카오톡 설치 여부에 따라 카카오톡 로그인 또는 카카오계정(웹)
      로그인이 실행되고, 성공 시 같은 다음 화면으로 진행한다.
- [ ] 로그인 실패/취소 시 스낵바로 메시지를 보여주고 로그인 화면에 머문다.
- [ ] `AuthRepository`/`LoginWithKakaoUseCase`가 `core:domain`에 존재해 서버 연동 시
      `AuthRepositoryImpl` 내부만 교체하면 되는 구조다.
- [ ] `RodiTheme.colors`/`typography` 외 색상은 카카오 브랜드 색(주석으로 사유 명시)만 예외다.
      Material 아이콘(`Icons.*`) 사용 없음.
- [ ] core:ui 컴포넌트 추가 없음(이번 버튼/아이콘은 `feature:auth` 로컬).
- [ ] 새 테스트 2개(UseCase, ViewModel) 통과.

## Verification
```
./gradlew assembleDebug
./gradlew test
```
에뮬레이터에서 로그인 화면 진입 → 둘러보기 → 다음 화면 진행을 스크린샷으로 확인
(카카오 로그인 자체는 로컬 환경에 카카오 계정/키 설정이 필요해 시각 검증은 "둘러보기" 경로
위주로 하고, 카카오 버튼 클릭 시 시스템 브라우저/카카오톡으로 전환되는 것까지만 확인).

## Out of scope
- 실제 서버 로그인 API 호출(엔드포인트 미확정).
- 로그인 세션/토큰 영속화(DataStore 등) — `EntryPreferences`처럼 로그인 상태를 저장하는 것은
  서버 연동 이후 작업.
- 로그아웃 플로우.
- idToken 방식 전환(OIDC 설정 확인 필요 — Open Questions에 기록).
- `RodiTheme`에 카카오/구글 브랜드 `SemanticColors` 추가(`BACKLOG.md`에 이미 있는 "테마 시스템
  고도화" 항목 범위).
- 구글 로그인, 게스트 로그인 등 다른 소셜 로그인 수단.

---
## Codex Result   <!-- Codex가 구현 후 채움 → Status=IMPL_DONE (또는 막히면 BLOCKED) -->
- Changed files: gradle/libs.versions.toml, settings.gradle.kts, app/build.gradle.kts, app/src/main/AndroidManifest.xml, app/src/main/java/com/dororong/rodi/ui/RodiRoute.kt, app/src/main/java/com/dororong/rodi/ui/RodiApp.kt, core/domain/src/main/kotlin/com/dororong/rodi/core/domain/AuthRepository.kt, core/domain/src/main/kotlin/com/dororong/rodi/core/domain/usecase/LoginWithKakaoUseCase.kt, core/domain/src/test/kotlin/com/dororong/rodi/core/domain/usecase/LoginWithKakaoUseCaseTest.kt, core/data/src/main/java/com/dororong/rodi/core/data/AuthRepositoryImpl.kt, core/data/src/main/java/com/dororong/rodi/core/data/di/DataModule.kt, feature/auth/build.gradle.kts, feature/auth/src/main/java/com/dororong/rodi/feature/auth/KakaoLoginManager.kt, feature/auth/src/main/java/com/dororong/rodi/feature/auth/LoginViewModel.kt, feature/auth/src/main/java/com/dororong/rodi/feature/auth/LoginScreen.kt, feature/auth/src/main/java/com/dororong/rodi/feature/auth/KakaoLoginButton.kt, feature/auth/src/main/res/drawable/ic_kakao.xml, feature/auth/src/test/java/com/dororong/rodi/feature/auth/LoginViewModelTest.kt, docs/PROJECT.md, docs/handoff/HANDOFF.md
- Build/test: ./gradlew test GREEN; ./gradlew assembleDebug GREEN
- Open questions: 서버가 idToken을 요구하는지 미확정. 현재 구현은 Rodi 카카오 앱의 OIDC 활성화 여부가 불확실해 accessToken 기반으로 연결함.

---
## Claude Review  <!-- Claude가 검토 후 채움 -->
- Blocking: 없음
- Nits: 없음 — 스펙과 diff가 거의 1:1로 일치. `collectAsState()`(Home 컨벤션)를 정확히 따랐고,
  Effect Channel 패턴/카카오 브랜드색 주석/AuthRepositoryImpl placeholder 주석 모두 지시대로 작성됨.
  idToken vs accessToken 결정은 Open Questions에 잘 기록됨(서버 연동 시 확인 필요, Out of scope 그대로).
- Verdict: APPROVE
---
