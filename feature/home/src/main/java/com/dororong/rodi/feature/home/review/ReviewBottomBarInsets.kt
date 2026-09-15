package com.dororong.rodi.feature.home.review

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 하단 CTA를 키보드·내비게이션 바 위로 올린다.
 *
 * 예전에는 두 인셋의 최댓값을 직접 읽어 padding으로 넣었는데, 그 값은 상위에서 이미
 * 소비한 인셋을 빼주지 않는다. 그래서 부모가 내비게이션 바 패딩을 먼저 넣은 기기에서는
 * 같은 높이가 두 번 들어가 버튼이 떠 보였다(3버튼 내비처럼 인셋이 큰 기기일수록 심함).
 * windowInsetsPadding은 남은 인셋만 적용해서 이 중복이 생기지 않는다.
 */
@Composable
internal fun Modifier.reviewBottomBarInsets(): Modifier =
    windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
