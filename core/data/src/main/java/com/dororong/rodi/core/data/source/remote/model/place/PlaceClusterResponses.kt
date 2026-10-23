package com.dororong.rodi.core.data.source.remote.model.place

import kotlinx.serialization.Serializable

@Serializable
data class PlaceMarkerClustersResponse(
    val places: List<PlaceCoordinateResponse>,
    val levels: List<PlaceClusterLevelResponse>,
)

@Serializable
data class PlaceClusterLevelResponse(
    val minLngSpan: Double,
    val maxLngSpan: Double?,
    val clusters: List<PlaceMarkerClusterResponse>,
    val placeIds: List<Long>,
)

@Serializable
data class PlaceMarkerClusterResponse(
    val lat: Double,
    val lng: Double,
    val count: Int,
    val bounds: PlaceBoundsResponse,
)

@Serializable
data class PlaceBoundsResponse(
    val swLat: Double,
    val swLng: Double,
    val neLat: Double,
    val neLng: Double,
)
