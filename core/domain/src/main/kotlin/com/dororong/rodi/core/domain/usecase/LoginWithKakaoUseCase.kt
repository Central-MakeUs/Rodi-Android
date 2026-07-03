package com.dororong.rodi.core.domain.usecase

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.AuthRepository
import javax.inject.Inject

class LoginWithKakaoUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(kakaoAccessToken: String): Result<Unit> =
        runSuspendCatching { authRepository.loginWithKakao(kakaoAccessToken) }
}
