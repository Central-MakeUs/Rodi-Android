package com.dororong.rodi.core.ui.map

import com.dororong.rodi.core.domain.model.course.GeoPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LastMapCameraStoreTest {
    @Test
    fun `valid camera values produce a snapshot`() {
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
    fun `latitude outside the valid range returns null`() {
        assertNull(
            mapCameraSnapshotOrNull(
                lat = 90.1,
                lng = 128.3446,
                zoom = 13,
            ),
        )
    }

    @Test
    fun `longitude outside the valid range returns null`() {
        assertNull(
            mapCameraSnapshotOrNull(
                lat = 36.1195,
                lng = 180.1,
                zoom = 13,
            ),
        )
    }

    @Test
    fun `non-positive zoom returns null`() {
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
