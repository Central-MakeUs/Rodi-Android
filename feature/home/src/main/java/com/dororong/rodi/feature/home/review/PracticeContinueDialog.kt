package com.dororong.rodi.feature.home.review

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.dororong.rodi.core.ui.components.dialog.RodiAlertDialog
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun PracticeContinueDialog(
    placeName: String,
    onContinue: () -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit,
) {
    RodiAlertDialog(
        confirmText = "계속 측정",
        onConfirm = onContinue,
        onDismissRequest = onDismiss,
        modifier = Modifier,
        dismissText = "측정 종료",
        onDismiss = onStop,
        showCloseButton = true,
        titleContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "‘$placeName’",
                    modifier = Modifier.fillMaxWidth(),
                    style = RodiTheme.typography.headline1,
                    color = RodiTheme.colors.primary600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "아직 코스를 연습 중이신가요?",
                    modifier = Modifier.fillMaxWidth(),
                    style = RodiTheme.typography.headline1,
                    color = RodiTheme.colors.black,
                    textAlign = TextAlign.Center,
                )
            }
        },
        description = "코스 주행을 이어서 측정할까요?",
    )
}

@Preview(name = "연습 측정 이어가기", showBackground = true, widthDp = 360)
@Composable
private fun PracticeContinueDialogPreview() {
    RodiTheme {
        PracticeContinueDialog(
            placeName = "성북구 드라이브 코스",
            onContinue = {},
            onStop = {},
            onDismiss = {},
        )
    }
}
