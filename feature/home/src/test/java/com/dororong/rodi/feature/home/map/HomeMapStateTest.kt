package com.dororong.rodi.feature.home.map

import androidx.compose.runtime.saveable.SaverScope
import com.dororong.rodi.core.domain.model.course.GeoPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HomeMapStateTest {
    private val viewport = MapViewport(
        northEast = GeoPoint(38.0, 128.0),
        southWest = GeoPoint(36.0, 126.0),
    )
    private val center = GeoPoint(37.0, 127.0)

    @Test
    fun `moveWithSearch records the pending search before moving the camera`() {
        val state = HomeMapState()
        var pendingWhenCameraMoved: PendingMapSearch? = null

        state.moveWithSearch(center, targetZoom = 13, reason = MapSearchMoveReason.REGION) {
            pendingWhenCameraMoved = state.pendingMapSearch
        }

        assertEquals(1L, state.searchGeneration)
        assertEquals(
            PendingMapSearch(generation = 1L, target = center, targetZoom = 13, reason = MapSearchMoveReason.REGION),
            pendingWhenCameraMoved,
        )
    }

    @Test
    fun `moveWithSearch clears the cluster scope unless member ids are given`() {
        val state = HomeMapState()
        state.moveWithSearch(center, null, MapSearchMoveReason.CLUSTER, clusterMemberIds = setOf(1L, 2L)) {}
        assertEquals(setOf(1L, 2L), state.activeClusterMemberIds)

        state.moveWithSearch(center, 13, MapSearchMoveReason.CURRENT_LOCATION) {}

        assertNull(state.activeClusterMemberIds)
        assertEquals(2L, state.searchGeneration)
    }

    @Test
    fun `user gesture cancels the pending search and marks the viewport as user chosen`() {
        val state = HomeMapState()
        state.moveWithSearch(center, 13, MapSearchMoveReason.CLUSTER, clusterMemberIds = setOf(1L)) {}
        state.isAtCurrentLocation = true
        state.isInitialLocationCameraMovePending = true

        state.onUserGesture()

        assertNull(state.pendingMapSearch)
        assertNull(state.activeClusterMemberIds)
        assertFalse(state.isAtCurrentLocation)
        assertFalse(state.isInitialLocationCameraMovePending)
        assertTrue(state.hasUserMovedMap)
        assertTrue(state.hasUserChosenMapViewport)
    }

    @Test
    fun `camera arriving at the pending target dispatches a programmatic search`() {
        val state = HomeMapState()
        state.isInitialLocationCameraMovePending = true
        state.moveWithSearch(center, 13, MapSearchMoveReason.INITIAL_LOCATION) {}

        val action = state.onCameraMoveEnd(viewport, zoomLevel = 13, InitialLocationState.Ready, hasCurrentLocation = true)

        assertEquals(CameraSettleAction.ProgrammaticSearch, action)
        assertNull(state.pendingMapSearch)
        assertFalse(state.isInitialLocationCameraMovePending)
        assertEquals(viewport, state.currentViewport)
        assertEquals(13, state.zoomLevel)
    }

    @Test
    fun `camera stopping short of the pending target keeps waiting`() {
        val state = HomeMapState()
        state.moveWithSearch(GeoPoint(35.0, 129.0), 13, MapSearchMoveReason.REGION) {}

        val action = state.onCameraMoveEnd(viewport, zoomLevel = 13, InitialLocationState.Unavailable, hasCurrentLocation = false)

        assertEquals(CameraSettleAction.None, action)
        assertNotNull(state.pendingMapSearch)
    }

    @Test
    fun `settled viewport without a pending search dispatches only when the initial search is allowed`() {
        val waitingForLocation = HomeMapState()
        val centered = HomeMapState(hasCenteredInitialLocation = true)

        val blocked = waitingForLocation.onCameraMoveEnd(viewport, 13, InitialLocationState.Pending, hasCurrentLocation = false)
        val allowed = centered.onCameraMoveEnd(viewport, 13, InitialLocationState.Ready, hasCurrentLocation = true)

        assertEquals(CameraSettleAction.None, blocked)
        assertEquals(CameraSettleAction.ViewportSettled, allowed)
    }

    @Test
    fun `initial search stays blocked while the initial location camera move is in flight`() {
        val state = HomeMapState(hasCenteredInitialLocation = true)
        state.isInitialLocationCameraMovePending = true

        val action = state.onCameraMoveEnd(viewport, 13, InitialLocationState.Ready, hasCurrentLocation = true)

        assertEquals(CameraSettleAction.None, action)
    }

    @Test
    fun `camera end without a measurable viewport only records the zoom level`() {
        val state = HomeMapState(currentViewport = viewport)
        state.moveWithSearch(center, 13, MapSearchMoveReason.REGION) {}

        val action = state.onCameraMoveEnd(null, zoomLevel = 15, InitialLocationState.Ready, hasCurrentLocation = true)

        assertEquals(CameraSettleAction.None, action)
        assertEquals(15, state.zoomLevel)
        assertNotNull(state.pendingMapSearch)
    }

    @Test
    fun `entering the screen again resets the centering flags`() {
        val state = HomeMapState(hasUserMovedMap = true, hasUserChosenMapViewport = true, hasCenteredInitialLocation = true)

        state.resetEntryFlags()

        assertFalse(state.hasUserMovedMap)
        assertFalse(state.hasUserChosenMapViewport)
        assertFalse(state.hasCenteredInitialLocation)
    }

    @Test
    fun `saver restores the viewport and flags but not in-flight searches`() {
        val state = HomeMapState(zoomLevel = 11, currentViewport = viewport, hasUserChosenMapViewport = true)
        state.moveWithSearch(center, 13, MapSearchMoveReason.CLUSTER, clusterMemberIds = setOf(3L)) {}

        val saved = with(HomeMapState.Saver) { SaverScope { true }.save(state) }
        val restored = HomeMapState.Saver.restore(requireNotNull(saved))

        requireNotNull(restored)
        assertEquals(11, restored.zoomLevel)
        assertEquals(viewport, restored.currentViewport)
        assertTrue(restored.hasUserChosenMapViewport)
        assertNull(restored.pendingMapSearch)
        assertNull(restored.activeClusterMemberIds)
        assertEquals(0L, restored.searchGeneration)
    }
}
