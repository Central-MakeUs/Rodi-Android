package com.dororong.rodi.feature.course.registration

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.course.registration.content.CourseRegistrationTutorialContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CourseRegistrationTutorialContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun swipingAdvancesTutorialPageWithoutInventedButtons() {
        var page by mutableIntStateOf(0)
        composeRule.setContent {
            RodiTheme {
                CourseRegistrationTutorialContent(
                    page = page,
                    isCompleting = false,
                    onPageChanged = { page = it },
                    onBack = {},
                    onComplete = {},
                )
            }
        }

        composeRule.onNodeWithText("지도를 움직여 핀을 놓을 위치를 정하고").assertIsDisplayed()
        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("아래 ‘출발지 선택’을 눌러, 위치를 선택해요").assertIsDisplayed()
    }
}
