package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.course.CoursePoint
import com.dororong.rodi.core.domain.model.course.RouteResult

interface CourseRegistrationRouteRepository {
    suspend fun getStrictRoute(
        origin: CoursePoint,
        waypoints: List<CoursePoint>,
        destination: CoursePoint,
    ): RouteResult
}
