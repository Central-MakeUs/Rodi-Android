package com.dororong.rodi.feature.home.review.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.common.graphemeLength
import com.dororong.rodi.core.domain.model.review.PracticeMethod
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.review.ReviewWriteUiState

@Composable
fun ReviewWriteDetail(
    state: ReviewWriteUiState,
    onPracticeMethod: (PracticeMethod) -> Unit,
    onContent: (String) -> Unit,
    isEditing: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        PracticeMethodBlock(state.practiceMethod, onPracticeMethod)
        ReviewContentBlock(state.content, onContent, isEditing)
    }
}

@Composable
private fun PracticeMethodBlock(
    practiceMethod: PracticeMethod?,
    onPracticeMethod: (PracticeMethod) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "연습 방법",
            modifier = Modifier.height(25.dp),
            style = RodiTheme.typography.body1SemiBold,
            color = RodiTheme.colors.black,
        )
        ChoicePairRow(
            startLabel = "혼자 연습",
            endLabel = "동승자와 연습",
            selected = when (practiceMethod) {
                PracticeMethod.SOLO -> false
                PracticeMethod.WITH_COMPANION -> true
                null -> null
            },
            onSelect = { isWithCompanion ->
                onPracticeMethod(if (isWithCompanion) PracticeMethod.WITH_COMPANION else PracticeMethod.SOLO)
            },
        )
    }
}

@Composable
private fun ReviewContentBlock(
    content: String,
    onContent: (String) -> Unit,
    isEditing: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("후기 작성", style = RodiTheme.typography.body1SemiBold, color = RodiTheme.colors.black)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.End,
        ) {
            ReviewTextField(
                value = content,
                onValueChange = onContent,
                placeholder = "자유롭게 후기를 작성해주세요.",
                multiline = true,
                maxGraphemes = 150,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = content.graphemeLength().toString(),
                    style = RodiTheme.typography.body3Medium,
                    color = RodiTheme.colors.gray600,
                )
                Text(
                    text = "/150",
                    style = RodiTheme.typography.body3Medium,
                    color = RodiTheme.colors.gray600,
                )
            }
            if (isEditing) {
                Text(
                    text = "레벨이 변경되면 해당 후기를 수정할 수 없어요.",
                    style = RodiTheme.typography.caption1Medium,
                    color = RodiTheme.colors.gray600,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(name = "후기 상세 - 미선택", showBackground = true, widthDp = 375)
@Composable
private fun ReviewWriteDetailEmptyPreview() = RodiTheme {
    ReviewWriteDetail(ReviewWriteUiState(), {}, {})
}

@Preview(name = "후기 상세 - 혼자", showBackground = true, widthDp = 375)
@Composable
private fun ReviewWriteDetailSoloPreview() = RodiTheme {
    ReviewWriteDetail(ReviewWriteUiState(practiceMethod = PracticeMethod.SOLO), {}, {})
}

@Preview(name = "후기 상세 - 동승", showBackground = true, widthDp = 375)
@Composable
private fun ReviewWriteDetailCompanionPreview() = RodiTheme {
    ReviewWriteDetail(ReviewWriteUiState(practiceMethod = PracticeMethod.WITH_COMPANION), {}, {})
}

@Preview(name = "후기 상세 - 내용", showBackground = true, widthDp = 375)
@Composable
private fun ReviewWriteDetailContentPreview() = RodiTheme {
    ReviewWriteDetail(ReviewWriteUiState(content = "초보 운전자도 연습하기 편했어요."), {}, {})
}

@Preview(name = "후기 수정 - 안내 문구", showBackground = true, widthDp = 375)
@Composable
private fun ReviewWriteDetailEditingPreview() = RodiTheme {
    ReviewWriteDetail(
        state = ReviewWriteUiState(
            practiceMethod = PracticeMethod.SOLO,
            content = "초보 운전자도 연습하기 편했어요.",
        ),
        onPracticeMethod = {},
        onContent = {},
        isEditing = true,
    )
}
