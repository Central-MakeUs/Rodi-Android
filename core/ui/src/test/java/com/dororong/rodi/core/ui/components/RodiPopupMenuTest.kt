package com.dororong.rodi.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dororong.rodi.core.ui.theme.RodiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w375dp-h812dp")
class RodiPopupMenuTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `항목을 고르면 해당 인덱스로 onSelect를 호출하고 메뉴를 닫는다`() {
        var expanded by mutableStateOf(true)
        var selectedIndex by mutableStateOf(-1)

        composeRule.setContent {
            RodiTheme {
                Box(Modifier.fillMaxSize()) {
                    RodiPopupMenu(
                        expanded = expanded,
                        items = listOf("전체", "새싹", "가지", "나무"),
                        onSelect = { index ->
                            selectedIndex = index
                            expanded = false
                        },
                        onDismissRequest = { expanded = false },
                    )
                }
            }
        }

        composeRule.onNodeWithText("새싹").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertEquals(1, selectedIndex)
        composeRule.onNodeWithText("새싹").assertDoesNotExist()
    }

    @Test
    fun `expanded가 false면 메뉴를 보여주지 않는다`() {
        composeRule.setContent {
            RodiTheme {
                Box(Modifier.fillMaxSize()) {
                    RodiPopupMenu(
                        expanded = false,
                        items = listOf("전체", "새싹", "가지", "나무"),
                        onSelect = {},
                        onDismissRequest = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("전체").assertDoesNotExist()
    }

    @Test
    fun `expanded가 false에서 true로 바뀌면 메뉴가 나타난다`() {
        var expanded by mutableStateOf(false)

        composeRule.setContent {
            RodiTheme {
                Box(Modifier.fillMaxSize()) {
                    RodiPopupMenu(
                        expanded = expanded,
                        items = listOf("전체", "새싹", "가지", "나무"),
                        onSelect = {},
                        onDismissRequest = { expanded = false },
                    )
                }
            }
        }

        composeRule.onNodeWithText("전체").assertDoesNotExist()

        expanded = true
        composeRule.waitForIdle()

        composeRule.onNodeWithText("전체").assertIsDisplayed()
    }
}
