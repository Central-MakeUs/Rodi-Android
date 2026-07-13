package com.dororong.rodi.core.domain.usecase.auth

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.auth.AccountRestoreResult
import com.dororong.rodi.core.domain.repository.AuthRepository
import javax.inject.Inject

class RestoreWithKakaoUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(credential: String): Result<AccountRestoreResult> =
        runSuspendCatching { authRepository.restoreWithKakao(credential) }
}
