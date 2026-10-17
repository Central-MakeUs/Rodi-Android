package com.dororong.rodi.core.ui.components.input

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun RodiFieldLabel(
    text: String,
    required: Boolean,
    modifier: Modifier = Modifier,
    assistiveText: String? = null,
) {
    Text(
        text = buildAnnotatedString {
            append(text)
            if (required) {
                withStyle(RodiTheme.typography.body1SemiBold.toSpanStyle().copy(color = RodiTheme.colors.primary600)) {
                    append("\u00A0*")
                }
            } else {
                withStyle(RodiTheme.typography.body3Medium.toSpanStyle().copy(color = RodiTheme.colors.gray600)) {
                    append("\u00A0(선\u2060택)")
                }
            }
            assistiveText?.let {
                withStyle(RodiTheme.typography.body3Medium.toSpanStyle().copy(color = RodiTheme.colors.gray600)) {
                    append("  $it")
                }
            }
        },
        modifier = modifier,
        style = RodiTheme.typography.body1SemiBold,
        color = RodiTheme.colors.black,
    )
}

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun RodiFieldLabelPreview() {
    RodiTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            RodiFieldLabel("연습유형", required = true)
            RodiFieldLabel("주의사항 작성", required = false)
            RodiFieldLabel("면허 취득후 도로주행을 해본 적이 있나요?", required = true, assistiveText = "복수선택")
            RodiFieldLabel("더 연습해보고 싶은 상황이 있나요?", required = false, assistiveText = "최대 3개")
        }
    }
}
