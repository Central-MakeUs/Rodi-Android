package com.dororong.rodi.feature.mypage.nickname

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dororong.rodi.core.domain.model.member.MyPage
import com.dororong.rodi.core.domain.model.member.NicknameAvailability
import com.dororong.rodi.core.domain.model.member.NicknameRejectedException
import com.dororong.rodi.core.domain.model.member.NicknameRejection
import com.dororong.rodi.core.domain.model.member.NicknameUpdate
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.usecase.member.CheckNicknameAvailabilityUseCase
import com.dororong.rodi.core.domain.usecase.member.GetMyPageUseCase
import com.dororong.rodi.core.domain.usecase.member.UpdateNicknameUseCase
import com.dororong.rodi.core.ui.theme.RodiTheme
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.File
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w375dp-h812dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NicknameContentTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val getMyPage = mockk<GetMyPageUseCase>()
    private val check = mockk<CheckNicknameAvailabilityUseCase>()
    private val update = mockk<UpdateNicknameUseCase>()
    private val restriction = LocalDateTime.of(2000, 1, 2, 3, 4)

    @Test
    fun `서버 제한 중에도 입력과 검증과 뒤로가기는 허용하고 완료 터치는 막는다`() {
        coEvery { getMyPage() } returns Result.success(page(restriction))
        coEvery { check("Different") } returns Result.success(
            NicknameAvailability("Different", false, NicknameRejection.CHANGE_RESTRICTED, restriction),
        )
        val vm = NicknameViewModel(getMyPage, check, update)
        var backs = 0
        rule.setContent { RodiTheme { NicknameScreen(onBack = { backs++ }, viewModel = vm) } }

        rule.onNodeWithText(GUIDANCE).assertIsDisplayed()
        rule.onNode(hasSetTextAction()).performTextReplacement("Different")
        rule.onNode(hasSetTextAction()).performImeAction()
        rule.waitForIdle()
        coVerify(exactly = 1) { check("Different") }
        assertTrue(vm.uiState.value.isChangeRestricted)
        assertEquals(restriction, vm.uiState.value.nicknameChangeableAt)
        rule.onNodeWithText(GUIDANCE).assertIsDisplayed()
        rule.onNodeWithContentDescription("완료").assertIsNotEnabled().performTouchInput { click() }
        coVerify(exactly = 0) { update(any()) }
        capture("restricted")

        rule.onNodeWithContentDescription("뒤로가기").performTouchInput { click() }
        rule.runOnIdle {
            assertEquals(1, backs)
            assertEquals("Different", vm.uiState.value.nickname)
        }
    }

    @Test
    fun `제한 중 검증된 대소문자 변경을 완료 터치로 저장하고 서버 시각을 유지하며 복귀한다`() {
        coEvery { getMyPage() } returns Result.success(page(restriction))
        coEvery { check("rodi_user") } returns Result.success(NicknameAvailability("rodi_user", true, null))
        coEvery { update("rodi_user") } returns Result.success(NicknameUpdate("rodi_user", restriction))
        val vm = NicknameViewModel(getMyPage, check, update)
        var backs = 0
        rule.setContent { RodiTheme { NicknameScreen(onBack = { backs++ }, viewModel = vm) } }

        rule.onNode(hasSetTextAction()).performTextReplacement("rodi_user")
        rule.onNode(hasSetTextAction()).performImeAction()
        rule.waitForIdle()
        assertTrue(vm.uiState.value.isChangeRestricted)
        assertEquals(restriction, vm.uiState.value.nicknameChangeableAt)
        rule.onNodeWithText("사용 가능한 닉네임이에요.").assertIsDisplayed()
        rule.onNodeWithText(GUIDANCE).assertDoesNotExist()
        rule.onNodeWithContentDescription("완료").assertIsEnabled()
        capture("case-only")

        rule.onNode(hasSetTextAction()).performClick().performTextReplacement("Different")
        rule.onNodeWithText(GUIDANCE).assertIsDisplayed()
        rule.onNodeWithContentDescription("완료").assertIsNotEnabled()
        assertEquals(restriction, vm.uiState.value.nicknameChangeableAt)
        rule.onNode(hasSetTextAction()).performTextReplacement("rodi_user")
        rule.onNode(hasSetTextAction()).performImeAction()
        rule.onNodeWithText("사용 가능한 닉네임이에요.").assertIsDisplayed()
        rule.onNodeWithContentDescription("완료").assertIsEnabled().performTouchInput { click() }

        rule.waitUntil { vm.uiState.value.saveSucceeded && backs == 1 }
        coVerify(exactly = 1) { update("rodi_user") }
        rule.runOnIdle {
            assertEquals("rodi_user", vm.uiState.value.nickname)
            assertEquals(restriction, vm.uiState.value.nicknameChangeableAt)
            assertEquals(1, backs)
        }
    }

    @Test
    fun `새 후보의 서버 허용으로 제한을 해제하고 완료 터치 후 서버 이름과 시각을 보존한다`() {
        val nextRestriction = LocalDateTime.of(2026, 11, 27, 10, 15)
        coEvery { getMyPage() } returns Result.success(page(restriction))
        coEvery { check("Different") } returns Result.success(NicknameAvailability("Different", true, null))
        coEvery { update("Different") } returns Result.success(NicknameUpdate("Canonical", nextRestriction))
        val vm = NicknameViewModel(getMyPage, check, update)
        var backs = 0
        rule.setContent { RodiTheme { NicknameScreen(onBack = { backs++ }, viewModel = vm) } }

        rule.onNode(hasSetTextAction()).performTextReplacement("Different")
        rule.onNode(hasSetTextAction()).performImeAction()
        rule.waitForIdle()
        assertFalse(vm.uiState.value.isChangeRestricted)
        assertEquals(null, vm.uiState.value.nicknameChangeableAt)
        rule.onNodeWithText("사용 가능한 닉네임이에요.").assertIsDisplayed()
        rule.onNodeWithContentDescription("완료").assertIsEnabled()
        capture("released")
        rule.onNodeWithContentDescription("완료").performTouchInput { click() }

        rule.waitUntil { vm.uiState.value.saveSucceeded && backs == 1 }
        rule.runOnIdle {
            assertEquals("Canonical", vm.uiState.value.nickname)
            assertEquals(nextRestriction, vm.uiState.value.nicknameChangeableAt)
            assertTrue(vm.uiState.value.isChangeRestricted)
            assertEquals(1, backs)
        }
    }

    @Test
    fun `저장의 제한 거절은 일반 오류 없이 안내하고 같은 후보를 다시 검증할 수 있다`() {
        coEvery { getMyPage() } returns Result.success(page(null))
        coEvery { check("Different") } returns Result.success(NicknameAvailability("Different", true, null))
        coEvery { update("Different") } returns Result.failure(
            NicknameRejectedException(NicknameRejection.CHANGE_RESTRICTED, restriction),
        )
        val vm = NicknameViewModel(getMyPage, check, update)
        rule.setContent { RodiTheme { NicknameScreen(onBack = {}, viewModel = vm) } }
        rule.onNode(hasSetTextAction()).performTextReplacement("Different")
        rule.onNode(hasSetTextAction()).performImeAction()
        rule.onNodeWithContentDescription("완료").assertIsEnabled().performTouchInput { click() }

        rule.onNodeWithText(GUIDANCE).assertIsDisplayed()
        rule.onNodeWithContentDescription("완료").assertIsNotEnabled()
        rule.onNodeWithText("닉네임을 저장하지 못했어요. 잠시 후 다시 시도해주세요.").assertDoesNotExist()
        rule.onNode(hasSetTextAction()).performClick()
        rule.onNode(hasSetTextAction()).performImeAction()
        rule.waitForIdle()

        coVerify(exactly = 2) { check("Different") }
        rule.onNodeWithText("사용 가능한 닉네임이에요.").assertIsDisplayed()
        rule.onNodeWithContentDescription("완료").assertIsEnabled()
    }

    @Test
    fun `변경 제한 중 대소문자 변경이 금칙어로 거절되면 제한을 유지하며 금칙어를 안내한다`() {
        coEvery { getMyPage() } returns Result.success(page(restriction))
        coEvery { check("rodi_user") } returns Result.success(NicknameAvailability("rodi_user", true, null))
        coEvery { update("rodi_user") } returns Result.failure(NicknameRejectedException(NicknameRejection.PROHIBITED_WORD))
        val vm = NicknameViewModel(getMyPage, check, update)
        rule.setContent { RodiTheme { NicknameScreen(onBack = {}, viewModel = vm) } }
        rule.onNode(hasSetTextAction()).performTextReplacement("rodi_user")
        rule.onNode(hasSetTextAction()).performImeAction()
        rule.onNodeWithContentDescription("완료").assertIsEnabled().performTouchInput { click() }

        rule.onNodeWithText("사용할 수 없는 단어가 포함되어 있어요.").assertIsDisplayed()
        rule.onNodeWithText(GUIDANCE).assertDoesNotExist()
        rule.onNodeWithContentDescription("완료").assertIsNotEnabled()
        assertTrue(vm.uiState.value.isChangeRestricted)
        assertEquals(restriction, vm.uiState.value.nicknameChangeableAt)
    }

    private fun page(changeableAt: LocalDateTime?) = MyPage(
        nickname = "Rodi_user",
        level = OnboardingLevel.SEED,
        recommendationTags = emptyList(),
        drivingGoal = null,
        savedPlaceCount = 0,
        nicknameChangeableAt = changeableAt,
    )

    private fun capture(name: String) {
        rule.runOnIdle {
            val view = rule.activity.window.decorView
            assertTrue(view.width > 0 && view.height > 0)
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val file = File("build/reports/nickname-ui/$name.png")
            file.parentFile?.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    private companion object {
        const val GUIDANCE = "닉네임은 변경 후 30일 동안 다시 바꿀 수 없어요."
    }
}
