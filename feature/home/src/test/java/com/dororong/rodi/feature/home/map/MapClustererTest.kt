package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.model.course.GeoPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MapClustererTest {
    @Test
    fun `uses screen clustering until zoom 10`() {
        (6..10).forEach { zoomLevel ->
            assertEquals(
                MapClusterPolicy(
                    mode = MapMarkerMode.NationalCluster,
                    targetZoom = 11,
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
    fun `does not grow a cluster beyond one radius from its screen center`() {
        val clusters = MapClusterer.clusterByScreenDistance(
            items = listOf(
                item(1, 37.50, 126.90, 0, 100),
                item(2, 37.51, 126.91, 56, 100),
                item(3, 37.52, 126.92, 112, 100),
                item(4, 37.53, 126.93, 168, 100),
            ),
            viewport = fullViewport,
            minimumDistancePx = 56,
            targetZoom = 14,
        )

        assertEquals(2, clusters.size)
        assertEquals(listOf(1L, 2L, 3L), clusters.first().memberIds)
        assertTrue(clusters.first().isClusterMarker)
        assertEquals(listOf(4L), clusters.last().memberIds)
    }

    @Test
    fun `every visible unique place belongs to exactly one cluster`() {
        val clusters = MapClusterer.clusterByScreenDistance(
            items = listOf(
                item(1, 37.50, 126.90, 10, 10),
                item(2, 37.51, 126.91, 40, 40),
                item(2, 37.51, 126.91, 40, 40),
                item(3, 37.80, 127.20, 300, 600),
                item(4, 38.00, 128.00, 500, 900),
            ),
            viewport = fullViewport,
            minimumDistancePx = 56,
            targetZoom = 11,
        )

        assertEquals(setOf(1L, 2L, 3L), clusters.flatMap { it.memberIds }.toSet())
        assertEquals(3, clusters.sumOf { it.count })
    }

    @Test
    fun `uses the padded SDK viewport instead of the full map view`() {
        val clusters = MapClusterer.clusterByScreenDistance(
            items = listOf(
                item(1, 37.50, 126.90, 15, 100),
                item(2, 37.51, 126.91, 16, 40),
                item(3, 37.52, 126.92, 343, 599),
                item(4, 37.53, 126.93, 344, 600),
            ),
            viewport = MapScreenRect(left = 16, top = 40, right = 344, bottom = 600),
            minimumDistancePx = 56,
            targetZoom = 11,
        )

        assertEquals(setOf(2L, 3L), clusters.flatMap { it.memberIds }.toSet())
    }

    private fun item(id: Long, lat: Double, lng: Double, x: Int, y: Int) =
        ProjectedMapItem(id, GeoPoint(lat, lng), x, y)

    private val fullViewport = MapScreenRect(left = 0, top = 0, right = 360, bottom = 720)
}
