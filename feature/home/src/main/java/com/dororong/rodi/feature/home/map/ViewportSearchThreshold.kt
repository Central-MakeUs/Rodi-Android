package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.MapViewportQuery
import kotlin.math.hypot

object ViewportSearchThreshold {
    fun isExceeded(
        searchedViewport: MapViewportQuery,
        currentViewport: MapViewportQuery,
    ): Boolean {
        val searchedLatSpan = searchedViewport.northEast.lat - searchedViewport.southWest.lat
        val searchedLngSpan = searchedViewport.northEast.lng - searchedViewport.southWest.lng
        if (searchedLatSpan <= 0.0 || searchedLngSpan <= 0.0) return false

        val searchedCenterLat = (searchedViewport.northEast.lat + searchedViewport.southWest.lat) / 2
        val searchedCenterLng = (searchedViewport.northEast.lng + searchedViewport.southWest.lng) / 2
        val currentCenterLat = (currentViewport.northEast.lat + currentViewport.southWest.lat) / 2
        val currentCenterLng = (currentViewport.northEast.lng + currentViewport.southWest.lng) / 2

        val horizontalDistance = (currentCenterLng - searchedCenterLng) / (searchedLngSpan / 2)
        val verticalDistance = (currentCenterLat - searchedCenterLat) / (searchedLatSpan / 2)
        return hypot(horizontalDistance, verticalDistance) >= 1.0
    }
}
