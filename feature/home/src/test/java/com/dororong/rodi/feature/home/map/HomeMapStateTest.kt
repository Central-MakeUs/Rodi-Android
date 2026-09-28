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
    fun `검색과 함께 이동하면 카메라를 옮기기 전에 대기 검색을 기록한다`() {
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
    fun `검색과 함께 이동하면 회원 id를 주지 않는 한 클러스터 범위를 지운다`() {
        val state = HomeMapState()
        state.moveWithSearch(center, null, MapSearchMoveReason.CLUSTER, clusterMemberIds = setOf(1L, 2L)) {}
        assertEquals(setOf(1L, 2L), state.activeClusterMemberIds)

        state.moveWithSearch(center, 13, MapSearchMoveReason.CURRENT_LOCATION) {}

        assertNull(state.activeClusterMemberIds)
        assertEquals(2L, state.searchGeneration)
    }

    @Test
    fun `사용자 제스처는 대기 검색을 취소하고 사용자가 고른 화면 영역으로 표시한다`() {
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
    fun `카메라가 대기 목표에 도착하면 프로그램 검색을 보낸다`() {
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
    fun `카메라가 대기 목표에 못 미쳐 멈추면 계속 기다린다`() {
        val state = HomeMapState()
        state.moveWithSearch(GeoPoint(35.0, 129.0), 13, MapSearchMoveReason.REGION) {}

        val action = state.onCameraMoveEnd(viewport, zoomLevel = 13, InitialLocationState.Unavailable, hasCurrentLocation = false)

        assertEquals(CameraSettleAction.None, action)
        assertNotNull(state.pendingMapSearch)
    }

    @Test
    fun `대기 검색 없이 멈춘 화면 영역은 초기 검색이 허용될 때만 보낸다`() {
        val waitingForLocation = HomeMapState()
        val centered = HomeMapState(hasCenteredInitialLocation = true)

        val blocked = waitingForLocation.onCameraMoveEnd(viewport, 13, InitialLocationState.Pending, hasCurrentLocation = false)
        val allowed = centered.onCameraMoveEnd(viewport, 13, InitialLocationState.Ready, hasCurrentLocation = true)

        assertEquals(CameraSettleAction.None, blocked)
        assertEquals(CameraSettleAction.ViewportSettled, allowed)
    }

    @Test
    fun `초기 위치로 카메라를 옮기는 중에는 초기 검색을 막는다`() {
        val state = HomeMapState(hasCenteredInitialLocation = true)
        state.isInitialLocationCameraMovePending = true

        val action = state.onCameraMoveEnd(viewport, 13, InitialLocationState.Ready, hasCurrentLocation = true)

        assertEquals(CameraSettleAction.None, action)
    }

    @Test
    fun `측정 가능한 화면 영역 없이 카메라가 멈추면 줌 레벨만 기록한다`() {
        val state = HomeMapState(currentViewport = viewport)
        state.moveWithSearch(center, 13, MapSearchMoveReason.REGION) {}

        val action = state.onCameraMoveEnd(null, zoomLevel = 15, InitialLocationState.Ready, hasCurrentLocation = true)

        assertEquals(CameraSettleAction.None, action)
        assertEquals(15, state.zoomLevel)
        assertNotNull(state.pendingMapSearch)
    }

    @Test
    fun `화면에 다시 들어오면 중심 맞춤 상태를 초기화한다`() {
        val state = HomeMapState(hasUserMovedMap = true, hasUserChosenMapViewport = true, hasCenteredInitialLocation = true)

        state.resetEntryFlags()

        assertFalse(state.hasUserMovedMap)
        assertFalse(state.hasUserChosenMapViewport)
        assertFalse(state.hasCenteredInitialLocation)
    }

    @Test
    fun `상태 저장은 화면 영역과 상태 값을 복원하고 진행 중인 검색은 복원하지 않는다`() {
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
