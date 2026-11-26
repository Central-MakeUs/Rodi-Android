package com.dororong.rodi.core.domain.model.onboarding

fun DrivingPeriod.answerCode(): String = when (this) {
    DrivingPeriod.UNDER_1_MONTH -> "UNDER_1_MONTH"
    DrivingPeriod.MONTHS_1_2 -> "MONTHS_1_2"
    DrivingPeriod.MONTHS_3_5 -> "MONTHS_3_5"
    DrivingPeriod.MONTHS_6_11 -> "MONTHS_6_11"
    DrivingPeriod.YEARS_1_2 -> "YEARS_1_2"
    DrivingPeriod.YEARS_3_9 -> "YEARS_3_9"
    DrivingPeriod.OVER_10_YEARS -> "OVER_10_YEARS"
}

fun RecentDrivingFrequency.answerCode(): String = when (this) {
    RecentDrivingFrequency.RARELY -> "RARELY"
    RecentDrivingFrequency.MONTHLY_1_TO_2 -> "MONTHLY_1_2"
    RecentDrivingFrequency.WEEKLY_1 -> "WEEKLY_1"
    RecentDrivingFrequency.WEEKLY_2_TO_3 -> "WEEKLY_2_3"
    RecentDrivingFrequency.WEEKLY_4_PLUS -> "WEEKLY_4_PLUS"
}

fun RoadExperience.answerCode(): String = when (this) {
    RoadExperience.NONE -> "NONE"
    RoadExperience.WITH_COMPANION -> "ACCOMPANIED"
    RoadExperience.PROFESSIONAL_LESSON -> "PROFESSIONAL_TRAINING"
    RoadExperience.SOLO -> "SOLO"
}

fun SoloDrivingRange.answerCode(): String = when (this) {
    SoloDrivingRange.NEAR_HOME -> "NEAR_HOME"
    SoloDrivingRange.FAMILIAR_ROAD -> "FAMILIAR_ROAD"
    SoloDrivingRange.UNFAMILIAR_ROAD -> "UNFAMILIAR_ROAD"
    SoloDrivingRange.HIGHWAY_LONG_DISTANCE -> "HIGHWAY_LONG"
}

fun SoloParkingLevel.answerCode(): String = when (this) {
    SoloParkingLevel.NONE -> "NONE"
    SoloParkingLevel.WIDE_SPACE_ONLY -> "WIDE_ONLY"
    SoloParkingLevel.FAMILIAR_SPOT -> "FAMILIAR_PLACE"
    SoloParkingLevel.MOSTLY_POSSIBLE -> "MOSTLY_POSSIBLE"
}

fun PracticeSituation.answerCode(): String = when (this) {
    PracticeSituation.U_TURN -> "U_TURN"
    PracticeSituation.TURN -> "LEFT_RIGHT_TURN"
    PracticeSituation.PARKING -> "PARKING"
    PracticeSituation.LANE_CHANGE -> "LANE_CHANGE"
    PracticeSituation.INTERSECTION -> "INTERSECTION"
    PracticeSituation.ROUNDABOUT -> "ROUNDABOUT"
    PracticeSituation.UNPROTECTED_LEFT_TURN -> "UNPROTECTED_LEFT_TURN"
    PracticeSituation.HIGHWAY_ENTRY -> "HIGHWAY_ENTRY"
    PracticeSituation.CORNERING -> "CORNERING"
    PracticeSituation.NARROW_ROAD -> "NARROW_ROAD"
    PracticeSituation.MULTI_LANE -> "MULTILANE"
    PracticeSituation.MERGING -> "MERGING"
    PracticeSituation.STRAIGHT -> "STRAIGHT"
}

fun VehicleType.answerCode(): String = when (this) {
    VehicleType.COMPACT -> "LIGHT"
    VehicleType.SMALL -> "COMPACT"
    VehicleType.MIDSIZE -> "MIDSIZE"
    VehicleType.SEMI_LARGE -> "SEMI_LARGE"
    VehicleType.LARGE -> "LARGE"
    VehicleType.SUV -> "SUV"
}
