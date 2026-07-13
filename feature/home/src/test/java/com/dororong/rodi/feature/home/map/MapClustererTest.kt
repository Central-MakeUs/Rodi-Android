package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.GeoPoint
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MapClustererTest {
    @Test
    fun `uses national grid from zoom 7 through 10`() {
        (7..10).forEach { zoom ->
            assertEquals(
                MapClusterPolicy(
                    mode = MapMarkerMode.NATIONAL_CLUSTER,
                    targetZoom = 11,
                    grid = MapClusterGrid(columns = 3, rows = 5),
                ),
                ClusterPolicy.forZoom(zoom),
            )
        }
    }

    @Test
    fun `uses regional distance clustering from zoom 11 through 13`() {
        (11..13).forEach { zoom ->
            assertEquals(
                MapClusterPolicy(MapMarkerMode.REGIONAL_CLUSTER, targetZoom = 14),
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
        val policy = MapClusterPolicy(
            mode = MapMarkerMode.NATIONAL_CLUSTER,
            targetZoom = 11,
            grid = MapClusterGrid(columns = 3, rows = 5),
        )
        val clusters = MapClusterer.cluster(
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
        assertTrue(clusters[0].representativePoint in listOf(GeoPoint(37.0, 127.0), GeoPoint(39.0, 129.0)))
        assertTrue(clusters[0].isClusterMarker)
        assertEquals(11, clusters[0].targetZoom)
    }

    @Test
    fun `renders a single member cell as an individual marker`() {
        val coursePoint = GeoPoint(37.0, 127.0)
        val cluster = MapClusterer.cluster(
            items = listOf(ProjectedMapItem(1, coursePoint, 10, 10)),
            viewportWidth = 300,
            viewportHeight = 500,
            policy = MapClusterPolicy(
                mode = MapMarkerMode.NATIONAL_CLUSTER,
                targetZoom = 11,
                grid = MapClusterGrid(columns = 3, rows = 5),
            ),
        ).single()

        assertEquals(coursePoint, cluster.representativePoint)
        assertFalse(cluster.isClusterMarker)
    }

    @Test
    fun `merges nearby points across former grid cell boundaries`() {
        val clusters = MapClusterer.clusterByScreenDistance(
            items = listOf(
                item(1, 37.50, 126.90, 90, 100),
                item(2, 37.51, 126.91, 170, 100),
                item(3, 37.60, 127.00, 350, 400),
            ),
            viewportWidth = 400,
            viewportHeight = 600,
            minimumDistancePx = 100,
            targetZoom = 14,
        )

        assertEquals(2, clusters.size)
        assertEquals(listOf(1, 2), clusters.first { it.memberIds.contains(1) }.memberIds)
        assertFalse(clusters.first { it.memberIds == listOf(3) }.isClusterMarker)
    }

    @Test
    fun `merges chained nearby points to prevent adjacent cluster collisions`() {
        val clusters = MapClusterer.clusterByScreenDistance(
            items = listOf(
                item(1, 37.50, 126.90, 0, 100),
                item(2, 37.51, 126.91, 90, 100),
                item(3, 37.52, 126.92, 180, 100),
            ),
            viewportWidth = 400,
            viewportHeight = 600,
            minimumDistancePx = 100,
            targetZoom = 14,
        )

        assertEquals(1, clusters.size)
        assertEquals(listOf(1, 2, 3), clusters.single().memberIds)
    }

    @Test
    fun `includes right and bottom boundary in final cell`() {
        val policy = MapClusterPolicy(
            mode = MapMarkerMode.NATIONAL_CLUSTER,
            targetZoom = 14,
            grid = MapClusterGrid(columns = 4, rows = 6),
        )
        val cluster = MapClusterer.cluster(
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
        val policy = MapClusterPolicy(
            mode = MapMarkerMode.NATIONAL_CLUSTER,
            targetZoom = 11,
            grid = MapClusterGrid(columns = 3, rows = 5),
        )
        val outside = listOf(item(1, 37.0, 127.0, -1, 20))

        assertTrue(MapClusterer.cluster(outside, 300, 500, policy).isEmpty())
        assertTrue(MapClusterer.cluster(emptyList(), 0, 500, policy).isEmpty())
    }

    @Test
    fun `keeps national cells fixed to the zoom 7 geographic bounds`() {
        val policy = NationalGrid.policy
        val bounds = NationalGrid.query

        val clusters = MapClusterer.clusterInFixedGeoGrid(
            items = listOf(
                point(1, 38.5, 125.0),
                point(2, 38.1, 126.0),
                point(3, 32.7, 131.8),
                point(4, 32.6, 126.0),
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
