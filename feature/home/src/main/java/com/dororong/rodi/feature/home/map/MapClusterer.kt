package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.GeoPoint
import com.dororong.rodi.core.domain.MapViewportQuery

enum class MapMarkerMode(val label: String) {
    NATIONAL_CLUSTER("전국 클러스터"),
    REGIONAL_CLUSTER("지역 클러스터"),
    INDIVIDUAL("개별 마커"),
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

object ClusterPolicy {
    fun forZoom(zoomLevel: Int): MapClusterPolicy? = when (zoomLevel) {
        in 7..10 -> MapClusterPolicy(
            mode = MapMarkerMode.NATIONAL_CLUSTER,
            targetZoom = 11,
            grid = MapClusterGrid(columns = 3, rows = 5),
        )
        in 11..13 -> MapClusterPolicy(
            mode = MapMarkerMode.REGIONAL_CLUSTER,
            targetZoom = 14,
        )
        else -> null
    }

    fun modeForZoom(zoomLevel: Int): MapMarkerMode =
        forZoom(zoomLevel)?.mode ?: MapMarkerMode.INDIVIDUAL
}

data class ProjectedMapItem(
    val id: Int,
    val point: GeoPoint,
    val x: Int,
    val y: Int,
)

data class MapCoursePoint(
    val id: Int,
    val point: GeoPoint,
)

data class MapCluster(
    val memberIds: List<Int>,
    val center: GeoPoint,
    val focusPoint: GeoPoint,
    val column: Int,
    val row: Int,
    val targetZoom: Int,
) {
    val count: Int get() = memberIds.size
    val isClusterMarker: Boolean get() = count > 1
    val representativePoint: GeoPoint get() = focusPoint
}

data class NationalGridSnapshot(
    val query: MapViewportQuery,
    val courseCount: Int,
    val clusters: List<MapCluster>,
)

object NationalGrid {
    val query = MapViewportQuery(
        northEast = GeoPoint(39.3, 131.8),
        southWest = GeoPoint(32.7, 124.4),
        zoomLevel = 7,
    )
    val policy = MapClusterPolicy(
        mode = MapMarkerMode.NATIONAL_CLUSTER,
        targetZoom = 11,
        grid = MapClusterGrid(columns = 3, rows = 5),
    )
}

object MapClusterer {
    fun cluster(
        items: List<ProjectedMapItem>,
        viewportWidth: Int,
        viewportHeight: Int,
        policy: MapClusterPolicy,
    ): List<MapCluster> {
        if (viewportWidth <= 0 || viewportHeight <= 0) return emptyList()
        val grid = policy.grid ?: return emptyList()
        val columns = grid.columns
        val rows = grid.rows
        val cellWidth = viewportWidth.toDouble() / columns
        val cellHeight = viewportHeight.toDouble() / rows
        return items
            .asSequence()
            .filter { it.x in 0..viewportWidth && it.y in 0..viewportHeight }
            .groupBy { item ->
                val column = (item.x / cellWidth).toInt().coerceIn(0, columns - 1)
                val row = (item.y / cellHeight).toInt().coerceIn(0, rows - 1)
                column to row
            }
            .map { (cell, members) ->
                val center = GeoPoint(
                    lat = members.map { it.point.lat }.average(),
                    lng = members.map { it.point.lng }.average(),
                )
                MapCluster(
                    memberIds = members.map(ProjectedMapItem::id),
                    center = center,
                    focusPoint = members.minBy { member ->
                        val latDistance = member.point.lat - center.lat
                        val lngDistance = member.point.lng - center.lng
                        latDistance * latDistance + lngDistance * lngDistance
                    }.point,
                    column = cell.first,
                    row = cell.second,
                    targetZoom = policy.targetZoom,
                )
            }
            .sortedWith(compareBy(MapCluster::row, MapCluster::column))
            .toList()
    }

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
            .toMutableList()
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

                val center = GeoPoint(
                    lat = members.map { it.point.lat }.average(),
                    lng = members.map { it.point.lng }.average(),
                )
                val focus = members.minBy { member ->
                    val latDistance = member.point.lat - center.lat
                    val lngDistance = member.point.lng - center.lng
                    latDistance * latDistance + lngDistance * lngDistance
                }
                add(
                    MapCluster(
                        memberIds = members.map(ProjectedMapItem::id),
                        center = center,
                        focusPoint = focus.point,
                        column = focus.x,
                        row = focus.y,
                        targetZoom = targetZoom,
                    ),
                )
            }
        }.sortedWith(compareBy(MapCluster::row, MapCluster::column))
    }

    fun clusterInFixedGeoGrid(
        items: List<MapCoursePoint>,
        bounds: MapViewportQuery,
        policy: MapClusterPolicy,
    ): List<MapCluster> {
        val northEast = bounds.northEast
        val southWest = bounds.southWest
        val latitudeSpan = northEast.lat - southWest.lat
        val longitudeSpan = northEast.lng - southWest.lng
        val grid = policy.grid ?: return emptyList()
        val columns = grid.columns
        val rows = grid.rows
        if (latitudeSpan <= 0.0 || longitudeSpan <= 0.0) return emptyList()

        return items
            .asSequence()
            .filter { item ->
                item.point.lat in southWest.lat..northEast.lat &&
                        item.point.lng in southWest.lng..northEast.lng
            }
            .groupBy { item ->
                val column = ((item.point.lng - southWest.lng) / longitudeSpan * columns)
                    .toInt()
                    .coerceIn(0, columns - 1)
                val row = ((northEast.lat - item.point.lat) / latitudeSpan * rows)
                    .toInt()
                    .coerceIn(0, rows - 1)
                column to row
            }
            .map { (cell, members) ->
                val center = GeoPoint(
                    lat = members.map { it.point.lat }.average(),
                    lng = members.map { it.point.lng }.average(),
                )
                MapCluster(
                    memberIds = members.map(MapCoursePoint::id),
                    center = center,
                    focusPoint = members.minBy { member ->
                        val latDistance = member.point.lat - center.lat
                        val lngDistance = member.point.lng - center.lng
                        latDistance * latDistance + lngDistance * lngDistance
                    }.point,
                    column = cell.first,
                    row = cell.second,
                    targetZoom = policy.targetZoom,
                )
            }
            .sortedWith(compareBy(MapCluster::row, MapCluster::column))
            .toList()
    }
}
