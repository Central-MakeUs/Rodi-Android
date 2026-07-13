package com.dororong.rodi.core.data.map

import com.dororong.rodi.core.domain.GeoPoint
import com.dororong.rodi.core.domain.MapViewportQuery
import com.dororong.rodi.core.data.SampleCourses
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LocalMapCourseFixtureDataSourceTest {
    private val source = LocalMapCourseFixtureDataSource()

    @Test
    fun `adds deterministic n queen markers to the existing sample course data`() {
        val courses = source.allCourses()

        assertEquals(SampleCourses.RODI_COURSES.size + 40, courses.size)
        assertEquals(courses, LocalMapCourseFixtureDataSource().allCourses())
        assertEquals(40, courses.count { it.source == "local-n-queens-spike" })
    }

    @Test
    fun `places each n queen board on unique rows and columns`() {
        val boards = source.allCourses()
            .filter { it.source == "local-n-queens-spike" }
            .chunked(8)

        assertEquals(5, boards.size)
        boards.forEach { board ->
            assertEquals(8, board.map { it.startWaypoint.lat }.distinct().size)
            assertEquals(8, board.map { it.startWaypoint.lng }.distinct().size)
        }
    }

    @Test
    fun `includes courses on north east and south west boundaries`() = runTest {
        val course = source.allCourses().first()
        val point = course.startWaypoint
        val query = MapViewportQuery(
            northEast = GeoPoint(point.lat, point.lng),
            southWest = GeoPoint(point.lat, point.lng),
            zoomLevel = 13,
        )

        assertEquals(listOf(course), source.getCourses(query))
    }

    @Test
    fun `returns only courses inside viewport`() = runTest {
        val query = MapViewportQuery(
            northEast = GeoPoint(37.70, 127.15),
            southWest = GeoPoint(37.40, 126.80),
            zoomLevel = 13,
        )

        val courses = source.getCourses(query)

        assertTrue(courses.isNotEmpty())
        assertTrue(courses.all { course ->
            course.startWaypoint.lat in query.southWest.lat..query.northEast.lat &&
                    course.startWaypoint.lng in query.southWest.lng..query.northEast.lng
        })
    }
}
