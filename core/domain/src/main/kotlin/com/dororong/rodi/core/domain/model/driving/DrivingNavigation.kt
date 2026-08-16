package com.dororong.rodi.core.domain.model.driving

data class DrivingNavigation(
    val placeId: Long,
    val launchedAtEpochMillis: Long,
    val measurementStarted: Boolean,
)
