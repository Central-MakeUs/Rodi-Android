package com.dororong.rodi.feature.home.review

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.dororong.rodi.core.domain.model.member.PracticeRecordItem
import com.dororong.rodi.core.ui.components.dialog.RodiAlertDialog
import com.dororong.rodi.core.ui.theme.RodiTheme
import java.time.Instant

@Composable
fun PracticePromptDialog(
    practice: PracticeRecordItem,
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
                    text = "'${practice.placeName}'",
                    modifier = Modifier.fillMaxWidth(),
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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            PracticePromptDialog(
                practice = PracticeRecordItem(1L, 1L, "강남역 코스", emptyList(), 0, Instant.EPOCH, false, false),
                onVisited = {},
                onNotVisited = {},
                onDismiss = {},
            )
        }
    }
}

@Preview(name = "연습 완료 팝업 - 긴 코스명", showBackground = true, widthDp = 360)
@Composable
private fun PracticePromptLongPreview() {
    RodiTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            PracticePromptDialog(
                practice = PracticeRecordItem(
                    practiceId = 1L,
                    placeId = 1L,
                    placeName = "서울특별시 강남구 테헤란로 초보 운전 연습 코스",
                    practiceTypes = emptyList(),
                    visitCount = 0,
                    visitedAt = null,
                    isVerified = false,
                    hasReview = false,
                ),
                onVisited = {},
                onNotVisited = {},
                onDismiss = {},
            )
        }
    }
}
