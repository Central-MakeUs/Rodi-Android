package com.dororong.rodi.core.ui.components.snackbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RodiSnackbarHost(
    state: RodiSnackbarHostState,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(state) { state.observe() }

    Box(modifier = modifier.padding(16.dp)) {
        AnimatedVisibility(
            visible = state.current != null,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it },
            exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            state.current?.let { RodiSnackbar(data = it) }
        }
    }
}
