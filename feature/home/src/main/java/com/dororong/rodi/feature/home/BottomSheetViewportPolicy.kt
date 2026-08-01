package com.dororong.rodi.feature.home

import kotlin.math.roundToInt

internal object BottomSheetViewportPolicy {
    fun bottomPaddingPx(mapHeightPx: Int, sheetTopPx: Float): Int =
        (mapHeightPx - sheetTopPx)
            .coerceIn(0f, mapHeightPx.coerceAtLeast(0).toFloat())
            .roundToInt()
}
