package com.dororong.rodi.core.domain.usecase.member

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.repository.MemberRepository
import javax.inject.Inject

class UpdateFilterTagsUseCase @Inject constructor(
    private val repository: MemberRepository,
) {
    suspend operator fun invoke(filterTags: Set<PracticeType>) =
        runSuspendCatching { repository.updateFilterTags(filterTags.toList()) }
}
