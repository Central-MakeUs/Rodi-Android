package com.dororong.rodi.core.domain.usecase.course

import com.dororong.rodi.core.domain.model.course.CourseLocationSuggestion
import com.dororong.rodi.core.domain.repository.CourseLocationRepository
import javax.inject.Inject

class SaveCourseSearchHistoryUseCase @Inject constructor(
    private val repository: CourseLocationRepository,
) {
    suspend operator fun invoke(suggestion: CourseLocationSuggestion) = repository.saveRecent(suggestion)
}
