package com.dororong.rodi.core.domain.usecase.course

import com.dororong.rodi.core.domain.repository.CourseDraftRepository
import javax.inject.Inject

class ClearCourseDraftUseCase @Inject constructor(
    private val repository: CourseDraftRepository,
) {
    suspend operator fun invoke() = repository.clear()
}
