package com.dororong.rodi.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dororong.rodi.core.ui.theme.RodiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `skip action is available when recent login is not shown`() {
        var skipped = false
        composeRule.setContent {
            RodiTheme {
                LoginContent(
                    uiState = LoginUiState.Idle,
                    showRecentKakaoLogin = false,
                    onKakaoLoginClick = {},
                    onSkipClick = { skipped = true },
                )
            }
        }

        composeRule.onNodeWithText("둘러보기")
            .assertIsDisplayed()
            .performClick()

        assertTrue(skipped)
    }

    @Test
    fun `recent login shows tooltip instead of skip action`() {
        composeRule.setContent {
            RodiTheme {
                LoginContent(
                    uiState = LoginUiState.Idle,
                    showRecentKakaoLogin = true,
                    onKakaoLoginClick = {},
                    onSkipClick = {},
                )
            }
        }

        composeRule.onNodeWithText("최근에 로그인했어요!").assertIsDisplayed()
    }
}
