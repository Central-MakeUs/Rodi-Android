package com.dororong.rodi.core.domain.model.auth

import java.time.Instant

sealed interface AccountRestoreResult {
    data class Restored(
        val isOnboarded: Boolean,
        val nickname: String,
    ) : AccountRestoreResult

    data class WithdrawalPending(
        val withdrawalRequestedAt: Instant?,
        val recoverableUntil: Instant?,
    ) : AccountRestoreResult

    /** 복구를 누르는 사이 유예 기간이 지났다. 복구할 수 없고 [reRegisterableAt] 이후 다시 가입할 수 있다. */
    data class WithdrawalLocked(val reRegisterableAt: Instant?) : AccountRestoreResult
}
