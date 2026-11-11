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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response

class ReviewRepositoryImplTest {
    @Test
    fun `후기 폼 API가 없으면 기존 후기 입력을 사용한다`() = runTest {
        val api = mockk<ReviewApi>()
        coEvery { api.getReviewForm(7) } throws httpException(404)
        assertNull(repository(api).getReviewIssueForm(7))
    }

    @Test
    fun `후기 폼 서버 오류는 기존 입력으로 숨기지 않고 전달한다`() = runTest {
        val api = mockk<ReviewApi>()
        coEvery { api.getReviewForm(7) } throws httpException(500)
        assertThrowsSuspend<ReviewException.Unexpected> { repository(api).getReviewIssueForm(7) }
    }

    @Test
    fun `주차장 후기 폼 거절은 후기를 받지 않는 장소로 알린다`() = runTest {
        val api = mockk<ReviewApi>()
        coEvery { api.getReviewForm(7) } throws httpException(
            400,
            """{"isSuccess":false,"code":"REVIEW_400_2","message":"주차장에는 후기를 작성할 수 없습니다.","data":null}""",
        )
        val error = assertThrowsSuspend<ReviewException.PlaceNotReviewable> { repository(api).getReviewIssueForm(7) }
        assertEquals("주차장에는 후기를 작성할 수 없습니다.", error.userMessage)
    }

    @Test
    fun `다른 400 오류는 잘못된 요청으로 유지한다`() = runTest {
        val api = mockk<ReviewApi>()
        coEvery { api.getReviewForm(7) } throws httpException(
            400,
            """{"isSuccess":false,"code":"COMMON_400","message":"잘못된 요청","data":null}""",
        )
        assertThrowsSuspend<ReviewException.InvalidRequest> { repository(api).getReviewIssueForm(7) }
    }

    private fun httpException(code: Int, body: String = "{}") = HttpException(
        Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())),
    )

    @Test
    fun `후기 목록 항목과 커서 정보를 매핑한다`() = runTest {
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
    fun `예상하지 못한 전송 오류는 후기 공통 문구로 알린다`() = runTest {
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
    fun `후기 작성 충돌은 레벨 필요로 매핑한다`() = runTest {
        val api = mockk<ReviewApi>()
        coEvery { api.createReview(7, reviewRequest()) } returns failureEnvelope("COMMON_409")
        val repository = repository(api)

        assertThrowsSuspend<ReviewException.LevelRequired> { repository.createReview(7, draft()) }
    }

    @Test
    fun `후기 수정 충돌은 레벨 변경으로 매핑한다`() = runTest {
        val api = mockk<ReviewApi>()
        coEvery { api.updateReview(1, reviewRequest()) } returns failureEnvelope("COMMON_409")
        val repository = repository(api)

        assertThrowsSuspend<ReviewException.LevelChanged> { repository.updateReview(1, draft()) }
    }

    @Test
    fun `후기 수정 권한 없음은 Forbidden으로 매핑한다`() = runTest {
        val api = mockk<ReviewApi>()
        coEvery { api.updateReview(1, reviewRequest()) } returns failureEnvelope("COMMON_403")
        val repository = repository(api)

        assertThrowsSuspend<ReviewException.Forbidden> { repository.updateReview(1, draft()) }
    }

    @Test
    fun `후기 삭제 권한 없음은 Forbidden으로 매핑한다`() = runTest {
        val api = mockk<ReviewApi>()
        coEvery { api.deleteReview(1) } returns failureEnvelope("COMMON_403")
        val repository = repository(api)

        assertThrowsSuspend<ReviewException.Forbidden> { repository.deleteReview(1) }
    }

    @Test
    fun `후기 신고의 잘못된 요청은 InvalidRequest로 매핑한다`() = runTest {
        val api = mockk<ReviewApi>()
        val request = ReportRequest("SELF", null)
        coEvery { api.reportReview(1, request) } returns failureEnvelope("COMMON_400")
        val repository = repository(api)

        assertThrowsSuspend<ReviewException.InvalidRequest> {
            repository.reportReview(1, ReportSubmission("SELF", null))
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
