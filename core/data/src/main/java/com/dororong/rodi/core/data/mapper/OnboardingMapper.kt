package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.onboarding.OnboardingRequest
import com.dororong.rodi.core.domain.model.onboarding.DrivingPeriod
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.PracticeSituation
import com.dororong.rodi.core.domain.model.onboarding.RecentDrivingFrequency
import com.dororong.rodi.core.domain.model.onboarding.RoadExperience
import com.dororong.rodi.core.domain.model.onboarding.SoloDrivingRange
import com.dororong.rodi.core.domain.model.onboarding.SoloParkingLevel
import com.dororong.rodi.core.domain.model.onboarding.VehicleType

fun OnboardingProfile.toRequest(level: OnboardingLevel): OnboardingRequest = OnboardingRequest(
    drivingPeriod = requireNotNull(drivingPeriod).toApiValue(),
    recentFrequency = recentFrequency?.toApiValue(),
    roadExperiences = roadExperiences.takeIf { it.isNotEmpty() }?.map { it.toApiValue() },
    soloDrivingRange = soloDrivingRange?.toApiValue(),
    soloParkingLevel = soloParkingLevel?.toApiValue(),
    level = level.name,
    practiceTypes = practiceSituations.map { it.toApiValue() },
    carType = vehicleType?.toApiValue(),
    drivingGoal = goal.ifBlank { null },
)

private fun DrivingPeriod.toApiValue(): String = when (this) {
    DrivingPeriod.UNDER_1_MONTH -> "UNDER_1_MONTH"
    DrivingPeriod.MONTH_1_TO_3 -> "MONTHS_1_3"
    DrivingPeriod.MONTH_3_TO_6 -> "MONTHS_3_6"
    DrivingPeriod.MONTH_6_TO_YEAR_1 -> "MONTHS_6_12"
    DrivingPeriod.YEAR_1_TO_2 -> "YEARS_1_2"
    DrivingPeriod.YEAR_2_TO_10 -> "YEARS_2_10"
    DrivingPeriod.OVER_YEAR_10 -> "OVER_10_YEARS"
}

private fun RecentDrivingFrequency.toApiValue(): String = when (this) {
    RecentDrivingFrequency.RARELY -> "RARELY"
    RecentDrivingFrequency.MONTHLY_1_TO_2 -> "MONTHLY_1_2"
    RecentDrivingFrequency.WEEKLY_1 -> "WEEKLY_1"
    RecentDrivingFrequency.WEEKLY_2_TO_3 -> "WEEKLY_2_3"
    RecentDrivingFrequency.WEEKLY_4_PLUS -> "WEEKLY_4_PLUS"
}

private fun RoadExperience.toApiValue(): String = when (this) {
    RoadExperience.NONE -> "NONE"
    RoadExperience.WITH_COMPANION -> "ACCOMPANIED"
    RoadExperience.PROFESSIONAL_LESSON -> "PROFESSIONAL_TRAINING"
    RoadExperience.SOLO -> "SOLO"
}

private fun SoloDrivingRange.toApiValue(): String = when (this) {
    SoloDrivingRange.NEAR_HOME -> "NEAR_HOME"
    SoloDrivingRange.FAMILIAR_ROAD -> "FAMILIAR_ROAD"
    SoloDrivingRange.UNFAMILIAR_ROAD -> "UNFAMILIAR_ROAD"
    SoloDrivingRange.HIGHWAY_LONG_DISTANCE -> "HIGHWAY_LONG"
}

private fun SoloParkingLevel.toApiValue(): String = when (this) {
    SoloParkingLevel.NONE -> "NONE"
    SoloParkingLevel.WIDE_SPACE_ONLY -> "WIDE_ONLY"
    SoloParkingLevel.FAMILIAR_SPOT -> "FAMILIAR_PLACE"
    SoloParkingLevel.MOSTLY_POSSIBLE -> "MOSTLY_POSSIBLE"
}

private fun PracticeSituation.toApiValue(): String = when (this) {
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

private fun VehicleType.toApiValue(): String = when (this) {
    VehicleType.COMPACT -> "LIGHT"
    VehicleType.SMALL -> "COMPACT"
    VehicleType.MIDSIZE -> "MIDSIZE"
    VehicleType.SEMI_LARGE -> "SEMI_LARGE"
    VehicleType.LARGE -> "LARGE"
    VehicleType.SUV -> "SUV"
}
