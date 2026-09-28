package com.dororong.rodi.core.domain.model.practice

import com.dororong.rodi.core.domain.model.place.PlaceType
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ActivePracticeSessionTest {
    private val measured = ActivePracticeSession(
        placeId = 7L,
        placeName = "코스",
        placeType = PlaceType.COURSE,
        startedAt = Instant.parse("2026-09-25T00:00:00Z"),
    )

    @Test
    fun `끝나지 않은 측정 세션은 자기 장소를 계속 추적한다`() {
        assertTrue(measured.isMeasuringAt(7L))
        assertTrue(measured.copy(isArrivalConfirmed = true).isMeasuringAt(7L))
    }

    @Test
    fun `연습이 없거나 끝났거나 바뀌었거나 경로만 안내하면 추적을 끝낸다`() {
        assertFalse(null.isMeasuringAt(7L))
        assertFalse(measured.copy(isCompleted = true).isMeasuringAt(7L))
        assertFalse(measured.copy(placeId = 8L).isMeasuringAt(7L))
        assertFalse(measured.copy(isMeasured = false).isMeasuringAt(7L))
    }
}
