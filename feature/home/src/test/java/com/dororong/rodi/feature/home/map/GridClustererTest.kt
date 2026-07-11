package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.GeoPoint
import com.dororong.rodi.core.domain.MapViewportQuery
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GridClustererTest {
    @Test
    fun `uses national grid from zoom 7 through 10`() {
        (7..10).forEach { zoom ->
            assertEquals(
                ClusterGridPolicy(MapMarkerMode.NATIONAL_CLUSTER, 3, 5, 11),
                ClusterPolicy.forZoom(zoom),
            )
        }
    }

    @Test
    fun `uses regional grid from zoom 11 through 13`() {
        (11..13).forEach { zoom ->
            assertEquals(
                ClusterGridPolicy(MapMarkerMode.REGIONAL_CLUSTER, 4, 6, 14),
                ClusterPolicy.forZoom(zoom),
            )
        }
    }

    @Test
    fun `uses individual markers from zoom 14`() {
        assertEquals(null, ClusterPolicy.forZoom(14))
        assertEquals(MapMarkerMode.INDIVIDUAL, ClusterPolicy.modeForZoom(14))
    }

    @Test
    fun `groups members by screen cell and averages coordinates`() {
        val policy = ClusterGridPolicy(MapMarkerMode.NATIONAL_CLUSTER, 3, 5, 11)
        val clusters = GridClusterer.cluster(
            items = listOf(
                item(1, 37.0, 127.0, 10, 10),
                item(2, 39.0, 129.0, 90, 90),
                item(3, 35.0, 126.0, 250, 450),
            ),
            viewportWidth = 300,
            viewportHeight = 500,
            policy = policy,
        )

        assertEquals(2, clusters.size)
        assertEquals(listOf(1, 2), clusters[0].memberIds)
        assertEquals(GeoPoint(38.0, 128.0), clusters[0].center)
        assertTrue(clusters[0].focusPoint in listOf(GeoPoint(37.0, 127.0), GeoPoint(39.0, 129.0)))
        assertEquals(11, clusters[0].targetZoom)
    }

    @Test
    fun `includes right and bottom boundary in final cell`() {
        val policy = ClusterGridPolicy(MapMarkerMode.REGIONAL_CLUSTER, 4, 6, 14)
        val cluster = GridClusterer.cluster(
            items = listOf(item(1, 37.0, 127.0, 400, 600)),
            viewportWidth = 400,
            viewportHeight = 600,
            policy = policy,
        ).single()

        assertEquals(3, cluster.column)
        assertEquals(5, cluster.row)
    }

    @Test
    fun `excludes points outside viewport and handles invalid size`() {
        val policy = ClusterGridPolicy(MapMarkerMode.NATIONAL_CLUSTER, 3, 5, 11)
        val outside = listOf(item(1, 37.0, 127.0, -1, 20))

        assertTrue(GridClusterer.cluster(outside, 300, 500, policy).isEmpty())
        assertTrue(GridClusterer.cluster(emptyList(), 0, 500, policy).isEmpty())
    }

    @Test
    fun `keeps national cells fixed to the zoom 7 geographic bounds`() {
        val policy = ClusterPolicy.forZoom(7)!!
        val bounds = MapViewportQuery(
            northEast = GeoPoint(40.0, 130.0),
            southWest = GeoPoint(30.0, 120.0),
            zoomLevel = 7,
        )

        val clusters = GridClusterer.clusterInFixedGeoGrid(
            items = listOf(
                point(1, 39.0, 121.0),
                point(2, 38.5, 122.0),
                point(3, 30.0, 130.0),
                point(4, 29.9, 126.0),
            ),
            bounds = bounds,
            policy = policy,
        )

        assertEquals(2, clusters.size)
        assertEquals(listOf(1, 2), clusters[0].memberIds)
        assertEquals(0, clusters[0].column)
        assertEquals(0, clusters[0].row)
        assertEquals(listOf(3), clusters[1].memberIds)
        assertEquals(2, clusters[1].column)
        assertEquals(4, clusters[1].row)
    }

    private fun item(id: Int, lat: Double, lng: Double, x: Int, y: Int) =
        ProjectedMapItem(id, GeoPoint(lat, lng), x, y)

    private fun point(id: Int, lat: Double, lng: Double) =
        MapCoursePoint(id, GeoPoint(lat, lng))
}
