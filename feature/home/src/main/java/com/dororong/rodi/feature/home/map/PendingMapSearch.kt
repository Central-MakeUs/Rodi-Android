package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.model.course.GeoPoint
import kotlin.math.abs

internal enum class MapSearchMoveReason {
    INITIAL_LOCATION,
    CURRENT_LOCATION,
    CLUSTER,
    REGION,
}

internal data class PendingMapSearch(
    val generation: Long,
    val target: GeoPoint,
    val targetZoom: Int?,
    val reason: MapSearchMoveReason,
    val requiredBounds: MapViewport? = null,
)

internal object PendingMapSearchMatcher {
    private const val TARGET_TOLERANCE_RATIO = 0.05

    fun matches(
        pending: PendingMapSearch,
        viewport: MapViewport,
        zoomLevel: Int,
    ): Boolean {
        pending.targetZoom?.let { if (zoomLevel != it) return false }

        pending.requiredBounds?.let { bounds ->
            return viewport.contains(bounds.northEast) && viewport.contains(bounds.southWest)
        }

        val latitudeSpan = viewport.northEast.lat - viewport.southWest.lat
        val longitudeSpan = viewport.northEast.lng - viewport.southWest.lng
        if (latitudeSpan <= 0.0 || longitudeSpan <= 0.0) return false

        val centerLatitude = (viewport.northEast.lat + viewport.southWest.lat) / 2.0
        val centerLongitude = (viewport.northEast.lng + viewport.southWest.lng) / 2.0
        return abs(centerLatitude - pending.target.lat) <= latitudeSpan * TARGET_TOLERANCE_RATIO &&
            abs(centerLongitude - pending.target.lng) <= longitudeSpan * TARGET_TOLERANCE_RATIO
    }
}
