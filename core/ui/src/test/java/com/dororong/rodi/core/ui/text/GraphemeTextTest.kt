package com.dororong.rodi.core.ui.text

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GraphemeTextTest {

    @Test
    fun `counts plain text same as String length`() {
        assertEquals(5, "hello".graphemeLength())
        assertEquals(3, "가나다".graphemeLength())
    }

    @Test
    fun `counts a surrogate-pair emoji as one character`() {
        val text = "안전😁운전"
        assertEquals(6, text.length)
        assertEquals(5, text.graphemeLength())
    }

    @Test
    fun `counts a family emoji joined with ZWJ as one character`() {
        val family = "👨‍👩‍👧‍👦"
        assertEquals(1, family.graphemeLength())
    }

    @Test
    fun `takeGraphemes keeps text untouched when under the limit`() {
        assertEquals("안전😁", "안전😁".takeGraphemes(5))
    }

    @Test
    fun `takeGraphemes truncates without breaking a surrogate pair`() {
        val text = "안전😁운전😁"
        val truncated = text.takeGraphemes(3)
        assertEquals("안전😁", truncated)
        assertEquals(3, truncated.graphemeLength())
    }

    @Test
    fun `takeGraphemes with zero or negative limit returns empty`() {
        assertEquals("", "안전😁".takeGraphemes(0))
        assertEquals("", "안전😁".takeGraphemes(-1))
    }

    @Test
    fun `takeGraphemes keeps thirty graphemes even when UTF-16 length is greater`() {
        val text = "가".repeat(15) + "😀".repeat(15)

        val limited = text.takeGraphemes(30)

        assertEquals(text, limited)
        assertEquals(45, limited.length)
        assertEquals(30, limited.graphemeLength())
    }

    @Test
    fun `이모지는 grapheme 1개로 센다`() {
        val emoji = "😀"
        assertEquals(1, emoji.graphemeLength())
        assertEquals(2, emoji.length)
    }

    @Test
    fun `이모지 30개는 code unit이 60이어도 30개 그대로 남는다`() {
        val text = "😀".repeat(30)
        val limited = text.takeGraphemes(30)
        assertEquals(text, limited)
        assertEquals(60, limited.length)
        assertEquals(30, limited.graphemeLength())
    }

    @Test
    fun `한글 30자는 그대로 남는다`() {
        val text = "가".repeat(30)
        assertEquals(text, text.takeGraphemes(30))
        assertEquals(30, text.graphemeLength())
    }
}
