package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceCoordinate
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceType

private data class PlaceLocationKey(
    val type: PlaceType,
    val point: GeoPoint,
)

internal fun List<PlaceCoordinate>.preferServerCoordinates(): List<PlaceCoordinate> =
    preferServerPlaces(
        idOf = PlaceCoordinate::id,
        keyOf = { PlaceLocationKey(it.type, it.point) },
    )

internal fun List<PlaceSummary>.preferServerSummaries(): List<PlaceSummary> =
    preferServerPlaces(
        idOf = PlaceSummary::id,
        keyOf = { PlaceLocationKey(it.type, it.point) },
    )

private fun <T> List<T>.preferServerPlaces(
    idOf: (T) -> Long,
    keyOf: (T) -> PlaceLocationKey,
): List<T> {
    val serverLocations = asSequence()
        .filter { idOf(it) > 0L }
        .map(keyOf)
        .toSet()
    return filter { item -> idOf(item) > 0L || keyOf(item) !in serverLocations }
}
