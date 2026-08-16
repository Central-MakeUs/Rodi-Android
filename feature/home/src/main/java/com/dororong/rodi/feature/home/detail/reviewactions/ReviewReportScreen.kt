package com.dororong.rodi.feature.home.detail.reviewactions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.domain.model.review.ReportForm
import com.dororong.rodi.core.domain.model.review.ReportFormOption
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiIconButton
import com.dororong.rodi.core.ui.components.input.rememberGraphemeTextFieldState
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarData
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHost
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHostState
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun ReviewReportScreen(
    reviewId: Long,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewActionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { RodiSnackbarHostState() }

    LaunchedEffect(reviewId) { viewModel.loadReportForm(reviewId) }
    LaunchedEffect(state.reportErrorMessage) {
        state.reportErrorMessage?.let { message ->
            snackbarHostState.show(RodiSnackbarData(message = message))
            viewModel.consumeReportError()
        }
    }
    BackHandler {
        if (!state.isReportSubmitting) {
            viewModel.closeReport()
            onClose()
        }
    }

    Box(modifier.fillMaxSize()) {
        ReviewReportContent(
            form = state.reportForm,
            selectedOptionCode = state.selectedOptionCode,
            detail = state.reportDetail,
            isLoading = state.isReportFormLoading,
            isSubmitting = state.isReportSubmitting,
            onBack = {
                if (!state.isReportSubmitting) {
                    viewModel.closeReport()
                    onClose()
                }
            },
            onSelectOption = viewModel::selectReportOption,
            onDetailChange = viewModel::updateReportDetail,
            onSubmit = viewModel::submitReport,
        )
        RodiSnackbarHost(snackbarHostState)
    }

    if (state.isReportSubmitted) {
        ReportSubmittedDialog(
            onConfirm = {
                viewModel.closeReport()
                onClose()
            },
        )
    }
}

