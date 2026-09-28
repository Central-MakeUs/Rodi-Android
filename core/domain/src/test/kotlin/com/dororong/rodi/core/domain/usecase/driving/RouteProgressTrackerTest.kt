package com.dororong.rodi.core.domain.usecase.driving

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.driving.DrivingLocationSample
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RouteProgressTrackerTest {
    @Test
    fun `경로를 따라 앞으로 가면 인정 거리가 늘어난다`() {
        val route = listOf(GeoPoint(37.5000, 127.0000), GeoPoint(37.5000, 127.0050))
        val tracker = RouteProgressTracker(route, requiredDistanceMeters = 400)

        val start = tracker.add(sample(37.5000, 127.0000, 1_000))
        val moved = tracker.add(sample(37.5000, 127.0025, 2_000))

        assertTrue(start.isInCourseScope)
        assertTrue(moved.recognizedDistanceMeters in 200.0..250.0)
    }

    @Test
    fun `경로 옆으로 벗어났다 돌아와도 인정 거리가 줄지 않는다`() {
        val route = listOf(GeoPoint(37.5000, 127.0000), GeoPoint(37.5000, 127.0050))
        val tracker = RouteProgressTracker(route, requiredDistanceMeters = 400)

        tracker.add(sample(37.5000, 127.0025, 1_000))
        val before = tracker.add(sample(37.5000, 127.0025, 2_000)).recognizedDistanceMeters
        // 경로에서 수직으로 100m 벗어남(위도 이동) — 여전히 200m 반경 코스 안
        val detoured = tracker.add(sample(37.5009, 127.0025, 3_000))
        val backOnRoute = tracker.add(sample(37.5000, 127.0025, 4_000))

        assertTrue(detoured.recognizedDistanceMeters >= before)
        assertTrue(backOnRoute.recognizedDistanceMeters >= before)
    }

    @Test
    fun `왕복 코스는 좌표가 겹쳐도 가는 길과 오는 길의 진행도를 모두 채운다`() {
        // 왕복 코스: 출발 -> 회차 -> 출발(같은 물리 경로를 두 번 지남)
        val outbound = GeoPoint(37.5000, 127.0000)
        val turnaround = GeoPoint(37.5000, 127.0050)
        val route = listOf(outbound, turnaround, outbound)
        val tracker = RouteProgressTracker(route, requiredDistanceMeters = 800)

        tracker.add(sample(37.5000, 127.0000, 1_000))
        val halfwayOut = tracker.add(sample(37.5000, 127.0025, 2_000))
        val atTurnaround = tracker.add(sample(37.5000, 127.0050, 3_000))
        // 같은 좌표(중간 지점)를 되돌아오는 길에 다시 지남 — 가는 길 값(~220m)이 아니라
        // 오는 길 값(~660m)으로 매칭돼야 한다.
        val halfwayBack = tracker.add(sample(37.5000, 127.0025, 4_000))
        val backAtStart = tracker.add(sample(37.5000, 127.0000, 5_000))

        assertTrue(halfwayOut.recognizedDistanceMeters in 150.0..300.0)
        assertTrue(atTurnaround.recognizedDistanceMeters in 400.0..480.0)
        assertTrue(
            halfwayBack.recognizedDistanceMeters > atTurnaround.recognizedDistanceMeters,
            "왕복 후 인정거리가 회차 지점 이후로 전진해야 한다",
        )
        assertTrue(backAtStart.recognizedDistanceMeters in 850.0..920.0)
        assertTrue(backAtStart.progressPercent >= 100)
    }

    @Test
    fun `경로에서 멀리 떨어진 위치는 진행도를 올리지 않는다`() {
        val route = listOf(GeoPoint(37.5000, 127.0000), GeoPoint(37.5000, 127.0050))
        val tracker = RouteProgressTracker(route, requiredDistanceMeters = 400)

        tracker.add(sample(37.5000, 127.0000, 1_000))
        val farAway = tracker.add(sample(37.55, 127.05, 2_000))

        assertFalse(farAway.isInCourseScope)
        assertEquals(0.0, farAway.recognizedDistanceMeters)
    }

    @Test
    fun `경과 시간상 가능한 앞쪽 GPS 튐은 진행도로 복구한다`() {
        val route = listOf(GeoPoint(37.5000, 127.0000), GeoPoint(37.5000, 127.0200))
        val tracker = RouteProgressTracker(route, requiredDistanceMeters = 2_000)

        tracker.add(sample(37.5000, 127.0000, 1_000))
        val recovered = tracker.add(sample(37.5000, 127.0060, 16_000))

        assertTrue(recovered.isInCourseScope)
        assertTrue(recovered.recognizedDistanceMeters > 300.0)
    }

    @Test
    fun `물리적으로 불가능한 앞쪽 GPS 튐은 진행도로 복구하지 않는다`() {
        val route = listOf(GeoPoint(37.5000, 127.0000), GeoPoint(37.5000, 127.0200))
        val tracker = RouteProgressTracker(route, requiredDistanceMeters = 2_000)

        tracker.add(sample(37.5000, 127.0000, 1_000))
        val rejected = tracker.add(sample(37.5000, 127.0060, 2_000))

        assertFalse(rejected.isInCourseScope)
        assertEquals(0.0, rejected.recognizedDistanceMeters)
    }

    @Test
    fun `인증 필요 거리는 코스 길이의 40퍼센트이고 최대 5킬로미터다`() {
        assertEquals(520, requiredCertifiedDistanceMeters(1_300))
        assertEquals(5_000, requiredCertifiedDistanceMeters(14_800))
        assertEquals(5_000, requiredCertifiedDistanceMeters(40_000))
    }

    private fun sample(
        latitude: Double,
        longitude: Double,
        elapsedRealtimeMillis: Long,
        accuracyMeters: Float = 10f,
    ) = DrivingLocationSample(
        point = GeoPoint(latitude, longitude),
        accuracyMeters = accuracyMeters,
        elapsedRealtimeMillis = elapsedRealtimeMillis,
    )
}
