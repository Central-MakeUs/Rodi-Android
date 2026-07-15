package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.model.course.GeoPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MapClustererTest {
    @Test
    fun `uses a 3 by 5 national grid until zoom 10`() {
        (7..10).forEach { zoomLevel ->
            assertEquals(
                MapClusterPolicy(
                    mode = MapMarkerMode.NationalCluster,
                    targetZoom = 11,
                    grid = MapClusterGrid(columns = 3, rows = 5),
                ),
                ClusterPolicy.forZoom(zoomLevel),
            )
        }
    }

    @Test
    fun `uses 56dp regional clustering from zoom 11 until zoom 13`() {
        (11..13).forEach { zoomLevel ->
            assertEquals(
                MapClusterPolicy(
                    mode = MapMarkerMode.RegionalCluster,
                    targetZoom = 14,
                ),
                ClusterPolicy.forZoom(zoomLevel),
            )
        }
        assertNull(ClusterPolicy.forZoom(14))
    }

    @Test
    fun `merges nearby points including chained neighbours`() {
        val clusters = MapClusterer.clusterByScreenDistance(
            items = listOf(
                item(1, 37.50, 126.90, 0, 100),
                item(2, 37.51, 126.91, 56, 100),
                item(3, 37.52, 126.92, 112, 100),
                item(4, 37.60, 127.00, 300, 300),
            ),
            viewportWidth = 360,
            viewportHeight = 720,
            minimumDistancePx = 56,
            targetZoom = 14,
        )

        assertEquals(2, clusters.size)
        assertEquals(listOf(1, 2, 3), clusters.first().memberIds)
        assertTrue(clusters.first().isClusterMarker)
        assertEquals(listOf(4), clusters.last().memberIds)
    }

    @Test
    fun `keeps the national cluster cells fixed to Korean bounds`() {
        val clusters = MapClusterer.clusterInFixedGeoGrid(
            items = listOf(
                MapCoursePoint(1, GeoPoint(38.5, 125.0)),
                MapCoursePoint(2, GeoPoint(38.1, 126.0)),
                MapCoursePoint(3, GeoPoint(32.7, 131.8)),
            ),
            northEast = NationalGrid.northEast,
            southWest = NationalGrid.southWest,
            policy = NationalGrid.policy,
        )

        assertEquals(listOf(1, 2), clusters.first().memberIds)
        assertEquals(listOf(3), clusters.last().memberIds)
    }

    private fun item(id: Int, lat: Double, lng: Double, x: Int, y: Int) =
        ProjectedMapItem(id, GeoPoint(lat, lng), x, y)
}
