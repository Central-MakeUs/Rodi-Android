package com.dororong.rodi.feature.course.registration.map

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InitialCameraZoomTest {
    @Test
    fun `saved zoom is used for the initial camera restore`() {
        assertEquals(
            9,
            initialOrDefaultZoom(
                isFirstApply = true,
                hadInitialCenter = false,
                savedZoom = 9,
                defaultZoom = 13,
            ),
        )
    }

    @Test
    fun `default zoom is used when an explicit center was already provided`() {
        assertEquals(
            13,
            initialOrDefaultZoom(
                isFirstApply = true,
                hadInitialCenter = true,
                savedZoom = 9,
                defaultZoom = 13,
            ),
        )
    }

    @Test
    fun `default zoom is used for a later recenter such as a search jump`() {
        assertEquals(
            13,
            initialOrDefaultZoom(
                isFirstApply = false,
                hadInitialCenter = false,
                savedZoom = 9,
                defaultZoom = 13,
            ),
        )
    }

    @Test
    fun `default zoom is used when there is no saved zoom`() {
        assertEquals(
            13,
            initialOrDefaultZoom(
                isFirstApply = true,
                hadInitialCenter = false,
                savedZoom = null,
                defaultZoom = 13,
            ),
        )
    }
}
