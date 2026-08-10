package com.dororong.rodi.feature.home.review.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun ReviewTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    multiline: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = RodiTheme.typography.body3Medium.copy(color = RodiTheme.colors.black),
        modifier = modifier
            .fillMaxWidth()
            .height(if (multiline) 100.dp else 50.dp)
            .border(
                width = 1.dp,
                color = if (isFocused) RodiTheme.colors.gray900 else RodiTheme.colors.gray300,
                shape = RoundedCornerShape(8.dp),
            ),
        singleLine = !multiline,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(RodiTheme.colors.primary600),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 16.dp,
                        vertical = if (multiline) 16.dp else 0.dp,
                    ),
                contentAlignment = if (multiline) Alignment.TopStart else Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = RodiTheme.typography.body3Medium,
                        color = RodiTheme.colors.gray500,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Preview(name = "후기 입력 - 안내", showBackground = true, widthDp = 375)
@Composable
private fun ReviewTextFieldPlaceholderPreview() = RodiTheme {
    ReviewTextField("", {}, "자유롭게 후기를 작성해주세요.", multiline = true)
}

@Preview(name = "후기 입력 - 작성", showBackground = true, widthDp = 375)
@Composable
private fun ReviewTextFieldFilledPreview() = RodiTheme {
    ReviewTextField("초보 운전자가 연습하기 좋았어요.", {}, "", multiline = true)
}

@Preview(name = "후기 입력 - 최대 글자", showBackground = true, widthDp = 375)
@Composable
private fun ReviewTextFieldMaxPreview() = RodiTheme {
    Column {
        ReviewTextField("후기 ".repeat(50), {}, "", multiline = true)
        Text(
            text = "150/150",
            modifier = Modifier.fillMaxWidth(),
            style = RodiTheme.typography.caption1Medium,
            color = RodiTheme.colors.gray500,
            textAlign = TextAlign.End,
        )
    }
}
