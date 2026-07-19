package com.dororong.rodi.core.data.source.local.database

import com.dororong.rodi.core.data.source.local.database.dao.PlaceCacheDao
import com.dororong.rodi.core.data.source.local.database.entity.PlaceCoordinateEntity
import com.dororong.rodi.core.data.source.local.database.entity.PlaceSummaryEntity
import com.dororong.rodi.core.data.source.local.sample.SamplePlaces
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceCoordinate
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import com.dororong.rodi.core.domain.model.place.PracticeType
import javax.inject.Inject
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class PlaceCacheLocalDataSource @Inject constructor(
    private val dao: PlaceCacheDao,
) {
    suspend fun seedSamplesIfEmpty() {
        if (dao.coordinateCount() == 0) dao.upsertCoordinates(SamplePlaces.coordinates().map(PlaceCoordinate::toEntity))
        if (dao.summaryCount() == 0) dao.upsertSummaries(SamplePlaces.allSummaries().map(PlaceSummary::toEntity))
    }

    suspend fun coordinates(): List<PlaceCoordinate> = dao.getCoordinates().map(PlaceCoordinateEntity::toDomain)

    suspend fun summaries(query: PlaceViewportQuery): List<PlaceSummary> = dao.getSummariesInViewport(
        southLatitude = query.southWest.lat,
        northLatitude = query.northEast.lat,
        westLongitude = query.southWest.lng,
        eastLongitude = query.northEast.lng,
    ).map { it.toDomain(query.origin) }
        .sortedBy(PlaceSummary::distanceFromMeMeters)

    suspend fun upsertCoordinates(items: List<PlaceCoordinate>) {
        dao.upsertCoordinates(items.map(PlaceCoordinate::toEntity))
    }

    suspend fun upsertSummaries(items: List<PlaceSummary>) {
        dao.upsertSummariesWithCoordinates(
            summaries = items.map(PlaceSummary::toEntity),
            coordinates = items.map(PlaceSummary::toCoordinateEntity),
        )
    }
}

private fun PlaceCoordinate.toEntity() = PlaceCoordinateEntity(id, type.name, name, address, point.lat, point.lng)

private fun PlaceCoordinateEntity.toDomain() = PlaceCoordinate(
    id = id,
    type = enumValueOf(type),
    name = name,
    address = address,
    point = GeoPoint(latitude, longitude),
)

private fun PlaceSummary.toEntity() = PlaceSummaryEntity(
    id = id,
    type = type.name,
    name = name,
    address = address,
    latitude = point.lat,
    longitude = point.lng,
    practiceTypes = practiceTypes.joinToString(",") { it.name },
    description = description,
    distanceMeters = distanceMeters,
    capacity = capacity,
    openTime = openTime,
)

private fun PlaceSummary.toCoordinateEntity() = PlaceCoordinateEntity(id, type.name, name, address, point.lat, point.lng)

private fun PlaceSummaryEntity.toDomain(origin: GeoPoint) = PlaceSummary(
    id = id,
    type = enumValueOf<PlaceType>(type),
    name = name,
    address = address,
    point = GeoPoint(latitude, longitude),
    distanceFromMeMeters = GeoPoint(latitude, longitude).distanceToMeters(origin),
    practiceTypes = practiceTypes.split(',').filter(String::isNotBlank).map(PracticeType::valueOf),
    description = description,
    distanceMeters = distanceMeters,
    capacity = capacity,
    openTime = openTime,
)

private fun GeoPoint.distanceToMeters(other: GeoPoint): Long {
    val latitudeDelta = Math.toRadians(other.lat - lat)
    val longitudeDelta = Math.toRadians(other.lng - lng)
    val a = sin(latitudeDelta / 2).pow(2) +
        cos(Math.toRadians(lat)) * cos(Math.toRadians(other.lat)) * sin(longitudeDelta / 2).pow(2)
    return (EARTH_RADIUS_METERS * 2 * asin(sqrt(a))).toLong()
}

private const val EARTH_RADIUS_METERS = 6_371_000
