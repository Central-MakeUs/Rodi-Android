package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.GeoPoint

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
}
