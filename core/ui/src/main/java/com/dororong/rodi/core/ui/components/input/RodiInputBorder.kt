package com.dororong.rodi.core.ui.components.input

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.dororong.rodi.core.ui.theme.RodiTheme

/**
 * 서비스 전역 텍스트 입력창 테두리 색.
 *
 * 화면마다 포커스 강조가 검정·보라로 갈리거나 아예 없었다. 디자인 기준은 포커스 시 검정이다.
 */
@Composable
fun rodiInputBorderColor(focused: Boolean): Color =
    if (focused) RodiTheme.colors.gray900 else RodiTheme.colors.gray300
