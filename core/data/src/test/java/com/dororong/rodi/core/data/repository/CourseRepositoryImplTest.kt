package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.remote.directions.KakaoDirectionsClient
import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.domain.model.course.CourseFeatures
import com.dororong.rodi.core.domain.model.course.Waypoint
import com.dororong.rodi.core.domain.model.course.WaypointType
import com.kakao.vectormap.LatLng
import com.dororong.rodi.core.domain.model.course.CoursePoint
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class CourseRepositoryImplTest {
    @Test
    fun `경로 탐색 결과를 도메인 경로로 매핑한다`() = runTest {
        val directionsClient = mockk<KakaoDirectionsClient>()
        val course = testCourse()
        coEvery {
            directionsClient.getRoute(course.origin, course.waypointPoints, course.destination)
        } returns KakaoDirectionsClient.RouteResult(
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

    @Test
    fun `같은 경유지 경로는 길찾기를 한 번만 부른다`() = runTest {
        val directionsClient = mockk<KakaoDirectionsClient>()
        coEvery { directionsClient.getRoute(start, emptyList(), end) } returns route(isRealRoute = true)
        val repository = CourseRepositoryImpl(directionsClient = directionsClient)

        repository.getRoute(start, emptyList(), end)
        repository.getRoute(start, emptyList(), end)

        coVerify(exactly = 1) { directionsClient.getRoute(start, emptyList(), end) }
    }

    @Test
    fun `같은 코스의 경로는 코스로 조회해도 길찾기를 한 번만 부른다`() = runTest {
        val directionsClient = mockk<KakaoDirectionsClient>()
        val course = testCourse()
        coEvery {
            directionsClient.getRoute(course.origin, course.waypointPoints, course.destination)
        } returns route(isRealRoute = true)
        val repository = CourseRepositoryImpl(directionsClient = directionsClient)

        repository.getRoute(course)
        repository.getRoute(course)

        coVerify(exactly = 1) {
            directionsClient.getRoute(course.origin, course.waypointPoints, course.destination)
        }
    }

    @Test
    fun `직선으로 대신 그린 경로는 담지 않고 다음에 다시 길찾기를 부른다`() = runTest {
        val directionsClient = mockk<KakaoDirectionsClient>()
        coEvery { directionsClient.getRoute(start, emptyList(), end) } returns route(isRealRoute = false)
        val repository = CourseRepositoryImpl(directionsClient = directionsClient)

        assertFalse(repository.getRoute(start, emptyList(), end).isRealRoute)
        repository.getRoute(start, emptyList(), end)

        coVerify(exactly = 2) { directionsClient.getRoute(start, emptyList(), end) }
    }

    private val start = CoursePoint("출발", 37.0, 127.0)
    private val end = CoursePoint("도착", 37.1, 127.1)

    private fun route(isRealRoute: Boolean) = KakaoDirectionsClient.RouteResult(
        points = listOf(LatLng.from(37.0, 127.0)),
        isRealRoute = isRealRoute,
        totalDistanceMeters = 100,
        snappedPoints = emptyList(),
    )
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
