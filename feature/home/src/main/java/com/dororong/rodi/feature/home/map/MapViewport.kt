package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.model.course.GeoPoint
import androidx.compose.ui.unit.IntSize
import com.kakao.vectormap.KakaoMap

data class MapViewport(
    val northEast: GeoPoint,
    val southWest: GeoPoint,
) {
    fun contains(point: GeoPoint): Boolean =
        point.lat in southWest.lat..northEast.lat && point.lng in southWest.lng..northEast.lng
}

fun List<GeoPoint>.boundsOrNull(): MapViewport? {
    if (isEmpty()) return null
    return MapViewport(
        northEast = GeoPoint(maxOf(GeoPoint::lat), maxOf(GeoPoint::lng)),
        southWest = GeoPoint(minOf(GeoPoint::lat), minOf(GeoPoint::lng)),
    )
}

object InitialViewportSearchPolicy {
    fun canDispatch(
        locationState: InitialLocationState,
        hasCurrentLocation: Boolean,
        hasCenteredInitialLocation: Boolean,
        isInitialLocationCameraMovePending: Boolean,
    ): Boolean = locationState == InitialLocationState.Ready &&
        hasCurrentLocation &&
        !isInitialLocationCameraMovePending &&
        hasCenteredInitialLocation
}

enum class InitialLocationState { Pending, Ready, Unavailable }

fun KakaoMap.viewportOrNull(size: IntSize): MapViewport? {
    if (size == IntSize.Zero) return null
    val northEast = fromScreenPoint(size.width, 0) ?: return null
    val southWest = fromScreenPoint(0, size.height) ?: return null
    return MapViewport(
        northEast = GeoPoint(northEast.latitude, northEast.longitude),
        southWest = GeoPoint(southWest.latitude, southWest.longitude),
    )
}
