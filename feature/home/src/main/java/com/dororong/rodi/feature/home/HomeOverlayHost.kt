package com.dororong.rodi.feature.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.review.Review
import com.dororong.rodi.core.domain.model.review.ReviewSubmissionResult
import com.dororong.rodi.core.ui.components.AccountRecoveryDialog
import com.dororong.rodi.core.ui.components.dialog.LevelUpDialog
import com.dororong.rodi.core.ui.components.dialog.RodiAlertDialog
import com.dororong.rodi.feature.home.components.LoginRequiredDialog
import com.dororong.rodi.feature.home.components.NaviPickerMode
import com.dororong.rodi.feature.home.components.NaviPickerSheet
import com.dororong.rodi.feature.home.detail.CourseReviewUiState
import com.dororong.rodi.feature.home.detail.levelreviews.LevelReviewsOverlay
import com.dororong.rodi.feature.home.detail.reviewactions.BlockMemberDialog
import com.dororong.rodi.feature.home.detail.reviewactions.ReviewReportScreen
import com.dororong.rodi.feature.home.filter.FilterBottomSheet
import com.dororong.rodi.feature.home.review.NotificationPermissionDialog
import com.dororong.rodi.feature.home.review.PracticeContinueDialog
import com.dororong.rodi.feature.home.review.PracticePromptDialog
import com.dororong.rodi.feature.home.review.ReviewWriteScreen

internal data class ReviewWriteTarget(
    val placeId: Long,
    val placeName: String,
    val review: Review?,
)

/** 홈 위에 뜨는 다이얼로그·시트·전체 화면 중 ViewModel 상태가 아니라 화면이 직접 여닫는 것들. */
@Stable
internal class HomeOverlayState {
    var reviewToReport by mutableStateOf<Review?>(null)
    var reviewToBlock by mutableStateOf<Review?>(null)
    var reviewToDelete by mutableStateOf<Review?>(null)
    var reviewToWrite by mutableStateOf<ReviewWriteTarget?>(null)
    var naviPlaceId by mutableStateOf<Long?>(null)
    var installNaviPlaceId by mutableStateOf<Long?>(null)
    var ownReviewActionToastMessage by mutableStateOf<String?>(null)

    fun requestReport(review: Review) {
        if (review.isMine) {
            ownReviewActionToastMessage = "내가 쓴 후기는 신고할 수 없습니다"
        } else {
            reviewToReport = review
        }
    }

    fun requestBlock(review: Review) {
        if (review.isMine) {
            ownReviewActionToastMessage = "내가 쓴 후기는 차단할 수 없습니다"
        } else {
            reviewToBlock = review
        }
    }
}

@Composable
internal fun rememberHomeOverlayState(): HomeOverlayState = remember { HomeOverlayState() }

internal data class HomeReviewOverlayActions(
    val onSelectLevel: (OnboardingLevel) -> Unit,
    val onLoadInitialReviews: () -> Unit,
    val onLoadNextReviews: () -> Unit,
    val onReviewReported: (Long) -> Unit,
    val onReviewSubmitted: (ReviewSubmissionResult) -> Unit,
    val onBlockMember: (memberId: Long) -> Unit,
    val onDeleteReview: (reviewId: Long) -> Unit,
)

