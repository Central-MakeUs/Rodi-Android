package com.dororong.rodi.core.domain.usecase.course

import com.dororong.rodi.core.domain.repository.CourseLocationRepository
import javax.inject.Inject

class ClearCourseSearchHistoryUseCase @Inject constructor(
    private val repository: CourseLocationRepository,
) {
    suspend operator fun invoke() = repository.clearRecent()
}
