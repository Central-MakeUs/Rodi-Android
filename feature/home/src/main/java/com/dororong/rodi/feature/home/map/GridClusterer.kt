package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.GeoPoint
import com.dororong.rodi.core.domain.MapViewportQuery

enum class MapMarkerMode(val label: String) {
    NATIONAL_CLUSTER("전국 클러스터"),
    REGIONAL_CLUSTER("지역 클러스터"),
    INDIVIDUAL("개별 마커"),
}

data class ClusterGridPolicy(
    val mode: MapMarkerMode,
    val columns: Int,
    val rows: Int,
    val targetZoom: Int,
)

object ClusterPolicy {
    fun forZoom(zoomLevel: Int): ClusterGridPolicy? = when (zoomLevel) {
        in 7..10 -> ClusterGridPolicy(MapMarkerMode.NATIONAL_CLUSTER, 3, 5, 11)
        in 11..13 -> ClusterGridPolicy(MapMarkerMode.REGIONAL_CLUSTER, 4, 6, 14)
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
    val policy = ClusterGridPolicy(
        mode = MapMarkerMode.NATIONAL_CLUSTER,
        columns = 3,
        rows = 5,
        targetZoom = 11,
    )
}

object GridClusterer {
    fun cluster(
        items: List<ProjectedMapItem>,
        viewportWidth: Int,
        viewportHeight: Int,
        policy: ClusterGridPolicy,
    ): List<MapCluster> {
        if (viewportWidth <= 0 || viewportHeight <= 0) return emptyList()
        val cellWidth = viewportWidth.toDouble() / policy.columns
        val cellHeight = viewportHeight.toDouble() / policy.rows
        return items
            .asSequence()
            .filter { it.x in 0..viewportWidth && it.y in 0..viewportHeight }
            .groupBy { item ->
                val column = (item.x / cellWidth).toInt().coerceIn(0, policy.columns - 1)
                val row = (item.y / cellHeight).toInt().coerceIn(0, policy.rows - 1)
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

    fun clusterInFixedGeoGrid(
        items: List<MapCoursePoint>,
        bounds: MapViewportQuery,
        policy: ClusterGridPolicy,
    ): List<MapCluster> {
        val northEast = bounds.northEast
        val southWest = bounds.southWest
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
                val column = ((item.point.lng - southWest.lng) / longitudeSpan * policy.columns)
                    .toInt()
                    .coerceIn(0, policy.columns - 1)
                val row = ((northEast.lat - item.point.lat) / latitudeSpan * policy.rows)
                    .toInt()
                    .coerceIn(0, policy.rows - 1)
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
