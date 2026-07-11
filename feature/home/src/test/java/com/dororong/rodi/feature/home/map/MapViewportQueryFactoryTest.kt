package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.GeoPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MapViewportQueryFactoryTest {
    @Test
    fun `keeps top right as north east and bottom left as south west`() {
        val northEast = GeoPoint(38.0, 128.0)
        val southWest = GeoPoint(37.0, 127.0)

        val query = MapViewportQueryFactory.fromCorners(northEast, southWest, 13)

        assertEquals(northEast, query.northEast)
        assertEquals(southWest, query.southWest)
        assertEquals(13, query.zoomLevel)
    }
}
