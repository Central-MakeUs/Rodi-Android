package com.dororong.rodi.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class RodiSemanticColors(
    val pinStart: Color,
    val pinArrival: Color,
    val tagDangerBg: Color,
)

val LightRodiSemanticColors = RodiSemanticColors(
    pinStart = Color(0xFF347BFF),
    pinArrival = Color(0xFFF3493C),
    tagDangerBg = Color(0xFFFFD6D6),
)

val LocalRodiSemanticColors = staticCompositionLocalOf { LightRodiSemanticColors }
