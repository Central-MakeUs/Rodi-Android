package com.dororong.rodi.core.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UserMessageTest {

    @Test
    fun `사용자 문구를 제공하는 예외의 메시지는 그대로 쓴다`() {
        assertEquals("다시 로그인해주세요.", ApprovedException("다시 로그인해주세요.").userMessage())
    }

    @Test
    fun `사용자 문구를 제공하지 않는 예외의 메시지는 숨긴다`() {
        assertEquals(
            "요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요.",
            IllegalStateException("JSON field 'accessToken' is missing").userMessage(),
        )
    }

    @Test
    fun `사용자 문구를 제공하지 않는 예외에는 호출자가 준 대체 문구를 쓴다`() {
        assertEquals(
            "저장목록을 불러오지 못했어요.",
            IllegalStateException("JSON field 'totalCount' is missing")
                .userMessage("저장목록을 불러오지 못했어요."),
        )
    }

    @Test
    fun `예외가 없으면 호출자가 준 대체 문구를 쓴다`() {
        assertEquals("저장목록을 불러오지 못했어요.", null.userMessage("저장목록을 불러오지 못했어요."))
    }

    @Test
    fun `제공된 사용자 문구가 비어 있으면 대체 문구를 쓴다`() {
        assertEquals(
            "요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요.",
            ApprovedException(" ").userMessage(),
        )
    }

    private class ApprovedException(
        override val userMessage: String,
    ) : Exception(userMessage), UserMessageProvider
}
