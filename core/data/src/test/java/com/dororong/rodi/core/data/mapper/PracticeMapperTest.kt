package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.practice.FormOptionResponse
import com.dororong.rodi.core.data.source.remote.model.practice.FormResponse
import com.dororong.rodi.core.data.source.remote.model.practice.PracticeRegisterResponse
import com.dororong.rodi.core.data.source.remote.model.practice.PracticeVisitResponse
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.practice.PracticeStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PracticeMapperTest {
    @Test
    fun `register response maps practice fields`() {
        val result = PracticeRegisterResponse(7, "VISITED", 2, 800).toDomain()

        assertEquals(7L, result.practiceId)
        assertEquals(PracticeStatus.VISITED, result.status)
        assertEquals(2, result.visitCount)
        assertEquals(800, result.requiredDistanceMeters)
    }

    @Test
    fun `unknown practice status falls back to planned`() {
        val result = PracticeRegisterResponse(status = "NEW_STATUS").toDomain()

        assertEquals(PracticeStatus.PLANNED, result.status)
    }

    @Test
    fun `visit response maps known level`() {
        val result = PracticeVisitResponse(levelUp = true, newLevel = "NAVIGATOR").toDomain()

        assertEquals(true, result.levelUp)
        assertEquals(OnboardingLevel.NAVIGATOR, result.newLevel)
    }

    @Test
    fun `visit response maps unknown level to null`() {
        val result = PracticeVisitResponse(levelUp = true, newLevel = "NEW_LEVEL").toDomain()

        assertNull(result.newLevel)
    }

    @Test
    fun `skip reason options are sorted and offset-less timestamp helper remains usable`() {
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
        assertEquals(parseServerTimestamp("2026-08-10T10:47:33.996642"), parseServerTimestamp("2026-08-10T10:47:33.996642"))
    }
}
