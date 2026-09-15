package com.dororong.rodi.core.ui.components.input

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.SolidColor
import com.dororong.rodi.core.ui.theme.RodiTheme

/**
 * 서비스 전역 텍스트 입력 커서.
 *
 * 화면마다 primary600과 black을 섞어 쓰고 있어서 커서 색이 제각각이었다.
 * 색을 바꿀 일이 생기면 이 함수 하나만 고치면 된다.
 */
@Composable
fun rodiCursorBrush(): SolidColor = SolidColor(RodiTheme.colors.primary600)
