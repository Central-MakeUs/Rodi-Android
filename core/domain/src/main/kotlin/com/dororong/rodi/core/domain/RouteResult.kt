package com.dororong.rodi.core.domain

data class GeoPoint(val lat: Double, val lng: Double)

data class MapViewportQuery(
    val northEast: GeoPoint,
    val southWest: GeoPoint,
    val zoomLevel: Int,
)

data class RouteResult(
    val points: List<GeoPoint>,
    val isRealRoute: Boolean,
    val totalDistanceMeters: Int = 0,
    val snappedPoints: List<GeoPoint> = emptyList(),
)
