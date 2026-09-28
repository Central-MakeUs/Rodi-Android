package com.dororong.rodi.core.domain.usecase.driving

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.driving.DrivingLocationSample
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RadiusArrivalPolicyTest {
    private val destination = GeoPoint(37.5, 127.0)

    @Test
    fun `반경 안의 정확한 위치가 두 번 연속이어야 도착으로 본다`() {
        val policy = RadiusArrivalPolicy()
        val first = policy.evaluate(sample(37.5005), destination, 0)
        val second = policy.evaluate(sample(37.5004), destination, first.consecutiveMatches)

        assertFalse(first.hasArrived)
        assertTrue(second.hasArrived)
    }

    @Test
    fun `반경 밖이거나 부정확한 위치가 들어오면 연속 횟수를 초기화한다`() {
        val policy = RadiusArrivalPolicy()
        val first = policy.evaluate(sample(37.5005), destination, 0)
        val outside = policy.evaluate(sample(37.502), destination, first.consecutiveMatches)
        val inaccurate = policy.evaluate(
            sample(37.5001, accuracyMeters = 80f),
            destination,
            outside.consecutiveMatches,
        )

        assertFalse(outside.hasArrived)
        assertFalse(inaccurate.hasArrived)
        assertTrue(inaccurate.consecutiveMatches == 0)
    }

    @Test
    fun `반경과 필요한 연속 횟수를 설정할 수 있다`() {
        val immediate = RadiusArrivalPolicy(radiusMeters = 150.0, requiredConsecutiveSamples = 1)
        val strict = RadiusArrivalPolicy(radiusMeters = 150.0, requiredConsecutiveSamples = 3)
        val location = sample(37.5009)

        assertTrue(immediate.evaluate(location, destination, 0).hasArrived)
        assertFalse(strict.evaluate(location, destination, 1).hasArrived)
        assertTrue(strict.evaluate(location, destination, 2).hasArrived)
    }

    private fun sample(
        latitude: Double,
        accuracyMeters: Float = 10f,
    ) = DrivingLocationSample(
        point = GeoPoint(latitude, 127.0),
        accuracyMeters = accuracyMeters,
        elapsedRealtimeMillis = 1L,
    )
}
