package com.dororong.rodi.core.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UserMessageTest {

    @Test
    fun `preserves a message from an approved provider`() {
        assertEquals("다시 로그인해주세요.", ApprovedException("다시 로그인해주세요.").userMessage())
    }

    @Test
    fun `hides a message from an unapproved throwable`() {
        assertEquals(
            "요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요.",
            IllegalStateException("JSON field 'accessToken' is missing").userMessage(),
        )
    }

    @Test
    fun `falls back when an approved message is blank`() {
        assertEquals(
            "요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요.",
            ApprovedException(" ").userMessage(),
        )
    }

    private class ApprovedException(
        override val userMessage: String,
    ) : Exception(userMessage), UserMessageProvider
}
