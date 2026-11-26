package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceBounds
import com.dororong.rodi.core.domain.model.place.PlaceCoordinate
import com.dororong.rodi.core.domain.model.place.PlaceMarkerClusters

internal sealed interface BrowseMapMarker {
    data class Cluster(val point: GeoPoint, val count: Int, val bounds: PlaceBounds) : BrowseMapMarker
    data class Place(val place: PlaceCoordinate) : BrowseMapMarker
}

internal fun PlaceMarkerClusters.markersForLongitudeSpan(longitudeSpan: Double): List<BrowseMapMarker> {
    if (!longitudeSpan.isFinite() || longitudeSpan < 0) return emptyList()
    val level = levels.firstOrNull {
        longitudeSpan >= it.minLngSpan && (it.maxLngSpan?.let { upper -> longitudeSpan < upper } ?: true)
    } ?: return emptyList()
    val placesById = places.associateBy(PlaceCoordinate::id)
    return level.clusters.map { BrowseMapMarker.Cluster(it.point, it.count, it.bounds) } +
        level.placeIds.mapNotNull { id -> placesById[id]?.let(BrowseMapMarker::Place) }
}
