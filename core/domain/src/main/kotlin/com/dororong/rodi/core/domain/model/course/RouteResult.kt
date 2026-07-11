package com.dororong.rodi.core.domain.model.course

data class GeoPoint(val lat: Double, val lng: Double)

data class RouteResult(
    val points: List<GeoPoint>,
    val isRealRoute: Boolean,
    val totalDistanceMeters: Int = 0,
    val snappedPoints: List<GeoPoint> = emptyList(),
)
