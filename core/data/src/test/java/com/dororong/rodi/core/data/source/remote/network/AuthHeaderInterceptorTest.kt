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
    fun `adds the access token as a bearer header`() {
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        val chain = RecordingChain()

        AuthHeaderInterceptor(tokenStore).intercept(chain)

        assertEquals("Bearer access", chain.sent?.header("Authorization"))
    }

    @Test
    fun `sends without the header when there is no session`() {
        coEvery { tokenStore.getTokens() } returns null
        val chain = RecordingChain()

        AuthHeaderInterceptor(tokenStore).intercept(chain)

        assertNull(chain.sent?.header("Authorization"))
    }

    @Test
    fun `keeps a header the caller already set`() {
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        val chain = RecordingChain(
            request = Request.Builder().url(URL).header("Authorization", "KakaoAK key").build(),
        )

        AuthHeaderInterceptor(tokenStore).intercept(chain)

        assertEquals("KakaoAK key", chain.sent?.header("Authorization"))
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
