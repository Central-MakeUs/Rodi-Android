package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.domain.model.course.CoursePoint
import com.dororong.rodi.core.domain.model.course.RouteResult
import kotlinx.coroutines.flow.Flow

interface CourseRepository {
    fun getCourses(): List<Course>
    suspend fun getRoute(course: Course): RouteResult
    suspend fun getRoute(
        origin: CoursePoint,
        waypoints: List<CoursePoint>,
        destination: CoursePoint,
    ): RouteResult
    fun observeSavedCourseIds(): Flow<Set<Int>>
    suspend fun toggleSavedCourse(courseId: Int)
}
