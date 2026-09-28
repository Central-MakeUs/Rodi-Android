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
    fun `지도를 불러온 적이 있으면 준비 상태로 없으면 로딩 상태로 시작한다`() {
        assertEquals(MapScreenState.Ready, status(hasLoadedMapBefore = true).screenState)
        assertEquals(MapScreenState.Loading, status(hasLoadedMapBefore = false).screenState)
    }

    @Test
    fun `오프라인으로 시작하면 스낵바를 보여주지만 아직 지도를 가리지 않는다`() {
        val offline = status(isOnline = false)

        assertTrue(offline.showNetworkSnackbar)
        assertEquals(MapScreenState.Loading, offline.screenState)
    }

    @Test
    fun `오프라인 유예 시간이 지난 뒤에만 네트워크 오류 화면을 보여준다`() = runTest {
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
    fun `유예 시간 안에 다시 연결되면 지도를 계속 보여준다`() = runTest {
        val status = status(isOnline = false, hasLoadedMapBefore = true)

        val grace = launch { status.awaitOfflineGrace() }
        advanceTimeBy(MAP_NETWORK_ERROR_GRACE_MILLIS / 2)
        grace.cancel()
        advanceTimeBy(MAP_NETWORK_ERROR_GRACE_MILLIS)
        runCurrent()

        assertEquals(MapScreenState.Ready, status.screenState)
    }

    @Test
    fun `다시 시도는 지도 뷰를 새로 만들고 짧은 시간 안의 반복은 무시한다`() {
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
    fun `다시 시도하는 동안 준비된 지도를 화면에 둔다`() {
        val status = status(hasLoadedMapBefore = true)

        status.retry()

        assertEquals(MapScreenState.Ready, status.screenState)
    }

    @Test
    fun `오프라인에서 다시 시도하면 지도를 새로 만들지 않고 네트워크 오류를 보여준다`() {
        val status = status(isOnline = false)

        assertFalse(status.retry())

        assertEquals(MapScreenState.NetworkError, status.screenState)
        assertEquals(0, status.retryKey)
    }

    @Test
    fun `온라인에서 지도 오류가 나면 네트워크 오류가 아니라 SDK 오류로 알린다`() {
        val status = status(isOnline = true)

        status.onMapError()

        assertEquals(MapScreenState.Error, status.screenState)
        assertFalse(status.showNetworkSnackbar)
    }

    @Test
    fun `오프라인에서 지도 오류가 나면 스낵바만 보여주고 화면은 유예 시간에 맡긴다`() {
        val status = status(isOnline = true, hasLoadedMapBefore = true)
        status.isOnline = false

        status.onMapError()

        assertTrue(status.showNetworkSnackbar)
        assertEquals(MapScreenState.Ready, status.screenState)
    }

    @Test
    fun `다시 연결되면 오류를 보여주던 경우에만 다시 시도한다`() {
        val healthy = status(hasLoadedMapBefore = true)
        val recovering = status(isOnline = false, hasLoadedMapBefore = true)
        recovering.isOnline = true

        assertFalse(healthy.shouldRetryOnReconnect)
        assertTrue(recovering.shouldRetryOnReconnect)
    }

    @Test
    fun `렌더링은 온라인일 때만 로드 완료로 본다`() {
        val online = status()
        val offline = status(isOnline = false)

        assertTrue(online.onMapRendered())
        assertEquals(MapScreenState.Ready, online.screenState)
        assertFalse(online.showNetworkSnackbar)
        assertFalse(offline.onMapRendered())
        assertEquals(MapScreenState.Loading, offline.screenState)
    }
}
