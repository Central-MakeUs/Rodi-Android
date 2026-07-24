package com.dororong.rodi.core.domain.usecase.onboarding

import com.dororong.rodi.core.domain.repository.EntryRepository
import com.dororong.rodi.core.domain.repository.OnboardingRepository
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ClearOnboardingDataUseCaseTest {
    @Test
    fun `clears onboarding profile and entry progress`() = runTest {
        val entryRepository = mockk<EntryRepository>()
        val onboardingRepository = mockk<OnboardingRepository>()
        coEvery { onboardingRepository.clear() } returns Unit
        coEvery { entryRepository.clear() } returns Unit

        ClearOnboardingDataUseCase(entryRepository, onboardingRepository)()

        coVerifyOrder {
            onboardingRepository.clear()
            entryRepository.clear()
        }
    }
}
