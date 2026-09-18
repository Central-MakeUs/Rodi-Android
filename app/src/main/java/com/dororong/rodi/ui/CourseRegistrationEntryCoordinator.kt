package com.dororong.rodi.ui

import androidx.compose.runtime.Composable
import com.dororong.rodi.core.domain.model.course.CourseDraft
import com.dororong.rodi.core.ui.components.dialog.RodiAlertDialog
import com.dororong.rodi.feature.course.registration.CourseRegistrationDialog
import com.dororong.rodi.feature.course.registration.CourseRegistrationIntent

internal fun CourseDraft?.courseRegistrationPreflightDecision(): CourseRegistrationPreflightDecision =
    if (this?.isMeaningful == true) {
        CourseRegistrationPreflightDecision.ShowResumeDialog
    } else {
        CourseRegistrationPreflightDecision.OpenImmediately
    }

internal fun courseRegistrationClearDraftFailureMessage(error: Throwable): String =
    error.message?.takeIf { it.isNotBlank() }
        ?: "작성 중인 코스를 삭제하지 못했어요. 다시 시도해주세요."

internal fun courseRegistrationIntentForEntry(
    entryMode: CourseRegistrationEntryMode,
    dialog: CourseRegistrationDialog?,
): CourseRegistrationIntent? {
    if (dialog != CourseRegistrationDialog.ResumeDraft) return null
    return when (entryMode) {
        CourseRegistrationEntryMode.StartFresh -> CourseRegistrationIntent.DraftDiscardClicked
        CourseRegistrationEntryMode.ContinueDraft,
        CourseRegistrationEntryMode.Normal,
        -> CourseRegistrationIntent.DraftContinueClicked
    }
}

@Composable
internal fun CourseRegistrationResumeDialog(
    onStartFresh: () -> Unit,
    onContinue: () -> Unit,
) {
    RodiAlertDialog(
        confirmText = "이어서 하기",
        onConfirm = onContinue,
        onDismissRequest = {},
        title = "작성 중인 코스가 있어요",
        description = "이전에 입력한 내용부터 등록할 수 있어요. 코스 등록을 이어서 할까요?",
        dismissText = "새로 하기",
        onDismiss = onStartFresh,
        dismissible = false,
    )
}
