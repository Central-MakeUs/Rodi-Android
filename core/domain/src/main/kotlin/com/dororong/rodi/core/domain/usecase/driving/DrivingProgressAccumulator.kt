package com.dororong.rodi.core.domain.usecase.driving

import com.dororong.rodi.core.domain.model.driving.DrivingLocationSample

class DrivingProgressAccumulator(
    private val maxAccuracyMeters: Float = 50f,
    private val minMovementMeters: Double = 3.0,
    private val maxMovementMeters: Double = 200.0,
) {
    private var previousSample: DrivingLocationSample? = null

    var traveledDistanceMeters: Double = 0.0
        private set

    fun add(sample: DrivingLocationSample): Double {
        if (sample.accuracyMeters !in 0f..maxAccuracyMeters) return traveledDistanceMeters
        val previous = previousSample
        if (previous == null || sample.elapsedRealtimeMillis <= previous.elapsedRealtimeMillis) {
            if (previous == null) previousSample = sample
            return traveledDistanceMeters
        }
        previousSample = sample
        val movement = previous.point.distanceTo(sample.point)
        if (movement in minMovementMeters..maxMovementMeters) {
            traveledDistanceMeters += movement
        }
        return traveledDistanceMeters
    }
}
