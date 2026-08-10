package com.dororong.rodi.core.domain.usecase.member

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.repository.MemberRepository
import javax.inject.Inject

class GetBlockedMembersUseCase @Inject constructor(private val repository: MemberRepository) {
    suspend operator fun invoke(cursor: String? = null, size: Int = 20) = runSuspendCatching {
        repository.getBlockedMembers(cursor, size)
    }
}
