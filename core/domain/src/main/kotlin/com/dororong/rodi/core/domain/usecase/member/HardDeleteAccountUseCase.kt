package com.dororong.rodi.core.domain.usecase.member

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.repository.MemberRepository
import com.dororong.rodi.core.domain.usecase.onboarding.ClearOnboardingDataUseCase
import javax.inject.Inject

class HardDeleteAccountUseCase @Inject constructor(
    private val memberRepository: MemberRepository,
    private val clearOnboardingData: ClearOnboardingDataUseCase,
) {
    suspend operator fun invoke(): Result<Unit> =
        runSuspendCatching {
            memberRepository.hardDelete()
            clearOnboardingData()
        }
}
