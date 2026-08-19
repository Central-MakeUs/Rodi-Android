package com.dororong.rodi.core.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GraphemeCodeUnitTest {
    private val emoji = "😀" // 😀 : grapheme 1, code unit 2

    @Test
    fun `이모지는 grapheme 1개로 센다`() {
        assertEquals(1, emoji.graphemeLength())
        assertEquals(2, emoji.length)
    }

    @Test
    fun `code unit 상한을 넘지 않게 자르고 이모지를 쪼개지 않는다`() {
        val text = emoji.repeat(30)
        val limited = text.takeGraphemesWithinCodeUnits(30)
        assertEquals(30, limited.length)
        assertEquals(15, limited.graphemeLength())
    }

    @Test
    fun `한글은 grapheme과 code unit이 같아 30자 그대로 남는다`() {
        val text = "가".repeat(30)
        assertEquals(text, text.takeGraphemesWithinCodeUnits(30))
        assertEquals(30, text.graphemeLength())
    }
}
