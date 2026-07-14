package com.dororong.rodi.core.domain.usecase.auth

import com.dororong.rodi.core.domain.model.auth.AccountRestoreResult
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class AccountAuthUseCasesTest {
    @Test
    fun `reissue returns success when repository succeeds`() = runTest {
        val repository = mockk<AuthRepository>()
        coEvery { repository.reissueToken() } returns Unit

        val result = ReissueAuthTokenUseCase(repository)()

        assertTrue(result.isSuccess)
        coVerify { repository.reissueToken() }
    }

    @Test
    fun `restore wraps repository failure as Result`() = runTest {
        val repository = mockk<AuthRepository>()
        coEvery { repository.restoreWithKakao("credential") } throws AuthException.RecoveryExpired("기간 만료")

        val result = RestoreWithKakaoUseCase(repository)("credential")

        assertTrue(result.isFailure)
        assertEquals("기간 만료", result.exceptionOrNull()?.message)
    }

    @Test
    fun `logout propagates cancellation`() = runTest {
        val repository = mockk<AuthRepository>()
        coEvery { repository.logout() } throws CancellationException("cancelled")

        try {
            LogoutUseCase(repository)()
        } catch (_: CancellationException) {
            coVerify { repository.logout() }
            return@runTest
        }
        throw AssertionError("CancellationException should be rethrown")
    }

    @Test
    fun `restore returns domain result from repository`() = runTest {
        val repository = mockk<AuthRepository>()
        val expected = AccountRestoreResult.WithdrawalPending(
            withdrawalRequestedAt = Instant.parse("2026-07-13T00:00:00Z"),
            recoverableUntil = Instant.parse("2026-07-16T00:00:00Z"),
        )
        coEvery { repository.restoreWithKakao("credential") } returns expected

        val result = RestoreWithKakaoUseCase(repository)("credential")

        assertEquals(expected, result.getOrThrow())
    }
}
