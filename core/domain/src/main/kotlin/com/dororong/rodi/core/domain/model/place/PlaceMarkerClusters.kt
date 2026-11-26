package com.dororong.rodi.core.domain.model.place

import com.dororong.rodi.core.domain.model.course.GeoPoint

data class PlaceMarkerClusters(
    val places: List<PlaceCoordinate>,
    val levels: List<PlaceClusterLevel>,
)

data class PlaceClusterLevel(
    val minLngSpan: Double,
    val maxLngSpan: Double?,
    val clusters: List<PlaceMarkerCluster>,
    val placeIds: List<Long>,
)

data class PlaceMarkerCluster(
    val point: GeoPoint,
    val count: Int,
    val bounds: PlaceBounds,
)

data class PlaceBounds(
    val southWest: GeoPoint,
    val northEast: GeoPoint,
)
