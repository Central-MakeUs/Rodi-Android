package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.place.CourseDetailResponse
import com.dororong.rodi.core.data.source.remote.model.place.CursorPagePlaceResponse
import com.dororong.rodi.core.data.source.remote.model.place.FeeInfoResponse
import com.dororong.rodi.core.data.source.remote.model.place.OperatingHoursResponse
import com.dororong.rodi.core.data.source.remote.model.place.ParkingDetailResponse
import com.dororong.rodi.core.data.source.remote.model.place.PlaceCoordinateResponse
import com.dororong.rodi.core.data.source.remote.model.place.PlaceDetailResponse
import com.dororong.rodi.core.data.source.remote.model.place.PlaceListItemResponse
import com.dororong.rodi.core.data.source.remote.model.place.RelatedSearchResponse
import com.dororong.rodi.core.data.source.remote.model.place.WaypointResponse
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.CoursePlaceDetail
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.ParkingFeeInfo
import com.dororong.rodi.core.domain.model.place.ParkingOperatingHours
import com.dororong.rodi.core.domain.model.place.ParkingPlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceCoordinate
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.place.PlaceWaypoint
import com.dororong.rodi.core.domain.model.place.PlaceWaypointType
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.model.search.PlaceSuggestion
import com.dororong.rodi.core.domain.model.search.RelatedSearch

fun PlaceCoordinateResponse.toDomain() = PlaceCoordinate(
    id = id,
    type = type.toPlaceType(),
    name = name,
    address = address,
    point = GeoPoint(lat, lng),
)

fun CursorPagePlaceResponse.toDomain() = CursorPage(
    items = items.map(PlaceListItemResponse::toDomain),
    hasNext = hasNext,
    nextCursor = nextCursor,
    totalCount = totalCount,
)

fun RelatedSearchResponse.toDomain() = RelatedSearch(
    regions = regions,
    places = CursorPage(
        items = places.items.map { PlaceSuggestion(it.placeId, it.name, it.region) },
        hasNext = places.hasNext,
        nextCursor = places.nextCursor,
        totalCount = places.totalCount,
    ),
)

fun PlaceListItemResponse.toDomain() = PlaceSummary(
    id = id,
    type = type.toPlaceType(),
    name = name,
    address = address,
    point = GeoPoint(lat, lng),
    distanceFromMeMeters = distanceFromMe,
    practiceTypes = practiceTypes.mapNotNull(String::toPracticeType),
    description = description,
    distanceMeters = distanceMeters,
    capacity = capacity,
    openTime = openTime,
)

fun PlaceDetailResponse.toDomain() = PlaceDetail(
    id = id,
    type = type.toPlaceType(),
    name = name,
    address = address,
    point = GeoPoint(lat, lng),
    practiceTypes = practiceTypes.mapNotNull(String::toPracticeType),
    bookmarkCount = bookmarkCount,
    isBookmarked = isBookmarked,
    course = course?.toDomain(),
    parking = parking?.toDomain(),
)

private fun CourseDetailResponse.toDomain() = CoursePlaceDetail(
    description = description,
    cautions = cautions,
    distanceMeters = distanceMeters,
    waypoints = waypoints.sortedBy(WaypointResponse::sequence).map(WaypointResponse::toDomain),
)

private fun WaypointResponse.toDomain() = PlaceWaypoint(
    type = when (type) {
        "START" -> PlaceWaypointType.START
        "VIA" -> PlaceWaypointType.VIA
        "DESTINATION" -> PlaceWaypointType.DESTINATION
        else -> throw IllegalArgumentException("Unsupported waypoint type: $type")
    },
    sequence = sequence,
    point = GeoPoint(lat, lng),
    name = name,
)

private fun ParkingDetailResponse.toDomain() = ParkingPlaceDetail(
    roadAddress = roadAddress,
    lotAddress = lotAddress,
    managementNo = managementNo,
    parkingType = parkingType,
    capacity = capacity,
    isFree = isFree,
    feeInfo = feeInfo?.toDomain(),
    operatingHours = operatingHours?.toDomain(),
)

private fun FeeInfoResponse.toDomain() = ParkingFeeInfo(
    baseMinutes = baseMinutes,
    baseFee = baseFee,
    addUnitMinutes = addUnitMinutes,
    addUnitFee = addUnitFee,
    dayTicketHours = dayTicketHours,
    dayTicketFee = dayTicketFee,
    monthlyFee = monthlyFee,
)

private fun OperatingHoursResponse.toDomain() = ParkingOperatingHours(weekday, saturday, holiday)

private fun String.toPlaceType(): PlaceType = runCatching { PlaceType.valueOf(this) }
    .getOrElse { throw IllegalArgumentException("Unsupported place type: $this") }

private fun String.toPracticeType(): PracticeType? = PracticeType.entries.firstOrNull { it.name == this }
