package com.dororong.rodi.core.data

import com.dororong.rodi.core.data.auth.AuthApi
import com.dororong.rodi.core.data.auth.AuthTokenResponse
import com.dororong.rodi.core.data.auth.AuthTokenStore
import com.dororong.rodi.core.data.auth.OAuthLoginRequest
import com.dororong.rodi.core.data.network.ApiEnvelope
import com.dororong.rodi.core.domain.AuthException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException

class AuthRepositoryImplTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `loginWithKakao saves tokens and returns isNewMember when api succeeds`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>(relaxed = true)
        coEvery { authApi.oauthLogin("kakao", OAuthLoginRequest("access-token")) } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = AuthTokenResponse(
                accessToken = "server-access-token",
                refreshToken = "server-refresh-token",
                isNewMember = true,
            ),
        )
        val repository = AuthRepositoryImpl(authApi, tokenStore, json)

        val result = repository.loginWithKakao("access-token")

        assertEquals(true, result)
        verify(exactly = 1) { tokenStore.save("server-access-token", "server-refresh-token") }
        coVerify(exactly = 1) { authApi.oauthLogin("kakao", OAuthLoginRequest("access-token")) }
    }

    @Test
    fun `loginWithKakao uses envelope message when response data is null`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { authApi.oauthLogin("kakao", OAuthLoginRequest("access-token")) } returns ApiEnvelope(
            isSuccess = false,
            code = "AUTH_401_5",
            message = "카카오 토큰이 유효하지 않습니다.",
            data = null,
        )
        val repository = AuthRepositoryImpl(authApi, tokenStore, json)

        val exception = assertThrowsSuspend<AuthException.Unknown> {
            repository.loginWithKakao("access-token")
        }

        assertEquals("카카오 토큰이 유효하지 않습니다.", exception.message)
    }

    @Test
    fun `loginWithKakao maps api failure to auth exception`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { authApi.oauthLogin("kakao", OAuthLoginRequest("access-token")) } throws IOException("offline")
        val repository = AuthRepositoryImpl(authApi, tokenStore, json)

        assertThrowsSuspend<AuthException.Network> {
            repository.loginWithKakao("access-token")
        }
    }

    @Test
    fun `loginWithKakao rethrows CancellationException`() = runTest {
        val authApi = mockk<AuthApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { authApi.oauthLogin("kakao", OAuthLoginRequest("access-token")) } throws CancellationException("cancelled")
        val repository = AuthRepositoryImpl(authApi, tokenStore, json)

        assertThrowsSuspend<CancellationException> {
            repository.loginWithKakao("access-token")
        }
    }

    private suspend inline fun <reified T : Throwable> assertThrowsSuspend(
        crossinline block: suspend () -> Unit,
    ): T {
        try {
            block()
        } catch (e: Throwable) {
            if (e is T) return e
            throw e
        }
        throw AssertionError("Expected ${T::class.simpleName} to be thrown")
    }
}
