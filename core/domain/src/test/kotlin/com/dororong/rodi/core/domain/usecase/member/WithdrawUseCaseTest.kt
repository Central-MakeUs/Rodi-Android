package com.dororong.rodi.core.domain.usecase.member

import com.dororong.rodi.core.domain.repository.MemberRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WithdrawUseCaseTest {
    @Test
    fun `returns success when repository withdraws account`() = runTest {
        val repository = mockk<MemberRepository>()
        coEvery { repository.withdraw() } returns Unit

        val result = WithdrawUseCase(repository)()

        assertTrue(result.isSuccess)
        coVerify { repository.withdraw() }
    }

    @Test
    fun `returns failure when repository withdraw fails`() = runTest {
        val repository = mockk<MemberRepository>()
        coEvery { repository.withdraw() } throws IllegalStateException("server error")

        val result = WithdrawUseCase(repository)()

        assertTrue(result.isFailure)
        coVerify { repository.withdraw() }
    }

    @Test
    fun `propagates cancellation when repository withdraw is cancelled`() = runTest {
        val repository = mockk<MemberRepository>()
        coEvery { repository.withdraw() } throws CancellationException("cancelled")

        try {
            WithdrawUseCase(repository)()
        } catch (_: CancellationException) {
            coVerify { repository.withdraw() }
            return@runTest
        }
        throw AssertionError("CancellationException should be rethrown")
    }
}
