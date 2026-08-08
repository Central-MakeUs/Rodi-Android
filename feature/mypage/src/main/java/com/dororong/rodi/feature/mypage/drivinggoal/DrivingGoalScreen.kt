package com.dororong.rodi.feature.mypage.drivinggoal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarData
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHost
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHostState
import com.dororong.rodi.core.ui.components.RodiSkeleton
import com.dororong.rodi.core.ui.effect.CollectEffect
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.mypage.drivinggoal.components.DrivingGoalInput
import com.dororong.rodi.feature.mypage.drivinggoal.components.DrivingGoalTopBar

@Composable
fun DrivingGoalScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DrivingGoalViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { RodiSnackbarHostState() }
    val errorIcon = painterResource(CoreUiR.drawable.ic_alert_circle)

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            DrivingGoalEffect.NavigateBack -> onBack()
            DrivingGoalEffect.ShowSyncError -> snackbarHostState.show(
                RodiSnackbarData(
                    message = "작성해주신 목표가 정상적으로 처리되지 못했어요.\n다시 한번 시도해주세요.",
                    icon = errorIcon,
                ),
            )
        }
    }
    BackHandler {
        if (!uiState.isSaving) onBack()
    }

    Box(modifier = modifier.fillMaxSize()) {
        DrivingGoalContent(
            initialGoal = uiState.initialGoal,
            goal = uiState.goal,
            isLoading = uiState.isLoading,
            isSaving = uiState.isSaving,
            onBack = { if (!uiState.isSaving) onBack() },
            onGoalChange = viewModel::updateGoal,
            onSave = viewModel::save,
        )
        RodiSnackbarHost(snackbarHostState)
    }
}

@Composable
private fun DrivingGoalContent(
    initialGoal: String,
    goal: String,
    isLoading: Boolean,
    isSaving: Boolean,
    onBack: () -> Unit,
    onGoalChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val canSave = goal != initialGoal && !isLoading && !isSaving

    LaunchedEffect(initialGoal, isLoading) {
        if (!isLoading && initialGoal.isBlank()) focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white)
            .statusBarsPadding(),
    ) {
        DrivingGoalTopBar(
            canSave = canSave,
            onBack = onBack,
            onSave = onSave,
        )
        if (isLoading) {
            DrivingGoalLoadingContent()
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            ) {
                Text(
                    text = "이루고 싶은 운전 목표를 입력해주세요.",
                    style = RodiTheme.typography.body3SemiBold,
                    color = RodiTheme.colors.black,
                )
                Spacer(Modifier.height(16.dp))
                DrivingGoalInput(
                    value = goal,
                    onValueChange = onGoalChange,
                    focusRequester = focusRequester,
                )
            }
        }
    }
}

@Composable
private fun DrivingGoalLoadingContent() {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
        RodiSkeleton(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(20.dp),
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(1.dp, RodiTheme.colors.gray300, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.CenterStart,
        ) {
            RodiSkeleton(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(0.56f)
                    .height(16.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            RodiSkeleton(modifier = Modifier.width(34.dp).height(12.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun DrivingGoalEmptyPreview() {
    RodiTheme {
        DrivingGoalContent(
            initialGoal = "",
            goal = "",
            isLoading = false,
            isSaving = false,
            onBack = {},
            onGoalChange = {},
            onSave = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun DrivingGoalFilledPreview() {
    RodiTheme {
        DrivingGoalContent(
            initialGoal = "복잡한 강남 자신있게 운전하기",
            goal = "복잡한 강남 자신있게 운전하기",
            isLoading = false,
            isSaving = false,
            onBack = {},
            onGoalChange = {},
            onSave = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun DrivingGoalSavingPreview() {
    RodiTheme {
        DrivingGoalContent(
            initialGoal = "야간 운전도 자신 있게 하기",
            goal = "야간 운전도 자신 있게 하기",
            isLoading = false,
            isSaving = true,
            onBack = {},
            onGoalChange = {},
            onSave = {},
        )
    }
}
