package com.dororong.rodi.feature.home.review

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.dororong.rodi.core.domain.model.practice.PracticeSession
import com.dororong.rodi.core.ui.components.dialog.RodiAlertDialog
import com.dororong.rodi.core.ui.theme.RodiTheme
import java.time.Instant

@Composable
fun PracticePromptDialog(
    session: PracticeSession,
    onVisited: () -> Unit,
    onNotVisited: () -> Unit,
    onDismiss: () -> Unit,
) {
    RodiAlertDialog(
        confirmText = "다녀왔어요",
        onConfirm = onVisited,
        onDismissRequest = onDismiss,
        dismissText = "안 했어요",
        onDismiss = onNotVisited,
        showCloseButton = true,
        description = "연습 기록을 남겨 늘어나는 실력을\n한눈에 확인해보세요.",
        titleContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "'${session.placeName}'",
                    style = RodiTheme.typography.headline1,
                    color = RodiTheme.colors.primary600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "연습은 잘 다녀오셨나요?",
                    modifier = Modifier.fillMaxWidth(),
                    style = RodiTheme.typography.headline1,
                    color = RodiTheme.colors.black,
                    textAlign = TextAlign.Center,
                )
            }
        },
    )
}

@Preview(name = "연습 완료 팝업 - 짧은 코스명", showBackground = true, widthDp = 360)
@Composable
private fun PracticePromptShortPreview() {
    RodiTheme {
        PracticePromptDialog(
            session = PracticeSession(1L, "강남역 코스", Instant.EPOCH),
            onVisited = {},
            onNotVisited = {},
            onDismiss = {},
        )
    }
}

@Preview(name = "연습 완료 팝업 - 긴 코스명", showBackground = true, widthDp = 360)
@Composable
private fun PracticePromptLongPreview() {
    RodiTheme {
        PracticePromptDialog(
            session = PracticeSession(
                placeId = 1L,
                placeName = "서울특별시 강남구 테헤란로 초보 운전 연습 코스",
                startedAt = Instant.EPOCH,
            ),
            onVisited = {},
            onNotVisited = {},
            onDismiss = {},
        )
    }
}
