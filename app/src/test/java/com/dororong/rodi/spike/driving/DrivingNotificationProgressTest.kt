package com.dororong.rodi.spike.driving

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.driving.DrivingSession
import com.dororong.rodi.core.domain.model.driving.DrivingSessionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DrivingNotificationProgressTest {
    @Test
    fun `필요 거리의 절반을 달리면 코스 전체 거리와 무관하게 50퍼센트로 표시한다`() {
        val session = session(plannedDistanceMeters = 5_000, requiredDistanceMeters = 2_000)

        assertEquals(50, session.notificationProgressPercent(1_000.0))
    }

    @Test
    fun `필요 거리에 닿기 직전에는 99퍼센트에서 멈춘다`() {
        val session = session(plannedDistanceMeters = 5_000, requiredDistanceMeters = 2_000)

        assertEquals(99, session.notificationProgressPercent(1_999.0))
        assertEquals(99, session.notificationProgressPercent(2_400.0))
    }

    @Test
    fun `출발 직후에는 0퍼센트다`() {
        val session = session(plannedDistanceMeters = 5_000, requiredDistanceMeters = 2_000)

        assertEquals(0, session.notificationProgressPercent(0.0))
    }

    @Test
    fun `필요 거리가 없는 세션은 코스 전체 거리를 기준으로 계산한다`() {
        val session = session(plannedDistanceMeters = 4_000, requiredDistanceMeters = null)

        assertEquals(25, session.notificationProgressPercent(1_000.0))
    }

    @Test
    fun `기준 거리가 하나도 없으면 진행률을 계산하지 않는다`() {
        val session = session(plannedDistanceMeters = null, requiredDistanceMeters = null)

        assertNull(session.notificationProgressPercent(1_000.0))
    }

    private fun session(
        plannedDistanceMeters: Int?,
        requiredDistanceMeters: Int?,
    ) = DrivingSession(
        id = "session",
        placeId = 1L,
        placeName = "테스트 코스",
        destination = GeoPoint(lat = 37.0, lng = 127.0),
        plannedDistanceMeters = plannedDistanceMeters,
        startedAtEpochMillis = 0L,
        arrivedAtEpochMillis = null,
        traveledDistanceMeters = 0.0,
        status = DrivingSessionStatus.ACTIVE,
        requiredDistanceMeters = requiredDistanceMeters,
    )
}
