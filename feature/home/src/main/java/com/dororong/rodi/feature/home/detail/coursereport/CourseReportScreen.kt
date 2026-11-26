package com.dororong.rodi.feature.home.detail.coursereport

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.ui.components.dialog.RodiAlertDialog
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarData
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHost
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHostState
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.detail.reviewactions.ReportSubmittedDialog

@Composable
fun CourseReportScreen(
    courseId: Long,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CourseReportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { RodiSnackbarHostState() }
    val close = {
        if (!state.isSubmitting) {
            viewModel.close()
            onClose()
        }
    }

    LaunchedEffect(courseId) { viewModel.load(courseId) }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.show(RodiSnackbarData(message = message))
            viewModel.consumeError()
        }
    }
    BackHandler(onBack = close)

    Box(modifier.fillMaxSize()) {
        val form = state.form
        if (form == null) {
            Surface(Modifier.fillMaxSize(), color = RodiTheme.colors.white) {
                if (state.isLoading) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RodiTheme.colors.primary600)
                    }
                }
            }
        } else {
            CourseReportContent(
                form = form,
                selected = state.selected,
                detail = state.detail,
                isSubmissionEnabled = state.canSubmit,
                onBack = close,
                onToggle = viewModel::toggle,
                onDetail = viewModel::updateDetail,
                onSubmit = viewModel::submit,
            )
        }
        RodiSnackbarHost(snackbarHostState)
    }

    state.loadErrorMessage?.let { message ->
        RodiAlertDialog(
            confirmText = "다시 시도",
            onConfirm = viewModel::retry,
            dismissText = "닫기",
            onDismiss = close,
            onDismissRequest = close,
            description = message,
        )
    }
    if (state.isSubmitted) ReportSubmittedDialog(onConfirm = close)
}
