package com.dororong.rodi.core.domain.usecase.driving

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.driving.DrivingLocationSample
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RadiusArrivalPolicyTest {
    private val destination = GeoPoint(37.5, 127.0)

    @Test
    fun `arrives after two accurate samples within one hundred meters`() {
        val policy = RadiusArrivalPolicy()

        val first = policy.evaluate(sample(37.5005, 127.0), destination, 0)
        val second = policy.evaluate(sample(37.5004, 127.0), destination, first.consecutiveMatches)

        assertFalse(first.hasArrived)
        assertTrue(second.hasArrived)
    }

    @Test
    fun `outside sample resets consecutive matches`() {
        val policy = RadiusArrivalPolicy()

        val first = policy.evaluate(sample(37.5005, 127.0), destination, 0)
        val outside = policy.evaluate(sample(37.502, 127.0), destination, first.consecutiveMatches)
        val nextInside = policy.evaluate(sample(37.5005, 127.0), destination, outside.consecutiveMatches)

        assertFalse(outside.hasArrived)
        assertFalse(nextInside.hasArrived)
    }

    @Test
    fun `inaccurate sample does not count as arrival`() {
        val policy = RadiusArrivalPolicy()

        val decision = policy.evaluate(
            sample = sample(37.5001, 127.0, accuracyMeters = 80f),
            destination = destination,
            previousConsecutiveMatches = 1,
        )

        assertFalse(decision.hasArrived)
        assertTrue(decision.consecutiveMatches == 0)
    }

    @Test
    fun `radius and consecutive sample count are configurable`() {
        val immediate = RadiusArrivalPolicy(radiusMeters = 150.0, requiredConsecutiveSamples = 1)
        val strict = RadiusArrivalPolicy(radiusMeters = 150.0, requiredConsecutiveSamples = 3)
        val location = sample(37.5009, 127.0)

        assertTrue(immediate.evaluate(location, destination, 0).hasArrived)
        assertFalse(strict.evaluate(location, destination, 1).hasArrived)
        assertTrue(strict.evaluate(location, destination, 2).hasArrived)
    }

    private fun sample(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float = 10f,
    ) = DrivingLocationSample(
        point = GeoPoint(latitude, longitude),
        accuracyMeters = accuracyMeters,
        elapsedRealtimeMillis = 1L,
    )
}
