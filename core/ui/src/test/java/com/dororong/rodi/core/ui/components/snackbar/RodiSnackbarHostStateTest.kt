package com.dororong.rodi.core.ui.components.snackbar

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RodiSnackbarHostStateTest {
    @Test
    fun `dismiss by id leaves an unrelated current snackbar visible`() {
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
    fun `dismiss by id advances to an unrelated queued snackbar`() {
        val state = RodiSnackbarHostState()
        state.show(RodiSnackbarData(id = "network", message = "네트워크 알림"))
        state.show(RodiSnackbarData(message = "일반 알림"))

        state.dismiss("network")
        state.advanceIfIdle()

        assertEquals("일반 알림", state.current?.message)
    }
}
