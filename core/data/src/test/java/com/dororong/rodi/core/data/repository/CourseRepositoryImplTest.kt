package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.remote.directions.KakaoDirectionsClient
import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.domain.model.course.CourseFeatures
import com.dororong.rodi.core.domain.model.course.Waypoint
import com.dororong.rodi.core.domain.model.course.WaypointType
import com.kakao.vectormap.LatLng
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CourseRepositoryImplTest {
    @Test
    fun `maps injected directions result to domain route`() = runTest {
        val directionsClient = mockk<KakaoDirectionsClient>()
        val course = testCourse()
        coEvery { directionsClient.getRoute(course) } returns KakaoDirectionsClient.RouteResult(
            points = listOf(LatLng.from(37.1, 127.1)),
            isRealRoute = true,
            totalDistanceMeters = 1_200,
            snappedPoints = listOf(LatLng.from(37.2, 127.2)),
        )

        val result = CourseRepositoryImpl(directionsClient = directionsClient).getRoute(course)

        assertTrue(result.isRealRoute)
        assertEquals(1_200, result.totalDistanceMeters)
        assertEquals(37.1, result.points.single().lat)
        assertEquals(127.2, result.snappedPoints.single().lng)
    }
}

private fun testCourse() = Course(
    id = 1,
    courseName = "테스트 코스",
    courseNickname = "테스트",
    areaName = "테스트동",
    region = "seoul",
    difficulty = 1,
    trafficDensity = null,
    source = "test",
    sourceUrl = "",
    crawledAt = "",
    waypoints = listOf(
        Waypoint(
            order = 0,
            type = WaypointType.START,
            name = "출발",
            lat = 37.5665,
            lng = 126.9780,
            address = "서울",
            category = "test",
        ),
        Waypoint(
            order = 1,
            type = WaypointType.END,
            name = "도착",
            lat = 37.5651,
            lng = 126.9895,
            address = "서울",
            category = "test",
        ),
    ),
    features = CourseFeatures(),
    recommendation = 1,
    caution = "",
    bestTime = "",
    enrichedDescription = "",
)
