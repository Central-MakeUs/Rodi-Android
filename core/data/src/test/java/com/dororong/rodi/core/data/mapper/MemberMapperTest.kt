package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.member.BlockedMemberItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.MyReviewItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.PracticeItemResponse
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * 서버가 `format: date-time`으로 선언해두고 오프셋 없이 내려보내던 값이 예외를 던져
 * 내 게시글·차단목록·연습기록이 통째로 비어 보이던 회귀를 막는다.
 */
class MemberMapperTest {
    @Test
    fun `offset-less createdAt does not break my review mapping`() {
        val result = MyReviewItemResponse(
            reviewId = 1,
            placeId = 10,
            placeName = "망원한강공원",
            content = "차선 변경 연습에 좋아요.",
            createdAt = OFFSET_LESS,
        ).toDomain()

        assertEquals(parseServerTimestamp(OFFSET_LESS), result.createdAt)
    }

    @Test
    fun `offset-less blockedAt does not break blocked member mapping`() {
        val result = BlockedMemberItemResponse(
            memberId = 2,
            nickname = "로디",
            blockedAt = OFFSET_LESS,
        ).toDomain()

        assertEquals(parseServerTimestamp(OFFSET_LESS), result.blockedAt)
    }

    @Test
    fun `offset-less lastActivityAt does not break practice mapping`() {
        val result = practiceItem(lastActivityAt = OFFSET_LESS).toDomain()

        assertEquals(parseServerTimestamp(OFFSET_LESS), result.visitedAt)
    }

    @Test
    fun `absent lastActivityAt stays null`() {
        assertNull(practiceItem(lastActivityAt = null).toDomain().visitedAt)
    }

    @Test
    fun `lastActivityAt from the member practices response becomes the record date`() {
        val response = Json.decodeFromString<PracticeItemResponse>(
            """
            {
              "practiceId": 1,
              "placeId": 10,
              "placeName": "망원한강공원",
              "status": "VISITED",
              "visitCount": 1,
              "lastActivityAt": "$OFFSET_LESS"
            }
            """.trimIndent(),
        )

        assertEquals(parseServerTimestamp(OFFSET_LESS), response.toDomain().visitedAt)
    }

    @Test
    fun `known practice status maps to domain status`() {
        assertEquals(
            com.dororong.rodi.core.domain.model.practice.PracticeStatus.VISITED,
            practiceItem(lastActivityAt = null, status = "VISITED").toDomain().status,
        )
    }

    @Test
    fun `unknown practice status falls back to planned`() {
        assertEquals(
            com.dororong.rodi.core.domain.model.practice.PracticeStatus.PLANNED,
            practiceItem(lastActivityAt = null, status = "UNKNOWN_STATUS").toDomain().status,
        )
    }

    private fun practiceItem(lastActivityAt: String?, status: String = "PLANNED") = PracticeItemResponse(
        practiceId = 1,
        placeId = 10,
        placeName = "망원한강공원",
        practiceTypes = listOf("ROUNDABOUT"),
        visitCount = 1,
        lastActivityAt = lastActivityAt,
        status = status,
    )

    private companion object {
        const val OFFSET_LESS = "2026-08-10T10:47:33.996642"
    }
}
