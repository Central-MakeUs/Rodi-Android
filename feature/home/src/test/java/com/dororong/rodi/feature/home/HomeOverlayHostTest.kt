package com.dororong.rodi.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dororong.rodi.core.domain.model.navi.NaviApp
import com.dororong.rodi.core.domain.model.review.Review
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.detail.CourseReviewUiState
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w375dp-h812dp")
class HomeOverlayHostTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<HomeIntent>()
    private val deletedReviewIds = mutableListOf<Long>()

    @Test
    fun `own review cannot be reported and shows a toast message instead`() {
        val overlay = HomeOverlayState()

        overlay.requestReport(review(isMine = true))
        overlay.requestBlock(review(isMine = true))

        assertNull(overlay.reviewToReport)
        assertNull(overlay.reviewToBlock)
        assertEquals("내가 쓴 후기는 차단할 수 없습니다", overlay.ownReviewActionToastMessage)
    }

    @Test
    fun `delete dialog blocks confirmation while the review is being deleted`() {
        val overlay = HomeOverlayState().apply { reviewToDelete = review(isMine = true) }
        setHost(overlay, isDeleting = true)

        composeRule.onNodeWithText("삭제 중").assertIsDisplayed().performClick()

        assertTrue(deletedReviewIds.isEmpty())
    }

    @Test
    fun `delete dialog confirms the selected review`() {
        val overlay = HomeOverlayState().apply { reviewToDelete = review(isMine = true) }
        setHost(overlay, isDeleting = false)

        composeRule.onNodeWithText("삭제").performClick()

        assertEquals(listOf(REVIEW_ID), deletedReviewIds)
    }

    @Test
    fun `navi picker sends the chosen app with the current notification permission and closes`() {
        val overlay = HomeOverlayState().apply { naviPlaceId = 1L }
        setHost(overlay, isDeleting = false)

        composeRule.onNodeWithText("카카오내비").performClick()
        composeRule.onNodeWithText("이번만").performClick()
        composeRule.waitForIdle()

        assertEquals(
            listOf(HomeIntent.NaviAppSelected(NaviApp.KAKAONAVI, always = false, notificationPermissionGranted = true)),
            intents,
        )
        assertNull(overlay.naviPlaceId)
    }

    private fun setHost(overlay: HomeOverlayState, isDeleting: Boolean) {
        composeRule.setContent {
            RodiTheme {
                HomeOverlayHost(
                    state = HomeUiState(),
                    reviewState = CourseReviewUiState(),
                    isBlocking = false,
                    isDeleting = isDeleting,
                    overlay = overlay,
                    onIntent = { intents += it },
                    reviewActions = HomeReviewOverlayActions(
                        onSelectLevel = {},
                        onLoadInitialReviews = {},
                        onLoadNextReviews = {},
                        onReviewReported = {},
                        onReviewSubmitted = {},
                        onBlockMember = {},
                        onDeleteReview = { deletedReviewIds += it },
                    ),
                    onNavigate = {},
                    onDismissLogin = {},
                    onKakaoLoginClick = {},
                    notificationPermissionGranted = { true },
                )
            }
        }
    }

    private fun review(isMine: Boolean) = Review(
        reviewId = REVIEW_ID,
        memberId = 7L,
        nickname = "운전자",
        memberLevel = null,
        isRecommended = null,
        difficulty = null,
        congestion = null,
        practiceMethod = null,
        content = "후기",
        caution = null,
        isMine = isMine,
        isEditable = isMine,
        isHidden = false,
        createdAt = Instant.EPOCH,
        isVerifiedVisit = null,
    )

    private companion object {
        const val REVIEW_ID = 42L
    }
}
