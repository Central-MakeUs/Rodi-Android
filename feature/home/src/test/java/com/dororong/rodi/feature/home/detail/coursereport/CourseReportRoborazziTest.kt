package com.dororong.rodi.feature.home.detail.coursereport

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dororong.rodi.core.domain.model.review.ReviewCongestion
import com.dororong.rodi.core.domain.model.review.ReviewDifficulty
import com.dororong.rodi.core.domain.model.review.ReviewIssueForm
import com.dororong.rodi.core.domain.model.review.ReviewIssueOption
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.HomePreviewData
import com.dororong.rodi.feature.home.detail.CourseDetailSheet
import com.dororong.rodi.feature.home.detail.reviewactions.ReportSubmittedDialog
import com.dororong.rodi.feature.home.review.ReviewWriteUiState
import com.dororong.rodi.feature.home.review.components.ReviewWriteBasics
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w375dp-h812dp")
class CourseReportRoborazziTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `아무 항목도 고르지 않은 신고 화면을 캡처한다`() {
        composeRule.setContent { RodiTheme { report(selected = emptyList(), detail = "", enabled = false) } }
        composeRule.onRoot().captureRoboImage("CourseReportRoborazziTest/report_empty.png")
    }

    @Test
    fun `여러 항목과 기타 입력을 채운 신고 화면을 캡처한다`() {
        composeRule.setContent {
            RodiTheme { report(selected = listOf("ROAD_INFO_MISMATCH", "OTHER"), detail = "주말엔 입구가 잠겨 있어요", enabled = true) }
        }
        composeRule.onRoot().captureRoboImage("CourseReportRoborazziTest/report_other.png")
    }

    @Test
    fun `추천해요로만 수정할 수 있는 주차장 후기 입력을 캡처한다`() {
        composeRule.setContent {
            RodiTheme {
                Surface(Modifier.fillMaxSize()) {
                    ReviewWriteBasics(
                        state = ReviewWriteUiState(
                            placeName = "강남역 공영주차장",
                            isRecommendOnly = true,
                            isRecommended = true,
                            difficulty = ReviewDifficulty.EASY,
                            congestion = ReviewCongestion.NORMAL,
                        ),
                        onRecommend = {}, onDifficulty = {}, onCongestion = {}, onCaution = {},
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage("CourseReportRoborazziTest/review_recommend_only.png")
    }

    @Test
    fun `펼친 코스 상세의 더보기 메뉴를 캡처한다`() {
        composeRule.setContent {
            RodiTheme {
                Surface(Modifier.fillMaxSize()) {
                    CourseDetailSheet(
                        place = HomePreviewData.courseDetail,
                        isBookmarkUpdating = false,
                        onDismiss = {}, onBookmarkClick = {}, onNavigate = {}, onSheetHeightChanged = {},
                        reviewContent = {},
                    )
                }
            }
        }
        val close = composeRule.onNodeWithContentDescription("닫기").getUnclippedBoundsInRoot()
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        with(composeRule.density) {
            val x = (root.left + (root.right - root.left) / 2).toPx()
            composeRule.onRoot().performTouchInput {
                swipe(Offset(x, close.top.toPx() - 12.dp.toPx()), Offset(x, root.top.toPx() + 12.dp.toPx()))
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("코스 더보기").performClick()
        composeRule.waitForIdle()
        captureScreenRoboImage("CourseReportRoborazziTest/more_menu.png")
    }

    @Test
    fun `신고 접수 완료 팝업을 캡처한다`() {
        composeRule.setContent {
            RodiTheme {
                report(selected = listOf("CONSTRUCTION"), detail = "", enabled = true)
                ReportSubmittedDialog(onConfirm = {})
            }
        }
        composeRule.waitForIdle()
        captureScreenRoboImage("CourseReportRoborazziTest/submitted.png")
    }

    @androidx.compose.runtime.Composable
    private fun report(selected: List<String>, detail: String, enabled: Boolean) {
        CourseReportContent(
            form = form,
            selected = selected,
            detail = detail,
            isSubmissionEnabled = enabled,
            onBack = {}, onToggle = {}, onDetail = {}, onSubmit = {},
        )
    }

    private val form = ReviewIssueForm(
        title = "해당 코스에 어떤 문제가 있나요?",
        options = listOf(
            ReviewIssueOption("VEHICLE_RESTRICTED", "차량 진입이 제한된 장소예요", false, null, null),
            ReviewIssueOption("ROAD_INFO_MISMATCH", "도로 정보가 실제와 달라요", false, null, null),
            ReviewIssueOption("CONSTRUCTION", "공사·통제 등으로 현재 주행할 수 없어요", false, null, null),
            ReviewIssueOption("NOT_FOR_PRACTICE", "연습 코스로 적합하지 않아요", false, null, null),
            ReviewIssueOption("OTHER", "기타", true, "이유를 작성해주세요.", 30),
        ),
    )
}
