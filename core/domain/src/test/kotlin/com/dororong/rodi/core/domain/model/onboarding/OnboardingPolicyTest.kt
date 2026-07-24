package com.dororong.rodi.core.domain.model.onboarding

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OnboardingPolicyTest {
    @Test
    fun `all distance frequency and non navigator level combinations use policy copy`() {
        val distanceProfiles = mapOf(
            "혼자서" to OnboardingProfile(roadExperiences = listOf(RoadExperience.WITH_COMPANION)),
            "집 근처" to soloProfile(SoloDrivingRange.NEAR_HOME),
            "자주 다니는 코스" to soloProfile(SoloDrivingRange.FAMILIAR_ROAD),
            "낯선 길까지" to soloProfile(SoloDrivingRange.UNFAMILIAR_ROAD),
            "멀리까지" to soloProfile(SoloDrivingRange.HIGHWAY_LONG_DISTANCE),
        )
        val frequencies = mapOf(
            RecentDrivingFrequency.RARELY to "잘 안",
            RecentDrivingFrequency.MONTHLY_1_TO_2 to "가끔",
            RecentDrivingFrequency.WEEKLY_1 to "종종",
            RecentDrivingFrequency.WEEKLY_2_TO_3 to "자주",
            RecentDrivingFrequency.WEEKLY_4_PLUS to "거의 매일",
        )
        val stageSentences = mapOf(
            OnboardingLevel.SEED to "도로에서 직접 핸들 잡는 게 아직 낯설어요.",
            OnboardingLevel.ROOKIE to "교차로·유턴이 아직 긴장돼요.",
            OnboardingLevel.OWNER to "고속도로 합류·다차로 주행이 아직 어려워요.",
            OnboardingLevel.EXPLORER to "더 다양한 상황들을 연습하고 싶어요.",
        )

        distanceProfiles.forEach { (distance, profile) ->
            frequencies.forEach { (frequency, frequencyCopy) ->
                stageSentences.forEach { (level, stageSentence) ->
                    val copy = profile.copy(recentFrequency = frequency).analysisCopy(level)
                    assertEquals("${distance}는 $frequencyCopy 나가는데,\n$stageSentence", copy.text)
                }
            }
        }
    }

    @Test
    fun `analysis copy combines distance frequency and level sentence`() {
        val profile = OnboardingProfile(
            drivingPeriod = DrivingPeriod.MONTHS_1_2,
            recentFrequency = RecentDrivingFrequency.MONTHLY_1_TO_2,
            roadExperiences = listOf(RoadExperience.SOLO),
            soloDrivingRange = SoloDrivingRange.NEAR_HOME,
        )

        assertEquals(
            "집 근처는 가끔 나가는데,\n도로에서 직접 핸들 잡는 게 아직 낯설어요.",
            profile.analysisCopy(OnboardingLevel.SEED).text,
        )
    }

    @Test
    fun `navigator uses fixed copy and activity recommendations`() {
        val copy = OnboardingProfile(drivingPeriod = DrivingPeriod.OVER_10_YEARS)
            .analysisCopy(OnboardingLevel.NAVIGATOR)

        assertEquals(null, copy.distanceExpression)
        assertEquals(listOf("코스 등록", "리뷰 작성", "추천 코스 공유"), OnboardingLevel.NAVIGATOR.recommendations)
    }

    @Test
    fun `recommendations follow canonical level policy`() {
        assertEquals(listOf("직선주행", "좌우회전", "차선변경"), OnboardingLevel.SEED.recommendations)
        assertEquals(listOf("유턴", "교차로", "주차"), OnboardingLevel.ROOKIE.recommendations)
        assertEquals(listOf("고속도로", "합류", "다차로주행"), OnboardingLevel.OWNER.recommendations)
        assertEquals(listOf("비보호좌회전", "회전교차로", "좁은도로", "코너링"), OnboardingLevel.EXPLORER.recommendations)
    }

    private fun soloProfile(range: SoloDrivingRange) = OnboardingProfile(
        roadExperiences = listOf(RoadExperience.SOLO),
        soloDrivingRange = range,
    )
}
