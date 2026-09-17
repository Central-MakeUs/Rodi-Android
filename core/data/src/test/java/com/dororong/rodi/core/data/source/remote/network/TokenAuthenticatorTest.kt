package com.dororong.rodi.core.data.source.remote.network

import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.local.security.AuthTokens
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TokenAuthenticatorTest {
    private val tokenStore = mockk<AuthTokenStore>()
    private val authRepository = mockk<AuthRepository>()
    private val authenticator = TokenAuthenticator(tokenStore) { authRepository }

    @Test
    fun `retries the request with the refreshed token`() {
        coEvery { authRepository.reissueToken() } returns Unit
        coEvery { tokenStore.getTokens() } returns AuthTokens("new", "refresh", "kakao")

        val retry = authenticator.authenticate(null, unauthorized("Bearer old"))

        assertEquals("Bearer new", retry?.header("Authorization"))
        coVerify(exactly = 1) { authRepository.reissueToken() }
    }

    @Test
    fun `gives up when the refresh fails`() {
        coEvery { authRepository.reissueToken() } throws AuthException.SessionRevoked("세션이 만료되었습니다.")

        assertNull(authenticator.authenticate(null, unauthorized("Bearer old")))
    }

    @Test
    fun `gives up when the token did not change`() {
        coEvery { authRepository.reissueToken() } returns Unit
        coEvery { tokenStore.getTokens() } returns AuthTokens("old", "refresh", "kakao")

        assertNull(authenticator.authenticate(null, unauthorized("Bearer old")))
    }

    @Test
    fun `does not retry more than once for the same request`() {
        coEvery { authRepository.reissueToken() } returns Unit
        coEvery { tokenStore.getTokens() } returns AuthTokens("new", "refresh", "kakao")
        val second = unauthorized("Bearer new", priorResponse = unauthorized("Bearer old"))

        assertNull(authenticator.authenticate(null, second))
    }

    private fun unauthorized(authorization: String, priorResponse: Response? = null): Response {
        val request = Request.Builder()
            .url("https://api.stillstar.store/api/v1/members/me")
            .header("Authorization", authorization)
            .build()
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .apply { priorResponse?.let { priorResponse(it) } }
            .build()
    }
}
