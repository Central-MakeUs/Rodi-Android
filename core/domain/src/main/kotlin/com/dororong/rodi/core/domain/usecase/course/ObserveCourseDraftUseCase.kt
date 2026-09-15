package com.dororong.rodi.core.domain.usecase.course

import com.dororong.rodi.core.domain.repository.CourseDraftRepository
import javax.inject.Inject

class ObserveCourseDraftUseCase @Inject constructor(
    private val repository: CourseDraftRepository,
) {
    operator fun invoke() = repository.observe()
}
