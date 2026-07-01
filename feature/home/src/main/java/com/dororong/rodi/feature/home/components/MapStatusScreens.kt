package com.dororong.rodi.feature.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun MapLoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white)
            .consumeTouches(),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .absoluteOffset(y = (-33).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RodiLoadingIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "지도를 불러오고 있어요",
                style = RodiTheme.typography.body1SemiBold,
                color = RodiTheme.colors.gray800,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "잠시만 기다려 주세요.",
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.gray800,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun MapNetworkErrorScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RodiTheme.colors.white)
                .consumeTouches(),
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .absoluteOffset(y = (-30).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.illust_network_disconntected),
                contentDescription = null,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "지도를 불러올 수 없어요",
                style = RodiTheme.typography.body1SemiBold,
                color = RodiTheme.colors.gray800,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "현재 위치 정보를 확인하기 위해\n네트워크 연결 상태를 확인해 주세요.",
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.gray800,
                textAlign = TextAlign.Center,
            )
        }

        RodiNetworkSnackbar(
            onRetry = onRetry,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .padding(bottom = 106.dp),
        )
    }
}

private fun Modifier.consumeTouches(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            event.changes.forEach { it.consume() }
        }
    }
}

@Composable
private fun RodiLoadingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "map_loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
        ),
        label = "map_loading_rotation",
    )
    val colors = listOf(
        RodiTheme.colors.primary600,
        Color(0xFFF4F4FF),
        RodiTheme.colors.primary50,
        Color(0xFFDBD9FF),
        RodiTheme.colors.primary200,
        RodiTheme.colors.primary300,
        RodiTheme.colors.primary400,
        RodiTheme.colors.primary500,
    )

    Canvas(modifier = modifier.size(39.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val strokeWidth = 3.dp.toPx()
        val lineLength = 10.dp.toPx()
        colors.forEachIndexed { index, color ->
            rotate(degrees = rotation + index * 45f, pivot = center) {
                drawLine(
                    color = color,
                    start = Offset(center.x, 0f),
                    end = Offset(center.x, lineLength),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun RodiNetworkSnackbar(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp),
        shape = RoundedCornerShape(8.dp),
        color = RodiTheme.colors.gray800,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SnackbarAlertIcon()
            Text(
                text = "네트워크 연결이 원활하지 않아요.\n다시 시도해볼까요?",
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.white,
                modifier = Modifier.weight(1f),
            )
            Surface(
                onClick = onRetry,
                shape = RoundedCornerShape(8.dp),
                color = RodiTheme.colors.primary600,
            ) {
                Text(
                    text = "새로고침",
                    style = RodiTheme.typography.caption2SemiBold,
                    color = RodiTheme.colors.white,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun SnackbarAlertIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val color = Color.White
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = color,
            radius = 9.5.dp.toPx(),
            center = center,
            style = Stroke(width = 2.dp.toPx()),
        )
        drawLine(
            color = color,
            start = Offset(center.x, center.y - 5.dp.toPx()),
            end = Offset(center.x, center.y + 2.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(color = color, radius = 1.3.dp.toPx(), center = Offset(center.x, center.y + 6.dp.toPx()))
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
fun MapLoadingScreenPreview() {
    RodiTheme {
        MapLoadingScreen()
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
fun MapNetworkErrorScreenPreview() {
    RodiTheme {
        MapNetworkErrorScreen(onRetry = {})
    }
}
