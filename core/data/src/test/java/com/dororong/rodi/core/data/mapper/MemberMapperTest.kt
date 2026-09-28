package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.member.BlockedMemberItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.LevelProgressResponse
import com.dororong.rodi.core.data.source.remote.model.member.MyPageResponse
import com.dororong.rodi.core.data.source.remote.model.member.MyReviewItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.PracticeItemResponse
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * 서버가 `format: date-time`으로 선언해두고 오프셋 없이 내려보내던 값이 예외를 던져
 * 내 게시글·차단목록·연습기록이 통째로 비어 보이던 회귀를 막는다.
 */
class MemberMapperTest {
    @Test
    fun `오프셋 없는 작성 시각도 내 후기로 매핑한다`() {
        val result = MyReviewItemResponse(
            reviewId = 1,
            placeId = 10,
            placeName = "망원한강공원",
            content = "차선 변경 연습에 좋아요.",
            isEditable = true,
            isHidden = false,
            isVerifiedVisit = false,
            createdAt = OFFSET_LESS,
        ).toDomain()

        assertEquals(parseServerTimestamp(OFFSET_LESS), result.createdAt)
    }

    @Test
    fun `오프셋 없는 차단 시각도 차단 회원으로 매핑한다`() {
        val result = BlockedMemberItemResponse(
            memberId = 2,
            nickname = "로디",
            blockedAt = OFFSET_LESS,
        ).toDomain()

        assertEquals(parseServerTimestamp(OFFSET_LESS), result.blockedAt)
    }

    @Test
    fun `오프셋 없는 마지막 활동 시각도 연습 기록으로 매핑한다`() {
        val result = practiceItem(lastActivityAt = OFFSET_LESS).toDomain()

        assertEquals(parseServerTimestamp(OFFSET_LESS), result.visitedAt)
    }

    @Test
    fun `마지막 활동 시각이 없으면 null로 둔다`() {
        assertNull(practiceItem(lastActivityAt = null).toDomain().visitedAt)
    }

    @Test
    fun `연습 목록 응답의 마지막 활동 시각을 기록 날짜로 쓴다`() {
        val response = Json.decodeFromString<PracticeItemResponse>(
            """
            {
              "practiceId": 1,
              "placeId": 10,
              "placeName": "망원한강공원",
              "practiceTypes": ["ROUNDABOUT"],
              "status": "VISITED",
              "visitCount": 1,
              "lastActivityAt": "$OFFSET_LESS",
              "hasReview": false
            }
            """.trimIndent(),
        )

        assertEquals(parseServerTimestamp(OFFSET_LESS), response.toDomain().visitedAt)
    }

    @Test
    fun `알려진 연습 상태를 도메인 상태로 매핑한다`() {
        assertEquals(
            com.dororong.rodi.core.domain.model.practice.PracticeStatus.VISITED,
            practiceItem(lastActivityAt = null, status = "VISITED").toDomain().status,
        )
    }

    @Test
    fun `알 수 없는 연습 상태는 방문 예정으로 취급하지 않고 실패한다`() {
        assertThrows(AuthException.Unknown::class.java) {
            practiceItem(lastActivityAt = null, status = "UNKNOWN_STATUS").toDomain()
        }
    }

    private fun practiceItem(lastActivityAt: String?, status: String = "PLANNED") = PracticeItemResponse(
        practiceId = 1,
        placeId = 10,
        placeName = "망원한강공원",
        practiceTypes = listOf("ROUNDABOUT"),
        visitCount = 1,
        lastActivityAt = lastActivityAt,
        hasReview = false,
        status = status,
    )

    private fun myPage(level: String) = MyPageResponse(
        nickname = "로디",
        level = level,
        recommendationTags = emptyList(),
        savedPlaceCount = 0,
        levelProgress = LevelProgressResponse(totalDistanceKm = 0.0, currentLevelStartKm = 0.0, progressPercent = 0),
    )

    private companion object {
        const val OFFSET_LESS = "2026-08-10T10:47:33.996642"
    }

    @Test
    fun `알려진 회원 레벨을 도메인 레벨로 매핑한다`() {
        val result = myPage(level = "NAVIGATOR").toDomain()

        assertEquals(OnboardingLevel.NAVIGATOR, result.level)
    }

    @Test
    fun `알 수 없는 회원 레벨은 최저 레벨로 보여주지 않고 실패한다`() {
        assertThrows(AuthException.Unknown::class.java) {
            myPage(level = "NEW_LEVEL").toDomain()
        }
    }
}
