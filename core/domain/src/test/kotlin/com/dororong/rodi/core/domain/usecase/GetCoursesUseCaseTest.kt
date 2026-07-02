package com.dororong.rodi.core.domain.usecase

import com.dororong.rodi.core.domain.Course
import com.dororong.rodi.core.domain.CourseFeatures
import com.dororong.rodi.core.domain.CourseRepository
import com.dororong.rodi.core.domain.RodiItemType
import com.dororong.rodi.core.domain.Waypoint
import com.dororong.rodi.core.domain.WaypointType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class GetCoursesUseCaseTest {

    @Test
    fun `invoke returns courses from repository`() {
        val repository = mockk<CourseRepository>()
        val courses = listOf(testCourse())
        every { repository.getCourses() } returns courses
        val useCase = GetCoursesUseCase(repository)

        val result = useCase()

        assertSame(courses, result)
        verify(exactly = 1) { repository.getCourses() }
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
