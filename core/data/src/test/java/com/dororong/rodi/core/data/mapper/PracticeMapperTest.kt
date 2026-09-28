package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.practice.FormOptionResponse
import com.dororong.rodi.core.data.source.remote.model.practice.FormResponse
import com.dororong.rodi.core.data.source.remote.model.practice.PracticeRegisterResponse
import com.dororong.rodi.core.data.source.remote.model.practice.PracticeVisitResponse
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.practice.PracticeException
import com.dororong.rodi.core.domain.model.practice.PracticeStatus
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PracticeMapperTest {
    @Test
    fun `연습 등록 응답의 필드를 매핑한다`() {
        val result = PracticeRegisterResponse(7, "VISITED", 2, 800).toDomain()

        assertEquals(7L, result.practiceId)
        assertEquals(PracticeStatus.VISITED, result.status)
        assertEquals(2, result.visitCount)
        assertEquals(800, result.requiredDistanceMeters)
    }

    @Test
    fun `알 수 없는 연습 상태는 방문 예정으로 취급하지 않고 실패한다`() {
        assertThrows(PracticeException.Unexpected::class.java) {
            PracticeRegisterResponse(practiceId = 1, status = "NEW_STATUS", visitCount = 0, requiredDistanceMeters = 0).toDomain()
        }
    }

    @Test
    fun `방문 응답의 알려진 레벨을 매핑한다`() {
        val result = PracticeVisitResponse(visitCount = 1, addedCertifiedDistanceMeters = 0, requiredDistanceMeters = 0, isCertifiedNow = true, totalDistanceKm = 0.0, levelUp = true, newLevel = "NAVIGATOR").toDomain()

        assertEquals(true, result.levelUp)
        assertEquals(OnboardingLevel.NAVIGATOR, result.newLevel)
    }

    @Test
    fun `방문 응답의 알 수 없는 레벨은 null로 매핑한다`() {
        val result = PracticeVisitResponse(visitCount = 1, addedCertifiedDistanceMeters = 0, requiredDistanceMeters = 0, isCertifiedNow = true, totalDistanceKm = 0.0, levelUp = true, newLevel = "NEW_LEVEL").toDomain()

        assertNull(result.newLevel)
    }

    @Test
    fun `미방문 사유 선택지를 정렬하고 오프셋 없는 시각도 해석한다`() {
        val result = FormResponse(
            questionId = "practice-skip",
            type = "SINGLE_SELECT",
            title = "미방문 사유",
            required = true,
            options = listOf(
                FormOptionResponse(code = "OTHER", label = "기타", order = 2, requiresTextInput = true),
                FormOptionResponse(code = "TOO_FAR", label = "멀어요", order = 1, requiresTextInput = false),
            ),
        ).toDomain()

        assertEquals(listOf("TOO_FAR", "OTHER"), result.options.map { it.code })
        assertEquals(
            Instant.parse("2026-08-10T01:47:33.996642Z"),
            parseServerTimestamp("2026-08-10T10:47:33.996642"),
        )
    }
}
