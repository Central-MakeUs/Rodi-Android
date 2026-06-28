package com.cmc.routi.model

/** 코스를 구성하는 한 지점. 좌표는 WGS84(위경도). */
data class CoursePoint(
    val name: String,
    val lat: Double,
    val lng: Double,
)

/** 코스 난이도. 카드에서 색상 태그로 표시. */
enum class Difficulty(val label: String, val level: Int) {
    LV1("매우 쉬움", 1),
    LV2("쉬움", 2),
    LV3("보통", 3),
    LV4("어려움", 4),
    LV5("매우 어려움", 5),
}

/** 연습 유형 태그. */
enum class PracticeTag(val label: String) {
    STRAIGHT("직선주행"),
    LANE_CHANGE("차선변경"),
    U_TURN("유턴"),
    TURN("좌우회전"),
    ALLEY("골목길"),
    PARKING("주차"),
    HIGHWAY_ENTRY("고속진입"),
    NIGHT("야간운전"),
    SIDE_ROAD("보조도로"),
    HIGHWAY("고속도로"),
    INTERSECTION("교차로"),
    ROUNDABOUT("회전교차로"),
}

/**
 * 연습 코스.
 *
 * MVP에서는 [com.cmc.routi.data.SampleCourses] 에 하드코딩한 데이터를 쓰고,
 * 추후 서버 데이터로 교체한다.
 */
data class Course(
    val id: String,
    val title: String,
    val region: String,           // 예: 서울 마포구
    val roadAddress: String,      // 도로명 주소
    val jibunAddress: String,     // 지번 주소
    val difficulty: Difficulty,
    val tags: Set<PracticeTag>,
    val summary: String,
    val rating: Double,           // 별점 (예: 4.4)
    val likeCount: Int,
    val origin: CoursePoint,
    val waypoints: List<CoursePoint>,
    val destination: CoursePoint,
) {
    val allPoints: List<CoursePoint> get() = listOf(origin) + waypoints + destination
}
