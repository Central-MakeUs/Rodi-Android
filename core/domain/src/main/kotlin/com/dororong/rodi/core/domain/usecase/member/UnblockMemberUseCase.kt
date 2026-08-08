package com.dororong.rodi.core.domain.usecase.member

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.repository.MemberRepository
import javax.inject.Inject

class UnblockMemberUseCase @Inject constructor(
    private val repository: MemberRepository,
) {
    suspend operator fun invoke(memberId: Long) = runSuspendCatching { repository.unblockMember(memberId) }
}
