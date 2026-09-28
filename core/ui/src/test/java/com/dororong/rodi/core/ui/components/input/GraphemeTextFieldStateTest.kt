package com.dororong.rodi.core.ui.components.input

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GraphemeTextFieldStateTest {

    @Test
    fun `초기 값은 입력창에 표시되기 전에 정규화한다`() {
        val normalized = normalizeGraphemeTextFieldValue("A😁B", maxGraphemes = 2)

        assertEquals("A😁", normalized.text)
        assertEquals(TextRange(3), normalized.selection)
    }

    @Test
    fun `최대 글자 수가 바뀌면 외부에서 준 값을 정규화한다`() {
        val normalized = normalizeGraphemeTextFieldValue("안녕😁", maxGraphemes = 2)

        assertEquals("안녕", normalized.text)
        assertEquals(TextRange(2), normalized.selection)
    }

    @Test
    fun `서로게이트 쌍은 한 글자로 제한한다`() {
        val normalized = TextFieldValue("A😁B", selection = TextRange(4)).limitGraphemes(2)

        assertEquals("A😁", normalized.text)
        assertEquals(TextRange(3), normalized.selection)
    }

    @Test
    fun `ZWJ 이모지는 한 글자로 제한한다`() {
        val family = "👨‍👩‍👧‍👦"

        assertEquals(family, TextFieldValue(family + "A").limitGraphemes(1).text)
    }

    @Test
    fun `선택 영역과 조합 영역을 글자 경계에 맞춰 자른다`() {
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
    fun `역방향 선택은 잘려도 방향을 유지한다`() {
        val value = TextFieldValue(
            text = "A😁B",
            selection = TextRange(4, 1),
        )

        val normalized = value.limitGraphemes(2)

        assertEquals(TextRange(3, 1), normalized.selection)
    }

    @Test
    fun `제한보다 짧은 입력은 조합 영역을 유지한다`() {
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
    fun `외부 텍스트가 같으면 동기화해도 조합 상태를 유지한다`() {
        val value = TextFieldValue(
            text = "ㅂ",
            selection = TextRange(1),
            composition = TextRange(0, 1),
        )

        val normalized = value.syncWithExternalText("ㅂ", maxGraphemes = Int.MAX_VALUE)

        assertEquals(value, normalized)
    }

    @Test
    fun `외부 텍스트가 바뀌면 값을 교체한다`() {
        val value = TextFieldValue(
            text = "ㅂ",
            selection = TextRange(1),
            composition = TextRange(0, 1),
        )

        val normalized = value.syncWithExternalText("백", maxGraphemes = Int.MAX_VALUE)

        assertEquals("백", normalized.text)
        assertEquals(TextRange(1), normalized.selection)
        assertNull(normalized.composition)
    }

    @Test
    fun `조합 영역이 제한으로 모두 지워지면 조합 상태를 지운다`() {
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
