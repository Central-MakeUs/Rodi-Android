package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.local.security.AuthTokens
import com.dororong.rodi.core.data.source.remote.api.AuthApi
import com.dororong.rodi.core.data.source.remote.model.auth.AuthTokenResponse
import com.dororong.rodi.core.data.source.remote.model.auth.LogoutRequest
import com.dororong.rodi.core.data.source.remote.model.auth.OAuthLoginRequest
import com.dororong.rodi.core.data.source.remote.model.auth.SocialLoginRequest
import com.dororong.rodi.core.data.source.remote.model.auth.SocialLoginResponse
import com.dororong.rodi.core.data.source.remote.model.auth.TokenRefreshRequest
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.domain.model.auth.AccountRestoreResult
import com.dororong.rodi.core.domain.model.auth.AuthException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class AuthRepositoryImplTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `getSession maps atomic token snapshot`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens(provider = "kakao")
        val repository = AuthRepositoryImpl(authApi, tokenStore, json)

        val session = repository.getSession()

        assertTrue(session.isLoggedIn)
        assertTrue(session.hasRecentKakaoLogin)
    }

    @Test
    fun `loginWithKakao saves server tokens`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { authApi.oauthLogin("kakao", OAuthLoginRequest("kakao-token")) } returns tokenEnvelope(true)
        coEvery { tokenStore.save("access-new", "refresh-new") } returns true
        val repository = AuthRepositoryImpl(authApi, tokenStore, json)

        val isNewMember = repository.loginWithKakao("kakao-token")

        assertTrue(isNewMember)
        coVerify { tokenStore.save("access-new", "refresh-new") }
    }

    @Test
    fun `reissueToken rotates current refresh token`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens()
        coEvery { authApi.reissue(TokenRefreshRequest("refresh-old")) } returns tokenEnvelope(false)
        coEvery { tokenStore.save("access-new", "refresh-new") } returns true
        val repository = AuthRepositoryImpl(authApi, tokenStore, json)

        repository.reissueToken()

        coVerify(exactly = 1) { authApi.reissue(TokenRefreshRequest("refresh-old")) }
        coVerify { tokenStore.save("access-new", "refresh-new") }
    }

    @Test
    fun `reissueToken does not call api without a session`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns null
        val repository = AuthRepositoryImpl(authApi, tokenStore, json)

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
        val repository = AuthRepositoryImpl(authApi, tokenStore, json)

        assertThrowsSuspend<AuthException.SessionRevoked> { repository.reissueToken() }

        coVerify { tokenStore.clear() }
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
                nickname = "로디",
            ),
        )
        coEvery { tokenStore.save("access-new", "refresh-new") } returns true
        val repository = AuthRepositoryImpl(authApi, tokenStore, json)

        val result = repository.restoreWithKakao("kakao-token")

        assertEquals(AccountRestoreResult.Restored(isNewMember = false, nickname = "로디"), result)
        coVerify { tokenStore.save("access-new", "refresh-new") }
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
                isNewMember = false,
                withdrawalRequestedAt = "2026-07-13T00:00:00Z",
                recoverableUntil = "2026-07-16T00:00:00Z",
            ),
        )
        val repository = AuthRepositoryImpl(authApi, tokenStore, json)

        val result = repository.restoreWithKakao("kakao-token")

        assertTrue(result is AccountRestoreResult.WithdrawalPending)
        coVerify(exactly = 0) { tokenStore.save(any(), any()) }
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
        val repository = AuthRepositoryImpl(authApi, tokenStore, json)

        repository.logout()

        coVerify { tokenStore.clear() }
    }

    @Test
    fun `loginWithKakao maps network failure`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { authApi.oauthLogin("kakao", OAuthLoginRequest("kakao-token")) } throws IOException("offline")
        val repository = AuthRepositoryImpl(authApi, tokenStore, json)

        assertThrowsSuspend<AuthException.Network> { repository.loginWithKakao("kakao-token") }
    }

    @Test
    fun `loginWithKakao propagates cancellation`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { authApi.oauthLogin("kakao", OAuthLoginRequest("kakao-token")) } throws CancellationException("cancelled")
        val repository = AuthRepositoryImpl(authApi, tokenStore, json)

        assertThrowsSuspend<CancellationException> { repository.loginWithKakao("kakao-token") }
    }

    private fun tokens(provider: String = "kakao") = AuthTokens(
        accessToken = "access-old",
        refreshToken = "refresh-old",
        provider = provider,
    )

    private fun tokenEnvelope(isNewMember: Boolean) = ApiEnvelope(
        isSuccess = true,
        code = "COMMON_200",
        message = "성공",
        data = AuthTokenResponse(
            accessToken = "access-new",
            refreshToken = "refresh-new",
            isNewMember = isNewMember,
        ),
    )

    private suspend inline fun <reified T : Throwable> assertThrowsSuspend(
        crossinline block: suspend () -> Unit,
    ): T {
        try {
            block()
        } catch (exception: Throwable) {
            if (exception is T) return exception
            throw exception
        }
        throw AssertionError("Expected ${T::class.simpleName} to be thrown")
    }
}
