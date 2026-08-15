package com.dororong.rodi.core.ui.components.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.dororong.rodi.core.common.takeGraphemes

data class GraphemeTextFieldState(
    val value: TextFieldValue,
    val onValueChange: (TextFieldValue) -> Unit,
)

@Composable
fun rememberGraphemeTextFieldState(
    text: String,
    maxGraphemes: Int,
    onTextChange: (String) -> Unit,
): GraphemeTextFieldState {
    var value by remember {
        mutableStateOf(normalizeGraphemeTextFieldValue(text, maxGraphemes))
    }
    val latestOnTextChange by rememberUpdatedState(onTextChange)

    LaunchedEffect(text, maxGraphemes) {
        val normalizedValue = normalizeGraphemeTextFieldValue(text, maxGraphemes)
        if (value != normalizedValue) {
            value = normalizedValue
        }
        if (normalizedValue.text != text) {
            latestOnTextChange(normalizedValue.text)
        }
    }

    return GraphemeTextFieldState(
        value = value,
        onValueChange = { updatedValue ->
            val previousText = value.text
            val normalizedValue = updatedValue.limitGraphemes(maxGraphemes)
            value = normalizedValue
            if (normalizedValue.text != previousText) {
                latestOnTextChange(normalizedValue.text)
            }
        },
    )
}

internal fun normalizeGraphemeTextFieldValue(text: String, maxGraphemes: Int): TextFieldValue =
    TextFieldValue(text, selection = TextRange(text.length)).limitGraphemes(maxGraphemes)

internal fun TextFieldValue.limitGraphemes(maxGraphemes: Int): TextFieldValue {
    val limitedText = text.takeGraphemes(maxGraphemes)
    if (limitedText == text) return this

    val limitedLength = limitedText.length
    val limitedSelection = selection.clampSelectionTo(limitedLength)
    val limitedComposition = composition?.let { it.clampTo(limitedLength) }

    return copy(
        text = limitedText,
        selection = limitedSelection,
        composition = limitedComposition,
    )
}

private fun TextRange.clampSelectionTo(length: Int): TextRange {
    val clampedStart = start.coerceIn(0, length)
    val clampedEnd = end.coerceIn(0, length)
    return TextRange(clampedStart, clampedEnd)
}

private fun TextRange.clampTo(length: Int): TextRange? {
    val clamped = clampSelectionTo(length)
    return clamped.takeUnless { it.start == it.end }
}
