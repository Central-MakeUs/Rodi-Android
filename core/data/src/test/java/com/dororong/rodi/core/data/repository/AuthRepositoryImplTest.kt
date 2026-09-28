package com.dororong.rodi.core.data.repository

import android.content.Context
import com.dororong.rodi.core.data.cache.PracticeRecordPresenceCache
import com.dororong.rodi.core.data.source.local.datastore.AuthTokenDataStore
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.local.security.AuthTokenMutationResult
import com.dororong.rodi.core.data.source.local.security.AuthTokens
import com.dororong.rodi.core.data.source.remote.api.AuthApi
import com.dororong.rodi.core.data.source.remote.model.auth.TokenRefreshResponse
import com.dororong.rodi.core.data.source.remote.model.auth.LogoutRequest
import com.dororong.rodi.core.data.source.remote.model.auth.OAuthLoginRequest
import com.dororong.rodi.core.data.source.remote.model.auth.SocialLoginRequest
import com.dororong.rodi.core.data.source.remote.model.auth.SocialLoginResponse
import com.dororong.rodi.core.data.source.remote.model.auth.TokenRefreshRequest
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.data.test.assertThrowsSuspend
import com.dororong.rodi.core.domain.model.auth.AccountRestoreResult
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.model.auth.LoginResult
import com.dororong.rodi.core.domain.repository.EntryRepository
import com.dororong.rodi.core.domain.repository.OnboardingRepository
import com.dororong.rodi.core.domain.repository.PracticeSessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import retrofit2.HttpException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.io.IOException

class AuthRepositoryImplTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val practiceSessionRepository = mockk<PracticeSessionRepository>(relaxed = true)
    private val onboardingRepository = mockk<OnboardingRepository>(relaxed = true)
    private val entryRepository = mockk<EntryRepository>(relaxed = true)

    private fun coordinator(
        tokenStore: AuthTokenStore,
        cache: PracticeRecordPresenceCache = PracticeRecordPresenceCache(),
    ) = AuthSessionCoordinator(tokenStore, practiceSessionRepository, cache, onboardingRepository, entryRepository)

    @Test
    fun `getSession은 한 번에 읽은 토큰 스냅샷으로 세션을 만든다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens(provider = "kakao")
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))

        val session = repository.getSession()

        assertTrue(session.isLoggedIn)
        assertTrue(session.hasRecentKakaoLogin)
    }

    @Test
    fun `토큰이 지워져도 최근 카카오 로그인 기록은 유지한다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns null
        coEvery { tokenStore.getRecentProvider() } returns "kakao"
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))

        val session = repository.getSession()

        assertFalse(session.isLoggedIn)
        assertTrue(session.hasRecentKakaoLogin)
    }

    @Test
    fun `카카오 로그인은 서버가 준 토큰을 저장한다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { authApi.oauthLogin("kakao", OAuthLoginRequest("kakao-token")) } returns loginEnvelope(isOnboarded = false)
        coEvery { tokenStore.save("access-new", "refresh-new", "kakao") } returns true
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))

        val result = repository.loginWithKakao("kakao-token")

        assertEquals(LoginResult.Success(isOnboarded = false, nickname = "서버 닉네임"), result)
        coVerify(exactly = 1) { practiceSessionRepository.clear() }
        coVerify { tokenStore.save("access-new", "refresh-new", "kakao") }
    }

    @Test
    fun `이전 연습 세션 정리가 실패해도 로그인은 성공한다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { authApi.oauthLogin("kakao", OAuthLoginRequest("kakao-token")) } returns loginEnvelope(isOnboarded = true)
        coEvery { tokenStore.save("access-new", "refresh-new", "kakao") } returns true
        coEvery { practiceSessionRepository.clear() } throws IOException("local storage unavailable")
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))

        assertTrue(repository.loginWithKakao("kakao-token") is LoginResult.Success)
        coVerify(exactly = 1) { tokenStore.save("access-new", "refresh-new", "kakao") }
        coVerify(exactly = 3) { practiceSessionRepository.clear() }
    }

    @Test
    fun `재발급은 현재 refresh 토큰을 교체한다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { authApi.reissue(TokenRefreshRequest("refresh-old")) } returns tokenEnvelope(false)
        coEvery { tokenStore.rotate(any(), "access-new", "refresh-new", false) } returns AuthTokenMutationResult.APPLIED
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))

        repository.reissueToken()

        coVerify(exactly = 1) { authApi.reissue(TokenRefreshRequest("refresh-old")) }
        coVerify { tokenStore.rotate(any(), "access-new", "refresh-new", false) }
    }

    @Test
    fun `재발급은 서버가 준 튜토리얼 완료 여부를 저장한다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access-old", "refresh-old", "kakao")
        coEvery { authApi.reissue(TokenRefreshRequest("refresh-old")) } returns tokenEnvelope(
            isOnboarded = true,
            isCourseTutorialCompleted = true,
        )
        coEvery { tokenStore.rotate(any(), "access-new", "refresh-new", true) } returns AuthTokenMutationResult.APPLIED
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))

        repository.reissueToken()

        coVerify(exactly = 1) { tokenStore.rotate(any(), "access-new", "refresh-new", true) }
    }

    @Test
    fun `같은 세션의 재발급은 연습 기록 여부 캐시를 유지한다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val cache = PracticeRecordPresenceCache().also { it.set(true) }
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { authApi.reissue(TokenRefreshRequest("refresh-old")) } returns tokenEnvelope(false)
        coEvery { tokenStore.rotate(any(), "access-new", "refresh-new", false) } returns AuthTokenMutationResult.APPLIED
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, cache))

        repository.reissueToken()

        assertEquals(true, cache.get())
        coVerify(exactly = 0) { practiceSessionRepository.clear() }
    }

    @Test
    fun `세션이 없으면 재발급 API를 호출하지 않는다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns null
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))

        assertThrowsSuspend<AuthException.NotAuthenticated> { repository.reissueToken() }

        coVerify(exactly = 0) { authApi.reissue(any()) }
    }

    @Test
    fun `refresh 토큰 재사용이 감지되면 로컬 토큰을 지운다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { authApi.reissue(TokenRefreshRequest("refresh-old")) } returns ApiEnvelope(
            isSuccess = false,
            code = "AUTH_401_4",
            message = "폐기된 토큰입니다.",
        )
        coEvery { tokenStore.clearSession(any()) } returns AuthTokenMutationResult.APPLIED
        coEvery { tokenStore.clearCourseRegistrationData() } returns Unit
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))

        assertThrowsSuspend<AuthException.SessionRevoked> { repository.reissueToken() }

        coVerify { tokenStore.clearSession(any()) }
        coVerify(exactly = 1) { practiceSessionRepository.clear() }
    }

    @Test
    fun `재발급 응답이 인증 실패면 세션 만료를 알린다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { authApi.reissue(TokenRefreshRequest("refresh-old")) } returns ApiEnvelope(
            isSuccess = false,
            code = "AUTH_401_1",
            message = "refresh token이 유효하지 않습니다.",
        )
        coEvery { tokenStore.clearSession(any()) } returns AuthTokenMutationResult.APPLIED
        coEvery { tokenStore.clearCourseRegistrationData() } returns Unit
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))
        val expiration = async(start = CoroutineStart.UNDISPATCHED) {
            repository.observeSessionExpiration().first { it }
        }

        assertThrowsSuspend<AuthException.SessionRevoked> { repository.reissueToken() }

        expiration.await()
        coVerify { tokenStore.clearSession(any()) }
        coVerify(exactly = 1) { practiceSessionRepository.clear() }
    }

    @Test
    fun `재발급 인증 실패 뒤 늦게 구독해도 세션 만료를 받는다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { authApi.reissue(TokenRefreshRequest("refresh-old")) } returns ApiEnvelope(
            isSuccess = false,
            code = "AUTH_401_1",
            message = "refresh token이 유효하지 않습니다.",
        )
        coEvery { tokenStore.clearSession(any()) } returns AuthTokenMutationResult.APPLIED
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))

        assertThrowsSuspend<AuthException.SessionRevoked> { repository.reissueToken() }

        assertTrue(repository.observeSessionExpiration().first())
        coVerify(exactly = 1) { practiceSessionRepository.clear() }
    }

    @Test
    fun `재발급이 HTTP 401로 실패하면 토큰을 지우고 세션을 만료한다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val httpException = mockk<HttpException>()
        every { httpException.code() } returns 401
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { authApi.reissue(TokenRefreshRequest("refresh-old")) } throws httpException
        coEvery { tokenStore.clearSession(any()) } returns AuthTokenMutationResult.APPLIED
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))

        assertThrowsSuspend<AuthException.SessionRevoked> { repository.reissueToken() }

        assertTrue(repository.observeSessionExpiration().first())
        coVerify { tokenStore.clearSession(any()) }
        coVerify(exactly = 1) { practiceSessionRepository.clear() }
    }

    @Test
    fun `재발급이 네트워크 오류로 실패하면 세션을 유지한다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { authApi.reissue(TokenRefreshRequest("refresh-old")) } throws IOException("offline")
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))

        assertThrowsSuspend<AuthException.Network> { repository.reissueToken() }

        coVerify(exactly = 0) { tokenStore.clearSession(any()) }
    }

    @Test
    fun `계정 복구가 성공하면 토큰을 저장한다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { authApi.restore("kakao", SocialLoginRequest("kakao-token")) } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = SocialLoginResponse(
                status = "SUCCESS",
                accessToken = "access-new",
                refreshToken = "refresh-new",
                isNewMember = false,
                isOnboarded = true,
                isCourseTutorialCompleted = false,
                nickname = "로디",
            ),
        )
        coEvery { tokenStore.save("access-new", "refresh-new", "kakao") } returns true
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))

        val result = repository.restoreWithKakao("kakao-token")

        assertEquals(AccountRestoreResult.Restored(isOnboarded = true, nickname = "로디"), result)
        coVerify { tokenStore.save("access-new", "refresh-new", "kakao") }
        coVerify(exactly = 1) { practiceSessionRepository.clear() }
    }

    @Test
    fun `탈퇴 유예 응답이면 토큰을 저장하지 않고 탈퇴 유예 결과를 반환한다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { authApi.restore("kakao", SocialLoginRequest("kakao-token")) } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = SocialLoginResponse(
                status = "WITHDRAWAL_PENDING",
                isCourseTutorialCompleted = false,
                isNewMember = false,
                isOnboarded = false,
                withdrawalRequestedAt = "2026-07-13T00:00:00Z",
                recoverableUntil = "2026-07-16T00:00:00Z",
            ),
        )
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))

        val result = repository.restoreWithKakao("kakao-token")

        assertTrue(result is AccountRestoreResult.WithdrawalPending)
        coVerify(exactly = 0) { tokenStore.save(any(), any(), any()) }
    }

    @Test
    fun `서버가 로그아웃을 받아들이면 토큰을 지운다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { authApi.logout(LogoutRequest("refresh-old")) } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
        )
        coEvery { tokenStore.clearSession(any()) } returns AuthTokenMutationResult.APPLIED
        coEvery { tokenStore.clearCourseRegistrationData() } returns Unit
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))

        repository.logout()

        coVerify(exactly = 1) { practiceSessionRepository.clear() }
        coVerify { tokenStore.clearSession(any()) }
    }

    @Test
    fun `카카오 로그인 네트워크 오류를 Network 예외로 매핑한다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { authApi.oauthLogin("kakao", OAuthLoginRequest("kakao-token")) } throws IOException("offline")
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))

        assertThrowsSuspend<AuthException.Network> { repository.loginWithKakao("kakao-token") }
    }

    @Test
    fun `카카오 로그인 중 취소를 그대로 전파한다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { authApi.oauthLogin("kakao", OAuthLoginRequest("kakao-token")) } throws CancellationException("cancelled")
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))

        assertThrowsSuspend<CancellationException> { repository.loginWithKakao("kakao-token") }
    }

    @Test
    fun `이전 세션의 refresh 성공은 새 로그인 세션을 덮어쓰지 않는다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = realTokenStore()
        val started = CompletableDeferred<Unit>()
        val response = CompletableDeferred<ApiEnvelope<TokenRefreshResponse>>()
        coEvery { authApi.reissue(any()) } coAnswers {
            started.complete(Unit)
            response.await()
        }
        coEvery { authApi.oauthLogin(any(), any()) } returns loginEnvelope(isOnboarded = true)
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))
        val refresh = async { repository.reissueToken() }
        started.await()

        repository.loginWithKakao("login-b")
        response.complete(staleRefreshEnvelope())
        refresh.await()

        assertEquals("access-new", tokenStore.getTokens()?.accessToken)
        assertEquals("refresh-new", tokenStore.getTokens()?.refreshToken)
    }

    @Test
    fun `이전 세션의 refresh 폐기 응답은 새 로그인 세션을 지우거나 만료시키지 않는다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = realTokenStore()
        val started = CompletableDeferred<Unit>()
        val response = CompletableDeferred<ApiEnvelope<TokenRefreshResponse>>()
        coEvery { authApi.reissue(any()) } coAnswers {
            started.complete(Unit)
            response.await()
        }
        coEvery { authApi.oauthLogin(any(), any()) } returns loginEnvelope(isOnboarded = true)
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))
        val refresh = async {
            try {
                repository.reissueToken()
            } catch (_: AuthException.SessionRevoked) {
                // The original caller may still fail; the replacement session must remain valid.
            }
        }
        started.await()

        repository.loginWithKakao("login-b")
        response.complete(ApiEnvelope(isSuccess = false, code = "AUTH_401_4", message = "revoked"))
        refresh.await()
        val current = tokenStore.getTokens()
        val expired = repository.observeSessionExpiration().first()

        assertAll(
            { assertEquals("access-new", current?.accessToken) },
            { assertFalse(expired) },
        )
        coVerify(exactly = 1) { practiceSessionRepository.clear() }
    }

    @Test
    fun `이전 세션의 refresh 성공은 로그아웃한 세션을 되살리지 않는다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = realTokenStore()
        val started = CompletableDeferred<Unit>()
        val response = CompletableDeferred<ApiEnvelope<TokenRefreshResponse>>()
        coEvery { authApi.reissue(any()) } coAnswers {
            started.complete(Unit)
            response.await()
        }
        coEvery { authApi.logout(any()) } returns ApiEnvelope(isSuccess = true, code = "COMMON_200", message = "success")
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))
        val refresh = async { repository.reissueToken() }
        started.await()

        repository.logout()
        response.complete(staleRefreshEnvelope())
        refresh.await()

        assertNull(tokenStore.getTokens())
    }

    @Test
    fun `같은 세션에서 동시에 재발급해도 토큰은 한 번만 교체한다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        var current = tokens()
        val sessionId = current.sessionId
        val response = CompletableDeferred<ApiEnvelope<TokenRefreshResponse>>()
        coEvery { tokenStore.getTokens() } answers { current }
        coEvery { authApi.reissue(any()) } coAnswers { response.await() }
        coEvery { tokenStore.rotate(any(), any(), any(), any()) } answers {
            current = current.copy(accessToken = secondArg(), refreshToken = thirdArg())
            AuthTokenMutationResult.APPLIED
        }
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))

        val first = async(start = CoroutineStart.UNDISPATCHED) { repository.reissueToken(sessionId, "access-old") }
        val second = async(start = CoroutineStart.UNDISPATCHED) { repository.reissueToken(sessionId, "access-old") }
        response.complete(tokenEnvelope(false))
        first.await()
        second.await()

        coVerify(exactly = 1) { authApi.reissue(TokenRefreshRequest("refresh-old")) }
        assertEquals("access-new", current.accessToken)
        assertEquals(sessionId, current.sessionId)
        coVerify(exactly = 0) { practiceSessionRepository.clear() }
    }

    @Test
    fun `저장소에 들어오기 전에 다른 요청이 토큰을 교체했으면 재발급을 건너뛴다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val current = tokens().copy(accessToken = "rotated", refreshToken = "rotated-refresh")
        coEvery { tokenStore.getTokens() } returns current
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))

        repository.reissueToken(current.sessionId, "access-old")

        coVerify(exactly = 0) { authApi.reissue(any()) }
        coVerify(exactly = 0) { tokenStore.rotate(any(), any(), any(), any()) }
    }

    @Test
    fun `이전 요청의 재발급은 교체된 새 세션을 재발급하지 않는다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = realTokenStore()
        val previous = tokenStore.getTokens()!!
        tokenStore.save("access-b", "refresh-b")
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))

        repository.reissueToken(previous.sessionId, previous.accessToken)

        coVerify(exactly = 0) { authApi.reissue(any()) }
        assertEquals("access-b", tokenStore.getTokens()?.accessToken)
    }

    @Test
    fun `이전 세션의 refresh 성공은 복구로 시작한 로그인을 덮어쓰지 않는다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = realTokenStore()
        val started = CompletableDeferred<Unit>()
        val response = CompletableDeferred<ApiEnvelope<TokenRefreshResponse>>()
        coEvery { authApi.reissue(any()) } coAnswers {
            started.complete(Unit)
            response.await()
        }
        coEvery { authApi.restore(any(), any()) } returns loginEnvelope(isOnboarded = true)
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))
        val refresh = async { repository.reissueToken() }
        started.await()

        repository.restoreWithKakao("restored-login")
        response.complete(staleRefreshEnvelope())
        refresh.await()

        assertEquals("access-new", tokenStore.getTokens()?.accessToken)
    }

    @Test
    fun `이전 세션의 로그아웃은 새 로그인을 지우지 않고 실패한다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = realTokenStore()
        val started = CompletableDeferred<Unit>()
        val response = CompletableDeferred<ApiEnvelope<JsonObject>>()
        coEvery { authApi.logout(any()) } coAnswers {
            started.complete(Unit)
            response.await()
        }
        coEvery { authApi.oauthLogin(any(), any()) } returns loginEnvelope(isOnboarded = true)
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))
        val logout = async {
            assertThrowsSuspend<AuthException.NotAuthenticated> { repository.logout() }
        }
        started.await()

        repository.loginWithKakao("login-b")
        response.complete(ApiEnvelope(isSuccess = true, code = "COMMON_200", message = "success"))
        logout.await()

        assertEquals("access-new", tokenStore.getTokens()?.accessToken)
        coVerify(exactly = 1) { practiceSessionRepository.clear() }
        coVerify(exactly = 0) { onboardingRepository.clear() }
        coVerify(exactly = 0) { entryRepository.clear() }
    }

    @Test
    fun `재발급 중 취소는 같은 취소를 전파하고 세션을 지우지 않는다`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = realTokenStore()
        val cancellation = CancellationException("cancelled")
        coEvery { authApi.reissue(any()) } throws cancellation
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, coordinator(tokenStore, PracticeRecordPresenceCache()))

        val thrown = assertThrowsSuspend<CancellationException> { repository.reissueToken() }

        assertSame(cancellation, thrown)
        assertEquals("access-old", tokenStore.getTokens()?.accessToken)
        assertFalse(repository.observeSessionExpiration().first())
        coVerify(exactly = 0) { practiceSessionRepository.clear() }
    }

    private fun realTokenStore(): AuthTokenStore {
        val context = mockk<Context>()
        val dataStore = mockk<AuthTokenDataStore>()
        every { context.deleteSharedPreferences(any()) } returns true
        coEvery { dataStore.read() } returns tokens()
        coEvery { dataStore.save(any()) } returns true
        coEvery { dataStore.clear(any()) } returns true
        return spyk(AuthTokenStore(context, dataStore)).also {
            coEvery { it.clearCourseRegistrationData() } returns Unit
        }
    }

    private fun staleRefreshEnvelope() = ApiEnvelope(
        isSuccess = true,
        code = "COMMON_200",
        message = "success",
        data = TokenRefreshResponse("access-stale", "refresh-stale", isOnboarded = true, isCourseTutorialCompleted = false),
    )

    private fun tokens(provider: String = "kakao") = AuthTokens(
        accessToken = "access-old",
        refreshToken = "refresh-old",
        provider = provider,
    )

    private fun tokenEnvelope(
        isOnboarded: Boolean,
        isCourseTutorialCompleted: Boolean = false,
    ) = ApiEnvelope(
        isSuccess = true,
        code = "COMMON_200",
        message = "성공",
        data = TokenRefreshResponse(
            accessToken = "access-new",
            refreshToken = "refresh-new",
            isOnboarded = isOnboarded,
            isCourseTutorialCompleted = isCourseTutorialCompleted,
        ),
    )

    private fun loginEnvelope(isOnboarded: Boolean) = ApiEnvelope(
        isSuccess = true,
        code = "COMMON_200",
        message = "성공",
        data = SocialLoginResponse(
            status = "SUCCESS",
            accessToken = "access-new",
            refreshToken = "refresh-new",
            isNewMember = !isOnboarded,
            isOnboarded = isOnboarded,
            isCourseTutorialCompleted = false,
            nickname = "서버 닉네임",
        ),
    )
}
