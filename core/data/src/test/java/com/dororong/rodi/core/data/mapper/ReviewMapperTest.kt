package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.review.CursorPageReviewResponse
import com.dororong.rodi.core.data.source.remote.model.review.ReportFormOptionResponse
import com.dororong.rodi.core.data.source.remote.model.review.ReportFormResponse
import com.dororong.rodi.core.data.source.remote.model.review.ReviewResponse
import com.dororong.rodi.core.data.source.remote.model.review.ReviewSummaryResponse
import com.dororong.rodi.core.domain.model.review.ReviewDifficulty
import com.dororong.rodi.core.domain.model.review.ReviewDraft
import com.dororong.rodi.core.domain.model.review.ReviewCongestion
import com.dororong.rodi.core.domain.model.review.PracticeMethod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ReviewMapperTest {
    @Test
    fun `unknown difficulty maps to null without throwing`() {
        val result = checkNotNull(reviewResponse(difficulty = "UNKNOWN_DIFFICULTY").toDomain())

        assertNull(result.difficulty)
    }

    @Test
    fun `unknown difficulty count key is excluded`() {
        val response = ReviewSummaryResponse(
            level = "ALL",
            totalCount = 3,
            recommendCount = 2,
            notRecommendCount = 1,
            difficultyCounts = mapOf("VERY_EASY" to 2, "UNKNOWN_DIFFICULTY" to 1),
        )

        val result = response.toDomain()

        assertEquals(mapOf(ReviewDifficulty.VERY_EASY to 2L), result.difficultyCounts)
    }

    @Test
    fun `unknown member level excludes only that review`() {
        val response = CursorPageReviewResponse(
            items = listOf(
                reviewResponse(reviewId = 1, memberLevel = "ROOKIE"),
                reviewResponse(reviewId = 2, memberLevel = "UNKNOWN_LEVEL"),
                reviewResponse(reviewId = 3, memberLevel = "OWNER"),
            ),
            hasNext = true,
            nextCursor = "next",
            totalCount = 3,
        )

        val result = response.toDomain()

        assertEquals(listOf(1L, 3L), result.items.map { it.reviewId })
        assertEquals(true, result.hasNext)
        assertEquals("next", result.nextCursor)
        assertEquals(3, result.totalCount)
    }

    @Test
    fun `report form options are sorted by order`() {
        val response = ReportFormResponse(
            questionId = "review-report",
            title = "신고 사유",
            required = true,
            options = listOf(
                reportOption("THIRD", 3),
                reportOption("FIRST", 1),
                reportOption("SECOND", 2),
            ),
        )

        val result = response.toDomain()

        assertEquals(listOf("FIRST", "SECOND", "THIRD"), result.options.map { it.code })
    }

    @Test
    fun `accompanied practice method maps to server enum`() {
        val request = ReviewDraft(
            isRecommended = true,
            difficulty = ReviewDifficulty.EASY,
            congestion = ReviewCongestion.QUIET,
            practiceMethod = PracticeMethod.WITH_COMPANION,
            content = "내용",
            caution = null,
        ).toRequest()

        assertEquals("ACCOMPANIED", request.practiceMethod)
    }

    @Test
    fun `accompanied practice method maps from server enum`() {
        val result = checkNotNull(reviewResponse(practiceMethod = "ACCOMPANIED").toDomain())

        assertEquals(PracticeMethod.WITH_COMPANION, result.practiceMethod)
    }

    private fun reviewResponse(
        reviewId: Long = 1,
        memberLevel: String = "ROOKIE",
        difficulty: String? = "VERY_EASY",
        practiceMethod: String? = "SOLO",
    ) = ReviewResponse(
        reviewId = reviewId,
        memberId = 10,
        nickname = "로디",
        memberLevel = memberLevel,
        isRecommended = true,
        difficulty = difficulty,
        congestion = "QUIET",
        practiceMethod = practiceMethod,
        isMine = false,
        isEditable = false,
        isHidden = false,
        createdAt = "2026-08-08T00:00:00Z",
    )

    private fun reportOption(code: String, order: Int) = ReportFormOptionResponse(
        code = code,
        label = code,
        order = order,
        requiresTextInput = false,
    )
}
