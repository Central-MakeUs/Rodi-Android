package com.dororong.rodi.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.entry.content.NicknameContent
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NicknameOnboardingContentTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `닉네임 수정 안내는 다음 버튼을 가리지 않고 본문 아래에 표시된다`() {
        rule.setContent {
            RodiTheme { NicknameContent(nickname = "흐름타는_고슴도치", onBack = {}, onNext = {}) }
        }
        val hint = rule.onNodeWithText("닉네임은 ‘마이페이지’에서 수정 가능해요.")
        hint.assertIsDisplayed()
        val next = rule.onNodeWithText("다음")
        next.assertIsDisplayed()
        val nickname = rule.onNodeWithText("‘흐름타는_고슴도치’")
        assertTrue(nickname.fetchSemanticsNode().boundsInRoot.bottom < hint.fetchSemanticsNode().boundsInRoot.top)
        assertTrue(hint.fetchSemanticsNode().boundsInRoot.bottom < next.fetchSemanticsNode().boundsInRoot.top)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = File(context.getExternalFilesDir("nickname-design"), "onboarding.png")
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
