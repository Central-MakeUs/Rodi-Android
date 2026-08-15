package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.local.security.AuthTokens
import com.dororong.rodi.core.data.source.remote.api.ReviewApi
import com.dororong.rodi.core.data.source.remote.model.review.CursorPageReviewResponse
import com.dororong.rodi.core.data.source.remote.model.review.ReportRequest
import com.dororong.rodi.core.data.source.remote.model.review.ReviewRequest
import com.dororong.rodi.core.data.source.remote.model.review.ReviewResponse
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.data.test.assertThrowsSuspend
import com.dororong.rodi.core.domain.model.review.PracticeMethod
import com.dororong.rodi.core.domain.model.review.ReportSubmission
import com.dororong.rodi.core.domain.model.review.ReviewCongestion
import com.dororong.rodi.core.domain.model.review.ReviewDifficulty
import com.dororong.rodi.core.domain.model.review.ReviewDraft
import com.dororong.rodi.core.domain.model.review.ReviewException
import com.dororong.rodi.core.domain.model.review.ReviewLevelFilter
import com.dororong.rodi.core.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReviewRepositoryImplTest {
    @Test
    fun `reviews map items and cursor metadata`() = runTest {
        val api = mockk<ReviewApi>()
        val tokenStore = tokenStore()
        coEvery { api.getReviews("Bearer access", 7, null, 10, null) } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = CursorPageReviewResponse(
                items = listOf(reviewResponse()),
                hasNext = true,
                nextCursor = "next-1",
                totalCount = 12,
            ),
        )
        val repository = repository(api, tokenStore)

        val result = repository.getReviews(7, ReviewLevelFilter.Mine)

        assertEquals(1L, result.items.single().reviewId)
        assertEquals(true, result.hasNext)
        assertEquals("next-1", result.nextCursor)
        assertEquals(12, result.totalCount)
    }

    @Test
    fun `unauthorized response refreshes once and retries with new token`() = runTest {
        val api = mockk<ReviewApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val authRepository = mockk<AuthRepository>()
        coEvery { tokenStore.getTokens() } returnsMany listOf(tokens("old"), tokens("new"))
        coEvery { api.getReviews("Bearer old", 7, null, 10, null) } returns failureEnvelope("COMMON_401")
        coEvery { authRepository.reissueToken() } returns Unit
        coEvery { api.getReviews("Bearer new", 7, null, 10, null) } returns reviewPageEnvelope()
        val repository = ReviewRepositoryImpl(api, tokenStore, authRepository)

        repository.getReviews(7)

        coVerify(exactly = 1) { authRepository.reissueToken() }
        coVerify(exactly = 1) { api.getReviews("Bearer new", 7, null, 10, null) }
    }

    @Test
    fun `second unauthorized response throws authentication required without another refresh`() = runTest {
        val api = mockk<ReviewApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val authRepository = mockk<AuthRepository>()
        coEvery { tokenStore.getTokens() } returnsMany listOf(tokens("old"), tokens("new"))
        coEvery { api.getReviews(any(), 7, null, 10, null) } returns failureEnvelope("COMMON_401")
        coEvery { authRepository.reissueToken() } returns Unit
        val repository = ReviewRepositoryImpl(api, tokenStore, authRepository)

        assertThrowsSuspend<ReviewException.AuthenticationRequired> { repository.getReviews(7) }

        coVerify(exactly = 1) { authRepository.reissueToken() }
    }

    @Test
    fun `create conflict maps to level required`() = runTest {
        val api = mockk<ReviewApi>()
        coEvery { api.createReview("Bearer access", 7, reviewRequest()) } returns failureEnvelope("COMMON_409")
        val repository = repository(api)

        assertThrowsSuspend<ReviewException.LevelRequired> { repository.createReview(7, draft()) }
    }

    @Test
    fun `update conflict maps to level changed`() = runTest {
        val api = mockk<ReviewApi>()
        coEvery { api.updateReview("Bearer access", 1, reviewRequest()) } returns failureEnvelope("COMMON_409")
        val repository = repository(api)

        assertThrowsSuspend<ReviewException.LevelChanged> { repository.updateReview(1, draft()) }
    }

    @Test
    fun `update forbidden maps to forbidden`() = runTest {
        val api = mockk<ReviewApi>()
        coEvery { api.updateReview("Bearer access", 1, reviewRequest()) } returns failureEnvelope("COMMON_403")
        val repository = repository(api)

        assertThrowsSuspend<ReviewException.Forbidden> { repository.updateReview(1, draft()) }
    }

    @Test
    fun `delete forbidden maps to forbidden`() = runTest {
        val api = mockk<ReviewApi>()
        coEvery { api.deleteReview("Bearer access", 1) } returns failureEnvelope("COMMON_403")
        val repository = repository(api)

        assertThrowsSuspend<ReviewException.Forbidden> { repository.deleteReview(1) }
    }

    @Test
    fun `report bad request maps to invalid request`() = runTest {
        val api = mockk<ReviewApi>()
        val request = ReportRequest("SELF", null, true)
        coEvery { api.reportReview("Bearer access", 1, request) } returns failureEnvelope("COMMON_400")
        val repository = repository(api)

        assertThrowsSuspend<ReviewException.InvalidRequest> {
            repository.reportReview(1, ReportSubmission("SELF", null, true))
        }
    }

    private fun repository(
        api: ReviewApi,
        tokenStore: AuthTokenStore = tokenStore(),
    ) = ReviewRepositoryImpl(api, tokenStore, mockk<AuthRepository>())

    private fun tokenStore() = mockk<AuthTokenStore>().also {
        coEvery { it.getTokens() } returns tokens("access")
    }

    private fun tokens(accessToken: String) = AuthTokens(accessToken, "refresh", "kakao")

    private fun draft() = ReviewDraft(
        isRecommended = true,
        difficulty = ReviewDifficulty.VERY_EASY,
        congestion = ReviewCongestion.QUIET,
        practiceMethod = PracticeMethod.SOLO,
        content = "좋아요",
        caution = null,
    )

    private fun reviewRequest() = ReviewRequest(
        isRecommended = true,
        difficulty = "VERY_EASY",
        congestion = "QUIET",
        practiceMethod = "SOLO",
        content = "좋아요",
        caution = null,
    )

    private fun reviewResponse() = ReviewResponse(
        reviewId = 1,
        memberId = 2,
        nickname = "로디",
        practiceMethod = "SOLO",
        content = "좋아요",
        isMine = false,
        isEditable = false,
        isHidden = false,
        isVerifiedVisit = true,
        createdAt = "2026-08-08T00:00:00Z",
    )

    private fun reviewPageEnvelope() = ApiEnvelope(
        isSuccess = true,
        code = "COMMON_200",
        message = "성공",
        data = CursorPageReviewResponse(),
    )

    private fun <T> failureEnvelope(code: String): ApiEnvelope<T> = ApiEnvelope(
        isSuccess = false,
        code = code,
        message = "실패",
        data = null,
    )
}
