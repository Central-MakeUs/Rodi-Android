package com.dororong.rodi.feature.home.component.sheet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified

@Composable
fun StableMeasuredDetailSheet(
    itemKey: Int,
    maxHeight: Dp,
    content: @Composable (Modifier) -> Unit,
) {
    val density = LocalDensity.current
    var measuredHeightPx by rememberSaveable(itemKey, maxHeight.value) { mutableStateOf<Float?>(null) }
    val measuredHeightDp = measuredHeightPx?.let { with(density) { it.toDp() } }
    val maxHeightModifier = if (maxHeight.isSpecified && maxHeight > 0.dp) {
        Modifier.heightIn(max = maxHeight)
    } else {
        Modifier
    }

    if (measuredHeightDp == null) {
        content(
            Modifier
                .fillMaxWidth()
                .then(maxHeightModifier)
                .onGloballyPositioned { coordinates ->
                    val heightPx = coordinates.size.height.toFloat()
                    if (heightPx <= 0f || measuredHeightPx != null) return@onGloballyPositioned
                    val maxHeightPx = with(density) {
                        if (maxHeight.isSpecified && maxHeight > 0.dp) maxHeight.toPx() else Float.POSITIVE_INFINITY
                    }
                    measuredHeightPx = heightPx.coerceAtMost(maxHeightPx)
                },
        )
    } else {
        content(
            Modifier
                .fillMaxWidth()
                .height(measuredHeightDp)
                .then(maxHeightModifier),
        )
    }
}
