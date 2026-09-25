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
    fun `an unfinished measured session keeps tracking its own place`() {
        assertTrue(measured.isMeasuringAt(7L))
        assertTrue(measured.copy(isArrivalConfirmed = true).isMeasuringAt(7L))
    }

    @Test
    fun `tracking ends when the practice is gone, finished, replaced, or route only`() {
        assertFalse(null.isMeasuringAt(7L))
        assertFalse(measured.copy(isCompleted = true).isMeasuringAt(7L))
        assertFalse(measured.copy(placeId = 8L).isMeasuringAt(7L))
        assertFalse(measured.copy(isMeasured = false).isMeasuringAt(7L))
    }
}
