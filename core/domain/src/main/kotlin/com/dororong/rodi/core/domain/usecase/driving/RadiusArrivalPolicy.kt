package com.dororong.rodi.core.domain.usecase.driving

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.driving.DrivingLocationSample
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class ArrivalDecision(
    val distanceToDestinationMeters: Double,
    val consecutiveMatches: Int,
    val hasArrived: Boolean,
)

class RadiusArrivalPolicy(
    private val radiusMeters: Double = 180.0,
    private val requiredConsecutiveSamples: Int = 2,
    private val maxAccuracyMeters: Float = 50f,
) {
    init {
        require(radiusMeters > 0)
        require(requiredConsecutiveSamples > 0)
        require(maxAccuracyMeters > 0)
    }

    fun evaluate(
        sample: DrivingLocationSample,
        destination: GeoPoint,
        previousConsecutiveMatches: Int,
    ): ArrivalDecision {
        val distance = sample.point.distanceTo(destination)
        val matches = if (
            sample.accuracyMeters in 0f..maxAccuracyMeters &&
            distance <= radiusMeters
        ) {
            previousConsecutiveMatches + 1
        } else {
            0
        }
        return ArrivalDecision(
            distanceToDestinationMeters = distance,
            consecutiveMatches = matches,
            hasArrived = matches >= requiredConsecutiveSamples,
        )
    }
}

fun GeoPoint.distanceTo(other: GeoPoint): Double {
    val earthRadiusMeters = 6_371_000.0
    val latitudeDelta = Math.toRadians(other.lat - lat)
    val longitudeDelta = Math.toRadians(other.lng - lng)
    val startLatitude = Math.toRadians(lat)
    val endLatitude = Math.toRadians(other.lat)
    val haversine = sin(latitudeDelta / 2).let { it * it } +
        cos(startLatitude) * cos(endLatitude) *
        sin(longitudeDelta / 2).let { it * it }
    return 2 * earthRadiusMeters * asin(sqrt(haversine))
}
