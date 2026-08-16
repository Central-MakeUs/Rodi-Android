package com.dororong.rodi.feature.home.review

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity

@Composable
internal fun Modifier.reviewBottomBarInsets(): Modifier {
    val density = LocalDensity.current
    val bottomInset = with(density) {
        maxOf(
            WindowInsets.ime.getBottom(this),
            WindowInsets.navigationBars.getBottom(this),
        ).toDp()
    }
    return padding(bottom = bottomInset)
}
