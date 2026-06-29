package com.dororong.rodi.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class RoutiColors(
    // Primary
    val primary50: Color,
    val primary100: Color,
    val primary200: Color,
    val primary300: Color,
    val primary400: Color,
    val primary500: Color,
    val primary600: Color,
    val primary700: Color,
    val primary800: Color,
    // Gray
    val gray50: Color,
    val gray100: Color,
    val gray200: Color,
    val gray300: Color,
    val gray400: Color,
    val gray500: Color,
    val gray600: Color,
    val gray700: Color,
    val gray800: Color,
    val gray900: Color,
    // Base
    val black: Color,
    val white: Color,
    val handleBar: Color,
    // Semantic
    val pinStart: Color,
    val pinArrival: Color,
    val tagDangerBg: Color,
)

val LightRoutiColors = RoutiColors(
    primary50 = Color(0xFFF0EFFF),
    primary100 = Color(0xFFF5F5F0),
    primary200 = Color(0xFFBAB6FF),
    primary300 = Color(0xFF9D97FF),
    primary400 = Color(0xFF8278FF),
    primary500 = Color(0xFF6C5CFF),
    primary600 = Color(0xFF5640FF),
    primary700 = Color(0xFF3800F6),
    primary800 = Color(0xFF2600B1),
    gray50 = Color(0xFFFAFAFA),
    gray100 = Color(0xFFF5F5F5),
    gray200 = Color(0xFFEFEFEF),
    gray300 = Color(0xFFE1E1E1),
    gray400 = Color(0xFFBEBEBE),
    gray500 = Color(0xFF9F9F9F),
    gray600 = Color(0xFF767676),
    gray700 = Color(0xFF626262),
    gray800 = Color(0xFF434343),
    gray900 = Color(0xFF323232),
    black = Color(0xFF222222),
    white = Color(0xFFFFFFFF),
    handleBar = Color(0xFFC6C8CB),
    pinStart = Color(0xFF347BFF),
    pinArrival = Color(0xFFF3493C),
    tagDangerBg = Color(0xFFFFD6D6),
)

val LocalRoutiColors = staticCompositionLocalOf { LightRoutiColors }
