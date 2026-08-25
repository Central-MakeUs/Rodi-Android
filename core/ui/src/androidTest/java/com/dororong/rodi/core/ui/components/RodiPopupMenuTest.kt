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

@RunWith(AndroidJUnit4::class)
class RodiPopupMenuTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `selecting an item invokes onSelect with its index and closes the menu`() {
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
    fun `menu is not shown when expanded is false`() {
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
    fun `menu appears when expanded toggles from false to true`() {
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
