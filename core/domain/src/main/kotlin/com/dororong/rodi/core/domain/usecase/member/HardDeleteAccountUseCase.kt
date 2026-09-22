package com.dororong.rodi.core.domain.usecase.member

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.member.HardDeleteResult
import com.dororong.rodi.core.domain.repository.MemberRepository
import javax.inject.Inject

class HardDeleteAccountUseCase @Inject constructor(
    private val memberRepository: MemberRepository,
) {
    suspend operator fun invoke(): Result<HardDeleteResult> = runSuspendCatching { memberRepository.hardDelete() }
}
