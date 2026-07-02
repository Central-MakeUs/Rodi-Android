package com.dororong.rodi.core.data

import com.dororong.rodi.core.data.directions.KakaoDirectionsClient
import com.dororong.rodi.core.domain.Course
import com.dororong.rodi.core.domain.CourseRepository
import com.dororong.rodi.core.domain.GeoPoint
import com.dororong.rodi.core.domain.RouteResult
import javax.inject.Inject

class CourseRepositoryImpl @Inject constructor() : CourseRepository {
    override fun getCourses(): List<Course> = SampleCourses.RODI_COURSES

    override suspend fun getRoute(course: Course): RouteResult {
        val raw = KakaoDirectionsClient.getRoute(course)
        return RouteResult(
            points = raw.points.map { GeoPoint(it.latitude, it.longitude) },
            isRealRoute = raw.isRealRoute,
            totalDistanceMeters = raw.totalDistanceMeters,
            snappedPoints = raw.snappedPoints.map { GeoPoint(it.latitude, it.longitude) },
        )
    }
}
