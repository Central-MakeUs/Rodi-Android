package com.dororong.rodi.core.domain.usecase.auth

import com.dororong.rodi.core.domain.model.auth.AuthSession
import com.dororong.rodi.core.domain.repository.AuthRepository
import javax.inject.Inject

class GetAuthSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): AuthSession = authRepository.getSession()
}
