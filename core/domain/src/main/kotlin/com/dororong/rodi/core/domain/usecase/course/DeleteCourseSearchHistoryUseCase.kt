package com.dororong.rodi.core.domain.usecase.course

import com.dororong.rodi.core.domain.repository.CourseLocationRepository
import javax.inject.Inject

class DeleteCourseSearchHistoryUseCase @Inject constructor(
    private val repository: CourseLocationRepository,
) {
    suspend operator fun invoke(id: String) = repository.deleteRecent(id)
}
