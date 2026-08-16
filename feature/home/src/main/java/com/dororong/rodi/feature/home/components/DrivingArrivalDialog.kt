package com.dororong.rodi.feature.home.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.dororong.rodi.core.ui.components.dialog.RodiAlertDialog
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun DrivingArrivalDialog(
    onConfirm: () -> Unit,
) {
    RodiAlertDialog(
        title = "목적지에 도착한 것 같아요",
        description = "안전한 장소에 정차한 후\n운전 기록을 확인해 주세요.",
        confirmText = "확인",
        onConfirm = onConfirm,
        onDismissRequest = {},
        dismissible = false,
    )
}

@Preview(showBackground = true, widthDp = 375, heightDp = 420)
@Composable
private fun DrivingArrivalDialogPreview() {
    RodiTheme {
        DrivingArrivalDialog(onConfirm = {})
    }
}
