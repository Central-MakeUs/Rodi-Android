package com.dororong.rodi.core.domain.usecase.member

import com.dororong.rodi.core.domain.repository.MemberRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HasPracticeRecordsUseCaseTest {
    private val repository = mockk<MemberRepository>()
    private val useCase = HasPracticeRecordsUseCase(repository)

    @Test
    fun `returns repository practice presence`() = runTest {
        coEvery { repository.hasPracticeRecords() } returns true

        assertTrue(useCase().getOrThrow())
        coVerify(exactly = 1) { repository.hasPracticeRecords() }
    }
}
