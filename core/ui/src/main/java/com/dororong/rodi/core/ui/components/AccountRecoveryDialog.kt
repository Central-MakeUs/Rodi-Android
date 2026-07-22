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
    Dialog(
        onDismissRequest = { if (!isRestoring) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isRestoring,
            dismissOnClickOutside = !isRestoring,
        ),
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
                    text = "탈퇴 요청한 계정이에요",
                    style = RodiTheme.typography.headline1,
                    color = RodiTheme.colors.black,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "탈퇴 유예 기간이 끝나기 전까지\n기존 계정을 복구할 수 있어요.",
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
                        text = "취소",
                        onClick = onDismiss,
                        variant = RodiButtonVariant.Secondary,
                        enabled = !isRestoring,
                        modifier = Modifier.weight(1f),
                    )
                    RodiButton(
                        text = if (isRestoring) "복구 중" else "계정 복구",
                        onClick = onConfirm,
                        enabled = !isRestoring,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 420)
@Composable
private fun AccountRecoveryDialogPreview() {
    RodiTheme { AccountRecoveryDialog(false, {}, {}) }
}
