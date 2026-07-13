package com.dororong.rodi.core.domain.usecase.member

import com.dororong.rodi.core.domain.repository.MemberRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
}
