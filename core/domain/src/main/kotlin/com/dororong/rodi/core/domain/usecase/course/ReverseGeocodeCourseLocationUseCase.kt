package com.dororong.rodi.core.domain.usecase.course

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.repository.CourseLocationRepository
import javax.inject.Inject

class ReverseGeocodeCourseLocationUseCase @Inject constructor(
    private val repository: CourseLocationRepository,
) {
    suspend operator fun invoke(point: GeoPoint) = runSuspendCatching { repository.reverseGeocode(point) }
}
