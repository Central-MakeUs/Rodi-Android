package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.mapper.toDomain
import com.dororong.rodi.core.data.source.remote.directions.KakaoDirectionsClient
import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.domain.model.course.CoursePoint
import com.dororong.rodi.core.domain.model.course.RouteResult
import com.dororong.rodi.core.domain.repository.CourseRepository
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CourseRepositoryImpl @Inject constructor(
    private val directionsClient: KakaoDirectionsClient,
) : CourseRepository {
    private val routeCacheMutex = Mutex()
    private val routeCache = object : LinkedHashMap<RouteKey, RouteResult>(ROUTE_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<RouteKey, RouteResult>?) =
            size > ROUTE_CACHE_SIZE
    }

    override suspend fun getRoute(course: Course): RouteResult = directionsClient.getRoute(course).toDomain()

    /** 같은 경유지 경로는 다시 길찾기를 부르지 않는다. 직선으로 대신 그린 결과는 다음에 다시 시도하도록 담지 않는다. */
    override suspend fun getRoute(
        origin: CoursePoint,
        waypoints: List<CoursePoint>,
        destination: CoursePoint,
    ): RouteResult {
        val key = RouteKey(origin, waypoints, destination)
        routeCacheMutex.withLock { routeCache[key] }?.let { return it }
        val route = directionsClient.getRoute(origin, waypoints, destination).toDomain()
        if (route.isRealRoute) routeCacheMutex.withLock { routeCache[key] = route }
        return route
    }

    private data class RouteKey(
        val origin: CoursePoint,
        val waypoints: List<CoursePoint>,
        val destination: CoursePoint,
    )
}

private const val ROUTE_CACHE_SIZE = 30
