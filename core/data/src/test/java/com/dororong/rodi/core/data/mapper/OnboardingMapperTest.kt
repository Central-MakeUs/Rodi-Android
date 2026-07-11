package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.domain.model.onboarding.DrivingPeriod
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.PracticeSituation
import com.dororong.rodi.core.domain.model.onboarding.RecentDrivingFrequency
import com.dororong.rodi.core.domain.model.onboarding.RoadExperience
import com.dororong.rodi.core.domain.model.onboarding.SoloDrivingRange
import com.dororong.rodi.core.domain.model.onboarding.SoloParkingLevel
import com.dororong.rodi.core.domain.model.onboarding.VehicleType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OnboardingMapperTest {
    @Test
    fun `maps onboarding profile to server enum values`() {
        val request = OnboardingProfile(
            drivingPeriod = DrivingPeriod.MONTH_1_TO_3,
            recentFrequency = RecentDrivingFrequency.WEEKLY_2_TO_3,
            roadExperiences = listOf(RoadExperience.WITH_COMPANION),
            soloDrivingRange = SoloDrivingRange.HIGHWAY_LONG_DISTANCE,
            soloParkingLevel = SoloParkingLevel.WIDE_SPACE_ONLY,
            practiceSituations = listOf(PracticeSituation.TURN, PracticeSituation.MULTI_LANE),
            vehicleType = VehicleType.COMPACT,
            goal = "출퇴근",
        ).toRequest(OnboardingLevel.ROOKIE)

        assertEquals("MONTHS_1_3", request.drivingPeriod)
        assertEquals("WEEKLY_2_3", request.recentFrequency)
        assertEquals(listOf("ACCOMPANIED"), request.roadExperiences)
        assertEquals("HIGHWAY_LONG", request.soloDrivingRange)
        assertEquals("WIDE_ONLY", request.soloParkingLevel)
        assertEquals(listOf("LEFT_RIGHT_TURN", "MULTILANE"), request.practiceTypes)
        assertEquals("LIGHT", request.carType)
        assertEquals("출퇴근", request.drivingGoal)
    }
}
