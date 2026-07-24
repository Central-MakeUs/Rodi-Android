package com.dororong.rodi.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme

private val SHEET_HANDLE_DRAG_THRESHOLD = 12.dp

@Composable
fun DismissibleSheetHandle(
    onDragDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnDragDown by rememberUpdatedState(onDragDown)
    val dragThresholdPx = with(LocalDensity.current) { SHEET_HANDLE_DRAG_THRESHOLD.toPx() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(dragThresholdPx) {
                var totalDrag = 0f
                var actionTriggered = false
                detectVerticalDragGestures(
                    onDragStart = {
                        totalDrag = 0f
                        actionTriggered = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        if (!actionTriggered) {
                            totalDrag = (totalDrag + dragAmount).coerceAtLeast(0f)
                            if (totalDrag >= dragThresholdPx) {
                                actionTriggered = true
                                currentOnDragDown()
                            }
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 60.dp, height = 4.dp)
                .background(RodiTheme.colors.handleBar, RoundedCornerShape(100.dp)),
        )
    }
}
