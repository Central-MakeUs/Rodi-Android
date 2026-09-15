package com.dororong.rodi.feature.home.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import com.dororong.rodi.core.domain.model.course.GeoPoint

internal enum class CameraSettleAction {
    /** 코드가 요청한 이동(pending)이 목표에 도달했다. 그 위치로 검색한다. */
    ProgrammaticSearch,

    /** 사용자가 옮긴 뒤 멈춘 화면. 초기 검색 조건을 만족하면 현재 화면으로 검색한다. */
    ViewportSettled,
    None,
}

/**
 * 홈 지도 화면의 카메라·검색 조율 상태.
 *
 * KakaoMap을 직접 들고 있지 않아 JVM 단위 테스트로 검증할 수 있다. 카메라 이동은 호출하는 쪽이
 * [moveWithSearch]의 `moveCamera`로 넘긴다.
 */
@Stable
internal class HomeMapState(
    zoomLevel: Int = DEFAULT_ZOOM,
    currentViewport: MapViewport? = null,
    hasUserMovedMap: Boolean = false,
    hasUserChosenMapViewport: Boolean = false,
    hasCenteredInitialLocation: Boolean = false,
) {
    var mapViewSize by mutableStateOf(IntSize.Zero)
    var zoomLevel by mutableIntStateOf(zoomLevel)
        private set
    var currentViewport by mutableStateOf(currentViewport)
    var pendingMapSearch by mutableStateOf<PendingMapSearch?>(null)
        private set
    var activeClusterMemberIds by mutableStateOf<Set<Long>?>(null)
        private set
    var searchGeneration by mutableLongStateOf(0L)
        private set
    var isInitialLocationCameraMovePending by mutableStateOf(false)
    var isAtCurrentLocation by mutableStateOf(false)
    var hasUserMovedMap by mutableStateOf(hasUserMovedMap)
        private set
    var hasUserChosenMapViewport by mutableStateOf(hasUserChosenMapViewport)
    var hasCenteredInitialLocation by mutableStateOf(hasCenteredInitialLocation)

    /**
     * 카메라를 옮기고, 도착하면 그 위치로 검색하도록 예약한다.
     *
     * 순서가 중요하다. pending을 먼저 기록한 뒤 카메라를 움직여야 이동 종료 콜백이 이번 이동을
     * 알아볼 수 있다. 순서가 뒤집히면 도착해도 검색이 나가지 않는다.
     */
    fun moveWithSearch(
        target: GeoPoint,
        targetZoom: Int?,
        reason: MapSearchMoveReason,
        requiredBounds: MapViewport? = null,
        clusterMemberIds: Set<Long>? = null,
        moveCamera: () -> Unit,
    ) {
        activeClusterMemberIds = clusterMemberIds
        searchGeneration += 1
        pendingMapSearch = PendingMapSearch(
            generation = searchGeneration,
            target = target,
            targetZoom = targetZoom,
            reason = reason,
            requiredBounds = requiredBounds,
        )
        moveCamera()
    }

    /** 사용자가 손으로 지도를 움직이면 코드가 예약한 이동·클러스터 범위는 의미가 없어진다. */
    fun onUserGesture() {
        isAtCurrentLocation = false
        hasUserMovedMap = true
        hasUserChosenMapViewport = true
        pendingMapSearch = null
        activeClusterMemberIds = null
        isInitialLocationCameraMovePending = false
    }

    fun onCameraMoveEnd(
        viewport: MapViewport?,
        zoomLevel: Int,
        locationState: InitialLocationState,
        hasCurrentLocation: Boolean,
    ): CameraSettleAction {
        this.zoomLevel = zoomLevel
        viewport ?: return CameraSettleAction.None
        currentViewport = viewport
        val pending = pendingMapSearch
        return when {
            pending != null && PendingMapSearchMatcher.matches(pending, viewport, zoomLevel) -> {
                pendingMapSearch = null
                isInitialLocationCameraMovePending = false
                CameraSettleAction.ProgrammaticSearch
            }

            pending != null && pending.generation != searchGeneration -> {
                pendingMapSearch = null
                CameraSettleAction.None
            }

            pending == null && InitialViewportSearchPolicy.canDispatch(
                locationState = locationState,
                hasCurrentLocation = hasCurrentLocation,
                hasCenteredInitialLocation = hasCenteredInitialLocation,
                isInitialLocationCameraMovePending = isInitialLocationCameraMovePending,
            ) -> CameraSettleAction.ViewportSettled

            else -> CameraSettleAction.None
        }
    }

    /** 화면에 새로 들어오거나 앱으로 돌아오면 현재 위치로 다시 맞출 기회를 준다. */
    fun resetEntryFlags() {
        hasCenteredInitialLocation = false
        hasUserMovedMap = false
        hasUserChosenMapViewport = false
    }

    companion object {
        val Saver: Saver<HomeMapState, Any> = Saver(
            save = { state ->
                val viewport = state.currentViewport
                arrayListOf<Any?>(
                    state.zoomLevel,
                    viewport?.northEast?.lat,
                    viewport?.northEast?.lng,
                    viewport?.southWest?.lat,
                    viewport?.southWest?.lng,
                    state.hasUserMovedMap,
                    state.hasUserChosenMapViewport,
                    state.hasCenteredInitialLocation,
                )
            },
            restore = { saved ->
                @Suppress("UNCHECKED_CAST")
                val values = saved as List<Any?>
                val northEastLat = values[1] as? Double
                val northEastLng = values[2] as? Double
                val southWestLat = values[3] as? Double
                val southWestLng = values[4] as? Double
                val viewport = if (
                    northEastLat != null && northEastLng != null && southWestLat != null && southWestLng != null
                ) {
                    MapViewport(
                        northEast = GeoPoint(northEastLat, northEastLng),
                        southWest = GeoPoint(southWestLat, southWestLng),
                    )
                } else {
                    null
                }
                HomeMapState(
                    zoomLevel = values[0] as Int,
                    currentViewport = viewport,
                    hasUserMovedMap = values[5] as Boolean,
                    hasUserChosenMapViewport = values[6] as Boolean,
                    hasCenteredInitialLocation = values[7] as Boolean,
                )
            },
        )
    }
}

@Composable
internal fun rememberHomeMapState(): HomeMapState = rememberSaveable(saver = HomeMapState.Saver) { HomeMapState() }
