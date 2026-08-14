package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.local.security.AuthTokens
import com.dororong.rodi.core.data.source.remote.api.PracticeApi
import com.dororong.rodi.core.data.source.remote.model.practice.PracticeSkipReasonRequest
import com.dororong.rodi.core.data.source.remote.model.practice.PracticeVisitRequest
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.data.test.assertThrowsSuspend
import com.dororong.rodi.core.domain.model.practice.PracticeException
import com.dororong.rodi.core.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PracticeRepositoryImplTest {
    @Test
    fun `visit request omits certified distance in phase a`() = runTest {
        val api = mockk<PracticeApi>()
        coEvery { api.recordVisit("Bearer access", 7, PracticeVisitRequest(null)) } returns
            ApiEnvelope(true, "COMMON_200", "성공", practiceVisitResponse())
        val repository = repository(api)

        val result = repository.recordVisit(7)

        assertEquals(1, result.visitCount)
    }

    @Test
    fun `skip reason conflict maps to already submitted`() = runTest {
        val api = mockk<PracticeApi>()
        coEvery { api.submitSkipReason("Bearer access", 7, PracticeSkipReasonRequest("OTHER", "이유")) } returns
            failureEnvelope("PRACTICE_409")
        val repository = repository(api)

        assertThrowsSuspend<PracticeException.SkipReasonAlreadySubmitted> {
            repository.submitSkipReason(7, "OTHER", "이유")
        }
    }

    private fun repository(api: PracticeApi): PracticeRepositoryImpl = PracticeRepositoryImpl(
        api = api,
        tokenStore = mockk<AuthTokenStore>().also {
            coEvery { it.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        },
        authRepository = mockk<AuthRepository>(),
    )

    private fun practiceVisitResponse() = com.dororong.rodi.core.data.source.remote.model.practice.PracticeVisitResponse(
        visitCount = 1,
    )

    private fun <T> failureEnvelope(code: String): ApiEnvelope<T> = ApiEnvelope(
        isSuccess = false,
        code = code,
        message = "실패",
        data = null,
    )
}
