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

@RunWith(AndroidJUnit4::class)
class CourseDetailSheetInteractionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun swipingSheetHandleExpandsCourseDetailContent() {
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
                    )
                }
            }
        }

        composeRule.onNodeWithText("주행거리").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("닫기").assertIsEnabled()
        val routeInfoNode = composeRule.onNodeWithText("경로 정보")
        val bottomActionNode = composeRule.onNodeWithText("연습하러 가기")
        assertTrue(
            routeInfoNode.getUnclippedBoundsInRoot().bottom >
                bottomActionNode.getUnclippedBoundsInRoot().top,
        )
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

        composeRule.onNodeWithContentDescription("닫기").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("접기").assertIsEnabled()
        assertTrue(
            routeInfoNode.getUnclippedBoundsInRoot().bottom <
                bottomActionNode.getUnclippedBoundsInRoot().top,
        )
        routeInfoNode.assertIsDisplayed()
    }
}
