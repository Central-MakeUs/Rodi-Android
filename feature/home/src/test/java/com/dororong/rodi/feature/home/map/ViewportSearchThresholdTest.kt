package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.model.course.GeoPoint
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ViewportSearchThresholdTest {
    @Test
    fun `does not show research before moving half a viewport`() {
        assertFalse(
            ViewportSearchThreshold.isExceeded(
                searchedViewport = viewport(centerLongitude = 126.98),
                currentViewport = viewport(centerLongitude = 126.99),
            ),
        )
    }

    @Test
    fun `shows research after moving half a viewport`() {
        assertTrue(
            ViewportSearchThreshold.isExceeded(
                searchedViewport = viewport(centerLongitude = 126.98),
                currentViewport = viewport(centerLongitude = 127.00),
            ),
        )
    }

    private fun viewport(centerLongitude: Double) = MapViewport(
        northEast = GeoPoint(37.60, centerLongitude + 0.02),
        southWest = GeoPoint(37.50, centerLongitude - 0.02),
    )
}
