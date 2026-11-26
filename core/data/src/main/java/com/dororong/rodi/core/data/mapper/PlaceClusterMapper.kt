package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.place.PlaceBoundsResponse
import com.dororong.rodi.core.data.source.remote.model.place.PlaceClusterLevelResponse
import com.dororong.rodi.core.data.source.remote.model.place.PlaceMarkerClusterResponse
import com.dororong.rodi.core.data.source.remote.model.place.PlaceMarkerClustersResponse
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceBounds
import com.dororong.rodi.core.domain.model.place.PlaceClusterLevel
import com.dororong.rodi.core.domain.model.place.PlaceMarkerCluster
import com.dororong.rodi.core.domain.model.place.PlaceMarkerClusters

fun PlaceMarkerClustersResponse.toDomain(): PlaceMarkerClusters {
    val placeIds = places.map { it.id }.toSet()
    require(placeIds.size == places.size) { "Duplicate cluster place ids" }
    require(levels.isNotEmpty()) { "Missing cluster levels" }
    require(levels.first().maxLngSpan == null && levels.last().minLngSpan == 0.0) {
        "Incomplete cluster level coverage"
    }
    require(levels.zipWithNext().all { (wider, narrower) -> wider.minLngSpan == narrower.maxLngSpan }) {
        "Cluster level range gap"
    }
    levels.forEach { level ->
        require(level.placeIds.distinct().size == level.placeIds.size) { "Duplicate level place ids" }
        require(level.placeIds.all(placeIds::contains)) { "Unknown level place id" }
        require(level.clusters.sumOf { it.count.toLong() } + level.placeIds.size == places.size.toLong()) {
            "Cluster level place count mismatch"
        }
    }
    return PlaceMarkerClusters(
        places = places.map { it.toDomain() },
        levels = levels.map(PlaceClusterLevelResponse::toDomain),
    )
}

private fun PlaceClusterLevelResponse.toDomain(): PlaceClusterLevel {
    require(minLngSpan.isFinite() && minLngSpan >= 0) { "Invalid minimum longitude span" }
    require(maxLngSpan == null || maxLngSpan.isFinite() && maxLngSpan > minLngSpan) {
        "Invalid maximum longitude span"
    }
    return PlaceClusterLevel(
        minLngSpan = minLngSpan,
        maxLngSpan = maxLngSpan,
        clusters = clusters.map(PlaceMarkerClusterResponse::toDomain),
        placeIds = placeIds,
    )
}

private fun PlaceMarkerClusterResponse.toDomain(): PlaceMarkerCluster {
    require(count >= 2) { "Invalid cluster count" }
    return PlaceMarkerCluster(
        point = point(lat, lng),
        count = count,
        bounds = bounds.toDomain(),
    )
}

private fun PlaceBoundsResponse.toDomain(): PlaceBounds {
    require(swLat <= neLat && swLng <= neLng) { "Invalid cluster bounds" }
    return PlaceBounds(
        southWest = point(swLat, swLng),
        northEast = point(neLat, neLng),
    )
}

private fun point(lat: Double, lng: Double): GeoPoint {
    require(lat.isFinite() && lat in -85.0..85.0) { "Invalid cluster latitude" }
    require(lng.isFinite() && lng in -180.0..180.0) { "Invalid cluster longitude" }
    return GeoPoint(lat, lng)
}
