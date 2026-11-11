package com.dororong.rodi.feature.home.detail

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.HomePreviewData
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w375dp-h812dp")
class CourseDetailSheetInteractionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `시트 손잡이를 밀어 올리면 코스 상세 내용이 펼쳐진다`() {
        setSheet()

        composeRule.onNodeWithText("주행거리").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("닫기").assertIsEnabled()
        val routeInfoNode = composeRule.onNodeWithText("경로 정보")
        val bottomActionNode = composeRule.onNodeWithText("연습하러 가기")
        assertTrue(
            routeInfoNode.getUnclippedBoundsInRoot().bottom >
                bottomActionNode.getUnclippedBoundsInRoot().top,
        )

        expandSheet()

        composeRule.onNodeWithContentDescription("닫기").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("접기").assertIsEnabled()
        assertTrue(
            routeInfoNode.getUnclippedBoundsInRoot().bottom <
                bottomActionNode.getUnclippedBoundsInRoot().top,
        )
        routeInfoNode.assertIsDisplayed()
    }

    @Test
    fun `펼친 상세의 더보기에서 신고하기를 고르면 신고 화면을 연다`() {
        var reportClicks = 0
        setSheet(onReportClick = { reportClicks++ })
        composeRule.onNodeWithContentDescription("코스 더보기").assertIsNotEnabled()

        expandSheet()
        composeRule.onNodeWithContentDescription("코스 더보기").assertIsEnabled().performClick()
        composeRule.onNodeWithText("신고하기").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertTrue(reportClicks == 1)
        composeRule.onNodeWithText("신고하기").assertDoesNotExist()
    }

    private fun setSheet(onReportClick: () -> Unit = {}) {
        composeRule.setContent {
            RodiTheme {
                Surface(Modifier.fillMaxSize()) {
                    CourseDetailSheet(
                        place = HomePreviewData.courseDetail,
                        isBookmarkUpdating = false,
                        onDismiss = {},
                        onBookmarkClick = {},
                        onNavigate = {},
                        onSheetHeightChanged = {},
                        reviewContent = {},
                        onReportClick = onReportClick,
                    )
                }
            }
        }
    }

    private fun expandSheet() {
        val closeButtonBounds = composeRule
            .onNodeWithContentDescription("닫기")
            .getUnclippedBoundsInRoot()
        val rootBounds = composeRule.onRoot().getUnclippedBoundsInRoot()
        val density = composeRule.density
        val rootCenterX = with(density) {
            rootBounds.left.toPx() + (rootBounds.right - rootBounds.left).toPx() / 2f
        }
        val handleInset = with(density) { 12.dp.toPx() }
        val startY = with(density) { closeButtonBounds.top.toPx() } - handleInset
        val endY = with(density) { rootBounds.top.toPx() } + handleInset

        composeRule.onRoot().performTouchInput {
            swipe(
                start = Offset(rootCenterX, startY),
                end = Offset(rootCenterX, endY),
            )
        }
        composeRule.waitForIdle()
    }
}
