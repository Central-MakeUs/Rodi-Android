package com.dororong.rodi.core.data.source.remote.network

import android.content.Context
import com.dororong.rodi.core.data.source.local.datastore.AuthTokenDataStore
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.local.security.AuthTokens
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlin.coroutines.cancellation.CancellationException
import io.mockk.mockk
import okhttp3.Protocol
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class TokenAuthenticatorTest {
    private val tokenStore = mockk<AuthTokenStore>()
    private val authRepository = mockk<AuthRepository>()
    private val authenticator = TokenAuthenticator(tokenStore) { authRepository }

    @Test
    fun `retries the request with the refreshed token`() {
        coEvery { tokenStore.getTokens() } returnsMany listOf(
            AuthTokens("old", "refresh", "kakao", sessionId = SESSION_ID),
            AuthTokens("new", "refresh-new", "kakao", sessionId = SESSION_ID),
        )
        coEvery { authRepository.reissueToken(any(), any()) } returns Unit

        val retry = authenticator.authenticate(null, unauthorized("Bearer old"))

        assertEquals("Bearer new", retry?.header("Authorization"))
        coVerify(exactly = 1) { authRepository.reissueToken(SESSION_ID, "old") }
    }

    @Test
    fun `retries with the token another request already refreshed`() {
        coEvery { tokenStore.getTokens() } returns AuthTokens("new", "refresh", "kakao", sessionId = SESSION_ID)

        val retry = authenticator.authenticate(null, unauthorized("Bearer old"))

        assertEquals("Bearer new", retry?.header("Authorization"))
        coVerify(exactly = 0) { authRepository.reissueToken(any(), any()) }
    }

    @Test
    fun `lets cancellation propagate`() {
        coEvery { tokenStore.getTokens() } returns AuthTokens("old", "refresh", "kakao", sessionId = SESSION_ID)
        val cancellation = CancellationException("cancelled")
        coEvery { authRepository.reissueToken(any(), any()) } throws cancellation

        val thrown = assertThrows(CancellationException::class.java) {
            authenticator.authenticate(null, unauthorized("Bearer old"))
        }
        assertSame(cancellation, thrown)
    }

    @Test
    fun `gives up when the refresh fails`() {
        coEvery { tokenStore.getTokens() } returns AuthTokens("old", "refresh", "kakao", sessionId = SESSION_ID)
        coEvery { authRepository.reissueToken(any(), any()) } throws AuthException.SessionRevoked("세션이 만료되었습니다.")

        assertNull(authenticator.authenticate(null, unauthorized("Bearer old")))
    }

    @Test
    fun `gives up when the token did not change`() {
        coEvery { authRepository.reissueToken(any(), any()) } returns Unit
        coEvery { tokenStore.getTokens() } returns AuthTokens("old", "refresh", "kakao", sessionId = SESSION_ID)

        assertNull(authenticator.authenticate(null, unauthorized("Bearer old")))
    }

    @Test
    fun `does not retry more than once for the same request`() {
        coEvery { authRepository.reissueToken(any(), any()) } returns Unit
        coEvery { tokenStore.getTokens() } returns AuthTokens("other", "refresh", "kakao", sessionId = SESSION_ID)
        val second = unauthorized("Bearer new", priorResponse = unauthorized("Bearer old"))

        assertNull(authenticator.authenticate(null, second))
    }

    @Test
    fun `old session request is not retried with a new login credential`() = runTest {
        val context = mockk<Context>()
        val dataStore = mockk<AuthTokenDataStore>()
        every { context.deleteSharedPreferences(any()) } returns true
        coEvery { dataStore.read() } returns AuthTokens("session-a", "refresh-a", "kakao")
        coEvery { dataStore.save(any()) } returns true
        val store = AuthTokenStore(context, dataStore)
        val request = Request.Builder().url("https://api.stillstar.store/api/v1/members/me").build()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            Response.Builder()
                .request(firstArg())
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .body("".toResponseBody())
                .build()
        }
        val response = AuthHeaderInterceptor(store).intercept(chain)
        store.save("session-b", "refresh-b", "kakao")

        val retry = TokenAuthenticator(store) { authRepository }.authenticate(null, response)

        assertNull(retry)
        coVerify(exactly = 0) { authRepository.reissueToken(any(), any()) }
    }

    @Test
    fun `does not retry if a new login completes during refresh`() {
        coEvery { tokenStore.getTokens() } returnsMany listOf(
            AuthTokens("old", "refresh", "kakao", sessionId = SESSION_ID),
            AuthTokens("session-b", "refresh-b", "kakao", sessionId = "session-b"),
        )
        coEvery { authRepository.reissueToken(SESSION_ID, "old") } returns Unit

        val retry = authenticator.authenticate(null, unauthorized("Bearer old"))

        assertNull(retry)
    }

    @Test
    fun `does not retry an unowned request after login`() {
        val response = unauthorized("Bearer old").let {
            it.newBuilder().request(it.request.newBuilder().tag(AuthRequestSession::class.java, null).build()).build()
        }

        assertNull(authenticator.authenticate(null, response))
        coVerify(exactly = 0) { authRepository.reissueToken(any(), any()) }
    }

    private companion object {
        const val SESSION_ID = "session-a"
    }

    private fun unauthorized(authorization: String, priorResponse: Response? = null): Response {
        val request = Request.Builder()
            .url("https://api.stillstar.store/api/v1/members/me")
            .header("Authorization", authorization)
            .tag(AuthRequestSession::class.java, AuthRequestSession(SESSION_ID))
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
