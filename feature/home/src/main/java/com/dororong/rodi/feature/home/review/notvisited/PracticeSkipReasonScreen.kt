package com.dororong.rodi.feature.home.review.notvisited

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.domain.model.practice.SkipReasonForm
import com.dororong.rodi.core.domain.model.practice.SkipReasonOption
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiIconButton
import com.dororong.rodi.core.ui.components.dialog.RodiAlertDialog
import com.dororong.rodi.core.ui.components.dialog.RodiDialog
import com.dororong.rodi.core.ui.components.dialog.RodiUnsavedChangesDialog
import com.dororong.rodi.core.ui.components.input.rememberGraphemeTextFieldState
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarData
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarDuration
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHost
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHostState
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.core.ui.components.input.rodiCursorBrush
import com.dororong.rodi.core.ui.components.input.rodiInputBorderColor
import com.dororong.rodi.feature.home.R
import com.dororong.rodi.feature.home.detail.reviewactions.ReportReasonRow
import com.dororong.rodi.feature.home.review.reviewBottomBarInsets

@Composable
fun PracticeSkipReasonScreen(
    practiceId: Long,
    onClose: () -> Unit,
    viewModel: PracticeSkipReasonViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(practiceId) { viewModel.load(practiceId) }

    val requestClose = {
        if ((state.selectedOptionCode != null || state.detail.isNotBlank()) && !state.isSubmitted) {
            confirmExit = true
        } else {
            onClose()
        }
    }
    BackHandler(onBack = requestClose)

    PracticeSkipReasonContent(
        form = state.form,
        selectedOptionCode = state.selectedOptionCode,
        detail = state.detail,
        isLoading = state.isLoading,
        isSubmitting = state.isSubmitting,
        onClose = requestClose,
        onSelect = viewModel::selectOption,
        onDetailChange = viewModel::updateDetail,
        onSubmit = viewModel::submit,
    )

    if (state.isSubmitted) {
        NotVisitedCompletionDialog(onConfirm = onClose)
    }
    val snackbarHostState = remember { RodiSnackbarHostState() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.show(
                RodiSnackbarData(message = message, duration = RodiSnackbarDuration.Medium),
            )
            viewModel.consumeError()
        }
    }
    RodiSnackbarHost(snackbarHostState)
    if (confirmExit) {
        RodiUnsavedChangesDialog(
            onContinueWriting = { confirmExit = false },
            onExit = onClose,
        )
    }
}

@Composable
private fun NotVisitedCompletionDialog(onConfirm: () -> Unit) {
    RodiDialog(
        onDismissRequest = onConfirm,
        modifier = Modifier
            .width(280.dp)
            .height(226.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 32.dp),
    ) {
        Text(
            text = "소중한 의견 감사해요!",
            style = RodiTheme.typography.price1,
            color = RodiTheme.colors.black,
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "남겨주신 내용은 코스 탐색 경험을\n개선하는 데 활용할게요 :)",
                style = RodiTheme.typography.caption1Medium,
                color = RodiTheme.colors.black,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(24.dp))
        RodiButton(
            text = "확인",
            onClick = onConfirm,
            modifier = Modifier.width(116.dp),
            fillMaxWidth = false,
            height = 42.dp,
        )
    }
}

