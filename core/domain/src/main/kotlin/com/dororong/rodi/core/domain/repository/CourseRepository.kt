package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.domain.model.course.CoursePoint
import com.dororong.rodi.core.domain.model.course.RouteResult

interface CourseRepository {
    suspend fun getRoute(course: Course): RouteResult
    suspend fun getRoute(
        origin: CoursePoint,
        waypoints: List<CoursePoint>,
        destination: CoursePoint,
    ): RouteResult
}
