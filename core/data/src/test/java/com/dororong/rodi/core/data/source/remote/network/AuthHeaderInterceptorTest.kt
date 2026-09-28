package com.dororong.rodi.core.data.source.remote.network

import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.local.security.AuthTokens
import io.mockk.coEvery
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AuthHeaderInterceptorTest {
    private val tokenStore = mockk<AuthTokenStore>()

    @Test
    fun `access 토큰을 Bearer 헤더로 붙인다`() {
        val tokens = AuthTokens("access", "refresh", "kakao")
        coEvery { tokenStore.getTokens() } returns tokens
        val chain = RecordingChain()

        AuthHeaderInterceptor(tokenStore).intercept(chain)

        assertEquals("Bearer access", chain.sent?.header("Authorization"))
        assertEquals(tokens.sessionId, chain.sent?.tag(AuthRequestSession::class.java)?.id)
    }

    @Test
    fun `세션이 없으면 인증 헤더 없이 보낸다`() {
        coEvery { tokenStore.getTokens() } returns null
        val chain = RecordingChain()

        AuthHeaderInterceptor(tokenStore).intercept(chain)

        assertNull(chain.sent?.header("Authorization"))
        assertNull(chain.sent?.tag(AuthRequestSession::class.java))
    }

    @Test
    fun `호출자가 이미 넣은 인증 헤더는 유지한다`() {
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        val chain = RecordingChain(
            request = Request.Builder().url(URL).header("Authorization", "KakaoAK key").build(),
        )

        AuthHeaderInterceptor(tokenStore).intercept(chain)

        assertEquals("KakaoAK key", chain.sent?.header("Authorization"))
        assertNull(chain.sent?.tag(AuthRequestSession::class.java))
    }

    private class RecordingChain(
        private val request: Request = Request.Builder().url(URL).build(),
    ) : Interceptor.Chain by mockk(relaxed = true) {
        var sent: Request? = null

        override fun request(): Request = request

        override fun proceed(request: Request): Response {
            sent = request
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("".toResponseBody())
                .build()
        }
    }

    private companion object {
        const val URL = "https://api.stillstar.store/api/v1/members/me"
    }
}
