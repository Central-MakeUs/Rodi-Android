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

@Composable
fun RodiSnackbarHost(
    state: RodiSnackbarHostState,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 114.dp,
) {
    val current = state.current

    // 현재 항목이 바뀔 때마다(=다른 스낵바로 전환되거나 dismiss로 null이 될 때마다) 새로 시작한다.
    // dismiss()가 current를 즉시 null로 바꾸면 이 이펙트도 즉시 취소되므로, 지난 구현처럼
    // "이미 시작된 delay가 끝날 때까지 다음 항목이 안 뜨는" 문제가 생기지 않는다.
    //
    // current가 null이 되면 여기서 advanceIfIdle()을 호출해 다음 큐 아이템으로 넘긴다 —
    // RodiSnackbarHostState.dismiss() 안에서 곧바로 다음 아이템을 대입하면 Compose snapshot
    // state가 리컴포지션 시점에 "마지막 값"만 관찰해 null 상태를 건너뛰고 두 스낵바가
    // 애니메이션 없이 바로 전환돼버리는 문제가 있었다(CodeRabbit 지적). LaunchedEffect는
    // 별도 코루틴 틱에서 실행되므로 null 상태가 먼저 리컴포지션에 반영된 뒤 다음 아이템으로
    // 넘어간다.
    LaunchedEffect(current) {
        if (current == null) {
            state.advanceIfIdle()
            return@LaunchedEffect
        }
        if (current.duration != RodiSnackbarDuration.Indefinite) {
            delay(current.duration.millis)
            state.dismiss()
        }
    }

    // AnimatedVisibility의 content는 visible=false가 되는 순간에도 exit 애니메이션 동안
    // 계속 그려져야 한다. state.current를 직접 참조하면 dismiss 즉시 null이 되어 애니메이션
    // 도중 빈 상자만 보이므로, 마지막으로 표시했던 데이터를 별도로 기억해뒀다가 그린다.
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
