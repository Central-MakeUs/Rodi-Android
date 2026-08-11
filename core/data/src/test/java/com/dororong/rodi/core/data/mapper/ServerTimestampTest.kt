package com.dororong.rodi.core.data.mapper

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ServerTimestampTest {
    @Test
    fun `parses UTC instant with Z suffix`() {
        assertEquals(
            Instant.parse("2026-08-10T10:47:33.996642Z"),
            parseServerTimestamp("2026-08-10T10:47:33.996642Z"),
        )
    }

    @Test
    fun `parses value with explicit offset`() {
        assertEquals(
            Instant.parse("2026-08-10T01:47:33.996642Z"),
            parseServerTimestamp("2026-08-10T10:47:33.996642+09:00"),
        )
    }

    /**
     * 서버가 `format: date-time`으로 선언해두고 오프셋 없이 내려보내던 실제 값.
     * 이 케이스가 예외를 던져 내 게시글·차단목록·코스 후기 목록이 통째로 비어 보였다.
     */
    @Test
    fun `parses offset-less value as service timezone`() {
        val expected = ZonedDateTime.of(2026, 8, 10, 10, 47, 33, 996_642_000, ZoneId.of("Asia/Seoul"))
            .toInstant()

        assertEquals(expected, parseServerTimestamp("2026-08-10T10:47:33.996642"))
    }

    @Test
    fun `parses offset-less value without fractional seconds`() {
        val expected = ZonedDateTime.of(2026, 8, 10, 10, 47, 33, 0, ZoneId.of("Asia/Seoul")).toInstant()

        assertEquals(expected, parseServerTimestamp("2026-08-10T10:47:33"))
    }

    @Test
    fun `throws on unparseable value`() {
        assertThrows(IllegalArgumentException::class.java) {
            parseServerTimestamp("not-a-timestamp")
        }
    }
}
