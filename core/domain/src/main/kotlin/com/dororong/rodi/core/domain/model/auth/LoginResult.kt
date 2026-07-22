package com.dororong.rodi.core.domain.model.auth

import java.time.Instant

sealed interface LoginResult {
    data class Success(
        val isNewMember: Boolean,
        val nickname: String,
    ) : LoginResult

    data class WithdrawalPending(
        val withdrawalRequestedAt: Instant,
        val recoverableUntil: Instant,
    ) : LoginResult
}
