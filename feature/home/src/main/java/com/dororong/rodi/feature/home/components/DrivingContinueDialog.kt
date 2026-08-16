package com.dororong.rodi.feature.home.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.dororong.rodi.core.ui.components.dialog.RodiAlertDialog
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun DrivingContinueDialog(
    onContinue: () -> Unit,
    onStop: () -> Unit,
    onDismissRequest: () -> Unit = onContinue,
) {
    RodiAlertDialog(
        title = "주행 측정을 계속할까요?",
        description = "현재 코스의 연습 진행률을 측정하고 있어요.",
        confirmText = "계속 측정",
        onConfirm = onContinue,
        dismissText = "측정 종료",
        onDismiss = onStop,
        onDismissRequest = onDismissRequest,
        showCloseButton = true,
    )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun DrivingContinueDialogPreview() {
    RodiTheme {
        DrivingContinueDialog(onContinue = {}, onStop = {})
    }
}
