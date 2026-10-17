package com.dororong.rodi.feature.mypage.nickname

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dororong.rodi.feature.mypage.MyPageProfile
import com.dororong.rodi.feature.mypage.components.ProfileCard
import com.dororong.rodi.core.domain.model.member.NicknameRejection
import com.dororong.rodi.core.ui.theme.RodiTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NicknameContentTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `입력만으로 검증하지 않고 완료와 외부 탭에서 검증하며 취소 콜백을 호출한다`() {
        val state = mutableStateOf(NicknameUiState(nickname = "운전하는_고양이", initialNickname = "운전하는_고양이", isLoading = false))
        var checks = 0
        var backs = 0
        rule.setContent {
            RodiTheme {
                NicknameContent(
                    uiState = state.value,
                    onBack = { backs++ },
                    onNicknameChange = { state.value = state.value.copy(nickname = it) },
                    onValidate = { checks++ },
                    onSave = {},
                    onRetry = {},
                    modifier = Modifier.testTag("screen"),
                )
            }
        }
        rule.onNodeWithContentDescription("완료").assertIsNotEnabled()
        capture("default")
        rule.onNode(hasSetTextAction()).performTextReplacement("새닉네임")
        rule.runOnIdle { assertEquals(0, checks) }
        rule.onNode(hasSetTextAction()).performImeAction()
        rule.runOnIdle { assertEquals(1, checks) }
        rule.onNode(hasSetTextAction()).performClick()
        rule.onNodeWithTag("screen").performTouchInput { click(center.copy(y = height * 0.7f)) }
        rule.runOnIdle { assertEquals(2, checks) }
        rule.onNodeWithContentDescription("뒤로가기").performClick()
        rule.runOnIdle { assertEquals(1, backs) }
    }

    @Test
    fun `서버 검증 결과별 안내와 완료 버튼 상태를 렌더링한다`() {
        val state = mutableStateOf(NicknameUiState(nickname = "운전하는_고양이", isLoading = false))
        rule.setContent {
            RodiTheme { NicknameContent(state.value, {}, {}, {}, {}, {}) }
        }
        val scenarios = listOf(
            Triple(NicknameRejection.PROHIBITED_WORD, "사용할 수 없는 단어가 포함되어 있어요.", "prohibited"),
            Triple(NicknameRejection.DUPLICATE, "이미 사용 중인 닉네임이에요.", "duplicate"),
            Triple(NicknameRejection.LENGTH, "2자 이상 10자 이하만 사용 가능해요.", "length"),
        )
        for ((rejection, message, name) in scenarios) {
            rule.runOnIdle { state.value = state.value.copy(rejection = rejection, validatedNickname = state.value.nickname) }
            rule.onNodeWithText(message).assertIsDisplayed()
            rule.onNodeWithContentDescription("완료").assertIsNotEnabled()
            capture(name)
        }
        rule.runOnIdle {
            state.value = state.value.copy(rejection = null, normalizedNickname = state.value.nickname)
        }
        rule.onNodeWithText("사용 가능한 닉네임이에요.").assertIsDisplayed()
        rule.onNodeWithContentDescription("완료").assertIsEnabled()
        capture("available")
    }

    @Test
    fun `변경 제한 안내가 검증 결과보다 우선하고 완료와 검증 요청을 막으며 뒤로갈 수 있다`() {
        val state = mutableStateOf(
            NicknameUiState(
                initialNickname = "운전하는_고양이",
                nickname = "산뜻한_기린",
                validatedNickname = "산뜻한_기린",
                normalizedNickname = "산뜻한_기린",
                isLoading = false,
                isChangeRestricted = true,
            ),
        )
        var checks = 0
        var saves = 0
        var backs = 0
        rule.setContent {
            RodiTheme {
                NicknameContent(
                    uiState = state.value,
                    onBack = { backs++ },
                    onNicknameChange = { state.value = state.value.copy(nickname = it) },
                    onValidate = { checks++ },
                    onSave = { saves++ },
                    onRetry = {},
                )
            }
        }
        val restrictedHint = "닉네임은 30일 후에 다시 바꿀 수 있어요."
        rule.onNodeWithText(restrictedHint).assertIsDisplayed()
        rule.onNodeWithText("사용 가능한 닉네임이에요.").assertDoesNotExist()
        rule.onNodeWithContentDescription("완료").assertIsNotEnabled()
            .performTouchInput { click() }
        capture("restricted")
        rule.runOnIdle { state.value = state.value.copy(isChecking = true, rejection = NicknameRejection.DUPLICATE) }
        rule.onNodeWithText(restrictedHint).assertIsDisplayed()
        rule.onNodeWithText("닉네임을 확인하고 있어요.").assertDoesNotExist()
        rule.onNodeWithText("이미 사용 중인 닉네임이에요.").assertDoesNotExist()
        rule.onNode(hasSetTextAction()).performTextReplacement("교체_닉네임")
        rule.onNode(hasSetTextAction()).performImeAction()
        rule.onNodeWithContentDescription("뒤로가기").performClick()
        rule.runOnIdle {
            assertEquals("교체_닉네임", state.value.nickname)
            assertEquals(0, checks)
            assertEquals(0, saves)
            assertEquals(1, backs)
            state.value = state.value.copy(
                isChangeRestricted = false,
                isChecking = false,
                rejection = null,
                validatedNickname = state.value.nickname,
                normalizedNickname = state.value.nickname,
            )
        }
        rule.onNodeWithText("사용 가능한 닉네임이에요.").assertIsDisplayed()
        rule.onNodeWithContentDescription("완료").assertIsEnabled()
        rule.runOnIdle { state.value = state.value.copy(isChangeRestricted = true) }
        rule.onNodeWithText(restrictedHint).assertIsDisplayed()
        rule.onNodeWithContentDescription("완료").assertIsNotEnabled()
    }

    @Test
    fun `프로필 닉네임 텍스트와 화살표와 행 여백이 모두 수정 콜백을 호출한다`() {
        var clicks = 0
        rule.setContent {
            RodiTheme {
                ProfileCard(
                    profile = MyPageProfile(nickname = "흐름타는_고슴도치"),
                    onGoalClick = {},
                    onNicknameClick = { clicks++ },
                )
            }
        }
        rule.mainClock.advanceTimeBy(2000)
        rule.onNodeWithText("흐름타는_고슴도치").performClick()
        rule.onNodeWithContentDescription("닉네임 수정").performClick()
        rule.onNodeWithText("흐름타는_고슴도치").performTouchInput { click(center.copy(x = width * 0.85f)) }
        rule.runOnIdle { assertEquals(3, clicks) }
        capture("profile-row")
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val target = File(instrumentation.targetContext.getExternalFilesDir("nickname-design"), "$name.png")
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
