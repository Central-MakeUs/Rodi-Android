package com.dororong.rodi.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiButtonVariant
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun AccountRecoveryDialog(
    isRestoring: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Dialog는 별도 윈도우라 정적 프리뷰에서 렌더되지 않는다. 프리뷰에서는 내용만 그린다.
    if (LocalInspectionMode.current) {
        AccountRecoveryDialogContent(isRestoring, onConfirm, onDismiss)
        return
    }
    Dialog(
        onDismissRequest = { if (!isRestoring) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isRestoring,
            dismissOnClickOutside = !isRestoring,
        ),
    ) {
        AccountRecoveryDialogContent(isRestoring, onConfirm, onDismiss)
    }
}

@Composable
private fun AccountRecoveryDialogContent(
    isRestoring: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.width(290.dp),
        shape = RoundedCornerShape(12.dp),
        color = RodiTheme.colors.white,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "탈퇴 처리 중 계정",
                style = RodiTheme.typography.headline1,
                color = RodiTheme.colors.black,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "계정을 복구하시겠습니까?",
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.gray700,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                RodiButton(
                    text = if (isRestoring) "복구 중" else "예",
                    onClick = onConfirm,
                    variant = RodiButtonVariant.Secondary,
                    enabled = !isRestoring,
                    modifier = Modifier.weight(1f),
                )
                RodiButton(
                    text = "아니오",
                    onClick = onDismiss,
                    enabled = !isRestoring,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Preview(name = "계정 복구 안내 - 기본", showBackground = true, widthDp = 375, heightDp = 420)
@Composable
private fun AccountRecoveryDialogPreview() {
    RodiTheme { AccountRecoveryDialog(isRestoring = false, onConfirm = {}, onDismiss = {}) }
}

@Preview(name = "계정 복구 안내 - 복구 중", showBackground = true, widthDp = 375, heightDp = 420)
@Composable
private fun AccountRecoveryDialogRestoringPreview() {
    RodiTheme { AccountRecoveryDialog(isRestoring = true, onConfirm = {}, onDismiss = {}) }
}
