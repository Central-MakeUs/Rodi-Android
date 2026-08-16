package com.dororong.rodi.feature.home.review

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.domain.model.review.ReviewSubmissionResult
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiIconButton
import com.dororong.rodi.core.ui.components.dialog.RodiAlertDialog
import com.dororong.rodi.core.ui.components.dialog.RodiUnsavedChangesDialog
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.R as HomeR
import com.dororong.rodi.feature.home.review.components.ReviewWriteBasics
import com.dororong.rodi.feature.home.review.components.ReviewWriteDetail

@Composable
fun ReviewWriteScreen(
    placeId: Long,
    placeName: String,
    editingReviewId: Long? = null,
    onClose: () -> Unit,
    onCompleted: (ReviewSubmissionResult) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewWriteViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmExit by remember { mutableStateOf(false) }
    LaunchedEffect(placeId, editingReviewId) {
        if (editingReviewId == null) {
            viewModel.start(placeId, placeName)
        } else {
            viewModel.startForReviewId(placeId, placeName, editingReviewId)
        }
    }
    val requestClose = { if (state.isDirty) confirmExit = true else onClose() }
    BackHandler {
        if (state.step == ReviewWriteStep.Detail) viewModel.back() else requestClose()
    }

    when {
        state.isInitializing -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RodiTheme.colors.primary600)
            }
        }
        state.initializationErrorMessage != null -> {
            RodiAlertDialog(
                confirmText = "다시 시도",
                onConfirm = {
                    editingReviewId?.let { viewModel.startForReviewId(placeId, placeName, it) } ?: onClose()
                },
                dismissText = "닫기",
                onDismiss = onClose,
                onDismissRequest = onClose,
                title = "후기를 불러오지 못했어요",
                description = state.initializationErrorMessage,
            )
        }
        else -> {
            ReviewWriteContent(
                state = state,
                onBack = viewModel::back,
                onClose = requestClose,
                onRecommend = viewModel::selectRecommend,
                onDifficulty = viewModel::selectDifficulty,
                onCongestion = viewModel::selectCongestion,
                onCaution = viewModel::updateCaution,
                onPracticeMethod = viewModel::selectPracticeMethod,
                onContent = viewModel::updateContent,
                onNext = viewModel::next,
                onSubmit = viewModel::submit,
                isEditing = editingReviewId != null,
                modifier = modifier,
            )
        }
    }

    if (confirmExit) {
        RodiUnsavedChangesDialog(
            onContinueWriting = { confirmExit = false },
            onExit = onClose,
        )
    }
    if (state.isSubmitted) {
        val complete: () -> Unit = {
            val result = viewModel.consumeSubmittedResult()
            if (result != null) onCompleted(result)
        }
        RodiAlertDialog(
            confirmText = "확인",
            onConfirm = complete,
            onDismissRequest = complete,
            title = if (state.editingReviewId == null) "후기 등록을 완료했어요!" else "후기 수정을 완료했어요!",
            description = "오늘 남긴 기록이 나의 운전 여정에도,\n" +
                "다른 운전자에게도 도움이 돼요.",
        )
    }
    state.errorMessage?.let { message ->
        RodiAlertDialog(
            confirmText = "확인",
            onConfirm = viewModel::consumeError,
            onDismissRequest = viewModel::consumeError,
            title = "후기를 저장하지 못했어요",
            description = message,
        )
    }
}

@Composable
private fun ReviewWriteContent(
    state: ReviewWriteUiState,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onRecommend: (Boolean) -> Unit,
    onDifficulty: (com.dororong.rodi.core.domain.model.review.ReviewDifficulty) -> Unit,
    onCongestion: (com.dororong.rodi.core.domain.model.review.ReviewCongestion) -> Unit,
    onCaution: (String) -> Unit,
    onPracticeMethod: (com.dororong.rodi.core.domain.model.review.PracticeMethod) -> Unit,
    onContent: (String) -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
    isEditing: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val ctaDividerColor = RodiTheme.colors.gray200
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white)
            .statusBarsPadding(),
    ) {
        ReviewWriteTopBar(
            isDetail = state.step == ReviewWriteStep.Detail,
            isEditing = isEditing,
            onBack = onBack,
            onClose = onClose,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            if (state.step == ReviewWriteStep.Basics) {
                ReviewWriteBasics(state, onRecommend, onDifficulty, onCongestion, onCaution)
            } else {
                ReviewWriteDetail(state, onPracticeMethod, onContent, isEditing = isEditing)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .reviewBottomBarInsets()
                .drawBehind {
                    drawLine(
                        color = ctaDividerColor,
                        start = Offset.Zero,
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            RodiButton(
                text = if (state.step == ReviewWriteStep.Basics) "다음" else "완료",
                onClick = { if (state.step == ReviewWriteStep.Basics) onNext() else onSubmit() },
                enabled = if (state.step == ReviewWriteStep.Basics) state.canGoNext else state.canSubmit,
            )
        }
    }
}

@Composable
private fun ReviewWriteTopBar(
    isDetail: Boolean,
    isEditing: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        if (isDetail) {
            RodiIconButton(
                painter = painterResource(CoreUiR.drawable.ic_chevron_left),
                onClick = onBack,
                contentDescription = "이전 단계",
                tint = RodiTheme.colors.black,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp),
            )
        }
        Text(
            text = if (isEditing) "후기 수정" else "후기 남기기",
            modifier = Modifier.align(Alignment.Center),
            style = RodiTheme.typography.headline1,
            color = RodiTheme.colors.black,
        )
        RodiIconButton(
            painter = painterResource(HomeR.drawable.ic_x),
            onClick = onClose,
            contentDescription = "닫기",
            tint = RodiTheme.colors.black,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
        )
    }
}

