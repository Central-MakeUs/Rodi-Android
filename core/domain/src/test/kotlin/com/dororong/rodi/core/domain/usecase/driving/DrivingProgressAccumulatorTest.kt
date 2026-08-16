package com.dororong.rodi.core.domain.usecase.driving

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.driving.DrivingLocationSample
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DrivingProgressAccumulatorTest {
    @Test
    fun ignoresGpsJumpsAndInaccurateSamples() {
        val accumulator = DrivingProgressAccumulator()

        assertEquals(0.0, accumulator.add(sample(37.5, 1_000)))
        val moved = accumulator.add(sample(37.5001, 2_000))
        val noiseIgnored = accumulator.add(sample(37.500105, 3_000))
        val jumpIgnored = accumulator.add(sample(37.51, 4_000))
        val inaccurateIgnored = accumulator.add(sample(37.5002, 5_000, 80f))

        assertTrue(moved in 10.0..12.5)
        assertEquals(moved, noiseIgnored)
        assertEquals(moved, jumpIgnored)
        assertEquals(moved, inaccurateIgnored)
    }

    private fun sample(
        latitude: Double,
        time: Long,
        accuracyMeters: Float = 10f,
    ) = DrivingLocationSample(
        point = GeoPoint(latitude, 127.0),
        accuracyMeters = accuracyMeters,
        elapsedRealtimeMillis = time,
    )
}
