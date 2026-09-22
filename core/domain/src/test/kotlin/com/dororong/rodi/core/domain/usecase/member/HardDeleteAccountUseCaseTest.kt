package com.dororong.rodi.core.domain.usecase.member

import com.dororong.rodi.core.domain.model.member.HardDeleteResult
import com.dororong.rodi.core.domain.repository.MemberRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HardDeleteAccountUseCaseTest {
    @Test
    fun `keeps the local cleanup outcome reported by the repository`() = runTest {
        val repository = mockk<MemberRepository>()
        coEvery { repository.hardDelete() } returns HardDeleteResult(localCleanupSucceeded = false)

        val result = HardDeleteAccountUseCase(repository)()

        assertEquals(HardDeleteResult(localCleanupSucceeded = false), result.getOrNull())
    }

    @Test
    fun `returns failure when hard delete fails`() = runTest {
        val repository = mockk<MemberRepository>()
        coEvery { repository.hardDelete() } throws IllegalStateException("server error")

        val result = HardDeleteAccountUseCase(repository)()

        assertTrue(result.isFailure)
    }

    @Test
    fun `propagates cancellation`() = runTest {
        val repository = mockk<MemberRepository>()
        val cancellation = CancellationException("cancelled")
        coEvery { repository.hardDelete() } throws cancellation

        try {
            HardDeleteAccountUseCase(repository)()
        } catch (thrown: CancellationException) {
            assertSame(cancellation, thrown)
            return@runTest
        }
        throw AssertionError("CancellationException should be rethrown")
    }
}
