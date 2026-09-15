package com.dororong.rodi.feature.home.map

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapLoadStatusTest {
    private var now = 10_000L
    private fun status(isOnline: Boolean = true, hasLoadedMapBefore: Boolean = false) =
        MapLoadStatus(isOnline, hasLoadedMapBefore, clock = { now })

    @Test
    fun `starts ready when the map loaded before and loading otherwise`() {
        assertEquals(MapScreenState.Ready, status(hasLoadedMapBefore = true).screenState)
        assertEquals(MapScreenState.Loading, status(hasLoadedMapBefore = false).screenState)
    }

    @Test
    fun `offline start shows the snackbar but does not cover the map yet`() {
        val offline = status(isOnline = false)

        assertTrue(offline.showNetworkSnackbar)
        assertEquals(MapScreenState.Loading, offline.screenState)
    }

    @Test
    fun `network error screen appears only after the offline grace period`() = runTest {
        val status = status(isOnline = false, hasLoadedMapBefore = true)

        launch { status.awaitOfflineGrace() }
        runCurrent()
        advanceTimeBy(MAP_NETWORK_ERROR_GRACE_MILLIS - 1)
        runCurrent()
        assertTrue(status.showNetworkSnackbar)
        assertEquals(MapScreenState.Ready, status.screenState)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(MapScreenState.NetworkError, status.screenState)
    }

    @Test
    fun `reconnecting within the grace period keeps the map visible`() = runTest {
        val status = status(isOnline = false, hasLoadedMapBefore = true)

        val grace = launch { status.awaitOfflineGrace() }
        advanceTimeBy(MAP_NETWORK_ERROR_GRACE_MILLIS / 2)
        grace.cancel()
        advanceTimeBy(MAP_NETWORK_ERROR_GRACE_MILLIS)
        runCurrent()

        assertEquals(MapScreenState.Ready, status.screenState)
    }

    @Test
    fun `retry recreates the map view and ignores repeats within the debounce window`() {
        val status = status()

        assertTrue(status.retry())
        now += MAP_RETRY_DEBOUNCE_MILLIS - 1
        assertFalse(status.retry())
        now += 1
        assertTrue(status.retry())

        assertEquals(2, status.retryKey)
        assertEquals(MapScreenState.Loading, status.screenState)
    }

    @Test
    fun `retry keeps a ready map on screen while reloading`() {
        val status = status(hasLoadedMapBefore = true)

        status.retry()

        assertEquals(MapScreenState.Ready, status.screenState)
    }

    @Test
    fun `retry while offline shows the network error without recreating the map`() {
        val status = status(isOnline = false)

        assertFalse(status.retry())

        assertEquals(MapScreenState.NetworkError, status.screenState)
        assertEquals(0, status.retryKey)
    }

    @Test
    fun `map error while online is reported as an sdk error instead of a network error`() {
        val status = status(isOnline = true)

        status.onMapError()

        assertEquals(MapScreenState.Error, status.screenState)
        assertFalse(status.showNetworkSnackbar)
    }

    @Test
    fun `map error while offline only shows the snackbar and leaves the screen to the grace period`() {
        val status = status(isOnline = true, hasLoadedMapBefore = true)
        status.isOnline = false

        status.onMapError()

        assertTrue(status.showNetworkSnackbar)
        assertEquals(MapScreenState.Ready, status.screenState)
    }

    @Test
    fun `reconnect retries only when an error was being shown`() {
        val healthy = status(hasLoadedMapBefore = true)
        val recovering = status(isOnline = false, hasLoadedMapBefore = true)
        recovering.isOnline = true

        assertFalse(healthy.shouldRetryOnReconnect)
        assertTrue(recovering.shouldRetryOnReconnect)
    }

    @Test
    fun `rendering counts as loaded only while online`() {
        val online = status()
        val offline = status(isOnline = false)

        assertTrue(online.onMapRendered())
        assertEquals(MapScreenState.Ready, online.screenState)
        assertFalse(online.showNetworkSnackbar)
        assertFalse(offline.onMapRendered())
        assertEquals(MapScreenState.Loading, offline.screenState)
    }
}
