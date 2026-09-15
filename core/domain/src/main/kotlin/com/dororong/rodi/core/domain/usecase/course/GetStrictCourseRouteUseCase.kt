package com.dororong.rodi.core.domain.usecase.course

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.course.CoursePoint
import com.dororong.rodi.core.domain.repository.CourseRegistrationRouteRepository
import javax.inject.Inject

class GetStrictCourseRouteUseCase @Inject constructor(
    private val repository: CourseRegistrationRouteRepository,
) {
    suspend operator fun invoke(
        origin: CoursePoint,
        waypoints: List<CoursePoint>,
        destination: CoursePoint,
    ) = runSuspendCatching { repository.getStrictRoute(origin, waypoints, destination) }
}
