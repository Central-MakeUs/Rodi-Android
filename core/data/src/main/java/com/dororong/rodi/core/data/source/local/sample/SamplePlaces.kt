package com.dororong.rodi.core.data.source.local.sample

import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.course.PracticeTag
import com.dororong.rodi.core.domain.model.course.WaypointType
import com.dororong.rodi.core.domain.model.place.CoursePlaceDetail
import com.dororong.rodi.core.domain.model.place.ParkingFeeInfo
import com.dororong.rodi.core.domain.model.place.ParkingOperatingHours
import com.dororong.rodi.core.domain.model.place.ParkingPlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceCoordinate
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import com.dororong.rodi.core.domain.model.place.PlaceWaypoint
import com.dororong.rodi.core.domain.model.place.PlaceWaypointType
import com.dororong.rodi.core.domain.model.place.PracticeType
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** 기존 목업 코스를 Place 모델로만 변환해 서버 장소와 함께 노출한다. */
object SamplePlaces {
    private val places = SampleCourses.RODI_COURSES.map(Course::toPlaceDetail)

    fun coordinates(): List<PlaceCoordinate> = places.map { place ->
        PlaceCoordinate(place.id, place.type, place.name, place.address, place.point)
    }

    fun summaries(query: PlaceViewportQuery): List<PlaceSummary> = places
        .asSequence()
        .filter { it.point.isWithin(query) }
        .map { it.toSummary(query.origin) }
        .sortedBy(PlaceSummary::distanceFromMeMeters)
        .toList()

    fun allSummaries(): List<PlaceSummary> = places.map { it.toSummary(it.point) }

    fun detail(placeId: Long): PlaceDetail? = places.firstOrNull { it.id == placeId }

    fun isSamplePlace(placeId: Long): Boolean = placeId < 0L
}

private fun Course.toPlaceDetail(): PlaceDetail {
    val isParkingPlace = isParking
    return PlaceDetail(
        id = -(id.toLong() + 1L),
        type = if (isParkingPlace) PlaceType.PARKING else PlaceType.COURSE,
        name = title,
        address = roadAddress.ifBlank { regionDisplay },
        point = GeoPoint(startWaypoint.lat, startWaypoint.lng),
        practiceTypes = tags.mapNotNull(PracticeTag::toPracticeType),
        bookmarkCount = 0,
        isBookmarked = false,
        course = if (!isParkingPlace) CoursePlaceDetail(
            description = enrichedDescription,
            cautions = caution.takeIf(String::isNotBlank)?.let(::listOf).orEmpty(),
            distanceMeters = routeDistanceMeters(),
            waypoints = waypoints.sortedBy { it.order }.map { waypoint ->
                PlaceWaypoint(
                    type = waypoint.type.toPlaceWaypointType(),
                    sequence = waypoint.order,
                    point = GeoPoint(waypoint.lat, waypoint.lng),
                    name = waypoint.name,
                )
            },
        ) else null,
        parking = parkingDetail?.let { parking ->
            ParkingPlaceDetail(
                roadAddress = roadAddress.ifBlank { null },
                lotAddress = jibunAddress.ifBlank { null },
                managementNo = parking.managementNo,
                parkingType = parking.parkingType,
                capacity = parking.capacity,
                isFree = parking.isFree,
                feeInfo = parking.feeInfo.toParkingFeeInfo(),
                operatingHours = parking.operatingHours?.let { hours ->
                    ParkingOperatingHours(hours.weekday, hours.saturday, hours.holiday)
                },
            )
        },
    )
}

private fun String?.toParkingFeeInfo(): ParkingFeeInfo? {
    val values = this
        ?.let(SAMPLE_FEE_FIELD_PATTERN::findAll)
        ?.associate { match -> match.groupValues[1] to match.groupValues[2].toIntOrNull() }
        .orEmpty()
    if (values.isEmpty()) return null

    return ParkingFeeInfo(
        baseMinutes = values["baseMinutes"],
        baseFee = values["baseFee"],
        addUnitMinutes = values["addUnitMinutes"],
        addUnitFee = values["addUnitFee"],
        dayTicketHours = values["dayTicketHours"],
        dayTicketFee = values["dayTicketFee"],
        monthlyFee = values["monthlyFee"],
    )
}

private val SAMPLE_FEE_FIELD_PATTERN = Regex("'([A-Za-z]+)':\\s*(None|-?\\d+)")

private fun PlaceDetail.toSummary(origin: GeoPoint) = PlaceSummary(
    id = id,
    type = type,
    name = name,
    address = address,
    point = point,
    distanceFromMeMeters = point.distanceToMeters(origin),
    practiceTypes = practiceTypes,
    description = course?.description,
    distanceMeters = course?.distanceMeters,
    capacity = parking?.capacity,
    openTime = parking?.operatingHours?.weekday,
)

private fun PracticeTag.toPracticeType(): PracticeType? = when (this) {
    PracticeTag.U_TURN -> PracticeType.U_TURN
    PracticeTag.TURN -> PracticeType.LEFT_RIGHT_TURN
    PracticeTag.PARKING -> PracticeType.PARKING
    PracticeTag.LANE_CHANGE -> PracticeType.LANE_CHANGE
    PracticeTag.INTERSECTION -> PracticeType.INTERSECTION
    PracticeTag.ROUNDABOUT -> PracticeType.ROUNDABOUT
    PracticeTag.HIGHWAY_ENTRY -> PracticeType.HIGHWAY_ENTRY
    PracticeTag.ALLEY -> PracticeType.NARROW_ROAD
    PracticeTag.STRAIGHT -> PracticeType.STRAIGHT
    PracticeTag.SIDE_ROAD, PracticeTag.HIGHWAY, PracticeTag.NIGHT -> null
}

private fun WaypointType.toPlaceWaypointType() = when (this) {
    WaypointType.START -> PlaceWaypointType.START
    WaypointType.WAYPOINT -> PlaceWaypointType.VIA
    WaypointType.END -> PlaceWaypointType.DESTINATION
}

private fun Course.routeDistanceMeters(): Int = waypoints.sortedBy { it.order }
    .zipWithNext { first, second ->
        GeoPoint(first.lat, first.lng).distanceToMeters(GeoPoint(second.lat, second.lng))
    }
    .sum()
    .coerceAtMost(Int.MAX_VALUE.toLong())
    .toInt()

private fun GeoPoint.isWithin(query: PlaceViewportQuery) =
    lat in query.southWest.lat..query.northEast.lat && lng in query.southWest.lng..query.northEast.lng

private fun GeoPoint.distanceToMeters(other: GeoPoint): Long {
    val latDelta = Math.toRadians(other.lat - lat)
    val lngDelta = Math.toRadians(other.lng - lng)
    val a = sin(latDelta / 2).pow(2) +
        cos(Math.toRadians(lat)) * cos(Math.toRadians(other.lat)) * sin(lngDelta / 2).pow(2)
    return (6_371_000 * 2 * asin(sqrt(a))).toLong()
}
