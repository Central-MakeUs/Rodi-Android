package com.dororong.rodi.feature.course.registration.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiButtonVariant
import com.dororong.rodi.core.ui.components.dialog.RodiAlertDialog
import com.dororong.rodi.core.ui.components.dialog.RodiDialog
import com.dororong.rodi.core.ui.components.dialog.RodiUnsavedChangesDialog
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.course.registration.CourseRegistrationDialog
import com.dororong.rodi.feature.course.registration.CourseRegistrationLoadingIndicator

@Composable
fun CourseRegistrationDialogHost(
    dialog: CourseRegistrationDialog,
    onContinueDraft: () -> Unit,
    onDiscardDraft: () -> Unit,
    onContinueWriting: () -> Unit,
    onExit: () -> Unit,
    onSuccessConfirmed: () -> Unit,
) {
    when (dialog) {
        CourseRegistrationDialog.ResumeDraft ->
            CourseRegistrationChoiceDialog(
                title = "작성 중인 코스가 있어요",
                description = "이전에 입력한 내용부터 등록할 수 있어요. 코스 등록을 이어서 할까요?",
                secondaryText = "새로 하기",
                primaryText = "이어서 하기",
                onSecondary = onDiscardDraft,
                onPrimary = onContinueDraft,
            )
        CourseRegistrationDialog.Exit ->
            RodiUnsavedChangesDialog(
                onContinueWriting = onContinueWriting,
                onExit = onExit,
                dismissible = false,
            )
        CourseRegistrationDialog.Success ->
            RodiAlertDialog(
                title = "등록 요청 완료!",
                description = "관리자의 검토 후 48시간 내에\n등록될 예정입니다.",
                confirmText = "확인",
                onConfirm = onSuccessConfirmed,
                onDismissRequest = {},
                dismissible = false,
            )
    }
}

@Composable
fun CourseRegistrationSubmissionLoadingDialog() {
    RodiDialog(
        onDismissRequest = {},
        modifier = Modifier.width(280.dp).heightIn(min = 226.dp),
        dismissible = false,
        contentPadding = PaddingValues(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(
                text = "등록 요청 중",
                modifier = Modifier.fillMaxWidth(),
                style = RodiTheme.typography.body1SemiBold,
                color = RodiTheme.colors.black,
                textAlign = TextAlign.Center,
            )
            CourseRegistrationLoadingIndicator()
        }
    }
}

@Composable
private fun CourseRegistrationChoiceDialog(
    title: String,
    description: String,
    secondaryText: String,
    primaryText: String,
    onSecondary: () -> Unit,
    onPrimary: () -> Unit,
) {
    RodiDialog(
        onDismissRequest = {},
        modifier = Modifier.width(280.dp),
        dismissible = false,
    ) {
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            style = RodiTheme.typography.price1,
            color = RodiTheme.colors.black,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = description,
            modifier = Modifier.fillMaxWidth(),
            style = RodiTheme.typography.caption1Medium,
            color = RodiTheme.colors.gray600,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RodiButton(
                text = secondaryText,
                onClick = onSecondary,
                modifier = Modifier.weight(1f),
                variant = RodiButtonVariant.Secondary,
                height = 42.dp,
            )
            RodiButton(
                text = primaryText,
                onClick = onPrimary,
                modifier = Modifier.weight(1f),
                height = 42.dp,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 250)
@Composable
private fun CourseRegistrationDialogPreview() {
    RodiTheme {
        CourseRegistrationChoiceDialog(
            title = "작성 중인 화면을 나갈까요?",
            description = "나가면 입력한 내용이 저장되지 않아요.",
            secondaryText = "나가기",
            primaryText = "계속 작성",
            onSecondary = {},
            onPrimary = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 260)
@Composable
private fun CourseRegistrationSubmissionLoadingDialogPreview() {
    RodiTheme { CourseRegistrationSubmissionLoadingDialog() }
}

@Preview(name = "Resume Draft Dialog", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationResumeDraftDialogPreview() {
    RodiTheme {
        CourseRegistrationDialogHost(
            dialog = CourseRegistrationDialog.ResumeDraft,
            onContinueDraft = {},
            onDiscardDraft = {},
            onContinueWriting = {},
            onExit = {},
            onSuccessConfirmed = {},
        )
    }
}

@Preview(name = "Exit Dialog", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationExitDialogPreview() {
    RodiTheme {
        CourseRegistrationDialogHost(
            dialog = CourseRegistrationDialog.Exit,
            onContinueDraft = {},
            onDiscardDraft = {},
            onContinueWriting = {},
            onExit = {},
            onSuccessConfirmed = {},
        )
    }
}

@Preview(name = "Success Dialog", showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationSuccessDialogPreview() {
    RodiTheme {
        CourseRegistrationDialogHost(
            dialog = CourseRegistrationDialog.Success,
            onContinueDraft = {},
            onDiscardDraft = {},
            onContinueWriting = {},
            onExit = {},
            onSuccessConfirmed = {},
        )
    }
}
