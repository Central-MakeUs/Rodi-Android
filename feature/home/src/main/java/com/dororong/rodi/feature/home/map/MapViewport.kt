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

data class MapScreenRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val isValid: Boolean get() = right > left && bottom > top

    fun contains(x: Int, y: Int): Boolean =
        x >= left && x < right && y >= top && y < bottom
}

data class VisibleMapViewport(
    val geo: MapViewport,
    val screen: MapScreenRect,
)

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
    return visibleViewportOrNull(size)?.geo
}

fun KakaoMap.visibleViewportOrNull(size: IntSize): VisibleMapViewport? {
    val sdkViewport = viewport
    val screen = MapScreenRect(
        left = sdkViewport.left,
        top = sdkViewport.top,
        right = sdkViewport.right,
        bottom = sdkViewport.bottom,
    ).takeIf(MapScreenRect::isValid) ?: MapScreenRect(
        left = 0,
        top = 0,
        right = size.width,
        bottom = size.height,
    ).takeIf(MapScreenRect::isValid) ?: return null
    val northEast = fromScreenPoint(screen.right - 1, screen.top) ?: return null
    val southWest = fromScreenPoint(screen.left, screen.bottom - 1) ?: return null
    return VisibleMapViewport(
        geo = MapViewport(
            northEast = GeoPoint(northEast.latitude, northEast.longitude),
            southWest = GeoPoint(southWest.latitude, southWest.longitude),
        ),
        screen = screen,
    )
}
