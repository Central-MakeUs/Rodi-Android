package com.dororong.rodi.feature.home.detail.coursereport

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dororong.rodi.core.domain.model.review.ReviewIssueForm
import com.dororong.rodi.core.domain.model.review.ReviewIssueOption
import com.dororong.rodi.core.ui.theme.RodiTheme
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CourseReportContentTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `복수 신고 사유를 유지하고 기타를 선택했을 때만 직접 입력을 표시한다`() {
        val selected = mutableStateOf<List<String>>(emptyList())
        val detail = mutableStateOf("")
        var wentBack = false
        val form = ReviewIssueForm("해당 코스에 어떤 문제가 있나요?", listOf(
            ReviewIssueOption("1", "차량 진입이 제한된 장소예요", false, null, null),
            ReviewIssueOption("2", "도로 정보가 실제와 달라요", false, null, null),
            ReviewIssueOption("3", "공사·통제 등으로 현재 주행할 수 없어요", false, null, null),
            ReviewIssueOption("4", "연습 코스로 적합하지 않아요", false, null, null),
            ReviewIssueOption("5", "기타", true, "이유를 작성해주세요.", 30),
        ))
        rule.setContent {
            RodiTheme {
                CourseReportContent(
                    form = form,
                    selected = selected.value,
                    detail = detail.value,
                    isSubmissionEnabled = false,
                    onBack = { wentBack = true },
                    onToggle = { code ->
                        val current = selected.value
                        selected.value = if (code in current) current - code else current + code
                    },
                    onDetail = { detail.value = it },
                    onSubmit = {},
                )
            }
        }
        rule.onNodeWithText("제출").assertIsNotEnabled()
        rule.onNodeWithText("기타").performClick()
        rule.onNodeWithText("이유를 작성해주세요.").assertIsDisplayed()
        rule.onNodeWithText("0/30").assertIsDisplayed()
        capture("course-report-other")
        rule.onNode(hasSetTextAction()).performTextReplacement("가".repeat(31))
        rule.onNodeWithText("30/30").assertIsDisplayed()
        rule.onNodeWithText("제출").assertIsNotEnabled()
        rule.onNodeWithText("도로 정보가 실제와 달라요").performClick()
        rule.onNodeWithText("도로 정보가 실제와 달라요").assertIsOn()
        rule.onNodeWithText("기타").assertIsOn()
        rule.onNodeWithText("30/30").assertIsDisplayed()
        rule.onNodeWithText("기타").performClick()
        rule.onNodeWithText("이유를 작성해주세요.").assertDoesNotExist()
        rule.onNodeWithText("도로 정보가 실제와 달라요").assertIsOn()
        rule.onNodeWithContentDescription("뒤로가기").performClick()
        rule.runOnIdle { assertTrue(wentBack) }
    }

    private fun capture(name: String) {
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        val folder = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir("review-1031")
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
