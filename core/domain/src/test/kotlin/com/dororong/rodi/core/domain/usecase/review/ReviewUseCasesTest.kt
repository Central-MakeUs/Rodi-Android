package com.dororong.rodi.core.domain.usecase.review

import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.review.PracticeMethod
import com.dororong.rodi.core.domain.model.review.ReportForm
import com.dororong.rodi.core.domain.model.review.ReportSubmission
import com.dororong.rodi.core.domain.model.review.Review
import com.dororong.rodi.core.domain.model.review.ReviewCongestion
import com.dororong.rodi.core.domain.model.review.ReviewDifficulty
import com.dororong.rodi.core.domain.model.review.ReviewDraft
import com.dororong.rodi.core.domain.model.review.ReviewLevelFilter
import com.dororong.rodi.core.domain.model.review.ReviewSummary
import com.dororong.rodi.core.domain.repository.ReviewRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReviewUseCasesTest {
    private val repository = mockk<ReviewRepository>()

    @Test
    fun `장소 후기 조회를 저장소에 한 번 위임한다`() = runTest {
        val page = CursorPage(listOf(review()), true, "next", 2)
        coEvery { repository.getReviews(7, ReviewLevelFilter.All, "cursor", 20) } returns page

        val result = GetPlaceReviewsUseCase(repository)(7, ReviewLevelFilter.All, "cursor", 20)

        assertEquals(page, result.getOrThrow())
        coVerify(exactly = 1) { repository.getReviews(7, ReviewLevelFilter.All, "cursor", 20) }
    }

    @Test
    fun `후기 요약 조회를 저장소에 한 번 위임한다`() = runTest {
        val summary = ReviewSummary(null, 1, 1, 0, emptyMap(), emptyMap())
        coEvery { repository.getSummary(7, ReviewLevelFilter.Mine) } returns summary

        val result = GetReviewSummaryUseCase(repository)(7)

        assertEquals(summary, result.getOrThrow())
        coVerify(exactly = 1) { repository.getSummary(7, ReviewLevelFilter.Mine) }
    }

    @Test
    fun `후기 작성을 저장소에 한 번 위임한다`() = runTest {
        val draft = draft()
        coEvery { repository.createReview(7, draft) } returns 3

        val result = CreateReviewUseCase(repository)(7, draft)

        assertEquals(3, result.getOrThrow())
        coVerify(exactly = 1) { repository.createReview(7, draft) }
    }

    @Test
    fun `후기 수정을 저장소에 한 번 위임한다`() = runTest {
        val draft = draft()
        coEvery { repository.updateReview(3, draft) } returns Unit

        val result = UpdateReviewUseCase(repository)(3, draft)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.updateReview(3, draft) }
    }

    @Test
    fun `후기 삭제를 저장소에 한 번 위임한다`() = runTest {
        coEvery { repository.deleteReview(3) } returns Unit

        val result = DeleteReviewUseCase(repository)(3)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.deleteReview(3) }
    }

    @Test
    fun `후기 신고를 저장소에 한 번 위임한다`() = runTest {
        val submission = ReportSubmission("SPAM", null, true)
        coEvery { repository.reportReview(3, submission) } returns Unit

        val result = ReportReviewUseCase(repository)(3, submission)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.reportReview(3, submission) }
    }

    @Test
    fun `신고 폼 조회를 저장소에 한 번 위임한다`() = runTest {
        val form = ReportForm("review-report", "신고 사유", null, true, emptyList())
        coEvery { repository.getReportForm() } returns form

        val result = GetReportFormUseCase(repository)()

        assertEquals(form, result.getOrThrow())
        coVerify(exactly = 1) { repository.getReportForm() }
    }

    private fun draft() = ReviewDraft(
        isRecommended = true,
        difficulty = ReviewDifficulty.VERY_EASY,
        congestion = ReviewCongestion.QUIET,
        practiceMethod = PracticeMethod.SOLO,
        content = null,
        caution = null,
    )

    private fun review() = Review(
        reviewId = 1,
        memberId = 2,
        nickname = "로디",
        memberLevel = OnboardingLevel.ROOKIE,
        isRecommended = true,
        difficulty = ReviewDifficulty.VERY_EASY,
        congestion = ReviewCongestion.QUIET,
        practiceMethod = PracticeMethod.SOLO,
        content = null,
        caution = null,
        isMine = false,
        isEditable = false,
        isHidden = false,
        createdAt = Instant.parse("2026-08-08T00:00:00Z"),
        isVerifiedVisit = true,
    )
}
