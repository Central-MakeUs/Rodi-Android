package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.GeoPoint
import com.dororong.rodi.core.domain.MapViewportQuery
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ViewportSearchThresholdTest {
    @Test
    fun `does not request a search before moving half a viewport`() {
        assertFalse(ViewportSearchThreshold.isExceeded(searchedViewport(), currentViewport(centerLng = 126.99)))
    }

    @Test
    fun `requests a search after moving half a viewport`() {
        assertTrue(ViewportSearchThreshold.isExceeded(searchedViewport(), currentViewport(centerLng = 127.00)))
    }

    private fun searchedViewport() = currentViewport(centerLng = 126.98)

    private fun currentViewport(centerLng: Double) = MapViewportQuery(
        northEast = GeoPoint(37.60, centerLng + 0.02),
        southWest = GeoPoint(37.50, centerLng - 0.02),
        zoomLevel = 13,
    )
}
