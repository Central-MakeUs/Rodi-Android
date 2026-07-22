package com.dororong.rodi.core.domain.model.onboarding

enum class OnboardingLevel {
    SEED,
    ROOKIE,
    OWNER,
    EXPLORER,
    NAVIGATOR,
}

data class OnboardingAssessment(
    val score: Int,
    val level: OnboardingLevel,
    val isLevelForced: Boolean,
)

fun OnboardingProfile.calculateAssessment(): OnboardingAssessment {
    val score = drivingPeriod.score + recentFrequency.score + roadExperiences.maxOfOrNull { it.score }.orZero +
        soloDrivingRange.score + soloParkingLevel.score

    val isLevelForced = drivingPeriod?.isNavigatorLevel == true
    val level = if (isLevelForced) OnboardingLevel.NAVIGATOR else when (score) {
        in 0..2 -> OnboardingLevel.SEED
        in 3..5 -> OnboardingLevel.ROOKIE
        in 6..9 -> OnboardingLevel.OWNER
        else -> OnboardingLevel.EXPLORER
    }
    return OnboardingAssessment(score, level, isLevelForced)
}

fun OnboardingProfile.calculateLevel(): OnboardingLevel = calculateAssessment().level

val DrivingPeriod.isNavigatorLevel: Boolean
    get() = this == DrivingPeriod.YEAR_2_TO_10 || this == DrivingPeriod.OVER_YEAR_10

private val DrivingPeriod?.score: Int
    get() = when (this) {
        DrivingPeriod.MONTH_6_TO_YEAR_1,
        DrivingPeriod.YEAR_1_TO_2,
        -> 1
        else -> 0
    }

private val RecentDrivingFrequency?.score: Int
    get() = when (this) {
        RecentDrivingFrequency.WEEKLY_1,
        RecentDrivingFrequency.WEEKLY_2_TO_3,
        -> 1
        RecentDrivingFrequency.WEEKLY_4_PLUS -> 2
        else -> 0
    }

private val RoadExperience.score: Int
    get() = when (this) {
        RoadExperience.WITH_COMPANION -> 1
        RoadExperience.PROFESSIONAL_LESSON,
        RoadExperience.SOLO,
        -> 2
        RoadExperience.NONE -> 0
    }

private val SoloDrivingRange?.score: Int
    get() = when (this) {
        SoloDrivingRange.NEAR_HOME -> 1
        SoloDrivingRange.FAMILIAR_ROAD -> 2
        SoloDrivingRange.UNFAMILIAR_ROAD -> 4
        SoloDrivingRange.HIGHWAY_LONG_DISTANCE -> 5
        null -> 0
    }

private val SoloParkingLevel?.score: Int
    get() = when (this) {
        SoloParkingLevel.WIDE_SPACE_ONLY -> 1
        SoloParkingLevel.FAMILIAR_SPOT -> 2
        SoloParkingLevel.MOSTLY_POSSIBLE -> 4
        SoloParkingLevel.NONE,
        null,
        -> 0
    }

private val Int?.orZero: Int
    get() = this ?: 0
