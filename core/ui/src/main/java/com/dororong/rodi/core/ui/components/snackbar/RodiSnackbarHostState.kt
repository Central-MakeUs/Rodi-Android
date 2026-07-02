package com.dororong.rodi.core.ui.components.snackbar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class RodiSnackbarHostState {
    private val queue = ArrayDeque<RodiSnackbarData>()

    var current: RodiSnackbarData? by mutableStateOf(null)
        private set

    fun show(data: RodiSnackbarData) {
        queue.addLast(data)
        advanceIfIdle()
    }

    fun showImmediately(data: RodiSnackbarData) {
        queue.addFirst(data)
        advanceIfIdle()
    }

    fun dismiss() {
        current = null
        advanceIfIdle()
    }

    /** 표시 중이거나 대기 중인 스낵바를 전부 비운다(화면 이탈 시 등). */
    fun clear() {
        queue.clear()
        current = null
    }

    private fun advanceIfIdle() {
        if (current == null && queue.isNotEmpty()) {
            current = queue.removeFirst()
        }
    }
}
