package com.dororong.rodi.feature.entry.content

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
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
class TermsAgreementContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `약관을 모두 선택하면 다음 버튼이 활성화된다`() {
        var allChecked by mutableStateOf(false)
        var nextClicked = false
        composeRule.setContent {
            RodiTheme {
                TermsAgreementContent(
                    service = allChecked,
                    privacy = allChecked,
                    location = allChecked,
                    onAllToggle = { allChecked = it },
                    onServiceToggle = {},
                    onPrivacyToggle = {},
                    onLocationToggle = {},
                    onBack = null,
                    onNext = { nextClicked = true },
                    onTermsClick = {},
                )
            }
        }

        composeRule.onNodeWithText("다음").assertIsNotEnabled()

        composeRule.onNodeWithText("약관 전체 동의").performClick()
        composeRule.onNodeWithText("다음").assertIsEnabled().performClick()

        assertTrue(nextClicked)
    }
}
