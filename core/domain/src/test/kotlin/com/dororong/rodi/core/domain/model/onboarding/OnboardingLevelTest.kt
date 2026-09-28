package com.dororong.rodi.core.domain.model.onboarding

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OnboardingLevelTest {

    @Test
    fun `운전 경력 선택지는 승인된 순서와 문구를 따른다`() {
        assertEquals(
            listOf(
                "UNDER_1_MONTH" to "1개월 미만",
                "MONTHS_1_2" to "1~2개월",
                "MONTHS_3_5" to "3~5개월",
                "MONTHS_6_11" to "6~11개월",
                "YEARS_1_2" to "1~2년",
                "YEARS_3_9" to "3~9년",
                "OVER_10_YEARS" to "10년 이상",
            ),
            DrivingPeriod.entries.map { it.name to it.label },
        )
    }

    @Test
    fun `점수 경계값을 내비게이터가 아닌 레벨에 정확히 매핑한다`() {
        val profilesByExpectedScore = mapOf(
            0 to OnboardingProfile(),
            2 to OnboardingProfile(recentFrequency = RecentDrivingFrequency.WEEKLY_4_PLUS),
            3 to OnboardingProfile(
                recentFrequency = RecentDrivingFrequency.WEEKLY_4_PLUS,
                soloDrivingRange = SoloDrivingRange.NEAR_HOME,
            ),
            5 to OnboardingProfile(soloDrivingRange = SoloDrivingRange.HIGHWAY_LONG_DISTANCE),
            6 to OnboardingProfile(
                drivingPeriod = DrivingPeriod.MONTHS_6_11,
                soloDrivingRange = SoloDrivingRange.HIGHWAY_LONG_DISTANCE,
            ),
            9 to OnboardingProfile(
                soloDrivingRange = SoloDrivingRange.HIGHWAY_LONG_DISTANCE,
                soloParkingLevel = SoloParkingLevel.MOSTLY_POSSIBLE,
            ),
            10 to OnboardingProfile(
                drivingPeriod = DrivingPeriod.MONTHS_6_11,
                soloDrivingRange = SoloDrivingRange.HIGHWAY_LONG_DISTANCE,
                soloParkingLevel = SoloParkingLevel.MOSTLY_POSSIBLE,
            ),
            14 to OnboardingProfile(
                drivingPeriod = DrivingPeriod.MONTHS_6_11,
                recentFrequency = RecentDrivingFrequency.WEEKLY_4_PLUS,
                roadExperiences = listOf(RoadExperience.SOLO),
                soloDrivingRange = SoloDrivingRange.HIGHWAY_LONG_DISTANCE,
                soloParkingLevel = SoloParkingLevel.MOSTLY_POSSIBLE,
            ),
        )
        val expectedLevels = mapOf(
            0 to OnboardingLevel.SEED,
            2 to OnboardingLevel.SEED,
            3 to OnboardingLevel.ROOKIE,
            5 to OnboardingLevel.ROOKIE,
            6 to OnboardingLevel.OWNER,
            9 to OnboardingLevel.OWNER,
            10 to OnboardingLevel.EXPLORER,
            14 to OnboardingLevel.EXPLORER,
        )

        profilesByExpectedScore.forEach { (score, profile) ->
            val assessment = profile.calculateAssessment()
            assertEquals(score, assessment.score)
            assertEquals(expectedLevels.getValue(score), assessment.level)
        }
    }

    @Test
    fun `운전 경력 3년 이상은 항상 내비게이터다`() {
        val periods = listOf(DrivingPeriod.YEARS_3_9, DrivingPeriod.OVER_10_YEARS)

        periods.forEach { period ->
            val assessment = OnboardingProfile(drivingPeriod = period).calculateAssessment()
            assertEquals(OnboardingLevel.NAVIGATOR, assessment.level)
            assertEquals(true, assessment.isLevelForced)
        }
    }

    @Test
    fun `운전 경력별 승인된 점수와 레벨 고정 정책을 적용한다`() {
        val expected = mapOf(
            DrivingPeriod.UNDER_1_MONTH to (0 to false),
            DrivingPeriod.MONTHS_1_2 to (0 to false),
            DrivingPeriod.MONTHS_3_5 to (0 to false),
            DrivingPeriod.MONTHS_6_11 to (1 to false),
            DrivingPeriod.YEARS_1_2 to (1 to false),
            DrivingPeriod.YEARS_3_9 to (0 to true),
            DrivingPeriod.OVER_10_YEARS to (0 to true),
        )

        expected.forEach { (period, policy) ->
            val assessment = OnboardingProfile(drivingPeriod = period).calculateAssessment()
            assertEquals(policy.first, assessment.score)
            assertEquals(policy.second, assessment.isLevelForced)
            assertEquals(
                if (policy.second) OnboardingLevel.NAVIGATOR else OnboardingLevel.SEED,
                assessment.level,
            )
        }
    }

    @Test
    fun `가장 높은 도로 경험과 단독 운전 답변으로 점수를 정한다`() {
        val profile = OnboardingProfile(
            drivingPeriod = DrivingPeriod.YEARS_1_2,
            recentFrequency = RecentDrivingFrequency.WEEKLY_2_TO_3,
            roadExperiences = listOf(RoadExperience.WITH_COMPANION, RoadExperience.SOLO),
            soloDrivingRange = SoloDrivingRange.UNFAMILIAR_ROAD,
            soloParkingLevel = SoloParkingLevel.FAMILIAR_SPOT,
        )

        assertEquals(OnboardingLevel.EXPLORER, profile.calculateLevel())
        assertEquals(10, profile.calculateAssessment().score)
    }

    @Test
    fun `조건부 답변이 없으면 0점으로 계산한다`() {
        val profile = OnboardingProfile(
            drivingPeriod = DrivingPeriod.UNDER_1_MONTH,
            recentFrequency = RecentDrivingFrequency.RARELY,
            roadExperiences = listOf(RoadExperience.NONE),
        )

        assertEquals(OnboardingLevel.SEED, profile.calculateLevel())
    }
}