/** 나중에 그린 것이 위에 뜨므로 순서를 바꾸지 않는다. */
@Composable
internal fun HomeOverlayHost(
    state: HomeUiState,
    reviewState: CourseReviewUiState,
    isBlocking: Boolean,
    isDeleting: Boolean,
    overlay: HomeOverlayState,
    onIntent: (HomeIntent) -> Unit,
    reviewActions: HomeReviewOverlayActions,
    onNavigate: () -> Unit,
    onDismissLogin: () -> Unit,
    onKakaoLoginClick: () -> Unit,
    notificationPermissionGranted: () -> Boolean,
) {
    if (state.pendingAction != null && !state.hasPendingRestore) {
        LoginRequiredDialog(
            isLoggingIn = state.isLoginInProgress,
            onDismiss = onDismissLogin,
            onKakaoLoginClick = onKakaoLoginClick,
        )
    }

    val levelReviewsPlace = state.selectedPlace
    if (state.isLevelReviewsVisible && levelReviewsPlace?.type == PlaceType.COURSE) {
        LevelReviewsOverlay(
            recommendCount = reviewState.recommendCount,
            selectedLevel = reviewState.selectedLevel,
            difficultyCounts = reviewState.difficultyCounts,
            reviews = reviewState.reviews,
            isBookmarked = levelReviewsPlace.isBookmarked,
            isBookmarkUpdating = state.isBookmarkUpdating,
            onClose = { onIntent(HomeIntent.LevelReviewsClosed) },
            onSelectLevel = reviewActions.onSelectLevel,
            onLoadInitial = reviewActions.onLoadInitialReviews,
            onLoadNext = reviewActions.onLoadNextReviews,
            onBookmarkClick = { onIntent(HomeIntent.BookmarkClicked) },
            onNavigate = onNavigate,
            onEditReviewClick = {
                overlay.reviewToWrite = ReviewWriteTarget(levelReviewsPlace.id, levelReviewsPlace.name, it)
            },
            onDeleteReviewClick = { overlay.reviewToDelete = it },
            onReportReviewClick = overlay::requestReport,
            onBlockMemberClick = overlay::requestBlock,
        )
    }
    overlay.reviewToReport?.let { review ->
        ReviewReportScreen(
            reviewId = review.reviewId,
            onClose = { overlay.reviewToReport = null },
            modifier = Modifier.fillMaxSize(),
            onReported = reviewActions.onReviewReported,
        )
    }
    overlay.reviewToWrite?.let { target ->
        ReviewWriteScreen(
            placeId = target.placeId,
            placeName = target.placeName,
            editingReviewId = target.review?.reviewId,
            onClose = { overlay.reviewToWrite = null },
            onCompleted = { result ->
                overlay.reviewToWrite = null
                reviewActions.onReviewSubmitted(result)
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
    state.practicePrompt?.let { session ->
        PracticePromptDialog(
            practice = session,
            onVisited = {
                onIntent(HomeIntent.PracticeVisitedAnswered)
            },
            onNotVisited = {
                onIntent(HomeIntent.PracticeNotVisitedAnswered)
            },
            onDismiss = { onIntent(HomeIntent.PracticePromptDismissed) },
        )
    }
    state.activePracticeSession
        ?.takeIf { state.isPracticeContinueDialogVisible }
        ?.let { session ->
            PracticeContinueDialog(
                placeName = session.placeName,
                onContinue = { onIntent(HomeIntent.PracticeContinueClicked) },
                onStop = { onIntent(HomeIntent.PracticeStopClicked) },
                onDismiss = { onIntent(HomeIntent.PracticeContinueClicked) },
            )
        }
    if (state.isNotificationPermissionRationaleVisible) {
        NotificationPermissionDialog(
            onAllow = { onIntent(HomeIntent.NotificationPermissionAllowClicked) },
            onRouteOnly = { onIntent(HomeIntent.NotificationPermissionRouteOnlyClicked) },
        )
    }
    state.levelUp?.let { level ->
        LevelUpDialog(
            level = level,
            onConfirm = { onIntent(HomeIntent.LevelUpDismissed) },
            onDismissRequest = { onIntent(HomeIntent.LevelUpDismissed) },
        )
    }
    overlay.reviewToBlock?.let { review ->
        BlockMemberDialog(
            isBlocking = isBlocking,
            onConfirm = { reviewActions.onBlockMember(review.memberId) },
            onDismiss = { overlay.reviewToBlock = null },
        )
    }
    overlay.reviewToDelete?.let { review ->
        RodiAlertDialog(
            title = "후기를 삭제할까요?",
            description = "삭제한 후기는 되돌릴 수 없어요.",
            confirmText = if (isDeleting) "삭제 중" else "삭제",
            dismissText = "취소",
            enabled = !isDeleting,
            dismissible = !isDeleting,
            onConfirm = { reviewActions.onDeleteReview(review.reviewId) },
            onDismiss = { overlay.reviewToDelete = null },
            onDismissRequest = { if (!isDeleting) overlay.reviewToDelete = null },
        )
    }
    if (state.hasPendingRestore) {
        AccountRecoveryDialog(
            isRestoring = state.isRestoreInProgress,
            onConfirm = { onIntent(HomeIntent.AccountRestoreClicked) },
            onDismiss = { onIntent(HomeIntent.AccountRestoreDismissed) },
        )
    }

    if (state.isFilterSheetVisible) {
        FilterBottomSheet(
            activeCategory = state.activeFilterCategory,
            selectedPracticeTypes = state.selectedFilterPracticeTypes,
            onCategorySelect = { onIntent(HomeIntent.FilterCategorySelected(it)) },
            onPracticeOptionToggle = { onIntent(HomeIntent.FilterPracticeOptionToggled(it)) },
            onReset = { onIntent(HomeIntent.FilterResetClicked) },
            onApply = { onIntent(HomeIntent.FilterApplyClicked) },
            onDismiss = { onIntent(HomeIntent.FilterDismissed) },
            isSaving = state.isFilterSaving,
        )
    }

    overlay.naviPlaceId?.let {
        NaviPickerSheet(
            onDismiss = { overlay.naviPlaceId = null },
            onSelect = { app, always ->
                onIntent(
                    HomeIntent.NaviAppSelected(
                        app = app,
                        always = always,
                        notificationPermissionGranted = notificationPermissionGranted(),
                    ),
                )
                overlay.naviPlaceId = null
            },
        )
    }
    overlay.installNaviPlaceId?.let {
        NaviPickerSheet(
            mode = NaviPickerMode.INSTALL,
            onDismiss = { overlay.installNaviPlaceId = null },
            onSelect = { app, _ ->
                onIntent(HomeIntent.NaviAppInstallSelected(app))
                overlay.installNaviPlaceId = null
            },
        )
    }
}
