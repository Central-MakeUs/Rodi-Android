package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceCoordinate
import com.dororong.rodi.core.domain.model.place.PlaceType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MapMarkerGeometryTest {
    @Test
    fun `cluster body and tail share one seam coordinate`() {
        val geometry = clusterSilhouetteGeometry(bodyBottom = 42f)

        assertEquals(geometry.bodyBottom, geometry.tailTop)
    }

    @Test
    fun `current location marker anchors map coordinates at the circle center`() {
        assertEquals(18f / 28f, currentLocationMarkerAnchorY())
    }

    @Test
    fun `individual markers keep one place when sample and server coordinates are identical`() {
        val places = listOf(
            parking(id = -1L, point = GeoPoint(37.558, 127.042)),
            parking(id = 10L, point = GeoPoint(37.558, 127.042)),
            parking(id = 11L, point = GeoPoint(37.559, 127.042)),
        )

        assertEquals(listOf(-1L, 11L), places.distinctMarkerPlaces().map(PlaceCoordinate::id))
    }

    private fun parking(id: Long, point: GeoPoint) = PlaceCoordinate(
        id = id,
        type = PlaceType.PARKING,
        name = "주차장 $id",
        address = "서울",
        point = point,
    )
}
