package com.dororong.rodi.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class RodiColors(
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
    val primary900: Color,
    val primary950: Color,
    val primary20: Color,
    // Point
    val pointRed: Color,
    val pointOrange: Color,
    val pointYellow: Color,
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
)

val LightRodiColors = RodiColors(
    primary50 = Color(0xFFF0EFFF),
    primary100 = Color(0xFFDBD9FF),
    primary200 = Color(0xFFBAB6FF),
    primary300 = Color(0xFF9D97FF),
    primary400 = Color(0xFF8278FF),
    primary500 = Color(0xFF7062FF),
    primary600 = Color(0xFF5640FF),
    primary700 = Color(0xFF3800F6),
    primary800 = Color(0xFF2600B1),
    primary900 = Color(0xFF13006D),
    primary950 = Color(0xFF0A004A),
    primary20 = Color(0xFFF4F4FF),
    pointRed = Color(0xFFC51D23),
    pointOrange = Color(0xFFFF8453),
    pointYellow = Color(0xFFFFB200),
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
)

val LocalRodiColors = staticCompositionLocalOf { LightRodiColors }
