package com.dororong.rodi.feature.entry.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TOTAL_STEPS = 3

internal class StepProgressAnimationState {
    val value = Animatable(0f)
}

internal val LocalStepProgressAnimationState =
    staticCompositionLocalOf<StepProgressAnimationState?> { null }

@Composable
internal fun rememberStepProgressAnimationState(): StepProgressAnimationState =
    remember { StepProgressAnimationState() }

@Composable
fun StepProgressIndicator(
    currentStep: Int,
    modifier: Modifier = Modifier,
) {
    val progressState = LocalStepProgressAnimationState.current ?: rememberStepProgressAnimationState()
    val progress = progressState.value
    val targetStep = currentStep.coerceIn(0, TOTAL_STEPS).toFloat()
    LaunchedEffect(targetStep) {
        progress.animateTo(
            targetValue = targetStep,
            animationSpec = tween(
                durationMillis = (abs(targetStep - progress.value) * PROGRESS_ANIMATION_DURATION_PER_STEP_MILLIS)
                    .roundToInt()
                    .coerceAtLeast(1),
            ),
        )
    }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(TOTAL_STEPS) { index ->
            val fillFraction = (progress.value - index).coerceIn(0f, 1f)
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(RodiTheme.colors.gray300),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fillFraction)
                        .background(RodiTheme.colors.primary600),
                )
            }
        }
    }
}

private const val PROGRESS_ANIMATION_DURATION_PER_STEP_MILLIS = 220
