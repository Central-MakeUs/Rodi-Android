package com.dororong.rodi.core.domain.usecase.course

import com.dororong.rodi.core.domain.repository.CourseLocationRepository
import javax.inject.Inject

class ObserveCourseSearchHistoryUseCase @Inject constructor(
    private val repository: CourseLocationRepository,
) {
    operator fun invoke() = repository.observeRecent()
}
