package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.review.ReportFormOptionResponse
import com.dororong.rodi.core.data.source.remote.model.review.ReportFormResponse
import com.dororong.rodi.core.data.source.remote.model.review.ReviewResponse
import com.dororong.rodi.core.data.source.remote.model.review.ReviewSummaryResponse
import com.dororong.rodi.core.domain.model.review.ReviewDifficulty
import com.dororong.rodi.core.domain.model.review.ReviewDraft
import com.dororong.rodi.core.domain.model.review.ReviewCongestion
import com.dororong.rodi.core.domain.model.review.PracticeMethod
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ReviewMapperTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `unknown difficulty count key is excluded`() {
        val response = ReviewSummaryResponse(
            level = "ALL",
            totalReviewCount = 3,
            recommendCount = 2,
            notRecommendCount = 1,
            difficultyCounts = mapOf("VERY_EASY" to 2, "UNKNOWN_DIFFICULTY" to 1),
        )

        val result = response.toDomain()

        assertEquals(mapOf(ReviewDifficulty.VERY_EASY to 2L), result.difficultyCounts)
    }

    @Test
    fun `totalReviewCount maps to domain totalCount`() {
        // 서버 스키마가 totalCount에서 levelReviewCount·totalReviewCount로 갈렸다(2026-08-13).
        // totalReviewCount(전체 레벨 합산)를 놓치면 전체보기 링크가 후기가 있어도 안 뜬다.
        val response = ReviewSummaryResponse(level = "ALL", totalReviewCount = 12)

        assertEquals(12, response.toDomain().totalCount)
    }

    @Test
    fun `review item payload does not require detail-only fields`() {
        val response = json.decodeFromString<ReviewResponse>(
            """
            {
              "reviewId": 1,
              "memberId": 10,
              "nickname": "로디",
              "practiceMethod": "SOLO",
              "content": "좋아요",
              "isMine": false,
              "isEditable": false,
              "isHidden": false,
              "isVerifiedVisit": true,
              "createdAt": "2026-08-08T00:00:00Z"
            }
            """.trimIndent(),
        )

        val result = response.toDomain()

        assertEquals(1L, result.reviewId)
        assertEquals(true, result.isVerifiedVisit)
        assertNull(result.memberLevel)
        assertNull(result.isRecommended)
        assertNull(result.difficulty)
        assertNull(result.congestion)
        assertNull(result.caution)
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

    @Test
    fun `unknown practice method maps to null`() {
        val result = checkNotNull(reviewResponse(practiceMethod = "UNKNOWN").toDomain())

        assertNull(result.practiceMethod)
    }

    /** 서버가 오프셋 없이 내려주는 값이 목록 전체를 날려버리던 회귀. */
    @Test
    fun `offset-less createdAt does not break review mapping`() {
        val result = checkNotNull(reviewResponse(createdAt = "2026-08-10T10:47:33.996642").toDomain())

        assertEquals(parseServerTimestamp("2026-08-10T10:47:33.996642"), result.createdAt)
    }

    private fun reviewResponse(
        reviewId: Long = 1,
        practiceMethod: String? = "SOLO",
        createdAt: String = "2026-08-08T00:00:00Z",
    ) = ReviewResponse(
        reviewId = reviewId,
        memberId = 10,
        nickname = "로디",
        practiceMethod = practiceMethod,
        content = "좋아요",
        isMine = false,
        isEditable = false,
        isHidden = false,
        isVerifiedVisit = true,
        createdAt = createdAt,
    )

    private fun reportOption(code: String, order: Int) = ReportFormOptionResponse(
        code = code,
        label = code,
        order = order,
        requiresTextInput = false,
    )
}
