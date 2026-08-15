package com.dororong.rodi.core.ui.components.input

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GraphemeTextFieldStateTest {

    @Test
    fun `initial value is normalized before the text field is displayed`() {
        val normalized = normalizeGraphemeTextFieldValue("A😁B", maxGraphemes = 2)

        assertEquals("A😁", normalized.text)
        assertEquals(TextRange(3), normalized.selection)
    }

    @Test
    fun `changing max graphemes normalizes the externally supplied value`() {
        val normalized = normalizeGraphemeTextFieldValue("안녕😁", maxGraphemes = 2)

        assertEquals("안녕", normalized.text)
        assertEquals(TextRange(2), normalized.selection)
    }

    @Test
    fun `surrogate pair is limited as one grapheme`() {
        val normalized = TextFieldValue("A😁B", selection = TextRange(4)).limitGraphemes(2)

        assertEquals("A😁", normalized.text)
        assertEquals(TextRange(3), normalized.selection)
    }

    @Test
    fun `ZWJ emoji is limited as one grapheme`() {
        val family = "👨‍👩‍👧‍👦"

        assertEquals(family, TextFieldValue(family + "A").limitGraphemes(1).text)
    }

    @Test
    fun `selection and composing region are clamped to the grapheme boundary`() {
        val value = TextFieldValue(
            text = "A😁B",
            selection = TextRange(1, 4),
            composition = TextRange(1, 4),
        )

        val normalized = value.limitGraphemes(2)

        assertEquals("A😁", normalized.text)
        assertEquals(TextRange(1, 3), normalized.selection)
        assertEquals(TextRange(1, 3), normalized.composition)
    }

    @Test
    fun `reversed selection keeps its direction when clamped`() {
        val value = TextFieldValue(
            text = "A😁B",
            selection = TextRange(4, 1),
        )

        val normalized = value.limitGraphemes(2)

        assertEquals(TextRange(3, 1), normalized.selection)
    }

    @Test
    fun `composing region is kept when input is under the limit`() {
        val value = TextFieldValue(
            text = "안녕😁",
            selection = TextRange(4),
            composition = TextRange(0, 4),
        )

        val normalized = value.limitGraphemes(3)

        assertEquals(value, normalized)
        assertEquals(TextRange(0, 4), normalized.composition)
    }

    @Test
    fun `composition is cleared when it is fully removed by the limit`() {
        val value = TextFieldValue(
            text = "😁A",
            selection = TextRange(2),
            composition = TextRange(0, 2),
        )

        val normalized = value.limitGraphemes(0)

        assertEquals("", normalized.text)
        assertEquals(TextRange.Zero, normalized.selection)
        assertNull(normalized.composition)
    }
}
