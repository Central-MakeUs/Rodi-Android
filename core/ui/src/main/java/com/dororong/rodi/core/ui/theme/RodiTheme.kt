package com.dororong.rodi.core.ui.theme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Rodi 앱 테마. 라이트 전용(다크 미예정).
 * 컬러/타이포 토큰을 CompositionLocal 로 제공하고, M3 컴포넌트 기본값도 토큰에 맞춰 둔다.
 *
 * 사용: `RodiTheme.colors.primary600`, `RodiTheme.typography.heading2`
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RodiTheme(content: @Composable () -> Unit) {
    val colors = LightRodiColors
    val semanticColors = LightRodiSemanticColors
    val typography = DefaultRodiTypography
    CompositionLocalProvider(
        LocalRodiColors provides colors,
        LocalRodiSemanticColors provides semanticColors,
        LocalRodiTypography provides typography,
        LocalOverscrollConfiguration provides null,
    ) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = colors.primary600,
                onPrimary = colors.white,
                secondary = colors.primary300,
                background = colors.white,
                surface = colors.white,
                onBackground = colors.black,
                onSurface = colors.black,
            ),
            content = content,
        )
    }
}

/** 토큰 접근자. 함수 [RodiTheme] 와 동명으로 공존(Jetsnack 패턴). */
object RodiTheme {
    val colors: RodiColors
        @Composable @ReadOnlyComposable get() = LocalRodiColors.current

    val semantic: RodiSemanticColors
        @Composable @ReadOnlyComposable get() = LocalRodiSemanticColors.current

    val typography: RodiTypography
        @Composable @ReadOnlyComposable get() = LocalRodiTypography.current
}
