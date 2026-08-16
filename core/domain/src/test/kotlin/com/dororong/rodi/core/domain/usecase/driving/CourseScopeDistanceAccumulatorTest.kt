package com.dororong.rodi.core.domain.usecase.driving

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.driving.DrivingLocationSample
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CourseScopeDistanceAccumulatorTest {
    private val route = listOf(
        GeoPoint(37.5000, 127.0000),
        GeoPoint(37.5050, 127.0000),
    )

    @Test
    fun `counts only movement while both samples are inside the route scope`() {
        val accumulator = CourseScopeDistanceAccumulator(
            route = route,
            requiredDistanceMeters = 1_000,
        )

        val outside = accumulator.add(sample(37.5000, 127.0030, 1_000))
        val inside = accumulator.add(sample(37.5000, 127.0000, 2_000))
        val moved = accumulator.add(sample(37.5010, 127.0000, 3_000))

        assertFalse(outside.isInCourseScope)
        assertTrue(inside.isInCourseScope)
        assertTrue(moved.isInCourseScope)
        assertTrue(moved.recognizedDistanceMeters in 100.0..115.0)
    }

    @Test
    fun `leaving the scope resets the distance bridge`() {
        val accumulator = CourseScopeDistanceAccumulator(
            route = route,
            requiredDistanceMeters = 1_000,
        )

        accumulator.add(sample(37.5000, 127.0000, 1_000))
        val firstMove = accumulator.add(sample(37.5010, 127.0000, 2_000))
        accumulator.add(sample(37.5010, 127.0030, 3_000))
        val reenter = accumulator.add(sample(37.5020, 127.0000, 4_000))

        assertTrue(firstMove.recognizedDistanceMeters > 0)
        assertEquals(firstMove.recognizedDistanceMeters, reenter.recognizedDistanceMeters)
    }

    @Test
    fun `inaccurate samples and gps jumps do not contribute`() {
        val accumulator = CourseScopeDistanceAccumulator(
            route = route,
            requiredDistanceMeters = 100,
        )

        accumulator.add(sample(37.5000, 127.0000, 1_000))
        val accurate = accumulator.add(sample(37.5005, 127.0000, 2_000))
        val jump = accumulator.add(sample(37.5040, 127.0000, 3_000))
        val afterJump = accumulator.add(sample(37.5045, 127.0000, 4_000))
        val inaccurate = accumulator.add(sample(37.5050, 127.0000, 5_000, accuracy = 80f))

        assertTrue(accurate.recognizedDistanceMeters in 50.0..60.0)
        assertEquals(accurate.recognizedDistanceMeters, inaccurate.recognizedDistanceMeters)
        assertEquals(accurate.recognizedDistanceMeters, jump.recognizedDistanceMeters)
        assertEquals(accurate.recognizedDistanceMeters, afterJump.recognizedDistanceMeters)
    }

    @Test
    fun `required distance is forty percent capped at five kilometers`() {
        assertEquals(520, requiredCertifiedDistanceMeters(1_300))
        assertEquals(1_520, requiredCertifiedDistanceMeters(3_800))
        assertEquals(5_000, requiredCertifiedDistanceMeters(14_800))
        assertEquals(5_000, requiredCertifiedDistanceMeters(40_000))
    }

    @Test
    fun `progress is capped and completion is idempotent`() {
        val accumulator = CourseScopeDistanceAccumulator(
            route = route,
            requiredDistanceMeters = 100,
            maxMovementMeters = 300.0,
        )
        accumulator.add(sample(37.5000, 127.0000, 1_000))
        val completed = accumulator.add(sample(37.5020, 127.0000, 2_000))
        val afterCompletion = accumulator.add(sample(37.5030, 127.0000, 3_000))

        assertTrue(completed.isComplete)
        assertEquals(100, completed.progressPercent)
        assertTrue(afterCompletion.isComplete)
        assertEquals(100, afterCompletion.progressPercent)
    }

    private fun sample(
        latitude: Double,
        longitude: Double,
        elapsedRealtimeMillis: Long,
        accuracy: Float = 10f,
    ) = DrivingLocationSample(
        point = GeoPoint(latitude, longitude),
        accuracyMeters = accuracy,
        elapsedRealtimeMillis = elapsedRealtimeMillis,
    )
}
