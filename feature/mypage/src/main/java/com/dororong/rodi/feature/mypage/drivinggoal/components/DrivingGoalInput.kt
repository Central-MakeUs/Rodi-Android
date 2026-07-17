package com.dororong.rodi.feature.mypage.drivinggoal.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
internal fun DrivingGoalInput(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = RodiTheme.typography.body3Medium.copy(color = RodiTheme.colors.black),
            cursorBrush = SolidColor(RodiTheme.colors.black),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused }
                .border(
                    width = 1.dp,
                    color = if (isFocused || value.isBlank()) RodiTheme.colors.black else RodiTheme.colors.gray300,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 16.dp),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) { innerTextField() }
            },
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            Text(
                text = "${value.length} /30",
                style = RodiTheme.typography.caption2Medium,
                color = RodiTheme.colors.gray500,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun DrivingGoalInputEmptyPreview() {
    RodiTheme {
        DrivingGoalInput(
            value = "",
            onValueChange = {},
            focusRequester = FocusRequester(),
        )
    }
}

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun DrivingGoalInputFilledPreview() {
    RodiTheme {
        DrivingGoalInput(
            value = "복잡한 강남 자신있게 운전하기",
            onValueChange = {},
            focusRequester = FocusRequester(),
        )
    }
}
