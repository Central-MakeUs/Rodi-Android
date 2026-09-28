package com.dororong.rodi.core.ui.map

import com.dororong.rodi.core.domain.model.course.GeoPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LastMapCameraStoreTest {
    @Test
    fun `유효한 카메라 값이면 스냅샷을 만든다`() {
        assertEquals(
            MapCameraSnapshot(
                center = GeoPoint(36.1195, 128.3446),
                zoomLevel = 13,
            ),
            mapCameraSnapshotOrNull(
                lat = 36.1195,
                lng = 128.3446,
                zoom = 13,
            ),
        )
    }

    @Test
    fun `위도가 범위를 벗어나면 null을 반환한다`() {
        assertNull(
            mapCameraSnapshotOrNull(
                lat = 90.1,
                lng = 128.3446,
                zoom = 13,
            ),
        )
    }

    @Test
    fun `경도가 범위를 벗어나면 null을 반환한다`() {
        assertNull(
            mapCameraSnapshotOrNull(
                lat = 36.1195,
                lng = 180.1,
                zoom = 13,
            ),
        )
    }

    @Test
    fun `줌이 0 이하면 null을 반환한다`() {
        assertNull(
            mapCameraSnapshotOrNull(
                lat = 36.1195,
                lng = 128.3446,
                zoom = 0,
            ),
        )
        assertNull(
            mapCameraSnapshotOrNull(
                lat = 36.1195,
                lng = 128.3446,
                zoom = -1,
            ),
        )
    }
}
