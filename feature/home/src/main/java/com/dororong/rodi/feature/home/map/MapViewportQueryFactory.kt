package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.GeoPoint
import com.dororong.rodi.core.domain.MapViewportQuery

object MapViewportQueryFactory {
    fun fromCorners(
        northEast: GeoPoint,
        southWest: GeoPoint,
        zoomLevel: Int,
    ): MapViewportQuery = MapViewportQuery(
        northEast = northEast,
        southWest = southWest,
        zoomLevel = zoomLevel,
    )
}
