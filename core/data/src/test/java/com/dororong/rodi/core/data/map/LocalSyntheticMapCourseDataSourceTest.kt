package com.dororong.rodi.core.data.map

import com.dororong.rodi.core.domain.GeoPoint
import com.dororong.rodi.core.domain.MapViewportQuery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LocalSyntheticMapCourseDataSourceTest {
    private val source = LocalSyntheticMapCourseDataSource()

    @Test
    fun `creates exactly 300 deterministic courses`() {
        assertEquals(300, source.allCourses().size)
        assertEquals(source.allCourses(), LocalSyntheticMapCourseDataSource().allCourses())
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
