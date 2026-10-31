package com.dororong.rodi.feature.home.review

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReviewWriteIssueContentTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `비추천 기타 입력은 다음 단계와 뒤로가기 이후에도 유지된다`() {
        val state = mutableStateOf(ReviewWriteUiState(placeName = "북악스카이웨이 드라이브", issueForm = form()))
        rule.setContent {
            RodiTheme {
                ReviewWriteContent(
                    state = state.value,
                    onBack = { state.value = state.value.copy(step = ReviewWriteStep.Basics) },
                    onClose = {},
                    onRecommend = { state.value = state.value.copy(isRecommended = it) },
                    onDifficulty = {}, onCongestion = {}, onCaution = {},
                    onCourseIssue = { code ->
                        state.value = state.value.copy(courseIssues =
                            if (code in state.value.courseIssues) state.value.courseIssues - code else state.value.courseIssues + code)
                    },
                    onCourseIssueDetail = { state.value = state.value.copy(courseIssueDetail = it) },
                    onPracticeMethod = {}, onContent = {},
                    onNext = { state.value = state.value.copy(step = ReviewWriteStep.Detail) }, onSubmit = {},
                )
            }
        }
        rule.onNodeWithText("난이도", substring = true).assertDoesNotExist()
        rule.onNodeWithText("별로에요").performClick()
        rule.onNodeWithText("해당 코스에 어떤 문제가 있나요?", substring = true).assertIsDisplayed()
        rule.onNodeWithText("다음").assertIsNotEnabled()
        rule.runOnIdle { state.value = state.value.copy(courseIssues = listOf("OTHER")) }
        rule.onNodeWithText("이유를 작성해주세요.").assertIsDisplayed()
        rule.onNodeWithText("다음").assertIsNotEnabled()
        capture("other-empty")
        rule.onNodeWithText("기타").performClick()
        rule.onNodeWithText("이유를 작성해주세요.").assertDoesNotExist()
        rule.onNodeWithText("기타").performClick()
        rule.onNode(hasSetTextAction()).performTextReplacement("공사 중")
        rule.onNodeWithText("다음").assertIsEnabled().performClick()
        rule.onNodeWithText("연습 방법", substring = true).assertIsDisplayed()
        rule.onNodeWithContentDescription("이전 단계").performClick()
        rule.onNodeWithText("공사 중").assertIsDisplayed()
        rule.onNodeWithText("추천해요").performClick()
        rule.onNodeWithText("난이도", substring = true).assertIsDisplayed()
        rule.onNodeWithText("별로에요").performClick()
        rule.onNodeWithText("공사 중").assertIsDisplayed()
    }

    private fun form() = ReviewIssueForm("해당 코스에 어떤 문제가 있나요?", listOf(
        ReviewIssueOption("VEHICLE_RESTRICTED", "차량 진입이 제한된 장소예요", false, null, null),
        ReviewIssueOption("ROAD_INFO_MISMATCH", "도로 정보가 실제와 달라요", false, null, null),
        ReviewIssueOption("CONSTRUCTION", "공사·통제 등으로 현재 주행할 수 없어요", false, null, null),
        ReviewIssueOption("NOT_FOR_PRACTICE", "연습 코스로 적합하지 않아요", false, null, null),
        ReviewIssueOption("OTHER", "기타", true, "이유를 작성해주세요.", 30),
    ))

    private fun capture(name: String) {
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        android.os.SystemClock.sleep(700)
        val folder = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir("review-1031")
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
