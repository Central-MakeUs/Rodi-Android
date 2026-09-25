package com.dororong.rodi.core.domain.usecase.member

import com.dororong.rodi.core.domain.repository.MemberRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WithdrawUseCaseTest {
    @Test
    fun `returns success when repository withdraws account`() = runTest {
        val repository = mockk<MemberRepository>()
        coEvery { repository.withdraw() } returns Unit

        val result = WithdrawUseCase(repository)()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `returns failure when repository withdraw fails`() = runTest {
        val repository = mockk<MemberRepository>()
        coEvery { repository.withdraw() } throws IllegalStateException("server error")

        val result = WithdrawUseCase(repository)()

        assertTrue(result.isFailure)
    }

    @Test
    fun `propagates cancellation when repository withdraw is cancelled`() = runTest {
        val repository = mockk<MemberRepository>()
        val cancellation = CancellationException("cancelled")
        coEvery { repository.withdraw() } throws cancellation

        try {
            WithdrawUseCase(repository)()
        } catch (thrown: CancellationException) {
            assertSame(cancellation, thrown)
            return@runTest
        }
        throw AssertionError("CancellationException should be rethrown")
    }
}
