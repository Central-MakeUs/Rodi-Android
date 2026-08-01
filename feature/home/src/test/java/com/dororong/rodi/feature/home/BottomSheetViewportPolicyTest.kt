package com.dororong.rodi.feature.home

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BottomSheetViewportPolicyTest {
    @Test
    fun `partially expanded sheet keeps only the map above its top visible`() {
        assertEquals(380, BottomSheetViewportPolicy.bottomPaddingPx(1_000, 620f))
    }

    @Test
    fun `expanded sheet excludes the covered map area`() {
        assertEquals(1_000, BottomSheetViewportPolicy.bottomPaddingPx(1_000, 0f))
    }

    @Test
    fun `overscrolled sheet never makes the bottom inset taller than the map`() {
        assertEquals(1_000, BottomSheetViewportPolicy.bottomPaddingPx(1_000, -80f))
    }

    @Test
    fun `hidden sheet leaves the map unobscured`() {
        assertEquals(0, BottomSheetViewportPolicy.bottomPaddingPx(1_000, 1_000f))
    }
}
