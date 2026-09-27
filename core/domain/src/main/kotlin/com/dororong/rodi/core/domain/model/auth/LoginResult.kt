package com.dororong.rodi.core.domain.model.auth

import java.time.Instant

sealed interface LoginResult {
    /** [isOnboarded]가 false면 신규 가입이든 가입 후 온보딩 중 이탈한 회원이든 온보딩으로 보낸다. */
    data class Success(
        val isOnboarded: Boolean,
        val nickname: String,
    ) : LoginResult

    /**
     * 탈퇴 유예 기간 중 재로그인. 두 시각은 안내 문구용이라 없거나 해석에 실패해도 복구 자체는 막지 않는다.
     * 서버가 `date-time`으로 선언해두고 실제로는 오프셋 없이 내려보내는 이력이 있어, 여기서 실패를
     * 예외로 올리면 복구 화면에 진입할 방법이 사라진다.
     */
    data class WithdrawalPending(
        val withdrawalRequestedAt: Instant?,
        val recoverableUntil: Instant?,
    ) : LoginResult
}
