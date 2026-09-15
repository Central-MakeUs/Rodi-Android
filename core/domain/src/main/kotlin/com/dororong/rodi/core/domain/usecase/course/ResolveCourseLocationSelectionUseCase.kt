package com.dororong.rodi.core.domain.usecase.course

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.course.CourseLocationSuggestion
import com.dororong.rodi.core.domain.repository.CourseLocationRepository
import javax.inject.Inject

class ResolveCourseLocationSelectionUseCase @Inject constructor(
    private val repository: CourseLocationRepository,
) {
    suspend operator fun invoke(suggestion: CourseLocationSuggestion) =
        runSuspendCatching { repository.resolveSelection(suggestion) }
}
