package com.dororong.rodi.feature.course.registration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CourseRegistrationLoadingIndicator(
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    var rotation by remember { mutableFloatStateOf(0f) }
    val primaryColor = RodiTheme.colors.primary600
    if (animate) {
        LaunchedEffect(Unit) {
            while (true) {
                rotation = (rotation + 45f) % 360f
                delay(100L)
            }
        }
    }
    Canvas(
        modifier = modifier
            .size(39.dp)
            .semantics { contentDescription = "로딩 중" },
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.minDimension / 2f
        val innerRadius = outerRadius * 0.55f
        repeat(8) { index ->
            val angle = Math.toRadians((index * 45f + rotation - 90f).toDouble())
            val alpha = 0.18f + (index / 7f) * 0.82f
            drawLine(
                color = primaryColor.copy(alpha = alpha),
                start = Offset(
                    center.x + cos(angle).toFloat() * innerRadius,
                    center.y + sin(angle).toFloat() * innerRadius,
                ),
                end = Offset(
                    center.x + cos(angle).toFloat() * outerRadius,
                    center.y + sin(angle).toFloat() * outerRadius,
                ),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 120)
@Composable
private fun CourseRegistrationLoadingIndicatorPreview() {
    RodiTheme { CourseRegistrationLoadingIndicator(animate = false) }
}
