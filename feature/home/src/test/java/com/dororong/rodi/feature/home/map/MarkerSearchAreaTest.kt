package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MarkerSearchAreaTest {
    @Test
    fun `마커 조회 범위를 화면 한 칸씩 사방으로 넓힌다`() {
        val query = PlaceViewportQuery(GeoPoint(37.0, 127.0), GeoPoint(37.1, 127.2), GeoPoint(37.05, 127.1))

        val expanded = query.expandedForMarkers()

        assertEquals(36.9, expanded.southWest.lat, 1e-9)
        assertEquals(126.8, expanded.southWest.lng, 1e-9)
        assertEquals(37.2, expanded.northEast.lat, 1e-9)
        assertEquals(127.4, expanded.northEast.lng, 1e-9)
        assertEquals(query.origin, expanded.origin)
    }

    @Test
    fun `넓힌 범위는 서버가 받는 위경도 한계를 넘지 않는다`() {
        val query = PlaceViewportQuery(GeoPoint(80.0, 170.0), GeoPoint(84.0, 179.0), GeoPoint(82.0, 175.0))

        val expanded = query.expandedForMarkers()

        assertEquals(85.0, expanded.northEast.lat, 1e-9)
        assertEquals(180.0, expanded.northEast.lng, 1e-9)
    }
}
