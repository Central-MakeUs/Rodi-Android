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
    fun `최근 검색어의 키워드와 id를 매핑한다`() = runTest {
        val api = mockk<RecentSearchApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { api.getRecentSearches() } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = listOf(RecentSearchResponse(5, "서울 중구")),
        )
        val repository = RecentSearchRepositoryImpl(api, tokenStore, json)

        val searches = repository.getRecentSearches()

        assertEquals(listOf("서울 중구"), searches.map { it.keyword })
        assertEquals(listOf(5L), searches.map { it.id })
    }

    @Test
    fun `최근 검색어 등록은 선택한 장소 id를 보낸다`() = runTest {
        val api = mockk<RecentSearchApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery {
            api.registerRecentSearch(
                RecentSearchRegisterRequest("PLACE", "중구 연습 코스", 13),
            )
        } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = buildJsonObject { },
        )
        val repository = RecentSearchRepositoryImpl(api, tokenStore, json)

        repository.registerRecentSearch(
            RecentSearchRegistration(SearchTargetType.PLACE, "중구 연습 코스", 13),
        )

        coVerify(exactly = 1) {
            api.registerRecentSearch(
                RecentSearchRegisterRequest("PLACE", "중구 연습 코스", 13),
            )
        }
    }

    @Test
    fun `최근 검색어 요청 중 취소는 토큰 재발급 없이 전파한다`() = runTest {
        val api = mockk<RecentSearchApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val authRepository = mockk<AuthRepository>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { api.getRecentSearches() } throws CancellationException("cancelled")
        val repository = RecentSearchRepositoryImpl(api, tokenStore, json)

        assertThrowsSuspend<CancellationException> { repository.getRecentSearches() }

        coVerify(exactly = 0) { authRepository.reissueToken() }
    }

    @Test
    fun `최근 검색어 요청의 인증 외 실패를 도메인 예외로 매핑한다`() = runTest {
        val api = mockk<RecentSearchApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { api.getRecentSearches() } returns ApiEnvelope(
            isSuccess = false,
            code = "COMMON_500",
            message = "서버 오류",
        )
        val repository = RecentSearchRepositoryImpl(api, tokenStore, json)

        assertThrowsSuspend<AuthException.Unknown> { repository.getRecentSearches() }
    }
}
