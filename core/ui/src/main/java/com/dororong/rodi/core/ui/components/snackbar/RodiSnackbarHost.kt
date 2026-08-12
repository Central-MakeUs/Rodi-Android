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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme
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

// Host는 하단 고정 위치와 인셋이 핵심이라 스낵바 단독 프리뷰(RodiSnackbar.kt)로는 확인되지 않는다.
// 자동 dismiss가 걸리면 인터랙티브 프리뷰에서 사라지므로 Indefinite로 고정한다.
@Composable
private fun previewHostState(data: RodiSnackbarData) =
    remember { RodiSnackbarHostState().apply { show(data) } }

@Preview(name = "RodiSnackbarHost - 텍스트만", showBackground = true, widthDp = 360, heightDp = 400)
@Composable
private fun RodiSnackbarHostPreview() {
    RodiTheme {
        RodiSnackbarHost(
            state = previewHostState(
                RodiSnackbarData(
                    message = "저장되었습니다",
                    duration = RodiSnackbarDuration.Indefinite,
                ),
            ),
        )
    }
}

@Preview(name = "RodiSnackbarHost - 액션 포함", showBackground = true, widthDp = 360, heightDp = 400)
@Composable
private fun RodiSnackbarHostWithActionPreview() {
    RodiTheme {
        RodiSnackbarHost(
            state = previewHostState(
                RodiSnackbarData(
                    message = "재가입 가능 날짜를 불러오지 못했어요.",
                    duration = RodiSnackbarDuration.Indefinite,
                    actionLabel = "새로고침",
                    onAction = {},
                ),
            ),
        )
    }
}

@Preview(name = "RodiSnackbarHost - 두 줄", showBackground = true, widthDp = 360, heightDp = 400)
@Composable
private fun RodiSnackbarHostTwoLinePreview() {
    RodiTheme {
        RodiSnackbarHost(
            state = previewHostState(
                RodiSnackbarData(
                    message = "요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요.",
                    duration = RodiSnackbarDuration.Indefinite,
                ),
            ),
        )
    }
}
