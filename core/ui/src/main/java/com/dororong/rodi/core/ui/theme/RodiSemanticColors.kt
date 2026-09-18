package com.dororong.rodi.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class RodiSemanticColors(
    val pinStart: Color,
    val pinArrival: Color,
    val pinVia: Color,
    val tagDangerBg: Color,
    /** 카카오 브랜드 가이드가 고정한 색 — 테마가 바뀌어도 따라 바뀌면 안 된다. */
    val brandKakao: Color,
    val onBrandKakao: Color,
    /** 지도 로딩 인디케이터 그라데이션의 중간 단계. primary 스케일에 없는 값이다. */
    val mapLoadingHighlight: Color,
)

val LightRodiSemanticColors = RodiSemanticColors(
    pinStart = Color(0xFF347BFF),
    pinArrival = Color(0xFFF3493C),
    pinVia = Color(0xFFFFD072),
    tagDangerBg = Color(0xFFFFD6D6),
    brandKakao = Color(0xFFFDE500),
    onBrandKakao = Color(0xFF222222),
    mapLoadingHighlight = Color(0xFFF4F4FF),
)

val LocalRodiSemanticColors = staticCompositionLocalOf { LightRodiSemanticColors }
