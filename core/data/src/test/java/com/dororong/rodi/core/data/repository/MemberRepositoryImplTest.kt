package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.local.security.AuthTokens
import com.dororong.rodi.core.data.source.remote.api.MemberApi
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.domain.model.auth.AuthException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MemberRepositoryImplTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `withdraw sends bearer access token and clears session after success`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.withdraw("Bearer access") } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
        )
        coEvery { tokenStore.clear() } returns true
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json)

        repository.withdraw()

        coVerify { memberApi.withdraw("Bearer access") }
        coVerify { tokenStore.clear() }
    }

    @Test
    fun `withdraw does not call api without a session`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns null
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json)

        val exception = assertThrowsSuspend<AuthException.NotAuthenticated> { repository.withdraw() }

        assertEquals("로그인 세션이 없습니다.", exception.message)
        coVerify(exactly = 0) { memberApi.withdraw(any()) }
    }

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
