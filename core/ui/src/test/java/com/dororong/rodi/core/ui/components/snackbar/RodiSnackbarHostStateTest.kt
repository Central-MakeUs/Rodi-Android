package com.dororong.rodi.core.ui.components.snackbar

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RodiSnackbarHostStateTest {
    @Test
    fun `id로 닫으면 대기 중인 해당 스낵바만 지우고 현재 스낵바는 남긴다`() {
        val state = RodiSnackbarHostState()
        state.show(RodiSnackbarData(message = "일반 알림"))
        state.show(RodiSnackbarData(id = "network", message = "네트워크 알림"))

        state.dismiss("network")

        assertEquals("일반 알림", state.current?.message)
        state.dismiss()
        state.advanceIfIdle()
        assertNull(state.current)
    }

    @Test
    fun `id로 닫으면 관계없는 대기 스낵바로 넘어간다`() {
        val state = RodiSnackbarHostState()
        state.show(RodiSnackbarData(id = "network", message = "네트워크 알림"))
        state.show(RodiSnackbarData(message = "일반 알림"))

        state.dismiss("network")
        state.advanceIfIdle()

        assertEquals("일반 알림", state.current?.message)
    }
}
