package com.dororong.rodi.core.data.source.remote.model.place

import kotlinx.serialization.Serializable

@Serializable
data class PlaceCoordinateResponse(
    val id: Long,
    val type: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
)

@Serializable
data class CursorPagePlaceResponse(
    val items: List<PlaceListItemResponse> = emptyList(),
    val hasNext: Boolean = false,
    val nextCursor: String? = null,
    val totalCount: Long? = null,
)

@Serializable
data class PlaceListItemResponse(
    val id: Long,
    val type: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val distanceFromMe: Long = 0,
    val practiceTypes: List<String> = emptyList(),
    val description: String? = null,
    val distanceMeters: Int? = null,
    val capacity: Int? = null,
    val openTime: String? = null,
)

@Serializable
data class PlaceDetailResponse(
    val id: Long,
    val type: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val practiceTypes: List<String> = emptyList(),
    val bookmarkCount: Long = 0,
    val isBookmarked: Boolean = false,
    val course: CourseDetailResponse? = null,
    val parking: ParkingDetailResponse? = null,
)

@Serializable
data class CourseDetailResponse(
    val description: String = "",
    val cautions: List<String> = emptyList(),
    val distanceMeters: Int = 0,
    val waypoints: List<WaypointResponse> = emptyList(),
)

@Serializable
data class WaypointResponse(
    val type: String,
    val sequence: Int,
    val lat: Double,
    val lng: Double,
    val name: String? = null,
)

@Serializable
data class ParkingDetailResponse(
    val roadAddress: String? = null,
    val lotAddress: String? = null,
    val managementNo: String? = null,
    val parkingType: String? = null,
    val capacity: Int? = null,
    val isFree: Boolean = false,
    val feeInfo: FeeInfoResponse? = null,
    val operatingHours: OperatingHoursResponse? = null,
)

@Serializable
data class FeeInfoResponse(
    val baseMinutes: Int? = null,
    val baseFee: Int? = null,
    val addUnitMinutes: Int? = null,
    val addUnitFee: Int? = null,
    val dayTicketHours: Int? = null,
    val dayTicketFee: Int? = null,
    val monthlyFee: Int? = null,
)

@Serializable
data class OperatingHoursResponse(
    val weekday: String? = null,
    val saturday: String? = null,
    val holiday: String? = null,
)
