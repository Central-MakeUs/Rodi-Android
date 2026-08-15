package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.cache.PracticeRecordPresenceCache
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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
    }

    @Test
    fun `reissueToken rotates current refresh token`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { authApi.reissue(TokenRefreshRequest("refresh-old")) } returns tokenEnvelope(false)
        coEvery { tokenStore.save("access-new", "refresh-new", "kakao") } returns true
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

        repository.reissueToken()

        coVerify(exactly = 1) { authApi.reissue(TokenRefreshRequest("refresh-old")) }
        coVerify { tokenStore.save("access-new", "refresh-new", "kakao") }
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
        coEvery { tokenStore.save("access-new", "refresh-new", "kakao", true) } returns true
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

        repository.reissueToken()

        coVerify(exactly = 1) { tokenStore.save("access-new", "refresh-new", "kakao", true) }
    }

    @Test
    fun `reissueToken keeps practice presence cache for the same session`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val cache = PracticeRecordPresenceCache().also { it.set(true) }
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { authApi.reissue(TokenRefreshRequest("refresh-old")) } returns tokenEnvelope(false)
        coEvery { tokenStore.save("access-new", "refresh-new", "kakao") } returns true
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
        coEvery { tokenStore.clear() } returns true
        coEvery { tokenStore.clearCourseRegistrationData() } returns Unit
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

        assertThrowsSuspend<AuthException.SessionRevoked> { repository.reissueToken() }

        coVerify { tokenStore.clear() }
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
        coEvery { tokenStore.clear() } returns true
        coEvery { tokenStore.clearCourseRegistrationData() } returns Unit
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)
        val expiration = async(start = CoroutineStart.UNDISPATCHED) {
            repository.observeSessionExpiration().first { it }
        }

        assertThrowsSuspend<AuthException.SessionRevoked> { repository.reissueToken() }

        expiration.await()
        coVerify { tokenStore.clear() }
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
        coEvery { tokenStore.clear() } returns true
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
        coEvery { tokenStore.clear() } returns true
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

        assertThrowsSuspend<AuthException.SessionRevoked> { repository.reissueToken() }

        assertTrue(repository.observeSessionExpiration().first())
        coVerify { tokenStore.clear() }
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

        coVerify(exactly = 0) { tokenStore.clear() }
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
        coEvery { tokenStore.clear() } returns true
        coEvery { tokenStore.clearCourseRegistrationData() } returns Unit
        val repository = AuthRepositoryImpl(authApi, tokenStore, json, PracticeRecordPresenceCache(), practiceSessionRepository)

        repository.logout()

        coVerify(exactly = 1) { practiceSessionRepository.clear() }
        coVerify { tokenStore.clear() }
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
