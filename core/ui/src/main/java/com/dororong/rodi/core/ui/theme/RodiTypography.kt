package com.dororong.rodi.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.dororong.rodi.core.ui.R

/** Pretendard 폰트 패밀리. core/ui/src/main/res/font/ 에 5 weight 배치. */
val RodiFontFamily = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_extrabold, FontWeight.ExtraBold),
)

/**
 * Rodi 타이포그래피 토큰 (Figma 디자인 시스템 기준).
 * lineHeight = 크기 × 1.3 (button1만 1.4), letterSpacing = -2% (-0.02em).
 * `RodiTheme.typography.xxx` 로 접근한다.
 */
@Immutable
data class RodiTypography(
    val heading1: TextStyle,        // Bold 22
    val heading2: TextStyle,        // Bold 20
    val headline1: TextStyle,       // Bold 18
    val headline2: TextStyle,       // Bold 17
    val price1: TextStyle,          // Bold 16
    val price2: TextStyle,          // ExtraBold 14
    val body1SemiBold: TextStyle,   // SemiBold 16
    val body1Medium: TextStyle,
    val body1Regular: TextStyle,
    val body2SemiBold: TextStyle,   // SemiBold 15
    val body2Medium: TextStyle,
    val body2Regular: TextStyle,
    val body3Medium: TextStyle,     // Medium 14
    val body3SemiBold: TextStyle,   // SemiBold 14
    val body3Regular: TextStyle,
    val caption1Regular: TextStyle, // Regular 13
    val caption1Medium: TextStyle,  // Medium 13
    val caption1SemiBold: TextStyle,// SemiBold 13
    val caption2SemiBold: TextStyle,// SemiBold 12
    val caption2Medium: TextStyle,  // Medium 12
    val caption2Regular: TextStyle,
    val caption3SemiBold: TextStyle,// SemiBold 10
    val caption3Medium: TextStyle,  // Medium 10
    val caption3Regular: TextStyle,
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
    heading1 = rodiStyle(22, FontWeight.Bold),
    heading2 = rodiStyle(20, FontWeight.Bold),
    headline1 = rodiStyle(18, FontWeight.Bold),
    headline2 = rodiStyle(17, FontWeight.Bold),
    price1 = rodiStyle(16, FontWeight.Bold),
    price2 = rodiStyle(14, FontWeight.ExtraBold),
    body1SemiBold = rodiStyle(16, FontWeight.SemiBold),
    body1Medium = rodiStyle(16, FontWeight.Medium),
    body1Regular = rodiStyle(16, FontWeight.Normal),
    body2SemiBold = rodiStyle(15, FontWeight.SemiBold),
    body2Medium = rodiStyle(15, FontWeight.Medium),
    body2Regular = rodiStyle(15, FontWeight.Normal),
    body3Medium = rodiStyle(14, FontWeight.Medium),
    body3SemiBold = rodiStyle(14, FontWeight.SemiBold),
    body3Regular = rodiStyle(14, FontWeight.Normal),
    caption1Regular = rodiStyle(13, FontWeight.Normal),
    caption1Medium = rodiStyle(13, FontWeight.Medium),
    caption1SemiBold = rodiStyle(13, FontWeight.SemiBold),
    caption2SemiBold = rodiStyle(12, FontWeight.SemiBold),
    caption2Medium = rodiStyle(12, FontWeight.Medium),
    caption2Regular = rodiStyle(12, FontWeight.Normal),
    caption3SemiBold = rodiStyle(10, FontWeight.SemiBold),
    caption3Medium = rodiStyle(10, FontWeight.Medium),
    caption3Regular = rodiStyle(10, FontWeight.Normal),
    button1 = rodiStyle(16, FontWeight.Medium, lineHeightRatio = 1.4f),
)

val LocalRodiTypography = staticCompositionLocalOf { DefaultRodiTypography }
