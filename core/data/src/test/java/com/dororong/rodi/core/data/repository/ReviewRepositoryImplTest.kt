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
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class ReviewRepositoryImplTest {
    @Test
    fun `reviews map items and cursor metadata`() = runTest {
        val api = mockk<ReviewApi>()
        val tokenStore = tokenStore()
        coEvery { api.getReviews(7, null, 10, null) } returns ApiEnvelope(
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
    fun `unexpected transport error uses generic review message`() = runTest {
        val api = mockk<ReviewApi>()
        val cause = IllegalStateException("Field 'totalCount' is required")
        coEvery { api.getReviews(7, null, 10, null) } throws cause
        val repository = repository(api)

        val exception = assertThrowsSuspend<ReviewException.Unexpected> {
            repository.getReviews(7)
        }

        assertEquals("후기 요청에 실패했습니다.", exception.userMessage)
        assertSame(cause, exception.cause)
    }

    @Test
    fun `create conflict maps to level required`() = runTest {
        val api = mockk<ReviewApi>()
        coEvery { api.createReview(7, reviewRequest()) } returns failureEnvelope("COMMON_409")
        val repository = repository(api)

        assertThrowsSuspend<ReviewException.LevelRequired> { repository.createReview(7, draft()) }
    }

    @Test
    fun `update conflict maps to level changed`() = runTest {
        val api = mockk<ReviewApi>()
        coEvery { api.updateReview(1, reviewRequest()) } returns failureEnvelope("COMMON_409")
        val repository = repository(api)

        assertThrowsSuspend<ReviewException.LevelChanged> { repository.updateReview(1, draft()) }
    }

    @Test
    fun `update forbidden maps to forbidden`() = runTest {
        val api = mockk<ReviewApi>()
        coEvery { api.updateReview(1, reviewRequest()) } returns failureEnvelope("COMMON_403")
        val repository = repository(api)

        assertThrowsSuspend<ReviewException.Forbidden> { repository.updateReview(1, draft()) }
    }

    @Test
    fun `delete forbidden maps to forbidden`() = runTest {
        val api = mockk<ReviewApi>()
        coEvery { api.deleteReview(1) } returns failureEnvelope("COMMON_403")
        val repository = repository(api)

        assertThrowsSuspend<ReviewException.Forbidden> { repository.deleteReview(1) }
    }

    @Test
    fun `report bad request maps to invalid request`() = runTest {
        val api = mockk<ReviewApi>()
        val request = ReportRequest("SELF", null, true)
        coEvery { api.reportReview(1, request) } returns failureEnvelope("COMMON_400")
        val repository = repository(api)

        assertThrowsSuspend<ReviewException.InvalidRequest> {
            repository.reportReview(1, ReportSubmission("SELF", null, true))
        }
    }

    private fun repository(
        api: ReviewApi,
        tokenStore: AuthTokenStore = tokenStore(),
    ) = ReviewRepositoryImpl(api, tokenStore, mockk(relaxed = true))

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
        data = CursorPageReviewResponse(items = emptyList(), hasNext = false),
    )

    private fun <T> failureEnvelope(code: String): ApiEnvelope<T> = ApiEnvelope(
        isSuccess = false,
        code = code,
        message = "실패",
        data = null,
    )
}
