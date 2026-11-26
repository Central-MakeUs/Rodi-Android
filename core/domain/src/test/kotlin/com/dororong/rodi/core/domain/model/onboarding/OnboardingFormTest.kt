package com.dororong.rodi.core.domain.model.onboarding

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OnboardingFormTest {
    private val form = localOnboardingForm()

    @Test
    fun `혼자 연습을 선택해야 조건부 질문이 보인다`() {
        val step = form.steps.first()
        assertFalse(form.visibleQuestions(step, OnboardingProfile()).any { it.code == "soloDrivingRange" })
        val profile = OnboardingProfile(roadExperiences = listOf(RoadExperience.SOLO))
        assertTrue(form.visibleQuestions(step, profile).any { it.code == "soloDrivingRange" })
        assertTrue(form.visibleQuestions(step, profile).any { it.code == "soloParkingLevel" })
    }

    @Test
    fun `후속 생략 선택지는 같은 단계의 뒤 문항을 숨긴다`() {
        val profile = OnboardingProfile(drivingPeriod = DrivingPeriod.YEARS_3_9)
        assertEquals(listOf("drivingPeriod"), form.visibleQuestions(form.steps.first(), profile).map { it.code })
        assertTrue(form.isStepValid(form.steps.first(), profile))
    }

    @Test
    fun `서버에서 선택으로 내린 문항은 미응답이어도 다음으로 갈 수 있다`() {
        val step = form.steps.first().let { step ->
            step.copy(questions = step.questions.map { it.copy(required = it.code == "drivingPeriod") })
        }
        assertTrue(form.isStepValid(step, OnboardingProfile(drivingPeriod = DrivingPeriod.MONTHS_1_2)))
        assertFalse(form.isStepValid(step, OnboardingProfile()))
    }

    @Test
    fun `복원된 혼합 도로 경험은 없음을 버리고 후속 응답을 유지한다`() {
        val profile = OnboardingProfile(
            roadExperiences = listOf(RoadExperience.NONE, RoadExperience.SOLO, RoadExperience.SOLO),
            soloDrivingRange = SoloDrivingRange.FAMILIAR_ROAD,
            soloParkingLevel = SoloParkingLevel.FAMILIAR_SPOT,
        ).normalizeRoadExperiences()
        assertEquals(listOf(RoadExperience.SOLO), profile.roadExperiences)
        assertEquals(SoloDrivingRange.FAMILIAR_ROAD, profile.soloDrivingRange)
        assertEquals(SoloParkingLevel.FAMILIAR_SPOT, profile.soloParkingLevel)
    }

    @Test
    fun `서버 선택지에서 빠진 값과 감춰진 후속 응답은 복원하지 않는다`() {
        val changedStep = form.steps.first().let { step ->
            step.copy(questions = step.questions.map { question ->
                if (question.code == "roadExperiences") question.copy(options = question.options.filter { it.code != "SOLO" })
                else question
            })
        }
        val changedForm = form.copy(steps = listOf(changedStep, form.steps.last()))
        val profile = OnboardingProfile(
            drivingPeriod = DrivingPeriod.MONTHS_1_2,
            roadExperiences = listOf(RoadExperience.SOLO),
            soloDrivingRange = SoloDrivingRange.HIGHWAY_LONG_DISTANCE,
        ).conformTo(changedForm)
        assertEquals(emptyList<RoadExperience>(), profile.roadExperiences)
        assertEquals(null, profile.soloDrivingRange)
    }

    @Test
    fun `순위 응답은 선택 순서를 보존하고 서버 최대 개수에 맞춰 복원한다`() {
        val step = form.steps.last().let { step ->
            step.copy(questions = step.questions.map { if (it.code == "practiceTypes") it.copy(maxSelect = 2) else it })
        }
        val changedForm = form.copy(steps = listOf(form.steps.first(), step))
        val profile = OnboardingProfile(practiceSituations = listOf(PracticeSituation.PARKING, PracticeSituation.TURN, PracticeSituation.U_TURN))
            .conformTo(changedForm)
        assertEquals(listOf(PracticeSituation.PARKING, PracticeSituation.TURN), profile.practiceSituations)
    }
}
