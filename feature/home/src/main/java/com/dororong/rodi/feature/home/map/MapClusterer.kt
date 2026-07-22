package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.model.course.GeoPoint
import kotlin.math.pow

enum class MapMarkerMode {
    NationalCluster,
    RegionalCluster,
    Individual,
}

data class MapClusterPolicy(
    val mode: MapMarkerMode,
    val targetZoom: Int,
)

data class ProjectedMapItem(
    val id: Long,
    val point: GeoPoint,
    val x: Int,
    val y: Int,
)

data class MapCluster(
    val memberIds: List<Long>,
    val representativePoint: GeoPoint,
    val targetZoom: Int,
) {
    val count: Int get() = memberIds.distinct().size
    val isClusterMarker: Boolean get() = count > 1
}

object ClusterPolicy {
    fun forZoom(zoomLevel: Int): MapClusterPolicy? = when (zoomLevel) {
        in 6..10 -> MapClusterPolicy(
            mode = MapMarkerMode.NationalCluster,
            targetZoom = 11,
        )

        in 11..13 -> MapClusterPolicy(
            mode = MapMarkerMode.RegionalCluster,
            targetZoom = 14,
        )

        else -> null
    }
}

object MapClusterer {
    fun clusterByScreenDistance(
        items: List<ProjectedMapItem>,
        viewport: MapScreenRect,
        minimumDistancePx: Int,
        targetZoom: Int,
    ): List<MapCluster> {
        if (!viewport.isValid || minimumDistancePx <= 0) return emptyList()
        val minimumDistanceSquared = minimumDistancePx.toDouble().pow(2)
        val clusters = mutableListOf<MutableList<ProjectedMapItem>>()
        items
            .distinctBy(ProjectedMapItem::id)
            .filter { viewport.contains(it.x, it.y) }
            .sortedWith(compareBy(ProjectedMapItem::x, ProjectedMapItem::y, ProjectedMapItem::id))
            .forEach { item ->
                val destination = clusters
                    .asSequence()
                    .filter { members -> canAccept(members, item, minimumDistanceSquared) }
                    .minWithOrNull(
                        compareBy<MutableList<ProjectedMapItem>>(
                            { members -> distanceSquared(item, centerOf(members)) },
                            { members -> members.minOf(ProjectedMapItem::id) },
                        ),
                    )
                if (destination == null) {
                    clusters += mutableListOf(item)
                } else {
                    destination += item
                }
            }

        return clusters
            .map { clusterProjectedItems(it, targetZoom) }
            .sortedBy { it.memberIds.minOrNull() }
    }

    private fun canAccept(
        members: List<ProjectedMapItem>,
        candidate: ProjectedMapItem,
        maximumDistanceSquared: Double,
    ): Boolean {
        val combined = members + candidate
        val center = centerOf(combined)
        return combined.all { distanceSquared(it, center) <= maximumDistanceSquared }
    }

    private fun centerOf(items: List<ProjectedMapItem>): ScreenPoint = ScreenPoint(
        x = items.map(ProjectedMapItem::x).average(),
        y = items.map(ProjectedMapItem::y).average(),
    )

    private fun distanceSquared(item: ProjectedMapItem, center: ScreenPoint): Double =
        (item.x - center.x).pow(2) + (item.y - center.y).pow(2)

    private fun clusterProjectedItems(items: List<ProjectedMapItem>, targetZoom: Int): MapCluster {
        val center = centerOf(items)
        val representative = items.minWith(
            compareBy<ProjectedMapItem>({ distanceSquared(it, center) }, ProjectedMapItem::id),
        )
        return MapCluster(
            memberIds = items.map(ProjectedMapItem::id).distinct(),
            representativePoint = representative.point,
            targetZoom = targetZoom,
        )
    }

}

private data class ScreenPoint(val x: Double, val y: Double)
