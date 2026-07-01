package com.dororong.rodi.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.dororong.rodi.R

/** Pretendard 폰트 패밀리. app/src/main/res/font/ 에 4 weight 배치 필요. */
val RodiFontFamily = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
)

/**
 * Rodi 타이포그래피 토큰 (Figma 디자인 시스템 기준).
 * lineHeight = 크기 × 1.3 (button1만 1.4), letterSpacing = -2% (-0.02em).
 * `RodiTheme.typography.xxx` 로 접근한다.
 */
@Immutable
data class RodiTypography(
    val headline1: TextStyle,       // Bold 18
    val body1SemiBold: TextStyle,   // SemiBold 16
    val body1Medium: TextStyle,
    val body3Medium: TextStyle,     // Medium 14
    val body3SemiBold: TextStyle,   // SemiBold 14
    val caption1Regular: TextStyle, // Regular 13
    val caption1Medium: TextStyle,  // Medium 13
    val caption1SemiBold: TextStyle,// SemiBold 13
    val caption2SemiBold: TextStyle,// SemiBold 12
    val caption3Medium: TextStyle,  // Medium 10
    val button1: TextStyle,         // Medium 16 / lh 1.4
)

private fun rodiStyle(
    size: Int,
    weight: FontWeight,
    lineHeightRatio: Float = 1.3f,
) = TextStyle(
    fontFamily = RodiFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size * lineHeightRatio).sp,
    letterSpacing = (-0.02).em,
)

val DefaultRodiTypography = RodiTypography(
    headline1 = rodiStyle(18, FontWeight.Bold),
    body1SemiBold = rodiStyle(16, FontWeight.SemiBold),
    body1Medium = rodiStyle(16, FontWeight.Medium),
    body3Medium = rodiStyle(14, FontWeight.Medium),
    body3SemiBold = rodiStyle(14, FontWeight.SemiBold),
    caption1Regular = rodiStyle(13, FontWeight.Normal),
    caption1Medium = rodiStyle(13, FontWeight.Medium),
    caption1SemiBold = rodiStyle(13, FontWeight.SemiBold),
    caption2SemiBold = rodiStyle(12, FontWeight.SemiBold),
    caption3Medium = rodiStyle(10, FontWeight.Medium),
    button1 = rodiStyle(16, FontWeight.Medium, lineHeightRatio = 1.4f),
)

val LocalRodiTypography = staticCompositionLocalOf { DefaultRodiTypography }
