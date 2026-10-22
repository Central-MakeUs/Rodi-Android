package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceBounds
import com.dororong.rodi.core.domain.model.place.PlaceClusterLevel
import com.dororong.rodi.core.domain.model.place.PlaceCoordinate
import com.dororong.rodi.core.domain.model.place.PlaceMarkerCluster
import com.dororong.rodi.core.domain.model.place.PlaceMarkerClusters
import com.dororong.rodi.core.domain.model.place.PlaceType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ServerMapMarkersTest {
    private val bounds = PlaceBounds(GeoPoint(37.0, 126.0), GeoPoint(38.0, 127.0))
    private val point = GeoPoint(37.5, 126.5)

    @Test
    fun `경도 범위가 최소값 이상이고 최대값 미만인 단계만 선택한다`() {
        val response = PlaceMarkerClusters(
            places = emptyList(),
            levels = listOf(level(0.0, 0.1, count = 2), level(0.1, 1.0, count = 3)),
        )

        assertEquals(listOf(BrowseMapMarker.Cluster(point, 2, bounds)), response.markersForLongitudeSpan(0.0))
        assertEquals(listOf(BrowseMapMarker.Cluster(point, 2, bounds)), response.markersForLongitudeSpan(0.0999))
        assertEquals(listOf(BrowseMapMarker.Cluster(point, 3, bounds)), response.markersForLongitudeSpan(0.1))
        assertTrue(response.markersForLongitudeSpan(1.0).isEmpty())
    }

    @Test
    fun `상한이 null인 단계는 최소값부터 넓은 경도 범위를 처리한다`() {
        val response = PlaceMarkerClusters(emptyList(), listOf(level(1.0, null, count = 7)))

        assertTrue(response.markersForLongitudeSpan(0.9999).isEmpty())
        assertEquals(listOf(BrowseMapMarker.Cluster(point, 7, bounds)), response.markersForLongitudeSpan(1.0))
        assertEquals(listOf(BrowseMapMarker.Cluster(point, 7, bounds)), response.markersForLongitudeSpan(360.0))
    }

    @Test
    fun `서버 클러스터의 중심과 개수와 경계를 그대로 유지한다`() {
        val suppliedBounds = PlaceBounds(GeoPoint(35.0, 128.0), GeoPoint(35.3, 128.4))
        val suppliedPoint = GeoPoint(35.1, 128.2)
        val response = PlaceMarkerClusters(
            emptyList(),
            listOf(PlaceClusterLevel(0.0, null, listOf(PlaceMarkerCluster(suppliedPoint, 23, suppliedBounds)), emptyList())),
        )

        assertEquals(
            listOf(BrowseMapMarker.Cluster(suppliedPoint, 23, suppliedBounds)),
            response.markersForLongitudeSpan(2.0),
        )
    }

    @Test
    fun `개별 마커는 현재 단계가 지정한 id만 서버 순서대로 가져온다`() {
        val first = place(1)
        val second = place(2)
        val response = PlaceMarkerClusters(
            places = listOf(first, second, place(3)),
            levels = listOf(PlaceClusterLevel(0.0, null, emptyList(), listOf(2, 1))),
        )

        assertEquals(
            listOf(BrowseMapMarker.Place(second), BrowseMapMarker.Place(first)),
            response.markersForLongitudeSpan(0.5),
        )
    }

    @Test
    fun `측정할 수 없거나 음수인 경도 범위는 마커를 만들지 않는다`() {
        val response = PlaceMarkerClusters(emptyList(), listOf(level(0.0, null, count = 2)))

        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -0.1).forEach { span ->
            assertTrue(response.markersForLongitudeSpan(span).isEmpty())
        }
    }

    private fun level(min: Double, max: Double?, count: Int) = PlaceClusterLevel(
        minLngSpan = min,
        maxLngSpan = max,
        clusters = listOf(PlaceMarkerCluster(point, count, bounds)),
        placeIds = emptyList(),
    )

    private fun place(id: Long) = PlaceCoordinate(
        id = id,
        type = PlaceType.COURSE,
        name = "코스$id",
        address = "주소$id",
        point = GeoPoint(37.0 + id / 10.0, 126.0 + id / 10.0),
    )
}
