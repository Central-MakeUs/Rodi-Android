package com.cmc.routi.model

enum class WaypointType { START, WAYPOINT, END }

/** 코스를 구성하는 한 지점. 좌표는 WGS84(위경도). */
data class CoursePoint(
    val name: String,
    val lat: Double,
    val lng: Double,
)

/** 서버 포맷 웨이포인트. */
data class Waypoint(
    val order: Int,
    val type: WaypointType,
    val name: String,
    val lat: Double,
    val lng: Double,
    val address: String,
    val jibunAddress: String = address,
    val category: String,
)

/** 코스 특징 (서버 포맷). */
data class CourseFeatures(
    val straightDriving: Boolean = false,
    val intersection: Boolean = false,
    val leftTurn: Boolean = false,
    val rightTurn: Boolean = false,
    val laneChange: Boolean = false,
    val parking: Boolean = false,
    val highway: Boolean = false,
    val highwayEntry: Boolean = false,
    val nightDriving: Boolean = false,
    val alley: Boolean = false,
)

/** 코스 난이도. 카드에서 색상 태그로 표시. */
enum class Difficulty(val label: String, val level: Int) {
    LV1("매우 쉬움", 1),
    LV2("쉬움", 2),
    LV3("보통", 3),
    LV4("어려움", 4),
    LV5("매우 어려움", 5),
}

/** 연습 유형 태그 (features 에서 파생). */
enum class PracticeTag(val label: String) {
    STRAIGHT("직진"),
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

fun CourseFeatures.toPracticeTags(): Set<PracticeTag> = buildSet {
    if (straightDriving) add(PracticeTag.STRAIGHT)
    if (intersection) add(PracticeTag.INTERSECTION)
    if (leftTurn || rightTurn) add(PracticeTag.TURN)
    if (laneChange) add(PracticeTag.LANE_CHANGE)
    if (parking) add(PracticeTag.PARKING)
    if (highway) add(PracticeTag.HIGHWAY)
    if (highwayEntry) add(PracticeTag.HIGHWAY_ENTRY)
    if (nightDriving) add(PracticeTag.NIGHT)
    if (alley) add(PracticeTag.ALLEY)
}

/**
 * 연습 코스. 서버 포맷과 일치하며, UI용 computed property를 제공한다.
 *
 * MVP에서는 [com.cmc.routi.data.SampleCourses] 에 하드코딩한 데이터를 쓰고,
 * 추후 서버 데이터로 교체한다.
 */
data class Course(
    val id: String,
    val courseName: String,
    val courseNickname: String,
    val areaName: String,
    val region: String,              // "seoul" | "busan" | "daegu" | ...
    val difficulty: Int,             // 1 ~ 5
    val trafficDensity: String?,     // "낮음" | "보통" | "높음" | null
    val source: String,
    val sourceUrl: String,
    val crawledAt: String,
    val waypoints: List<Waypoint>,   // START + WAYPOINT* + END 순서
    val features: CourseFeatures,
    val recommendation: Int,         // 1 ~ 5
    val caution: String,
    val bestTime: String,
    val enrichedDescription: String,
) {
    // ── UI 편의 프로퍼티 ────────────────────────────────────────────────

    val title: String get() = courseName

    val regionDisplay: String get() {
        val cityName = when (region) {
            "seoul"    -> "서울"
            "busan"    -> "부산"
            "daegu"    -> "대구"
            "incheon"  -> "인천"
            "gwangju"  -> "광주"
            "daejeon"  -> "대전"
            "ulsan"    -> "울산"
            "sejong"   -> "세종"
            "gyeonggi" -> "경기"
            "gangwon"  -> "강원"
            "chungnam" -> "충남"
            "chungbuk" -> "충북"
            "jeonnam"  -> "전남"
            "jeonbuk"  -> "전북"
            "gyeongnam"-> "경남"
            "gyeongbuk"-> "경북"
            "jeju"     -> "제주"
            else       -> region
        }
        return "$cityName $areaName"
    }

    val difficultyEnum: Difficulty
        get() = Difficulty.entries.first { it.level == difficulty.coerceIn(1, 5) }

    val tags: Set<PracticeTag> get() = features.toPracticeTags()

    val summary: String get() = enrichedDescription

    val rating: Double get() = recommendation.toDouble()

    val likeCount: Int get() = recommendation * 20

    // ── 지점 접근자 ─────────────────────────────────────────────────────

    val startWaypoint: Waypoint
        get() = waypoints.first { it.type == WaypointType.START }

    val endWaypoint: Waypoint
        get() = waypoints.first { it.type == WaypointType.END }

    /** 지도·길찾기 렌더링용 출발지. */
    val origin: CoursePoint
        get() = startWaypoint.let { CoursePoint(it.name, it.lat, it.lng) }

    /** 지도·길찾기 렌더링용 목적지. */
    val destination: CoursePoint
        get() = endWaypoint.let { CoursePoint(it.name, it.lat, it.lng) }

    /** 경유지(WAYPOINT 타입만) — 길찾기 API용. */
    val waypointPoints: List<CoursePoint>
        get() = waypoints
            .filter { it.type == WaypointType.WAYPOINT }
            .sortedBy { it.order }
            .map { CoursePoint(it.name, it.lat, it.lng) }

    /** 출발→경유→도착 전체 지점 목록. */
    val allPoints: List<CoursePoint>
        get() = listOf(origin) + waypointPoints + destination

    val roadAddress: String get() = startWaypoint.address
    val jibunAddress: String get() = startWaypoint.jibunAddress
}
