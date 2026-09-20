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

    @Test
    fun `getSession maps atomic token snapshot`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens(provider = "kakao")
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

        val session = repository.getSession()

        assertTrue(session.isLoggedIn)
        assertTrue(session.hasRecentKakaoLogin)
    }

    @Test
    fun `getSession keeps recent Kakao login after tokens are cleared`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns null
        coEvery { tokenStore.getRecentProvider() } returns "kakao"
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

        val session = repository.getSession()

        assertFalse(session.isLoggedIn)
        assertTrue(session.hasRecentKakaoLogin)
    }

    @Test
    fun `loginWithKakao saves server tokens`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { authApi.oauthLogin("kakao", OAuthLoginRequest("kakao-token")) } returns loginEnvelope(true)
        coEvery { tokenStore.save("access-new", "refresh-new", "kakao") } returns true
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

        val result = repository.loginWithKakao("kakao-token")

        assertEquals(LoginResult.Success(true, "서버 닉네임"), result)
        coVerify(exactly = 1) { practiceSessionRepository.clear() }
        coVerify { tokenStore.save("access-new", "refresh-new", "kakao") }
    }

    @Test
    fun `login succeeds when stale practice session cleanup fails`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { authApi.oauthLogin("kakao", OAuthLoginRequest("kakao-token")) } returns loginEnvelope(false)
        coEvery { tokenStore.save("access-new", "refresh-new", "kakao") } returns true
        coEvery { practiceSessionRepository.clear() } throws IOException("local storage unavailable")
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

        assertTrue(repository.loginWithKakao("kakao-token") is LoginResult.Success)
        coVerify(exactly = 1) { tokenStore.save("access-new", "refresh-new", "kakao") }
        coVerify(exactly = 3) { practiceSessionRepository.clear() }
    }

    @Test
    fun `reissueToken rotates current refresh token`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { authApi.reissue(TokenRefreshRequest("refresh-old")) } returns tokenEnvelope(false)
        coEvery { tokenStore.rotate(any(), "access-new", "refresh-new", false) } returns AuthTokenMutationResult.APPLIED
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

        repository.reissueToken()

        coVerify(exactly = 1) { authApi.reissue(TokenRefreshRequest("refresh-old")) }
        coVerify { tokenStore.rotate(any(), "access-new", "refresh-new", false) }
    }

    @Test
    fun `reissueToken persists tutorial flag returned by server`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access-old", "refresh-old", "kakao")
        coEvery { authApi.reissue(TokenRefreshRequest("refresh-old")) } returns tokenEnvelope(
            isOnboarded = true,
            isCourseTutorialCompleted = true,
        )
        coEvery { tokenStore.rotate(any(), "access-new", "refresh-new", true) } returns AuthTokenMutationResult.APPLIED
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

        repository.reissueToken()

        coVerify(exactly = 1) { tokenStore.rotate(any(), "access-new", "refresh-new", true) }
    }

    @Test
    fun `reissueToken keeps practice presence cache for the same session`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val cache = PracticeRecordPresenceCache().also { it.set(true) }
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { authApi.reissue(TokenRefreshRequest("refresh-old")) } returns tokenEnvelope(false)
        coEvery { tokenStore.rotate(any(), "access-new", "refresh-new", false) } returns AuthTokenMutationResult.APPLIED
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, cache, practiceSessionRepository)

        repository.reissueToken()

        assertEquals(true, cache.get())
        coVerify(exactly = 0) { practiceSessionRepository.clear() }
    }

    @Test
    fun `reissueToken does not call api without a session`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns null
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

        assertThrowsSuspend<AuthException.NotAuthenticated> { repository.reissueToken() }

        coVerify(exactly = 0) { authApi.reissue(any()) }
    }

    @Test
    fun `reissueToken clears local tokens when refresh token reuse is detected`() = runTest {
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
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

        assertThrowsSuspend<AuthException.SessionRevoked> { repository.reissueToken() }

        coVerify { tokenStore.clearSession(any()) }
        coVerify(exactly = 1) { practiceSessionRepository.clear() }
    }

    @Test
    fun `reissueToken emits session expiration after an unauthorized refresh response`() = runTest {
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
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)
        val expiration = async(start = CoroutineStart.UNDISPATCHED) {
            repository.observeSessionExpiration().first { it }
        }

        assertThrowsSuspend<AuthException.SessionRevoked> { repository.reissueToken() }

        expiration.await()
        coVerify { tokenStore.clearSession(any()) }
        coVerify(exactly = 1) { practiceSessionRepository.clear() }
    }

    @Test
    fun `reissueToken preserves session expiration for a late subscriber`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { authApi.reissue(TokenRefreshRequest("refresh-old")) } returns ApiEnvelope(
            isSuccess = false,
            code = "AUTH_401_1",
            message = "refresh token이 유효하지 않습니다.",
        )
        coEvery { tokenStore.clearSession(any()) } returns AuthTokenMutationResult.APPLIED
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

        assertThrowsSuspend<AuthException.SessionRevoked> { repository.reissueToken() }

        assertTrue(repository.observeSessionExpiration().first())
        coVerify(exactly = 1) { practiceSessionRepository.clear() }
    }

    @Test
    fun `reissueToken clears tokens and expires the session for an HTTP 401 response`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val httpException = mockk<HttpException>()
        every { httpException.code() } returns 401
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { authApi.reissue(TokenRefreshRequest("refresh-old")) } throws httpException
        coEvery { tokenStore.clearSession(any()) } returns AuthTokenMutationResult.APPLIED
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

        assertThrowsSuspend<AuthException.SessionRevoked> { repository.reissueToken() }

        assertTrue(repository.observeSessionExpiration().first())
        coVerify { tokenStore.clearSession(any()) }
        coVerify(exactly = 1) { practiceSessionRepository.clear() }
    }

    @Test
    fun `reissueToken keeps the session for a network failure`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { authApi.reissue(TokenRefreshRequest("refresh-old")) } throws IOException("offline")
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

        assertThrowsSuspend<AuthException.Network> { repository.reissueToken() }

        coVerify(exactly = 0) { tokenStore.clearSession(any()) }
    }

    @Test
    fun `restoreWithKakao saves tokens for restored account`() = runTest {
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
                isCourseTutorialCompleted = false,
                nickname = "로디",
            ),
        )
        coEvery { tokenStore.save("access-new", "refresh-new", "kakao") } returns true
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

        val result = repository.restoreWithKakao("kakao-token")

        assertEquals(AccountRestoreResult.Restored(isNewMember = false, nickname = "로디"), result)
        coVerify { tokenStore.save("access-new", "refresh-new", "kakao") }
        coVerify(exactly = 1) { practiceSessionRepository.clear() }
    }

    @Test
    fun `restoreWithKakao returns withdrawal pending without saving tokens`() = runTest {
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
                withdrawalRequestedAt = "2026-07-13T00:00:00Z",
                recoverableUntil = "2026-07-16T00:00:00Z",
            ),
        )
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

        val result = repository.restoreWithKakao("kakao-token")

        assertTrue(result is AccountRestoreResult.WithdrawalPending)
        coVerify(exactly = 0) { tokenStore.save(any(), any(), any()) }
    }

    @Test
    fun `logout clears tokens after server accepts refresh token`() = runTest {
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
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

        repository.logout()

        coVerify(exactly = 1) { practiceSessionRepository.clear() }
        coVerify { tokenStore.clearSession(any()) }
    }

    @Test
    fun `loginWithKakao maps network failure`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { authApi.oauthLogin("kakao", OAuthLoginRequest("kakao-token")) } throws IOException("offline")
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

        assertThrowsSuspend<AuthException.Network> { repository.loginWithKakao("kakao-token") }
    }

    @Test
    fun `loginWithKakao propagates cancellation`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { authApi.oauthLogin("kakao", OAuthLoginRequest("kakao-token")) } throws CancellationException("cancelled")
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

        assertThrowsSuspend<CancellationException> { repository.loginWithKakao("kakao-token") }
    }

    @Test
    fun `stale refresh success does not overwrite a new login`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = realTokenStore()
        val started = CompletableDeferred<Unit>()
        val response = CompletableDeferred<ApiEnvelope<TokenRefreshResponse>>()
        coEvery { authApi.reissue(any()) } coAnswers {
            started.complete(Unit)
            response.await()
        }
        coEvery { authApi.oauthLogin(any(), any()) } returns loginEnvelope(false)
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)
        val refresh = async { repository.reissueToken() }
        started.await()

        repository.loginWithKakao("login-b")
        response.complete(staleRefreshEnvelope())
        refresh.await()

        assertEquals("access-new", tokenStore.getTokens()?.accessToken)
        assertEquals("refresh-new", tokenStore.getTokens()?.refreshToken)
    }

    @Test
    fun `stale revoked refresh neither clears nor expires a new login`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = realTokenStore()
        val started = CompletableDeferred<Unit>()
        val response = CompletableDeferred<ApiEnvelope<TokenRefreshResponse>>()
        coEvery { authApi.reissue(any()) } coAnswers {
            started.complete(Unit)
            response.await()
        }
        coEvery { authApi.oauthLogin(any(), any()) } returns loginEnvelope(false)
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)
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
    fun `stale refresh success does not revive a logged out session`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = realTokenStore()
        val started = CompletableDeferred<Unit>()
        val response = CompletableDeferred<ApiEnvelope<TokenRefreshResponse>>()
        coEvery { authApi.reissue(any()) } coAnswers {
            started.complete(Unit)
            response.await()
        }
        coEvery { authApi.logout(any()) } returns ApiEnvelope(isSuccess = true, code = "COMMON_200", message = "success")
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)
        val refresh = async { repository.reissueToken() }
        started.await()

        repository.logout()
        response.complete(staleRefreshEnvelope())
        refresh.await()

        assertNull(tokenStore.getTokens())
    }

    @Test
    fun `concurrent refreshes rotate tokens only once within the same session`() = runTest {
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
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

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
    fun `refresh is skipped if another request rotated before repository entry`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val current = tokens().copy(accessToken = "rotated", refreshToken = "rotated-refresh")
        coEvery { tokenStore.getTokens() } returns current
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

        repository.reissueToken(current.sessionId, "access-old")

        coVerify(exactly = 0) { authApi.reissue(any()) }
        coVerify(exactly = 0) { tokenStore.rotate(any(), any(), any(), any()) }
    }

    @Test
    fun `refresh for an old request does not refresh a replacement session`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = realTokenStore()
        val previous = tokenStore.getTokens()!!
        tokenStore.save("access-b", "refresh-b")
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

        repository.reissueToken(previous.sessionId, previous.accessToken)

        coVerify(exactly = 0) { authApi.reissue(any()) }
        assertEquals("access-b", tokenStore.getTokens()?.accessToken)
    }

    @Test
    fun `stale refresh success does not overwrite a restored login`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = realTokenStore()
        val started = CompletableDeferred<Unit>()
        val response = CompletableDeferred<ApiEnvelope<TokenRefreshResponse>>()
        coEvery { authApi.reissue(any()) } coAnswers {
            started.complete(Unit)
            response.await()
        }
        coEvery { authApi.restore(any(), any()) } returns loginEnvelope(false)
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)
        val refresh = async { repository.reissueToken() }
        started.await()

        repository.restoreWithKakao("restored-login")
        response.complete(staleRefreshEnvelope())
        refresh.await()

        assertEquals("access-new", tokenStore.getTokens()?.accessToken)
    }

    @Test
    fun `stale logout fails without clearing a new login`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = realTokenStore()
        val started = CompletableDeferred<Unit>()
        val response = CompletableDeferred<ApiEnvelope<JsonObject>>()
        coEvery { authApi.logout(any()) } coAnswers {
            started.complete(Unit)
            response.await()
        }
        coEvery { authApi.oauthLogin(any(), any()) } returns loginEnvelope(false)
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)
        val logout = async {
            assertThrowsSuspend<AuthException.NotAuthenticated> { repository.logout() }
        }
        started.await()

        repository.loginWithKakao("login-b")
        response.complete(ApiEnvelope(isSuccess = true, code = "COMMON_200", message = "success"))
        logout.await()

        assertEquals("access-new", tokenStore.getTokens()?.accessToken)
        coVerify(exactly = 1) { practiceSessionRepository.clear() }
    }

    @Test
    fun `refresh propagates the same cancellation without clearing the session`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = realTokenStore()
        val cancellation = CancellationException("cancelled")
        coEvery { authApi.reissue(any()) } throws cancellation
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

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

    private fun loginEnvelope(isNewMember: Boolean) = ApiEnvelope(
        isSuccess = true,
        code = "COMMON_200",
        message = "성공",
        data = SocialLoginResponse(
            status = "SUCCESS",
            accessToken = "access-new",
            refreshToken = "refresh-new",
            isNewMember = isNewMember,
            isCourseTutorialCompleted = false,
            nickname = "서버 닉네임",
        ),
    )
}
