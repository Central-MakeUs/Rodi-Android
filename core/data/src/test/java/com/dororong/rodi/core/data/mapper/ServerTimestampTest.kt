package com.dororong.rodi.core.data.mapper

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ServerTimestampTest {
    @Test
    fun `Z 접미사가 붙은 UTC 시각을 해석한다`() {
        assertEquals(
            Instant.parse("2026-08-10T10:47:33.996642Z"),
            parseServerTimestamp("2026-08-10T10:47:33.996642Z"),
        )
    }

    @Test
    fun `오프셋이 명시된 시각을 해석한다`() {
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
    fun `오프셋 없는 시각은 서비스 시간대로 해석한다`() {
        val expected = ZonedDateTime.of(2026, 8, 10, 10, 47, 33, 996_642_000, ZoneId.of("Asia/Seoul"))
            .toInstant()

        assertEquals(expected, parseServerTimestamp("2026-08-10T10:47:33.996642"))
    }

    @Test
    fun `소수 초가 없는 오프셋 없는 시각도 해석한다`() {
        val expected = ZonedDateTime.of(2026, 8, 10, 10, 47, 33, 0, ZoneId.of("Asia/Seoul")).toInstant()

        assertEquals(expected, parseServerTimestamp("2026-08-10T10:47:33"))
    }

    @Test
    fun `해석할 수 없는 값이면 예외를 던진다`() {
        assertThrows(IllegalArgumentException::class.java) {
            parseServerTimestamp("not-a-timestamp")
        }
    }
}
