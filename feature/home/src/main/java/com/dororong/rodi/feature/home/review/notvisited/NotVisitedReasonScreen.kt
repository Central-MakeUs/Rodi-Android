package com.dororong.rodi.feature.home.review.notvisited

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiIconButton
import com.dororong.rodi.core.ui.components.dialog.RodiAlertDialog
import com.dororong.rodi.core.ui.components.dialog.RodiDialog
import com.dororong.rodi.core.ui.components.dialog.RodiUnsavedChangesDialog
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.R
import com.dororong.rodi.feature.home.detail.reviewactions.ReportReasonRow

@Composable
fun NotVisitedReasonScreen(onClose: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf<NotVisitedReason?>(null) }
    var detail by rememberSaveable { mutableStateOf("") }
    var isCompleteDialogVisible by rememberSaveable { mutableStateOf(false) }
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    val requestClose = {
        if ((selected != null || detail.isNotBlank()) && !isCompleteDialogVisible) {
            confirmExit = true
        } else {
            onClose()
        }
    }
    BackHandler(onBack = requestClose)

    NotVisitedReasonContent(
        selected = selected,
        detail = detail,
        onClose = requestClose,
        onSelect = { selected = it },
        onDetailChange = { detail = it },
        onSubmit = { isCompleteDialogVisible = true },
    )

    if (isCompleteDialogVisible) {
        NotVisitedCompletionDialog(onConfirm = onClose)
    }
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
private fun NotVisitedReasonContent(
    selected: NotVisitedReason?,
    detail: String,
    onClose: () -> Unit,
    onSelect: (NotVisitedReason) -> Unit,
    onDetailChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val canSubmit = selected != null && (selected != NotVisitedReason.OTHER || detail.isNotBlank())
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white)
            .statusBarsPadding()
            .imePadding(),
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
                text = "이유를 알려주시면 더 나은 코스를 추천해드릴게요!",
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.gray600,
            )
            Spacer(Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                NotVisitedReason.entries.forEach { reason ->
                    ReportReasonRow(
                        label = reason.label,
                        selected = selected == reason,
                        onClick = { onSelect(reason) },
                    )
                }
            }
            if (selected == NotVisitedReason.OTHER) {
                Spacer(Modifier.height(4.dp))
                NotVisitedDetailInput(
                    value = detail,
                    placeholder = "이유를 입력해주세요",
                    onValueChange = onDetailChange,
                )
            }
        }
        HorizontalDivider(color = RodiTheme.colors.gray200)
        RodiButton(
            text = "다음",
            onClick = onSubmit,
            enabled = canSubmit,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun NotVisitedDetailInput(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = RodiTheme.typography.body3Medium.copy(color = RodiTheme.colors.black),
        singleLine = true,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(RodiTheme.colors.primary600),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .border(
                width = 1.dp,
                color = if (isFocused) RodiTheme.colors.gray900 else RodiTheme.colors.gray300,
                shape = RoundedCornerShape(8.dp),
            ),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
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

@Preview(name = "미방문 사유 - 미선택", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun NotVisitedReasonEmptyPreview() = RodiTheme {
    NotVisitedReasonContent(null, "", {}, {}, {}, {})
}

@Preview(name = "미방문 사유 - 선택", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun NotVisitedReasonSelectedPreview() = RodiTheme {
    NotVisitedReasonContent(NotVisitedReason.TOO_FAR, "", {}, {}, {}, {})
}

@Preview(name = "미방문 사유 - 기타 입력", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun NotVisitedReasonOtherPreview() = RodiTheme {
    NotVisitedReasonContent(NotVisitedReason.OTHER, "다음에 다시 방문할게요.", {}, {}, {}, {})
}

@Preview(name = "미방문 사유 - 완료", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun NotVisitedReasonCompletePreview() = RodiTheme {
    NotVisitedReasonContent(NotVisitedReason.SCHEDULE, "", {}, {}, {}, {})
    RodiAlertDialog("확인", {}, {}, title = "소중한 의견 감사해요!")
}
