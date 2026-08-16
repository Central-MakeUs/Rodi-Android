package com.dororong.rodi.core.domain.usecase.driving

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.driving.DrivingLocationSample
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DrivingProgressAccumulatorTest {
    @Test
    fun `adds plausible movement and ignores noise jump and inaccurate samples`() {
        val accumulator = DrivingProgressAccumulator()

        assertEquals(0.0, accumulator.add(sample(37.5, 127.0, time = 1_000)))
        val moved = accumulator.add(sample(37.5001, 127.0, time = 2_000))
        val noiseIgnored = accumulator.add(sample(37.500105, 127.0, time = 3_000))
        val jumpIgnored = accumulator.add(sample(37.51, 127.0, time = 4_000))
        val inaccurateIgnored = accumulator.add(
            sample(37.5002, 127.0, time = 5_000, accuracy = 80f),
        )

        assertTrue(moved in 10.0..12.5)
        assertEquals(moved, noiseIgnored)
        assertEquals(moved, jumpIgnored)
        assertEquals(moved, inaccurateIgnored)
    }

    private fun sample(
        latitude: Double,
        longitude: Double,
        time: Long,
        accuracy: Float = 10f,
    ) = DrivingLocationSample(
        point = GeoPoint(latitude, longitude),
        accuracyMeters = accuracy,
        elapsedRealtimeMillis = time,
    )
}
