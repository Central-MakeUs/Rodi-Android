package com.dororong.rodi.feature.home.filter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dororong.rodi.core.ui.theme.RodiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w375dp-h812dp")
class FilterBottomSheetDismissTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var dismissCount = 0

    @Test
    fun `저장 중 뒤로가기를 누르면 시트가 닫히지 않는다`() {
        setSheet(isSaving = true)

        Espresso.pressBackUnconditionally()
        composeRule.mainClock.advanceTimeBy(ANIMATION_SETTLE_MS)
        composeRule.waitForIdle()

        assertEquals(0, dismissCount)
        composeRule.onNodeWithText("결과보기").assertIsDisplayed()
    }

    @Test
    fun `저장 중 아래로 밀어도 시트가 닫히지 않는다`() {
        setSheet(isSaving = true)

        composeRule.onNodeWithText("필터").performTouchInput { swipeDown() }
        composeRule.mainClock.advanceTimeBy(ANIMATION_SETTLE_MS)
        composeRule.waitForIdle()

        assertEquals(0, dismissCount)
        composeRule.onNodeWithText("결과보기").assertIsDisplayed()
    }

    @Test
    fun `저장이 끝나면 뒤로가기로 시트가 닫힌다`() {
        var isSaving by mutableStateOf(true)
        setSheet(isSaving = { isSaving })

        Espresso.pressBackUnconditionally()
        composeRule.mainClock.advanceTimeBy(ANIMATION_SETTLE_MS)
        composeRule.waitForIdle()
        isSaving = false
        composeRule.waitForIdle()
        Espresso.pressBackUnconditionally()
        composeRule.mainClock.advanceTimeBy(ANIMATION_SETTLE_MS)
        composeRule.waitForIdle()

        assertEquals(1, dismissCount)
    }

    @Test
    fun `저장 중이 아니면 뒤로가기로 시트가 닫힌다`() {
        setSheet(isSaving = false)

        Espresso.pressBackUnconditionally()
        composeRule.mainClock.advanceTimeBy(ANIMATION_SETTLE_MS)
        composeRule.waitForIdle()

        assertEquals(1, dismissCount)
    }

    private fun setSheet(isSaving: Boolean) = setSheet { isSaving }

    private fun setSheet(isSaving: () -> Boolean) {
        composeRule.setContent {
            RodiTheme {
                FilterBottomSheet(
                    activeCategory = null,
                    selectedPracticeTypes = emptySet(),
                    onCategorySelect = {},
                    onPracticeOptionToggle = {},
                    onReset = {},
                    onApply = {},
                    onDismiss = { dismissCount++ },
                    isSaving = isSaving(),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("결과보기").assertIsDisplayed()
    }

    private companion object {
        const val ANIMATION_SETTLE_MS = 2_000L
    }
}
