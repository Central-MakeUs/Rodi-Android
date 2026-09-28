package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ViewportSearchThresholdTest {
    @Test
    fun `초기 검색은 현재 위치로 카메라 이동이 끝날 때까지 기다린다`() {
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
    fun `위치를 쓸 수 없을 때만 초기 검색에 화면 영역을 쓴다`() {
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
    fun `화면 영역은 경계 좌표를 포함하고 바깥 좌표는 제외한다`() {
        val viewport = viewport(centerLongitude = 126.98)

        assertTrue(viewport.contains(viewport.northEast))
        assertTrue(viewport.contains(viewport.southWest))
        assertFalse(viewport.contains(GeoPoint(37.61, 126.98)))
        assertFalse(viewport.contains(GeoPoint(37.55, 127.01)))
    }

    @Test
    fun `클러스터 회원 범위는 모든 회원 좌표를 포함한다`() {
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
    fun `마커 화면 영역은 마지막 검색 영역보다 현재 카메라를 따른다`() {
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
    fun `위치 스트림이 다시 시작되는 동안에는 복원한 화면 영역을 우선한다`() {
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
    fun `화면 영역과 현재 위치가 없으면 마지막으로 저장한 중심을 쓴다`() {
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
    fun `카메라 중심이 모두 없으면 기본값을 쓴다`() {
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
    fun `마지막으로 저장한 중심보다 현재 위치를 우선한다`() {
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
    fun `현재 위치와 마지막 중심보다 저장한 화면 영역을 우선한다`() {
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
