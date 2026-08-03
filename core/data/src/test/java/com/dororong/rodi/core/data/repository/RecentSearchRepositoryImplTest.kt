package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.local.security.AuthTokens
import com.dororong.rodi.core.data.source.remote.api.RecentSearchApi
import com.dororong.rodi.core.data.source.remote.model.search.RecentSearchResponse
import com.dororong.rodi.core.data.source.remote.model.search.RecentSearchRegisterRequest
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.data.test.assertThrowsSuspend
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.repository.AuthRepository
import com.dororong.rodi.core.domain.model.search.RecentSearchRegistration
import com.dororong.rodi.core.domain.model.search.SearchTargetType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RecentSearchRepositoryImplTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `recent searches use bearer access token and map identifiers`() = runTest {
        val api = mockk<RecentSearchApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { api.getRecentSearches("Bearer access") } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = listOf(RecentSearchResponse(5, "서울 중구")),
        )
        val repository = RecentSearchRepositoryImpl(api, tokenStore, mockk<AuthRepository>(), json)

        val searches = repository.getRecentSearches()

        assertEquals(listOf("서울 중구"), searches.map { it.keyword })
        assertEquals(listOf(5L), searches.map { it.id })
    }

    @Test
    fun `recent search delete retries once after access token refresh`() = runTest {
        val api = mockk<RecentSearchApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val authRepository = mockk<AuthRepository>()
        coEvery { tokenStore.getTokens() } returnsMany listOf(
            AuthTokens("old", "refresh", "kakao"),
            AuthTokens("new", "refresh", "kakao"),
        )
        coEvery { api.deleteRecentSearch("Bearer old", 7) } returns ApiEnvelope(
            isSuccess = false,
            code = "COMMON_401",
            message = "만료됨",
        )
        coEvery { authRepository.reissueToken() } returns Unit
        coEvery { api.deleteRecentSearch("Bearer new", 7) } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = buildJsonObject { },
        )
        val repository = RecentSearchRepositoryImpl(api, tokenStore, authRepository, json)

        repository.deleteRecentSearch(7)

        coVerify(exactly = 1) { authRepository.reissueToken() }
        coVerify(exactly = 1) { api.deleteRecentSearch("Bearer new", 7) }
    }

    @Test
    fun `recent search registration sends the selected place id`() = runTest {
        val api = mockk<RecentSearchApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery {
            api.registerRecentSearch(
                "Bearer access",
                RecentSearchRegisterRequest("PLACE", "중구 연습 코스", 13),
            )
        } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = buildJsonObject { },
        )
        val repository = RecentSearchRepositoryImpl(api, tokenStore, mockk<AuthRepository>(), json)

        repository.registerRecentSearch(
            RecentSearchRegistration(SearchTargetType.PLACE, "중구 연습 코스", 13),
        )

        coVerify(exactly = 1) {
            api.registerRecentSearch(
                "Bearer access",
                RecentSearchRegisterRequest("PLACE", "중구 연습 코스", 13),
            )
        }
    }

    @Test
    fun `recent search request propagates cancellation without token refresh`() = runTest {
        val api = mockk<RecentSearchApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val authRepository = mockk<AuthRepository>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { api.getRecentSearches("Bearer access") } throws CancellationException("cancelled")
        val repository = RecentSearchRepositoryImpl(api, tokenStore, authRepository, json)

        assertThrowsSuspend<CancellationException> { repository.getRecentSearches() }

        coVerify(exactly = 0) { authRepository.reissueToken() }
    }

    @Test
    fun `recent search request maps non authentication failure to domain exception`() = runTest {
        val api = mockk<RecentSearchApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { api.getRecentSearches("Bearer access") } returns ApiEnvelope(
            isSuccess = false,
            code = "COMMON_500",
            message = "서버 오류",
        )
        val repository = RecentSearchRepositoryImpl(api, tokenStore, mockk<AuthRepository>(), json)

        assertThrowsSuspend<AuthException.Unknown> { repository.getRecentSearches() }
    }
}
