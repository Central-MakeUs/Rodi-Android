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
    fun `알 수 없는 난이도 집계 키는 제외한다`() {
        val response = ReviewSummaryResponse(
            level = "ALL",
            levelReviewCount = 3,
            totalReviewCount = 3,
            recommendCount = 2,
            notRecommendCount = 1,
            difficultyCounts = mapOf("VERY_EASY" to 2, "UNKNOWN_DIFFICULTY" to 1),
            levelCounts = emptyMap(),
        )

        val result = response.toDomain()

        assertEquals(mapOf(ReviewDifficulty.VERY_EASY to 2L), result.difficultyCounts)
    }

    @Test
    fun `totalReviewCount를 도메인 totalCount로 매핑한다`() {
        // 서버 스키마가 totalCount에서 levelReviewCount·totalReviewCount로 갈렸다(2026-08-13).
        // totalReviewCount(전체 레벨 합산)를 놓치면 전체보기 링크가 후기가 있어도 안 뜬다.
        val response = ReviewSummaryResponse(
            level = "ALL",
            levelReviewCount = 12,
            totalReviewCount = 12,
            recommendCount = 0,
            notRecommendCount = 0,
            difficultyCounts = emptyMap(),
            levelCounts = emptyMap(),
        )

        assertEquals(12, response.toDomain().totalCount)
    }

    @Test
    fun `후기 목록 항목은 상세 전용 필드 없이도 매핑한다`() {
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
    fun `신고 폼 선택지를 순서대로 정렬한다`() {
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
    fun `동행 연습 방식을 서버 enum으로 매핑한다`() {
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
    fun `서버 enum의 동행 연습 방식을 도메인으로 매핑한다`() {
        val result = checkNotNull(reviewResponse(practiceMethod = "ACCOMPANIED").toDomain())

        assertEquals(PracticeMethod.WITH_COMPANION, result.practiceMethod)
    }

    @Test
    fun `알 수 없는 연습 방식은 null로 매핑한다`() {
        val result = checkNotNull(reviewResponse(practiceMethod = "UNKNOWN").toDomain())

        assertNull(result.practiceMethod)
    }

    /** 서버가 오프셋 없이 내려주는 값이 목록 전체를 날려버리던 회귀. */
    @Test
    fun `오프셋 없는 작성 시각도 후기로 매핑한다`() {
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
