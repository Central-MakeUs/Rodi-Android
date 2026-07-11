package com.dororong.rodi.core.ui.components.snackbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun RodiSnackbarHost(
    state: RodiSnackbarHostState,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 114.dp,
) {
    val current = state.current

    // current=null을 한 프레임 노출한 뒤 다음 항목으로 넘어가야 exit/enter 애니메이션이 유지된다.
    LaunchedEffect(current) {
        if (current == null) {
            state.advanceIfIdle()
            return@LaunchedEffect
        }
        if (current.duration != RodiSnackbarDuration.Indefinite) {
            delay(current.duration.millis.milliseconds)
            state.dismiss()
        }
    }

    // exit 애니메이션 동안 그릴 마지막 데이터를 유지한다.
    var lastShown by remember { mutableStateOf<RodiSnackbarData?>(null) }
    if (current != null) lastShown = current

    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = bottomPadding),
    ) {
        AnimatedVisibility(
            visible = current != null,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it },
            exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            lastShown?.let { RodiSnackbar(data = it) }
        }
    }
}
