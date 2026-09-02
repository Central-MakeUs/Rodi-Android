package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ViewportSearchThresholdTest {
    @Test
    fun `initial search waits until current location camera move finishes`() {
        assertFalse(
            InitialViewportSearchPolicy.canDispatch(
                locationState = InitialLocationState.Pending,
                hasCurrentLocation = false,
                hasCenteredInitialLocation = false,
                isInitialLocationCameraMovePending = false,
            ),
        )
        assertFalse(
            InitialViewportSearchPolicy.canDispatch(
                locationState = InitialLocationState.Ready,
                hasCurrentLocation = true,
                hasCenteredInitialLocation = true,
                isInitialLocationCameraMovePending = true,
            ),
        )
        assertTrue(
            InitialViewportSearchPolicy.canDispatch(
                locationState = InitialLocationState.Ready,
                hasCurrentLocation = true,
                hasCenteredInitialLocation = true,
                isInitialLocationCameraMovePending = false,
            ),
        )
    }

    @Test
    fun `initial search uses the viewport only when location is unavailable`() {
        assertTrue(
            InitialViewportSearchPolicy.canDispatch(
                locationState = InitialLocationState.Unavailable,
                hasCurrentLocation = false,
                hasCenteredInitialLocation = false,
                isInitialLocationCameraMovePending = false,
            ),
        )
        assertFalse(
            InitialViewportSearchPolicy.canDispatch(
                locationState = InitialLocationState.Unavailable,
                hasCurrentLocation = false,
                hasCenteredInitialLocation = false,
                isInitialLocationCameraMovePending = true,
            ),
        )
        assertFalse(
            InitialViewportSearchPolicy.canDispatch(
                locationState = InitialLocationState.Ready,
                hasCurrentLocation = true,
                hasCenteredInitialLocation = false,
                isInitialLocationCameraMovePending = false,
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

    @Test
    fun `cluster member bounds contain every member point`() {
        val points = listOf(
            GeoPoint(37.40, 126.80),
            GeoPoint(37.70, 127.10),
            GeoPoint(37.55, 126.95),
        )

        val bounds = points.boundsOrNull()

        assertEquals(GeoPoint(37.70, 127.10), bounds?.northEast)
        assertEquals(GeoPoint(37.40, 126.80), bounds?.southWest)
        assertTrue(points.all { bounds?.contains(it) == true })
    }

    @Test
    fun `marker viewport follows the current camera before the last search viewport`() {
        val currentViewport = MapViewport(
            northEast = GeoPoint(38.0, 128.0),
            southWest = GeoPoint(36.0, 126.0),
        )
        val searchedQuery = PlaceViewportQuery(
            southWest = GeoPoint(37.4, 126.8),
            northEast = GeoPoint(37.6, 127.1),
            origin = GeoPoint(37.5, 126.95),
        )

        assertEquals(
            currentViewport,
            markerViewportOrNull(currentViewport, searchedQuery),
        )
        assertEquals(
            MapViewport(searchedQuery.northEast, searchedQuery.southWest),
            markerViewportOrNull(null, searchedQuery),
        )
    }

    @Test
    fun `restored viewport is preferred while the location stream is restarting`() {
        val savedViewport = MapViewport(
            northEast = GeoPoint(37.60, 127.02),
            southWest = GeoPoint(37.50, 126.92),
        )

        assertEquals(
            GeoPoint(37.55, 126.97),
            initialMapCenter(
                savedViewport = savedViewport,
                currentLocation = GeoPoint(36.10, 128.30),
                lastSavedCenter = GeoPoint(35.10, 129.10),
                fallback = GeoPoint(37.5665, 126.9780),
            ),
        )
        assertEquals(
            GeoPoint(36.10, 128.30),
            initialMapCenter(
                savedViewport = null,
                currentLocation = GeoPoint(36.10, 128.30),
                lastSavedCenter = GeoPoint(35.10, 129.10),
                fallback = GeoPoint(37.5665, 126.9780),
            ),
        )
    }

    @Test
    fun `last saved center is used when viewport and current location are absent`() {
        val lastSavedCenter = GeoPoint(36.1195, 128.3446)

        assertEquals(
            lastSavedCenter,
            initialMapCenter(
                savedViewport = null,
                currentLocation = null,
                lastSavedCenter = lastSavedCenter,
                fallback = GeoPoint(37.5665, 126.9780),
            ),
        )
    }

    @Test
    fun `fallback is used when all camera centers are absent`() {
        val fallback = GeoPoint(37.5665, 126.9780)

        assertEquals(
            fallback,
            initialMapCenter(
                savedViewport = null,
                currentLocation = null,
                lastSavedCenter = null,
                fallback = fallback,
            ),
        )
    }

    @Test
    fun `current location is preferred over the last saved center`() {
        val currentLocation = GeoPoint(36.10, 128.30)

        assertEquals(
            currentLocation,
            initialMapCenter(
                savedViewport = null,
                currentLocation = currentLocation,
                lastSavedCenter = GeoPoint(35.10, 129.10),
                fallback = GeoPoint(37.5665, 126.9780),
            ),
        )
    }

    @Test
    fun `saved viewport is preferred over current location and last saved center`() {
        val savedViewport = MapViewport(
            northEast = GeoPoint(37.60, 127.02),
            southWest = GeoPoint(37.50, 126.92),
        )

        assertEquals(
            GeoPoint(37.55, 126.97),
            initialMapCenter(
                savedViewport = savedViewport,
                currentLocation = GeoPoint(36.10, 128.30),
                lastSavedCenter = GeoPoint(35.10, 129.10),
                fallback = GeoPoint(37.5665, 126.9780),
            ),
        )
    }

    private fun viewport(centerLongitude: Double) = MapViewport(
        northEast = GeoPoint(37.60, centerLongitude + 0.02),
        southWest = GeoPoint(37.50, centerLongitude - 0.02),
    )
}
