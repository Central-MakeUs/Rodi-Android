package com.dororong.rodi.core.domain.usecase.member

import com.dororong.rodi.core.domain.repository.MemberRepository
import com.dororong.rodi.core.domain.usecase.onboarding.ClearOnboardingDataUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WithdrawUseCaseTest {
    @Test
    fun `returns success when repository withdraws account`() = runTest {
        val repository = mockk<MemberRepository>()
        val clearOnboardingData = mockk<ClearOnboardingDataUseCase>()
        coEvery { repository.withdraw() } returns Unit
        coEvery { clearOnboardingData() } returns Unit

        val result = WithdrawUseCase(repository, clearOnboardingData)()

        assertTrue(result.isSuccess)
        coVerifyOrder {
            repository.withdraw()
            clearOnboardingData()
        }
    }

    @Test
    fun `returns failure when repository withdraw fails`() = runTest {
        val repository = mockk<MemberRepository>()
        val clearOnboardingData = mockk<ClearOnboardingDataUseCase>()
        coEvery { repository.withdraw() } throws IllegalStateException("server error")

        val result = WithdrawUseCase(repository, clearOnboardingData)()

        assertTrue(result.isFailure)
        coVerify { repository.withdraw() }
        coVerify(exactly = 0) { clearOnboardingData() }
    }

    @Test
    fun `propagates cancellation when repository withdraw is cancelled`() = runTest {
        val repository = mockk<MemberRepository>()
        val clearOnboardingData = mockk<ClearOnboardingDataUseCase>()
        coEvery { repository.withdraw() } throws CancellationException("cancelled")

        try {
            WithdrawUseCase(repository, clearOnboardingData)()
        } catch (_: CancellationException) {
            coVerify { repository.withdraw() }
            coVerify(exactly = 0) { clearOnboardingData() }
            return@runTest
        }
        throw AssertionError("CancellationException should be rethrown")
    }
}