@Preview(name = "후기 작성 - P1 미선택", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun ReviewWriteBasicsEmptyPreview() = RodiTheme {
    ReviewWriteContent(
        state = ReviewWriteUiState(placeName = "강남역 코스"),
        onBack = {}, onClose = {}, onRecommend = {}, onDifficulty = {}, onCongestion = {},
        onCaution = {}, onPracticeMethod = {}, onContent = {}, onNext = {}, onSubmit = {},
    )
}

@Preview(name = "후기 작성 - P1 선택", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun ReviewWriteBasicsSelectedPreview() = RodiTheme {
    ReviewWriteContent(
        state = ReviewWriteUiState(
            placeName = "성북구 드라이브 코스",
            isRecommended = true,
            difficulty = com.dororong.rodi.core.domain.model.review.ReviewDifficulty.VERY_EASY,
            congestion = com.dororong.rodi.core.domain.model.review.ReviewCongestion.NORMAL,
        ),
        onBack = {}, onClose = {}, onRecommend = {}, onDifficulty = {}, onCongestion = {},
        onCaution = {}, onPracticeMethod = {}, onContent = {}, onNext = {}, onSubmit = {},
    )
}

@Preview(name = "후기 작성 - P1 입력 완료", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun ReviewWriteBasicsCompletedPreview() = RodiTheme {
    ReviewWriteContent(
        state = ReviewWriteUiState(
            placeName = "성북구 드라이브 코스",
            isRecommended = true,
            difficulty = com.dororong.rodi.core.domain.model.review.ReviewDifficulty.VERY_EASY,
            congestion = com.dororong.rodi.core.domain.model.review.ReviewCongestion.NORMAL,
            caution = "내용 작성 완료",
        ),
        onBack = {}, onClose = {}, onRecommend = {}, onDifficulty = {}, onCongestion = {},
        onCaution = {}, onPracticeMethod = {}, onContent = {}, onNext = {}, onSubmit = {},
    )
}

@Preview(name = "후기 작성 - P2 미선택", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun ReviewWriteDetailEmptyPreview() = RodiTheme {
    ReviewWriteContent(
        state = ReviewWriteUiState(
            step = ReviewWriteStep.Detail,
            isRecommended = true,
            difficulty = com.dororong.rodi.core.domain.model.review.ReviewDifficulty.VERY_EASY,
            congestion = com.dororong.rodi.core.domain.model.review.ReviewCongestion.NORMAL,
        ),
        onBack = {}, onClose = {}, onRecommend = {}, onDifficulty = {}, onCongestion = {},
        onCaution = {}, onPracticeMethod = {}, onContent = {}, onNext = {}, onSubmit = {},
    )
}

@Preview(name = "후기 작성 - P2 입력 완료", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun ReviewWriteDetailCompletedPreview() = RodiTheme {
    ReviewWriteContent(
        state = ReviewWriteUiState(
            step = ReviewWriteStep.Detail,
            isRecommended = true,
            difficulty = com.dororong.rodi.core.domain.model.review.ReviewDifficulty.VERY_EASY,
            congestion = com.dororong.rodi.core.domain.model.review.ReviewCongestion.NORMAL,
            practiceMethod = com.dororong.rodi.core.domain.model.review.PracticeMethod.SOLO,
            content = "자전거 타기 좋은 곳이에요. 초보 운전자도 편하게 연습할 수 있어요.",
        ),
        onBack = {}, onClose = {}, onRecommend = {}, onDifficulty = {}, onCongestion = {},
        onCaution = {}, onPracticeMethod = {}, onContent = {}, onNext = {}, onSubmit = {},
    )
}
