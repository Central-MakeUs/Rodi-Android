package com.dororong.rodi.feature.home.review

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.dororong.rodi.core.ui.components.dialog.RodiAlertDialog
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun NotificationPermissionDialog(
    onAllow: () -> Unit,
    onRouteOnly: () -> Unit,
) {
    RodiAlertDialog(
        confirmText = "알림 허용하기",
        onConfirm = onAllow,
        onDismissRequest = onRouteOnly,
        dismissText = "경로만 보기",
        onDismiss = onRouteOnly,
        dismissible = false,
        title = "주행 기록 측정을 위해\n알림을 허용해 주세요",
        description = "앱을 나가도 주행 상태를 확인하고\n측정을 종료할 수 있도록 알림을 사용해요.",
    )
}

@Preview(name = "알림 권한 안내", showBackground = true, widthDp = 360)
@Composable
private fun NotificationPermissionDialogPreview() {
    RodiTheme {
        NotificationPermissionDialog(onAllow = {}, onRouteOnly = {})
    }
}