@Composable
private fun PracticeSkipReasonContent(
    form: SkipReasonForm?,
    selectedOptionCode: String?,
    detail: String,
    isLoading: Boolean,
    isSubmitting: Boolean,
    onClose: () -> Unit,
    onSelect: (SkipReasonOption) -> Unit,
    onDetailChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val selectedOption = form?.options?.firstOrNull { it.code == selectedOptionCode }
    val canSubmit = selectedOption != null &&
        (!selectedOption.requiresTextInput || detail.isNotBlank()) &&
        !isLoading &&
        !isSubmitting
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(
                text = "미방문 사유",
                modifier = Modifier.align(Alignment.Center),
                style = RodiTheme.typography.headline1,
                color = RodiTheme.colors.black,
            )
            RodiIconButton(
                painter = painterResource(R.drawable.ic_x),
                onClick = onClose,
                contentDescription = "닫기",
                tint = RodiTheme.colors.black,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
        ) {
            Text(
                text = "왜 연습을 다녀오지 않았나요?",
                style = RodiTheme.typography.heading2,
                color = RodiTheme.colors.black,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = form?.description ?: "이유를 알려주시면 더 나은 코스를 추천해드릴게요!",
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.gray600,
            )
            Spacer(Modifier.height(24.dp))
            when {
                isLoading -> CircularProgressIndicator(
                    color = RodiTheme.colors.primary600,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                form != null -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        form.options.forEach { option ->
                            ReportReasonRow(
                                label = option.label,
                                selected = selectedOptionCode == option.code,
                                onClick = { onSelect(option) },
                            )
                        }
                    }
                    if (selectedOption?.requiresTextInput == true) {
                        Spacer(Modifier.height(4.dp))
                        NotVisitedDetailInput(
                            value = detail,
                            placeholder = selectedOption.textInputPlaceholder ?: "이유를 입력해주세요",
                            maxGraphemes = selectedOption.textInputMaxLength ?: Int.MAX_VALUE,
                            onValueChange = onDetailChange,
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = RodiTheme.colors.gray200)
        RodiButton(
            text = if (isSubmitting) "제출 중" else "다음",
            onClick = onSubmit,
            enabled = canSubmit,
            modifier = Modifier
                .reviewBottomBarInsets()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun NotVisitedDetailInput(
    value: String,
    placeholder: String,
    maxGraphemes: Int,
    onValueChange: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val textFieldState = rememberGraphemeTextFieldState(value, maxGraphemes, onValueChange)

    BasicTextField(
        value = textFieldState.value,
        onValueChange = textFieldState.onValueChange,
        textStyle = RodiTheme.typography.body3Medium.copy(color = RodiTheme.colors.black),
        singleLine = true,
        interactionSource = interactionSource,
        cursorBrush = rodiCursorBrush(),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .border(
                width = 1.dp,
                color = rodiInputBorderColor(isFocused),
                shape = RoundedCornerShape(8.dp),
            ),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (textFieldState.value.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = RodiTheme.typography.body3Medium,
                        color = RodiTheme.colors.gray500,
                    )
                }
                innerTextField()
            }
        },
    )
}

private val PreviewForm = SkipReasonForm(
    questionId = "practice-skip",
    type = "SINGLE_SELECT",
    title = "미방문 사유",
    description = "이유를 알려주시면 더 나은 코스를 추천해드릴게요!",
    required = true,
    options = listOf(
        SkipReasonOption("TOO_FAR", "생각보다 멀었어요", 1, false, null, null),
        SkipReasonOption("OTHER", "기타", 2, true, "이유를 입력해주세요", 100),
    ),
)

@Preview(name = "미방문 사유 - 미선택", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun PracticeSkipReasonEmptyPreview() = RodiTheme {
    PracticeSkipReasonContent(PreviewForm, null, "", false, false, {}, {}, {}, {})
}

@Preview(name = "미방문 사유 - 선택", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun PracticeSkipReasonSelectedPreview() = RodiTheme {
    PracticeSkipReasonContent(PreviewForm, "TOO_FAR", "", false, false, {}, {}, {}, {})
}

@Preview(name = "미방문 사유 - 기타 입력", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun PracticeSkipReasonOtherPreview() = RodiTheme {
    PracticeSkipReasonContent(PreviewForm, "OTHER", "다음에 다시 방문할게요.", false, false, {}, {}, {}, {})
}

@Preview(name = "미방문 사유 - 완료", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun PracticeSkipReasonCompletePreview() = RodiTheme {
    PracticeSkipReasonContent(PreviewForm, "TOO_FAR", "", false, false, {}, {}, {}, {})
    RodiAlertDialog("확인", {}, {}, title = "소중한 의견 감사해요!")
}
