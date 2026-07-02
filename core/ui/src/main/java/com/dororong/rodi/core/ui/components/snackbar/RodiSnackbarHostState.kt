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

    // dismiss()가 current=null과 다음 아이템 대입을 같은 호출 안에서 연달아 하면, Compose
    // snapshot state는 리컴포지션 시점에 "마지막 값"만 관찰하므로 null 상태가 관찰되지 않고
    // 곧바로 다음 아이템으로 건너뛴다 — AnimatedVisibility의 exit/enter 애니메이션이 씹힌다.
    // 그래서 dismiss()는 null 대입만 하고, 다음 아이템으로의 전진은 RodiSnackbarHost의
    // LaunchedEffect(current)가 별도 코루틴 틱에서 [advanceIfIdle]을 호출해 수행한다.
    fun dismiss() {
        current = null
    }

    /** 표시 중이거나 대기 중인 스낵바를 전부 비운다(화면 이탈 시 등). */
    fun clear() {
        queue.clear()
        current = null
    }

    internal fun advanceIfIdle() {
        if (current == null && queue.isNotEmpty()) {
            current = queue.removeFirst()
        }
    }
}
