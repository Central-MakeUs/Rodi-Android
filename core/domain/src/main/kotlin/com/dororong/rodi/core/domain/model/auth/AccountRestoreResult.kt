package com.dororong.rodi.core.domain.model.auth

import java.time.Instant

sealed interface AccountRestoreResult {
    data class Restored(
        val isNewMember: Boolean,
        val nickname: String,
    ) : AccountRestoreResult

    data class WithdrawalPending(
        val withdrawalRequestedAt: Instant?,
        val recoverableUntil: Instant?,
    ) : AccountRestoreResult
}
