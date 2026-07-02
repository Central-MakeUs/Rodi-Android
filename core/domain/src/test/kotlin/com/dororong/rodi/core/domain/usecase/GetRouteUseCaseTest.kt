package com.dororong.rodi.core.domain.usecase

import com.dororong.rodi.core.domain.Course
import com.dororong.rodi.core.domain.CourseFeatures
import com.dororong.rodi.core.domain.CourseRepository
import com.dororong.rodi.core.domain.GeoPoint
import com.dororong.rodi.core.domain.RodiItemType
import com.dororong.rodi.core.domain.RouteResult
import com.dororong.rodi.core.domain.Waypoint
import com.dororong.rodi.core.domain.WaypointType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetRouteUseCaseTest {

    @Test
    fun `invoke returns success when repository returns route`() = runTest {
        val repository = mockk<CourseRepository>()
        val course = testCourse()
        val routeResult = RouteResult(points = listOf(GeoPoint(37.5665, 126.9780)), isRealRoute = true)
        coEvery { repository.getRoute(course) } returns routeResult
        val useCase = GetRouteUseCase(repository)

        val result = useCase(course)

        assertTrue(result.isSuccess)
        assertEquals(routeResult, result.getOrThrow())
    }

    @Test
    fun `invoke wraps repository failure as Result failure`() = runTest {
        val repository = mockk<CourseRepository>()
        val course = testCourse()
        coEvery { repository.getRoute(course) } throws RuntimeException("boom")
        val useCase = GetRouteUseCase(repository)

        val result = useCase(course)

        assertTrue(result.isFailure)
        assertEquals("boom", result.exceptionOrNull()?.message)
    }
}

private fun testCourse(id: Int = 1, itemType: RodiItemType = RodiItemType.COURSE) = Course(
    id = id,
    courseName = "테스트 코스",
    courseNickname = "테스트",
    areaName = "테스트동",
    region = "seoul",
    difficulty = 1,
    trafficDensity = null,
    source = "test",
    sourceUrl = "",
    crawledAt = "",
    waypoints = testWaypoints(),
    features = CourseFeatures(),
    recommendation = 1,
    caution = "",
    bestTime = "",
    enrichedDescription = "",
    itemType = itemType,
)

private fun testWaypoints() = listOf(
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
)
