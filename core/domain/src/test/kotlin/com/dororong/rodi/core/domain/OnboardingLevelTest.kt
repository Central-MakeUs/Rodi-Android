package com.dororong.rodi.core.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OnboardingLevelTest {

    @Test
    fun `two years or more is always navigator`() {
        val profile = OnboardingProfile(drivingPeriod = DrivingPeriod.YEAR_2_TO_10)

        assertEquals(OnboardingLevel.NAVIGATOR, profile.calculateLevel())
    }

    @Test
    fun `highest road experience and solo answers determine score`() {
        val profile = OnboardingProfile(
            drivingPeriod = DrivingPeriod.YEAR_1_TO_2,
            recentFrequency = RecentDrivingFrequency.WEEKLY_2_TO_3,
            roadExperiences = listOf(RoadExperience.WITH_COMPANION, RoadExperience.SOLO),
            soloDrivingRange = SoloDrivingRange.UNFAMILIAR_ROAD,
            soloParkingLevel = SoloParkingLevel.FAMILIAR_SPOT,
        )

        assertEquals(OnboardingLevel.EXPLORER, profile.calculateLevel())
    }

    @Test
    fun `missing conditional answers are scored as zero`() {
        val profile = OnboardingProfile(
            drivingPeriod = DrivingPeriod.UNDER_1_MONTH,
            recentFrequency = RecentDrivingFrequency.RARELY,
            roadExperiences = listOf(RoadExperience.NONE),
        )

        assertEquals(OnboardingLevel.SEED, profile.calculateLevel())
    }
}
