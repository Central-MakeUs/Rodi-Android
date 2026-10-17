package com.dororong.rodi.core.domain.usecase.member

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.repository.MemberRepository
import javax.inject.Inject

class CheckNicknameAvailabilityUseCase @Inject constructor(private val repository: MemberRepository) {
    suspend operator fun invoke(nickname: String) =
        runSuspendCatching { repository.checkNicknameAvailability(nickname) }
}
