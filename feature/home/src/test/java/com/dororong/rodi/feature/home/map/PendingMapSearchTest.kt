package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.model.course.GeoPoint
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PendingMapSearchTest {
    private val viewport = MapViewport(
        northEast = GeoPoint(38.0, 128.0),
        southWest = GeoPoint(36.0, 126.0),
    )

    @Test
    fun `목표 중심과 줌이 일치하면 검색을 완료할 수 있다`() {
        val pending = pending(target = GeoPoint(37.0, 127.0), zoom = 13)

        assertTrue(PendingMapSearchMatcher.matches(pending, viewport, zoomLevel = 13))
    }

    @Test
    fun `이전 카메라 종료는 새 pending 검색과 일치하지 않는다`() {
        val pending = pending(target = GeoPoint(35.0, 129.0), zoom = 13)

        assertFalse(PendingMapSearchMatcher.matches(pending, viewport, zoomLevel = 13))
    }

    @Test
    fun `중심이 맞아도 줌이 다르면 검색을 완료하지 않는다`() {
        val pending = pending(target = GeoPoint(37.0, 127.0), zoom = 14)

        assertFalse(PendingMapSearchMatcher.matches(pending, viewport, zoomLevel = 13))
    }

    @Test
    fun `viewport span의 5퍼센트까지 목표 오차를 허용한다`() {
        val inside = pending(target = GeoPoint(37.0999, 127.0999), zoom = 13)
        val outside = pending(target = GeoPoint(37.1001, 127.0), zoom = 13)

        assertTrue(PendingMapSearchMatcher.matches(inside, viewport, zoomLevel = 13))
        assertFalse(PendingMapSearchMatcher.matches(outside, viewport, zoomLevel = 13))
    }

    @Test
    fun `fit bounds 이동은 모든 멤버 좌표가 viewport 안에 들어오면 완료된다`() {
        val pending = PendingMapSearch(
            generation = 1,
            target = GeoPoint(37.0, 127.0),
            targetZoom = null,
            reason = MapSearchMoveReason.CLUSTER,
            requiredBounds = MapViewport(
                northEast = GeoPoint(37.5, 127.5),
                southWest = GeoPoint(36.5, 126.5),
            ),
        )

        assertTrue(PendingMapSearchMatcher.matches(pending, viewport, zoomLevel = 8))
        assertFalse(
            PendingMapSearchMatcher.matches(
                pending,
                MapViewport(GeoPoint(37.4, 127.4), GeoPoint(36.6, 126.6)),
                zoomLevel = 8,
            ),
        )
    }

    private fun pending(target: GeoPoint, zoom: Int) = PendingMapSearch(
        generation = 1,
        target = target,
        targetZoom = zoom,
        reason = MapSearchMoveReason.INITIAL_LOCATION,
    )
}
