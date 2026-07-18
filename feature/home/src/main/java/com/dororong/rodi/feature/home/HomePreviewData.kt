package com.dororong.rodi.feature.home

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.CoursePlaceDetail
import com.dororong.rodi.core.domain.model.place.ParkingFeeInfo
import com.dororong.rodi.core.domain.model.place.ParkingOperatingHours
import com.dororong.rodi.core.domain.model.place.ParkingPlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.place.PlaceWaypoint
import com.dororong.rodi.core.domain.model.place.PlaceWaypointType
import com.dororong.rodi.core.domain.model.place.PracticeType

internal object HomePreviewData {
    val courseSummary = PlaceSummary(
        id = 9_223_372_036L,
        type = PlaceType.COURSE,
        name = "한강공원 순환 코스",
        address = "서울특별시 마포구",
        point = GeoPoint(37.5563, 126.9220),
        distanceFromMeMeters = 1_240,
        practiceTypes = listOf(PracticeType.LANE_CHANGE, PracticeType.LEFT_RIGHT_TURN),
        description = "초보 운전자가 도심 주행과 차선 변경을 함께 연습하기 좋은 코스입니다.",
        distanceMeters = 12_400,
        capacity = null,
        openTime = null,
    )

    val longCourseSummary = courseSummary.copy(
        id = 9_223_372_037L,
        name = "성수동에서 한강을 지나 도심 교차로까지 이어지는 아주 긴 연습 코스 이름",
        practiceTypes = PracticeType.entries.toList(),
    )

    val parkingSummary = PlaceSummary(
        id = 9_223_372_038L,
        type = PlaceType.PARKING,
        name = "망원 한강공원 공영주차장",
        address = "서울특별시 마포구",
        point = GeoPoint(37.5568, 126.9190),
        distanceFromMeMeters = 850,
        practiceTypes = listOf(PracticeType.PARKING),
        description = null,
        distanceMeters = null,
        capacity = 128,
        openTime = "06:00",
    )

    val summaries = listOf(courseSummary, parkingSummary, longCourseSummary)

    val courseDetail = PlaceDetail(
        id = courseSummary.id,
        type = PlaceType.COURSE,
        name = courseSummary.name,
        address = courseSummary.address,
        point = courseSummary.point,
        practiceTypes = courseSummary.practiceTypes,
        bookmarkCount = 248,
        isBookmarked = false,
        course = CoursePlaceDetail(
            description = courseSummary.description.orEmpty(),
            cautions = listOf("차선 변경 구간 주의", "출퇴근 시간 혼잡"),
            distanceMeters = courseSummary.distanceMeters ?: 0,
            waypoints = listOf(
                waypoint(PlaceWaypointType.START, 0, "망원한강공원", 37.5563, 126.9220),
                waypoint(PlaceWaypointType.VIA, 1, "합정역", 37.5497, 126.9140),
                waypoint(PlaceWaypointType.VIA, 2, "상수역", 37.5477, 126.9229),
                waypoint(PlaceWaypointType.DESTINATION, 3, "홍대입구", 37.5572, 126.9254),
            ),
        ),
        parking = null,
    )

    val parkingDetail = PlaceDetail(
        id = parkingSummary.id,
        type = PlaceType.PARKING,
        name = parkingSummary.name,
        address = parkingSummary.address,
        point = parkingSummary.point,
        practiceTypes = listOf(PracticeType.PARKING),
        bookmarkCount = 31,
        isBookmarked = true,
        course = null,
        parking = ParkingPlaceDetail(
            roadAddress = "서울특별시 마포구 마포나루길 467",
            lotAddress = "서울특별시 마포구 망원동 205-4",
            managementNo = "MAPO-2026-001",
            parkingType = "노외",
            capacity = 128,
            isFree = false,
            feeInfo = ParkingFeeInfo(
                baseMinutes = 30,
                baseFee = 1_200,
                addUnitMinutes = 10,
                addUnitFee = 500,
                dayTicketHours = 12,
                dayTicketFee = 10_000,
                monthlyFee = null,
            ),
            operatingHours = ParkingOperatingHours(
                weekday = "06:00-22:00",
                saturday = "00:00-24:00",
                holiday = "00:00-24:00",
            ),
        ),
    )

    val parkingMissingFields = parkingDetail.copy(
        id = 9_223_372_039L,
        name = "정보가 적은 주차장",
        parking = parkingDetail.parking?.copy(
            roadAddress = null,
            lotAddress = null,
            capacity = null,
            feeInfo = null,
            operatingHours = null,
        ),
    )

    private fun waypoint(
        type: PlaceWaypointType,
        sequence: Int,
        name: String,
        lat: Double,
        lng: Double,
    ) = PlaceWaypoint(type, sequence, GeoPoint(lat, lng), name)
}
