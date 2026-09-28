package com.dororong.rodi.feature.auth

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dororong.rodi.core.ui.theme.RodiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w375dp-h812dp")
class LoginContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `최근 로그인 안내가 없으면 둘러보기 버튼을 쓸 수 있다`() {
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
    fun `최근 로그인이면 둘러보기 대신 말풍선을 보여준다`() {
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
        composeRule.onAllNodesWithText("둘러보기").assertCountEquals(0)
    }
}
