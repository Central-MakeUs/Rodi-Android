package com.dororong.rodi.feature.entry.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme

private const val TOTAL_STEPS = 3

@Composable
fun StepProgressIndicator(
    currentStep: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(TOTAL_STEPS) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (index < currentStep) RodiTheme.colors.primary600 else RodiTheme.colors.gray300,
                    ),
            )
        }
    }
}
