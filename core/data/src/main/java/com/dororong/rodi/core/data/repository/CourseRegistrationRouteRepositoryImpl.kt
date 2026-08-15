package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.remote.directions.KakaoDirectionsClient
import com.dororong.rodi.core.domain.model.course.CoursePoint
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.course.RouteResult
import com.dororong.rodi.core.domain.repository.CourseRegistrationRouteRepository
import javax.inject.Inject

class CourseRegistrationRouteRepositoryImpl @Inject constructor(
    private val directionsClient: KakaoDirectionsClient,
) : CourseRegistrationRouteRepository {
    override suspend fun getStrictRoute(
        origin: CoursePoint,
        waypoints: List<CoursePoint>,
        destination: CoursePoint,
    ): RouteResult = directionsClient.getStrictRoute(origin, waypoints, destination).let { result ->
        RouteResult(
            points = result.points.map { GeoPoint(it.latitude, it.longitude) },
            isRealRoute = result.isRealRoute,
            totalDistanceMeters = result.totalDistanceMeters,
            snappedPoints = result.snappedPoints.map { GeoPoint(it.latitude, it.longitude) },
        )
    }
}
