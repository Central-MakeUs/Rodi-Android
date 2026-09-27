package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.cache.PracticeRecordPresenceCache
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.local.security.AuthTokens
import com.dororong.rodi.core.data.source.remote.api.PracticeApi
import com.dororong.rodi.core.data.source.remote.model.practice.PracticeRegisterResponse
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PracticeRepositoryImplTest {
    @Test
    fun `visit request omits certified distance in phase a`() = runTest {
        val api = mockk<PracticeApi>()
        coEvery { api.recordVisit(7, PracticeVisitRequest(null)) } returns
            ApiEnvelope(true, "COMMON_200", "성공", practiceVisitResponse())
        val cache = PracticeRecordPresenceCache()
        val repository = repository(api, cache)

        val result = repository.recordVisit(7)

        assertEquals(1, result.visitCount)
        assertTrue(cache.get() == true)
    }

    @Test
    fun `registering a planned practice does not mark record presence`() = runTest {
        val api = mockk<PracticeApi>()
        coEvery { api.register(7) } returns
            ApiEnvelope(true, "COMMON_200", "성공", PracticeRegisterResponse(practiceId = 11, status = "PLANNED", visitCount = 0, requiredDistanceMeters = 0))
        val cache = PracticeRecordPresenceCache()
        val repository = repository(api, cache)

        repository.register(7)

        assertNull(cache.get())
    }

    @Test
    fun `skip reason conflict maps to already submitted`() = runTest {
        val api = mockk<PracticeApi>()
        coEvery { api.submitSkipReason(7, PracticeSkipReasonRequest("OTHER", "이유")) } returns
            failureEnvelope("PRACTICE_409")
        val repository = repository(api)

        assertThrowsSuspend<PracticeException.SkipReasonAlreadySubmitted> {
            repository.submitSkipReason(7, "OTHER", "이유")
        }
    }

    private fun repository(
        api: PracticeApi,
        cache: PracticeRecordPresenceCache = PracticeRecordPresenceCache(),
    ): PracticeRepositoryImpl = PracticeRepositoryImpl(
        api = api,
        tokenStore = mockk<AuthTokenStore>().also {
            coEvery { it.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        },
        practiceRecordPresenceCache = cache,
    )

    private fun practiceVisitResponse() = com.dororong.rodi.core.data.source.remote.model.practice.PracticeVisitResponse(
        visitCount = 1,
        addedCertifiedDistanceMeters = 0,
        requiredDistanceMeters = 0,
        isCertifiedNow = true,
        totalDistanceKm = 0.0,
        levelUp = false,
    )

    private fun <T> failureEnvelope(code: String): ApiEnvelope<T> = ApiEnvelope(
        isSuccess = false,
        code = code,
        message = "실패",
        data = null,
    )
}
