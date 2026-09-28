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
    fun `내 후기는 신고하거나 차단할 수 없고 대신 안내 문구를 보여준다`() {
        val overlay = HomeOverlayState()

        overlay.requestReport(review(isMine = true))
        overlay.requestBlock(review(isMine = true))

        assertNull(overlay.reviewToReport)
        assertNull(overlay.reviewToBlock)
        assertEquals("내가 쓴 후기는 차단할 수 없습니다", overlay.ownReviewActionToastMessage)
    }

    @Test
    fun `후기 삭제 중에는 삭제 다이얼로그 확인이 동작하지 않는다`() {
        val overlay = HomeOverlayState().apply { reviewToDelete = review(isMine = true) }
        setHost(overlay, isDeleting = true)

        composeRule.onNodeWithText("삭제 중").assertIsDisplayed().performClick()

        assertTrue(deletedReviewIds.isEmpty())
    }

    @Test
    fun `삭제 다이얼로그에서 확인하면 선택한 후기를 삭제한다`() {
        val overlay = HomeOverlayState().apply { reviewToDelete = review(isMine = true) }
        setHost(overlay, isDeleting = false)

        composeRule.onNodeWithText("삭제").performClick()

        assertEquals(listOf(REVIEW_ID), deletedReviewIds)
    }

    @Test
    fun `내비 선택에서 앱을 고르면 현재 알림 권한과 함께 보내고 닫는다`() {
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

    @Test
    fun `재가입 대기 계정은 서버의 재가입 가능 날짜와 확인 버튼 하나를 보여준다`() {
        // UTC 정오라 CI(UTC)와 기기 시간대(KST) 어디서도 같은 날짜로 표시된다.
        val state = HomeUiState(withdrawalLockedUntil = Instant.parse("2026-09-20T12:00:00Z"))
        setHost(HomeOverlayState(), isDeleting = false, state = state)

        composeRule.onNodeWithText("탈퇴 처리 중 계정").assertIsDisplayed()
        composeRule.onNodeWithText("9월 20일 이후 재가입 가능해요.").assertIsDisplayed()
        composeRule.onNodeWithText("예").assertDoesNotExist()
        composeRule.onNodeWithText("확인").performClick()

        assertEquals(listOf<HomeIntent>(HomeIntent.WithdrawalLockedDismissed), intents)
    }

    private fun setHost(overlay: HomeOverlayState, isDeleting: Boolean, state: HomeUiState = HomeUiState()) {
        composeRule.setContent {
            RodiTheme {
                HomeOverlayHost(
                    state = state,
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
