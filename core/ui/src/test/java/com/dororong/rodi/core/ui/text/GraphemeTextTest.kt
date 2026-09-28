package com.dororong.rodi.core.ui.text

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GraphemeTextTest {

    @Test
    fun `일반 텍스트는 String 길이와 같게 센다`() {
        assertEquals(5, "hello".graphemeLength())
        assertEquals(3, "가나다".graphemeLength())
    }

    @Test
    fun `서로게이트 쌍 이모지는 한 글자로 센다`() {
        val text = "안전😁운전"
        assertEquals(6, text.length)
        assertEquals(5, text.graphemeLength())
    }

    @Test
    fun `ZWJ로 이어진 가족 이모지는 한 글자로 센다`() {
        val family = "👨‍👩‍👧‍👦"
        assertEquals(1, family.graphemeLength())
    }

    @Test
    fun `제한보다 짧으면 takeGraphemes는 텍스트를 그대로 둔다`() {
        assertEquals("안전😁", "안전😁".takeGraphemes(5))
    }

    @Test
    fun `takeGraphemes는 서로게이트 쌍을 깨지 않고 자른다`() {
        val text = "안전😁운전😁"
        val truncated = text.takeGraphemes(3)
        assertEquals("안전😁", truncated)
        assertEquals(3, truncated.graphemeLength())
    }

    @Test
    fun `제한이 0 이하면 takeGraphemes는 빈 문자열을 반환한다`() {
        assertEquals("", "안전😁".takeGraphemes(0))
        assertEquals("", "안전😁".takeGraphemes(-1))
    }

    @Test
    fun `UTF-16 길이가 더 길어도 takeGraphemes는 30글자를 유지한다`() {
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
