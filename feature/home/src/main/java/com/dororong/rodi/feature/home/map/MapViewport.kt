package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.model.course.GeoPoint
import androidx.compose.ui.unit.IntSize
import com.kakao.vectormap.KakaoMap
import kotlin.math.hypot

data class MapViewport(
    val northEast: GeoPoint,
    val southWest: GeoPoint,
)

object ViewportSearchThreshold {
    fun isExceeded(
        searchedViewport: MapViewport,
        currentViewport: MapViewport,
    ): Boolean {
        val latitudeSpan = searchedViewport.northEast.lat - searchedViewport.southWest.lat
        val longitudeSpan = searchedViewport.northEast.lng - searchedViewport.southWest.lng
        if (latitudeSpan <= 0.0 || longitudeSpan <= 0.0) return false

        val searchedCenterLatitude = (searchedViewport.northEast.lat + searchedViewport.southWest.lat) / 2
        val searchedCenterLongitude = (searchedViewport.northEast.lng + searchedViewport.southWest.lng) / 2
        val currentCenterLatitude = (currentViewport.northEast.lat + currentViewport.southWest.lat) / 2
        val currentCenterLongitude = (currentViewport.northEast.lng + currentViewport.southWest.lng) / 2
        val horizontalDistance = (currentCenterLongitude - searchedCenterLongitude) / (longitudeSpan / 2)
        val verticalDistance = (currentCenterLatitude - searchedCenterLatitude) / (latitudeSpan / 2)
        return hypot(horizontalDistance, verticalDistance) >= 1.0
    }
}

fun KakaoMap.viewportOrNull(size: IntSize): MapViewport? {
    if (size == IntSize.Zero) return null
    val northEast = fromScreenPoint(size.width, 0) ?: return null
    val southWest = fromScreenPoint(0, size.height) ?: return null
    return MapViewport(
        northEast = GeoPoint(northEast.latitude, northEast.longitude),
        southWest = GeoPoint(southWest.latitude, southWest.longitude),
    )
}
