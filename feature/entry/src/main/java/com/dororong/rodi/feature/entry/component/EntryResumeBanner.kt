package com.dororong.rodi.feature.entry.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiRadius
import com.dororong.rodi.core.ui.theme.RodiTheme
import kotlinx.coroutines.delay

private const val AUTO_DISMISS_MILLIS = 3_500L

/** 온보딩 재진입 시 로컬에 저장된 이전 입력을 이어서 복원했음을 알리는 배너. 일정 시간 후 자동으로 사라진다. */
@Composable
fun EntryResumeBanner(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(visible) {
        if (visible) {
            delay(AUTO_DISMISS_MILLIS)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it },
        exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { it },
        modifier = modifier,
    ) {
        Text(
            text = "이전 입력 내용부터 이어서 가입을 진행할게요 :)",
            style = RodiTheme.typography.body3Medium,
            color = RodiTheme.colors.black,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .background(RodiTheme.colors.primary50, RoundedCornerShape(RodiRadius.md))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        )
    }
}

@Preview(name = "EntryResumeBanner - Visible", showBackground = true, widthDp = 360)
@Composable
private fun EntryResumeBannerPreview() {
    RodiTheme {
        EntryResumeBanner(visible = true, onDismiss = {})
    }
}
