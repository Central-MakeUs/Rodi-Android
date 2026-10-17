package com.dororong.rodi.core.ui.components.input

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dororong.rodi.core.ui.theme.RodiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w375dp-h812dp")
class RodiFieldLabelTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `필수 라벨은 제목과 보조 문구를 유지하며 필수 상태를 제공한다`() {
        rule.setContent {
            RodiTheme {
                RodiFieldLabel("도로주행 경험", required = true, assistiveText = "복수선택")
            }
        }

        val required = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "필수")
        rule.onNodeWithText("도로주행 경험", substring = true).assert(required)
        rule.onNodeWithText("복수선택", substring = true).assert(required)
        rule.onNodeWithText("도로주행 경험", substring = true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
    }

    @Test
    fun `선택 라벨로 바뀌면 필수 상태를 제거하고 라벨 텍스트를 유지한다`() {
        val required = mutableStateOf(true)
        rule.setContent {
            RodiTheme { RodiFieldLabel("연습 방법", required = required.value) }
        }
        rule.onNodeWithText("연습 방법", substring = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "필수"))

        rule.runOnIdle { required.value = false }

        rule.onNodeWithText("연습 방법", substring = true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }
}
