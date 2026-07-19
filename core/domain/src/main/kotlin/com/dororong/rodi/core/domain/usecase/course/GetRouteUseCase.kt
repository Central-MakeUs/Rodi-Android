package com.dororong.rodi.core.domain.usecase.course

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.domain.model.course.CoursePoint
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceWaypointType
import com.dororong.rodi.core.domain.repository.CourseRepository
import com.dororong.rodi.core.domain.model.course.RouteResult
import javax.inject.Inject

class GetRouteUseCase @Inject constructor(
    private val courseRepository: CourseRepository,
) {
    suspend operator fun invoke(course: Course): Result<RouteResult> =
        runSuspendCatching { courseRepository.getRoute(course) }

    suspend operator fun invoke(place: PlaceDetail): Result<RouteResult> = runSuspendCatching {
        val course = requireNotNull(place.course) { "코스 상세 정보가 없습니다." }
        val ordered = course.waypoints.sortedBy { it.sequence }
        val start = ordered.firstOrNull { it.type == PlaceWaypointType.START }
            ?: error("출발 지점이 없습니다.")
        val destination = ordered.firstOrNull { it.type == PlaceWaypointType.DESTINATION }
            ?: error("도착 지점이 없습니다.")
        courseRepository.getRoute(
            origin = CoursePoint(start.name.orEmpty(), start.point.lat, start.point.lng),
            waypoints = ordered.filter { it.type == PlaceWaypointType.VIA }.map {
                CoursePoint(it.name.orEmpty(), it.point.lat, it.point.lng)
            },
            destination = CoursePoint(destination.name.orEmpty(), destination.point.lat, destination.point.lng),
        )
    }
}
