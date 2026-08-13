package com.dororong.rodi.feature.home

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt

internal fun Modifier.layoutHeightPx(height: Density.() -> Float): Modifier =
    layout { measurable, constraints ->
        val resolved = height().roundToInt().coerceIn(0, constraints.maxHeight)
        val placeable = measurable.measure(
            constraints.copy(minHeight = resolved, maxHeight = resolved),
        )
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
