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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarData
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHost
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHostState
import com.dororong.rodi.core.ui.effect.CollectEffect
import com.dororong.rodi.core.ui.theme.RodiTheme
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
                    message = "정보를 불러오지 못했어요. 다시 시도해주세요.",
                    icon = errorIcon,
                ),
            )
        }
    }
    BackHandler(onBack = onBack)

    Box(modifier = modifier.fillMaxSize()) {
        DrivingGoalContent(
            initialGoal = uiState.initialGoal,
            isSaving = uiState.isSaving,
            onBack = onBack,
            onSave = viewModel::save,
        )
        RodiSnackbarHost(snackbarHostState)
    }
}

@Composable
private fun DrivingGoalContent(
    initialGoal: String,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var goal by rememberSaveable(initialGoal) { mutableStateOf(initialGoal) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val canSave = goal.isNotBlank() && !isSaving

    LaunchedEffect(initialGoal) {
        if (initialGoal.isBlank()) focusRequester.requestFocus()
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
            onSave = { onSave(goal) },
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
        ) {
            Text(
                text = "이루고 싶은 운전 목표를 입력해주세요.",
                style = RodiTheme.typography.body3SemiBold,
                color = RodiTheme.colors.black,
            )
            Spacer(Modifier.height(16.dp))
            BasicTextField(
                value = goal,
                onValueChange = { goal = it.take(DRIVING_GOAL_MAX_LENGTH) },
                textStyle = RodiTheme.typography.body3Medium.copy(color = RodiTheme.colors.black),
                cursorBrush = SolidColor(RodiTheme.colors.black),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused }
                    .border(
                        width = 1.dp,
                        color = if (isFocused || goal.isBlank()) RodiTheme.colors.black else RodiTheme.colors.gray300,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 16.dp),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) { innerTextField() }
                },
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${goal.length} /30",
                    style = RodiTheme.typography.caption2Medium,
                    color = RodiTheme.colors.gray500,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun DrivingGoalEmptyPreview() {
    RodiTheme {
        DrivingGoalContent(
            initialGoal = "",
            isSaving = false,
            onBack = {},
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
            isSaving = false,
            onBack = {},
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
            isSaving = true,
            onBack = {},
            onSave = {},
        )
    }
}
