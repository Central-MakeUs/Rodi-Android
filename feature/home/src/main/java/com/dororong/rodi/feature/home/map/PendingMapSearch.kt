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

    /**
     * 시트가 지도를 가리면 지도에 아래 여백을 주므로 카메라 중심이 화면 범위의 한가운데보다 위에 온다.
     * 그래서 카메라가 알려 준 중심이 있으면 그것과 비교하고, 없을 때만 화면 범위의 한가운데를 쓴다.
     */
    fun matches(
        pending: PendingMapSearch,
        viewport: MapViewport,
        zoomLevel: Int,
        cameraCenter: GeoPoint? = null,
    ): Boolean {
        pending.targetZoom?.let { if (zoomLevel != it) return false }

        pending.requiredBounds?.let { bounds ->
            return viewport.contains(bounds.northEast) && viewport.contains(bounds.southWest)
        }

        val latitudeSpan = viewport.northEast.lat - viewport.southWest.lat
        val longitudeSpan = viewport.northEast.lng - viewport.southWest.lng
        if (latitudeSpan <= 0.0 || longitudeSpan <= 0.0) return false

        val centerLatitude = cameraCenter?.lat ?: ((viewport.northEast.lat + viewport.southWest.lat) / 2.0)
        val centerLongitude = cameraCenter?.lng ?: ((viewport.northEast.lng + viewport.southWest.lng) / 2.0)
        return abs(centerLatitude - pending.target.lat) <= latitudeSpan * TARGET_TOLERANCE_RATIO &&
            abs(centerLongitude - pending.target.lng) <= longitudeSpan * TARGET_TOLERANCE_RATIO
    }
}
