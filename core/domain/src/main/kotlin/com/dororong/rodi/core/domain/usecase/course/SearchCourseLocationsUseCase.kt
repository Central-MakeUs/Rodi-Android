package com.dororong.rodi.core.domain.usecase.course

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.repository.CourseLocationRepository
import javax.inject.Inject

class SearchCourseLocationsUseCase @Inject constructor(
    private val repository: CourseLocationRepository,
) {
    suspend operator fun invoke(keyword: String) = runSuspendCatching { repository.search(keyword) }
}
