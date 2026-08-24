package com.dororong.rodi.core.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.theme.RodiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoreUiComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `clicking a button invokes its callback`() {
        var clicked = false
        composeRule.setContent {
            RodiTheme {
                RodiButton(text = "확인", onClick = { clicked = true })
            }
        }

        composeRule.onNodeWithText("확인").performClick()

        assertTrue(clicked)
    }

    @Test
    fun `clicking a selectable chip invokes its callback`() {
        var clicked = false
        composeRule.setContent {
            RodiTheme {
                RodiSelectableChip(text = "선택", selected = false, onClick = { clicked = true })
            }
        }

        composeRule.onNodeWithText("선택").performClick()

        assertTrue(clicked)
    }
}
