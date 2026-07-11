package com.dororong.rodi.feature.home

import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.domain.model.course.CourseFeatures
import com.dororong.rodi.core.domain.model.course.OperatingHours
import com.dororong.rodi.core.domain.model.course.ParkingDetail
import com.dororong.rodi.core.domain.model.course.RodiItemType
import com.dororong.rodi.core.domain.model.course.Waypoint
import com.dororong.rodi.core.domain.model.course.WaypointType

internal object HomePreviewData {
    val courses = listOf(
        course(
            id = 1,
            name = "한강공원 순환 코스",
            nickname = "한강 순환",
            difficulty = 2,
            features = CourseFeatures(straightDriving = true, laneChange = true),
        ),
        course(
            id = 2,
            name = "성수 골목길 적응 코스",
            nickname = "성수 골목",
            areaName = "성수동",
            difficulty = 4,
            features = CourseFeatures(alley = true, rightTurn = true, parking = true),
        ),
        course(
            id = 3,
            name = "도심 교차로 연습 코스",
            nickname = "도심 교차로",
            areaName = "을지로",
            difficulty = 3,
            features = CourseFeatures(intersection = true, leftTurn = true, laneChange = true),
        ),
    )

    val freeParking = parking(
        id = 101,
        name = "망원 한강공원 공영주차장",
        isFree = true,
        feeInfo = null,
        note = "주말 혼잡 시간대에는 진입 대기가 발생할 수 있습니다.",
    )

    val paidParking = parking(
        id = 102,
        name = "합정역 공영주차장",
        isFree = false,
        feeInfo = """{"baseMinutes":30,"baseFee":1200,"addUnitMinutes":10,"addUnitFee":500}""",
        note = "최초 30분 이후 10분 단위 추가요금",
    )

    val all = courses + freeParking + paidParking

    private fun course(
        id: Int,
        name: String,
        nickname: String,
        areaName: String = "망원동",
        difficulty: Int,
        features: CourseFeatures,
    ) = Course(
        id = id,
        courseName = name,
        courseNickname = nickname,
        areaName = areaName,
        region = "seoul",
        difficulty = difficulty,
        trafficDensity = "보통",
        source = "preview",
        sourceUrl = "",
        crawledAt = "",
        waypoints = waypoints(),
        features = features,
        recommendation = 4,
        caution = "초행길에서는 차선 변경 구간을 천천히 확인하세요.",
        bestTime = "평일 오전",
        enrichedDescription = "초보 운전자가 도심 주행과 차선 변경을 함께 연습하기 좋은 코스입니다.",
    )

    private fun parking(
        id: Int,
        name: String,
        isFree: Boolean,
        feeInfo: String?,
        note: String,
    ) = Course(
        id = id,
        courseName = name,
        courseNickname = name,
        areaName = "망원동",
        region = "seoul",
        difficulty = 1,
        trafficDensity = null,
        source = "preview",
        sourceUrl = "",
        crawledAt = "",
        waypoints = listOf(
            Waypoint(
                order = 0,
                type = WaypointType.START,
                name = name,
                lat = 37.5563,
                lng = 126.9220,
                address = "서울특별시 마포구 마포나루길 467",
                jibunAddress = "서울특별시 마포구 망원동 205-4",
                category = "parking",
            ),
            Waypoint(
                order = 1,
                type = WaypointType.END,
                name = name,
                lat = 37.5563,
                lng = 126.9220,
                address = "서울특별시 마포구 마포나루길 467",
                jibunAddress = "서울특별시 마포구 망원동 205-4",
                category = "parking",
            ),
        ),
        features = CourseFeatures(parking = true),
        recommendation = 4,
        caution = "",
        bestTime = "",
        enrichedDescription = "주차 연습과 목적지 설정 흐름을 확인하는 Preview 데이터입니다.",
        parkingDetail = ParkingDetail(
            managementNo = "preview-$id",
            parkingType = if (isFree) "무료 주차장" else "공영주차장",
            capacity = 128,
            isFree = isFree,
            feeInfo = feeInfo,
            operatingHours = OperatingHours(
                weekday = "00:00-24:00",
                saturday = "00:00-24:00",
                holiday = "00:00-24:00",
            ),
            paymentMethods = listOf("카드"),
            hasAccessibleSpace = true,
            phone = "02-000-0000",
            operator = "마포구",
            note = note,
        ),
        itemType = RodiItemType.PARKING,
    )

    private fun waypoints() = listOf(
        Waypoint(
            order = 0,
            type = WaypointType.START,
            name = "망원한강공원",
            lat = 37.5563,
            lng = 126.9220,
            address = "서울특별시 마포구 마포나루길 467",
            jibunAddress = "서울특별시 마포구 망원동 205-4",
            category = "park",
        ),
        Waypoint(
            order = 1,
            type = WaypointType.WAYPOINT,
            name = "합정역",
            lat = 37.5497,
            lng = 126.9140,
            address = "서울특별시 마포구 양화로 55",
            jibunAddress = "서울특별시 마포구 서교동 393",
            category = "station",
        ),
        Waypoint(
            order = 2,
            type = WaypointType.END,
            name = "상수역",
            lat = 37.5477,
            lng = 126.9229,
            address = "서울특별시 마포구 독막로 85",
            jibunAddress = "서울특별시 마포구 상수동 309",
            category = "station",
        ),
    )
}
