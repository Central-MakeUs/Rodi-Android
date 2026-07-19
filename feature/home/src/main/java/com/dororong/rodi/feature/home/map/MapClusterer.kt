package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.model.course.GeoPoint
import java.util.LinkedList

enum class MapMarkerMode {
    NationalCluster,
    RegionalCluster,
    Individual,
}

data class MapClusterGrid(
    val columns: Int,
    val rows: Int,
)

data class MapClusterPolicy(
    val mode: MapMarkerMode,
    val targetZoom: Int,
    val grid: MapClusterGrid? = null,
)

data class ProjectedMapItem(
    val id: Long,
    val point: GeoPoint,
    val x: Int,
    val y: Int,
)

data class MapCoursePoint(
    val id: Long,
    val point: GeoPoint,
)

data class MapCluster(
    val memberIds: List<Long>,
    val representativePoint: GeoPoint,
    val targetZoom: Int,
) {
    val count: Int get() = memberIds.size
    val isClusterMarker: Boolean get() = count > 1
}

object ClusterPolicy {
    fun forZoom(zoomLevel: Int): MapClusterPolicy? = when (zoomLevel) {
        in 6..10 -> MapClusterPolicy(
            mode = MapMarkerMode.NationalCluster,
            targetZoom = 11,
            grid = MapClusterGrid(columns = 3, rows = 5),
        )

        in 11..13 -> MapClusterPolicy(
            mode = MapMarkerMode.RegionalCluster,
            targetZoom = 14,
        )

        else -> null
    }
}

object NationalGrid {
    val northEast = GeoPoint(39.3, 131.8)
    val southWest = GeoPoint(32.7, 124.4)
    val policy = MapClusterPolicy(
        mode = MapMarkerMode.NationalCluster,
        targetZoom = 11,
        grid = MapClusterGrid(columns = 3, rows = 5),
    )
}

object MapClusterer {
    fun clusterByScreenDistance(
        items: List<ProjectedMapItem>,
        viewportWidth: Int,
        viewportHeight: Int,
        minimumDistancePx: Int,
        targetZoom: Int,
    ): List<MapCluster> {
        if (viewportWidth <= 0 || viewportHeight <= 0 || minimumDistancePx <= 0) return emptyList()
        val unassigned = items
            .filter { it.x in 0..viewportWidth && it.y in 0..viewportHeight }
            .sortedBy(ProjectedMapItem::id)
            .let(::LinkedList)
        val minimumDistanceSquared = minimumDistancePx.toLong() * minimumDistancePx

        return buildList {
            while (unassigned.isNotEmpty()) {
                val members = mutableListOf(unassigned.removeAt(0))
                var memberIndex = 0
                while (memberIndex < members.size) {
                    val member = members[memberIndex]
                    val iterator = unassigned.listIterator()
                    while (iterator.hasNext()) {
                        val candidate = iterator.next()
                        val xDistance = (member.x - candidate.x).toLong()
                        val yDistance = (member.y - candidate.y).toLong()
                        if (xDistance * xDistance + yDistance * yDistance <= minimumDistanceSquared) {
                            members += candidate
                            iterator.remove()
                        }
                    }
                    memberIndex += 1
                }
                add(clusterProjectedItems(members, targetZoom))
            }
        }.sortedBy { it.memberIds.minOrNull() }
    }

    fun clusterInFixedGeoGrid(
        items: List<MapCoursePoint>,
        northEast: GeoPoint,
        southWest: GeoPoint,
        policy: MapClusterPolicy,
    ): List<MapCluster> {
        val grid = policy.grid ?: return emptyList()
        val latitudeSpan = northEast.lat - southWest.lat
        val longitudeSpan = northEast.lng - southWest.lng
        if (latitudeSpan <= 0.0 || longitudeSpan <= 0.0) return emptyList()

        return items
            .asSequence()
            .filter { item ->
                item.point.lat in southWest.lat..northEast.lat &&
                    item.point.lng in southWest.lng..northEast.lng
            }
            .groupBy { item ->
                val column = ((item.point.lng - southWest.lng) / longitudeSpan * grid.columns)
                    .toInt()
                    .coerceIn(0, grid.columns - 1)
                val row = ((northEast.lat - item.point.lat) / latitudeSpan * grid.rows)
                    .toInt()
                    .coerceIn(0, grid.rows - 1)
                column to row
            }
            .values
            .map { members -> clusterCoursePoints(members, policy.targetZoom) }
            .sortedBy { it.memberIds.minOrNull() }
    }

    private fun clusterProjectedItems(items: List<ProjectedMapItem>, targetZoom: Int): MapCluster =
        clusterCoursePoints(items.map { MapCoursePoint(it.id, it.point) }, targetZoom)

    private fun clusterCoursePoints(items: List<MapCoursePoint>, targetZoom: Int): MapCluster {
        val center = GeoPoint(
            lat = items.map { it.point.lat }.average(),
            lng = items.map { it.point.lng }.average(),
        )
        val representative = items.minBy { item ->
            val latitudeDistance = item.point.lat - center.lat
            val longitudeDistance = item.point.lng - center.lng
            latitudeDistance * latitudeDistance + longitudeDistance * longitudeDistance
        }
        return MapCluster(
            memberIds = items.map(MapCoursePoint::id),
            representativePoint = representative.point,
            targetZoom = targetZoom,
        )
    }
}