@Composable
fun BlockMemberDialog(
    isBlocking: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = { if (!isBlocking) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isBlocking,
            dismissOnClickOutside = !isBlocking,
        ),
    ) {
        val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
        SideEffect { dialogWindowProvider?.window?.setDimAmount(0.5f) }
        Surface(
            modifier = Modifier
                .width(280.dp)
                .height(226.dp),
            shape = RoundedCornerShape(12.dp),
            color = RodiTheme.colors.white,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "사용자를 차단하겠습니까?",
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
                        text = "이 사용자의 모든 리뷰를 보지 않습니다.",
                        style = RodiTheme.typography.caption1Medium,
                        color = RodiTheme.colors.black,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DialogButton(
                        text = "취소",
                        isPrimary = false,
                        enabled = !isBlocking,
                        onClick = onDismiss,
                    )
                    DialogButton(
                        text = if (isBlocking) "차단 중" else "차단",
                        isPrimary = true,
                        enabled = !isBlocking,
                        onClick = onConfirm,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewReportContent(
    form: ReportForm?,
    selectedOptionCode: String?,
    detail: String,
    isLoading: Boolean,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onSelectOption: (ReportFormOption) -> Unit,
    onDetailChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val selectedOption = form?.options?.firstOrNull { it.code == selectedOptionCode }
    val canSubmit = selectedOption != null &&
        (!selectedOption.requiresTextInput || detail.isNotBlank()) &&
        !isLoading &&
        !isSubmitting

    Surface(Modifier.fillMaxSize(), color = RodiTheme.colors.white) {
        Column(Modifier.statusBarsPadding()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                RodiIconButton(
                    painter = androidx.compose.ui.res.painterResource(CoreUiR.drawable.ic_chevron_left),
                    onClick = onBack,
                    iconSize = 24.dp,
                    contentDescription = "뒤로가기",
                    tint = RodiTheme.colors.black,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp),
                )
                Text(
                    text = "신고하기",
                    style = RodiTheme.typography.headline1,
                    color = RodiTheme.colors.black,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            ) {
                Text(
                    text = form?.title ?: "신고 사유",
                    style = RodiTheme.typography.heading2,
                    color = RodiTheme.colors.black,
                )
                Spacer(Modifier.height(24.dp))
                if (form != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        form.options.forEach { option ->
                            ReportReasonRow(
                                option = option,
                                selected = option.code == selectedOptionCode,
                                onClick = { onSelectOption(option) },
                            )
                        }
                    }
                    if (selectedOption?.requiresTextInput == true) {
                        Spacer(Modifier.height(4.dp))
                        ReportDetailInput(
                            value = detail,
                            placeholder = selectedOption.textInputPlaceholder ?: "이유를 작성해주세요",
                            maxGraphemes = selectedOption.textInputMaxLength ?: Int.MAX_VALUE,
                            onValueChange = onDetailChange,
                        )
                    }
                }
            }

            HorizontalDivider(color = RodiTheme.colors.gray200)
            RodiButton(
                text = if (isSubmitting) "제출 중" else "제출",
                onClick = onSubmit,
                enabled = canSubmit,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
internal fun ReportReasonRow(
    option: ReportFormOption,
    selected: Boolean,
    onClick: () -> Unit,
) = ReportReasonRow(option.label, selected, onClick)

@Composable
internal fun ReportReasonRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = RodiTheme.colors.primary600,
                unselectedColor = RodiTheme.colors.gray300,
            ),
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = RodiTheme.typography.body1Medium,
            color = RodiTheme.colors.black,
        )
    }
}

@Composable
private fun ReportDetailInput(
    value: String,
    placeholder: String,
    maxGraphemes: Int,
    onValueChange: (String) -> Unit,
) {
    val textFieldState = rememberGraphemeTextFieldState(value, maxGraphemes, onValueChange)
    BasicTextField(
        value = textFieldState.value,
        onValueChange = textFieldState.onValueChange,
        textStyle = RodiTheme.typography.body3Medium.copy(color = RodiTheme.colors.black),
        singleLine = true,
        cursorBrush = SolidColor(RodiTheme.colors.primary600),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .border(1.dp, RodiTheme.colors.gray300, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
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

@Composable
private fun ReportSubmittedDialog(onConfirm: () -> Unit) {
    Dialog(
        onDismissRequest = onConfirm,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
        SideEffect { dialogWindowProvider?.window?.setDimAmount(0.5f) }
        Surface(
            modifier = Modifier
                .width(280.dp)
                .height(226.dp),
            shape = RoundedCornerShape(12.dp),
            color = RodiTheme.colors.white,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "신고가 접수되었습니다.",
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
                        text = "관리자의 검토 후 빠른 시일내에\n조치 예정입니다.",
                        style = RodiTheme.typography.caption1Medium,
                        color = RodiTheme.colors.black,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(24.dp))
                DialogButton(
                    text = "확인",
                    isPrimary = true,
                    enabled = true,
                    onClick = onConfirm,
                )
            }
        }
    }
}

@Composable
private fun DialogButton(
    text: String,
    isPrimary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val buttonShape = RoundedCornerShape(8.dp)
    Surface(
        modifier = Modifier
            .width(116.dp)
            .height(42.dp)
            .clip(buttonShape)
            .clickable(enabled = enabled, onClick = onClick),
        shape = buttonShape,
        color = if (isPrimary) RodiTheme.colors.primary600 else RodiTheme.colors.white,
        border = if (isPrimary) null else BorderStroke(1.dp, RodiTheme.colors.gray300),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = RodiTheme.typography.button1,
                color = if (isPrimary) RodiTheme.colors.white else RodiTheme.colors.gray800,
            )
        }
    }
}

private val PreviewReportForm = ReportForm(
    questionId = "review-report",
    title = "신고 사유",
    description = null,
    required = true,
    options = listOf(
        ReportFormOption("SPAM", "스팸/광고", 1, false, null, null),
        ReportFormOption("ABUSE", "욕설, 음란성, 혐오 표현", 2, false, null, null),
        ReportFormOption("OFF_TOPIC", "코스와 무관한 내용", 3, false, null, null),
        ReportFormOption("MISINFORMATION", "허위정보", 4, false, null, null),
        ReportFormOption("OTHER", "기타", 5, true, "이유를 작성해주세요", 100),
    ),
)

@Preview(name = "신고하기 - 사유 선택", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun ReviewReportContentPreview() {
    RodiTheme {
        ReviewReportContent(
            form = PreviewReportForm,
            selectedOptionCode = "ABUSE",
            detail = "",
            isLoading = false,
            isSubmitting = false,
            onBack = {},
            onSelectOption = {},
            onDetailChange = {},
            onSubmit = {},
        )
    }
}

@Preview(name = "신고하기 - 기타 입력", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun ReviewReportDetailPreview() {
    RodiTheme {
        ReviewReportContent(
            form = PreviewReportForm,
            selectedOptionCode = "OTHER",
            detail = "작성 완료",
            isLoading = false,
            isSubmitting = false,
            onBack = {},
            onSelectOption = {},
            onDetailChange = {},
            onSubmit = {},
        )
    }
}

@Preview(name = "사용자 차단 확인", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun BlockMemberDialogPreview() {
    RodiTheme { BlockMemberDialog(false, {}, {}) }
}
