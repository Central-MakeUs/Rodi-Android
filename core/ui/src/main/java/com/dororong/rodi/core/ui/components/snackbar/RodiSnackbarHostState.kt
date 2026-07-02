package com.dororong.rodi.core.ui.components.snackbar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

class RodiSnackbarHostState {
    private val queue = ArrayDeque<RodiSnackbarData>()

    var current: RodiSnackbarData? by mutableStateOf(null)
        private set

    fun show(data: RodiSnackbarData) {
        queue.addLast(data)
    }

    fun showImmediately(data: RodiSnackbarData) {
        queue.addFirst(data)
    }

    suspend fun observe() {
        while (true) {
            if (current == null && queue.isNotEmpty()) {
                current = queue.removeFirst()
                if (current?.duration != RodiSnackbarDuration.Indefinite) {
                    delay(current?.duration?.millis ?: 0L)
                    current = null
                }
            } else {
                delay(100)
            }
        }
    }

    fun dismiss() {
        current = null
    }
}
