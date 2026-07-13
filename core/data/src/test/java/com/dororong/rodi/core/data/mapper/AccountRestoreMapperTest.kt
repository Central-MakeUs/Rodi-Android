package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.auth.SocialLoginResponse
import com.dororong.rodi.core.domain.model.auth.AccountRestoreResult
import com.dororong.rodi.core.domain.model.auth.AuthException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class AccountRestoreMapperTest {
    @Test
    fun `maps withdrawal pending timestamps to domain result`() {
        val response = SocialLoginResponse(
            status = "WITHDRAWAL_PENDING",
            withdrawalRequestedAt = "2026-07-13T00:00:00+09:00",
            recoverableUntil = "2026-07-16T00:00:00+09:00",
        )

        val result = response.toAccountRestoreResult()

        assertEquals(
            AccountRestoreResult.WithdrawalPending(
                withdrawalRequestedAt = Instant.parse("2026-07-12T15:00:00Z"),
                recoverableUntil = Instant.parse("2026-07-15T15:00:00Z"),
            ),
            result,
        )
    }

    @Test
    fun `rejects success response without required tokens`() {
        val response = SocialLoginResponse(
            status = "SUCCESS",
            isNewMember = false,
            nickname = "로디",
        )

        val exception = assertThrows(AuthException.Unknown::class.java) { response.toAuthTokenResponse() }

        assertTrue(exception.message!!.contains("accessToken"))
    }
}
