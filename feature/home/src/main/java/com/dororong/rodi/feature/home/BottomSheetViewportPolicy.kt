package com.dororong.rodi.feature.home

import kotlin.math.roundToInt

internal object BottomSheetViewportPolicy {
    fun bottomPaddingPx(mapHeightPx: Int, sheetTopPx: Float): Int =
        (mapHeightPx - sheetTopPx).coerceAtLeast(0f).roundToInt()
}
