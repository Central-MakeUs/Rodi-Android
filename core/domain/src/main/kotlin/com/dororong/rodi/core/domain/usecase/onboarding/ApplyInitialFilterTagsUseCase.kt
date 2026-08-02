package com.dororong.rodi.core.domain.usecase.onboarding

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.model.onboarding.initialFilterTags
import com.dororong.rodi.core.domain.repository.MemberRepository
import com.dororong.rodi.core.domain.repository.OnboardingRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class ApplyInitialFilterTagsUseCase @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
    private val memberRepository: MemberRepository,
) {
    suspend operator fun invoke(level: OnboardingLevel): Result<Unit> {
        if (onboardingRepository.isInitialFilterTagsApplied.first()) return Result.success(Unit)
        return runSuspendCatching {
            memberRepository.updateFilterTags(level.initialFilterTags.toList())
            onboardingRepository.markInitialFilterTagsApplied()
        }
    }
}
