package com.dororong.rodi.core.domain.model.driving

import com.dororong.rodi.core.domain.model.course.GeoPoint

enum class DrivingSessionStatus {
    ACTIVE,
    ARRIVED,
}

data class DrivingSession(
    val id: String,
    val placeId: Long,
    val placeName: String,
    val destination: GeoPoint,
    val plannedDistanceMeters: Int?,
    val startedAtEpochMillis: Long,
    val arrivedAtEpochMillis: Long?,
    val traveledDistanceMeters: Double,
    val status: DrivingSessionStatus,
    val isArrivalNoticePending: Boolean,
    val courseRoute: List<GeoPoint> = emptyList(),
    val requiredDistanceMeters: Int? = null,
)

data class DrivingLocationSample(
    val point: GeoPoint,
    val accuracyMeters: Float,
    val elapsedRealtimeMillis: Long,
)
