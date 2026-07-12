package com.dororong.rodi.core.domain.model.onboarding

enum class DrivingPeriod(val label: String) {
    UNDER_1_MONTH("1개월 미만"),
    MONTH_1_TO_3("1~3개월"),
    MONTH_3_TO_6("3~6개월"),
    MONTH_6_TO_YEAR_1("6~1년"),
    YEAR_1_TO_2("1~2년"),
    YEAR_2_TO_10("2~10년"),
    OVER_YEAR_10("10년 이상"),
}

enum class RecentDrivingFrequency(val label: String) {
    RARELY("거의 없음"),
    MONTHLY_1_TO_2("월 1~2회"),
    WEEKLY_1("주 1회"),
    WEEKLY_2_TO_3("주 2~3회"),
    WEEKLY_4_PLUS("주 4회 이상"),
}

enum class RoadExperience(val label: String) {
    NONE("없음"),
    WITH_COMPANION("동승 연습"),
    PROFESSIONAL_LESSON("전문 도로 연수"),
    SOLO("혼자 연습"),
}

enum class SoloDrivingRange(val label: String) {
    NEAR_HOME("집 근처"),
    FAMILIAR_ROAD("익숙한 길"),
    UNFAMILIAR_ROAD("낯선 도로"),
    HIGHWAY_LONG_DISTANCE("고속·장거리"),
}

enum class SoloParkingLevel(val label: String) {
    NONE("없음"),
    WIDE_SPACE_ONLY("넓은 곳만"),
    FAMILIAR_SPOT("익숙한 곳"),
    MOSTLY_POSSIBLE("대부분 가능"),
}

enum class PracticeSituation(val label: String) {
    U_TURN("유턴"),
    TURN("좌우 회전"),
    PARKING("주차"),
    LANE_CHANGE("차선변경"),
    INTERSECTION("교차로"),
    ROUNDABOUT("회전 교차로"),
    UNPROTECTED_LEFT_TURN("비보호 좌회전"),
    HIGHWAY_ENTRY("고속진입"),
    CORNERING("코너링"),
    NARROW_ROAD("좁은 도로 주행"),
    MULTI_LANE("다차로 주행"),
    MERGING("합류"),
    STRAIGHT("직선주행"),
}

enum class VehicleType(val label: String) {
    COMPACT("경차"),
    SMALL("소형차"),
    MIDSIZE("중형차"),
    SEMI_LARGE("준대형"),
    LARGE("대형차"),
    SUV("SUV"),
}

/** 온보딩 설문 응답. 점수 계산/서버 전송은 미확정이라 이번엔 로컬 저장까지만 한다. */
data class OnboardingProfile(
    val nickname: String = "",
    val drivingPeriod: DrivingPeriod? = null,
    val recentFrequency: RecentDrivingFrequency? = null,
    val roadExperiences: List<RoadExperience> = emptyList(),
    val soloDrivingRange: SoloDrivingRange? = null,
    val soloParkingLevel: SoloParkingLevel? = null,
    val practiceSituations: List<PracticeSituation> = emptyList(),
    val vehicleType: VehicleType? = null,
    val goal: String = "",
)
