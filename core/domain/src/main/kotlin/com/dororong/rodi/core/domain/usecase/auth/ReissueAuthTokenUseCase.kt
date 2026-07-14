package com.dororong.rodi.core.domain.usecase.auth

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.repository.AuthRepository
import javax.inject.Inject

class ReissueAuthTokenUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): Result<Unit> =
        runSuspendCatching { authRepository.reissueToken() }
}
