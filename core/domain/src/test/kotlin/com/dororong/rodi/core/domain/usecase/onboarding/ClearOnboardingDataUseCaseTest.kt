package com.dororong.rodi.core.domain.usecase.onboarding

import com.dororong.rodi.core.domain.repository.EntryRepository
import com.dororong.rodi.core.domain.repository.OnboardingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
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

    @Test
    fun `clears entry progress when onboarding cleanup fails and reports the failed target`() = runTest {
        val entryRepository = mockk<EntryRepository>()
        val onboardingRepository = mockk<OnboardingRepository>()
        coEvery { onboardingRepository.clear() } throws IllegalStateException("onboarding failed")
        coEvery { entryRepository.clear() } returns Unit

        val exception = try {
            ClearOnboardingDataUseCase(entryRepository, onboardingRepository)()
            throw AssertionError("OnboardingDataCleanupException should be thrown")
        } catch (exception: OnboardingDataCleanupException) {
            exception
        }

        assertEquals(setOf(OnboardingDataCleanupTarget.ONBOARDING), exception.failedTargets)
        coVerify { entryRepository.clear() }
    }

    @Test
    fun `clears onboarding profile when entry cleanup fails and reports the failed target`() = runTest {
        val entryRepository = mockk<EntryRepository>()
        val onboardingRepository = mockk<OnboardingRepository>()
        coEvery { onboardingRepository.clear() } returns Unit
        coEvery { entryRepository.clear() } throws IllegalStateException("entry failed")

        val exception = try {
            ClearOnboardingDataUseCase(entryRepository, onboardingRepository)()
            throw AssertionError("OnboardingDataCleanupException should be thrown")
        } catch (exception: OnboardingDataCleanupException) {
            exception
        }

        assertEquals(setOf(OnboardingDataCleanupTarget.ENTRY), exception.failedTargets)
        coVerify { onboardingRepository.clear() }
    }

    @Test
    fun `propagates cancellation without clearing entry progress`() = runTest {
        val entryRepository = mockk<EntryRepository>()
        val onboardingRepository = mockk<OnboardingRepository>()
        coEvery { onboardingRepository.clear() } throws CancellationException("cancelled")

        try {
            ClearOnboardingDataUseCase(entryRepository, onboardingRepository)()
        } catch (_: CancellationException) {
            coVerify(exactly = 0) { entryRepository.clear() }
            return@runTest
        }
        throw AssertionError("CancellationException should be rethrown")
    }
}
