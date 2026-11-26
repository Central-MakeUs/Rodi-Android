package com.dororong.rodi.feature.entry.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dororong.rodi.core.domain.model.onboarding.DrivingPeriod
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile
import com.dororong.rodi.core.domain.model.onboarding.RoadExperience
import com.dororong.rodi.core.domain.model.onboarding.localOnboardingForm
import com.dororong.rodi.core.ui.theme.RodiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w375dp-h812dp")
class OnboardingQuestionsContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `서버 제목과 선택지 라벨을 표시하고 선택 코드를 전달한다`() {
        val original = localOnboardingForm()
        val question = original.steps.first().questions.first().let { question ->
            question.copy(title = "서버 질문", options = question.options.take(2).map {
                it.copy(label = "서버 ${it.code}")
            })
        }
        val step = original.steps.first().copy(title = "서버 단계 제목", questions = listOf(question))
        val form = original.copy(steps = listOf(step, original.steps.last()))
        var selected: Pair<String, String>? = null
        composeRule.setContent {
            RodiTheme {
                OnboardingQuestionsContent(form, step, OnboardingProfile(), { code, value -> selected = code to value }, {})
            }
        }

        composeRule.onNodeWithText("서버 단계 제목").assertIsDisplayed()
        composeRule.onNodeWithText("서버 질문", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("서버 MONTHS_1_2").performClick()

        assertEquals("drivingPeriod" to "MONTHS_1_2", selected)
    }

    @Test
    fun `조건을 충족하지 않은 혼자 운전 질문은 화면에 표시하지 않는다`() {
        val original = localOnboardingForm()
        val step = original.steps.first().copy(questions = original.steps.first().questions.filter {
            it.code == "roadExperiences" || it.code == "soloDrivingRange"
        })
        composeRule.setContent {
            RodiTheme {
                OnboardingQuestionsContent(original, step, OnboardingProfile(drivingPeriod = DrivingPeriod.MONTHS_1_2, roadExperiences = listOf(RoadExperience.NONE)), { _, _ -> }, {})
            }
        }

        composeRule.onNodeWithText("없음").assertIsDisplayed()
        composeRule.onNodeWithText("혼자 운전, 어디까지 해봤나요?", substring = true).assertDoesNotExist()
    }
}
