package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.model.course.GeoPoint
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ViewportSearchThresholdTest {
    @Test
    fun `initial search waits until current location camera move finishes`() {
        assertFalse(
            InitialViewportSearchPolicy.canDispatch(
                isLocationResolved = false,
                hasCurrentLocation = false,
                hasCenteredInitialLocation = false,
                hasUserMovedMap = false,
                isInitialLocationCameraMovePending = false,
            ),
        )
        assertFalse(
            InitialViewportSearchPolicy.canDispatch(
                isLocationResolved = true,
                hasCurrentLocation = true,
                hasCenteredInitialLocation = true,
                hasUserMovedMap = false,
                isInitialLocationCameraMovePending = true,
            ),
        )
        assertTrue(
            InitialViewportSearchPolicy.canDispatch(
                isLocationResolved = true,
                hasCurrentLocation = true,
                hasCenteredInitialLocation = true,
                hasUserMovedMap = false,
                isInitialLocationCameraMovePending = false,
            ),
        )
    }

    @Test
    fun `initial search can use fallback or user selected viewport`() {
        assertTrue(
            InitialViewportSearchPolicy.canDispatch(
                isLocationResolved = true,
                hasCurrentLocation = false,
                hasCenteredInitialLocation = false,
                hasUserMovedMap = false,
                isInitialLocationCameraMovePending = false,
            ),
        )
        assertTrue(
            InitialViewportSearchPolicy.canDispatch(
                isLocationResolved = true,
                hasCurrentLocation = true,
                hasCenteredInitialLocation = false,
                hasUserMovedMap = true,
                isInitialLocationCameraMovePending = false,
            ),
        )
    }

    @Test
    fun `does not show research before moving thirty percent of a viewport`() {
        assertFalse(
            ViewportSearchThreshold.isExceeded(
                searchedViewport = viewport(centerLongitude = 126.98),
                currentViewport = viewport(centerLongitude = 126.991),
            ),
        )
    }

    @Test
    fun `shows research after moving thirty percent of a viewport`() {
        assertTrue(
            ViewportSearchThreshold.isExceeded(
                searchedViewport = viewport(centerLongitude = 126.98),
                currentViewport = viewport(centerLongitude = 126.993),
            ),
        )
    }

    @Test
    fun `viewport contains boundary points and excludes outside points`() {
        val viewport = viewport(centerLongitude = 126.98)

        assertTrue(viewport.contains(viewport.northEast))
        assertTrue(viewport.contains(viewport.southWest))
        assertFalse(viewport.contains(GeoPoint(37.61, 126.98)))
        assertFalse(viewport.contains(GeoPoint(37.55, 127.01)))
    }

    private fun viewport(centerLongitude: Double) = MapViewport(
        northEast = GeoPoint(37.60, centerLongitude + 0.02),
        southWest = GeoPoint(37.50, centerLongitude - 0.02),
    )
}
