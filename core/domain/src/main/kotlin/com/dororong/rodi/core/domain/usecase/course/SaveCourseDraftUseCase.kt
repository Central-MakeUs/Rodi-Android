package com.dororong.rodi.core.domain.usecase.course

import com.dororong.rodi.core.domain.model.course.CourseDraft
import com.dororong.rodi.core.domain.repository.CourseDraftRepository
import javax.inject.Inject

class SaveCourseDraftUseCase @Inject constructor(
    private val repository: CourseDraftRepository,
) {
    suspend operator fun invoke(draft: CourseDraft) {
        if (draft.isMeaningful) repository.save(draft) else repository.clear()
    }
}
