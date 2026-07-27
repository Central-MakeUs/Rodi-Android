package com.dororong.rodi.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun SearchScreen(
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white),
    )
}
