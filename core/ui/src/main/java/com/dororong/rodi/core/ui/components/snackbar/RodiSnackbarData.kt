package com.dororong.rodi.core.ui.components.snackbar

import androidx.compose.ui.graphics.painter.Painter

enum class RodiSnackbarDuration(val millis: Long) {
    Short(2_000),
    /** 에러 토스트 기본값. 디자인이 여러 화면에서 3초 자동 dismiss로 명시한다. */
    Medium(3_000),
    Extended(3_500),
    Indefinite(Long.MAX_VALUE),
}

data class RodiSnackbarData(
    val id: String? = null,
    val message: String,
    val icon: Painter? = null,
    val duration: RodiSnackbarDuration = RodiSnackbarDuration.Short,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)
