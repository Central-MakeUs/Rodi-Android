package com.dororong.rodi.core.domain.model.place

import com.dororong.rodi.core.domain.model.course.GeoPoint

enum class PlaceType {
    COURSE,
    PARKING,
}

enum class PracticeType(val label: String) {
    U_TURN("유턴"),
    LEFT_RIGHT_TURN("좌우회전"),
    PARKING("주차"),
    LANE_CHANGE("차선변경"),
    INTERSECTION("교차로"),
    ROUNDABOUT("회전교차로"),
    UNPROTECTED_LEFT_TURN("비보호 좌회전"),
    HIGHWAY_ENTRY("고속도로 진입"),
    CORNERING("코너링"),
    NARROW_ROAD("좁은 길"),
    MULTILANE("다차로"),
    MERGING("합류"),
    STRAIGHT("직진"),
}

data class PlaceCoordinate(
    val id: Long,
    val type: PlaceType,
    val name: String,
    val address: String,
    val point: GeoPoint,
)

data class PlaceSummary(
    val id: Long,
    val type: PlaceType,
    val name: String,
    val address: String,
    val point: GeoPoint,
    val distanceFromMeMeters: Long?,
    val practiceTypes: List<PracticeType>,
    val description: String?,
    val distanceMeters: Int?,
    val capacity: Int?,
    val openTime: String?,
)

data class CursorPage<T>(
    val items: List<T>,
    val hasNext: Boolean,
    val nextCursor: String?,
    val totalCount: Long?,
)

data class PlaceViewportQuery(
    val southWest: GeoPoint,
    val northEast: GeoPoint,
    val origin: GeoPoint,
)

data class PlaceDetail(
    val id: Long,
    val type: PlaceType,
    val name: String,
    val address: String,
    val point: GeoPoint,
    val practiceTypes: List<PracticeType>,
    val bookmarkCount: Long,
    val isBookmarked: Boolean,
    val course: CoursePlaceDetail?,
    val parking: ParkingPlaceDetail?,
)

data class CoursePlaceDetail(
    val description: String,
    val cautions: List<String>,
    val distanceMeters: Int,
    val waypoints: List<PlaceWaypoint>,
)

enum class PlaceWaypointType {
    START,
    VIA,
    DESTINATION,
}

data class PlaceWaypoint(
    val type: PlaceWaypointType,
    val sequence: Int,
    val point: GeoPoint,
    val name: String?,
)

data class ParkingPlaceDetail(
    val roadAddress: String?,
    val lotAddress: String?,
    val managementNo: String?,
    val parkingType: String?,
    val capacity: Int?,
    val isFree: Boolean,
    val feeInfo: ParkingFeeInfo?,
    val operatingHours: ParkingOperatingHours?,
)

data class ParkingFeeInfo(
    val baseMinutes: Int?,
    val baseFee: Int?,
    val addUnitMinutes: Int?,
    val addUnitFee: Int?,
    val dayTicketHours: Int?,
    val dayTicketFee: Int?,
    val monthlyFee: Int?,
)

data class ParkingOperatingHours(
    val weekday: String?,
    val saturday: String?,
    val holiday: String?,
)
